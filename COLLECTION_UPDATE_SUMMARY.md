# Collection and Field Name Update Summary

## Changes Made ✅

Updated the Firestore collection and field names to match your requirements:

### Collection Name Change

- **OLD**: `upi_transactions`
- **NEW**: `transactions`

### Field Name Change

- **OLD**: `upi_id`
- **NEW**: `borrower_upi`

---

## Files Updated

### 1. BorrowerLoanDashboardScreen.kt ✅

**Changes:**

- Changed collection query from `"upi_transactions"` to `"transactions"`
- Changed field filter from `"upi_id"` to `"borrower_upi"`
- Added `.orderBy("timestamp", Query.Direction.DESCENDING)` to the query

**Query Before:**

```kotlin
firestore.collection("upi_transactions")
    .whereEqualTo("upi_id", upiId)
    .whereGreaterThanOrEqualTo("timestamp", startDate)
```

**Query After:**

```kotlin
firestore.collection("transactions")
    .whereEqualTo("borrower_upi", upiId)
    .whereGreaterThanOrEqualTo("timestamp", startDate)
    .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
```

---

### 2. TestDataSeeder.kt ✅

**Changes:**

- Updated `seedTestTransactions()` to use `"transactions"` collection
- Updated `seedCustomTransactions()` to use `"transactions"` collection
- Updated `clearTestTransactions()` to query `"transactions"` collection
- Changed all transaction documents to use `"borrower_upi"` field

**Document Structure Before:**

```kotlin
hashMapOf(
    "upi_id" to upiId,
    "amount" to transactionAmount,
    "timestamp" to Timestamp(calendar.time)
)
```

**Document Structure After:**

```kotlin
hashMapOf(
    "borrower_upi" to upiId,
    "amount" to transactionAmount,
    "timestamp" to Timestamp(calendar.time)
)
```

---

### 3. Documentation Files Updated ✅

- `SARRAL_SCORE_CALCULATION_GUIDE.md`
- `FIREBASE_SETUP_COMPLETE.md`
- `FIX_NEW_FIREBASE_SETUP.md`

All references updated to reflect:

- Collection: `transactions`
- Field: `borrower_upi`

---

## Firestore Structure

### Collection: `transactions`

```
Document ID: auto-generated
Fields:
  - borrower_upi: string (e.g., "test@paytm")
  - amount: number (transaction amount in ₹)
  - timestamp: timestamp (date and time)
  - description: string (optional)
  - type: string (e.g., "credit")
```

**Example Document:**

```json
{
  "borrower_upi": "test@paytm",
  "amount": 2500.50,
  "timestamp": "2024-11-08T10:30:00Z",
  "description": "Test Transaction - Month 3",
  "type": "credit"
}
```

---

## Query Details

### Full Query Implementation

```kotlin
firestore.collection("transactions")
    .whereEqualTo("borrower_upi", upiId)
    .whereGreaterThanOrEqualTo("timestamp", com.google.firebase.Timestamp(startDate))
    .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
    .get()
```

**Query Filters:**

1. `borrower_upi` equals the user's UPI ID
2. `timestamp` is within the last 180 days
3. Results ordered by `timestamp` descending (newest first)

---

