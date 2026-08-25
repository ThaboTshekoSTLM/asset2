# ICT Asset Register: complete project blueprint

Document owner: Municipal ICT  
System: ICT Asset Register  
Repository: `ThaboTshekoSTLM/asset2`  
Production branch: `master`  
Last reviewed: 25 August 2026

## 1. Purpose

The ICT Asset Register records municipal ICT equipment from initial capture through
allocation, transfer, repair, return, and disposal. It gives field staff an Android
application, office users a browser application, and authorized auditors a shared
view of the same centrally secured information.

The system is designed to answer these questions:

- What ICT assets does the municipality own?
- What are the barcode and serial number of each asset?
- Where is each asset currently located?
- Who currently has custody of it?
- Who registered or moved it, when, and why?
- What movement history belongs to a particular asset?
- Is there photographic evidence for the asset?
- Which assets belong to a department, building, or employee?

## 2. Scope

### Included

- Email/password authentication.
- Role-based authorization and active/inactive user profiles.
- Asset registration with unique barcode and serial number.
- Camera capture and low-bandwidth private photo storage.
- Barcode and serial-number scanning on Android.
- Browser serial scanning where `BarcodeDetector` is supported.
- Search by an individual asset's barcode or serial number.
- Authenticated asset detail view with its photograph.
- Asset movement history and current-location updates.
- Dashboard summaries and recent movements.
- Operational reports and downloadable exports.
- Administrator-only asset editing and soft deletion/archive on the web.
- Audit records for database asset and movement changes.
- Android APK builds and GitHub Pages deployment through GitHub Actions.
- Local demonstration mode for the web application when Supabase is not configured.

### Not yet included as a complete production capability

- Administrator user creation or password reset through a trusted server function.
- Automatic in-app Android updating.
- A signed release APK/AAB distribution channel.
- Full bidirectional offline synchronization and conflict resolution.
- Reference-data administration for departments, buildings, sections, and rooms.
- Automated database backups, retention enforcement, and restore drills.
- Push notifications, approval workflows, or asset-label printing.
- Automated test coverage and production monitoring alerts.

## 3. System context

```text
Municipal staff
   |-- Android phone --> Native Android application ------+
   |                                                       |
   +-- Desktop/mobile browser --> GitHub Pages web app ----+--> Supabase Auth
                                                           +--> PostgreSQL/Data API
                                                           +--> Private Storage

Developers/administrators --> GitHub repository --> GitHub Actions
                                                   |-- GitHub Pages deployment
                                                   +-- Debug APK artifact
```

Production endpoints:

| Component | Location |
| --- | --- |
| Web application | <https://thabotshekostlm.github.io/asset2/> |
| Source repository | <https://github.com/ThaboTshekoSTLM/asset2> |
| Supabase project | `zspqzluwzuiasabiphdd` |
| Android package | `za.gov.municipal.ictasset` |

Supabase is the production source of truth. The public web files are hosted by
GitHub Pages; no privileged server key is stored in either client.

## 4. Repository map

| Path | Responsibility |
| --- | --- |
| `app/` | Android application module |
| `app/src/main/java/.../data` | Local database, remote API, repository implementations |
| `app/src/main/java/.../domain` | Models, repository contracts, and use cases |
| `app/src/main/java/.../presentation` | Compose screens, view models, navigation, session UI |
| `app/src/main/res` | Android resources and FileProvider configuration |
| `web/index.html` | Web application structure and forms |
| `web/app.js` | UI state, rendering, forms, search, reports, and local fallback |
| `web/backend.js` | Supabase Auth, Data API, Storage, compression, and data mapping |
| `web/styles.css` | Responsive visual design |
| `web/config.js` | Production Supabase URL and public browser key |
| `supabase/schema.sql` | Complete target database schema and security definition |
| `supabase/migrations/` | Incremental production database changes |
| `.github/workflows/android-build.yml` | Debug APK continuous integration build |
| `.github/workflows/web-pages.yml` | Static web deployment to GitHub Pages |
| `docs/PROJECT_SETUP.md` | Setup, deployment, administration, and troubleshooting record |
| `docs/PROJECT_BLUEPRINT.md` | This architecture and operational source of truth |

