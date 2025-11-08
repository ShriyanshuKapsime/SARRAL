# Quick Start: Testing SARRAL Score Feature

## 🚀 Fast Track Testing (5 Minutes)

### Step 1: Build the App

```bash
./gradlew assembleDebug
```

### Step 2: Run on Device/Emulator

1. Install and launch the app
2. Login with your test account (or create one)

### Step 3: Test SARRAL Score

#### Option A: Using Built-in Test Data Seeder (Recommended)

1. **Navigate**: Dashboard → "Borrow Money" → "Enter UPI Details"
2. **Enter UPI ID**: Type `test@paytm`
3. **Seed Data**: Click "🧪 Seed Test Data (Dev Only)"
4. **Wait**: Until you see "✅ Test data seeded successfully!"
5. **Verify**: Click "Verify UPI" button
6. **View Score**: Automatically navigates to dashboard with calculated score

**Expected Result:**

- SARRAL Score: ~5,370
- Loan Limit: ~₹2,685

#### Option B: Using Firebase Console

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Select your project
3. Navigate to Firestore Database
4. Create collection: `upi_transactions`
5. Add documents manually (see template below)

---

## 📊 Test Data Template (Firebase Console)

### Document 1

```
upi_id: "test@paytm"
amount: 2000
timestamp: [Today - 30 days]
```

### Document 2

```
upi_id: "test@paytm"
amount: 2500
timestamp: [Today - 60 days]
```

### Document 3-20

Repeat with varying amounts and dates spread across 6 months.

---

## 🧪 Test Scenarios

### Scenario 1: High Score (Consistent High Income)

```kotlin
// In your test code or Firebase Console
val monthlyAmounts = listOf(15000.0, 15500.0, 15200.0, 15300.0, 15100.0, 15400.0)
TestDataSeeder.seedCustomTransactions("highscore@paytm", monthlyAmounts)
```

**Expected**: Score ~9,300, Limit ~₹4,590

### Scenario 2: Low Score (Inconsistent Income)

```kotlin
val monthlyAmounts = listOf(5000.0, 12000.0, 3000.0, 15000.0, 4000.0, 10000.0)
TestDataSeeder.seedCustomTransactions("lowscore@paytm", monthlyAmounts)
```

**Expected**: Score ~3,800, Limit ~₹2,450

### Scenario 3: Growing Income

```kotlin
val monthlyAmounts = listOf(5000.0, 6000.0, 7500.0, 9000.0, 10500.0, 12000.0)
TestDataSeeder.seedCustomTransactions("growing@paytm", monthlyAmounts)
```

**Expected**: Score ~5,600, Limit ~₹2,500

---

## ✅ Verification Checklist

- [ ] App builds without errors
- [ ] Can login/signup successfully
- [ ] Can navigate to "Borrow Money"
- [ ] Can enter UPI ID
- [ ] Test data seeder works
- [ ] UPI ID saves to Firestore
- [ ] Dashboard loads with loading indicator
- [ ] SARRAL Score displays correctly
- [ ] Loan Limit displays correctly
- [ ] Error messages show when no data
- [ ] Can navigate back without crashes

---

## 🐛 Troubleshooting

### Issue: "No transaction history found"

**Solution**: Make sure test data was seeded with correct UPI ID

### Issue: "UPI ID not found"

**Solution**: Click "Verify UPI" button after entering UPI ID

### Issue: Loading forever

**Solution**: Check Firebase Console logs, ensure internet connection

### Issue: Build errors

**Solution**:

```bash
./gradlew clean
./gradlew build
```

### Issue: Firebase not configured

**Solution**: Ensure `google-services.json` is in `app/` directory

---

## 📱 User Flow Summary

```
Login
  ↓
Dashboard
  ↓
"Borrow Money"
  ↓
"Enter UPI Details"
  ↓
Enter: "test@paytm"
  ↓
Click: "🧪 Seed Test Data"
  ↓
Wait for success
  ↓
Click: "Verify UPI"
  ↓
View SARRAL Score & Loan Limit
```

---

## 🔍 What to Look For

### On UPI Input Screen

- ✅ Input validation works
- ✅ Loading indicator during save
- ✅ Test data seeder button works
- ✅ Success message after seeding

### On Dashboard Screen

- ✅ Loading indicator while calculating
- ✅ Large SARRAL score displayed (e.g., "5370")
- ✅ Loan limit formatted with ₹ symbol
- ✅ Loan marketplace shows offers below

### On Error

- ✅ Clear error message displayed
- ✅ Red error card visible
- ✅ "Go Back" button available

---

## 💡 Pro Tips

1. **Multiple Tests**: Use different UPI IDs for different scenarios
2. **Clear Data**: Use `TestDataSeeder.clearTestTransactions()` to reset
3. **Verify Calculations**: Use `TestDataSeeder.calculateExpectedScore()` to verify math
4. **Firebase Console**: Check Firestore to see saved data
5. **Logs**: Monitor Android Logcat for detailed debug info

---

## 🎯 Success Criteria

Your implementation is working correctly if:

1. ✅ UPI ID saves to Firestore
2. ✅ Test data seeds successfully (24+ transactions created)
3. ✅ Dashboard loads and shows calculated score
4. ✅ Score matches expected calculation (~5,370 for default data)
5. ✅ Loan limit is 30% of monthly inflow
6. ✅ No crashes or errors during flow
7. ✅ Error handling works for edge cases

---

## 📚 Additional Resources

- **Detailed Guide**: See `SARRAL_SCORE_IMPLEMENTATION_COMPLETE.md`
- **Algorithm Details**: See `SARRAL_SCORE_CALCULATION_GUIDE.md`
- **Code Reference**: See `TestDataSeeder.kt` for examples

---

## ⚡ Quick Commands

### Clean Build

```bash
./gradlew clean build
```

### Install on Device

```bash
./gradlew installDebug
```

### View Logs

```bash
adb logcat | findstr SARRAL
```

---

**Ready to Test!** 🎉

Follow Step 1-3 above to see your SARRAL score in action.
