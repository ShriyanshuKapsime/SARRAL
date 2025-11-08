# Fix "Firebase not configured" Error

## Good News! ✅

Your `google-services.json` file is correctly configured with project: **sarral-protostars**

## The Problem 🔴

The error is showing because **Android Studio hasn't processed the new Firebase configuration yet**.

---

## Solution: Sync and Rebuild

### Step 1: Clean the Project (IMPORTANT!)

1. In Android Studio, go to **Build** → **Clean Project**
2. Wait for it to complete (you'll see "BUILD SUCCESSFUL" in the bottom)

### Step 2: Sync Gradle Files

1. Go to **File** → **Sync Project with Gradle Files**
2. Wait for sync to complete (watch the progress bar at the bottom)
3. You should see "Gradle sync finished" when done

### Step 3: Invalidate Caches (If Still Not Working)

If step 2 didn't work:

1. Go to **File** → **Invalidate Caches / Restart...**
2. Click **"Invalidate and Restart"**
3. Android Studio will restart
4. Wait for indexing to complete

### Step 4: Rebuild the Project

1. Go to **Build** → **Rebuild Project**
2. Wait for rebuild to complete
3. Check the "Build" tab at bottom for any errors

### Step 5: Run the App

1. Click the green **Run** button (▶)
2. Select your device/emulator
3. Wait for app to install and launch

---

## Still Getting the Error?

### Check #1: Verify Package Name Match

Your `google-services.json` has:

```
package_name: "com.runanywhere.startup_hackathon20"
```

Your `app/build.gradle.kts` should have:

```kotlin
applicationId = "com.runanywhere.startup_hackathon20"
```

✅ These match! Good.

### Check #2: Enable Email/Password Authentication

1. Go to **Firebase Console**: https://console.firebase.google.com/
2. Open your project: **sarral-protostars**
3. Click **Authentication** in left sidebar
4. Click **"Get started"** (if you haven't yet)
5. Go to **"Sign-in method"** tab
6. Click on **"Email/Password"**
7. **Toggle ON** the first switch (Email/Password)
8. Click **"Save"**

**This is CRITICAL - The app won't work without this!**

### Check #3: Check Build Output

Look at the **Build** tab at the bottom of Android Studio for errors like:

- "google-services.json not found" → File is in wrong location
- "No matching client found" → Package name mismatch
- Gradle sync errors → Dependencies not downloaded

### Check #4: Verify File Location

The file MUST be at:

```
app/google-services.json   ✅ Correct
```

NOT at:

```
google-services.json       ❌ Wrong (root level)
src/google-services.json   ❌ Wrong
```

---

## Common Issues After Adding Firebase

### Issue: "Default FirebaseApp is not initialized"

**Solution:**

1. Make sure you added the Google Services plugin to `app/build.gradle.kts`
2. Verify this line exists at the TOP of `app/build.gradle.kts`:
   ```kotlin
   id("com.google.gms.google-services")
   ```
3. Rebuild the project

### Issue: "FirebaseAuth is not initialized"

**Solution:**

1. Clean project
2. Sync Gradle
3. Rebuild project
4. Restart Android Studio if needed

### Issue: Build fails with "google-services plugin" error

**Solution:**
Check that `build.gradle.kts` (root) has:

```kotlin
id("com.google.gms.google-services") version "4.4.0" apply false
```

---

## Debugging: Check Logcat

1. Open **Logcat** tab at the bottom
2. Filter by: `FirebaseApp` or `FirebaseAuth`
3. Look for initialization errors
4. Common messages:
    - ✅ "Successfully configured..." = Working!
    - ❌ "FirebaseApp initialization unsuccessful" = Not synced properly
    - ❌ "Default FirebaseApp is not initialized" = Plugin issue

---

## Nuclear Option: Start Fresh Build

If nothing works:

1. Close Android Studio
2. Delete these folders from your project:
    - `.gradle/`
    - `.idea/`
    - `app/build/`
    - `build/`
3. Open Android Studio
4. Open your project (it will re-index everything)
5. Sync Gradle
6. Build → Rebuild Project
7. Run the app

---

## Verify Firebase is Working

After syncing and rebuilding, test:

1. **Launch the app**
2. **Click "Sign Up"**
3. Fill in:
    - Name: Test User
    - Email: test@test.com
    - Password: test123
    - Confirm: test123
4. **Click "Sign Up"**

**If you get a DIFFERENT error** (like "email already exists" on second try), that means Firebase is
working! The first error was just the config issue.

**If signup succeeds**, you'll be redirected to Login. Then:

1. Login with test@test.com / test123
2. You should see the User Dashboard with your info
3. ✅ **Firebase is fully working!**

---

## Check Firebase Console

After successful signup:

1. Go to Firebase Console
2. Open **sarral-protostars** project
3. Click **Authentication** → **Users** tab
4. You should see your test user listed there
5. This confirms Firebase is working correctly

---

## Summary Checklist

- [x] `google-services.json` has real Firebase config ✅ (Done!)
- [ ] Cleaned project
- [ ] Synced Gradle files
- [ ] Rebuilt project
- [ ] Email/Password auth enabled in Firebase Console
- [ ] Tested signup/login
- [ ] Verified user appears in Firebase Console

Once you complete these steps, the "Firebase not configured" error will be gone!

---

## Quick Commands

If you prefer terminal commands:

```bash
# Clean Gradle cache
./gradlew clean

# Build the project
./gradlew build

# Install debug APK
./gradlew installDebug
```

Then run the app from Android Studio.

---

**Most Common Solution**: Just doing **Clean Project** → **Sync Gradle** → **Rebuild Project** fixes
90% of cases! 🚀
