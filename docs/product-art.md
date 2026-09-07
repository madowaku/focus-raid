# Replaceable product artwork

Screens use `CompanionArtwork` / `BossArtwork`. `ArtworkCatalog` resolves an exact
identity, growth stage and presentation slot to a drawable, or the existing Canvas
fallback. Drawable entries support Android vectors and raster resources without
changing screens. No animation dependency was added.

Production defaults remain procedural **placeholder artwork**. Generated images
are unapproved candidates in `app/src/debug/res/drawable-nodpi`, excluded from the
release variant. Launch `VisualQaActivity` with `--ez concept_art true` to preview
the candidate catalog. Unfilled states keep their correct procedural fallback;
an idle image is not silently reused for a celebration or defeated boss.

## Candidates generated on 2026-09-08

Built-in image generation was used, with genuine transparent PNG output. The
original outputs were copied into the repository without deleting the originals.
No API-key/CLI fallback was used. These are painted illustrations, not final
48px pixel sprites; final style, raster size, pose sheets and approval remain open.

| Resource | Slot | Prompt specification |
|---|---|---|
| `rag_hatchling_idle_concept.png` | Rag / hatchling / idle | Full-body warm red-orange baby dragon, oversized head, two pale-gold horns, cream muzzle/belly, red wing/orange membrane, thick dark-tipped tail, brown adventurer backpack, short legs, dark eye highlight. Calm right-facing three-quarter pose; crisp chunky painted game art; transparent square; no text/environment. Palette #ef4a35/#ff7352/#b9323a/#ffd58a/#3a2330. |
| `volga_normal_concept.png` | Volga / normal | Full-body massive charcoal-purple armored ash dragon, swept horns, bat wings, orange ember cracks and eyes, grounded left-facing stance. Ancient and formidable, no horror/gore; painted chunky mobile silhouette; transparent square; no text/environment. |
| `miko_hatchling_idle_concept.png` | Miko / hatchling / idle | Tiny mint-green leaf fox adventurer, cream muzzle/chest/tail tip, violet eyes, leaf ears, gold leaf clasp, brown satchel and blanket, short legs and curling tail. Calm right-facing pose matching Rag's painted game style; transparent square; no text/environment. |
| `mord_normal_concept.png` | Mord / normal | Ancient quadruped tortoise-golem of mossy dark-teal rock, root legs, emerald crystal shell, amber eyes, compact powerful left-facing silhouette. Painted fantasy mobile art; emerald/mint/deep-purple palette; transparent square; no text/environment. |

All characters were visually inspected after generation. Candidate images are
static; the active focus screen retains quiet procedural poses by default.

## Replacing approved assets

1. Add the approved drawable to main resources.
2. Map its exact `ArtworkKey` in `ArtworkCatalog.Production`.
3. Run mapping tests and device captures at both target sizes and large fonts.
4. Preserve descriptive semantics and avoid implying a shared boss was defeated
   merely because one local focus session finished.
