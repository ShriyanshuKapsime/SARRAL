# ⚠️ HOW TO FIX "Internal Error" During Sign-In

## The Problem

You're getting an **"internal error"** when trying to sign in because the app is using a *
*PLACEHOLDER Firebase configuration file**.

The file `app/google-services.json` currently has dummy values like:

```json
"project_id": "YOUR_PROJECT_ID"
"api_key": "YOUR_API_KEY"
```

This will **NEVER work**. Firebase can't connect to a project that doesn't exist!

---

## The Solution (5 Minutes)

### Step 1: Create Firebase Project (2 min)

1. Open: **https://console.firebase.google.com/**
2. Click **"Add project"**
3. Name it: **"SARRAL"** (or anything you like)
4. Click Continue → Continue → Create project
5. Wait for project to be created

### Step 2: Add Android App (1 min)

1. Click the **Android icon** to add an Android app
2. In "Android package name", enter: **`com.runanywhere.startup_hackathon20`**
    - ⚠️ **Must be EXACT!**
3. Click **"Register app"**

### Step 3: Download Real Config File (30 sec)

1. You'll see **"Download google-services.json"** button
2. Click it to download
3. **IMPORTANT**: Move the downloaded file to replace:
   ```
   C:\Users\shriy\StudioProjects\SARRAL\app\google-services.json
   ```
4. Confirm to replace the existing placeholder file

### Step 4: Enable Email/Password Auth (1 min)

1. In Firebase Console sidebar, click **"Authentication"**
2. Click **"Get started"** (if first time)
3. Click **"Sign-in method"** tab
4. Click **"Email/Password"**
5. Toggle **ON** the first switch
6. Click **"Save"**

### Step 5: Rebuild App (30 sec)

In Android Studio:

1. Click **"File"** → **"Sync Project with Gradle Files"**
2. Wait for sync to complete
3. Click the **"Run"** button (green play icon)

---

## Verify It's Fixed

Run the app and:

1. Click **"Sign Up"**
2. Fill in:
    - Name: Test User
    - Email: test@example.com
    - Password: test123
    - Confirm: test123
3. Click **"Sign Up"**
4. ✅ Should succeed and navigate to Login screen
5. Login with test@example.com / test123
6. ✅ Should navigate to User Dashboard

---

## Quick Check Script

Run this in PowerShell to check if you've replaced the file:

```powershell
.\check_firebase_setup.ps1
```

If it says **"PLACEHOLDER file"** → You still need to replace it!

---

## Still Not Working?

### Check #1: File is in the right place?

```
✅ app/google-services.json
❌ google-services.json (in root)
❌ Downloads/google-services.json
```

### Check #2: Package name matches?

Open the downloaded `google-services.json` and verify:

```json
"android_client_info": {
  "package_name": "com.runanywhere.startup_hackathon20"
}
```

### Check #3: Email/Password enabled?

Firebase Console → Authentication → Sign-in method → Email/Password should show **"Enabled"**

### Check #4: Internet connection?

The app needs internet to connect to Firebase

---

## Error Messages Explained

| Error Message | Cause | Fix |
|--------------|-------|-----|
| "Internal error" | Placeholder config file | Replace with real file |
| "CONFIGURATION_NOT_FOUND" | Invalid config file | Download new file from Firebase |
| "No account found" | Account doesn't exist | Use Sign Up first |
| "Email already registered" | Account exists | Use Login instead |
| "Network error" | No internet | Check connection |

---

## Summary

1. ❌ **Problem**: Using placeholder `google-services.json`
2. ✅ **Solution**: Download real file from Firebase Console
3. 📁 **Location**: Replace `app/google-services.json`
4. 🔧 **Enable**: Turn on Email/Password auth in Firebase
5. 🔄 **Rebuild**: Sync Gradle and run app

**Total time: ~5 minutes**

---

## Need More Help?

- **Detailed Guide**: Read `FIREBASE_SETUP_INSTRUCTIONS.md`
- **All Errors**: Read `TROUBLESHOOTING_FIREBASE.md`
- **Implementation Details**: Read `IMPLEMENTATION_SUMMARY.md`

---

**TIP**: Once you replace the file and rebuild, the error will be gone! The app works perfectly once
Firebase is configured. 🚀