## 5. Technology stack

### Android

- Kotlin and Java 17 bytecode.
- Android SDK 35; minimum supported Android SDK 26.
- Jetpack Compose Material 3 UI.
- Navigation Compose.
- Kotlin coroutines and `Flow`.
- Room 2.6.1 for local/reference and legacy offline data.
- CameraX 1.4.0 for camera access.
- ML Kit barcode scanning 17.3.0.
- Direct HTTPS calls to Supabase Auth, PostgREST, RPC, and Storage.
- Local PDF and Excel-compatible XML report export.

### Web

- Static HTML, CSS, and vanilla JavaScript.
- Supabase JavaScript browser client.
- Browser Canvas and `createImageBitmap` for photo compression.
- Browser `BarcodeDetector` when available.
- CSV report download.
- `localStorage` demonstration fallback.

### Backend and delivery

- Supabase Authentication.
- PostgreSQL with Row Level Security.
- Supabase Data API and PostgreSQL RPC.
- Private Supabase Storage bucket named `asset-photos`.
- GitHub repository and GitHub Actions.
- GitHub Pages static hosting.

## 6. Android architecture

The Android project follows a practical data/domain/presentation split.

```text
Compose screen
    --> ViewModel
        --> Use case or repository interface
            --> Supabase repository
                --> SupabaseApi --> Auth / REST / RPC / Storage
            --> Room DAOs (reference data and retained offline implementation)
```

### Dependency composition

`AppContainer` constructs the application dependencies:

- `SupabaseApi` reads `BuildConfig.SUPABASE_URL` and
  `BuildConfig.SUPABASE_PUBLISHABLE_KEY`.
- `SupabaseAuthRepository` handles sign-in and profile loading.
- `SupabaseAssetRepository` is the active asset repository.
- `SupabaseReportRepository` builds reports from the active remote asset state.
- `OfflineReferenceRepository` supplies locally stored departments/buildings/rooms.
- `SessionManager` owns the signed-in application session and sign-out callback.
- `AppViewModelFactory` injects dependencies into view models.

The `OfflineAssetRepository`, `OfflineAuthRepository`, and
`OfflineReportRepository` remain in the codebase as local implementations, but the
production dependency container selects the Supabase implementations.

### Android navigation

| Route | Purpose |
| --- | --- |
| `dashboard` | Counts, recent activity, and navigation entry point |
| `dashboardStatus/{type}` | Filtered dashboard status list |
| `search` | Search by barcode or serial number |
| `history/{assetId}` | Individual asset and movement history |
| `register` | Capture asset details and photo |
| `movement` | Allocate, transfer, return, repair, or dispose |
| `reports` | Build and export reports |
| `users` | User view/management surface |
| `scanner/{target}` | Camera scanner reused by asset, serial, room, move, and search flows |

Scanner result keys distinguish registration barcode, registration serial, room,
movement barcode, movement room, and search scanning.

### Android data/session behavior

- Authentication tokens and the Supabase user UUID are stored in private Android
  `SharedPreferences` named `supabase_session`.
- Sign-out clears that preference file.
- Asset and movement lists are refreshed from Supabase after authentication.
- Remote information is held in memory as Kotlin `StateFlow` values.
- Photo downloads are cached under the application's cache directory.
- Production use therefore requires internet connectivity for authoritative data.
- Room does not currently queue and synchronize production asset mutations.

## 7. Web architecture

The web application has two operating modes.

### Production remote mode

When `web/config.js` contains a Supabase URL and public key, `backend.js` creates a
Supabase client. Authentication, assets, movements, and photos come from Supabase.
Database RLS remains the final authority regardless of what buttons the UI displays.

