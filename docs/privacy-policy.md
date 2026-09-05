# Focus Raid Privacy Policy / プライバシーポリシー

Effective date / 適用開始日: 2026-09-05

Developer / 開発者: **madowaku**  
Contact / お問い合わせ: **raindrum909@gmail.com**

This policy explains how the Android app **Focus Raid** handles information. The Japanese section appears first, followed by an English version.

---

## 日本語

### 1. このポリシーについて

Focus Raid（以下「本アプリ」）は、madowaku が提供する集中タイマー／習慣化アプリです。本ポリシーは、本アプリが端末内で保存する情報、Firebase を利用した共有機能、Google Play／RevenueCat を利用した購入機能、およびそれらに関するデータの取り扱いを説明します。

### 2. 端末内に保存するデータ

本アプリは、次のような集中・進行データを端末内に保存します。

- 集中セッション履歴
- 予定時間、実際に集中した時間、完走／途中終了の結果
- レベル、経験値、連続日数などの進行情報
- 選択した集中時間や遠征
- タイマーの復元に必要な状態
- ローカル統計の計算に必要な情報

これらの集中履歴を、Focus Raid のアプリ処理が Firebase や RevenueCat にアップロードすることはありません。

端末内データは、ユーザーがアプリのストレージを消去する、またはアプリをアンインストールすることで削除できます。ただし、Android のバックアップ／復元設定によっては、Google のバックアップ機能等により一部データが復元される場合があります。

### 3. Firebase を利用する共有世界・足跡

本アプリは、共有世界と定型メッセージ式の「足跡」機能のため、Google Firebase の次のサービスを利用します。

- Firebase Authentication（匿名認証）
- Cloud Firestore

本アプリでは、メールアドレス、パスワード、表示名、電話番号を入力してアカウントを作成する必要はありません。Firebase は共有機能へのアクセス制御のため、匿名ユーザー ID を生成します。

Firebase Authentication SDK は、認証・不正利用防止・サービス提供のため、Firebase の仕様に基づき IP アドレス、ユーザーエージェント情報、Firebase Android App ID、Firebase ユーザーエージェント等の技術情報を処理する場合があります。

Cloud Firestore への認証済みリクエストには、該当する Firebase 匿名ユーザー ID が含まれます。

ユーザーが足跡を残した場合、本アプリが Firestore に保存する内容は次のとおりです。

- 組み込み定型メッセージを示す `presetId`
- サーバー時刻 `createdAt`
- ドキュメント識別に使用される匿名 Firebase UID
- 遠征およびチェックポイントを示すドキュメントパス

本アプリは、足跡に自由入力テキスト、表示名、プロフィール、GPS 位置情報を保存しません。足跡の文章とアイコンは、保存された `presetId` からアプリ内で復元されます。

共有世界の `world/current` はクライアントから読み取るための状態であり、端末内の集中履歴をアップロードするものではありません。

### 4. Focus Raid Pro の購入

本アプリは、買い切りの Focus Raid Pro を提供するために次のサービスを利用します。

- Google Play Billing
- RevenueCat

Google Play が決済そのものを処理します。madowaku および Focus Raid は、クレジットカード番号等の支払手段情報を直接受け取りません。

RevenueCat は、購入の検証、Pro 権限の付与・復元、および購入状況の管理のため、購入履歴、商品／取引情報、権限状態、匿名 App User ID、ならびにサービス提供に必要な技術情報を処理します。

Focus Raid は、RevenueCat に氏名、メールアドレス、電話番号などの独自の顧客属性を送信するようには構成していません。また、広告識別子を利用する RevenueCat 連携を前提としていません。

### 5. データを利用する目的

本アプリおよび利用するサービスは、主に次の目的でデータを利用します。

- 集中タイマー、履歴、進行、復元機能の提供
- 匿名認証と共有世界への安全なアクセス
- 定型足跡の読み書きと不正利用防止
- Focus Raid Pro 購入の検証、権限付与、復元
- 障害調査、サポート、データ削除依頼への対応
- Firebase／RevenueCat 各サービスの提供、維持、改善に必要な処理

