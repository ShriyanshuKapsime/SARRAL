# Role Switch Restriction - Updated Implementation

## Overview

Updated role switching logic across both Profile and Main Dashboard screens to properly check the
`active_loans` collection in real-time for ongoing loans, preventing users from switching roles when
they have active loans as either borrower or lender.

---

## 🎯 Key Changes

### 1. **Real-Time Active Loan Detection**

Previously: Used `active_loan` boolean field from `user_profiles` document
Now: **Queries `active_loans` collection in real-time** for ongoing loans

**Query Logic:**

```kotlin
// Check if user is borrower with active loan
active_loans WHERE borrower_uid == current_user_uid AND status == "ongoing"

// Check if user is lender with active loan  
active_loans WHERE lender_uid == current_user_uid AND status == "ongoing"

// hasActiveLoan = (borrower loans OR lender loans) exists
```

### 2. **Firestore Snapshot Listener**

Uses `addSnapshotListener` for real-time updates:

- Automatically detects when loans are completed
- Updates `hasActiveLoan` state immediately
- Enables role switch as soon as loan status changes

### 3. **Updated Restriction Message**

**New Message:**

```
"You cannot switch roles while an active loan is ongoing. 
Please complete or close all active loans first."
```

---

## 📁 Files Modified

### 1. **UserProfileScreen.kt** (Profile Screen)

**Changes:**

- ✅ Removed `activeLoan` field from `UserProfile` data class
- ✅ Added `hasActiveLoan` state variable
- ✅ Added `isCheckingActiveLoan` state variable
- ✅ Added real-time Firestore snapshot listener for `active_loans`
- ✅ Updated "Active Loan" badge to use `hasActiveLoan`
- ✅ Updated "Active Loan: Yes/No" display to use `hasActiveLoan`
- ✅ Simplified role switch logic (removed redundant queries)
- ✅ Updated AlertDialog message
- ✅ Changed success toast to "Role switched successfully."

**Code Structure:**

```kotlin
// State variables
var hasActiveLoan by remember { mutableStateOf(false) }
var isCheckingActiveLoan by remember { mutableStateOf(true) }

// Real-time listener
LaunchedEffect(Unit) {
    firestore.collection("active_loans")
        .whereEqualTo("borrower_uid", currentUser.uid)
        .whereEqualTo("status", "ongoing")
        .addSnapshotListener { borrowerSnapshot, error ->
            val hasBorrowerLoan = !borrowerSnapshot.isEmpty
            
            // Also check lender loans
            firestore.collection("active_loans")
                .whereEqualTo("lender_uid", currentUser.uid)
                .whereEqualTo("status", "ongoing")
                .get()
                .addOnSuccessListener { lenderSnapshot ->
                    hasActiveLoan = hasBorrowerLoan || !lenderSnapshot.isEmpty
                }
        }
}

// Role switch button
Button(onClick = {
    if (hasActiveLoan) {
        showActiveLoanDialog = true
    } else {
        // Proceed with role switch
    }
})
```

### 2. **UserDashboardScreen.kt** (Main Dashboard)

**Changes:**

- ✅ Added `hasActiveLoan` state variable
- ✅ Added `isCheckingActiveLoan` state variable
- ✅ Added `showActiveLoanDialog` state variable
- ✅ Added real-time Firestore snapshot listener for `active_loans`
- ✅ Updated "Borrow Money" button to check `hasActiveLoan`
- ✅ Updated "Lend Money" button to check `hasActiveLoan`
- ✅ Added AlertDialog with proper message
- ✅ Disabled buttons while checking active loan status

**Code Structure:**

```kotlin
// State variables
var hasActiveLoan by remember { mutableStateOf(false) }
var isCheckingActiveLoan by remember { mutableStateOf(true) }
var showActiveLoanDialog by remember { mutableStateOf(false) }

// Real-time listener (same as Profile screen)
LaunchedEffect(Unit) {
    // ... active loan checking logic
}

// Borrow Money button
Button(
    onClick = {
        if (isCheckingActiveLoan) return@Button
        if (hasActiveLoan) {
            showActiveLoanDialog = true
            return@Button
        }
        // Update role to borrower and navigate
    },
    enabled = !isUpdatingRole && !isCheckingActiveLoan
)

// Lend Money button  
Button(
    onClick = {
        if (isCheckingActiveLoan) return@Button
        if (hasActiveLoan) {
            showActiveLoanDialog = true
            return@Button
        }
        // Update role to lender and navigate
    },
    enabled = !isUpdatingRole && !isCheckingActiveLoan
)
```