## Updated Firestore Security Rules

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // User profiles
    match /user_profiles/{userId} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
    
    // Transactions - updated collection name
    match /transactions/{transactionId} {
      allow read: if request.auth != null && 
                     resource.data.borrower_upi == get(/databases/$(database)/documents/user_profiles/$(request.auth.uid)).data.upi_id;
      allow write: if false; // Transactions should be created by backend only
    }
    
    // Loan requests
    match /loan_requests/{requestId} {
      allow read: if request.auth != null;
      allow create: if request.auth != null && request.resource.data.borrower_uid == request.auth.uid;
      allow update: if false;
    }
  }
}
```

---

## Testing with New Collection

### 1. Using Test Data Seeder (Automatic)

The test data seeder automatically uses the new collection and field names:

```kotlin
// In UPI Input Screen, click "🧪 Seed Test Data"
// Creates documents in "transactions" collection with "borrower_upi" field
```

**What it creates:**

- Collection: `transactions`
- ~24-30 documents
- Each with `borrower_upi` = your entered UPI ID
- Timestamps spread across last 6 months

---

### 2. Manual Testing (Firebase Console)

If adding data manually:

1. Go to Firebase Console → Firestore Database
2. Create collection: `transactions` (NOT `upi_transactions`)
3. Add document with these fields:
   ```
   borrower_upi: "test@paytm"
   amount: 2500
   timestamp: [Select date within last 180 days]
   type: "credit"
   description: "Manual test transaction"
   ```
4. Repeat for multiple months

---

## Migration Notes

### If You Have Existing Data

If you previously created data in `upi_transactions` collection:

**Option 1: Create New Test Data**

- Use the test data seeder with a new UPI ID
- It will create data in the correct `transactions` collection

**Option 2: Manually Migrate (if needed)**

1. Export data from `upi_transactions`
2. Transform field names: `upi_id` → `borrower_upi`
3. Import to `transactions` collection

**Option 3: Delete Old Data**

1. Firebase Console → Firestore Database
2. Delete `upi_transactions` collection
3. Use test data seeder to create fresh data in `transactions`

---

## Verification Steps

After updating, verify everything works:

### Step 1: Clean Build

```bash
./gradlew clean
./gradlew assembleDebug
```

### Step 2: Test Data Seeding

1. Launch app
2. Navigate: Borrow Money → Enter UPI Details
3. Enter UPI ID: `test@paytm`
4. Click "🧪 Seed Test Data"
5. Verify success message

### Step 3: Check Firebase Console

1. Go to Firestore Database
2. Look for `transactions` collection (NOT `upi_transactions`)
3. Click on a document
4. Verify it has `borrower_upi` field (NOT `upi_id`)

### Step 4: Test SARRAL Score

1. Click "Verify UPI"
2. Dashboard should load
3. Should display calculated SARRAL Score
4. Should display Loan Limit

**If score doesn't calculate:**

- Check Logcat for errors
- Verify collection name is `transactions`
- Verify field name is `borrower_upi`
- Verify timestamps are within last 180 days

---

## Firestore Indexes

### Required Composite Index

For the query to work efficiently, Firestore needs a composite index:

**Fields:**

1. `borrower_upi` (Ascending)
2. `timestamp` (Descending)

**Creating the Index:**

**Method 1: Automatic (Recommended)**

- Run the app and trigger the query
- Firestore will show an error with a link to create the index
- Click the link and create the index
- Wait 1-2 minutes for index to build

**Method 2: Manual**

1. Firebase Console → Firestore Database
2. Click **Indexes** tab
3. Click **"Add Index"**
4. Collection: `transactions`
5. Add fields:
    - `borrower_upi`: Ascending
    - `timestamp`: Descending
6. Click **"Create"**

---

## Summary of Changes

| Aspect | Old Value | New Value |
|--------|-----------|-----------|
| Collection Name | `upi_transactions` | `transactions` |
| Field Name | `upi_id` | `borrower_upi` |
| Query Ordering | None | `timestamp DESC` |
| Files Updated | 2 code files | ✅ Updated |
| Documentation | 3 docs | ✅ Updated |
| Security Rules | Old collection | ✅ Updated |

---

## Quick Reference

### Query Pattern

```kotlin
collection("transactions")
    .whereEqualTo("borrower_upi", upiId)
    .whereGreaterThanOrEqualTo("timestamp", startDate)
    .orderBy("timestamp", Query.Direction.DESCENDING)
```

### Document Structure

```kotlin
hashMapOf(
    "borrower_upi" to "test@paytm",
    "amount" to 2500.0,
    "timestamp" to Timestamp.now(),
    "description" to "Transaction description",
    "type" to "credit"
)
```

### Security Rule

```javascript
match /transactions/{transactionId} {
  allow read: if request.auth != null;
  allow write: if request.auth != null;
}
```

---

## Status: ✅ Complete

All code and documentation have been updated to use:

- **Collection**: `transactions`
- **Field**: `borrower_upi`
- **Ordering**: `timestamp` descending

The SARRAL score calculation will now work with the new collection structure!

---

**Last Updated**: Collection structure updated
**Status**: ✅ Ready for testing
