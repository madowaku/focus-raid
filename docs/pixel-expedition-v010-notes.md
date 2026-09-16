# Focus Raid v0.10 Pixel Expedition — GitHub-side implementation notes

Issue: #14
Branch: `feat/v0.10-pixel-expedition`

## Phase 1 now on branch

- `PixelExpeditionWorld.kt`
  - Deep Layer five-stage route model
  - progress -> stage mapping
  - progress -> monotonic virtual camera mapping
  - procedural first-pass WORLD renderer
  - camp / path / ridge / gate / distant raid boss language
  - quiet world lights that do not claim live-user activity
  - paused world darkening
- `PixelExpeditionWorldTest.kt`
  - stage boundaries
  - clamp safety
  - monotonic camera
  - paused / raid status meaning
  - ordered five-stage Deep Layer spec

## Deliberate limitation

This first GitHub-side commit does **not** replace the production Focusing screen yet.

Reason: without the local Android/Gradle runtime available in this session, the safest first step is to land the presentation model and renderer independently, then wire it into `SignatureFocusingScreen` in a small follow-up change once CI/static review is clean.

No timer, persistence, billing, First Run, Return Raid, or Firebase logic is duplicated or changed.

## Next GitHub-side steps

1. Wire `PixelExpeditionWorld(state.progress, paused)` into the current Focusing screen, replacing `SignatureJourneyRail` visually while preserving the existing timer and controls.
2. Add a compact boss HUD below the WORLD viewport.
3. Add debug Visual QA routes for 25:00 / 12:30 / 00:59 / PAUSED.
4. If GitHub CI is available, use it as the compile/test gate until local Codex access returns.
5. Final visual acceptance still requires A401OP 360x800 screenshots.
