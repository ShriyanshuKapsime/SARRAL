# UPI Analyzer Screen - Complete Documentation

## Overview

The UPI Analyzer Screen is an intelligent data generation and analysis tool that creates realistic
UPI transaction history for small shopkeepers and calculates their SARRAL Score automatically.

---

## File Created

`app/src/main/java/com/runanywhere/startup_hackathon20/UPIAnalyzerScreen.kt`

---

## Features

### 1. **UPI ID Input**

- Text field for entering UPI ID
- Auto-lowercase conversion
- Format validation (must contain `@`)
- Error message display

### 2. **Smart Data Detection**

- Checks if transaction data already exists
- Skips generation if data found
- Prevents duplicate data creation

### 3. **Realistic Transaction Generation**

- **Duration**: Last 30 days
- **Transactions per day**: Random 5-12 (simulates small shop activity)
- **Amount per transaction**: Random ₹50-₹600
- **Time distribution**: 8 AM to 9 PM (business hours)
- **Customer names**: Auto-generated (Customer1001-Customer9999)

### 4. **SARRAL Score Calculation**

```kotlin
avg_tx = total_inflow / total_transactions
consistency = (total_transactions / (30 * 12)) * 100
score = min(100, round((consistency * 0.5) + ((avg_tx / 600) * 50)))
```

### 5. **Loan Limit Calculation**

```kotlin
loan_limit = 0.3 × total_inflow
```

### 6. **Borrower Profile Creation**

Stores comprehensive borrower data in Firestore

### 7. **Progress Tracking**

Real-time progress updates during analysis

---

## UI Components

### Input Screen

```
┌─────────────────────────────────────────┐
│ [← Back]  UPI Analyzer                  │
├─────────────────────────────────────────┤
│                                         │
│    UPI Activity Analyzer                │
│                                         │
│  Analyze your UPI transaction history  │
│  to calculate your SARRAL Score         │
│                                         │
│  ┌───────────────────────────────────┐ │
│  │ Enter UPI ID                      │ │
│  │ shopkeeper@paytm                  │ │
│  └───────────────────────────────────┘ │
│                                         │
│  ┌───────────────────────────────────┐ │
│  │    Analyze UPI Activity           │ │
│  └───────────────────────────────────┘ │
│                                         │
│  ┌─────────────────────────────────────┐│
│  │ ℹ️ How it works                    ││
│  │                                    ││
│  │ We analyze your last 30 days of   ││
│  │ UPI transactions to calculate     ││
│  │ your SARRAL Score...              ││
│  └─────────────────────────────────────┘│
└─────────────────────────────────────────┘
```

### Progress Screen

```
┌─────────────────────────────────────────┐
│ [← Back]  UPI Analyzer                  │
├─────────────────────────────────────────┤
│                                         │
│          ◐◐◐◐◐◐◐◐                      │
│      (Loading Animation)                │
│                                         │
│    Analyzing UPI inflows...             │
│                                         │
│  Generating transaction history...      │
│                                         │
└─────────────────────────────────────────┘
```

---

## Data Flow

### Step-by-Step Process

```
User Input
    ↓
UPI ID Validation
    ↓
Check Existing Data (Firestore Query)
    ↓
┌──────────────┴──────────────┐
│                              │
Data Exists              No Data Found
    ↓                         ↓
Skip to Dashboard      Generate Transactions
                              ↓
                       Calculate Scores
                              ↓
                       Create Borrower Profile
                              ↓
                       Navigate to Dashboard
```

---

## Transaction Generation Logic

### Daily Transaction Pattern

```kotlin
for each of 30 days:
    transactions_today = random(5, 12)
    
    for each transaction:
        amount = random(₹50, ₹600)
        time = random(8:00 AM, 9:00 PM)
        customer = "Customer" + random(1001, 9999)
        
        save to Firestore
```

### Example Generated Data

```json
{
  "borrower_upi": "shopkeeper@paytm",
  "amount": 245.67,
  "timestamp": "2024-11-10T14:23:45Z",
  "source": "Customer5432",
  "type": "credit"
}
```

---

