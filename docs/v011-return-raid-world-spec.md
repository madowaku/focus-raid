# Focus Raid v0.11 Return Raid World Integration

Goal: make the completion flow feel like the same place reached during Pixel Expedition, not a jump back to a card-based result screen.

## Product loop

CAMP -> PATH -> RIDGE -> GATE -> RAID ARENA -> OTHER LIGHTS -> SELF STRIKE -> RESULT -> CAMP

The user should feel that the road shown during focusing physically continues into the raid arena.

## Scope

- ABYSS + Rag vertical slice only.
- Reuse the existing `ReturnRaidSequenceStateMachine`, `RaidFocusArrival`, SFX, haptics, contribution data and persistence.
- No new timer/session/network state.
- FIRST 25 remains reserved exactly as today.

## World direction

### RETURNING
- Start at the volcanic gate / threshold seen at the end of focusing.
- The camera settles into the raid arena beyond the gate.
- Copy: `火口へ到着した` / `${creditedMinutes}分の遠征が、ここへ届いた。`

### ECHO
- Keep Volga physically present in the arena.
- Up to two existing real echoes arrive as anonymous lights from different edges / paths.
- Each arrival triggers the existing HP step, audio and impact feedback.
- Do not invent companions or player counts.

### YOUR_TURN
- Rag is visible at the near side of the arena.
- Other lights remain as traces around the party line.
- Copy keeps the strong existing promise: `そして、あなたの{minutes}分が届く。`
- Primary action remains `一撃を刻む`.

### STRIKING
- Rag / self light advances one visual step.
- Existing self strike arrival and impact are emphasized.
- HP decreases in sync with sound and haptic.

### RESULT
- If not defeated: Volga remains standing, damaged.
- If defeated: short restrained completion state; no explosive arcade ending.
- Keep result text concise.

### CAMP
- Return to a camp / ember scene, not a generic card stack.
- Existing footprints / again / done actions remain available.

## Visual principles

- World fills the scene; cards are secondary overlays only where needed for legibility.
- Preserve modern, quiet controls.
- Use volcanic warm light and depth from Pixel Expedition.
- Other users are lights / traces unless real companion identity is available.
- Self is the selected companion where the current slice supports it; v0.11 may remain Rag-only.

## QA gate

At the transition from focusing to Return Raid, the first impression should be: `I arrived at the place I was walking toward.`

Required verification:
- existing ReturnRaid state machine order unchanged
- echo HP steps preserved
- self strike HP/audio/haptic sync preserved
- victory audio once
- no duplicate audio on recomposition
- FIRST 25 unchanged
- 360x800
- font scale 1.5
- Reduce Motion
- unit / lint / assemble / androidTest compile / diff check
