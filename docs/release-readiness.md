# Focus Raid release readiness

This is the manual release gate for the first public Android build. Automated CI is necessary but not sufficient because production stores, Firebase rules, purchase accounts, signing, and Play policy cannot be fully validated by the credential-free CI environment.

Last policy review: 2026-09-05.

## 1. Automated gate

Before promoting a commit to a release candidate, require the latest branch head to pass all Android CI jobs:

- [ ] `verify`
  - [ ] `testDebugUnitTest`
  - [ ] `lintDebug`
  - [ ] debug APK build
  - [ ] release AAB compile gate (`bundleRelease`)
- [ ] `visual-qa`
  - [ ] 360×800 captures
  - [ ] 720×1280 captures
  - [ ] Free / Pro Raid overview
  - [ ] Pro paywall
  - [ ] Footprint loading / present / posting / error / posted states
- [ ] `timer-durability`
  - [ ] screen off
  - [ ] deep Doze
  - [ ] process kill / restore
  - [ ] reboot / restore

The CI release AAB is a release-variant compile artifact. Unless upload-key credentials are deliberately provided to that environment, it is **not** the signed bundle to upload to Google Play.

Do not mark this section permanently complete in the repository. Re-check it against the exact release candidate commit.

## 2. RevenueCat + Google Play purchase gate

Production identifiers expected by the app:

```text
entitlement: pro
product: focus_raid_pro_lifetime
```

Provide the RevenueCat Google public SDK key through:

```text
FOCUS_RAID_REVENUECAT_GOOGLE_API_KEY
```

Before public release:

- [ ] Create/verify the one-time product in Google Play Console.
- [ ] Attach the Play product to the RevenueCat project and `pro` entitlement.
- [ ] Verify the current RevenueCat Offering exposes the lifetime product.
- [ ] Confirm the paywall shows the localized Google Play price, not placeholder copy.
- [ ] License-test a purchase cancellation.
- [ ] License-test a successful purchase and immediate Pro unlock.
- [ ] Relaunch the app and confirm Pro remains active.
- [ ] Reinstall on a test account and confirm Restore Purchases recovers Pro.
- [ ] Test restore when there is no prior purchase and confirm the app remains neutral/usable.
- [ ] Test temporary RevenueCat/network failure and confirm cached Pro is not demoted.
- [ ] Verify the purchase appears in the RevenueCat customer dashboard.

The Free timer must remain usable when the RevenueCat key or product lookup is unavailable.

## 3. Firebase shared-world gate

Provide production Firebase values through:

```text
FOCUS_RAID_FIREBASE_PROJECT_ID
FOCUS_RAID_FIREBASE_API_KEY
FOCUS_RAID_FIREBASE_APP_ID
```

Before public release:

- [ ] Enable Firebase Authentication Anonymous sign-in.
- [ ] Enable Cloud Firestore.
- [ ] Create and validate `world/current`.
- [ ] Deploy the repository's current `firestore.rules`.
- [ ] Confirm a valid anonymous client can read `world/current`.
- [ ] Confirm a signed-out client cannot read shared data.
- [ ] Confirm a valid preset Footprint can be created at `footprints/{EXPEDITION}/checkpoints/{CHECKPOINT}/entries/{UID}`.
- [ ] Confirm an unknown/free-form preset ID is rejected.
- [ ] Confirm a user cannot write another UID's Footprint document.
- [ ] Confirm a Footprint cannot add arbitrary fields.
- [ ] Confirm updating a Footprint cannot change `createdAt`.
- [ ] Confirm authoritative `world/**` client writes are rejected.
- [ ] Test offline/failed Firestore and confirm VICTORY, local history, and timer completion are unaffected.
- [ ] Confirm failed real-backend Footprint reads show no fake seeded users.

The Android CI currently does not deploy or emulate Firestore Security Rules. Treat Rules validation as a separate production gate.

## 4. Privacy + Google Play Data Safety gate

Google Play currently requires every app to provide a comprehensive privacy policy in Play Console and make the policy link or text accessible from inside the app. The policy must describe collection, use, sharing, retention/deletion, secure handling, and a privacy contact/mechanism.

Focus Raid currently uses services that require disclosure review:

