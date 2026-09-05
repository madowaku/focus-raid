# Focus Raid

Focus Raid is an Android-native focus timer where real-world concentration powers a shared fantasy world.

## Product principles

- Start focusing within five seconds.
- Focus time is never rolled back.
- Your companion grows from time spent together, not paid power.
- World systems reward cooperation, not rankings.
- During a focus session, the app becomes quiet.
- The world can be playful; the timer must remain useful.
- Shared features must stay low-maintenance: preset interaction over free-form moderation.

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
- Optional Firebase Auth + Firestore shared world and preset footprints
- RevenueCat-backed one-time Pro entitlement
- Cloud Run remains the planned boundary for any future authoritative world writes

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
- Companion growth from egg to mature form using cumulative focus minutes
- Distinct Compose Canvas silhouettes for egg / hatchling / first growth / second growth / mature
- `RAG EVOLVED!` result reveal whenever credited focus crosses a growth threshold
- Home / Raid / Companion / Log bottom navigation
- World Raid overview with shared boss, Tower, Abyss, and Pro Star Route progress
- Star Route as the first real Pro expedition
- Detailed statistics and full-history access for Pro
- RevenueCat purchase/restore wiring around entitlement `pro`
- Anonymous Firebase Auth + one-shot Firestore `world/current` read when backend config is present
- Preset-only shared Footprints fetched and posted asynchronously after completed sessions
- One anonymous user footprint per checkpoint, with no free-form social text
- No fake-user fallback for Footprints when a real Firebase backend is configured but unavailable
- Shared-world refresh only outside active RUNNING / PAUSED sessions
- Pure Kotlin domain tests
- Android emulator visual QA across 360×800 and 720×1280, including Free/Pro, paywall, companion evolution, and footprint states

## Monetization

Focus Raid uses **Free + Pro lifetime purchase**. There is no subscription and no advertising.

The guiding boundary is:

> Focus features stay Free. Pro unlocks more world and deeper records.

The current paywall sells only implemented value:

- Pro Raid: Star Route
- detailed statistics
- full history

The Android purchase still goes through Google Play. RevenueCat validates the purchase and exposes the `pro` entitlement to the app. If RevenueCat configuration is absent, the Free app remains usable.

See `docs/billing.md` for configuration and store-test requirements.

## Companion growth

Rag starts as an egg on a fresh install and visually evolves as credited focus time accumulates.

- 0 min: egg
- 75 min: hatchling
- 720 min: first growth
- 1,800 min: second growth
- 4,500 min: mature

The Companion tab shows the current form, progress to the next form, time spent together, today's contribution, and a five-form progression strip. Future forms stay dimmed until unlocked.

When a completed or partially credited session crosses a growth threshold, the result state records the old and new form. A rare `RAG EVOLVED!` card briefly shows the previous silhouette before revealing the newly unlocked form. Evolution never depends on streaks or paid acceleration.

## Shared world and Footprints

The Android client can initialize Firebase without committing `google-services.json`.

Provide these values as Gradle properties or environment variables:

```text
FOCUS_RAID_FIREBASE_PROJECT_ID
FOCUS_RAID_FIREBASE_API_KEY
FOCUS_RAID_FIREBASE_APP_ID
```

When all three exist, Focus Raid signs in anonymously and performs a one-shot server read of `world/current` while the app is outside an active focus session. Completed sessions can then fetch the latest preset Footprints for the reached checkpoint and post one preset Footprint for the current anonymous user.

Footprint documents store only a preset ID and server timestamp. The displayed message and glyph come from the app's built-in preset list. Free-form text is never written to Firestore.

If Firebase configuration is missing, the app uses `FakeWorldRepository` for local preview. If a real Firebase backend is configured but a Footprint request fails, Focus Raid shows no remote Footprints rather than pretending seeded users are real.

`WorldRepository` exposes both the latest snapshot and a sync status (`LOCAL_PREVIEW`, `CONNECTING`, `LIVE`, `OFFLINE`). Firestore data is mapped defensively, and all Footprint network work is asynchronous so cloud availability never blocks starting or completing a focus session.

See `docs/firebase-setup.md` for the Firestore shape, preset Footprint model, and Security Rules.

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

## Release gate

Automated CI protects the timer and core UI, but production credentials, store purchases, Firebase Security Rules deployment, privacy policy, Google Play Data Safety, release signing, and final device testing are manual release gates.

See `docs/release-readiness.md` before creating the public Play release.

## Architecture

See:

- `docs/architecture.md`
- `docs/design-system.md`
- `docs/kotlin-migration.md`
- `docs/rag-baby-sprite.md`
- `docs/companion-growth.md`
- `docs/firebase-setup.md`
- `docs/billing.md`
- `docs/release-readiness.md`
