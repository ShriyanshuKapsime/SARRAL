# SARRAL Score Calculation Implementation Guide

## Overview

The SARRAL (Smart Automated Reliable, Repayment and Lending) score is calculated based on the user's
UPI transaction history over the last 180 days.

## Implementation Summary

### Files Modified

1. **UPIInputScreen.kt** - Added Firebase integration to save UPI ID
2. **BorrowerLoanDashboardScreen.kt** - Implemented SARRAL score calculation algorithm

## How It Works

### Step 1: User Enters UPI ID

- User enters their UPI ID in the `UPIInputScreen`
- UPI ID is validated and saved to Firestore in the `user_profiles` collection
- Document structure:
  ```json
  {
    "upi_id": "user@paytm",
    "user_uid": "firebase_user_uid",
    "updated_at": "timestamp"
  }
  ```

### Step 2: Fetch UPI Transactions

When the user navigates to the Borrower Loan Dashboard:

1. Fetch the user's UPI ID from `user_profiles` collection
2. Query `transactions` collection where:
    - `borrower_upi` == user's UPI ID
    - `timestamp` >= (current date - 180 days)

### Step 3: Calculate Monthly Totals

- Group transactions by calendar month (YYYY-MM format)
- Sum transaction amounts for each month
- Results in up to 6 monthly totals

### Step 4: Calculate Metrics

#### Monthly Inflow

```
monthly_inflow = sum(all_monthly_totals) / 6
```

#### Income Score

```
income_score = (monthly_inflow / 60000) * 100

If income_score > 100:
    income_score = 100
```

**Note**: Income is benchmarked against ₹60,000/month. A user earning ₹60,000 or more gets 100
income score.

#### Consistency Score

```
max_month = maximum of monthly_totals
min_month = minimum of monthly_totals
consistency_score = 100 - ((max_month - min_month) / max_month * 100)

If consistency_score < 0:
    consistency_score = 0
If consistency_score > 100:
    consistency_score = 100
```

**Note**: Perfect consistency (same amount every month) = 100 score.

#### SARRAL Score

```
sarral_score = (income_score * 0.3) + (consistency_score * 0.7)

Round to nearest integer
```

**Weightage**:

- Income Score: 30%
- Consistency Score: 70%

#### Loan Limit

```
loan_limit = monthly_inflow * 0.30

Round to nearest rupee
```

**Note**: Loan limit is 30% of monthly income, regardless of score.

## Firestore Collections

### user_profiles

```
Document ID: {user_uid}
Fields:
  - upi_id: string
  - user_uid: string
  - updated_at: timestamp
```

### transactions

```
Document ID: auto-generated
Fields:
  - borrower_upi: string (e.g., "user@paytm")
  - amount: number (transaction amount in rupees)
  - timestamp: timestamp (transaction date and time)
```

## Testing with Dummy Data

To test the SARRAL score calculation, you need to add dummy transactions to Firestore.

### Option 1: Using Firebase Console

1. Go to Firebase Console → Firestore Database
2. Create collection `transactions`
3. Add documents with the following fields:
    - `borrower_upi`: your test UPI ID (e.g., "test@paytm")
    - `amount`: numeric value (e.g., 5000, 8000, 12000)
    - `timestamp`: timestamp (within last 180 days)

### Option 2: Using Android Code

Add this test data seeding function (for development only):

```kotlin
fun seedTestTransactions(upiId: String) {
    val firestore = FirebaseFirestore.getInstance()
    val calendar = Calendar.getInstance()
    
    // Generate 6 months of transaction data
    val monthlyAmounts = listOf(
        8000.0,  // Month 1
        9500.0,  // Month 2
        8200.0,  // Month 3
        10000.0, // Month 4
        8800.0,  // Month 5
        9200.0   // Month 6
    )
    
    monthlyAmounts.forEachIndexed { index, amount ->
        // Create 4-5 transactions per month
        val transactionsPerMonth = (4..5).random()
        
        repeat(transactionsPerMonth) {
            calendar.add(Calendar.DAY_OF_YEAR, -(180 - (index * 30) - (it * 7)))
            
            val transaction = hashMapOf(
                "borrower_upi" to upiId,
                "amount" to (amount / transactionsPerMonth),
                "timestamp" to Timestamp(calendar.time)
            )
            
            firestore.collection("transactions").add(transaction)
        }
    }
}
```

### Example Test Data

For UPI ID: `test@paytm`

#### Month 1 (6 months ago): ₹8,000

- Transaction 1: ₹2,000
- Transaction 2: ₹2,500
- Transaction 3: ₹1,800
- Transaction 4: ₹1,700

#### Month 2 (5 months ago): ₹9,500

- Transaction 1: ₹2,300
- Transaction 2: ₹2,400
- Transaction 3: ₹2,600
- Transaction 4: ₹2,200

