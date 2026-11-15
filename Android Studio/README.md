# Vidyapath Android app

This is a separate Android Studio project. The existing web app remains in the
repository root and is not used as a WebView; the Android app is implemented
with Kotlin and Jetpack Compose.

## Open and run

Open this `Android Studio` folder in Android Studio and let Gradle sync. Run the
`app` configuration on an Android 8.0+ device or emulator.

Accounts are created and authenticated locally on the device. Sign-up stores
the account name and email with a salted PBKDF2-HMAC-SHA256 password hash; the
password itself is not stored. No network connection, database, Neon endpoint,
or email verification setup is required to sign up or sign in.

Study tasks, subjects, reminders, resources, progress, and profile photos are
stored in the app's private device storage and separated by local account.
Accounts and study data do not sync or transfer to another device. On a new
device, create an account there before signing in. Uploaded files are private to
the app sandbox. Profile images can be captured with the device camera or
selected from storage.

## Security notes

The app avoids network authentication and broad storage permissions, stores
passwords as salted PBKDF2-HMAC-SHA256 hashes, shares private files through a
non-exported `FileProvider`, and disables Android backup. Local sign-in is for
demo use; it does not provide server-backed identity, account recovery, or
cross-device authentication. These measures cannot guarantee zero
vulnerabilities. Review dependency advisories and run Android security testing
before release.
