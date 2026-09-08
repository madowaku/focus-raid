# Issue #5 productization report

Historical receipt for commit `ce139af`. The later user-authorized production-art replacement and content expansion supersede its candidate-art status; see [release-art.md](release-art.md).

Baseline: `6ba27cd5795bb6d0c709b565228fa4a8c9655de8` (PR #4 / `feat/revenuecat-pro-access`). Work branch: `feat/v0.1-productization-sprint`; `main` was not used. Verification date: 2026-09-08, Windows, Java 17, Gradle 9.5.0, Android API 36 x86_64 emulator.

## Audit and implemented fixes

| Priority | Finding | Resolution |
| --- | --- | --- |
| P0 | Start/result UI could acknowledge a transition before persistence; interruption between Room and focus-credit writes could lose or reroll an aborted result. | Storage-first serialized transitions; immutable persisted finish journal; Room ID deduplication; idempotent credit commit; journal replay on restart. Legacy recorded results are reused. |
| P0 | An old Footprint response could overwrite a newly started session. | Result-session ownership, cancellation, bounded requests and no stale state replacement. |
| P0 | An older billing refresh/cache write could demote a newly restored Pro purchase. | Request generation and server timestamps, serialized snapshot/cache writes, persistent verified entitlement, duplicate action guard. |
| P1 | Pause rounded away milliseconds; early end used stale ticker time; legacy PAUSED identity migration transiently wrote RUNNING. | Exact remaining milliseconds, wall-clock credit calculation, deadline-aware completion and atomic identity migration. |
| P1 | Any Lifetime package could be selected; raw errors and unavailable pricing lacked safe recovery. | Exact lifetime SKU + package/product type; bounded configuration/product refresh; public retryable copy and Free exit while billing is busy. |
| P1 | Some start paths did not recheck current entitlement; system Back did not follow app state. | Central FeatureAccess guards for repeat/education starts; tab, active timer, result and modal Back behavior. |
| P1 | Configured backend failures resembled successful empty/preview data. | Explicit unavailable state, retained last server snapshot, retryable Footprint read errors, neutral configured-offline world, bounded reads. |
| P1 | Large text clipped timer digits/actions; multiple activity entries could create timer controllers. | Measured timer autosizing, scroll/minimum touch targets/insets; singleTask launcher/notification activity. |
| P2 | Procedural art coupled screens to placeholders; missing launcher identity and control semantics. | Stable typed artwork catalog with exact state resolution and accessible fallback, adaptive icon, labeled controls and timer state. |
| P2 | Purchase feedback was buried below benefits; preset chips and dialogs were cramped. | Error/status first in scrollable paywall, full-width presets, scrollable result/dialog content and safe privacy-link failure. |

No history is deleted for Free limits. Reward/drop rules, growth thresholds, Star Route and lifetime-only monetization remain intact. The new regression tests strengthen coverage; original gates are retained.

## Changed file groups

- `ui/FocusViewModel.kt`, `data/SessionStore.kt`, `data/SessionPreferences.kt`, `timer/*`: durable transition/replay and alarm recovery contracts.
- `billing/*`, `MainActivity.kt`: verified Pro cache, ordered requests, exact product selection and retryable public errors.
- `data/FirebaseWorldRepository.kt`, `ui/WorldStatusLabel.kt`, `ui/FocusRaidRoot.kt`: honest backend status, failure isolation and navigation/entitlement guards.
- `ui/FocusRaidApp.kt`, `CustomDurationSheet.kt`, `ProPaywallDialog.kt`, `CompanionProgressOverview.kt`: small-screen layout, semantics and actionable errors.
- `ui/ArtworkCatalog.kt`, `ProductArtwork.kt`, `RaidArtwork.kt`, `AdditionalFallbackArtwork.kt`, debug drawable resources: replaceable assets and generated concepts.
- `core/domain/AdventureCollection.kt`, `ui/AdventureCollectionUi.kt`, `FocusRules.kt`: companions, personal boss and persistent-history inventory without altered rewards.
- `app/src/test`, `app/src/androidTest`, `app/build.gradle.kts`, `scripts/*`, `.github/workflows/ci.yml`: regression/device coverage and expanded capture gates.
- `AndroidManifest.xml`, icon resources, README and `docs/*`: launcher identity, activity ownership and accurate release documentation.

Paths above are relative to `app/src/main/java/com/madowaku/focusraid` unless a repository-root path is given.

## Added adventure content

- **Miko**: second companion earned at 75 cumulative credited minutes; persisted selection, shared existing growth rules, all five stages and three presentation states. Free, no paid growth advantage.
- **Mord**: personal Abyss challenge, 180 credited minutes, distinct session IDs prevent duplicate progress. Clearly separated from Volga's shared world display.
- **Inventory**: stable catalog IDs for existing reward pools and counts from complete local history, including unknown legacy item names. No new randomized rewards or paid consumables.
- **Artwork**: Rag, Miko, Volga and Mord generated transparent concepts are available only in debug previews. Release defaults to procedural fallback; concepts are not approved final production art. See [art catalog and prompts](product-art.md) and [content rules](adventure-collection.md).

The user's desired shared-focus experience still needs an authoritative, idempotent server contribution integration. The current client reads shared world state and posts preset Footprints; it does **not** claim local credit changed global HP. Result copy and offline/preview labels make this boundary explicit. No speculative backend architecture was introduced in this sprint.

## Verification

Commands below run from the repository root. This host uses the installed Gradle distribution (no wrapper required by the existing project).

| Gate | Command | Result |
| --- | --- | --- |
| JVM regressions | `gradle testDebugUnitTest` | 52 tests, 0 failures |
| Android instrumented regressions | `gradle connectedDebugAndroidTest` | 9 tests, 0 failures at 360×800 / density 160; accessibility fixtures use font 1.5 |
| Lint | `gradle lintDebug` | 0 errors, 22 warnings, 2 hints |
| Debug and release compile | `gradle assembleDebug bundleRelease` | Passed; release AAB compile is not signed Play validation |
| Visual capture | `PYTHON=python bash scripts/capture-android-ui.sh` | 113 required PNGs validated: original 49 + 60 font 1.3/1.5 captures + 4 concepts, at 360×800 and 720×1280. Latest APK: 40 affected-state recaptures via `PYTHON=python bash artifacts/final-capture.sh`, all PNG checks passed |
| Timer durability | `bash scripts/test-timer-durability.sh` | 4/4 passed: deep Doze, screen off, process kill, device reboot; completion delivery and fresh-launch journal reconciliation |
| Whitespace | `git diff --check` | Passed |

Android tests cover real DataStore/Room replay and concurrent deduplication, billing cancel/failure/success with a gateway fixture, entitlement revocation during education, Back navigation, and small-screen semantics/actions. They do not substitute for real Play transactions or actual TalkBack spoken traversal.

Human image inspection covered READY, maximum timer/custom duration, RAID, PAUSED, ABORTED, VICTORY, Footprint present/error, paywall/error/restoring, companions and boss concepts, including 1.3/1.5 fonts. No critical clipped essential action found; scrollable content intentionally extends beyond the viewport.

Visual evidence lives in ignored `artifacts/visual/`; build/test reports live in `app/build/reports/`; device durability evidence lives in `artifacts/timer-durability/report.txt`. CI retains visual/device reports as artifacts. Visual capture verifies PNG presence/dimensions; selected images are additionally inspected by the implementation agent, not treated as an automated pixel-perfect assertion.

During verification, timer overflow was caught and fixed without relaxing the assertion. A new Android billing fixture initially constructed Activity off the main thread; fixed fixture dispatch, rerun passed. On Windows, all 113 captures completed but the validation interpreter alias was unavailable; the script now accepts `PYTHON`, and its unchanged PNG checks passed with the installed Python.

Final local artifact SHA-256:

- Debug APK: `02be1c72e1c302225c39faa5654bfd3bdefab7b7139a12ae968c7116d27e9417` (29,829,256 bytes).
- Release compile AAB: `13633339772e8e3fc409d92fa57799dd5c3cecbd0860c13db5e030a2ece9bd74` (15,702,889 bytes).
- Archive inspection confirmed four concept resources in debug and zero in release.

## External release tasks

These are not locally certified and remain in [release-readiness](release-readiness.md):

- Play one-time product `focus_raid_pro_lifetime`; RevenueCat entitlement `pro`, Current Offering/Lifetime mapping and production API key.
- Real Play license purchase, cancellation, refund and restore, including reinstall/offline behavior on Play-distributed builds.
- Production Firebase anonymous Auth, Firestore, App Check/configuration and deployed rules; two-installation Footprint test.
- Hosted privacy-policy URL and final Play Data Safety declarations.
- Upload key backup, signing secrets, signed AAB, Play App Signing and internal/closed track install; hardware notification/exact-alarm/background smoke.
- Final approved art and state variants, store screenshots and release copy. No final art approval is implied by generated debug concepts.

## Non-blocking later work

Authoritative shared focus contribution is a separately scoped product integration; final artwork/state packs; long-history lazy rendering/performance; remaining dependency/lint maintenance; tablet/orientation refinement; real TalkBack speech and additional physical OEM battery-policy checks. No subscription, ads, chat, iOS or second Pro raid was added.
