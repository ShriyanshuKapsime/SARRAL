# SARRAL Score Algorithm Update

## Summary of Changes

The SARRAL score calculation algorithm has been updated to better reflect creditworthiness based on
income level and consistency.

**New**: Calculated scores are now automatically saved to Firestore `user_profiles` collection.

---

## New Algorithm (Updated)

### Step 1: Calculate Monthly Inflow

```
monthly_inflow = sum(all_6_month_totals) / 6
```

### Step 2: Calculate Income Score

```
income_score = (monthly_inflow / 60000) * 100

If income_score > 100:
    income_score = 100
```

**Benchmark**: ₹60,000/month = 100 income score

- ₹60,000 or higher → 100 points
- ₹30,000 → 50 points
- ₹12,000 → 20 points
- ₹6,000 → 10 points

### Step 3: Calculate Consistency Score

```
max_month = maximum of monthly_totals
min_month = minimum of monthly_totals
consistency_score = 100 - ((max_month - min_month) / max_month * 100)

If consistency_score < 0:
    consistency_score = 0
If consistency_score > 100:
    consistency_score = 100
```

**Benchmark**: Perfect consistency (same amount every month) = 100 points

- Same amount each month → 100 points
- ±10% variation → ~90 points
- ±20% variation → ~80 points
- ±50% variation → ~50 points

### Step 4: Calculate Final SARRAL Score

```
sarral_score = (income_score * 0.3) + (consistency_score * 0.7)

Round to nearest integer
```

**Weightage**:

- Income Score: 30%
- Consistency Score: 70%

**Why 30/70?** Consistency is more important than absolute income for loan repayment reliability.

### Step 5: Calculate Loan Limit

```
loan_limit = monthly_inflow * 0.30

Round to nearest rupee
```

**Loan limit is always 30% of monthly income** (independent of score).

### Step 6: Save to Firestore (NEW)

```
Update user_profiles document:
  - sarral_score = calculated_score
  - loan_limit = calculated_limit
  - last_score_update = current_timestamp
```

**Persistence**: Scores are automatically saved to user profile for later retrieval.

---

## Firestore Updates

### Score Persistence

After calculating the SARRAL score and loan limit, the system now automatically updates the user's
profile in Firestore:

```kotlin
firestore.collection("user_profiles")
    .document(currentUser.uid)
    .update(mapOf(
        "sarral_score" to roundedSarralScore,
        "loan_limit" to roundedLoanLimit,
        "last_score_update" to Timestamp.now()
    ))
```

### Updated user_profiles Structure

```javascript
{
  "uid": "user_uid",
  "upi_id": "test@paytm",
  "role": "borrower",
  "sarral_score": 60,           // 
  "loan_limit": 2685,            // 
  "goodwill_score": 85,          // Manual/separate calculation
  "last_score_update": "timestamp", // 
  "updated_at": "timestamp"
}
```

### Benefits

1. **Profile Persistence**: Scores available in UserProfileScreen without recalculation
2. **Historical Tracking**: `last_score_update` timestamp tracks when score was calculated
3. **Quick Access**: Other screens can read scores from profile instead of recalculating
4. **Consistency**: Same score displayed across all screens
5. **Audit Trail**: Can track score changes over time

### Error Handling

- If Firestore update fails, the scores are still displayed to the user
- Calculation success takes priority over persistence
- Silent failure for Firestore updates (calculation is more important)

---

## Old vs New Algorithm Comparison

| Aspect | Old Algorithm | New Algorithm |
|--------|--------------|---------------|
| Income Metric | `monthly_inflow * 0.6` | `income_score * 0.3` |
| Income Cap | None | Capped at 100 (₹60k/month) |
| Consistency Weight | 40% | 70% |
| Income Weight | 60% | 30% |
| Score Range | 0-∞ (unbounded) | 0-100 (normalized) |
| Display Format | Raw number | "X/100" |

---

## Example Calculations

### Example 1: Default Test Data (Low-Medium Income, Good Consistency)

**Monthly Totals**: ₹8,000, ₹9,500, ₹8,200, ₹10,000, ₹8,800, ₹9,200

