# Vidyapath Android app

This is a separate Android Studio project. The existing web app remains in the
repository root and is not used as a WebView; the Android app is implemented
with Kotlin and Jetpack Compose.

## Open and run

Open this `Android Studio` folder in Android Studio and let Gradle sync. Run the
`app` configuration on an Android 8.0+ device or emulator.

The app uses the same Neon Managed Auth project for email one-time-code sign-in.
Set the public branch Auth endpoint in this folder's ignored `local.properties`:

```properties
neonAuthBaseUrl=https://YOUR_NEON_AUTH_ENDPOINT
```

Do not put a database connection string, Neon API key, or other server secret in
the Android app. The Auth endpoint is public; database access stays server-side.

Study tasks, subjects, reminders, resources, progress, and profile photos are
stored in the app's private device storage and separated by authenticated user.
They do not sync across devices. Uploaded files are private to the app sandbox.
Profile images can be captured with the device camera or selected from storage.

## Security notes

The app uses HTTPS-only Auth requests, avoids broad storage permissions, shares
private files through a non-exported `FileProvider`, disables Android backup,
and encrypts its session cookie with an Android Keystore AES-GCM key. These
measures reduce risk but cannot guarantee that an app has zero vulnerabilities.
Review dependency advisories and run Android security testing before release.