- Firebase Authentication
  - anonymous Firebase user identity
  - Firebase documents automatic collection such as IP address and user-agent information for authentication/security
- Cloud Firestore
  - anonymous Firebase UID is included with authenticated Firestore requests
  - preset Footprints store a preset ID and server timestamp
- RevenueCat
  - purchase history is processed for purchase validation/entitlement functionality
  - RevenueCat may create an anonymous App User ID when no custom App User ID is provided

Before public release:

- [ ] Publish a clearly titled **Privacy Policy** at a public, active, non-geofenced URL.
- [ ] Put the same Privacy Policy link or text inside Focus Raid.
- [ ] Include the developer/app identity used in the Play listing.
- [ ] Include a privacy contact or request mechanism.
- [ ] State retention and deletion handling for Firebase anonymous identity, Footprints, and RevenueCat purchase/customer records.
- [ ] Provide a practical data-deletion request path even if the product exposes no visible account profile.
- [ ] Complete Play Console Data Safety based on the exact production SDK configuration.
- [ ] At minimum, review RevenueCat's required declaration for **Financial info → Purchase history**.
- [ ] Review Firebase's current Play data-disclosure guidance for Authentication and Firestore.
- [ ] Re-check whether any analytics/integrations have been enabled in RevenueCat or Firebase before submitting Data Safety.

Current references:

- Google Play User Data policy: https://support.google.com/googleplay/android-developer/answer/10144311
- Firebase Android Play data disclosure: https://firebase.google.com/docs/android/play-data-disclosure
- RevenueCat Google Play Data Safety guidance: https://www.revenuecat.com/docs/platform-resources/google-platform-resources/google-plays-data-safety

Policy forms can change. Re-open the current official pages when completing the Play Console questionnaire rather than copying this checklist mechanically.

## 5. Release build + signing gate

The current development build is not the public artifact. See `docs/release-signing.md` for the upload-key workflow.

Before public release:

- [ ] Choose the public version name/version code for the first release.
- [ ] Generate and safely back up a dedicated upload key.
- [ ] Configure release signing without committing keystore files or passwords.
- [ ] Produce a signed Android App Bundle (`.aab`).
- [ ] Verify the bundle signature with `jarsigner -verify -verbose -certs`.
- [ ] Inspect the release bundle for the correct application ID: `com.madowaku.focusraid`.
- [ ] Configure/confirm Play App Signing for the new app.
- [ ] Install/test the Play-generated build through an internal or closed testing track.
- [ ] Cold-install on a real Android device.
- [ ] Verify notification permission education.
- [ ] Verify exact-alarm fallback and optional permission path.
- [ ] Verify focus completion with the app backgrounded.
- [ ] Verify Free mode without purchase.
- [ ] Verify Pro mode through a real Play license-test purchase.
- [ ] Verify Firebase-backed Footprints with two separate test installations/accounts.

## 6. Product honesty gate

Before publishing store copy, screenshots, or the Shipaton demo:

- [ ] Do not advertise unimplemented Pro raids/themes/customization as current features.
- [ ] Do not imply fake preview Footprints are real users.
- [ ] Clearly describe Pro as a one-time purchase, not a subscription.
- [ ] Keep the core focus timer fully usable in Free.
- [ ] Confirm all screenshots correspond to the release candidate UI.

## 7. Shipaton submission gate

For the 2026 submission:

- [ ] Publicly release the new Android app within the eligible Shipaton period.
- [ ] Keep the app accessible to judges in the United States.
- [ ] Verify RevenueCat powers the submitted in-app purchase.
- [ ] Prepare an English app description / full English translation.
- [ ] Prepare a public YouTube/Vimeo demo under 2 minutes.
- [ ] Show the functioning app on the target device.
- [ ] Prepare the required app icon and screenshots.
- [ ] Provide a practical judge path to unlock/test the paid experience.
- [ ] Submit early enough to absorb Play review risk.

## Release-candidate rule

A commit becomes a release candidate only when:

1. all automated CI jobs are green for that exact commit;
2. production Firebase + RevenueCat configurations have been exercised on Play testing infrastructure;
3. privacy/Data Safety materials are complete and linked;
4. the signed Play-distributed build passes a real-device smoke test.

Until all four are true, call the build **release-ready development**, not **public-release ready**.
