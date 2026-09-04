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
- Pure Kotlin domain rules
- Firebase / Firestore / Cloud Run boundary preserved for the next phase

The previous React/Vite PWA prototype is preserved on the `archive/pwa-mvp-v0.1` branch.

## Implemented vertical slice

- READY: duration selection, expedition selection, large timer hero, raid CTA, current raid card
- RAID: 208dp timer hero, pause/resume, early return, quiet reduced-information layout
- VICTORY: credited focus time, personal damage, world EP, discovery result, repeat CTA
- Session restoration after process death using an absolute end timestamp
- Exact completion alarm when exact alarms are permitted, graceful inexact fallback otherwise
- Notification permission request on Android 13+
- Home / Raid / Companion / Log bottom navigation
- Pure Kotlin domain tests

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
