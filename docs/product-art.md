# Production artwork

At the user's follow-up request on 2026-09-08, generated artwork replaces the simple
companion/egg/boss Canvas drawings in the normal release build. All 45 companion
states, nine boss states and 36 known items resolve to registered raster artwork.
The earlier four debug concepts have been replaced by seven release atlases.

Screens still use `CompanionArtwork`, `BossArtwork` and `ItemArtwork`. The catalog
supports registered atlas frames and individual vector/raster drawables. Procedural
fallbacks remain only for deliberately unmapped slots; an empty catalog still has
safe fallback behavior. Unknown legacy item names remain visible, even if no image
exists for them. No animation or network image dependency was added.

## Rendering and registration

The adopted atlases are opaque midnight-backed painted panels, not transparent
sprites. Two attempts at alpha generation returned a painted checkerboard; those
outputs were rejected. Subsequent dark-background artwork was generated with the
built-in image tool and copied unchanged into `app/src/main/res/drawable-nodpi`.
No generated PNG pixels were edited in scripts.

`ProductionArtworkCatalog.kt` maps identity, growth stage and mood to exact frames.
`ArtworkFrameRegistration.kt` contains per-frame display bounds. Generated layout
is not assumed to be a mathematically perfect grid: separate registration retains
wings/tails and centers eggs/babies without excessive empty space. The read-only
`scripts/register-art.py` analyzes the originals and regenerates these coordinates;
Pillow is required to run it. `docs/art-registration.json` records source hashes,
sizes and normalized bounds. Visually review bounds after replacing an atlas.

`AtlasArtwork` draws a source region directly, without allocating cropped copies.
A shared 48 MiB LRU cache bounds decoded sheet retention; views may retain a sheet
while it is displayed. The seven production images decode to roughly 42 MiB in
total. Rendering remains static during focus. Descriptions preserve the companion,
stage and mood, boss state, or item name.

## Assets and generation prompts

See [release art/content report](release-art.md) for the adopted files and generation
prompt set. Originals remain in the Codex generated-images directory; all runtime
assets are committed inside the repository.

## Replacement workflow

1. Generate/choose replacement art for the required exact identities/states.
2. Copy it into main resources; keep original generation outputs.
3. Update registration gutters if necessary, then run `python scripts/register-art.py`.
4. Run catalog tests, the device decoder/registration test and gallery captures.
5. Inspect actual timer, result, companion and raid screens at both target sizes.

`VisualQaActivity` phases `ART_RAG`, `ART_MIKO`, `ART_LUNE`, `ART_BOSSES`,
`ART_ITEMS_TOWER`, `ART_ITEMS_ABYSS`, `ART_ITEMS_STAR` show production renderer
registration. `COMPANION_LUNE` shows the new companion in the real overview screen.
The old `concept_art` intent flag no longer swaps in a partial placeholder catalog.