#### Month 3 (4 months ago): ₹8,200

- Transaction 1: ₹2,100
- Transaction 2: ₹2,000
- Transaction 3: ₹2,050
- Transaction 4: ₹2,050

#### Month 4 (3 months ago): ₹10,000

- Transaction 1: ₹2,500
- Transaction 2: ₹2,500
- Transaction 3: ₹2,500
- Transaction 4: ₹2,500

#### Month 5 (2 months ago): ₹8,800

- Transaction 1: ₹2,200
- Transaction 2: ₹2,200
- Transaction 3: ₹2,200
- Transaction 4: ₹2,200

#### Month 6 (1 month ago): ₹9,200

- Transaction 1: ₹2,300
- Transaction 2: ₹2,300
- Transaction 3: ₹2,300
- Transaction 4: ₹2,300

### Expected Results

With the above test data:

- **Sum of monthly totals**: ₹53,700
- **Monthly inflow**: ₹53,700 / 6 = ₹8,950
- **Income score**: (8,950 / 60,000) * 100 = 14.92
- **Max month**: ₹10,000
- **Min month**: ₹8,000
- **Consistency score**: 100 - ((10,000 - 8,000) / 10,000 * 100) = 100 - 20 = 80
- **SARRAL Score**: (14.92 * 0.3) + (80 * 0.7) = 4.48 + 56 = **60/100** (rounded)
- **Loan Limit**: 8,950 * 0.30 = **₹2,685**

### Score Interpretation

| SARRAL Score | Interpretation                              |
|--------------|---------------------------------------------|
| 80-100       | Excellent - High income, very consistent    |
| 60-79        | Good - Decent income or good consistency    |
| 40-59        | Fair - Moderate income and/or consistency   |
| 20-39        | Poor - Low income or inconsistent           |
| 0-19         | Very Poor - Very low or inconsistent income |

**Examples**:

1. **High Income, Perfect Consistency**: ₹60,000/month, same each month
    - Income Score: 100, Consistency Score: 100
    - SARRAL Score: (100 * 0.3) + (100 * 0.7) = **100/100**

2. **Medium Income, Good Consistency**: ₹30,000/month, ±10% variation
    - Income Score: 50, Consistency Score: 90
    - SARRAL Score: (50 * 0.3) + (90 * 0.7) = **78/100**

3. **Low Income, Poor Consistency**: ₹12,000/month, varies ₹5,000-₹20,000
    - Income Score: 20, Consistency Score: 75
    - SARRAL Score: (20 * 0.3) + (75 * 0.7) = **59/100**

## UI Display

The dashboard displays:

- **SARRAL Score**: Large centered number (e.g., "60/100")
- **Loan Limit Available**: Formatted with rupee symbol (e.g., "₹2,685")

## Error Handling

The implementation handles the following scenarios:

1. **User not authenticated**: Shows error message
2. **UPI ID not found**: Prompts user to enter UPI details first
3. **No transactions found**: Shows error message indicating no transaction history
4. **Firebase errors**: Displays specific error messages

## Testing Steps

1. **Build and run the app**
2. **Login** with your test account
3. **Navigate to Borrow Flow**
4. **Enter UPI ID**: Use "test@paytm" (or your seeded UPI ID)
5. **View Dashboard**: Should show calculated SARRAL score and loan limit

## Production Considerations

For production deployment:

1. **Security Rules**: Add Firestore security rules to protect user data
2. **Real UPI Integration**: Replace dummy data with actual UPI API integration
3. **Transaction Verification**: Implement UPI transaction verification
4. **Data Privacy**: Ensure compliance with data protection regulations
5. **Caching**: Cache calculated scores to reduce Firestore reads
6. **Background Updates**: Periodically refresh transaction data

## Firebase Security Rules

Add these rules to protect your collections:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // User profiles - users can only read/write their own profile
    match /user_profiles/{userId} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
    
    // Transactions - read only by transaction owner
    match /transactions/{transactionId} {
      allow read: if request.auth != null && 
                     resource.data.borrower_upi == get(/databases/$(database)/documents/user_profiles/$(request.auth.uid)).data.upi_id;
      allow write: if false; // Transactions should be created by backend only
    }
    
    // Loan requests
    match /loan_requests/{requestId} {
      allow read: if request.auth != null;
      allow create: if request.auth != null && request.resource.data.borrower_uid == request.auth.uid;
      allow update: if false; // Updates only through backend
    }
  }
}
```

## Next Steps

1. Seed test transaction data in Firestore
2. Test the SARRAL score calculation with different scenarios
3. Implement UPI API integration for real transaction data
4. Add score caching mechanism
5. Implement periodic score recalculation
6. Add analytics to track score distributions

---

**Status**: ✅ Implementation Complete

The SARRAL score calculation is fully functional and ready for testing with dummy data.
