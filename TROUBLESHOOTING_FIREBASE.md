# Firebase Authentication Troubleshooting Guide

## Internal Error During Sign-In - SOLUTION

If you're getting an **internal error** when trying to sign in, it's almost certainly because you're
using the **placeholder `google-services.json` file**.

### 🔴 The Problem

The app currently has a placeholder Firebase configuration file at `app/google-services.json` with
dummy values:

```json
{
  "project_id": "YOUR_PROJECT_ID",
  "api_key": "YOUR_API_KEY",
  ...
}
```

This will **ALWAYS fail** with an internal error because Firebase can't connect to a valid project.

### ✅ The Solution

You MUST replace the placeholder file with your actual Firebase configuration:

#### Step 1: Create a Firebase Project

1. Go to https://console.firebase.google.com/
2. Click **"Add project"** (or use existing project)
3. Give it a name (e.g., "SARRAL App")
4. Follow the setup wizard

#### Step 2: Add Your Android App

1. In Firebase Console, click the **Android icon** (⚙️ > Project settings)
2. Click **"Add app"**
3. Enter your package name: **`com.runanywhere.startup_hackathon20`**
    - ⚠️ This MUST match exactly!
4. (Optional) Add app nickname: "SARRAL"
5. Skip SHA-1 for now (not needed for email/password auth)
6. Click **"Register app"**

#### Step 3: Download the REAL google-services.json

1. After registering, Firebase will show a **"Download google-services.json"** button
2. Click to download the file
3. **REPLACE** the file at `app/google-services.json` with this downloaded file

#### Step 4: Enable Email/Password Authentication

1. In Firebase Console, go to **Authentication** (left sidebar)
2. Click **"Get started"** (if first time)
3. Go to **"Sign-in method"** tab
4. Click on **"Email/Password"**
5. **Toggle ON** the first switch (Email/Password)
6. Click **"Save"**

#### Step 5: Rebuild the App

1. In Android Studio, click **"File" > "Sync Project with Gradle Files"**
2. Wait for sync to complete
3. Clean and rebuild: **"Build" > "Clean Project"** then **"Build" > "Rebuild Project"**
4. Run the app

---

## Common Error Messages and Solutions

### Error: "CONFIGURATION_NOT_FOUND"

**Cause**: `google-services.json` file is missing or invalid

**Solution**:

1. Verify the file exists at `app/google-services.json`
2. Make sure it's the actual file from Firebase Console, not the placeholder
3. Sync Gradle and rebuild

### Error: "No account found with this email"

**Cause**: You're trying to login but haven't created an account yet

**Solution**: Click "Sign Up" first to create an account

### Error: "Email already registered"

**Cause**: You're trying to sign up but the email is already in use

**Solution**: Click "Login" instead

### Error: "Network error. Check your connection"

**Cause**: No internet connection or Firebase servers unreachable

**Solution**:

1. Check your internet connection
2. Try again in a few moments
3. Check Firebase status: https://status.firebase.google.com/

### Error: "Password must be at least 6 characters"

**Cause**: Firebase requires minimum 6 character passwords

**Solution**: Use a longer password

### Error: "Incorrect password"

**Cause**: Wrong password entered

**Solution**:

1. Try again with correct password
2. Use the "Show/Hide" button to verify what you're typing
3. Check if Caps Lock is on

### Error: "Invalid email format"

**Cause**: Email is not properly formatted

**Solution**: Enter a valid email like `user@example.com`

---

## How to Verify Firebase is Working

### Check 1: Verify google-services.json is Real

Open `app/google-services.json` and check:

- Does `project_id` start with `YOUR_`? ❌ It's still the placeholder!
- Does `project_id` have a real value like `sarral-app-12345`? ✅ Good!

### Check 2: Verify Authentication is Enabled

1. Go to Firebase Console
2. Open your project
3. Click "Authentication" in left sidebar
4. Go to "Sign-in method" tab
5. Email/Password should show "Enabled"

### Check 3: Test the App

1. **Launch app** - Should show Login screen
2. **Click "Sign Up"**
3. Fill in:
    - Name: Test User
    - Email: test@example.com
    - Password: test123
    - Confirm: test123
4. **Click "Sign Up"** - Should succeed and navigate to Login
5. **Login** with test@example.com / test123
6. Should navigate to User Dashboard

---

## Debugging Steps

### 1. Check Logcat for Detailed Errors

In Android Studio:

1. Open **Logcat** tab (bottom of screen)
2. Filter by "Firebase" or "Auth"
3. Look for error messages with details

### 2. Verify Package Name Matches

Check these match exactly:

- `app/build.gradle.kts`: `applicationId = "com.runanywhere.startup_hackathon20"`
- `app/google-services.json`: `"package_name": "com.runanywhere.startup_hackathon20"`
- Firebase Console: The registered Android app

### 3. Check Internet Permission

Verify `app/src/main/AndroidManifest.xml` has:

```xml
<uses-permission android:name="android.permission.INTERNET" />
```

✅ This is already in your manifest, so this should be fine.

### 4. Verify Dependencies are Downloaded

1. Open **Build** > **Make Project**
2. Check for any dependency download errors
3. If errors, try:
    - **File** > **Invalidate Caches / Restart**
    - Delete `.gradle` folder and re-sync

---

## Testing Without Firebase (Temporary Workaround)

If you want to test the UI without Firebase setup:

1. You can't - Firebase Authentication requires a valid project
2. However, you can:
    - Set up Firebase (takes 5 minutes!)
    - Use Firebase Emulator (more complex setup)

**Recommendation**: Just set up Firebase properly - it's faster!

---

## Quick Checklist

Before running the app, verify:

- [ ] Created Firebase project
- [ ] Added Android app with correct package name
- [ ] Downloaded actual `google-services.json`
- [ ] Replaced placeholder file with downloaded file
- [ ] Enabled Email/Password authentication in Firebase Console
- [ ] Synced Gradle in Android Studio
- [ ] Rebuilt the project
- [ ] Device/emulator has internet connection

---

## Still Having Issues?

### Option 1: Start Fresh

1. Delete `app/google-services.json`
2. Go to Firebase Console
3. Delete the Android app registration
4. Re-add the Android app
5. Download NEW `google-services.json`
6. Place in `app/` folder
7. Sync and rebuild

### Option 2: Check Firebase Console

In Firebase Console > Authentication > Users:

- After successful signup, you should see user entries here
- If users appear here, Firebase is working!

### Option 3: Verify API Key

1. Open `app/google-services.json`
2. Look for `"current_key"` under `"api_key"`
3. Copy that value
4. Go to Firebase Console > Project Settings > General
5. Verify the API key matches

---

## Contact Information

If you've followed all steps and still have issues:

1. Check the full error message in Logcat
2. Verify Firebase status: https://status.firebase.google.com/
3. Review Firebase Authentication docs: https://firebase.google.com/docs/auth/android/start

---

## Summary

**The most common cause of "internal error"**:

- ❌ Using placeholder `google-services.json`
- ✅ Solution: Download and use real file from Firebase Console

**Second most common cause**:

- ❌ Email/Password authentication not enabled in Firebase
- ✅ Solution: Enable it in Firebase Console > Authentication > Sign-in method

Follow the steps above and your authentication will work! 🚀
