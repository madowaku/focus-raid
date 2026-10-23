# Walking Expedition — issue #25

Android v0.13 (`a79ebb7`) is the implementation baseline. The integration keeps
the old main (`fe0ac0e`) in history and retains its published bilingual privacy
policy, including the deletion instructions. The PWA remains on
`archive/pwa-mvp-v0.1`. Existing uncommitted work in other checkouts is untouched.

## Experience

- Free and optional. Home keeps the focus CTA first and adds a quiet path summary.
- “歩数を使う” opens an explanation before the system READ_STEPS request.
  Cancelling or refusing does not affect focus, FIRST 25, purchases or raids.
- 1,000 / 3,000 / 5,000 steps unlock three daily discoveries, chosen deterministically
  from six handmade scenes, each with a story and keepsake. No random loot economy.
- Home and Companion both open the walking notebook. Keepsakes are journal entries,
  separate from focus-session inventory; no minutes, damage or growth are awarded.
- Above 5,000, the count remains in the diary without additional reward pressure.
- No animation, sound, walking notification, location collection or walking service.
  No walking UI appears during focusing or focus-result sequences.

## Source and integrity

Production always uses Health Connect `connect-client:1.1.0` (stable).
`StepsRecord.COUNT_TOTAL` is aggregated without a DataOrigin filter, allowing Health
Connect to apply its own source aggregation/deduplication. A missing result is
“no data”, not zero and not a fabricated count. Permission, unavailable provider,
provider update, read/storage error and zero are distinct states.

Only foreground READY entry or deliberate refresh reads steps. STOP / entering
focus cancels the active read; no background read permission is requested. All
queried days finish before a single DataStore transaction commits daily counts and
checkpoint sets. A timeout/failure cannot commit a partial batch. Repeated reads,
restarts and concurrent updates cannot duplicate a `(date, checkpoint)` discovery.
Downward corrections update counts without taking earned keepsakes away.

The first enabled date is inclusive (that calendar day's existing data can count).
Every refresh rechecks up to 30 calendar days since that date, including yesterday
and late data. Longer absences cannot backfill outside this window. Old notebook
entries remain. The zone chosen at initial opt-in defines the diary calendar even
when travelling, avoiding overlapping reward days; the UI discloses that zone.
Day boundaries use `atStartOfDay`, including 23/25-hour daylight-saving days.

Walking data is in a separate `walking_expedition.preferences_pb` store, excluded
from both Android cloud backup and device transfer. No changes to Room schema,
focus credit, contribution payloads, billing or remote data. Stopping reads keeps
the notebook. Health Connect settings manage permission/source records; clearing
app storage/uninstall removes local records (including the existing focus data).

## Device boundary

Android 14 + SDK extension 20 can record on-device steps after READ_STEPS consent.
Other devices need a supported app supplying steps to Health Connect. Android 8
can run Focus Raid but cannot use the Health Connect provider. This first version
does not pretend to record sensor steps when Health Connect has no data, and does
not introduce an unverified sensor fallback.

References checked 2026-10-24:

- https://developer.android.com/health-and-fitness/health-connect/features/steps
- https://developer.android.com/health-and-fitness/health-connect/get-started
- https://developer.android.com/jetpack/androidx/releases/health-connect

## Verification and remaining acceptance

Automated coverage: boundaries 0/999/1000/2999/3000/4999/5000, excess steps,
missing versus zero, repeated reads, corrections, next-day late data, bounded
backfill, clock rollback, travel, DST, persistent concurrent replay, and explicit
permission explanation/cancel at font scale 1.5. See build/test reports from this
change for executed gates.

Before Play publication: declare READ_STEPS / the health-app use case in Play
Console, publish the updated privacy policy and review Data Safety. No production
service or store release is performed by this change.

Human/device acceptance remains separate: A401OP Android 15 real walking versus
Health Connect, permission deny/revoke/regrant, process death, next-day arrival,
offline focus and comfort at 360×800 / font scale 1.5. An emulator or fake repository
does not demonstrate sensor accuracy, background OS recording or enjoyment during
a week without focus. Issue #25 remains open for these acceptance checks.


### Executed on 2026-10-24

- `./gradlew.bat testDebugUnitTest`: 112 tests, 0 failures.
- `lintDebug`, `assembleDebug`, `bundleRelease`, `compileDebugAndroidTestKotlin`: PASS.
- `connectedDebugAndroidTest` on Android 16 emulator, 360×800 dp: 14 tests, 0 failures.
  Selected classes: WalkingStorageTest, WalkingUiTest, BeginningLightUiTest,
  FirstRunUiTest, RootNavigationTest, SessionPersistenceTest, BillingActionTest.
- Walking consent UI exercised at font scale 1.5; persistent concurrent replay tested.
- `git diff --check`: PASS.
- The existing FirstRun animation test originally timed out on a wall-clock wait.
  It now advances the Compose clock explicitly, retaining the exact ordered
  `[0, 1, 2]` arrival assertion; the complete selected regression suite then passed.
- No A401OP was connected. Real steps, source deduplication on that device,
  permission revocation on hardware and playtest enjoyment remain unverified.
- Release AAB is a compile gate only; no Play upload or production deployment.
