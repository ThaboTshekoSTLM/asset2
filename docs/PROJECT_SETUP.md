# ICT Asset Register: implementation and deployment record

Last updated: 24 August 2026

## 1. Result

The ICT Asset Register is now a shared system with:

- an Android application for field work, barcode scanning, photographs, asset
  registration, movement, search, reports, and user views;
- a browser application for colleagues using computers or phones;
- one Supabase backend shared by Android and the web app;
- email/password authentication and role-based access;
- a PostgreSQL database protected by Row Level Security (RLS);
- an `asset-photos` Supabase Storage bucket;
- GitHub Actions builds for the Android APK and GitHub Pages web deployment.

Public web address:

<https://thabotshekostlm.github.io/asset2/>

GitHub repository:

<https://github.com/ThaboTshekoSTLM/asset2>

Supabase project:

- Name: `ICT Asset Register`
- Project reference: `zspqzluwzuiasabiphdd`
- Region: West EU (Ireland)

## 2. Work completed

1. Reviewed the existing Android and web project.
2. Created a Supabase organization and the `ICT Asset Register` project.
3. Installed the database schema from `supabase/schema.sql`.
4. Applied the API grants in `supabase/migrations/002_api_grants.sql`.
5. Created a Supabase Authentication user and confirmed web sign-in.
6. Connected the web application to Supabase Auth, Database, and Storage.
7. Connected the Android application to the same Supabase project.
8. Built the Android APK successfully.
9. Created the GitHub repository `ThaboTshekoSTLM/asset2`.
10. Pushed commit `6e0c3cb` (`Connect web and Android apps to Supabase`).
11. Enabled GitHub Pages with GitHub Actions as its source.
12. Reran the Pages deployment and verified a successful deployment.
13. Opened the public site and verified the `ICT Asset Web Register` login page.

## 3. Architecture

```text
Android app -------------------+
                               |
                               +--> Supabase Auth
Web app on GitHub Pages -------+--> PostgreSQL + RLS
                               +--> Storage (asset-photos)

GitHub repository --> GitHub Actions --> Web deployment / Android APK artifact
```

Important folders:

| Location | Purpose |
| --- | --- |
| `app/` | Kotlin/Jetpack Compose Android application |
| `web/` | Static browser application deployed by GitHub Pages |
| `supabase/schema.sql` | Tables, triggers, roles, RLS policies, storage, and grants |
| `supabase/migrations/` | Additional database migrations |
| `.github/workflows/` | Automated web deployment and Android APK build |

## 4. Supabase database

The schema creates and secures these main tables:

- `profiles`: application profile and role for every authenticated user;
- `assets`: asset identity, barcode, serial number, location, custodian, status,
  photograph path, and audit fields;
- `asset_movements`: allocation, transfer, return, disposal, and other movement
  history;
- `audit_logs`: important user and data events.

It also creates the private `asset-photos` Storage bucket. Authenticated users can
read photos; permitted writers can upload/update them; administrators can delete
them.

RLS is enabled. The public/publishable key is therefore safe to place in the clients,
but the Supabase **service-role key must never be put in the web app, Android app,
GitHub repository, APK, or screenshots**.

### Roles

Roles live in `public.profiles`. The schema supports:

- `admin`: user management and full administrative access;
- `technician`: operational asset capture and movement work;
- `standard`: normal permitted asset work;
- `auditor`: read/report access.

The exact permission checks are defined by functions and RLS policies in
`supabase/schema.sql`.

## 5. Creating colleague accounts

Colleagues do not use the Supabase dashboard or a shared password. Give every person
their own account:

1. Open Supabase Dashboard.
2. Select `ICT Asset Register`.
3. Open **Authentication > Users**.
4. Select **Add user** and enter the colleague's email and a temporary password.
5. If offered, create/confirm the user immediately.
6. Open **Table Editor > profiles**.
7. Find the new user and set the correct `role` and `active` value.
8. Give the colleague the public web URL or install the APK on their phone.
9. Ask them to sign in with their email address and password.

If a user is deleted and recreated, it receives a new UUID. Confirm that the trigger
created a new matching row in `profiles`, then assign the role again.

Never document or send production passwords in this repository. A database password
that has been shared in chat or a screenshot should be rotated in Supabase.

## 6. Web application configuration

`web/config.js` provides the browser client with the project URL and public key:

```javascript
window.ICT_ASSET_CONFIG = {
  supabaseUrl: "https://YOUR_PROJECT.supabase.co",
  supabaseAnonKey: "YOUR_PUBLIC_PUBLISHABLE_KEY"
};
```

Use the project's publishable/anon key only. The example is in
`web/config.example.js`.

For local testing, serve the folder with an HTTP server; do not rely on opening the
HTML file directly:

```powershell
python -m http.server 8766 --directory web
```

Then open <http://localhost:8766/>.

## 7. Android application configuration and build

The Android build reads the Supabase values from environment variables or the local,
untracked `local.properties` file:

