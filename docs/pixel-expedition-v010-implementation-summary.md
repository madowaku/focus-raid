# v0.10 Pixel Expedition vertical slice

Current GitHub-only implementation status:

- Deep / 深層 focus sessions route through `FocusRaidV10Root`.
- `FocusUiState.progress` remains the single source of journey progress.
- `PixelExpeditionWorld` maps progress onto CAMP / PATH / RIDGE / GATE / RAID and a monotonic virtual camera.
- `PixelExpeditionFocusingScreen` keeps a modern timer / pause / end HUD over the world layer.
- READY, FIRST RUN, Return Raid, billing and other expeditions remain on the v0.8 shell.
- Debug-only `PixelExpeditionQaActivity` exposes deterministic 25:00, 12:30, 00:59 and PAUSED states.
- JVM stage/camera tests and Compose UI state tests were added.

## Important limitation

This work was authored directly on GitHub while local Codex execution was unavailable. Gradle compilation, lint and A401OP screenshots have **not** yet been run against the current head. Treat the branch as a reviewable vertical slice, not release-ready code, until local QA passes.
