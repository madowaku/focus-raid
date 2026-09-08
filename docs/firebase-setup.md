# Firebase shared world setup

Focus Raid reads the shared raid snapshot from Firebase and can share preset-only footprints while keeping the focus timer and progression fully local.

The Android build intentionally does not require `google-services.json` at runtime. Firebase is initialized explicitly from three build values so CI and local preview builds can continue to run without backend credentials.

## Production checklist

Before a public Google Play release, all of these must be complete:

- create/select the production Firebase project
- register Android package `com.madowaku.focusraid`
- enable Firebase Authentication anonymous sign-in
- create the default Cloud Firestore database
- create `world/current`
- deploy and verify this repository's `firestore.rules`
- import the Firebase Android config into local Gradle properties
- verify a real device can read the world and read/write preset Footprints
- register Firebase App Check with Play Integrity
- verify App Check metrics from an internal-test Play build
- enable App Check enforcement for Cloud Firestore and Authentication only after valid production traffic is confirmed

Do not enable enforcement before the internal-test build is successfully producing valid App Check tokens.

## 1. Create the Firebase project and Android app

In Firebase Console, create a dedicated production project for Focus Raid or select the intended existing project.

Register an Android app with the exact package/application ID:

```text
com.madowaku.focusraid
```

The package name is case-sensitive and cannot be changed for that Firebase Android app after registration.

Download the generated `google-services.json`. Focus Raid does **not** commit or load this file directly; the helper script can use it once as a convenient source for the three identifiers the app already expects.

## 2. Enable Authentication

In Firebase Console:

```text
Security → Authentication → Sign-in method → Anonymous → Enable
```

Focus Raid does not create email/password or social accounts. The anonymous Firebase UID is used only to authenticate protected backend requests and to own one Footprint document per checkpoint.

If Identity Platform automatic cleanup for old anonymous users is enabled later, evaluate the effect on old Footprint ownership before turning it on. Deleting an Auth account does not automatically delete its Firestore Footprint documents.

## 3. Create Cloud Firestore

Create the default Cloud Firestore database. Use production rules rather than open/test-mode rules before distributing the app.

The client reads the shared world document:

```text
world/current
```

Suggested initial fields:

```json
{
  "focusNow": 4218,
  "bossName": "灰燼竜ヴォルガ",
  "bossHp": 428192,
  "bossMaxHp": 650000,
  "raidParticipants": 12481,
  "towerFloor": 4281,
  "abyssDepth": 12481,
  "armoryReady": 68
}
```

Numeric Firestore values are mapped defensively. Missing or invalid fields fall back to local preview values rather than breaking the timer UI.

## 4. Import the Android Firebase identifiers locally

From the repository root on Windows PowerShell, after downloading `google-services.json`:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\configure-firebase.ps1 `
  -GoogleServicesJson "C:\path\to\google-services.json"
```

The script verifies that the file contains an Android client for:

```text
com.madowaku.focusraid
```

It extracts and stores these values in the current user's `~/.gradle/gradle.properties`:

```text
FOCUS_RAID_FIREBASE_PROJECT_ID
FOCUS_RAID_FIREBASE_API_KEY
FOCUS_RAID_FIREBASE_APP_ID
```

The API key in Firebase Android configuration is not treated as a server secret, but project-specific configuration should still not be pasted into issue trackers or committed merely to make builds work.

The app-level Gradle build reads the values from Gradle properties or environment variables. If any of the three are absent, `WorldRepositoryFactory` deliberately selects `FakeWorldRepository`.

## 5. Deploy Firestore Security Rules

This repository owns the production rules source:

```text
firestore.rules
```

and maps it through:

```text
firebase.json
```

Deploy only the Firestore rules from the repository root:

```powershell
firebase login
firebase deploy --only firestore:rules --project YOUR_FIREBASE_PROJECT_ID
```

Firebase CLI deployments overwrite the active Firestore rules with the rules from the repository, so avoid maintaining a different long-lived copy only in the Firebase Console.

The current rules enforce:

- authenticated reads only
- `world/**` client writes are always denied
- only `TOWER`, `ABYSS`, and `STAR_ROUTE` Footprint paths are accepted
- Footprint document ID must equal the signed-in UID
- only known preset IDs are accepted
- only `presetId` and `createdAt` may exist
- `createdAt` must equal server request time on create
- updates may change the preset but must preserve the original `createdAt`
- deletes are denied
- every other unmatched path is denied by default

