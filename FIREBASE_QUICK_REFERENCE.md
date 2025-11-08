# 🔥 Firebase Quick Reference - SARRAL App

## ⚠️ CRITICAL ACTION REQUIRED

**The `app/google-services.json` is currently a TEMPLATE and MUST be replaced!**

### Quick Steps to Fix:

1. **Go to**: https://console.firebase.google.com/
2. **Project Settings** → **Your Apps** → **Android App**
3. **Package name MUST be**: `com.runanywhere.startup_hackathon20`
4. **Download** the real `google-services.json`
5. **Replace** `app/google-services.json` with downloaded file
6. **Sync** project in Android Studio
7. **Clean & Rebuild** the project

---

## ✅ What's Already Configured

| Component | Version | Status |
|-----------|---------|--------|
| Google Services Plugin | 4.4.2 | ✅ Configured |
| Firebase BOM | 33.5.1 | ✅ Added |
| Firebase Auth | Latest (via BOM) | ✅ Added |
| Firebase Firestore | Latest (via BOM) | ✅ Added |
| Firebase Analytics | Latest (via BOM) | ✅ Added |
| Firebase Initialization | MyApplication.kt | ✅ Added |

---

## 📁 File Locations

### ✅ CORRECT (Already Done)

```
SARRAL/
├── build.gradle.kts          ← Google Services plugin (4.4.2)
└── app/
    ├── build.gradle.kts      ← Plugin applied + Firebase deps
    ├── google-services.json  ← ⚠️ TEMPLATE - Replace with real file!
    └── src/main/java/.../MyApplication.kt  ← Firebase initialized
```

### Package Name

```
com.runanywhere.startup_hackathon20
```

---

## 🧪 How to Verify Setup Works

After replacing `google-services.json`:

1. **Sync Project**: Click elephant icon in Android Studio
2. **Run App**: Launch on device/emulator
3. **Check Logcat**: Look for this message:
   ```
   I/MyApp: FirebaseApp initialization successful
   ```

---

## 🐛 Common Issues

| Issue | Solution |
|-------|----------|
| "FirebaseApp not initialized" | Replace template google-services.json with real file |
| "Package name mismatch" | Re-download with correct package: `com.runanywhere.startup_hackathon20` |
| "File not found" | Ensure file is at `app/google-services.json` (not in subfolders) |
| Build sync fails | File → Invalidate Caches → Restart |

---

## 📚 Full Documentation

See `FIREBASE_CONFIGURATION_FIXED.md` for complete details and troubleshooting.

---

**Ready to Test?** ✅ Replace google-services.json → ✅ Sync → ✅ Build → ✅ Run → ✅ Check Logcat
