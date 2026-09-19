# Focus Raid v0.12 HD-2D Subtraction Polish

This document is the implementation brief for the visual refinement pass tracked by the new v0.12 issue.

## North star

> 世界への没入を強めるものは残す。説明のためだけに載っているUIは削る。

Focus Raid should read as one continuous world:

- Home = 出発地点
- Focusing = 遠征
- Return Raid = 火口の広場
- Companion = 同じ世界で育つ仲間

## Keep

- World-first composition
- Camp → route → gate → volcano continuity
- Rag as a traveler in the world
- Volga as a destination / physical presence
- Anonymous shared-focus lights
- Modern, quiet controls
- One primary CTA per screen
- Readable timer and boss HP

## Reduce

- Purple surface area
- Large rounded cards
- Duplicate explanations
- Mechanical labels
- Equal-weight actions
- Result metadata shown before the emotional result

## Remove

- Development-facing labels
- World framed as a generic image card
- Duplicate boss/companion art when the world already communicates it
- Constant decorative animation
- Fake social presence
- Generic result-card stacks

## Screen-specific target

### Home
The first read is: “I start here and travel toward that volcano.”

### Focusing
At 12:30 the first read is: “I’m already on the ridge.”

### Return Raid
The first read is: “I crossed the gate and arrived here.”

### Companion
The first read is: “This companion travels with me.”

## Engineering guardrails

Do not rewrite timer/session/persistence/contribution/FIRST 25/billing/network logic.

Primary QA target: 360×800, then font scale 1.5 and Reduce Motion.
