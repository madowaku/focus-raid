# UI SFX audio selection

Focus Raid uses a small, hand-picked subset of the UI SFX `zen` pack for the
runtime cues that previously used sharper impact/interface sounds. The pack is
well suited to a focus product: short one-shots, quiet tails, and a calm
productivity-oriented sound character.

Source: [UI SFX](https://uisfx.com/) and its canonical [GitHub repository](https://github.com/romainsimon/uisfx)
at commit `9950fe66f993a6660dab9c2651dcbcd899ffd83b` (package version `0.4.0`).
The source package's `LICENSE-AUDIO` identifies the audio as CC0-1.0.

## Runtime mapping

Only these nine OGG files from `packages/uisfx/sounds/zen/` were admitted to
the app. Existing `fr_*.ogg` files from the earlier audio pass remain
untouched; the SoundPool path reads the `fr_sfx_*.ogg` resources below.

| Runtime resource | UI SFX source | Duration | Role |
| --- | --- | ---: | --- |
| `fr_sfx_focus_start.ogg` | `zen/start.ogg` | 0.408s | Focus start |
| `fr_sfx_ui_confirm.ogg` | `zen/select.ogg` | 0.260s | Important CTA confirmation |
| `fr_sfx_companion_knock.ogg` | `zen/press.ogg` | 0.162s | FIRST 25 companion tap |
| `fr_sfx_focus_complete.ogg` | `zen/complete.ogg` | 0.644s | Focus completion |
| `fr_sfx_raid_hit_self.ogg` | `zen/checkpoint.ogg` | 0.441s | World contribution |
| `fr_sfx_raid_hit_other_01.ogg` | `zen/receive.ogg` | 0.416s | Raid echo 1 |
| `fr_sfx_raid_hit_other_02.ogg` | `zen/drop.ogg` | 0.277s | Raid echo 2 |
| `fr_sfx_raid_hit_other_03.ogg` | `zen/reaction.ogg` | 0.359s | Raid echo 3 |
| `fr_sfx_raid_victory.ogg` | `zen/achievement.ogg` | 0.918s | Raid victory |

The SoundPool gain now follows the Zen pack's published default-volume range
(`0.17`–`0.26`) so the new cues remain audible without becoming startling.
Event IDs, at-most-once delivery, mute handling, and the existing notification
path remain unchanged.
