# Focus Raid Free / Pro & RevenueCat v1.0

## Product policy

Focus Raid v1.0 uses a single one-time Pro purchase. There are no subscriptions, consumable purchases, ads, or a custom billing server.

> 集中するために必要なものはFree。集中を続けたくなるものをPro。

Free must remain a complete focus timer and progression experience. Pro expands depth, history, customization, and expedition variety.

## Free / Pro boundary

### Free

- Core focus / break timer features
- Pause, resume, skip, notifications, and background timing
- Level / XP / streak with no progression cap
- Launch Free raids
- Basic statistics
- All session data is stored, while the UI may limit history visibility to the latest 7 days
- Basic themes
- Read and leave basic preset footprints
- No ads

### Pro lifetime

- All Pro raids
- Detailed statistics
- Full-history visibility
- All themes
- Advanced customization
- Footprint customization
- Special effects
- Future content explicitly classified as Pro raids or Pro themes

Free-era history must never be deleted merely because it is not visible in the Free UI. Buying Pro should reveal the already-saved older history.

## Billing contract

| Layer | ID / rule |
| --- | --- |
| Google Play product | `focus_raid_pro_lifetime` |
| Product type | One-time product / non-consumable |
| RevenueCat entitlement | `pro` |
| RevenueCat Offering | Default / Current Offering |
| RevenueCat package | Lifetime package |
| App entitlement source of truth | RevenueCat `CustomerInfo` entitlement `pro` |

Do not use the Google Play product ID itself to decide whether app features are unlocked. The app should only care whether RevenueCat entitlement `pro` is active.

Do not persist a local `is_pro=true` flag as the billing source of truth.

## Google Play Console setup

1. Create the Android app using application ID `com.madowaku.focusraid`.
2. Create a one-time in-app product with ID `focus_raid_pro_lifetime`.
3. Configure it as a permanent one-time purchase. It must not be treated as a consumable product.
4. Set price and availability in Play Console.
5. Configure license-test accounts before purchase testing.

The displayed price in Focus Raid is fetched from the store through RevenueCat and must not be hard-coded in the app.

## RevenueCat setup

1. Create / select the Focus Raid project.
2. Add the Google Play Android app for `com.madowaku.focusraid`.
3. Connect the Google Play product `focus_raid_pro_lifetime`.
4. Create entitlement `pro`.
5. Attach `focus_raid_pro_lifetime` to entitlement `pro`.
6. Create the Default Offering and make it the Current Offering.
7. Put the product in the Lifetime package.
8. For this no-login app, configure restore behavior so a valid store purchase can transfer to the newly generated anonymous App User ID when restoring after reinstall.

## Android configuration

RevenueCat's Google public SDK key is injected through either a Gradle property or environment variable:

```text
FOCUS_RAID_REVENUECAT_GOOGLE_API_KEY=goog_xxxxxxxxxxxxxxxxx
```

The build exports it as `BuildConfig.REVENUECAT_GOOGLE_API_KEY`.

If the key is absent, Focus Raid itself still launches and Free features remain available. The Pro purchase surface reports that purchasing is not configured instead of crashing the app.

## Architecture

```text
Compose UI
   |
ProAccessViewModel
   |
ProAccessRepository
   |
BillingGateway
   |
RevenueCatBillingGateway
   |
RevenueCat SDK
   |
Google Play Billing
```

Responsibilities:

- `FeatureAccess`: central Free / Pro feature policy.
- `ProAccessRepository`: owns app-level access state and protects known Pro access from transient refresh / restore failures.
- `BillingGateway`: isolates the app from RevenueCat-specific APIs.
- `RevenueCatBillingGateway`: fetches Offering / price, purchases, restores, and maps CustomerInfo to the `pro` entitlement.
- `ProAccessViewModel`: lifecycle-aware UI bridge.
- `ProPaywallDialog`: Focus Raid Material 3 purchase UI. It never hard-codes the price.

## Runtime rules

- App startup initializes RevenueCat once in `FocusRaidApplication` when an API key exists.
- Startup refresh reads `CustomerInfo` and Current Offering.
- A network failure must not demote a Pro user already known by the repository.
- Purchase success is not enough by itself. Pro unlock occurs only when returned `CustomerInfo` shows active entitlement `pro`.
- Purchase cancellation leaves access unchanged and is not presented as an error.
- Restore failure leaves current access unchanged.
- Restore success without an active `pro` entitlement is shown as no restorable Pro purchase found.
- Free timer functionality must continue working if RevenueCat is unavailable.

## v1.0 non-goals

Do not add these without revisiting the monetization specification:

- Monthly or yearly subscriptions
- Free trial
- Consumable coins
- Per-raid purchases
- Per-theme purchases
- Pro tiers / Pro+
- Ad-removal purchase
- Custom billing account
- Custom billing backend
- Web checkout

## Release test checklist

- [ ] Clean install starts as Free
- [ ] Free core timer works without RevenueCat configuration
- [ ] Free raids remain usable
- [ ] Pro feature access is centralized through `FeatureAccess`
- [ ] Paywall receives localized store price from RevenueCat
- [ ] Purchase cancellation keeps Free access
- [ ] Successful purchase activates entitlement `pro` and immediately unlocks Pro
- [ ] App restart preserves Pro through RevenueCat CustomerInfo
- [ ] Temporary refresh failure does not demote an already-known Pro session
- [ ] Restore after reinstall reactivates Pro for the same Google Play purchase
- [ ] Restore with no purchase displays a neutral no-purchase result
- [ ] Historic Free data becomes visible when Pro is unlocked
- [ ] Unit tests, lint, and debug APK build pass
