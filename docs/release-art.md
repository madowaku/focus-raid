# Adopted artwork and adventure expansion

2026-09-08 follow-up to issue #5: the user explicitly requested replacing the simple
placeholder eggs/companions/bosses with production images, one more companion,
and additional illustrated bosses/items. This tranche adopts seven generated
atlases in the release source set, with 90 individually registered display frames.
It supersedes the candidate-only artwork status in the earlier productization report.

## Implemented

- Rag, Miko and new **Lune / ルネ** each have egg, hatchling, first growth, second
  growth and mature artwork, with idle/focused/celebrate states: 45 frames.
- Lune is earned at **180 cumulative credited minutes**, selectable and persisted.
  Growth time is shared; no reset, paid strength or timer distraction was added.
- Volga, Mord and new **Zephyr / 星嵐鳥ゼファー** have normal/damaged/defeated art:
  nine frames. Zephyr is a **personal Tower challenge at 300 HP**. Mord remains the
  personal Abyss challenge at 180 HP; shared Volga state remains distinct.
- All 30 existing inventory items are illustrated; six new keepsakes bring the
  catalog to 36. Their images also appear in session results. New drops are
  appended within common/rare pools; existing IDs, saved discoveries, rarity
  probabilities and the 25-minute discovery cadence remain intact.
- All ordinary character slots now resolve to image assets, with exhaustive
  tests. Simple Canvas drawings remain only as unmapped-slot fallback code.
- Seven shared sheets use a bounded decoded-image cache; individual frame drawing
  does not allocate cropped bitmaps. Artwork remains static during concentration.

## Adopted source files and prompt set

All files below are under `app/src/main/res/drawable-nodpi/`. Generation used the
built-in `image_gen` tool, not a CLI/API-key fallback. The PNG files were copied
unchanged from `C:/Users/hiro/.codex/generated_images/01a07dd7-12ff-7b30-bdc4-0b7f37f2bab6/`.
Source dimensions, SHA-256 and final display bounds are in [art-registration.json](art-registration.json).

Common final prompt direction: polished painted fantasy mobile RPG art with clear
silhouettes, whole subjects separated in atlas cells, no labels/text/grid/borders,
midnight purple background. These are intentionally opaque art panels. Initial
checkerboard outputs were rejected; no false transparency claim is made.

| Adopted file | Original generated filename | Prompt / cell specification |
| --- | --- | --- |
| `art_rag.png` | `exec-f44b3bfb-db74-4af8-9cd8-650245e99daf.png` | Preserve the 5×3 Rag atlas; replace the checkerboard with solid midnight purple; keep every sprite and add clear margin around horns/feet. Columns: warm ivory/red egg, orange baby dragon, young explorer, horned/scarf adolescent, mature friendly guardian. Rows: idle, quiet focused, joyful celebration. Cream belly, pale-gold horns, leather satchel. |
| `art_miko.png` | `exec-a317d546-aade-47ce-9d95-90fd708f72c9.png` | New 5×3 atlas matching Rag's layout/style. Mint leaf fox, cream muzzle/tail, violet eyes, leaf ears, satchel and gold leaf clasp. Columns: leaf egg, baby, young scarfed fox, adolescent leafy mane, mature flowing leafy guardian. Rows idle/focused/celebrate; complete tails in each cell. |
| `art_lune.png` | `exec-1a8425c9-1eb2-44d4-adf6-992cf94c95f2.png` | New 5×3 moonlight owl atlas: navy/lavender feathers, cream heart-shaped face, amber eyes, crescent mark, brass lantern and book satchel. Columns: crescent egg, fluffy baby, young scholar, layered-feather adolescent, noble feather-caped guardian. Rows idle, eyes-closed focus, lifted-wing joy. |
| `art_bosses.png` | `exec-5fca3a5c-9a85-4916-a1a2-426c67c7a09c.png` | 3×3 boss atlas. Columns: charcoal/ember ash dragon Volga; emerald crystal/root tortoise-golem Mord; cobalt/turquoise storm bird Zephyr with gold crest, cream chest and cloud tail. Rows normal, fatigued/damaged, peacefully defeated/resting. No gore; complete silhouettes. |
| `art_items_tower.png` | `exec-ac0bc1e5-35e9-495e-9109-d09836e8c312.png` | 3×4, row-major: iron sword, wooden bow, iron ore; wooden shield, silver spear, ice bow; knight shield, lightning-cannon mechanism, azure spear; star greatsword, ribbon wind bell, storm-feather brooch. Clean isolated readable steel/sky/gold keepsakes. |
| `art_items_abyss.png` | `exec-aac9c34a-bc71-403a-9aa0-0d415dae822a.png` | 3×4: violet magic stone, old map fragment, herbs; blue crystal, ancient key, fire talisman; moon quartz, resonance crystals, analysis lens; abyss compass, moss lantern, root/crystal seal. Match Tower's painted style. |
| `art_items_star.png` | `exec-3c1d338a-0e89-4bf8-9965-267aa90f7cd6.png` | 3×4: star-sand vial, torn star chart, guide stone; nautical tag, comet compass, luminous sail; stargazer lens, star-ring engine fragment, void scroll; celestial sphere, moon bookmark, comet ink. Restrained gold/violet/blue highlights. |

New Japanese item names: 風音の鈴 / 嵐羽のブローチ, 苔灯のランタン / 根晶の印章,
月読のしおり / 彗星インク. Exact item-to-frame registration uses stable IDs and an
explicit illustrator ordering, so inserting a new drop cannot shift existing icons.

The first runtime gallery review found oversized empty gutters around eggs and
babies. `scripts/register-art.py` now reads raster brightness within reviewed gutters,
adds outline-safe margins and records tight display bounds. It never edits PNG
pixels. A second runtime review checked those bounds against all 90 frames.

## Verification

- `gradle testDebugUnitTest`: **56 tests passed**. Existing fallback tests now target
  an explicitly empty catalog; new production tests require all 90 unique images.
  Added Lune threshold, Zephyr deduplication/expedition scope and retained item-ID tests.
- `gradle connectedDebugAndroidTest`: **12 tests passed** on API 36, 360×800.
  Includes all-frame resource decoding/bounds/nonblank checks, actual Lune selection
  and home identity, DataStore persistence, and the prior billing/navigation/accessibility tests.
  A new result-screen test checks non-overlapping labels and the reachable Done action at font 1.5.
- `gradle lintDebug assembleDebug bundleRelease`: passed; **0 errors, 23 warnings,
  2 hints**. No test assertions were removed to mask a product failure.
- Local visual pass: `bash artifacts/capture-release-art.sh` captures 32 images:
  seven art galleries and seven real app states at 360×800 and 720×1280, plus four
  real states at font scale 1.5, plus scrolled Zephyr and 720×1600/density-320 Lune checks.
  Review found and fixed overlapping result labels at font 1.5; those result captures were refreshed.
  Final capture review is recorded by the task receipt.
- `scripts/capture-android-ui.sh` retains the original 113 outputs and adds 16
  production-art/new-companion captures: **129 required CI PNGs**.
- `git diff --check`: passed.

Local images are under ignored `artifacts/visual/art-release-*.png`; build/device
reports remain under `app/build/reports/`. The fixed atlas originals and registration
metadata are committed, so the complete CI capture gate is reproducible.

Play signing, real store billing, Firebase deployment/two-installation testing and
privacy/store publishing remain the external tasks already listed in
[release-readiness](release-readiness.md). This expansion does not implement or
claim authoritative worldwide focus aggregation.
