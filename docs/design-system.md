# Focus Raid Visual Design System

## Direction

Focus Raid uses **modern productivity UI × nighttime pixel RPG**.

The timer remains calm and contemporary. Pixel art appears as a window into the shared fantasy world, especially before and after focus sessions and on the WORLD surface.

## Core principles

1. **START wins the hierarchy.** No card may compete with the main focus CTA on HOME.
2. **The game recedes during focus.** FOCUS uses one small expedition scene, one timer ring, and one compact task card.
3. **Pixel art carries fantasy, DOM UI carries information.** Do not turn the entire app into a retro game menu.
4. **WORLD is spatial, not dashboard-first.** Present Tower → Town → Abyss vertically before secondary stats.
5. **Strong motion is reserved for departure, return, rare loot, and raids.** Respect reduced-motion.
6. **Companion sprites are replaceable assets.** UI layout must not depend on one exact character silhouette.

## Color tokens

Defined in `src/styles.css`.

- Canvas: `#050912`
- Surface: `#0c1525`
- Raised surface: `#111d31`
- Border: translucent cool white
- Primary focus accent: `#48e2cf`
- Raid accent: `#a880ff`
- Legendary / armory accent: `#efc56a`
- Boss HP / danger: `#ff657f`
- Success / focus-link: `#73e7a3`

Teal means **focus / forward progress**. Violet means **unknown / raid / abyss**. Gold means **history / relic / armory value**. Red is reserved for **boss danger**, not ordinary navigation.

## Card language

- Default radius: 16–22px
- Hero cards: 28px
- Thin cool border, never heavy chrome
- Dark layered surfaces with restrained glow
- Cards group actions or state; avoid equal-weight dashboard grids on HOME

## Timer

- Large tabular numerals
- Circular progress ring using the focus teal
- No constant game animation around the clock
- One small expedition scene above the timer is the maximum persistent fantasy surface
- During focus, navigation is hidden

## Pixel art

Temporary MVP pixel primitives live in `src/ui/pixel.tsx`.

They are intentionally small and replaceable:

- `PixelRag`
- `PixelTower`
- `PixelAbyss`
- `PixelBoss`

Production assets should preserve:

- crisp nearest-neighbor rendering
- readable silhouette at 24–48px
- limited palette per sprite
- shared bottom-center anchor for companion animation
- consistent scale between IDLE / DEPART / RETURN / RAID frames

The approved production path is: **one approved seed sprite → whole animation strip → normalization → in-app preview**.

## WORLD composition

The primary WORLD map reads vertically:

1. Sky / WORLD TOWER
2. WORLD TOWN
3. WORLD ABYSS
4. WORLD ARMORY
5. NEXT WORLD RAID

WORLD should feel like a place whose frontier moves, not an analytics dashboard.

## Screen roles

### HOME

Companion → duration → personal expedition → START → destination choice → next raid → lightweight world summary.

### FOCUS

Expedition vignette → timer ring → current task / personal boss HP. Nothing else.

### RESULT

Return camp → discovery → focus/personal/world contribution → armory readiness → continue or leave cleanly.

### WORLD

Large spatial world map first, armory and raid second.

## Future asset rule

Do not make a sprite-generation system part of the core product architecture. Generated art is an asset pipeline concern. Runtime UI consumes normalized static sprite sheets so the app remains lightweight and deterministic.
