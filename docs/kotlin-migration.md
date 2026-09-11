# PWA -> Android native migration

## Decision

Focus Raid v1 is Android native only.

The PWA MVP is preserved on `archive/pwa-mvp-v0.1`. The Android implementation lives on `feat/android-native-mvp` until merged.

## Reuse unchanged

- product principles
- focus / raid / reward rules
- Firebase / Firestore / Cloud Run service boundary
- world concepts and fake fixtures
- companion identity and sprite bible
- visual QA discipline
- the rule that the app becomes quiet during focus

## Replace with Kotlin / Android

| PWA asset | Android replacement |
| --- | --- |
| `App.tsx` state | `FocusViewModel` + immutable `FocusUiState` |
| `window.setInterval` | wall-clock end timestamp + lifecycle ticker |
| browser persistence | Preferences DataStore |
| web notification assumptions | AlarmManager + Android notification |
| CSS | Material 3 Expressive theme + Compose modifiers |
| React UI | Jetpack Compose |
| Vitest | JUnit |
| Playwright browser screenshots | Compose/device screenshot QA |
| mock TS service | `FakeWorldRepository` |
| Node/Vite CI | Gradle Android CI |

## Remove from Android mainline

- Vite runtime
- `index.html`
- `package.json`
- TypeScript config
- React source
- web-only CSS
- browser raster-generation helpers
- Playwright browser capture script

Nothing is lost: the complete web prototype remains on the archive branch.

## Migration status

Implemented in the first Android vertical slice:

- project skeleton
- Material 3 Expressive theme
- pure Kotlin domain rules + tests
- DataStore recovery
- robust wall-clock timer
- exact-alarm capable completion notification
- READY / RAID / VICTORY UI
- Home / Raid / Companion / Log navigation
- Android Gradle CI

Still planned:

- production Rag and boss sprite assets
- exact-alarm permission education UI
- Firebase Auth / Firestore / Cloud Run
- FCM world raid notifications
- Room session history if/when needed
- screenshot regression harness for 360×800 and 720×1280