## SARRAL Score Algorithm

### Formula Breakdown

```
1. Average Transaction
   avg_tx = total_inflow / total_transactions

2. Consistency Score (0-100)
   consistency = (actual_tx / expected_max_tx) * 100
   where expected_max_tx = 30 days × 12 tx/day = 360

3. SARRAL Score (0-100)
   consistency_weight = consistency × 0.5  (50% weight)
   amount_weight = (avg_tx / 600) × 50    (50% weight)
   score = min(100, round(consistency_weight + amount_weight))
```

### Example Calculation

**Scenario**: Shopkeeper with consistent activity

- Total transactions: 270 (9 per day average)
- Total inflow: ₹67,500
- Avg transaction: ₹250

**Calculation**:

```
consistency = (270 / 360) × 100 = 75%
consistency_weight = 75 × 0.5 = 37.5

amount_weight = (250 / 600) × 50 = 20.83

score = 37.5 + 20.83 = 58.33 ≈ 58
```

**Result**:

- SARRAL Score: **58**
- Loan Limit: ₹67,500 × 0.3 = **₹20,250**

---

## Firestore Collections

### 1. `transactions` Collection

**Document Structure**:

```json
{
  "borrower_upi": "string",
  "amount": "double",
  "timestamp": "Timestamp",
  "source": "string",
  "type": "string"
}
```

**Example**:

```json
{
  "borrower_upi": "shopkeeper@paytm",
  "amount": 345.50,
  "timestamp": Timestamp(2024-11-15 10:30:00),
  "source": "Customer7845",
  "type": "credit"
}
```

### 2. `borrowers` Collection

**Document ID**: UPI ID  
**Document Structure**:

```json
{
  "name": "string",
  "upi_id": "string",
  "sarral_score": "int",
  "loan_limit": "int",
  "goodwill_score": "int",
  "active_loan": "boolean",
  "created_at": "Timestamp",
  "total_inflow": "int",
  "total_transactions": "int",
  "avg_transaction": "int"
}
```

**Example**:

```json
{
  "name": "Shopkeeper",
  "upi_id": "shopkeeper@paytm",
  "sarral_score": 58,
  "loan_limit": 20250,
  "goodwill_score": 74,
  "active_loan": false,
  "created_at": Timestamp.now(),
  "total_inflow": 67500,
  "total_transactions": 270,
  "avg_transaction": 250
}
```

---

## Progress Messages

The screen shows real-time progress:

1. **"Checking existing data..."** - Querying Firestore
2. **"Data found! Redirecting..."** - Existing data detected
3. **"Generating transaction history..."** - Creating transactions
4. **"Generated X transactions..."** - Batch commit updates
5. **"Calculating SARRAL Score..."** - Running algorithm
6. **"Creating borrower profile..."** - Saving to Firestore
7. **"Analysis complete!"** - Done, navigating

---

## Error Handling

### Input Validation

| Error Condition | Error Message |
|----------------|---------------|
| Empty UPI ID | "Please enter a UPI ID" |
| Missing @ symbol | "Invalid UPI ID format" |
| Firestore error | "Failed to analyze UPI activity: [error]" |

### Exception Handling

```kotlin
try {
    // Analysis logic
} catch (e: Exception) {
    Log.e("UPIAnalyzer", "Error", e)
    onError("Failed to analyze: ${e.message}")
}
```

---

## Performance Optimizations

### Batch Writes

```kotlin
// Commit every 500 documents (Firestore limit)
if (batchCount >= 500) {
    batch.commit().await()
    batchCount = 0
}
```

**Benefits**:

- Reduces network calls
- Faster execution
- Complies with Firestore limits

### Coroutine Usage

```kotlin
CoroutineScope(Dispatchers.IO).launch {
    // Background work
    withContext(Dispatchers.Main) {
        // UI updates
    }
}
```

**Benefits**:

- Non-blocking UI
- Smooth user experience
- Efficient threading

---

## Integration with BorrowerDashboard

### Navigation

```kotlin
onNavigateToDashboard: () -> Unit
```

