# Companions, personal boss and inventory

Added at the user's request during issue #5. These are Free progression features;
they add no subscription, paid power, new Pro expedition or backend service.

- Rag remains available from the beginning.
- Miko, the leaf fox, unlocks at 75 cumulative credited minutes. Selection persists
  in DataStore. All companions share existing earned growth, so switching cannot
  reduce minutes or reset progress. Both use the same five growth thresholds and
  idle/focused/celebrate artwork slots, now supplied by production raster art.
- Lune, the moonlight owl, unlocks at 180 cumulative credited minutes. Its selection
  persists under the same rules as Rag and Miko; all three share earned growth.
- Mord, the root-crystal beast, is a **personal Abyss raid** shown below the shared
  Volga raid. Every credited Abyss minute, including early-ended sessions, counts
  toward 180 HP. Progress derives from distinct persisted session IDs. It does not
  modify the shared world document or pretend to be another player's contribution.
- Zephyr, the star-storm bird, is a personal Tower raid at 300 HP. Only distinct
  credited Tower sessions count; Abyss and Star Route time do not. Like Mord,
  partial sessions count and the challenge grants no paid power or extra rewards.
- Inventory is shown in Companion. Existing discovery names/rarities are moved
  into a stable item catalog with the original IDs preserved. Two items are appended to each expedition
  (one common and one rare), for 36 total; rarity probabilities and the discovery
  cadence are unchanged. Existing saved discoveries are never rerolled. Counts derive from complete
  local history, including older-than-Free-history-limit records. Unknown legacy
  item names remain visible. Items are keepsakes, not focus/purchase multipliers.

The shared-world vision is that many people's focused minutes combine into one
raid. Current code reads shared world state and exchanges preset Footprints; it
does not yet submit authoritative focus contributions. That requires a separately
verified, idempotent server integration before advertising global damage as live.

Added keepsakes: Tower — 風音の鈴 / 嵐羽のブローチ; Abyss — 苔灯のランタン /
根晶の印章; Star Route — 月読のしおり / 彗星インク. Every catalog item has a
specific image in inventory and session results. Star Route still requires Pro;
the companion and personal Tower/Abyss bosses do not.
