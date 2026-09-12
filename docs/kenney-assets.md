# Kenney integration audit — Focus Raid v0.2

監査日: 2026-09-12 JST

指定された展開先を走査したところ、実際のフォルダ名は `C:\Dev\AssetsShared\Kenney\kenney_light-masks-1.0`、`kenney_particle-pack`、`kenney_emotes-pack`、および `C:\Dev\AssetsShared\Audio\SE\kenney_impact-sounds`、`kenney_interface-sounds` でした。ZIPは参照・コピーしていません。

## 候補と選定

Light Masks はすべて 512×512 PNG。黒背景のように見える部分は透過マスクです。

| 種別 | 候補 | 判断 |
|---|---|---|
| Light Mask | `Default/circle_a.png` (38,943 B), `Default/circle_rings_a.png` (58,449 B), `Default/ring_a.png` (26,291 B), `Default/shape_a.png` (24,927 B) | 柔らかい円・細いリングを採用。密度の高い composed/noise 系は不採用。 |
| Particle | `circle_02.png` (43,380 B), `star_01.png` (42,118 B), `flare_01.png` (42,886 B), `twirl_01.png` (51,060 B), `star_06.png` (35,453 B) | 円・星・横フレア・小さな弧の4種を採用。雷・炎・煙・魔法陣は不採用。 |
| Interface SE | `confirmation_001.ogg` (0.290 s), `click_001.ogg` (0.100 s), `pluck_002.ogg` (0.165 s), `select_003.ogg` (0.383 s), `bong_001.ogg` (0.123 s), `open_001.ogg` (0.148 s) | 3種だけ採用。全ボタンには割り当てない。 |
| Impact SE | `impactBell_heavy_004.ogg` (0.301 s), `impactBell_heavy_003.ogg` (0.654 s), `impactWood_heavy_000.ogg` (0.313 s), `impactWood_medium_000..002.ogg` (0.333 s), `impactSoft_medium_001.ogg` (0.183 s) | Bell 2種と木質 impact 4種を採用。soft は候補止まり。 |
| Emote | `emote_stars`, `faceHappy`, `dots3`, `star`, `sleep`, `cloud`, `exclamation`, `idea` (各32×38 PNG) | Vector Style 3の8種を採用。既存の8定型足跡へ1対1で割り当て。 |

## Repoへコピーした素材

### VFX (`app/src/main/res/drawable-nodpi/`)

| Resource | 元ファイル | 用途 |
|---|---|---|
| `fr_vfx_light_soft.png` | `kenney_light-masks-1.0/Default/circle_a.png` | 集中完了の中心光 |
| `fr_vfx_light_ring.png` | `kenney_light-masks-1.0/Default/ring_a.png` | 完了・レイド貢献の拡散リング |
| `fr_vfx_particle_circle.png` | `kenney_particle-pack/PNG (Transparent)/circle_02.png` | 貢献時の短い輪 |
| `fr_vfx_particle_star.png` | `kenney_particle-pack/PNG (Transparent)/star_01.png` | 小さな達成のきらめき |
| `fr_vfx_particle_flare.png` | `kenney_particle-pack/PNG (Transparent)/flare_01.png` | 貢献時の横フレア |
| `fr_vfx_particle_twirl.png` | `kenney_particle-pack/PNG (Transparent)/twirl_01.png` | 余韻の弧 |

### SE (`app/src/main/res/raw/`)

Kenneyの元ファイルをそのまま Ogg Vorbis としてコピーし、resource名だけ `lowercase_snake_case` にしています。

| Resource | 元ファイル | 実装イベント |
|---|---|---|
| `fr_sfx_focus_start.ogg` | `kenney_interface-sounds/Audio/confirmation_001.ogg` | Focus開始 |
| `fr_sfx_ui_confirm.ogg` | `kenney_interface-sounds/Audio/click_001.ogg` | 「今日はここまで」など重要確定 |
| `fr_sfx_companion_knock.ogg` | `kenney_interface-sounds/Audio/pluck_002.ogg` | FIRST 25の卵タップ |
| `fr_sfx_focus_complete.ogg` | `kenney_impact-sounds/Audio/impactBell_heavy_004.ogg` | 集中完了・通知チャンネル |
| `fr_sfx_raid_hit_self.ogg` | `kenney_impact-sounds/Audio/impactWood_heavy_000.ogg` | 自分の世界レイド貢献 |
| `fr_sfx_raid_hit_other_01.ogg` | `kenney_impact-sounds/Audio/impactWood_medium_000.ogg` | 残響1 |
| `fr_sfx_raid_hit_other_02.ogg` | `kenney_impact-sounds/Audio/impactWood_medium_001.ogg` | 残響2 |
| `fr_sfx_raid_hit_other_03.ogg` | `kenney_impact-sounds/Audio/impactWood_medium_002.ogg` | 残響3 |
| `fr_sfx_raid_victory.ogg` | `kenney_impact-sounds/Audio/impactBell_heavy_003.ogg` | ボス撃破時のみ |

### Footprint Emotes (`app/src/main/res/drawable-nodpi/`)

すべて `kenney_emotes-pack/PNG/Vector/Style 3/` からコピーしています。既存の自由文・定型文APIは変更せず、表示だけを画像で補強します。

| Preset id | Resource | 元ファイル |
|---|---|---|
| `made_it` | `fr_emote_made_it.png` | `emote_stars.png` |
| `keep_going` | `fr_emote_keep_going.png` | `emote_faceHappy.png` |
| `almost` | `fr_emote_almost.png` | `emote_dots3.png` |
| `one_step` | `fr_emote_one_step.png` | `emote_star.png` |
| `rest` | `fr_emote_rest.png` | `emote_sleep.png` |
| `waiting` | `fr_emote_waiting.png` | `emote_cloud.png` |
| `fire` | `fr_emote_fire.png` | `emote_exclamation.png` |
| `strong` | `fr_emote_strong.png` | `emote_idea.png` |

## Runtime guardrails

- VFXは `clearAndSetSemantics {}` の短い overlay。入力を持たず、0.68〜1.0秒で終了し、無限アニメーションはありません。
- レイドのSEは `FocusViewModel` のイベント起点。Composeの再composeでは再生せず、session id と delivery journal で重複を抑えます。
- 既存の `fr_focus_*` 等の未追跡音声素材は、既存working treeの変更を保全するため削除・上書きしていません。runtimeが参照するKenney版は上記 `fr_sfx_*` です。
- 既存のFootprint機能があるため8種を統合しました。新しいSNS・自由文機能は追加していません。