### Demonstration local mode

When Supabase configuration is absent, `app.js` loads seed data and saves changes in
browser `localStorage` under `ict-web-register-state-v1`. This mode is isolated to
one browser profile and is not shared, backed up, or suitable for production.

The seed users and passwords embedded in `app.js` are demonstration credentials
only. They do not authenticate against Supabase.

### Web screens and capabilities

| Screen | Capability |
| --- | --- |
| Dashboard | Asset totals, movement totals, status summaries, recent activity |
| Assets/Search | Exact working search surface for barcode or serial; detail dialog and photo |
| Register | Asset capture, serial scan image, optional asset photo |
| Move | Select asset and record its new owner/location/movement |
| Reports | Preview and download CSV reports |
| Users | Admin-only local-mode user management UI |

Admin asset controls appear in the individual asset detail dialog. Edit can replace
asset data and optionally the photo. Delete performs a soft archive by setting
`deleted_at`, `deleted_by`, and `updated_by`; it intentionally preserves the asset
row, movement history, and audit history.

## 8. Domain model

### User roles

| Application label | Database value | Read assets | Register/move | Reports | Edit/archive assets | Manage profiles |
| --- | --- | ---: | ---: | ---: | ---: | ---: |
| Admin | `admin` | Yes | Yes | Yes | Yes | Yes |
| Standard User | `standard_user` | Yes | Yes | Yes | No | No |
| ICT Technician | `ict_technician` | Yes | Yes | Yes | No | No |
| Viewer / Auditor | `viewer_auditor` | Yes | No | Yes | No | No |

Inactive profiles are excluded by the role helper functions and cannot receive
writer or administrator privileges.

### Movement types

| Value | Meaning |
| --- | --- |
| `new_allocation` | First assignment to a custodian/location |
| `transfer` | Transfer to a different custodian or location |
| `return` | Return from a custodian |
| `repair` | Movement for repair or maintenance |
| `disposal` | Movement into the disposal process |

### Core asset fields

The asset record contains identity, classification, location, custody, evidence,
movement summary, archival state, and audit attribution:

- Device description.
- Unique uppercase asset barcode.
- Unique uppercase serial number.
- Department and section.
- Building, office number, and room barcode.
- Current and previous owner.
- Registering/latest technician.
- Registration and latest movement timestamps.
- Current/latest movement type.
- Notes and private photo object path.
- Soft-deletion timestamp and deleting administrator.
- Creating/updating user and timestamps.

## 9. Database blueprint

### Entity relationship

```text
auth.users (1) ---- (1) profiles
                         | created_by / updated_by / deleted_by
                         v
                      assets (1) ---- (*) asset_movements
                         |                    |
                         +--------------------+--> audit trigger events

profiles (1) ---- (*) asset_movements.created_by
profiles (1) ---- (*) audit_logs.actor_user_id
assets.photo_path ----> storage.objects in private asset-photos bucket
```

### `profiles`

One application profile per Supabase Auth user. The UUID is the Auth user UUID.
Username is lowercase and unique. Role defaults to Standard User. The Auth insertion
trigger creates the profile from user metadata or the email prefix.

### `assets`

The authoritative current state of every active or archived asset. Barcode and
serial number have database unique constraints. Normal authenticated reads include
only rows where `deleted_at is null`.

### `asset_movements`

Append-oriented custody/location history. Every row points to an asset using a
restricted foreign key, preventing physical asset deletion while movement history
exists. It stores both previous and new context so historical reports do not depend
only on the asset's current values.

### `audit_logs`

Immutable-by-client event records created by triggers after insert, update, or
delete operations on assets and movements. Details contain the affected row as
JSON. Only administrators and viewer/auditors may select audit records.

### Storage

`asset-photos` is private. Clients store the object path—not a permanent public URL—
in `assets.photo_path`. Authenticated clients request a signed URL or download the
object using their session.

