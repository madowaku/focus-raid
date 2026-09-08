# Worldwide contribution service

Runtime: Node 22, Firebase Functions v2 in `us-central1`, default Firestore database.
Use Java 21 for current Firebase emulators. Dependencies are pinned in the lockfile.

From repository root:

```sh
npm ci --prefix functions
npm test --prefix functions
npm run test:emulator --prefix functions
```

The emulator test uses only project `demo-focus-raid`. It exercises actual callable
requests with separate Auth identities and security-rules-protected Firestore reads
and writes. Test Admin access seeds/rotates the raid; clients never get Admin access.

For Android SDK integration, start emulators and leave them running:

```sh
functions/node_modules/.bin/firebase emulators:start --project demo-focus-raid --only auth,firestore,functions
```

In another shell, seed the demo and run the opt-in instrumented test on an Android
emulator (host alias `10.0.2.2`; auth 9099, Firestore 8080, functions 5001):

```sh
GCLOUD_PROJECT=demo-focus-raid FIRESTORE_EMULATOR_HOST=127.0.0.1:8080 node functions/seed.js
gradle connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.worldEmulator=true -Pandroid.testInstrumentationRunnerArguments.class=com.madowaku.focusraid.WorldwideEmulatorTest
```

PowerShell uses `$env:GCLOUD_PROJECT='demo-focus-raid'` and
`$env:FIRESTORE_EMULATOR_HOST='127.0.0.1:8080'` before the seed command.
Debug permits cleartext emulator traffic; release does not enable that exception.
Regular credential-free Android tests skip this opt-in test explicitly.

## Production deployment (operator credentials required)

1. Confirm the intended production Firebase project, billing-enabled Functions
   support, default Firestore location, anonymous Auth and Android configuration.
2. Configure Play Integrity App Check and exercise an internal Play build. The
   callable enforces App Check in production; only the Functions emulator bypasses it.
3. Create `world/current` using a unique generation ID, actual start timestamp,
   `status: active`, positive integer `bossMaxHp`/`bossHp`, `totalFocusMinutes: 0`
   and `raidParticipants: 0`. Optional display fields start at honest zero/default
   values. Never invent online counts.
4. Review and deploy `firestore.rules` and `functions:submitContribution` to that
   explicit project. This repository does not deploy implicitly or embed credentials.
5. Complete two-installation acceptance on the Play-distributed build, including
   offline recovery, duplicate response/retry, App Check/auth failure and generation
   rotation. Emulator success is not production certification.
6. Publish updated privacy disclosures for anonymous session contribution records.

Generation replacement is a trusted operator transaction writing a new unique ID,
start time, HP and zero totals together. Never reuse IDs or set the start time before
the actual generation starts. Keep receipts and participant/budget authority private.

Permanent receipts live at `contributions/{sessionId}` with owner UID, immutable
request and result. Per-day UID budgets and per-generation participation markers are
server-only. Do not delete/TTL receipts: they are the replay barrier. A future privacy
deletion design must retain a non-identifying replay tombstone before removing owner
data; that design is not silently supplied by Auth automatic cleanup.

The single world document is intentionally a small, auditable implementation.
Contention can delay requests, which remain queued; load-test before large-scale
promotion and adopt a new aggregation design only with equivalent replay guarantees.