### 6. 第三者サービスへの提供・共有

本アプリは、機能提供に必要な範囲で次のサービスを利用します。

- Google Firebase
- Google Play
- RevenueCat

本アプリは、ユーザーデータを広告目的で販売しません。広告 SDK、Firebase Analytics、Crashlytics は現行のリリース構成には含めていません。

第三者サービスにおけるデータ処理は、それぞれのサービス提供者の規約・プライバシーポリシーにも従います。

### 7. 本アプリが利用しない情報・機能

現行の Focus Raid は、次の情報をアプリ機能のために取得しません。

- 正確な位置情報／概算位置情報
- 連絡先
- カメラ
- マイク
- 写真・動画・音声ファイル
- SMS／通話履歴
- 健康・フィットネス情報
- 自由文チャット
- 広告目的のトラッキング

通知、正確なタイマー、端末再起動後のタイマー復元等のため、Android の通知・アラーム・再起動通知等の権限／仕組みを利用します。

### 8. 保存期間と削除

**端末内データ**  
ユーザーがアプリのストレージを消去する、またはアンインストールするまで保存されます。Android のバックアップ設定により復元される場合があります。

**Firebase の匿名ユーザー／足跡データ**  
共有機能の提供、不正利用防止、サポートに必要な期間保存します。削除を希望する場合は、下記メールアドレスへご連絡ください。

**RevenueCat の顧客・購入関連データ**  
購入検証、購入復元、権限管理、サポート、法令上必要な記録保持等のため保存されます。削除可能な RevenueCat 顧客データについて削除を希望する場合は、下記メールアドレスへご連絡ください。Google Play 側の購入履歴や、法令・会計・不正防止上保持が必要な記録については、当方だけでは削除できない場合があります。

データ削除を依頼する場合、可能であれば購入注文情報や、本アプリ内で表示される識別情報など、対象データを確認するための情報を添えてください。必要最小限の範囲で追加情報をお願いする場合があります。

### 9. セキュリティ

Firebase および RevenueCat との通信は、各サービスが提供する HTTPS 等の暗号化通信を利用します。本アプリは、必要な範囲を超えて個人情報を収集しない設計を基本としています。

### 10. お問い合わせ・削除依頼

プライバシーに関する問い合わせ、サーバー側データまたは RevenueCat 顧客データの削除依頼は、次のメールアドレスへご連絡ください。

**raindrum909@gmail.com**

### 11. 本ポリシーの変更

本アプリの機能、利用 SDK、法令・プラットフォーム要件等の変更に応じて、本ポリシーを更新する場合があります。重要な変更がある場合は、本ページの更新日を変更し、必要に応じてアプリ内またはストア掲載情報で案内します。

---

## English

### 1. About this policy

Focus Raid is a focus timer and habit-building Android app provided by **madowaku**. This policy explains information stored locally by the app, data handled through Firebase for shared features, and purchase-related data processed through Google Play and RevenueCat.

### 2. Data stored locally on your device

Focus Raid stores product data such as:

- focus session history;
- planned and credited focus time and completion/early-exit results;
- level, XP, streak, and other progression data;
- selected focus duration and expedition;
- state required to recover an active timer; and
- information used to calculate local statistics.

Focus Raid's application logic does not upload this focus-session history to Firebase or RevenueCat.

Local data can be removed by clearing the app's storage or uninstalling the app. Depending on your Android backup and restore settings, some data may later be restored by Android/Google backup services.

### 3. Firebase shared world and preset Footprints

Focus Raid uses these Google Firebase services for the shared world and preset-message Footprints:

- Firebase Authentication with anonymous sign-in; and
- Cloud Firestore.

You do not need to create a Focus Raid account with an email address, password, display name, or phone number. Firebase creates an anonymous user identifier so the app can authorize access to shared features.