### Indexes

- Assets by department.
- Assets by building.
- Assets by current owner.
- Movements by asset and newest movement date.
- Audit events by newest creation date.

## 10. Database functions and triggers

| Object | Purpose |
| --- | --- |
| `current_profile_role()` | Returns the active signed-in user's database role |
| `can_write_assets()` | Allows Admin, Standard User, and ICT Technician operations |
| `is_admin()` | Central administrator authorization check |
| `touch_updated_at()` | Maintains profile and asset update timestamps |
| `handle_new_auth_user()` | Creates the matching profile after Auth user creation |
| `audit_row_change()` | Writes asset/movement row changes to `audit_logs` |
| `record_asset_movement(...)` | Creates movement and updates current asset state atomically |

The movement RPC locks the selected asset row, records its previous context, inserts
the movement, and updates ownership/location within one transaction.

## 11. Security blueprint

### Security boundary

The web and Android apps are untrusted clients. UI role checks improve usability,
but PostgreSQL RLS and Storage policies enforce the actual permissions.

### Data RLS

- Active authenticated users can read non-archived assets and movement history.
- Writers can insert new assets and movements only while attributing their own UUID.
- Only administrators can directly update, archive, or physically delete assets.
- Only administrators can update profiles.
- Only administrators and auditors can read audit logs.

### Storage policies

- Authenticated users can read private asset photos.
- writers can upload or update asset photos.
- Only administrators can delete photo objects.

### Secret handling

- The Supabase public/publishable key is expected in client applications and is
  constrained by RLS.
- The Supabase service-role key must never be placed in Git, the APK, web JavaScript,
  screenshots, or client configuration.
- Production passwords must never be committed or documented.
- GitHub Actions reads Android Supabase configuration from repository secrets.

### Known security considerations

- `web/config.js` is public by design; only the publishable key belongs there.
- Local demo credentials in `app.js` should never be presented as production users.
- User account creation/password reset needs a trusted administrative backend such
  as a Supabase Edge Function before it can be safely offered inside the apps.
- Organization owner accounts should use multi-factor authentication.

## 12. Principal workflows

### Sign in

1. User enters email or username and password.
2. A username is converted to the configured local email form where applicable.
3. Supabase Auth returns a session token and Auth user UUID.
4. The client fetches the matching active profile.
5. The role controls visible operations; RLS independently controls backend access.
6. Missing/inactive profiles are reported as invalid or inactive users.

### Register an asset

1. Writer opens Register.
2. Barcode/serial are typed or scanned and normalized to uppercase.
3. Client checks currently loaded assets for an obvious duplicate.
4. Optional photo is captured/selected.
5. Photo is resized to a maximum 960-pixel longest side and compressed as JPEG.
6. The asset is inserted with the signed-in user as `created_by`.
7. The photo is uploaded to private Storage and its path is associated with the asset.
8. Database uniqueness remains the final duplicate protection.

Android JPEG quality is 48. The web uses compressed JPEG output from Canvas. These
settings favor low bandwidth and report/detail viewing rather than archival-quality
photography.

### Search and view

1. User searches or scans a barcode/serial number.
2. The client filters the authenticated non-archived asset collection.
3. Selecting the individual asset opens its complete detail.
4. The private photo is retrieved only for that detail context.
5. Movement history remains connected to the selected asset.

### Record movement

1. Writer selects/scans the asset.
2. User supplies new custodian, location, movement type, reason, technician, and confirmation.
3. Client calls `record_asset_movement`.
4. PostgreSQL creates the history row and updates current asset state atomically.
5. Audit triggers record both mutations.

### Administrator edit/archive

1. Administrator opens an individual asset on the web.
2. Edit validates barcode/serial uniqueness and updates the row with `updated_by`.
3. An optional replacement photo is compressed and uploaded privately.
4. Delete requests confirmation and sets soft-deletion metadata.
5. Normal asset queries stop returning the archived row, while movement and audit
   history remain intact.