There is intentionally no generic writable `/users/**` rule in production.

## 6. Footprint data model

Remote footprints use this path:

```text
footprints/{EXPEDITION}/checkpoints/{CHECKPOINT}/entries/{ANONYMOUS_UID}
```

Example:

```text
footprints/TOWER/checkpoints/4281/entries/abc123...
```

Each entry stores only:

```json
{
  "presetId": "keep_going",
  "createdAt": "server timestamp"
}
```

The displayed glyph and text are reconstructed from the app's built-in `FootprintPresets`. Free-form text is never stored in Firestore.

The anonymous Firebase UID is used as the document ID, so one anonymous user owns at most one Footprint document per checkpoint. Updating the preset does not refresh `createdAt`, preventing repeated edits from pushing the same user back to the top of the latest-footprints list.

The UI reads at most the newest three entries for the current checkpoint.

## 7. Firebase App Check

Release builds use Firebase App Check with the Play Integrity provider. Debug builds use the Firebase App Check debug provider so local emulator/device development can continue after enforcement is enabled.

Focus Raid initializes App Check for the same named Firebase app (`focus-raid-remote`) before obtaining Firebase Auth or Firestore instances.

### Register Play Integrity App Check

For the production Android app:

1. In Google Play Console, open Focus Raid and link the Play Integrity API to the same Google Cloud project backing the Firebase project.
2. In Firebase Console, open `Security → App Check`.
3. Register the Android app with the Play Integrity provider.
4. Add the required SHA-256 signing certificate fingerprint.
5. Distribute an internal-test build through Google Play.
6. Confirm App Check metrics show valid requests from the Play-installed build.
7. Only then enable enforcement for Cloud Firestore and Authentication.

For a Google-Play-only release, start with Firebase's recommended Play Integrity defaults rather than adding unusually strict device-integrity requirements.

### Debug token

When a debug build uses the real Firebase backend, the debug App Check provider logs a debug token. Register that token under:

```text
Firebase Console → Security → App Check → Manage debug tokens
```

Never commit or publish a debug token. Delete it in Firebase Console if compromised.

## 8. Runtime behavior

When Firebase configuration is present:

1. the app restores the local focus session first
2. if no focus session is running or paused, it signs in anonymously
3. it performs a one-shot server read of `world/current`
4. the shared world snapshot replaces the local preview snapshot
5. after a completed session, nearby preset Footprints are fetched asynchronously
6. a selected Footprint is written without blocking the VICTORY screen
7. after a result is dismissed and the app returns to READY, it refreshes the shared snapshot again

There is no realtime Firestore listener during focus. RUNNING and PAUSED remain isolated from network-driven world changes.

If Authentication, App Check, or Firestore fails, the timer and local progression continue. Cloud state is never allowed to block starting or completing a focus session.

When a real Firebase backend is configured, failed Footprint reads do **not** fall back to fake seeded users. An empty or failed remote read must never make Focus Raid pretend that other people are present.

## 9. Production smoke test

Before public release, verify from a Play-installed internal-test build:

1. clean install starts without Firebase crash
2. anonymous sign-in succeeds
3. shared world status becomes LIVE
4. `world/current` values appear
5. complete a short test focus session
6. VICTORY appears before network work finishes
7. Footprints load for the reached checkpoint
8. post one preset Footprint
9. change the preset and confirm its original timestamp/ordering is preserved
10. confirm a second test account/device can read it
11. airplane mode leaves timer/history usable
12. restore network and confirm the world can refresh again
13. confirm App Check metrics classify the Play build as valid
14. after enforcement, repeat world read + Footprint read/write

## Shared-world write policy

The Android client remains read-only for authoritative shared-world values.

`firestore.rules` denies client writes to `world/**`. Future raid contribution aggregation, if added, should go through a trusted backend such as Cloud Run rather than allowing clients to mutate boss HP or global counters directly.

Preset Footprints are the deliberate exception because accepted values are tightly constrained by Security Rules, protected by Auth, and additionally guarded by App Check in production.
