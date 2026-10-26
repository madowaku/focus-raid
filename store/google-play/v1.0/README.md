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
| 6 | [06-walking.png](ja-JP/06-walking.png) | 新規。歩数探索のホームカードと探索手帳（実画面）。歩数は端末内のfixture |
| 横長（日本語） | [feature-graphic-1024x500.png](ja-JP/feature-graphic-1024x500.png) | 撮り直しのみ |
| 横長（英語） | [en-US/feature-graphic-1024x500.png](en-US/feature-graphic-1024x500.png) | 新規。背景はアプリ内の `world_arena_v13.png`、ボスはFIRST RUNの実画面クロップ |
| Playアイコン | [icon-512.png](ja-JP/icon-512.png) | 変更なし |

ランチャーアイコンは本体側を修正した。`ic_launcher_foreground_safe.xml` / `ic_launcher_monochrome_safe.xml` で既存PNGに21%のinsetを入れ、輪・斜線・星が66dpの安全域に収まるようにした。PNG自体は変更していない。模擬表示は [launcher-preview.png](review/launcher-preview.png)。

## 撮影データ

- `raw/` は1080×2400（360×800dp・font scale 1）。撮影は `StoreCaptureTest`（opt-in、7ケース）。
- 状態はローカルfixture：ボスHP 181/250、仲間の残響2つ（「演出デモ」・各25分）、自分の25分、累計0分の卵。
- 歩数探索は3日分のfixture（3,240歩・5,812歩・1,450歩、1,000/3,000/5,000歩の発見）。日付区切りは `Asia/Tokyo`、確認時刻は18:30に固定。`walking` テストはホームを歩数カードまでスクロールし、探索手帳を開いて撮影する（8ケース目）。
- 撮影ホストはシステムバーを隠すため、`capture-store.ps1` がエミュレーターの「全画面表示」の案内を確認済みに設定する。
- `raw/capture-provenance.json` に製品コミット、撮影日時、テストソースと元画像のSHA-256を記録。

## 説明文

- [listing-copy.json](listing-copy.json)（ja-JP）と [listing-copy.en-US.json](listing-copy.en-US.json)（en-US）。短い説明3案、全文説明、各画像の代替テキストを収録。`verify-store.py` がPlayの上限（タイトル30・短い説明80・全文4000文字）を検証する。
- Proとして書いているのは実装済みの3つだけ：星渡り航路、詳細統計、全期間の履歴（`ProPaywallDialog` と `FeatureAccess` に一致）。テーマやカスタマイズは未実装なので書いていない。
- アプリのUIは日本語のみのため、英語の説明文の末尾にその旨を明記した。
- 共有レイドの記述は `publicationGate` のとおり、本番の2端末検証が済むまで公開に使わない。

## ライセンス

- UIのピクセルはすべて本アプリの画面。アート（相棒・ボス・世界の背景）はプロジェクト用に生成した自作素材（出典は `docs/release-art.md`、`docs/v013-art-manifest.json` に記録）。画面内のVFXや絵文字はKenneyのCC0素材（`docs/licenses/kenney-focus-raid.txt`）。
- 見出しの書体はNoto Sans JP（SIL Open Font License 1.1）。画像への描画は許可されており、フォントファイル自体はリポジトリに含めていない。
- 撮影・描画スクリプトは本リポジトリのコード。Playwright CLI（Apache-2.0）はnpxで実行時に取得し、Pillow（MIT-CMU）はローカル環境のもので、どちらも同梱していない。
- 第三者の画像、v0.9の旧スクリーンショット、一時ファイル（`output/`、`.playwright-cli/`）は含めていない。

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
5. 英語のスクリーンショットは未作成。アプリのUIが日本語のみのため、英語の見出しを付けるかどうかを決める。

画像のアップロードや公開はしていない。