**Calculations**:

- Monthly Inflow: ₹53,700 / 6 = ₹8,950
- Income Score: (8,950 / 60,000) × 100 = 14.92
- Max Month: ₹10,000
- Min Month: ₹8,000
- Consistency Score: 100 - ((10,000 - 8,000) / 10,000 × 100) = 80
- **SARRAL Score**: (14.92 × 0.3) + (80 × 0.7) = 4.48 + 56 = **60/100**
- **Loan Limit**: ₹8,950 × 0.30 = **₹2,685**

**Old Algorithm Result**: 5,402 (misleading, looks like ₹5,402)
**New Algorithm Result**: 60/100 (clear score out of 100)

---

### Example 2: High Income, Perfect Consistency

**Monthly Totals**: ₹60,000 × 6 months (all equal)

**Calculations**:

- Monthly Inflow: ₹360,000 / 6 = ₹60,000
- Income Score: (60,000 / 60,000) × 100 = 100
- Consistency Score: 100 - 0 = 100
- **SARRAL Score**: (100 × 0.3) + (100 × 0.7) = 30 + 70 = **100/100**
- **Loan Limit**: ₹60,000 × 0.30 = **₹18,000**

---

### Example 3: Very High Income, Poor Consistency

**Monthly Totals**: ₹20,000, ₹80,000, ₹30,000, ₹100,000, ₹25,000, ₹65,000

**Calculations**:

- Monthly Inflow: ₹320,000 / 6 = ₹53,333
- Income Score: (53,333 / 60,000) × 100 = 88.89 (capped at 100, but under cap)
- Max Month: ₹100,000
- Min Month: ₹20,000
- Consistency Score: 100 - ((100,000 - 20,000) / 100,000 × 100) = 20
- **SARRAL Score**: (88.89 × 0.3) + (20 × 0.7) = 26.67 + 14 = **41/100**
- **Loan Limit**: ₹53,333 × 0.30 = **₹16,000**

**Key Insight**: High income doesn't guarantee high score if inconsistent!

---

### Example 4: Medium Income, Perfect Consistency

**Monthly Totals**: ₹25,000 × 6 months (all equal)

**Calculations**:

- Monthly Inflow: ₹150,000 / 6 = ₹25,000
- Income Score: (25,000 / 60,000) × 100 = 41.67
- Consistency Score: 100
- **SARRAL Score**: (41.67 × 0.3) + (100 × 0.7) = 12.50 + 70 = **83/100**
- **Loan Limit**: ₹25,000 × 0.30 = **₹7,500**

**Key Insight**: Moderate but consistent income gets excellent score!

---

### Example 5: Low Income, Excellent Consistency

**Monthly Totals**: ₹10,000, ₹10,200, ₹9,900, ₹10,100, ₹9,800, ₹10,000

**Calculations**:

- Monthly Inflow: ₹60,000 / 6 = ₹10,000
- Income Score: (10,000 / 60,000) × 100 = 16.67
- Max Month: ₹10,200
- Min Month: ₹9,800
- Consistency Score: 100 - ((10,200 - 9,800) / 10,200 × 100) = 96.08
- **SARRAL Score**: (16.67 × 0.3) + (96.08 × 0.7) = 5.00 + 67.26 = **72/100**
- **Loan Limit**: ₹10,000 × 0.30 = **₹3,000**

---

## Score Interpretation Guide

| Score Range | Rating | Interpretation | Action |
|-------------|--------|----------------|--------|
| 90-100 | Excellent | High/stable income, perfect consistency | Premium rates, high approval |
| 80-89 | Very Good | Good income or excellent consistency | Good rates, high approval |
| 70-79 | Good | Decent income with good consistency | Standard rates, likely approval |
| 60-69 | Fair | Moderate income or fair consistency | Higher rates, case-by-case |
| 50-59 | Below Average | Low income or poor consistency | High rates, cautious approval |
| 40-49 | Poor | Low and inconsistent income | Very high rates, risky |
| 0-39 | Very Poor | Very low or very inconsistent | Likely rejection |

---

## Key Improvements

### 1. Normalized Score (0-100)