### Reporting

Current report concepts include:

- Complete asset register/all asset fields.
- Assets per department.
- Assets per building.
- Assets allocated to a user.
- Movements performed by a technician.
- Movements within a date range.

The web previews tables and exports CSV. Android builds tabular reports and exports
PDF or Excel-compatible XML. Photos are deliberately shown in individual asset
detail rather than bulk reports to limit file size and protect evidence access.

## 13. Build and configuration

### Android local configuration

Create untracked `local.properties` values:

```properties
SUPABASE_URL=https://YOUR_PROJECT.supabase.co
SUPABASE_PUBLISHABLE_KEY=YOUR_PUBLIC_PUBLISHABLE_KEY
```

Build:

```powershell
.\gradlew.bat assembleDebug
```

Output:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Android application version is currently `versionCode = 1`, `versionName = 1.0`.
Any distributed Android change requires a new APK installation unless a formal app
distribution/update mechanism is introduced. Future releases must increment the
version code and should use a protected release-signing key.

### Web local configuration

`web/config.js` uses this structure:

```javascript
window.ICT_ASSET_CONFIG = {
  supabaseUrl: "https://YOUR_PROJECT.supabase.co",
  supabaseAnonKey: "YOUR_PUBLIC_PUBLISHABLE_KEY"
};
```

Serve locally over HTTP:

```powershell
python -m http.server 8766 --directory web
```

Then open <http://localhost:8766/>.

## 14. Continuous integration and deployment

### Web

Pushes to `main` or `master` that touch `web/**` trigger `Web App Pages Deploy`.
The workflow uploads the complete `web` folder and deploys it to GitHub Pages.

### Android

Pushes and pull requests to `main` or `master` trigger `Android APK Build`. The
workflow configures Java 17/Android SDK 35, injects Supabase GitHub secrets, builds a
debug APK, and uploads artifact `ict-asset-register-debug-apk`.

The APK artifact is not an automatic phone update. Users must install the newly
built APK over the existing application. A stable signing identity is required for
Android to accept an update without uninstalling the existing app.

### Database

Database changes are not automatically deployed by the current workflows. Apply
numbered SQL migrations to Supabase, verify their result, and retain them in Git.
`schema.sql` must describe the complete desired state for a fresh installation.

## 15. Operational administration

### Create a user

1. Create the Auth user in Supabase Authentication.
2. Confirm the profile trigger created `public.profiles`.
3. Set the correct role and `active = true`.
4. Provide an individual temporary password through an approved private channel.
5. Never share an administrator account between staff members.

### Deactivate a user

Set `profiles.active = false`. The role helper functions stop granting application
permissions. For a complete access response, also invalidate sessions or disable the
Auth account in Supabase.

### Reset a password

Use Supabase Authentication administrator controls until a secure server-side reset
workflow exists. The public browser must not receive service-role credentials.

### Archive an asset

Use the administrator Delete action in the web detail dialog. This is recoverable by
clearing archive metadata through an authorized database operation. It does not
remove movements, audit rows, or the stored photo.

## 16. Validation checklist

Before each production release:

- Android and web point to project `zspqzluwzuiasabiphdd`.
- Database migrations have been applied and verified.
- Web JavaScript syntax checks pass.
- Android Gradle build passes.
- A normal writer can register one uniquely tagged test asset.
- Duplicate barcode and duplicate serial attempts are rejected.
- The asset appears in both clients after refresh.
- Its photo appears only in authenticated individual detail.
- Movement updates current custody and creates history.
- Auditor cannot write.
- Non-admin cannot edit/archive assets through UI or direct API.
- Admin can edit and archive; archived asset disappears from normal search.
- Audit entries exist for the test mutations.
- GitHub Pages deployment and Android build workflows succeed.
- Test data is archived or clearly labeled after validation.

## 17. Failure modes and troubleshooting

