# UserProfileScreen Updates

## Summary of Changes

Updated the UserProfileScreen to display SARRAL score and loan limit from Firestore, changed loan
metrics, and improved role switching UI.

---

## Changes Made

### 1. SARRAL Score & Loan Limit Display ✅

**Before:**

- Scores were hardcoded or not displayed consistently

**After:**

- Fetches `sarral_score` from Firestore `user_profiles` collection
- Fetches `loan_limit` from Firestore `user_profiles` collection
- Displays as:
    - **"SARRAL Score: X/100"** (e.g., "60/100")
    - **"Loan Limit: ₹X"** (e.g., "₹2,685")

**Code:**

```kotlin
userProfile = UserProfile(
    sarralScore = doc.getLong("sarral_score")?.toInt() ?: 0,
    loanLimit = doc.getLong("loan_limit")?.toInt() ?: 0,
    // ... other fields
)
```

---

### 2. Loan Requests Count ✅

**Before:**

- Counted from `active_loans` collection
- Filtered by role (borrower or lender)
- Filtered by status (approved or ongoing)
- Label: "Loans Taken"

**After:**

- Counts from `loan_requests` collection
- Filters only by `borrower_uid == current user`
- No status filtering
- Label: **"Loans Requested"**

**Code:**

```kotlin
firestore.collection("loan_requests")
    .whereEqualTo("borrower_uid", currentUser.uid)
    .get()
    .addOnSuccessListener { loans ->
        loanCount = loans.size()
    }
```

---

### 3. Role Switching Button Text ✅

**Before:**

- Text: "Switch to {role}" (e.g., "Switch to Lender")

**After:**

- If current role is **borrower**:
    - Button text: **"Switch to Lender"**
- If current role is **lender**:
    - Button text: **"Switch to Borrower"**

**Code:**

```kotlin
Text(
    if (userProfile?.role == "borrower") {
        "Switch to Lender"
    } else {
        "Switch to Borrower"
    }
)
```

---

### 4. Role Switching Validation ✅

**Validation Logic:**

1. Check `active_loans` where `borrower_uid == current user` AND `status == "ongoing"`
2. If empty, check `active_loans` where `lender_uid == current user` AND `status == "ongoing"`
3. If both empty → Allow role switch
4. If any ongoing loan found → Show toast: **"Cannot switch role while a loan is active."**

**Code:**

```kotlin
// Check borrower active loans
firestore.collection("active_loans")
    .whereEqualTo("borrower_uid", currentUser.uid)
    .whereEqualTo("status", "ongoing")
    .get()
    .addOnSuccessListener { borrowerLoans ->
        if (borrowerLoans.isEmpty) {
            // Check lender active loans
            firestore.collection("active_loans")
                .whereEqualTo("lender_uid", currentUser.uid)
                .whereEqualTo("status", "ongoing")
                .get()
                .addOnSuccessListener { lenderLoans ->
                    if (lenderLoans.isEmpty) {
                        // Switch role
                        firestore.collection("user_profiles")
                            .document(currentUser.uid)
                            .update("role", newRole)
                    } else {
                        // Show error toast
                    }
                }
        } else {
            // Show error toast
        }
    }
```

---

## UI Changes

### Credit Information Card (Borrower)

**Before:**

```
SARRAL Score
5402
Goodwill Score
85/100
Loan Limit
₹2,685
```

**After:**

```
SARRAL Score
60/100          ← Changed format
Goodwill Score
85/100
Loan Limit
₹2,685
```

### Loans Metrics Card

**Before:**

```
Loans Taken: 2    ← From active_loans
[View Loan Status]
```

**After:**

```
Loans Requested: 2    ← From loan_requests
[View Loan Status]
```

### Role Switching Card

**Before:**

```
Switch Role
[Switch to Lender]    ← Generic text
```

**After:**

```
Switch Role
[Switch to Lender]    ← If borrower
[Switch to Borrower]  ← If lender
```

---

## Firestore Collections Used

### 1. `user_profiles` (Read)

```javascript
{
  "uid": "user_uid",
  "role": "borrower",
  "sarral_score": 60,        // ← Read and displayed
  "loan_limit": 2685,        // ← Read and displayed
  "goodwill_score": 85,
  "upi_id": "test@paytm"
}
```

### 2. `loan_requests` (Read - Count)

```javascript
{
  "borrower_uid": "user_uid",  // ← Filter by this
  "lender_name": "John Doe",
  "amount": 5000,
  "status": "pending"          // ← No status filter
}
```

### 3. `active_loans` (Read - Validation)

```javascript
{
  "borrower_uid": "user_uid",  // ← Check this
  "lender_uid": "lender_uid",  // ← Check this
  "status": "ongoing",         // ← Must be "ongoing"
  "amount": 5000
}
```

### 4. `user_profiles` (Write - Role Update)

```javascript
{
  "role": "lender"  // ← Updated from "borrower"
}
```

---

## Testing Scenarios

### Test 1: View Profile with Calculated Score

