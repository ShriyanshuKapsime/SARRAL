# Firebase Authentication Setup Instructions

This guide will help you set up Firebase Authentication for the SARRAL app.

## Step 1: Create a Firebase Project

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Click on "Add project" or select an existing project
3. Follow the setup wizard to create your project

## Step 2: Add Android App to Firebase Project

1. In the Firebase Console, click on the Android icon to add an Android app
2. Enter your package name: `com.runanywhere.startup_hackathon20`
3. (Optional) Add an app nickname: "SARRAL"
4. (Optional) Add SHA-1 certificate fingerprint (not required for email/password authentication)
5. Click "Register app"

## Step 3: Download google-services.json

1. After registering your app, Firebase will provide a `google-services.json` file
2. Download this file
3. **IMPORTANT**: Replace the placeholder `app/google-services.json` file in this project with the
   downloaded file
4. The file should be placed at: `app/google-services.json`

## Step 4: Enable Email/Password Authentication

1. In the Firebase Console, go to **Authentication** → **Sign-in method**
2. Click on **Email/Password** provider
3. Enable the first toggle switch (Email/Password)
4. Click "Save"

## Step 5: Sync Gradle and Build

1. Open the project in Android Studio
2. Sync Gradle files (File → Sync Project with Gradle Files)
3. Wait for the sync to complete
4. Build and run the app on your device or emulator

## App Features

### Login Screen

- Email and password fields with validation
- Password visibility toggle
- Error messages for failed login attempts
- Navigation to Signup screen

### Signup Screen

- Full name, email, password, and confirm password fields
- Password strength validation (minimum 6 characters)
- Password match verification
- User profile update with display name
- Navigation to Login screen after successful signup

### User Dashboard

- Displays user information (name, email, user ID)
- Logout functionality
- Automatic navigation to login on logout

## Navigation Flow

```
App Start
    ↓
Check Auth State
    ├─ Logged In → User Dashboard
    └─ Not Logged In → Login Screen
                           ↓
                      Sign Up Screen
                           ↓
                      Login Screen
                           ↓
                    User Dashboard
```

## Troubleshooting

### Issue: "google-services.json not found" error

**Solution**: Make sure you've replaced the placeholder `google-services.json` file with the actual
file from Firebase Console.

### Issue: Build fails with Firebase dependencies

**Solution**: Ensure you have internet connectivity and let Gradle download the required
dependencies. Clean and rebuild the project.

### Issue: Authentication fails

**Solution**:

- Check that Email/Password authentication is enabled in Firebase Console
- Verify that the package name matches in both Firebase Console and your app
- Check your internet connection

### Issue: Linter errors in Android Studio

**Solution**: Sync Gradle files and wait for dependencies to download. The errors should resolve
automatically.

## Firebase SDK Used

- **Firebase BoM**: 32.7.0
- **Firebase Authentication**: Latest (managed by BoM)

## Security Notes

1. **Never commit** your actual `google-services.json` file to public repositories
2. Use Firebase Security Rules to protect your data
3. Consider implementing rate limiting for authentication attempts
4. Use strong password policies in production

## Next Steps

After completing the setup, you can:

- Add password reset functionality
- Implement email verification
- Add additional authentication providers (Google, Facebook, etc.)
- Set up Firebase Firestore for data storage
- Add user profile management features

## Support

For issues related to Firebase setup, visit:

- [Firebase Documentation](https://firebase.google.com/docs/auth/android/start)
- [Firebase Support](https://firebase.google.com/support)