According to Firebase's Android SDK documentation, Firebase Authentication may process technical information such as IP address, user-agent information, Firebase Android App ID, and Firebase user-agent information for authentication, security, abuse prevention, and service operation.

Authenticated Cloud Firestore requests include the applicable Firebase anonymous user ID.

When you choose to leave a Footprint, Focus Raid stores:

- a built-in preset message identifier (`presetId`);
- a server timestamp (`createdAt`);
- the anonymous Firebase UID used as the document identifier; and
- the expedition/checkpoint represented by the Firestore document path.

Focus Raid does not store free-form Footprint text, public profile names, profiles, or GPS coordinates. The visible Footprint text and icon are reconstructed locally from the preset ID.

The shared `world/current` state is read from the server and does not upload your local focus-session history.

### 4. Focus Raid Pro purchases

Focus Raid uses:

- Google Play Billing; and
- RevenueCat

to provide the one-time Focus Raid Pro purchase.

Google Play processes the payment. Focus Raid and madowaku do not directly receive your credit-card number or other payment-card details.

RevenueCat processes purchase history, product/transaction information, entitlement status, an anonymous App User ID, and technical information needed to validate purchases, unlock or restore Pro access, and operate its service.

Focus Raid is not configured to send custom RevenueCat customer attributes such as your name, email address, or phone number, and does not rely on RevenueCat integrations that use advertising identifiers for advertising purposes.

### 5. Why data is used

Data is used to:

- provide the focus timer, history, progression, and recovery features;
- provide anonymous authentication and secure shared-world access;
- read and write preset Footprints and prevent abuse;
- validate, unlock, and restore Focus Raid Pro;
- investigate problems and respond to support or deletion requests; and
- allow Firebase and RevenueCat to provide, maintain, secure, and improve their services as described by those providers.

### 6. Service providers and sharing

Focus Raid uses these service providers where necessary for app functionality:

- Google Firebase;
- Google Play; and
- RevenueCat.

Focus Raid does not sell user data for advertising. The current release configuration does not include advertising SDKs, Firebase Analytics, or Crashlytics.

Data handled by these third-party services is also subject to the applicable provider terms and privacy policies.

### 7. Data and features Focus Raid does not use

The current Focus Raid release does not use the following for app functionality:

- precise or approximate location;
- contacts;
- camera;
- microphone;
- photos, videos, or audio files;
- SMS or call logs;
- health or fitness data;
- free-form chat; or
- advertising tracking.

Focus Raid uses Android notification, alarm, reboot, and vibration mechanisms as needed to provide timer notifications and reliable timer recovery.

### 8. Retention and deletion

**Local data**  
Local app data is retained until you clear app storage or uninstall the app, subject to Android backup/restore behavior.

**Firebase anonymous identity and Footprints**  
Server-side data is retained as needed to provide shared features, prevent abuse, and provide support. You may request deletion by contacting the address below.

**RevenueCat customer and purchase-related data**  
Purchase-related records may be retained as needed for purchase validation, restore, entitlement management, support, fraud prevention, and applicable legal/accounting requirements. You may contact us to request deletion of RevenueCat customer data that can be deleted. Google Play purchase records or records that must be retained for legal, accounting, or fraud-prevention reasons may not be deletable by us alone.

When requesting deletion, please include any available information that helps us locate the relevant anonymous record, such as purchase order information or identifiers displayed by the app. We may request limited additional information if necessary to match the request to the correct data.

### 9. Security

Communications with Firebase and RevenueCat use the encrypted transport provided by those services, including HTTPS. Focus Raid is designed to avoid collecting information that is not necessary for its features.

### 10. Contact and deletion requests

For privacy questions or requests to delete server-side or RevenueCat customer data, contact:

**raindrum909@gmail.com**

### 11. Changes to this policy

This policy may be updated when Focus Raid's features, SDKs, legal requirements, or platform requirements change. Material updates will be reflected by updating the date on this page and, where appropriate, through the app or store listing.
