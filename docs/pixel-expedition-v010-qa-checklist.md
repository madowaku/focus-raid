# v0.10 Pixel Expedition QA checklist

## Static / unit
- [ ] `testDebugUnitTest`
- [ ] `lintDebug`
- [ ] `assembleDebug`
- [ ] `compileDebugAndroidTestKotlin`
- [ ] `git diff --check`

## Visual QA
- [ ] `PIXEL_FOCUS_25_00`
- [ ] `PIXEL_FOCUS_12_30`
- [ ] `PIXEL_FOCUS_00_59`
- [ ] `PIXEL_FOCUS_PAUSED`
- [ ] font scale 1.5
- [ ] Reduce Motion

## Product gate
- [ ] 25:00 and 00:59 clearly communicate different distance to the raid
- [ ] companion reads as a traveler inside the world
- [ ] boss reads as a destination, not a card thumbnail
- [ ] 12:30 feels like “already on the ridge” before it feels like “12 minutes left”
- [ ] world does not distract from focus controls
- [ ] no fake live-user counts or activity