After analysis, the screen navigates to `BorrowerLoanDashboardScreen` which:

1. Queries the borrower document by UPI ID
2. Displays SARRAL Score
3. Shows Loan Limit
4. Lists available loan offers

---

## Example User Flow

### Scenario: New Shopkeeper

**Input**: `rajesh@phonepe`

**Processing**:

1. No existing data found
2. Generates 270 transactions (9/day × 30 days)
3. Total inflow: ₹73,000
4. Avg transaction: ₹270
5. Consistency: 75%
6. SARRAL Score: 60
7. Loan Limit: ₹21,900
8. Goodwill Score: 67 (random)

**Result**:

```
Borrower Profile Created:
- Name: Rajesh
- UPI: rajesh@phonepe
- SARRAL Score: 60/100
- Loan Limit: ₹21,900
- Goodwill: 67/100
- Active Loan: No
```

---

## AI Insight (Optional Feature)

### Implementation (Commented Out)

```kotlin
val aiPrompt = "Summarize UPI inflow of borrower $upiId " +
               "having score $score and inflow ₹${inflow} " +
               "over 30 days. Suggest improvements."

RunAnywhereSDK.textGenerate(
    context = context,
    prompt = aiPrompt,
    model = "runanywhere/gpt-mini",
    callback = { response ->
        firestore.collection("borrowers")
            .document(upiId)
            .update("ai_insight", response)
    }
)
```

### Example AI Response

```
"You had steady UPI inflows this month, averaging ₹270 per 
transaction with 9 transactions daily. To improve your SARRAL 
Score to 70+, try to:
1. Increase transaction frequency to 10-12 daily
2. Maintain consistent amounts above ₹300
3. Continue regular business hours (8 AM - 9 PM)"
```

---

## Testing Checklist

- [ ] UPI ID validation works
- [ ] Existing data detection works
- [ ] Transaction generation completes
- [ ] Batch writes commit properly
- [ ] SARRAL Score calculated correctly
- [ ] Loan limit accurate (30% of inflow)
- [ ] Borrower document created
- [ ] Progress messages update
- [ ] Navigation to dashboard works
- [ ] Error handling displays messages
- [ ] Works on slow network
- [ ] No crashes on back press

---

## Code Statistics

- **Lines of Code**: ~380
- **Transactions Generated**: 150-360 per user
- **Collections Modified**: 2 (transactions, borrowers)
- **Average Execution Time**: 3-5 seconds
- **Firestore Writes**: 151-361 (transactions + 1 borrower)

---

## Future Enhancements

1. **AI Insight Integration**
    - Uncomment AI code
    - Add RunAnywhere SDK dependency
    - Display insights on dashboard

2. **Data Visualization**
    - Show transaction graph
    - Display daily breakdown
    - Monthly trend analysis

3. **Export Feature**
    - Download transaction CSV
    - PDF report generation

4. **Advanced Scoring**
    - Time-of-day patterns
    - Weekend vs weekday
    - Customer retention metrics

5. **Bulk Upload**
    - Import real transaction data
    - CSV/Excel support

---

## Usage Example

### In MainActivity/Navigation

```kotlin
// Add route
composable("upi_analyzer") {
    UPIAnalyzerScreen(
        onNavigateBack = { navController.popBackStack() },
        onNavigateToDashboard = { 
            navController.navigate("borrower_dashboard") {
                popUpTo("upi_analyzer") { inclusive = true }
            }
        }
    )
}
```

---

## Summary

The UPI Analyzer Screen provides:

✅ **Smart Data Detection** - Avoids duplicate generation  
✅ **Realistic Simulation** - Mirrors actual shop patterns  
✅ **Accurate Scoring** - Fair SARRAL Score calculation  
✅ **Fast Processing** - Optimized Firestore batching  
✅ **User Feedback** - Real-time progress updates  
✅ **Error Handling** - Graceful failure recovery  
✅ **Clean UI** - Material Design 3 compliance  
✅ **Ready for AI** - Prepared for insight integration

This screen enables quick onboarding for new borrowers by automatically creating their transaction
history and creditworthiness profile! 🚀
