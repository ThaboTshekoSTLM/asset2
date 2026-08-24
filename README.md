# ICT Asset Register

Municipal ICT asset management system with an Android application, a browser-based
web application, and a shared Supabase backend. Both clients use the same users,
assets, movements, photographs, roles, and reports.

## Live services

- Web app: <https://thabotshekostlm.github.io/asset2/>
- Source repository: <https://github.com/ThaboTshekoSTLM/asset2>
- Supabase project: `zspqzluwzuiasabiphdd` (`ICT Asset Register`)

For the complete record of setup, deployment, user administration, testing, and
troubleshooting, see [docs/PROJECT_SETUP.md](docs/PROJECT_SETUP.md).

## Technology

- Kotlin
- Jetpack Compose
- Room Database
- Clean architecture package split: `data`, `domain`, `presentation`
- CameraX + ML Kit barcode scanning
- Local PDF and Excel-compatible XML exports
- Offline-first repositories that can be replaced or wrapped by a sync backend later

## Production login

Production users sign in with an email address and password created in Supabase
Authentication. Passwords are intentionally not stored in this repository or its
documentation. The old offline demonstration usernames are not production logins.

## Open In Android Studio

1. Open this folder in Android Studio.
2. Allow Gradle to sync and download dependencies.
3. Run the `app` configuration on an emulator or Android phone.

The Android app now authenticates against Supabase and shares assets, movements,
search results, dashboards, and reports with the web app. Add these values to the
untracked `local.properties` file before building locally:

```properties
SUPABASE_URL=https://YOUR_PROJECT.supabase.co
SUPABASE_PUBLISHABLE_KEY=YOUR_PUBLIC_PUBLISHABLE_KEY
```

Production users sign in with their Supabase email address and app-user password.
User accounts are created in Supabase Authentication; roles are stored in the
`public.profiles` table.

## GitHub Build

This project includes `.github/workflows/android-build.yml`.

After pushing the project to GitHub, open the repository's **Actions** tab and run **Android APK Build**. The workflow builds `app-debug.apk` and uploads it as an artifact named `ict-asset-register-debug-apk`.

Before running the GitHub build, add repository Actions secrets named
`SUPABASE_URL` and `SUPABASE_PUBLISHABLE_KEY`.

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

The deployed web version connects to the shared Supabase project. When its Supabase
configuration is empty, it falls back to local demonstration data in `localStorage`.
The database schema, role policies, audit logging, and setup instructions are in
`supabase/`.
