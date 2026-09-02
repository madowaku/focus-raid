# Rag BABY sprite bible

Rag is the first dragon-type Focus Raid companion and the visual seed for the companion pipeline.

## Character promise

Rag is a tiny dragon who behaves like a brave adventurer before being physically ready for the job. He carries a backpack that looks a little too heavy, tries hard during raids, and returns tired rather than triumphant. The character should read as determined, warm, and slightly over-equipped, never aggressive.

## Silhouette invariants

These features must survive every pose and future animation frame:

- oversized head relative to torso
- two pale-gold horns, with the rear/right horn slightly taller
- red-orange body with a cream muzzle and belly
- one small red wing with warm orange membrane visible in profile
- thick tail with a dark-red tip region
- brown adventurer backpack that remains present through BABY exploration states
- large dark eye with one bright highlight pixel
- short legs and low center of gravity

If a generated frame loses the horns, cream belly, wing, backpack, or thick tail, reject it rather than correcting the identity later.

## Production scale

- canonical design grid: `32 x 32` logical pixels in the current SVG prototype
- target exported sprite seed: `48 x 48 px` transparent PNG
- supported presentation sizes: `16 / 24 / 32 / 48 px` and integer multiples
- scaling: nearest-neighbor only
- anchor: bottom-center / feet center
- no anti-aliasing inside final raster sprites

The SVG prototype intentionally uses a 32-pixel logical grid so shape and palette can be reviewed in source control. A future PNG sprite strip should preserve the same silhouette and anchor.

## Locked palette family

| Role | Hex |
| --- | --- |
| outline | `#3a2330` |
| body | `#ef4a35` |
| body highlight | `#ff7352` |
| body shadow | `#b9323a` |
| belly | `#ffd58a` |
| belly highlight | `#fff0bf` |
| horn | `#ffe08a` |
| horn shadow | `#d79a52` |
| wing | `#d93b42` |
| wing membrane | `#f38e61` |
| backpack | `#8b573a` |
| backpack highlight | `#bd7b4c` |
| backpack dark | `#5f3a31` |
| eye | `#121725` |
| spear blade | `#d9ecff` |
| spear handle | `#8a5b3c` |

Minor palette tuning is allowed only if contrast fails at 16px. Do not introduce a new body hue per animation.

## MVP poses

### IDLE

Used on HOME and LOG.

- upright stance
- wings folded
- backpack visible
- tiny two-step breathing/bob animation
- expression: ready, curious

### DEPART

Used during FOCUS exploration.

- slight forward lean
- wings more open
- staggered feet
- backpack remains visibly heavy
- tiny dust pixels allowed
- movement uses stepped animation rather than smooth interpolation

### RETURN

Used on RESULT / camp.

- lowered posture
- tired but content
- backpack sits lower on the body
- one or two sweat/breath pixels allowed
- never communicate failure or guilt

### RAID

Reserved for raid presentation.

- world-armory spear raised
- wings open
- feet planted
- backpack may remain because Rag arrives from expedition
- readable as brave rather than violent

## Animation targets

When the PNG sprite pipeline replaces the current SVG poses, target:

| Animation | Frames | Frame duration | Loop |
| --- | ---: | ---: | --- |
| IDLE | 6 | 200ms | yes |
| DEPART | 8 | 120ms | transition / short loop |
| RETURN | 8 | 120ms | transition |
| RAID | 8 | 120ms | action |
| REST | 6 | 200ms | yes |

Generate each full strip in one pass from the approved seed frame. Do not generate frames independently because silhouette and costume drift are especially visible on Rag's horns and backpack.

## Product-screen mapping

- HOME: `idle`
- FOCUS: `depart`
- RESULT: `return`
- LOG: `idle`
- WORLD RAID: `raid`

During FOCUS the character animation is secondary to the timer. Rag must never become a distracting looping spectacle.

## Growth compatibility

ADVENTURER and VETERAN must preserve the BABY identity rather than replacing it:

- same horn family, larger over time
- same warm red-orange body family
- same cream belly
- backpack evolves but remains recognizable as the original travel bag lineage
- wings grow, but the silhouette stays compact enough for mobile UI

The user's focus history, not rarity, should create later individual differences.