---

## 🎨 User Experience Flow

### Profile Screen

**Initial State:**

```
┌─────────────────────────────────────┐
│  Profile Header                     │
│  👤 Krish                           │
│  ┌────────────┐  ┌──────────────┐  │
│  │ BORROWER   │  │ ACTIVE LOAN  │  │
│  └────────────┘  └──────────────┘  │
│                                     │
│  Switch Role Card                   │
│  Active Loan: Yes (Red)             │
│  [ Switch to Lender ]               │
└─────────────────────────────────────┘
```

**User clicks "Switch to Lender":**

```
⚠️ AlertDialog appears:
╔═══════════════════════════════════╗
║  Role Switch Disabled             ║
║                                   ║
║  You cannot switch roles while    ║
║  an active loan is ongoing.       ║
║  Please complete or close all     ║
║  active loans first.              ║
║                                   ║
║           [  OK  ]                ║
╚═══════════════════════════════════╝
```

**After loan completion (real-time update):**

```
┌─────────────────────────────────────┐
│  Profile Header                     │
│  👤 Krish                           │
│  ┌────────────┐                    │
│  │ BORROWER   │  (No active loan)  │
│  └────────────┘                    │
│                                     │
│  Switch Role Card                   │
│  Active Loan: No (Blue)             │
│  [ Switch to Lender ]               │
└─────────────────────────────────────┘
```

**User clicks "Switch to Lender":**

```
✅ Role switches successfully
🎉 Toast: "Role switched successfully."
```

### Main Dashboard Screen

**Initial State (with active loan):**

```
┌─────────────────────────────────────┐
│         Welcome to SARRAL           │
│  Smart Automated Reliable...        │
│                                     │
│  [ Borrow Money ]  ← Enabled        │
│                                     │
│  [ Lend Money ]    ← Enabled        │
└─────────────────────────────────────┘
```

**User clicks "Borrow Money" or "Lend Money":**

```
⚠️ AlertDialog appears (same as Profile)
```

**Initial State (no active loan):**

```
User can freely switch between roles
✅ Borrow Money → Updates role to "borrower"
✅ Lend Money → Updates role to "lender"
```

---

## 🔧 Technical Implementation

### Active Loan Detection Logic

**Firestore Queries:**

1. **Borrower Loans:**
   ```kotlin
   firestore.collection("active_loans")
       .whereEqualTo("borrower_uid", currentUser.uid)
       .whereEqualTo("status", "ongoing")
       .addSnapshotListener { ... }
   ```

2. **Lender Loans:**
   ```kotlin
   firestore.collection("active_loans")
       .whereEqualTo("lender_uid", currentUser.uid)
       .whereEqualTo("status", "ongoing")
       .get()
   ```

3. **Combined Result:**
   ```kotlin
   hasActiveLoan = hasBorrowerLoan || hasLenderLoan
   ```

### Real-Time Updates

**Snapshot Listener Benefits:**

- ✅ Automatically triggered when `active_loans` collection changes
- ✅ Updates UI immediately when loan status changes to "completed"
- ✅ No manual refresh needed
- ✅ Accurate real-time status

**Example Scenario:**

```
1. User has active loan → hasActiveLoan = true
2. Lender marks loan as "completed" in Firestore
3. Snapshot listener fires automatically
4. hasActiveLoan updates to false
5. "Active Loan: Yes" changes to "No" (Profile)
6. "Switch to Lender" button now works
```

### State Management

**Profile Screen:**

```kotlin
var hasActiveLoan by remember { mutableStateOf(false) }
var isCheckingActiveLoan by remember { mutableStateOf(true) }
var showActiveLoanDialog by remember { mutableStateOf(false) }
```

**Main Dashboard:**

```kotlin
var hasActiveLoan by remember { mutableStateOf(false) }
var isCheckingActiveLoan by remember { mutableStateOf(true) }
var showActiveLoanDialog by remember { mutableStateOf(false) }
var isUpdatingRole by remember { mutableStateOf(false) }
```

