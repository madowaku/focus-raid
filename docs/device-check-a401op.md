# A401OP release-candidate device check

Use a physical A401OP on Android 15 with a test profile. The script rejects emulator serials and any attached device whose `adb devices -l` model and `ro.product.model` are not both `A401OP`. It installs `app-debug.apk` and replaces the app's active focus session; finish or abandon any personal session before starting. It saves screenshots and the operator's answers under `output/device-check/<timestamp>/` (ignored by Git). Do not interpret a screenshot or an adb probe as a human PASS.

## Preparation

1. Enable USB debugging, unlock the A401OP, and confirm `adb devices -l` shows `model:A401OP` and `device` (not `offline`). Never use the `focus_raid_qa` emulator serial.
2. Install/open Health Connect and a step source that records on this device. Note Health Connect's selected sources, day and time zone. Have a real walk available; an adb step injection is not evidence.
3. Build the debug APK: `./gradlew.bat assembleDebug`.
4. Run `powershell -ExecutionPolicy Bypass -File .\scripts\device-check-a401op.ps1 -Serial <physical-A401OP-serial>`.
5. Follow each prompt. Enter `PASS`, `FAIL`, `BLOCKED` or `NOT TESTED` and an observation. The script captures each prompted screen. Review `results.md` and screenshots afterward.

The script restores the prior `font_scale` and leaves the device out of forced Doze. It changes notification and exact-alarm permission states while testing; review their final states in Android Settings and restore your preferred settings afterward. Its `CLEAR_TIMER` broadcast clears debug timer state. A failed or interrupted run may leave permission state changed; check it manually.

## Expected behavior and evidence

| Check | Expected result | Evidence to record |
| --- | --- | --- |
| Notifications denied, then re-granted | Focus completes without notification access; granting access permits completion notifications. | Permission state, completion time, notification observation. |
| Exact alarm allowed, background, screen off, forced Doze | Debug `SEED/PROBE/CLEAR` can seed and inspect a session; completion notification and persisted result survive screen off/idle. | Probe values (`exact`, `completionEpochMillis`, `phase`, `finishedId`), actual elapsed time, reopened UI. |
| Exact alarm denied, background, screen off | Focus remains recoverable and completes after reopening even if Android delays the inexact alarm. No exact delivery deadline is assumed. | Denied state, completion/notification timing, reopened result and credit. |
| Exact-alarm deny and re-grant | Optional access can change without blocking the Free focus timer. | Android Settings state and app response for both states. |
| Home, focusing, walking notebook, paywall, consent dialog at font scale 1.5 | On a 360x800-class viewport, text and controls remain visible or scrollable, with no clipping or inaccessible primary action. | Actual display dimensions, five screenshots, any scrolling or clipping. |
| READ_STEPS deny, grant, revoke, re-grant | Focus remains usable. Walking state clearly reflects denial and resumes only after permission and a deliberate refresh. | Each Health Connect permission state and matching notebook state. |
| Health Connect rationale | Permission-usage/rationale screen shows a Privacy Policy link; it opens the published HTTPS policy. | Screenshot, URL and whether browser opened. A debug build without configured policy URL may lack the link: record FAIL, do not infer release behavior. |
| Real walking | Focus Raid's count is compared with Health Connect's own count for the same calendar day and time zone. Missing data is distinct from zero. | Both counts, step source, time zone, refresh time and any discrepancy. |

The script's debug receiver drives short diagnostic timers. Also do a normal UI-started focus session with the app backgrounded and screen off before signing off the candidate; note its selected duration, start/end times and whether the result appears after reopening. Do not replace this with the seeded diagnostic result.

## Operator results (leave blank until physical testing)

| Check | Result | Observation / evidence path |
| --- | --- | --- |
| Android version and actual viewport |  |  |
| Notification deny / re-grant |  |  |
| Exact allowed: background, screen off, Doze |  |  |
| Exact denied: background, screen off |  |  |
| Exact-alarm UI deny / re-grant |  |  |
| Normal UI-started focus session |  |  |
| Font 1.5: home |  |  |
| Font 1.5: focusing |  |  |
| Font 1.5: walking notebook |  |  |
| Font 1.5: paywall |  |  |
| Font 1.5: consent dialog |  |  |
| READ_STEPS deny / grant / revoke / re-grant |  |  |
| Health Connect rationale and policy link |  |  |
| Real walk: Health Connect versus Focus Raid |  |  |
