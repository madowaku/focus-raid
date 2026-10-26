# v1.0 Google Play素材候補

v0.9候補（[Issue #13](https://github.com/madowaku/focus-raid/issues/13)）の構成と撮影パイプラインを、現行main（Pixel Expedition・Pixel Return Raid・歩数探索）に合わせて撮り直したもの。訴求は「集中時間が、みんなのレイドへ届く。相棒も育つ。」。

UIのピクセルは本番コンポーザブルの撮影のみ。画像生成でUIやキャラクターを描き直していない。端末フレーム、価格、レビュー、架空の参加人数は使っていない。

## 成果物

| 素材 | ファイル | v0.9からの変更 |
|---|---|---|
| 1 A / 1 B | [01-hero-a.png](ja-JP/01-hero-a.png) / [01-hero-b.png](ja-JP/01-hero-b.png) | FIRST RUNは同じ画面。撮り直しのみ |
| 2 | [02-shared-raid.png](ja-JP/02-shared-raid.png) | 同上 |
| 3 | [03-companion.png](ja-JP/03-companion.png) | 同上 |
| 4 | [04-return-raid.png](ja-JP/04-return-raid.png) | 深層＋ラグの本番経路である **Pixel Return Raid**（火口の広場）に変更。HP帯の別クロップは削除し、一撃のフレームを大きく表示 |
| 5 | [05-everyday.png](ja-JP/05-everyday.png) | 集中中を **Pixel Expedition**（灰の稜線・12:30）に変更。ホームCTAのクロップ位置を現行レイアウトに合わせた |
| 横長（日本語） | [feature-graphic-1024x500.png](ja-JP/feature-graphic-1024x500.png) | 撮り直しのみ |
| 横長（英語） | [en-US/feature-graphic-1024x500.png](en-US/feature-graphic-1024x500.png) | 新規。背景はアプリ内の `world_arena_v13.png`、ボスはFIRST RUNの実画面クロップ |
| Playアイコン | [icon-512.png](ja-JP/icon-512.png) | 変更なし |

ランチャーアイコンは本体側を修正した。`ic_launcher_foreground_safe.xml` / `ic_launcher_monochrome_safe.xml` で既存PNGに21%のinsetを入れ、輪・斜線・星が66dpの安全域に収まるようにした。PNG自体は変更していない。模擬表示は [launcher-preview.png](review/launcher-preview.png)。

## 撮影データ

- `raw/` は1080×2400（360×800dp・font scale 1）。撮影は `StoreCaptureTest`（opt-in、7ケース）。
- 状態はローカルfixture：ボスHP 181/250、仲間の残響2つ（「演出デモ」・各25分）、自分の25分、累計0分の卵。
- ホームには歩数探索のfixture（今日3,240歩・1,000/3,000歩の発見）を与えているが、カードは2400pxの画面外にあり、現在の素材には写っていない。
- `raw/capture-provenance.json` に製品コミット、撮影日時、テストソースと元画像のSHA-256を記録。

## 再生成

必要なものは v0.9 と同じ（JDK 17、Android SDK、使い捨てエミュレーター、Node.js/npm、Python + Pillow、ローカルの **Noto Sans JP**）。リポジトリのルートで:

```powershell
./scripts/capture-store.ps1 -Serial emulator-5554
./scripts/render-store.ps1
python scripts/verify-store.py
```

## 公開前に残る確認

1. 3〜5人の5秒レビューでA/Bを決める（[テンプレート](review/five-second-review.csv)）。
2. inset後のランチャーアイコンを実機のランチャー（円・角丸・themed icon、明暗の壁紙）で確認する。
3. Play Consoleでの縮小・トリミングの確認と、同系色アプリとの識別性の確認。
4. 本番の共有レイドを2端末で検証してから、世界の共有レイドをうたうコピーを公開する。
5. 英語の短い説明・長い説明・スクリーンショットは未作成。英語版は今回feature graphicのみ。

画像のアップロードや公開はしていない。