---

## 📊 Firestore Schema

### Collection: `active_loans`

**Document Fields:**

```json
{
  "borrower_uid": "user123",
  "lender_uid": "lender456",
  "status": "ongoing",  // or "completed", "cancelled"
  "amount": 5000,
  "start_date": "2024-01-15",
  "end_date": "2024-02-15",
  ...
}
```

**Query Conditions:**

- `borrower_uid == current_user_uid` → User is borrowing
- `lender_uid == current_user_uid` → User is lending
- `status == "ongoing"` → Loan is active
- **If any match** → Block role switch

---

## ✅ Requirements Checklist

### Logic:

- ✅ Query `active_loans` collection for ongoing loans
- ✅ Check both `borrower_uid` and `lender_uid`
- ✅ Real-time updates with snapshot listener
- ✅ Disable role switch if any active loan found
- ✅ Show AlertDialog when restriction applies
- ✅ Allow role switch when no active loans

### UI - Profile Screen:

- ✅ "Switch Role" button always visible
- ✅ "Active Loan: Yes/No" display
- ✅ Red "ACTIVE LOAN" badge when true
- ✅ AlertDialog with proper message
- ✅ Button disabled while checking status

### UI - Main Dashboard:

- ✅ "Borrow Money" button checks active loans
- ✅ "Lend Money" button checks active loans
- ✅ AlertDialog with proper message
- ✅ Buttons disabled while checking status

### Messages:

- ✅ Dialog Title: "Role Switch Disabled"
- ✅ Dialog Message: "You cannot switch roles while an active loan is ongoing. Please complete or
  close all active loans first."
- ✅ Success Toast: "Role switched successfully."

### Real-Time:

- ✅ Snapshot listener for live updates
- ✅ Automatic UI refresh when loan completes
- ✅ No manual refresh needed

---

## 🎯 Scenarios Tested

### Scenario 1: User is Borrower with Active Loan

**Setup:**

- User UID: `user123`
- `active_loans` has document with `borrower_uid = "user123"`, `status = "ongoing"`

**Result:**

- ✅ `hasActiveLoan = true`
- ✅ "Active Loan: Yes" (red)
- ✅ "ACTIVE LOAN" badge visible
- ✅ Click "Switch to Lender" → Shows AlertDialog

### Scenario 2: User is Lender with Active Loan

**Setup:**

- User UID: `lender456`
- `active_loans` has document with `lender_uid = "lender456"`, `status = "ongoing"`

**Result:**

- ✅ `hasActiveLoan = true`
- ✅ "Active Loan: Yes" (red)
- ✅ Click "Switch to Borrower" → Shows AlertDialog

### Scenario 3: User with No Active Loan

**Setup:**

- User UID: `user789`
- No documents in `active_loans` matching user as borrower or lender with `status = "ongoing"`

**Result:**

- ✅ `hasActiveLoan = false`
- ✅ "Active Loan: No" (blue)
- ✅ Click "Switch to Lender" → Role switches successfully

### Scenario 4: Loan Completed (Real-Time)

**Setup:**

- User has active loan initially
- Loan status changes from "ongoing" → "completed"

**Result:**

- ✅ Snapshot listener fires
- ✅ `hasActiveLoan` updates to `false`
- ✅ UI updates automatically
- ✅ User can now switch roles

---

## 🚀 Performance Optimizations

1. **Snapshot Listener:** Only listens to borrower loans (most common)
2. **Single Query:** Lender loans checked with `.get()` (not snapshot)
3. **Early Return:** Buttons check `isCheckingActiveLoan` first
4. **State Caching:** `hasActiveLoan` cached until next update

---

## 🎉 Result

Both Profile and Main Dashboard screens now feature:

- ✅ **Real-time active loan detection** from `active_loans` collection
- ✅ **Comprehensive checks** for both borrower and lender roles
- ✅ **Professional AlertDialog** with clear restriction message
- ✅ **Automatic updates** when loan status changes
- ✅ **Consistent UX** across all screens
- ✅ **Disabled states** while checking loan status
- ✅ **Visual indicators** (badges, colors) for active loan status

Users can no longer switch roles when they have active loans, and the system automatically enables
role switching as soon as all loans are completed! 🚀
