# Firebase shared world setup

Focus Raid reads the shared raid snapshot from Firebase and can share preset-only footprints while keeping the focus timer and progression fully local.

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
5. after a completed session, nearby preset footprints are fetched asynchronously
6. a selected footprint is written without blocking the VICTORY screen
7. after a result is dismissed and the app returns to READY, it refreshes the shared snapshot again

There is no realtime Firestore listener during focus. RUNNING and PAUSED remain isolated from network-driven world changes.

If authentication or Firestore fails, the timer and local progression continue. Cloud state is never allowed to block starting or completing a focus session.

When a real Firebase backend is configured, failed footprint reads do **not** fall back to fake seeded users. An empty or failed remote read must never make Focus Raid pretend that other people are present.

## Footprint data model

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

The anonymous Firebase UID is used as the document ID, which means one anonymous user owns at most one footprint document per checkpoint. Updating the preset changes only `presetId`; the original `createdAt` value is preserved so repeatedly changing the message cannot push the same user back to the top of the latest-footprints list.

The UI reads at most the newest three entries for the current checkpoint.

## Security rules

`firestore.rules` enforces the social boundary rather than trusting only the Android client:

- reads require Firebase Authentication
- `world/**` remains client read-only
- footprint writes require the document ID to equal the signed-in anonymous UID
- only the known preset IDs are accepted
- only `presetId` and `createdAt` may exist in a footprint document
- `createdAt` must be the server request time on create
- updates must preserve the original `createdAt`
- footprint deletion is denied
- all unmatched paths are denied by default

Deploy rules with your normal Firebase workflow before distributing a build that has Firebase credentials.

## Shared-world write policy

The Android client remains read-only for authoritative shared-world values.

`firestore.rules` denies client writes to `world/**`. Future raid contribution aggregation, if added, should go through a trusted backend such as Cloud Run rather than allowing clients to mutate boss HP or global counters directly.

Preset footprints are the deliberate exception because the accepted values are tightly constrained by Firestore Security Rules and do not contain free-form user content.
