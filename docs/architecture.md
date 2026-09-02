# Focus Raid MVP architecture

## Core rule

Read shared state from Firestore, mutate authoritative world state through Cloud Run, and keep the focus interval local to the client.

## Phase 1: local vertical slice

The first implementation intentionally uses deterministic local/mock services so the product loop can be tested before backend complexity is introduced.

### Client responsibilities

- render home, timer, result, world summary and raid teaser
- own countdown locally during focus
- preserve last-selected duration and expedition later
- keep the active session quiet: no realtime world listeners while focusing

### Domain rules

- 1 credited minute = 1 personal damage
- 1 credited minute = 1 world EP
- every accumulated 25 focus minutes produces one discovery roll
- companion growth uses cumulative focus minutes, never streaks or paid boosts
- world armory uses shared contributions and later resets per raid campaign

## Phase 2: Firebase boundary

Planned services:

- Firebase Auth: anonymous first, optional account linking later
- Firestore: user summaries, goals, world/current, raids, raid entries
- Cloud Run: startFocus, finishFocus, submitRaid, worldRollup
- FCM: opted-in raid notifications
- App Check: protect server endpoints from casual forged clients

### Suggested collections

```text
/users/{uid}
/users/{uid}/goals/{goalId}
/users/{uid}/sessions/{sessionId}
/world/current
/raids/{raidId}
/raids/{raidId}/entries/{uid}
```

## Scaling constraints

- do not update one shared world document per user session at large scale
- do not keep realtime Firestore listeners running during focus sessions
- submit raid results to per-user entry documents, then aggregate
- move to sharded counters only when traffic proves it is required

## MVP product hypotheses

1. People choose Focus Raid again instead of a plain timer.
2. A single persistent companion creates attachment.
3. The world raid creates a meaningful synchronized payoff after a quiet focus interval.
4. Tower/Abyss exploration gives off-raid sessions meaning.
5. Personal loop bosses turn real deadlines into satisfying progress without punishment.
