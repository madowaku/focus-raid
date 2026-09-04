# Companion growth

Rag grows only from cumulative credited focus minutes. Growth never rolls back when a streak ends and cannot be accelerated by paid power.

## Stages

| Stage | Cumulative focus | Visual language |
| --- | ---: | --- |
| 卵 | 0–74 min | Glowing egg, visible crack, no dragon silhouette yet |
| 幼体 | 75–719 min | Round body, tiny horns, short wings |
| 第一成長 | 720–1,799 min | Taller body, longer horns and wings, tail appears |
| 第二成長 | 1,800–4,499 min | Broad wings, longer tail, shoulder spikes |
| 成熟 | 4,500+ min | Largest wings and horns, crown horn, brighter core aura |

The silhouettes are drawn with Compose Canvas today so every state remains deterministic and testable before final sprite production. The growth stage is supplied by `CompanionGrowth`, the same pure Kotlin rule used by the progress UI.

## UI contract

- HOME, RAID, ABORTED and VICTORY render Rag in the player's actual current stage.
- The Companion tab renders the current form as the hero image.
- The Companion tab also shows all five forms as a progression strip. Future forms are deliberately dimmed and named `???` until unlocked.
- The current stage label and visual form must always agree.
- The egg stage must render as an egg, not a baby dragon.

## Visual QA

Visual QA captures every growth silhouette at 360×800. The regular hatchling Companion screen is also captured at 720×1280 with the rest of the standard large-device QA set.

This protects the main progression promise: spending real focus time should visibly change the companion, not only increment a number.
