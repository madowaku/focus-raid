# Focus Raid store asset plan

Working asset specification for the first public Android release and Shipaton 2026 submission.

Last requirements review: 2026-09-05.

Current candidate package: [store/google-play/v1.0](../store/google-play/v1.0/README.md) (recaptured 2026-10-26 from the v1.0 release-prep screens, with English feature graphic and launcher safe-zone inset).

## Brand principle

Use one recognizable Focus Raid mark across launcher, Google Play, Shipaton, and promotional graphics instead of designing each asset independently.

The mark should communicate both halves of the product without tiny text:

- **Focus**: timer / progress ring / calm concentration
- **Raid**: expedition / impact / forward motion

The current app already has a dark fantasy Material 3 Expressive visual language with violet, warm accent, and teal highlights. The store identity should look like the same world rather than generic productivity branding.

## Recommended icon direction: RAID RING

Primary direction for exploration:

- chunky rounded circular focus ring
- one energetic diagonal break / strike through the ring
- a small star or impact point where focus becomes raid progress
- dark-violet field with bright violet/warm highlight relationships matching the app
- no letters, timer digits, price badges, crowns, rankings, or fine text
- readable when reduced to notification/launcher scale
- foreground composition safe for Android adaptive-icon masking

Avoid making the icon look primarily like a sword-fighting game. The timer ring must remain the first read.

Secondary directions worth comparing only if the primary direction fails:

1. **Rag Emblem**: companion silhouette nested in a focus ring. More character-driven, less immediately productivity-readable.
2. **Star Beacon**: Star Route beacon inside a focus ring. Elegant and distinctive, but too Pro-specific for the whole app unless generalized.

## Android launcher asset

The current Android app already declares `android:icon` and `android:roundIcon` as
`@mipmap/ic_launcher` in `app/src/main/AndroidManifest.xml`. The adaptive icon is
defined in `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml` with background,
foreground, and monochrome layers. The app's `minSdk` is 26, so a separate
pre-Android-8 legacy icon is not a release requirement.

PR #28 replaces the foreground and monochrome references with safe-zone inset
variants. Those candidate resources are not yet part of `main`. Before release:

- [x] provide adaptive icon resources and manifest icon declarations;
- [ ] verify the candidate safe-zone inset against the exact release build;
- [ ] verify common adaptive masks (circle, squircle, rounded square) and themed icons;
- [ ] verify the icon at small launcher size on a real Android device.

Keep all essential geometry inside the adaptive-icon safe zone. Automated asset
checks do not replace the real-launcher acceptance gate.

## Google Play icon

Current Google Play requirement:

- 512 × 512 px
- 32-bit PNG with alpha
- maximum 1024 KB

Google Play's store icon is separate from the launcher resource and should be a high-fidelity rendering of the same mark.

Output target:

```text
store/google-play/v1.0/ja-JP/icon-512.png
```

## Shipaton icon

Prepare the same mark at the Shipaton submission size already tracked by the release checklist:

```text
store/shipaton/icon-1024.png
```

Do not create a visually different hackathon logo. Recognition is more valuable than one-off decoration.

## Google Play feature graphic

Current Google Play requirement:

- 1024 × 500 px
- JPEG or 24-bit PNG
- no alpha

Recommended composition:

- Focus Raid world gradient / environment as background
- timer-to-expedition progression as the visual story
- Rag or an expedition landmark as secondary character, not a giant logo duplicate
- keep primary content near the center because Play crops the asset in different contexts
- little or no text; if text is used, keep it subordinate and localized per store listing

Output target:

```text
store/google-play/v1.0/ja-JP/feature-graphic-1024x500.png
```

## Phone screenshots

Google Play requires at least two screenshots. For stronger merchandising eligibility, prepare at least four portrait screenshots at 1080 px or wider and 9:16 where practical.

Focus Raid already has automated visual-QA states. Reuse release-candidate UI rather than staging fake mockups.

Recommended first four store-story screens:

1. **READY / Home**: timer + Rag + expedition selection. Communicates the product in one glance.
2. **RAID / Focus session**: quiet active timer. Proves it remains a real focus tool while playing.
3. **VICTORY**: concentration becoming damage/reward. Shows the transformation from time to adventure.
4. **Footprints**: preset asynchronous messages at a reached location. Shows the distinctive quiet-social idea.

Optional fifth/sixth:

5. **Star Route**: visibly different Pro expedition.
6. **Adventure Log**: history/statistics and long-term value.

Keep screenshots faithful to the shipped build. If marketing captions are added around screenshots for Play, keep separate raw UI-only captures for Shipaton requirements and review use.

## Source-of-truth rule

Do not maintain four independent logos.

Keep one approved master mark, then derive:

```text
master mark
  ├─ Android adaptive foreground
  ├─ Android launcher fallback
  ├─ Play icon 512
  ├─ Shipaton icon 1024
  └─ Feature Graphic visual language
```

## Final visual QA

Before upload:

- [ ] launcher icon looks correct on at least circle and rounded-square masks;
- [ ] no important icon element is clipped by adaptive masking;
- [ ] 512 Play icon remains legible at small preview size;
- [ ] feature graphic focal content stays inside safe central area;
- [ ] first four screenshots tell the Focus → Raid → Victory → Footprints loop in order;
- [ ] screenshots match the exact release candidate UI;
- [ ] no screenshot accidentally exposes test prices, fake users, placeholder backend values, or debug badges.

## Official Google Play reference

- Preview assets: https://support.google.com/googleplay/android-developer/answer/9866151
