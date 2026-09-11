# Firebase / Cloud Run setup

Focus Raid keeps the 25-minute countdown local. Firebase supplies identity + shared reads; Cloud Run owns authoritative writes.

## 1. Firebase project

Create a Firebase project and web app, then enable:

- Authentication → Anonymous
- Cloud Firestore (prefer `asia-northeast1` / Tokyo for the intended Japan-first MVP)

Copy the web app config into `.env.local` using `.env.example`.

## 2. Firestore rules

Deploy `firestore.rules`. Clients may read public WORLD / RAID state and their own user history, but **cannot mutate authoritative game state**. Cloud Run uses the Admin SDK and performs writes server-side.

## 3. Cloud Run API

The API lives in `server/` and requires Node.js 22+.

```bash
cd server
npm install
npm start
```

Cloud Run deployment should:

- run with a service account that can use Firebase Authentication verification and Cloud Firestore
- listen on the injected `PORT` (the server already does this)
- set `ALLOWED_ORIGIN` to the production web origin
- deploy in the same region as Firestore when practical

After deployment, set:

```text
VITE_FOCUS_RAID_API_BASE_URL=https://<cloud-run-service>
```

## 4. Runtime modes

### LOCAL mode

If Firebase environment variables are absent, the existing deterministic local/mock MVP remains fully playable. CI uses this mode.

### REMOTE mode

When Firebase config + API base URL are present:

1. app signs in anonymously
2. `/focus/start` creates an authoritative server session
3. countdown runs locally with no realtime backend traffic
4. `/focus/finish` calculates credited minutes from server time
5. Cloud Run writes user progress, Personal damage, WORLD contribution, loot and optional RAID entry
6. clients read shared `world/current` from Firestore

If a remote start/finish fails, the UI must remain usable and surface/fallback to LOCAL behavior rather than losing the user's focus session.

## 5. Firestore MVP documents

```text
/users/{uid}
/users/{uid}/sessions/{sessionId}
/world/current
/raids/{raidId}
/raids/{raidId}/entries/{uid}
```

The MVP intentionally updates `world/current` transactionally because the first live raid target is tens of users. Before high concurrency, replace direct global increments with sharded/rollup counters as described in `docs/architecture.md`.

## 6. Seed `world/current`

Before the first REMOTE test, create the document with values compatible with the UI, for example:

```json
{
  "towerFloor": 4281,
  "towerProgress": 82,
  "towerEp": 0,
  "abyssDepth": 12481,
  "abyssProgress": 51,
  "abyssEp": 0,
  "armoryReady": 68,
  "focusNowEstimate": 0,
  "currentRaidId": "volga-001",
  "raidDamage": 0
}
```

The seed numbers are presentation defaults only. The first multiplayer test should replace them with deliberately chosen test state.
