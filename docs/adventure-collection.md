# Companions, personal boss and inventory

Added at the user's request during issue #5. These are Free progression features;
they add no subscription, paid power, new Pro expedition or backend service.

- Rag remains available from the beginning.
- Miko, the leaf fox, unlocks at 75 cumulative credited minutes. Selection persists
  in DataStore. All companions share existing earned growth, so switching cannot
  reduce minutes or reset progress. Both use the same five growth thresholds and
  idle/focused/celebrate artwork slots.
- Mord, the root-crystal beast, is a **personal Abyss raid** shown below the shared
  Volga raid. Every credited Abyss minute, including early-ended sessions, counts
  toward 180 HP. Progress derives from distinct persisted session IDs. It does not
  modify the shared world document or pretend to be another player's contribution.
- Inventory is shown in Companion. Existing discovery names/rarities are moved
  into a stable item catalog without changing drops. Counts derive from complete
  local history, including older-than-Free-history-limit records. Unknown legacy
  item names remain visible. Items are keepsakes, not focus/purchase multipliers.

The shared-world vision is that many people's focused minutes combine into one
raid. Current code reads shared world state and exchanges preset Footprints; it
does not yet submit authoritative focus contributions. That requires a separately
verified, idempotent server integration before advertising global damage as live.