- **Old**: Score could be 5,000+ (confusing)
- **New**: Always between 0-100 (clear)

### 2. Income Benchmarking

- **Old**: Linear scaling (unlimited)
- **New**: Benchmarked against ₹60k/month

### 3. Consistency Prioritized

- **Old**: 40% weight
- **New**: 70% weight (more important for loan repayment)

### 4. Bounded Scores

- **Old**: No bounds
- **New**: All scores bounded 0-100

### 5. Clear Display

- **Old**: "5402" (what does this mean?)
- **New**: "60/100" (clear percentage-like score)

---

## UI Updates

### Before:

```
Your SARRAL Score
5402
Loan Limit Available: ₹2,685
```

### After:

```
Your SARRAL Score
60/100
Loan Limit Available: ₹2,685
```

---

## Implementation Details

### Files Updated:

1. ✅ `BorrowerLoanDashboardScreen.kt` - Main calculation logic
2. ✅ `TestDataSeeder.kt` - Expected score calculation
3. ✅ `SARRAL_SCORE_CALCULATION_GUIDE.md` - Documentation

### Code Changes:

```kotlin
// Added income score calculation
var incomeScore = (monthlyInflow / 60000.0) * 100.0
if (incomeScore > 100.0) {
    incomeScore = 100.0
}

// Updated consistency score with bounds
var consistencyScore = if (maxMonth > 0) {
    100.0 - ((maxMonth - minMonth) / maxMonth * 100.0)
} else {
    0.0
}
if (consistencyScore < 0.0) consistencyScore = 0.0
if (consistencyScore > 100.0) consistencyScore = 100.0

// New SARRAL score formula
val sarralScore = (incomeScore * 0.3) + (consistencyScore * 0.7)

// Display updated
text = "$sarralScore/100"
```

---

## Testing

### Quick Test:

1. Enter UPI ID: `test@paytm`
2. Click "🧪 Seed Test Data"
3. Click "Verify UPI"
4. **Expected Result**:
    - SARRAL Score: **60/100** (was 5402)
    - Loan Limit: **₹2,685** (unchanged)

### Verify Calculation:

```kotlin
val (score, limit) = TestDataSeeder.calculateExpectedScore(
    listOf(8000.0, 9500.0, 8200.0, 10000.0, 8800.0, 9200.0)
)
// score = 60, limit = 2685
```

---

## Migration Notes

### For Existing Users:

- Scores will be **significantly lower** (normalized to 0-100)
- A score of 5,402 → becomes ~60/100
- This is **expected and correct**
- Loan limits remain the same (30% of monthly income)

### For New Features:

- Can now easily set thresholds (e.g., require 70+ for instant approval)
- Can compare scores meaningfully (60 vs 80 is clear)
- Can show score trends over time

---

## FAQs

### Q: Why did my score drop from 5,402 to 60?

**A**: The old score was misleading. The new score is normalized to 0-100 scale. A 60/100 score with
₹9k/month income is accurate.

### Q: Why is consistency weighted more (70%) than income (30%)?

**A**: For loan repayment, consistent income is more reliable than high but variable income. Someone
earning ₹25k consistently is safer than someone earning ₹50k but inconsistently.

### Q: What's a good score?

**A**:

- 70+: Good creditworthiness
- 80+: Very good creditworthiness
- 90+: Excellent creditworthiness

### Q: Can I get 100/100?

**A**: Yes! You need ₹60k+/month with perfect consistency (same amount every month).

### Q: Does loan limit depend on score?

**A**: No. Loan limit is always 30% of monthly income. The score determines approval likelihood and
interest rates.

---

## Summary

✅ **Algorithm updated** to be more meaningful and normalized
✅ **Score range**: 0-100 (clear and bounded)
✅ **Weightage**: 30% income, 70% consistency (reliability-focused)
✅ **Display**: "X/100" format (intuitive)
✅ **Benchmarked**: Against ₹60k/month standard
✅ **Documentation**: Fully updated

**Status**: ✅ Ready for production testing

---

**Last Updated**: Algorithm v2.0
**Effective**: Immediate
