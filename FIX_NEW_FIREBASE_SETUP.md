# Quick Fix: Firebase Not Configured (After Changing Firebase)

## 🚀 Your Firebase Setup Status

✅ **Good News**: Your `google-services.json` is correctly configured!

- Project: **sarral-db308**
- Package: `com.runanywhere.startup_hackathon20`
- API Key: Present

---

## ⚡ Quick Fix (Do These in Order)

### Step 1: Clean & Sync (2 minutes)

Run these commands in terminal:

```bash
# Clean all build files
./gradlew clean

# Sync Gradle
./gradlew --refresh-dependencies
```

**OR** in Android Studio:

1. **Build** → **Clean Project**
2. **File** → **Sync Project with Gradle Files**

---

### Step 2: Rebuild Project

```bash
# Rebuild everything
./gradlew build
```

**OR** in Android Studio:

1. **Build** → **Rebuild Project**

---

### Step 3: Enable Firebase Authentication

🔥 **CRITICAL**: Your new Firebase project needs Authentication enabled!

1. Go to: https://console.firebase.google.com/
2. Open project: **sarral-db308**
3. Click **Authentication** (left sidebar)
4. Click **"Get started"** button
5. Click **"Sign-in method"** tab
6. Find **Email/Password** row
7. Click on it
8. **Toggle ON** the "Enable" switch
9. Click **"Save"**

**Without this, login/signup will fail!**

---

### Step 4: Enable Firestore Database

Your app needs Firestore for transactions and user profiles:

1. Still in Firebase Console
2. Click **Firestore Database** (left sidebar)
3. Click **"Create database"**
4. Choose **"Start in test mode"** (for now)
5. Select location: **asia-south1** (Mumbai - closest to India)
6. Click **"Enable"**

---

### Step 5: Set Up Firestore Security Rules

After Firestore is enabled:

1. Go to **Firestore Database** → **Rules** tab
2. Replace with these rules:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // User profiles
    match /user_profiles/{userId} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
    
    // Transactions
    match /transactions/{transactionId} {
      allow read: if request.auth != null;
      allow write: if request.auth != null;
    }
    
    // Loan requests
    match /loan_requests/{requestId} {
      allow read: if request.auth != null;
      allow create: if request.auth != null && request.resource.data.borrower_uid == request.auth.uid;
      allow update, delete: if request.auth != null && resource.data.borrower_uid == request.auth.uid;
    }
  }
}
```

3. Click **"Publish"**

---

### Step 6: Install & Test

```bash
# Install on device
./gradlew installDebug
```

**OR** click the green **Run** button ▶ in Android Studio

---

## 🧪 Test the App

### Test 1: Signup

1. Launch app
2. Click **"Sign Up"**
3. Enter:
    - Name: `Test User`
    - Email: `test@example.com`
    - Password: `test123`
    - Confirm: `test123`
4. Click **"Sign Up"**
5. ✅ Should succeed and redirect to Login

### Test 2: Login

1. Enter:
    - Email: `test@example.com`
    - Password: `test123`
2. Click **"Login"**
3. ✅ Should see User Dashboard

### Test 3: SARRAL Score

1. Click **"Borrow Money"**
2. Click **"Enter UPI Details"**
3. Enter: `test@paytm`
4. Click **"🧪 Seed Test Data (Dev Only)"**
5. Wait for success message
6. Click **"Verify UPI"**
7. ✅ Should see SARRAL Score calculated

---

## 🔍 Verify Firebase is Working

### Check Authentication:

1. Firebase Console → **Authentication** → **Users**
2. You should see `test@example.com` listed
3. ✅ If visible, auth is working!

### Check Firestore:

1. Firebase Console → **Firestore Database** → **Data**
2. You should see collections:
    - `user_profiles`
   - `transactions` (after seeding test data)
3. ✅ If visible, Firestore is working!

---

## 🐛 Common Errors & Fixes

### Error: "Default FirebaseApp is not initialized"

**Cause**: Gradle didn't process the new `google-services.json`

**Fix**:

```bash
./gradlew clean
./gradlew --refresh-dependencies
./gradlew build
```

Then restart Android Studio.

---

### Error: "FirebaseAuth has not been initialized"

**Cause**: Google Services plugin not applied

**Fix**: Verify `app/build.gradle.kts` has this at the top:

```kotlin
plugins {
    // ... other plugins
    id("com.google.gms.google-services")
}
```

Then sync Gradle.

---

### Error: "CONFIGURATION_NOT_FOUND"

**Cause**: Package name mismatch

**Fix**: Verify both files match:

**google-services.json**:

```json
"package_name": "com.runanywhere.startup_hackathon20"
```

**app/build.gradle.kts**:

```kotlin
applicationId = "com.runanywhere.startup_hackathon20"
```

They match! ✅

---

### Error: "Failed to get document because the client is offline"

**Cause**: Firestore not enabled or no internet

**Fix**:

1. Enable Firestore Database in Firebase Console (Step 4 above)
2. Check internet connection
3. Disable airplane mode

---

### Error: "PERMISSION_DENIED" in Firestore

**Cause**: Firestore security rules too restrictive

**Fix**: Update rules (see Step 5 above)

---

## 🔥 Nuclear Option (If Nothing Works)

If errors persist, do a complete clean:

```bash
# Stop Gradle daemon
./gradlew --stop

# Remove build files
rm -rf .gradle build app/build

# On Windows use:
# rmdir /s /q .gradle
# rmdir /s /q build
# rmdir /s /q app\build

# Clean and rebuild
./gradlew clean
./gradlew build
./gradlew installDebug
```

**In Android Studio**:

1. **File** → **Invalidate Caches / Restart**
2. Choose **"Invalidate and Restart"**
3. Wait for re-indexing
4. **Build** → **Rebuild Project**
5. Run the app

---

## ✅ Success Checklist

Complete setup when all are checked:

- [ ] `google-services.json` in `app/` folder
- [ ] Cleaned project with `./gradlew clean`
- [ ] Synced Gradle files
- [ ] Rebuilt project
- [ ] Email/Password auth **enabled** in Firebase Console
- [ ] Firestore Database **created** in Firebase Console
- [ ] Firestore Security Rules **published**
- [ ] App builds without errors
- [ ] Signup works (test@example.com created)
- [ ] Login works
- [ ] Can see user in Firebase Console → Authentication
- [ ] Test data seeder works
- [ ] SARRAL score calculates correctly

---

## 📱 Expected App Flow

```
Launch App
   ↓
Signup (test@example.com)
   ↓
Login
   ↓
User Dashboard
   ↓
Borrow Money
   ↓
Enter UPI Details (test@paytm)
   ↓
Seed Test Data
   ↓
Verify UPI
   ↓
View SARRAL Score (~5,370)
   ↓
View Loan Limit (~₹2,685)
   ↓
See Loan Marketplace
```

---

## 🎯 TL;DR - Just Do This

```bash
# In terminal:
./gradlew clean
./gradlew build
./gradlew installDebug
```

**In Firebase Console:**

1. Enable Authentication (Email/Password)
2. Create Firestore Database (test mode)
3. Publish security rules

**Run the app and test signup/login!**

---

## 📞 Need More Help?

If error persists:

1. Copy the EXACT error message from Logcat
2. Check which line of code is failing
3. Verify Firebase Console shows your project name: **sarral-db308**
4. Make sure you're connected to internet

Common error locations:

- `FirebaseAuth.getInstance()` → Auth not enabled
- `FirebaseFirestore.getInstance()` → Firestore not created
- `.collection("...")` → Security rules too strict

---

**Status**: Your Firebase config file is correct! Just need to sync & enable services. 🚀
