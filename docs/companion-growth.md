# Companion Growth

Focus Raid's companion growth is a direct reflection of cumulative credited focus time.

## Rules

- 1 credited focus minute = 1 companion growth minute.
- Growth never rolls back when a streak breaks.
- Paid features never accelerate growth.
- Early-ended sessions still contribute any credited whole minutes.
- Mature companions keep accumulating shared focus time even after the final visual form is unlocked.

## Stages

| Stage | Cumulative focus | Visual identity |
| --- | ---: | --- |
| Egg | 0–74 min | Glowing egg, shell cracks |
| Hatchling | 75–719 min | Round body, small horns and wings |
| First growth | 720–1,799 min | Larger wings and horns, tail appears |
| Second growth | 1,800–4,499 min | Larger silhouette, longer tail, shoulder spikes |
| Mature | 4,500+ min | Largest wings and horns, crown horn, stronger core aura |

`CompanionGrowth` is the single source of truth for stage, next threshold, remaining minutes, and within-stage progress. HOME, RAID, result, and Companion surfaces all render from that same domain state.

## Evolution reveal

When credited focus changes the companion stage, `CompanionGrowth.evolutionBetween(beforeMinutes, afterMinutes)` returns the old and new stages. The session result stores that one-time evolution event.

On a normal completion, the VICTORY screen inserts a rare `RAG EVOLVED!` card ahead of the normal damage/reward stack. It briefly shows the previous form, then reveals the new form after about 650 ms. The rest of VICTORY remains available by scrolling, so evolution feels special without turning every focus completion into a cutscene.

If an early-ended session still crosses a threshold, ABORTED also acknowledges the evolution in a compact banner. This preserves the core rule that credited focus time is never rolled back just because a session ended early.

Evolution state is cleared when the result is dismissed or a new session starts.

## Companion screen

The Companion tab shows:

- current visual form
- total time spent together
- today's credited focus
- progress toward the next stage
- remaining minutes to the next stage
- a five-form progression strip

Future forms are dimmed and labeled `???` until unlocked. This exposes the existence of future growth without spoiling the exact unlocked silhouette too aggressively.

## QA

Visual QA includes every companion form at 360×800 and an explicit evolution result at both 360×800 and 720×1280. Domain tests cover exact thresholds, no-op sessions, and forward-only evolution detection.
