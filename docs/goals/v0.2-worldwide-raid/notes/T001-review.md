# In-progress audit and corrections

Read-only review identified two P1 gaps: first-ever offline sessions were terminal
unavailable, and old result copy implied global damage before confirmation.

Corrections: new sessions persist original start time and nullable generation;
unknown-generation rows remain pending and server binding requires the session
start to be within the current generation. Pre-v0.2 records alone remain excluded.
Damage headers now explicitly say personal rewards, shared HP is not locally
decremented, and offline copy says unconfirmed to cover server-commit response loss.

Added JVM generation freeze/restart and concurrent retry tests; device tests cover
real v1 DB migration, journal codec, outbox persistence and scheduling failure.
Backend real callable tests exercise two identities, duplicate concurrency, stale
generations, rules rejection, payload bounds, budget and defeat. Final executed
counts and remaining rollout tasks belong in the completion report.
