# Borrow Flow - UPI Implementation

## Summary

I've successfully updated the BorrowFlowStart screen and created the UPIInputScreen for collecting
user UPI details to calculate their SARRAL Score and loan limit.

## What Was Updated/Created

### 1. **Updated BorrowFlowStartScreen.kt**

The borrow flow start screen now features:

- **Title**: "Borrow Money" (32sp, Bold)
- **Description Card**: Explains the UPI verification process
    - "Verify your UPI Incoming Payments to calculate your SARRAL Score and loan limit."
    - Displayed in a highlighted card with secondary container color
- **Large Button**: "Enter UPI Details" (80dp height, 24dp rounded corners)
- **Navigation**: Clicking the button navigates to UPIInputScreen

**Design Features**:

- Professional layout with proper spacing
- Information card with colored background for emphasis
- Large, accessible button (80dp height)
- Centered content for better UX

### 2. **Created UPIInputScreen.kt** (New)

A comprehensive UPI ID input screen with:

- **Title**: "Enter Your UPI ID"
- **Description**: Explains what will happen with the UPI data
- **UPI ID Input Field**:
    - Auto-converts to lowercase
    - Placeholder: "example@paytm"
    - Email keyboard type
    - Validation on submit
- **Info Card**: Tips about UPI ID format
    - Examples: john@paytm, user@oksbi
- **Verify Button**: Large button (64dp height)
- **Privacy Note**: "🔒 Your data is secure and encrypted"

**Validation Features**:

- Checks if UPI ID is blank
- Validates @ symbol presence
- Checks for multiple @ symbols (invalid format)
- Shows error messages below the input field
- Real-time error clearing on input change

### 3. **Updated MainActivity.kt**

Added navigation:

- `"upiInput"` route → UPIInputScreen
- BorrowFlowStart now has `onNavigateToUPIInput` callback
- Proper back navigation implemented

## Navigation Flow

```
UserDashboard
    ↓
Borrow Money
    ↓
BorrowFlowStart
    ├─ "Enter UPI Details" →
    ↓
UPIInputScreen
    ├─ "Verify UPI" → (TODO: Navigate to verification/score screen)
    └─ Back → Returns to BorrowFlowStart
```

## Files Modified

1. `app/src/main/java/com/runanywhere/startup_hackathon20/BorrowFlowStartScreen.kt` - Updated with
   UPI description and button
2. `app/src/main/java/com/runanywhere/startup_hackathon20/MainActivity.kt` - Added UPI screen
   navigation

## Files Created

1. `app/src/main/java/com/runanywhere/startup_hackathon20/UPIInputScreen.kt` - New UPI input screen
   with validation

## UI Specifications

### BorrowFlowStart Screen

- **Title Font**: HeadlineLarge, Bold, 32sp
- **Description Card**:
    - Background: SecondaryContainer color
    - Padding: 20dp
    - Text: BodyLarge, 16sp, 24sp line height
    - Centered text
- **Button**:
    - Height: 80dp
    - Shape: 24dp rounded corners
    - Text: "Enter UPI Details", TitleLarge, SemiBold, 22sp

### UPI Input Screen

- **Title Font**: HeadlineLarge, Bold, 28sp
- **Input Field**:
    - Full width
    - Outlined style
    - Supporting text with format hint
    - Error state handling
- **Info Card**:
    - Background: TertiaryContainer color
    - Emoji icon: 💡
    - Helpful tips about UPI ID format
- **Button**:
    - Height: 64dp
    - Shape: 16dp rounded corners
    - Text: "Verify UPI", TitleLarge, SemiBold, 20sp
- **Privacy Note**:
    - Small text with lock emoji: 🔒
    - Reassures users about data security

## Validation Rules

The UPI input screen validates:

1. **Not Empty**: UPI ID must be entered
2. **Contains @**: Must have @ symbol (e.g., user@bank)
3. **Single @**: Only one @ symbol allowed
4. **Lowercase**: Automatically converts input to lowercase

**Error Messages**:

- "Please enter your UPI ID" - when blank
- "Invalid UPI ID format. Must contain @" - when @ is missing
- "Invalid UPI ID format" - when multiple @ symbols

## How to Test

1. **Build and run the app**
2. **Login** and navigate to Dashboard
3. **Click "Borrow Money"**
4. You'll see:
    - "Borrow Money" title
    - Description card explaining UPI verification
    - "Enter UPI Details" button
5. **Click "Enter UPI Details"**
6. You'll see the UPI input screen
7. **Try entering UPI IDs**:
    - Leave blank and click Verify → Error: "Please enter your UPI ID"
    - Enter "test" → Error: "Invalid UPI ID format. Must contain @"
    - Enter "test@@bank" → Error: "Invalid UPI ID format"
    - Enter "test@paytm" → Valid! (Currently goes back, ready for next screen)
8. **Click back arrow** → Returns to previous screen

## Next Steps

The UPI input is ready. Next, you should:

1. **Create UPI Verification Screen**:
    - Show loading state while verifying
    - Display transaction history
    - Calculate SARRAL Score

2. **Create SARRAL Score Display Screen**:
    - Show calculated score
    - Display loan limit
    - Show recommended loan amount
    - Add "Continue to Loan Details" button

3. **Implement Backend**:
    - UPI verification API
    - Transaction analysis
    - Score calculation algorithm
    - Loan limit determination

4. **Handle onSubmitUPI**:
    - Currently just goes back
    - Update to navigate to verification screen
    - Pass UPI ID to next screen

## Code Quality

- ✅ Proper input validation
- ✅ User-friendly error messages
- ✅ Real-time error clearing
- ✅ Accessibility features (keyboard management)
- ✅ Material 3 design guidelines
- ✅ Proper state management
- ✅ Clean separation of concerns
- ✅ Helpful UI hints and tips

## Design Highlights

- **User-Friendly**: Clear instructions and helpful tips
- **Professional**: Clean, modern Material 3 design
- **Secure Messaging**: Privacy note reassures users
- **Validation**: Real-time feedback on input
- **Accessibility**: Large touch targets, proper keyboard handling
- **Visual Hierarchy**: Important information highlighted in cards

---

**Status**: ✅ Implementation Complete

The UPI input flow is fully functional and ready for integration with backend services. The UI
provides a professional, user-friendly experience for collecting UPI details.
