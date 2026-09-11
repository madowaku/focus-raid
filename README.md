# Focus Raid

Focus Raid is an Android-native focus timer where real-world concentration powers a shared fantasy world.

## Product principles

- Start focusing within five seconds.
- Focus time is never rolled back.
- Your companion grows from time spent together, not paid power.
- World systems reward cooperation, not rankings.
- During a focus session, the app becomes quiet.
- The world can be playful; the timer must remain useful.

## Android v1 direction

Focus Raid v1 is intentionally Android-only.

- Kotlin
- Jetpack Compose
- Material 3 Expressive
- Wall-clock based resilient timer
- AlarmManager completion notification
- Preferences DataStore session recovery
- Room session history
- Pure Kotlin domain rules
- Optional Firebase Auth + Firestore shared-world reads
- Cloud Run remains the planned boundary for authoritative world writes

The previous React/Vite PWA prototype is preserved on the `archive/pwa-mvp-v0.1` branch.

## Implemented vertical slice

- READY: duration selection, expedition selection, large timer hero, raid CTA, current raid card
- RAID: 208dp timer hero, pause/resume, early return, quiet reduced-information layout
- VICTORY / ABORTED: credited focus time remains meaningful whether the session completes or ends early
- Session restoration after process death using an absolute end timestamp
- Exact completion alarm when exact alarms are permitted, graceful inexact fallback otherwise
- Reboot / app-update alarm restoration and a four-case durability CI gate
- Notification and exact-alarm education before optional system access
- Adventure Log backed by Room
- Today focus minutes and streak derived from actual local history
- Fixed-message Footprints for lightweight asynchronous interaction
- Companion growth from egg to mature form using cumulative focus minutes
- Distinct Compose Canvas silhouettes for egg / hatchling / first growth / second growth / mature
- `RAG EVOLVED!` result reveal whenever credited focus crosses a growth threshold
- Anonymous Firebase Auth + one-shot Firestore `world/current` read when backend config is present
- Local preview fallback when Firebase is unavailable or unconfigured
- Shared-world refresh only outside active RUNNING / PAUSED sessions
- Home / Raid / Companion / Log bottom navigation
- Pure Kotlin domain tests
- 26 Android emulator visual QA screenshots across 360×800 and 720×1280, including evolution

## Companion growth

Rag starts as an egg on a fresh install and visually evolves as credited focus time accumulates.

- 0 min: egg
- 75 min: hatchling
- 720 min: first growth
- 1,800 min: second growth
- 4,500 min: mature

The Companion tab shows the current form, progress to the next form, time spent together, today's contribution, and a five-form progression strip. Future forms stay dimmed until unlocked.

When a completed or partially credited session crosses a growth threshold, the result state records the old and new form. A rare `RAG EVOLVED!` card briefly shows the previous silhouette before revealing the newly unlocked form. Evolution never depends on streaks or paid acceleration.

## Shared world

The Android client can initialize Firebase without committing `google-services.json`.

Provide these values as Gradle properties or environment variables:

```text
FOCUS_RAID_FIREBASE_PROJECT_ID
FOCUS_RAID_FIREBASE_API_KEY
FOCUS_RAID_FIREBASE_APP_ID
```

When all three exist, Focus Raid signs in anonymously and performs a one-shot server read of `world/current` while the app is outside an active focus session. Missing or failed backend configuration falls back to `FakeWorldRepository`, so cloud availability never blocks the timer.

`WorldRepository` exposes both the latest snapshot and a sync status (`LOCAL_PREVIEW`, `CONNECTING`, `LIVE`, `OFFLINE`). Firestore data is mapped defensively, so incomplete remote documents cannot crash the focus UI.

See `docs/firebase-setup.md` for the Firestore document shape and setup steps.

## Build

Recommended toolchain:

- Android Studio Quail 3 or newer
- JDK 17
- Gradle 9.5
- Android SDK 37 (target SDK 36)

If the Gradle wrapper has not been generated in your clone yet:

```bash
gradle wrapper --gradle-version 9.5.0
```

Then:

```bash
./gradlew test
./gradlew lint
./gradlew assembleDebug
```

On Windows:

```powershell
gradlew.bat test
gradlew.bat assembleDebug
```

## Architecture

See:

- `docs/architecture.md`
- `docs/design-system.md`
- `docs/kotlin-migration.md`
- `docs/rag-baby-sprite.md`
- `docs/companion-growth.md`
- `docs/firebase-setup.md`