| Symptom | Likely cause | Check |
| --- | --- | --- |
| Invalid/inactive user | Wrong password, missing profile, inactive profile, wrong project | Auth user, profile UUID, role, active flag, client URL |
| Duplicate barcode constraint | Barcode already exists or wrong QR was scanned | Search exact barcode; scan municipal tag |
| Duplicate serial constraint | Serial is already assigned | Search exact serial before capture |
| Photo captures but does not appear | Upload failed, missing path, storage policy/session issue | `photo_path`, bucket object, signed-in session, Storage policy |
| Web shows old behavior | GitHub Pages deployment/cache delay | Actions status, hard refresh, deployed commit |
| Android shows old behavior | Old APK remains installed | Build/download/install new APK and verify version |
| Asset move fails | Inactive/non-writer role or invalid asset | Profile role, RPC grant, RLS, asset archive status |
| Data differs between clients | Stale in-memory data or different project configuration | Refresh/relogin; compare Supabase URL |
| Local web data is not shared | App is in demonstration mode | Configure `web/config.js` and use HTTP hosting |

## 18. Data governance and continuity

Before formal production adoption, the municipality should approve:

- Authoritative asset ownership and data-steward roles.
- Mandatory fields and standardized department/building naming.
- Barcode and serial-number correction procedure.
- Photo consent, retention, resolution, and acceptable-content rules.
- Disposal record retention period.
- Audit-log retention and review frequency.
- Backup schedule, restore responsibility, and restore testing.
- Account onboarding/offboarding service levels.
- Incident response for lost phones and exposed credentials.
- GitHub/Supabase ownership succession and emergency access.

## 19. Prioritized roadmap

### Priority 1: production safety

1. Verify every production migration and record the applied version.
2. Introduce signed release builds, version increments, and protected signing keys.
3. Disable unwanted public signup and enforce individual accounts/MFA for owners.
4. Configure backup/restore procedures and test a recovery.
5. Add automated unit, database-policy, and end-to-end tests.

### Priority 2: administration

1. Add a server-side Edge Function for account creation, role changes, deactivation,
   password-reset initiation, and session revocation.
2. Add an administrator archive/recovery screen and archive reason.
3. Add controlled reference-data management.
4. Add audit-log review and export screens.

### Priority 3: field reliability

1. Implement offline mutation queues, retry status, and conflict handling.
2. Add explicit sync/refresh indicators and last-synchronized time.
3. Add scanning-quality feedback and supported symbology rules.
4. Add managed Android distribution or an approved update channel.

### Priority 4: reporting and lifecycle

1. Add scheduled reports and approval workflows.
2. Add asset condition, procurement, warranty, cost, and disposal certificate fields.
3. Add label printing and stocktake/reconciliation campaigns.
4. Add monitoring alerts for authentication, database, storage, and deployment failures.

## 20. Change-control rules

- Make database changes through numbered migrations and update `schema.sql`.
- Preserve unique barcode and serial constraints.
- Preserve audit and movement history; prefer archival over physical deletion.
- Treat RLS as mandatory and test every role after policy changes.
- Never commit passwords, service-role keys, private signing keys, or `local.properties`.
- Increment Android versions for every distributed build.
- Test locally, commit only intended files, push, verify Actions, and smoke-test both
  production clients after deployment.
- Update this blueprint whenever architecture, permissions, data fields, deployment,
  or major workflows change.

## 21. Definition of done for future features

A feature is complete only when:

1. User behavior and role permissions are specified.
2. Database/RLS changes are captured in a migration.
3. Android and web behavior are aligned where required.
4. Error and offline/slow-network behavior are handled.
5. Security and audit implications are reviewed.
6. Automated or repeatable manual tests pass.
7. Deployment succeeds and production is smoke-tested.
8. Setup documentation and this blueprint are updated.

This blueprint intentionally contains no passwords, private keys, or service-role
credentials.
