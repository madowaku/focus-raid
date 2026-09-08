# Issue #6 implementation and verification

Branch: `feat/v0.2-worldwide-raid-integration`, based on the existing v0.1 branch,
not old main. Issue #6 and its entire goal contract were read before implementation.

## Implemented

- Firebase v2 `submitContribution`, permanent session receipts, transactional HP
  and focus totals, distinct anonymous participation and bounded daily credit.
- Strict completed-session payload validation; no client damage/HP field, no
  client writes to shared authority, App Check enforced outside the emulator.
- DataStore session generation/start-time binding and finish-journal round trip.
- Room v1→v2 migration and atomic local history/outbox insertion with UUID keys.
- WorkManager retry/backoff/network constraint, startup and periodic recovery,
  response-loss replay, immutable terminal receipt state under concurrent drains.
- Nullable first-offline binding resolved conservatively by the server. Old
  generation work never spills into a replacement; ended work becomes STALE.
- Result/world pending, retrying, unconfirmed, auth, accepted/already-counted,
  stale/rejected states. Removed simulated local decrements of shared HP and
  labeled personal rewards explicitly. No Free/Pro boundary or content expansion.

## Acceptance evidence

| Gate | Reproducible evidence | Result |
|---|---|---|
| A two installations | `WorldwideEmulatorTest`: independent FirebaseApp/Auth identities, actual Android Functions SDK and server Firestore reads | Combined +10 focus minutes, -10 HP, +2 participants; both snapshots identical |
| B duplicates | Callable emulator tests: 8 concurrent requests; response replay; changed-owner/payload rejection. Android test loses actual successful reply, reopens Room and retries | Exactly one server application; retry ALREADY_COUNTED |
| C offline | Android Room tests preserve pending across reopen; Android real callable test simulates transport outage then lost response; JVM ViewModel test completes first-offline session and recovers | Local credit remains 25; pending survives; eventual once-only acceptance |
| D generations | Emulator test rotates N→N+1, replays bound and null-bound old sessions; JVM pause/restart generation freeze | STALE, no new-generation damage; old accepted receipt survives rotation |
| E authority | Real Auth/Firestore emulator rules tests, callable malformed/bounds/UID-budget checks | Direct world/receipt/budget writes and other-user receipt reads denied; arbitrary damage rejected; 1,440-minute daily bound |
| F honesty | JVM timeout/auth/rejection/cancellation/concurrent drain tests; API unavailable/unauth tests; font-1.5 retry accessibility test and new captures | Pending retained on uncertain delivery; terminal states explicit; no fake success |
| G regression | Commands below | Local gates passed; timer evidence recorded separately below |

Local verification on Windows, Gradle 9.5.0, Java 17 for Android and Java 21 for
Firebase emulators, Node 24 host (deployment/CI configured Node 22), API 36 emulator:

- `gradle testDebugUnitTest`: **63 passed, 0 failures**.
- `gradle connectedDebugAndroidTest`: **15 passed, 0 failures, 1 explicitly
  skipped opt-in Firebase test**. That opt-in test was run separately and passed.
- Actual Android→Firebase emulator integration: **1 passed, 0 skipped/failures**.
- `node --test functions/test/unit.test.js`: **2 passed**.
- Actual callable/Auth/Firestore rules suite: **5 passed**, repeated after SDK/CLI
  upgrade and lockfile reinstall. Final receipt: `artifacts-world-backend-lockfile.log`.
- `gradle lintDebug assembleDebug bundleRelease`: **passed**, lint **0 errors,
  25 warnings, 2 hints**. AAB is a compile artifact, not signed Play certification.
- `bash scripts/capture-android-ui.sh`: **147 PNGs validated**, retaining all 129
  prior captures and adding 18 contribution-state/font captures. Inspected normal
  and font-1.5 accepted, offline, stale and result layouts; actions remain readable.
- `npm ci --prefix functions`: passed with committed lockfile. Production npm
  audit still reports **8 moderate, 0 high/critical** transitive advisories through
  Google storage/HTTP/UUID dependencies. Current direct Firebase versions and
  compatible fixes were applied; no forced downgrade or suppression was used.

During verification, a malformed fake API-key fixture prevented Android Functions
SDK calls. The fixture now uses a syntactically valid emulator-only dummy key and
asserts the real server reply before simulating response loss. An older Firebase
CLI was incompatible with Functions v7; updated CLI and Java 21, then reran the
actual callable suite successfully. Test assertions were not weakened.

## External production work

No production backend was deployed and no live worldwide user traffic is claimed.
Operator must select the production project, enable/configure billing and Functions,
deploy callable/rules, seed a real unique raid generation, validate Play Integrity
App Check and run two Play installations through the documented acceptance cases.
Update privacy/retention/deletion disclosures for contribution metadata, finish
Play billing/signing/store checks and perform physical-device background tests.
Exact commands and schema: [functions/README.md](../functions/README.md).

## Limits and later work

- App Check and server bounds cannot prove human focus; anonymous identities can
  be recreated. Avoid claims of cheat-proof attention validation.
- Single-document transactional authority favors auditability; load-test actual
  traffic before promotion. Retry absorbs contention, not unlimited throughput.
- Receipts cannot be blindly TTL-deleted without reopening replay. A future
  privacy deletion workflow must preserve non-identifying idempotency tombstones.
- WorkManager timing is OS-controlled. Manual retry and foreground refresh exist;
  background delivery is eventual rather than an exact-time promise.
- Existing lint/dependency maintenance, physical TalkBack traversal and OEM battery
  policies remain follow-up work. No subscriptions, ads, chat, profiles or analytics.

Final timer regression: bash scripts/test-timer-durability.sh passed **4/4** (deep Doze, screen off, process kill and device reboot), including completion notification marker and fresh-launch journal reconciliation. Evidence: artifacts/timer-durability/report.txt, finished 2026-09-08T14:34:52Z.