```properties
SUPABASE_URL=https://YOUR_PROJECT.supabase.co
SUPABASE_PUBLISHABLE_KEY=YOUR_PUBLIC_PUBLISHABLE_KEY
```

Build a debug APK on Windows:

```powershell
.\gradlew.bat assembleDebug
```

Expected APK:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Copy that APK to an Android phone, permit installation from the selected file manager
when Android asks, install it, and sign in with a Supabase user account. The phone must
have internet access because production data is shared through Supabase.

For a GitHub build, add repository Actions secrets named `SUPABASE_URL` and
`SUPABASE_PUBLISHABLE_KEY`, run **Android APK Build** under **Actions**, and download
the `ict-asset-register-debug-apk` artifact after it succeeds.

## 8. GitHub Pages deployment

The workflow `.github/workflows/web-pages.yml` publishes the contents of `web/`.

Initial setup performed:

1. Created the public GitHub repository `ThaboTshekoSTLM/asset2`.
2. Pushed the project to its `master` branch.
3. Opened **Settings > Pages**.
4. Changed **Source** to **GitHub Actions**.
5. Opened **Actions > Web App Pages Deploy**.
6. Reran the failed job after Pages was enabled.
7. Confirmed status **Success** (21 seconds for deployment attempt 2).
8. Verified the public login page at the URL above.

Future pushes that change `web/**` or the Pages workflow automatically redeploy the
site. A manual deployment can be started with **Run workflow**.

## 9. Access from anywhere

### Browser

Send colleagues this address:

<https://thabotshekostlm.github.io/asset2/>

It is an internet address; `localhost:8766` works only on the computer running the
local server and must not be shared as the production address.

### Android

Install the current APK on each authorized device. Because Android and the web app
use the same Supabase project, saved assets and movements should appear in both after
refreshing/reopening the relevant view.

## 10. Testing checklist

Use a non-critical test asset with a genuinely unique barcode:

- sign in on the public web URL;
- sign in on Android with the same or another authorized user;
- register one asset on Android, including a photograph;
- confirm the asset appears in web search/listing;
- record a movement and confirm it appears in history/reports;
- register an asset on the web and confirm Android can find it;
- verify a read-only/auditor account cannot perform writer operations;
- verify an inactive account cannot access protected data;
- confirm photographs load only after authentication.

## 11. Problems encountered and resolutions

### Invalid username or password

Cause: the newly created Supabase project did not contain the previous local/demo
account, or the entered password did not match the Auth user.

Resolution: create/recreate the user in **Authentication > Users**, confirm its
`profiles` row and role, then use the Supabase email and password. This was tested and
the user successfully logged in.

### `duplicate key value violates unique constraint "assets_asset_barcode_key"`

Cause: `assets.asset_barcode` is unique and the scanned value already exists. In the
observed Android test, the barcode field contained `HTTP://WWW.MANCOSA.CO.ZA`, which
was likely a QR code/URL rather than the municipality's unique asset tag.

Resolution:

1. Search for the barcode before registering.
2. If it exists, update/move the existing asset instead of creating another record.
3. If the wrong QR code was scanned, clear the field and scan/type the actual unique
   municipal asset barcode.
4. Do not remove the unique constraint; it prevents duplicate asset records.

The data can be saved to the shared Supabase project only when its barcode is unique.

### GitHub Pages workflow failed initially

Cause: GitHub Pages had not yet been enabled for the new repository.

Resolution: select **Settings > Pages > Source: GitHub Actions**, then rerun the
failed workflow. The next run succeeded.

### Supabase user recreated but login still fails

Confirm all of the following:

- the email spelling is exact;
- the password is the newly assigned password;
- the user is confirmed in Authentication;
- a matching `profiles` record exists;
- `profiles.active` is `true`;
- both clients point to project `zspqzluwzuiasabiphdd`;
- the device has internet access.

## 12. Security and operational recommendations

- Rotate any database or account password that was exposed in chat/screenshots.
- Disable open public sign-up unless the municipality deliberately wants it.
- Give every colleague an individual account; never share the administrator login.
- Assign the least-privileged role appropriate to each person.
- Remove/deactivate access promptly when a colleague leaves or changes duties.
- Keep the service-role key only in a trusted server-side environment.
- Use a server-side Supabase Edge Function for future in-app user creation/reset.
- Treat a debug APK as a test build. Create a signed release APK/AAB before formal
  organizational distribution.
- Protect the GitHub and Supabase owner accounts with multi-factor authentication.
- Review Supabase logs, audit records, storage usage, and free-tier limits regularly.
- Plan backups and data-retention rules before production use.

## 13. Routine maintenance

When changing the application:

1. Test locally.
2. Commit only intended files and do not commit secrets or `local.properties`.
3. Push to GitHub.
4. Confirm both relevant GitHub Actions workflows succeed.
5. Verify the live web application.
6. Build/install the new Android version when Android code changes.
7. Apply database changes as numbered migrations, not by editing production data
   manually without a record.

This document deliberately excludes all passwords and secret keys.