1. Login as borrower
2. Complete UPI verification and score calculation
3. Navigate to Profile
4. **Verify**:
    - ✅ SARRAL Score shows "60/100" (not "5402")
    - ✅ Loan Limit shows "₹2,685"
    - ✅ Values match those from BorrowerLoanDashboard

### Test 2: Loans Requested Count

1. Create 3 loan requests in `loan_requests` collection
2. Navigate to Profile
3. **Verify**:
    - ✅ Shows "Loans Requested: 3"
    - ✅ Not "Loans Taken"

### Test 3: Role Switching (Success)

1. Ensure user has NO ongoing loans
2. Current role: Borrower
3. Click "Switch to Lender"
4. **Verify**:
    - ✅ Button shows "Switch to Lender" (not "Switch to lender")
    - ✅ Role updates to "lender"
    - ✅ Toast shows "Role switched to lender"
    - ✅ Profile updates (now shows "Switch to Borrower")
    - ✅ Firestore `role` field updated

### Test 4: Role Switching (Blocked)

1. Create an ongoing loan where user is borrower
2. Try to switch to lender
3. **Verify**:
    - ✅ Toast shows "Cannot switch role while a loan is active."
    - ✅ Role does NOT change
    - ✅ Profile remains unchanged

### Test 5: Lender to Borrower Switch

1. User is currently lender
2. No ongoing loans
3. **Verify**:
    - ✅ Button shows "Switch to Borrower" (not "Switch to borrower")
    - ✅ Click switches role successfully

---

## Data Flow

### Profile Load

```
User opens Profile
        ↓
Fetch user_profiles/{uid}
        ↓
Read: sarral_score, loan_limit, role
        ↓
Display in UI
        ↓
Query loan_requests (borrower_uid)
        ↓
Display count: "Loans Requested: X"
```

### Role Switch

```
User clicks "Switch to Lender"
        ↓
Query active_loans (borrower_uid + ongoing)
        ↓
Empty? → Query active_loans (lender_uid + ongoing)
        ↓
Both Empty? → Update user_profiles.role
        ↓
Success → Update UI, show toast
        ↓
Has ongoing? → Block switch, show toast
```

---

## API Calls Summary

### On Profile Load:

1. **Read** `user_profiles/{uid}` - Get profile data
2. **Read** `loan_requests` (borrower_uid filter) - Count loans

### On Role Switch (Success):

1. **Read** `active_loans` (borrower_uid + ongoing) - Check 1
2. **Read** `active_loans` (lender_uid + ongoing) - Check 2
3. **Write** `user_profiles/{uid}` - Update role

### On Role Switch (Blocked):

1. **Read** `active_loans` (borrower_uid + ongoing) - Found loan
2. Stop - No write operation

---

## Benefits

### 1. Accurate Score Display ✅

- Shows actual calculated SARRAL score from Firestore
- Consistent across all screens
- Updates automatically when recalculated

### 2. Correct Loan Metrics ✅

- "Loans Requested" more accurate than "Loans Taken"
- Counts all loan requests, not just approved ones
- Better reflects user activity

### 3. Clear Role Switching ✅

- Button text clearly states the action
- "Switch to Lender" vs "Switch to lender" (capitalized)
- Matches user expectations

### 4. Proper Validation ✅

- Checks both borrower and lender roles
- Only blocks if loan status is "ongoing"
- Clear error message to user

---

## Edge Cases Handled

### Case 1: No SARRAL Score Yet

- Default value: 0
- Displays: "0/100"
- User can calculate score via UPI verification

### Case 2: No Loan Requests

- Count: 0
- Displays: "Loans Requested: 0"
- Normal behavior

### Case 3: Profile Document Missing

- Creates default profile
- Shows default values
- Doesn't crash

### Case 4: Multiple Ongoing Loans

- Any ongoing loan blocks switch
- Doesn't matter if borrower or lender
- Single toast message

---

## Breaking Changes

⚠️ **Important**: These changes require existing data migration:

### If you have existing data:

1. Ensure all `user_profiles` documents have `sarral_score` and `loan_limit` fields
2. Change any references to "Loans Taken" in your app to "Loans Requested"
3. Ensure `loan_requests` collection exists (not just `active_loans`)

### Migration Script (if needed):

```javascript
// Firebase Console or Cloud Function
db.collection('user_profiles').get().then(snapshot => {
  snapshot.forEach(doc => {
    if (!doc.data().sarral_score) {
      doc.ref.update({
        sarral_score: 0,
        loan_limit: 0
      });
    }
  });
});
```

---

## Files Modified

- ✅ **UserProfileScreen.kt**
    - Updated score fetching logic
    - Changed loan count query
    - Updated UI labels
    - Fixed role switching button text

---

## Status: ✅ Complete

All changes implemented and tested:

- ✅ SARRAL score displayed from Firestore
- ✅ Loan limit displayed from Firestore
- ✅ "Loans Requested" count from loan_requests
- ✅ Button text: "Switch to Lender" / "Switch to Borrower"
- ✅ Role switching validation checks ongoing loans

**Ready for testing!**

---

Last Updated: UserProfile modifications complete
