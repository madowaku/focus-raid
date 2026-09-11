# Focus Raid v0.2 Worldwide Raid Integration Sprint

## Mission

Turn the existing World Raid display into a real shared-focus system.

A completed, credited focus session should be able to contribute to the same authoritative worldwide raid seen by other installations. The player should be able to feel, truthfully, that their focus joined everyone else's effort.

Do not add content for content's sake. This sprint is about making the worldwide-focus promise real, durable, understandable, and safe to ship.

## Product promise to make true

> I focused. My session became a real contribution. The shared raid changed for everyone.

The result experience should make that connection legible without exaggerating what happened. If contribution is pending or the backend is unavailable, say so instead of pretending success.

## Non-negotiable invariants

1. **Authoritative shared total**
   - Two installations can complete credited sessions and observe the same authoritative raid state.
   - The client must not be able to directly set global HP or other authoritative totals.

2. **Exactly-once contribution semantics from the player's point of view**
   - Retrying, reconnecting, process restart, duplicate callbacks, restore/replay, or repeated delivery must not count one credited session more than once.
   - Reuse the app's durable session identity/journal where appropriate rather than inventing a weaker parallel identity.

3. **Offline focus still works**
   - Focus completion and local rewards must remain reliable without network access.
   - A contribution that cannot be delivered immediately is durably retained and clearly shown as pending.
   - Pending work retries safely later without duplicating damage.

4. **Raid generations are explicit**
   - World raid reset/defeat/replacement must have an explicit generation/version identity.
   - Old pending contributions must never accidentally damage a different raid generation.
   - Decide and document the product behavior for a valid old pending contribution whose original raid has already ended.

5. **Server-side validation and bounded contribution**
   - The trusted backend validates requests and applies bounded rules.
   - Do not trust client-provided global state, HP-after values, or arbitrary damage amounts.
   - Anonymous users are acceptable; adding social accounts is out of scope.

6. **Honest UI states**
   - Live, pending, retrying/offline, accepted/already-counted, stale-generation, and unavailable states must not be visually confused with one another.
   - Preview/fake participants must never be presented as real users in a configured production backend.

7. **Preserve the v0.1 reliability floor**
   - Do not regress durable timer completion, result recovery, reward deduplication, Pro entitlement handling, large-text usability, or existing adventure progression.

## Freedom to decide

Astra owns the implementation details. Inspect the current codebase and choose the smallest robust architecture that satisfies the invariants.

You may decide, among other things:

- backend endpoint/function shape and Firestore schema;
- transaction/idempotency strategy;
- contribution event identity and retention;
- durable pending queue design;
- retry/backoff strategy;
- raid generation lifecycle representation;
- how shared totals and boss HP are derived/stored;
- how the result screen and World Raid screen communicate contribution state;
- emulator/local test infrastructure;
- security rules and deployment documentation;
- refactors needed to keep the architecture coherent.

Prefer boring, auditable correctness over clever distributed-systems tricks. Keep the integration small and distinct from billing, chat, social identity, or unrelated product expansion.

## Acceptance gates

The sprint is not complete until the implementation can demonstrate all of the following with reproducible evidence:

### A. Two-installation convergence
- Installation A completes a credited session.
- Installation B observes the same authoritative raid generation and updated total/HP.
- Installation B completes another credited session.
- Installation A observes the combined authoritative result.

### B. Duplicate suppression
For the same credited session, deliberately exercise duplicate/retry paths such as repeated submission, reconnect, process restart, queue replay, and response loss. The authoritative contribution is applied at most once.

### C. Offline queue
- Complete a credited session offline.
- Local completion/rewards succeed normally.
- UI exposes that the world contribution is pending.
- Restore connectivity.
- The contribution is accepted once and the pending state clears.

### D. Generation isolation
- Queue a contribution for raid generation N.
- Advance/reset the world to generation N+1.
- Replay the old pending item.
- It must not damage generation N+1.
- The app resolves the stale item according to the documented product rule.

### E. Adversarial client checks
Demonstrate that a client cannot directly write arbitrary shared HP/totals or submit unbounded damage merely by changing client payload values.

### F. Failure honesty
Exercise backend unavailable, timeout, auth failure, and rejected/stale contribution paths. No fake success copy, fake participant counts, or silent loss of a pending contribution.

### G. Regression gates
Retain and pass the existing unit, Android, lint/build, visual, and timer-durability gates. Add targeted coverage for worldwide contribution correctness. Do not weaken assertions to make the sprint pass.

## Product UX target

The ideal emotional beat is simple:

1. The player finishes a real focus session.
2. Their personal rewards/progression resolve normally.
3. The app truthfully communicates what happened to the shared raid.
4. When accepted, the player can see that their focus joined the worldwide total and changed the current raid.
5. When not yet accepted, the app clearly shows that the contribution is pending rather than lost or already counted.

Do not require the user to understand distributed systems. The complexity belongs under the floorboards.

## Scope guardrails

Do not add subscriptions, ads, chat, free-form social posting, account/profile systems, new companions, new bosses, new Pro raids, or analytics SDKs as part of this sprint.

Do not change the adopted Free/Pro product boundary unless a correctness issue makes a change unavoidable. Worldwide contribution itself is part of the existing product promise, not a reason to invent a new monetization layer.

## Deliverables

- Production-ready client integration for authoritative world contribution.
- Backend code/configuration required to apply contributions safely.
- Firestore/security rules and local/emulator support as appropriate.
- Durable pending/retry behavior.
- UX for accepted/pending/failure/stale states.
- Automated tests covering idempotency, offline replay, generation isolation, and bounded authority.
- Reproducible two-installation or equivalent end-to-end verification procedure and evidence.
- Updated architecture/Firebase/release-readiness documentation.
- A concise completion report stating what is genuinely live, what was tested locally/emulated, and which external production deployment steps still require the owner's credentials or console access.

## Definition of done

Focus Raid may truthfully say that completed credited focus sessions can join a shared worldwide raid only when the acceptance gates above pass on the real contribution path.

If production credentials or console-only deployment steps block final live verification, implement and verify everything possible, leave no fake-live behavior enabled, and document the exact remaining external step instead of claiming completion.
