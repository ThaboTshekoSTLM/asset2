# ICT Asset Register

Offline-first Android application for municipal ICT asset registration, allocation, movement tracking, audit logging, and local report export.

## Technology

- Kotlin
- Jetpack Compose
- Room Database
- Clean architecture package split: `data`, `domain`, `presentation`
- CameraX + ML Kit barcode scanning
- Local PDF and Excel-compatible XML exports
- Offline-first repositories that can be replaced or wrapped by a sync backend later

## Sample Login Accounts

| Role | Username | Password |
| --- | --- | --- |
| Admin | `admin` | `admin123` |
| Standard User | `standard` | `user123` |
| ICT Technician | `tech` | `tech123` |
| Viewer / Auditor | `auditor` | `audit123` |

## Open In Android Studio

1. Open this folder in Android Studio.
2. Allow Gradle to sync and download dependencies.
3. Run the `app` configuration on an emulator or Android phone.

## GitHub Build

This project includes `.github/workflows/android-build.yml`.

After pushing the project to GitHub, open the repository's **Actions** tab and run **Android APK Build**. The workflow builds `app-debug.apk` and uploads it as an artifact named `ict-asset-register-debug-apk`.

## Web Version

The `web/` folder contains a static browser version for colleagues to view assets, manually capture assets, record movements, preview reports, and manage users.

Open locally:

```text
web/index.html
```

Deploy on GitHub Pages:

1. Push the project to GitHub.
2. Open **Settings** > **Pages**.
3. Set the source to **GitHub Actions**.
4. Run the **Web App Pages Deploy** workflow.

The current web version stores data in each browser using `localStorage`. To let all colleagues share the same live asset database, connect the web app and Android app to a backend such as Supabase, Firebase, or a municipal API.
