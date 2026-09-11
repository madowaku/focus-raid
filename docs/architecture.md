# Focus Raid Android architecture

## Core rule

Keep focus timing authoritative on-device, read shared world state through repositories, and send authoritative world mutations through server endpoints rather than trusting the Android client.

The timer must remain useful even when every network dependency is unavailable.

## Session flow

Jetpack Compose renders one focus flow as state:

```text
READY -> RUNNING <-> PAUSED -> COMPLETED
                       \-> ABORTED
```

READY / RAID / VICTORY are visual states of that flow. Navigation disappears while a focus session is active.

## Timer

Never make `remainingSeconds -= 1` the source of truth.

Running sessions persist an absolute end timestamp:

```text
endEpochMillis = startEpochMillis + duration
```

The UI derives remaining time from `endEpochMillis - now`. This survives screen-off, activity recreation and process death.

`AlarmManager` schedules completion notification delivery. If exact alarms are allowed, Focus Raid uses an exact alarm; otherwise it uses an idle-safe fallback and still restores the correct elapsed state when the app opens.

`BOOT_COMPLETED`, `MY_PACKAGE_REPLACED`, and exact-alarm permission changes restore an active completion alarm from DataStore. If the persisted end time already passed, recovery posts the completion notification and the next app process reconciles the expired session.

### Timer durability gate

`scripts/test-timer-durability.sh` exercises four Android emulator failure modes on every PR:

1. deep Doze while screen-off
2. normal screen-off sleep
3. application process death via `am kill`
4. full emulator reboot with an active persisted session

Each case verifies both completion delivery and fresh-process reconciliation of expired `RUNNING -> READY` state.

The debug control receiver exists only under `src/debug`; production builds do not expose it.

Current v1 behavior does not require a foreground service. Revisit that only if a future feature needs continuous foreground execution rather than end-time correctness and notification delivery.

## System access

Do not cold-prompt notification permission at launch.

The first focus start can show a short education dialog for:

- app notifications
- exact alarms / Alarms & reminders special access

Users can continue without either capability. The persisted end timestamp remains authoritative regardless of notification permission.

## Persistence

Preferences DataStore stores compact current-session state:

- selected duration
- selected expedition
- current session phase
- active session id
- running end timestamp
- paused remaining duration
- cumulative credited focus minutes
- system-access education acknowledgement

Room stores durable Adventure Log rows. Each meaningful completed or aborted session records:

- stable `sessionId`
- completion timestamp
- planned and credited minutes
- expedition
- outcome
- damage
- optional rarity / discovery

The same `sessionId` survives pause, resume, process death and reboot. It is also the Room primary key, preventing duplicate history during recovery.

Productization adds an exact finished-entry journal to DataStore. A transition is
acknowledged in UI only after its durable write completes. Completion freezes the
session ID, credited minutes, outcome and random discovery before writing Room;
then it commits cumulative credit and clears the active ID atomically. The journal
remains until result dismissal or the next start. Recovery can safely replay after
either write and show the same result without rerolling loot or crediting twice.
Old builds' already-inserted Room rows are honored during upgrade recovery.

Paused time retains milliseconds; only display rounds to seconds. Legacy identity
migration changes the ID alone, never temporarily changes PAUSED to RUNNING.
Alarms notify only after checking the durable current session; they never write
rewards. MainActivity uses singleTask to prevent two independent timer controllers
from notification/launcher entry. Storage errors expose retry without acknowledging
a result that has not been saved.

Footprint work uses bounded network waits and result identity checks, is canceled
when leaving a result, and cannot overwrite later timer state. Shared-world errors
retain the last server snapshot or show unavailable values rather than sample
participants. Unconfigured local preview is explicitly labeled.

RevenueCat snapshot application and local verified-cache writes are serialized.
Generation checks reject refreshes superseded by purchase/restore, and CustomerInfo
request dates prevent stale negatives from demoting known Pro. A newer verified
negative still applies refunds. See `product-art.md` and `adventure-collection.md`
for the asset boundary and additional user-requested content.

Session-state DataStore writes are serialized in invocation order so a delayed write from a previous session cannot overwrite a newly-started session.

