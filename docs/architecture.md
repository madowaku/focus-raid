# Focus Raid Android architecture

## Core rule

Keep focus timing authoritative on-device, read shared state through repositories, and mutate authoritative world state through Cloud Run when the backend is introduced.

## Android v1 boundaries

### UI

Jetpack Compose renders the app as state, not as separate timer pages.

The core focus flow is:

```text
READY -> RUNNING <-> PAUSED -> COMPLETED
                       \-> ABORTED
```

READY / RAID / VICTORY are visual states of one session flow. Navigation is hidden while a focus session is active.

### Timer

Never make `remainingSeconds -= 1` the source of truth.

Running sessions persist:

```text
endEpochMillis = startEpochMillis + duration
```

The UI derives remaining time from `endEpochMillis - now`. This survives screen-off, activity recreation and process death.

`AlarmManager` schedules the completion notification. If exact alarms are allowed, Focus Raid uses an exact alarm; otherwise it uses an idle-safe fallback and still restores the correct elapsed state when the app opens.

`BOOT_COMPLETED` and `MY_PACKAGE_REPLACED` restore the completion alarm from DataStore for an active running session. If the persisted end time already passed while the device was unavailable, Focus Raid posts the completion notification on recovery and resolves the completed session when the app next opens.

### System access

Do not cold-prompt notification permission on app launch.

The first focus start may show a short education dialog when either of these capabilities is unavailable:

- app notifications
- exact alarms / Alarms & reminders special access

The dialog explains why Focus Raid uses each capability and always allows the user to continue without granting them. The education acknowledgement is persisted so a deliberate opt-out does not block every future session.

When permissions are unavailable, the focus session remains valid because the authoritative end timestamp is persisted independently of the notification path.

### Persistence

Preferences DataStore stores:

- selected duration
- selected expedition
- current session phase
- running end timestamp
- paused remaining duration
- cumulative focus minutes
- whether timer system-access education has been acknowledged

Session history can move to Room when the product needs queries that exceed simple aggregate state.

### Domain

`core/domain` is pure Kotlin. No Android APIs belong there.

Current MVP rules:

- 1 credited minute = 1 personal damage
- 1 credited minute = 1 world EP
- every accumulated 25 focus minutes produces one discovery roll
- companion growth uses cumulative focus minutes, never streaks or paid boosts

### Data

`WorldRepository` is currently backed by `FakeWorldRepository` so the complete UI flow is testable without Firebase.

Planned backend services remain:

- Firebase Auth: anonymous first, optional linking later
- Firestore: user summaries, goals, world/current, raids, raid entries
- Cloud Run: startFocus, finishFocus, submitRaid, worldRollup
- FCM: opt-in raid notifications
- App Check: protect server endpoints

### Scaling

- Do not update one shared world document per user session at large scale.
- Do not keep realtime Firestore listeners running while focusing.
- Submit raid results to per-user entry documents and aggregate.
- Add sharded counters only when traffic proves they are needed.
