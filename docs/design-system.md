# Focus Raid Material 3 Expressive design system

## Direction

**Material 3 Expressive productivity UI × nighttime fantasy raid world**

The timer is always the hero. The game creates emotional context around it.

## Core principles

1. START wins the hierarchy.
2. The game recedes during focus.
3. READY invites; RAID quiets; VICTORY rewards.
4. State changes carry the expressive motion, not every control.
5. Navigation disappears while focusing.
6. Character art is replaceable and must never determine layout geometry.
7. Respect reduced-motion and Android accessibility defaults.

## Baseline viewport

Primary visual QA:

- 360 × 800 dp
- 720 × 1280 px equivalent reference

Layout:

- horizontal margin: 16dp
- 4dp grid
- preferred spacing: 8 / 12 / 16 / 24 / 32dp
- minimum interactive target: 48 × 48dp
- edge-to-edge with system insets, never hardcoded status-bar height

## READY

- Top area: 56dp
- Timer hero: 184dp diameter
- Timer text: 60sp
- Duration chips: 48dp height
- Expedition chips: 48dp height
- Primary CTA: 64dp
- Boss card: rounded 28dp container
- Bottom navigation visible

## RAID

- Timer hero grows to 208dp
- Timer text: 68sp
- Pause/resume: 64dp
- Exit is a low-emphasis text action
- Boss information collapses
- Bottom navigation hidden
- No persistent busy animation around the timer

## VICTORY

- Completion headline
- Credited minutes
- Damage as the primary result number
- One compact boss HP card
- One compact reward card
- Primary CTA repeats the selected focus duration
- Secondary action returns to READY

## Color roles

- Primary violet: focus identity / hero emphasis
- Raid coral: boss danger and battle accents
- Reward gold: rare reward emphasis
- Deep navy-purple surfaces: nighttime world
- Teal is reserved for future forward-progress/world-link accents

Use Material color roles in Compose. Do not scatter raw colors throughout feature code.

## Motion

`MaterialExpressiveTheme` + `MotionScheme.expressive()` is the app theme baseline.

Hero transitions may use expressive spatial motion:

- READY timer -> RAID timer
- start CTA -> active controls
- RAID -> VICTORY result reveal

Routine repeated interactions should remain restrained.

## Production art

The current Kotlin vertical slice uses lightweight placeholder glyphs so layout and timer behavior are deterministic. Production sprite work follows `docs/rag-baby-sprite.md` and should replace glyphs with normalized PNG/WebP sprite sheets without changing layout contracts.
