# Worldwide raid architecture (v0.2)

Implementation contract: issue #6 and `goals/v0.2-worldwide-raid/goal.md`.
Production rollout is separate from emulator verification; do not label a preview build live.

## Session to shared contribution

1. Starting a timer persists its existing UUID, original start time and known raid
   generation in the same DataStore edit as RUNNING. Pause/resume never changes the
   binding or original start time. A first offline session can have a null binding.
2. Completion freezes these fields in the existing finish journal. Room records
   history and its contribution outbox row in one transaction, keyed by that UUID.
   Personal credit commits through the existing idempotent journal protocol.
3. Only fully completed, credited sessions enter delivery. Aborted focus still
   earns the existing personal rewards. Pre-v0.2 history is never bulk submitted.
4. WorkManager uses network constraints, exponential backoff, a one-shot drain and
   a 15-minute periodic recovery job. Startup re-enqueues work. Database commit
   does not depend on successful work scheduling. A killed/restarted drain can
   safely resume rows left RETRYING. Each pass handles at most 50 rows and retries
   while more remain. OS scheduling is eventual, not an exact delivery deadline.
5. The Firebase callable authenticates the same anonymous identity used by the
   world repository. Its Firestore transaction records a permanent receipt and
   updates the shared world atomically. Concurrent calls or lost replies cannot
   apply the same session UUID again. The client sends no HP or damage amount.
6. The client acknowledges a row only from a valid server receipt. Terminal rows
   cannot be downgraded by a slower worker. A timeout means unconfirmed, not proof
   that the backend did not commit. Personal completion never waits for network.

## Generations and offline policy

Known generation N is immutable. If N has ended or been replaced, its pending
session resolves STALE with no damage to N+1. Personal rewards remain intact.
Defeat closes the current raid; advancing is an explicit trusted operator action,
not a client-controlled reset or automatic creation of another boss.

For first-ever offline sessions, the server may resolve a null generation to the
current raid only when the original session start is on/after that raid's start.
If the raid began after the session started, the session resolves STALE. The
client never rewrites the binding to whatever is current on reconnect. This is
conservative: an uncertain old session can miss world credit, but cannot damage
an unrelated replacement. Device clock anomalies may be rejected; do not repair
timestamps in a pending payload, which would change the identity's meaning.

## Authority and limits

The backend derives damage from validated whole credited minutes and clamps HP at
zero. One request is bounded to a completed 5–180 minute session; the server also
enforces a per-anonymous-UID daily budget of 1,440 minutes. World total measures
credited minutes; applied damage can be smaller on the killing contribution.
Participants are distinct contributing anonymous UIDs per generation, not online
people. No presence/analytics system is introduced.

App Check and bounds reduce abuse but cannot prove that a human actually focused.
Anonymous reinstallation can create another UID. These limits are explicit;
neither a client timer nor App Check is a human-attention attestation.

Keep receipt identifiers indefinitely unless a replacement tombstone strategy is
introduced. Deleting receipts permits replay. Do not reuse raid generation IDs.
Account deletion/retention disclosure must consider receipt ownership and budget
records; do not silently TTL them as part of a generic cleanup job.

## UI and privacy

Result and World Raid show pending, retrying, unconfirmed/offline, authentication
failure, accepted, already counted, stale and rejected states separately. World
HP is the last server snapshot, never locally decremented as if submission had
succeeded. An accepted receipt can precede the next snapshot refresh. Foreground
quiet screens refresh every 30 seconds and on explicit retry; the active timer
keeps its visual snapshot and immutable participation binding.

Disclose anonymous identity, session UUID, session start/completion times and
credited/planned minutes, receipt status and shared aggregates in the production
privacy materials. No discovery inventory, personal note, billing state or Pro
entitlement is sent with a contribution. Worldwide contribution is available to
Free and Pro equally.

See `worldwide-raid-report.md` for executed evidence and external release tasks.
