# Firebase shared world setup

Focus Raid can now read the shared raid snapshot from Firebase while keeping the focus timer fully local.

The Android build intentionally does not require `google-services.json`. Firebase is initialized explicitly from three build values so CI and local preview builds can continue to run without backend credentials.

## 1. Create or select a Firebase project

Enable:

- Firebase Authentication
- Anonymous sign-in
- Cloud Firestore

Deploy `firestore.rules` before exposing the project to users.

## 2. Create `world/current`

Create this Firestore document:

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

Numeric Firestore values are mapped defensively. Missing or invalid fields fall back to the local preview values rather than breaking the timer UI.

## 3. Configure the Android build

Provide these values through Gradle properties or environment variables:

```text
FOCUS_RAID_FIREBASE_PROJECT_ID
FOCUS_RAID_FIREBASE_API_KEY
FOCUS_RAID_FIREBASE_APP_ID
```

A convenient local option is `~/.gradle/gradle.properties`:

```properties
FOCUS_RAID_FIREBASE_PROJECT_ID=your-project-id
FOCUS_RAID_FIREBASE_API_KEY=your-web-or-android-api-key
FOCUS_RAID_FIREBASE_APP_ID=1:1234567890:android:example
```

Do not commit account-specific configuration just to make preview builds work. If any of the three values are absent, `WorldRepositoryFactory` deliberately selects `FakeWorldRepository`.

## Runtime behavior

When Firebase configuration is present:

1. the app restores the local focus session first
2. if no focus session is running or paused, it signs in anonymously
3. it performs a one-shot server read of `world/current`
4. the shared world snapshot replaces the local preview snapshot
5. after a result is dismissed and the app returns to READY, it refreshes the shared snapshot again

There is no realtime Firestore listener during focus. RUNNING and PAUSED remain isolated from network-driven world changes.

If authentication or Firestore fails, the timer and local progression continue with the preview snapshot and the repository reports `OFFLINE`. Cloud state is never allowed to block starting or completing a focus session.

## Current write policy

The Android client is read-only for authoritative shared-world data.

`firestore.rules` denies client writes to `world/**`. Future raid contributions should go through Cloud Run endpoints such as `finishFocus` / `submitRaid`, then be aggregated into world state server-side.

Footprints still use the local repository path in this phase. Remote footprint reads/writes and server validation are a separate follow-up so free-form or forged social content never becomes an accidental client-trust surface.
