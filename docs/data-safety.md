# Focus Raid Google Play Data Safety worksheet

> Working release worksheet, not legal advice and not a substitute for the current Play Console questionnaire.
>
> Last reviewed against the production-intended Android dependency set: 2026-09-05.

Use this document to keep the Play Console Data Safety answers aligned with the actual Focus Raid build and Privacy Policy. Re-check the current Google, Firebase, and RevenueCat guidance immediately before submission because SDK behavior and Play form wording can change.

## Current production-intended SDK surface

Focus Raid currently includes:

- Firebase Authentication with anonymous sign-in
- Cloud Firestore
- RevenueCat Purchases SDK
- local Room session history
- local DataStore timer/session preferences

Focus Raid currently does **not** intentionally include:

- advertising SDKs
- ad identifiers for monetization
- Firebase Analytics
- Crashlytics
- free-form chat or free-form Footprint text
- a custom email/password account system
- location, contacts, camera, microphone, health, SMS, or call-log features

Re-check the dependency graph before release instead of assuming this list remains true forever.

## Data that leaves the device

### RevenueCat: purchase history

RevenueCat's Google Play Data Safety guidance requires apps using RevenueCat to disclose **Financial info → Purchase history**.

Working answers from RevenueCat's current guidance:

- Collected: **Yes**
- Shared: normally **No** when RevenueCat is acting as a service provider, unless additional third-party integrations change this
- Processed ephemerally: **No**
- Optional: **No**, collection is required for the purchase/entitlement feature
- Purpose: **App functionality** and **Analytics** according to RevenueCat's current guidance

Focus Raid uses this information to validate the one-time `focus_raid_pro_lifetime` purchase and expose entitlement `pro`.

Before submission, inspect the RevenueCat project for any enabled integrations that could change the sharing declaration.

### Firebase Authentication

Firebase documents automatic collection by the Android Authentication SDK including:

- Firebase user-agent information
- IP address for security / abuse prevention during authentication
- user-agent strings including SDK version and device platform information
- Firebase Android App ID

Focus Raid uses anonymous Firebase Authentication only to authorize shared-world and preset-Footprint requests. The app does not ask the player to provide an email address, password, display name, or phone number for this feature.

Do not describe anonymous authentication as meaning that no identifier exists. Firebase creates an anonymous user identity, and authenticated Firestore requests include the applicable Firebase user ID.

### Cloud Firestore

Firebase documents automatic collection of the Firebase user agent by the Firestore Android SDK. When Firestore is used with Firebase Authentication, authenticated requests automatically include the applicable Firebase user ID.

Focus Raid additionally sends developer-defined Footprint data when the player chooses to leave one:

```text
presetId
createdAt (server timestamp)
```

The Footprint document path contains the anonymous Firebase UID as the document ID:

```text
footprints/{EXPEDITION}/checkpoints/{CHECKPOINT}/entries/{ANONYMOUS_UID}
```

Focus Raid does not send the rendered preset message as arbitrary user-entered text. The app reconstructs glyph/text locally from the built-in preset ID.

The shared-world read of `world/current` is server-to-client state and does not upload local focus-history rows.

## Data that remains local to Focus Raid

The following product data is currently stored locally through Room/DataStore and is not intentionally uploaded by Focus Raid's application logic:

- session history
- planned / credited focus minutes
- completion / aborted outcome
- local progression totals
- selected timer duration
- selected expedition
- timer recovery state
- local streak/statistics inputs

The app manifest currently permits Android backup. Review the final backup policy separately before release if the desired privacy statement is that app data remains only on the current physical device.

## Footprints and social data

Footprints are deliberately constrained to minimize privacy/moderation risk:

- no free-form text
- no public display name
- no profile
- no direct messaging
- no location permission or GPS data
- preset ID only
- server timestamp
- anonymous Firebase UID used for access control/document ownership

Firestore Security Rules must be deployed and tested before release. They are intended to reject unknown preset IDs, arbitrary fields, writes to another UID's Footprint, timestamp rewriting, and authoritative client writes to `world/**`.

## Encryption / transport

Firebase documents encryption in transit using HTTPS for the end-user data described in its Android Play disclosure guidance.

RevenueCat's current Data Safety guidance should be re-checked for its transport/security declarations at submission time.

Do not claim end-to-end encryption or local-only processing for Firebase/RevenueCat traffic.

## Retention and deletion questions to settle before public release

The Privacy Policy must state a real retention/deletion policy. Before marking the release candidate public-ready, decide and document:

- how long server-side Footprints are retained;
- how a user can request deletion of Footprint / anonymous Firebase data;
- how RevenueCat purchase/customer deletion requests are handled where legally/technically appropriate;
- what identifying information the user needs to provide so a deletion request can be matched to anonymous records;
- whether Android backup remains enabled for local app data.

Until these are concrete, do not over-promise automatic deletion in the Privacy Policy.

## Play Console review checklist

Before submitting Data Safety:

- [ ] Open the exact release dependency graph and verify no analytics/ads/crash SDK was added.
- [ ] Re-open Google's current Data Safety form documentation.
- [ ] Re-open Firebase Android Play disclosure guidance for Authentication and Firestore.
- [ ] Re-open RevenueCat's current Google Play Data Safety guidance.
- [ ] Declare RevenueCat purchase history as required by its current guidance.
- [ ] Account for Firebase Authentication automatic collection and authenticated user ID usage.
- [ ] Account for developer-defined Firestore Footprint data.
- [ ] Verify whether any RevenueCat integrations alter the sharing answer.
- [ ] Keep Data Safety answers consistent with the published Privacy Policy.
- [ ] Keep the published Privacy Policy consistent with the exact shipped SDK configuration.

## Official references

- Google Play User Data policy: https://support.google.com/googleplay/android-developer/answer/10144311
- Google Play Data Safety form: https://support.google.com/googleplay/android-developer/answer/10787469
- Firebase Android Play data disclosure: https://firebase.google.com/docs/android/play-data-disclosure
- RevenueCat Google Play Data Safety guidance: https://www.revenuecat.com/docs/platform-resources/google-platform-resources/google-plays-data-safety