## Domain

`core/domain` is pure Kotlin and contains no Android API dependencies.

Current rules:

- 1 credited minute = 1 personal damage
- 1 credited minute = 1 world EP
- every accumulated 25 focus minutes produces one discovery roll
- companion growth uses cumulative credited focus minutes only
- streak loss never rolls companion growth back
- paid power never accelerates companion growth

### Companion growth

| Total focus time | Stage |
| ---: | --- |
| 0–74 min | 卵 |
| 75–719 min | 幼体 |
| 720–1,799 min | 第一成長 |
| 1,800–4,499 min | 第二成長 |
| 4,500+ min | 成熟 |

`CompanionGrowth` owns thresholds, progress, next-stage calculations and threshold-crossing detection. When a session crosses a threshold, the result state carries a `CompanionEvolution` so the UI can show the rare `RAG EVOLVED!` reveal without duplicating growth rules.

## Shared world reads

`WorldRepository` is observable through `StateFlow<WorldSnapshot>` and also exposes a small `WorldSyncStatus` state.

Two implementations exist:

- `FakeWorldRepository`: deterministic local preview used by CI, screenshots and unconfigured builds
- `FirebaseWorldRepository`: anonymous Firebase Auth + one-shot Firestore read of `world/current`

`WorldRepositoryFactory` selects Firebase only when all three build values exist:

```text
FOCUS_RAID_FIREBASE_PROJECT_ID
FOCUS_RAID_FIREBASE_API_KEY
FOCUS_RAID_FIREBASE_APP_ID
```

Otherwise it deliberately falls back to the fake repository.

Firebase initialization is explicit, so the repository does not require committing `google-services.json` just to build the app.

Runtime order:

1. restore the local focus session
2. if the session is not RUNNING or PAUSED, sign in anonymously if needed
3. perform a one-shot server read of `world/current`
4. validate the contribution schema and map display fields onto a neutral zero-people snapshot
5. publish the new snapshot through `StateFlow`
6. refresh on quiet foreground screens every 30 seconds, after accepted receipts, and on explicit refresh

There is no realtime Firestore listener during focus. A rapid `もう一回集中する` path also skips the post-result refresh so a new focus session does not intentionally launch network work first.

If Auth or Firestore fails, the repository reports `OFFLINE`, retains the last server snapshot (or a neutral unavailable state), and leaves local progression untouched. It never fabricates preview people for a configured backend.

The expected Firestore shape and setup steps live in `docs/firebase-setup.md`.

## Authoritative world writes (v0.2)

Completed credited sessions enter a transactional Room outbox using their durable
session UUID and frozen generation/start time. WorkManager retries a Firebase v2
callable. Its Firestore transaction validates the session, checks a permanent
receipt and per-UID budget, and atomically updates world HP/total and participant
identity. Clients cannot write world totals or receipts directly.

See [worldwide-raid.md](worldwide-raid.md) for generation isolation, nullable
first-offline binding, response-loss recovery and explicit authority limits.
The minimal implementation uses one authoritative world document; contention
causes retry rather than a false success. Load-test before large promotion.
## Footprints

Footprints are lightweight asynchronous traces, not chat.

- fixed preset messages only
- no free-form text in v1
- at most a few recent traces are shown at a checkpoint
- Tower footprints are keyed by floor
- Abyss footprints are keyed by depth

Configured builds use Firebase footprint reads/writes with preset validation and owner rules. Unconfigured previews are explicitly labeled.

Suggested remote shape:

```text
footprints/{expedition}/checkpoints/{checkpoint}/entries/{uid}
  presetId
  createdAt
```

Clients should render glyph/text from the local preset catalog rather than trusting display text from Firestore.

## Backend scope

Firebase anonymous Auth, Firestore and the submitContribution callable are used.
App Check protects production calls. Profiles, account linking, chat, analytics
and new notification services are outside this sprint.

## Scaling rules

- no realtime shared-world listener while focusing
- submit raid results to per-user entry documents and aggregate server-side
- fetch footprints only around completion / checkpoint views
- add sharded counters only when real traffic proves they are required
