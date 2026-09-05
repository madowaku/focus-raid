# Focus Raid release signing

Focus Raid uses Google Play App Signing. Google Play should manage the app-signing key used for final distribution, while the developer keeps a separate upload key used to sign Android App Bundles before upload.

Do not commit the keystore or passwords to this repository. `*.jks` and `*.keystore` are ignored by Git.

## 1. Generate the upload key

On Windows PowerShell with JDK 17 available:

```powershell
keytool -genkeypair -v `
  -keystore focus-raid-upload.jks `
  -alias focusraid-upload `
  -keyalg RSA `
  -keysize 4096 `
  -validity 10000
```

Store the generated keystore somewhere outside the repository and make at least one secure backup.

The upload key and the Google Play app-signing key should be separate keys.

## 2. Provide signing values locally

`app/build.gradle.kts` reads these values from Gradle properties or environment variables:

```text
FOCUS_RAID_UPLOAD_KEYSTORE_PATH
FOCUS_RAID_UPLOAD_STORE_PASSWORD
FOCUS_RAID_UPLOAD_KEY_ALIAS
FOCUS_RAID_UPLOAD_KEY_PASSWORD
```

A temporary PowerShell session can use:

```powershell
$env:FOCUS_RAID_UPLOAD_KEYSTORE_PATH="C:\Secure\focus-raid-upload.jks"
$env:FOCUS_RAID_UPLOAD_STORE_PASSWORD="..."
$env:FOCUS_RAID_UPLOAD_KEY_ALIAS="focusraid-upload"
$env:FOCUS_RAID_UPLOAD_KEY_PASSWORD="..."
```

Do not put real secret values in scripts committed to Git.

If all four values are present, the `release` build type uses the `releaseUpload` signing config. If any value is missing, Gradle still permits a release-variant compile so CI can catch release-only build failures, but that output is not the Play upload artifact.

## 3. Build the signed AAB

With the signing environment configured:

```powershell
.\gradlew.bat bundleRelease
```

Expected output:

```text
app\build\outputs\bundle\release\app-release.aab
```

## 4. Verify the signature

Before uploading:

```powershell
jarsigner -verify -verbose -certs app\build\outputs\bundle\release\app-release.aab
```

Treat a verification failure as a release blocker.

## 5. Google Play App Signing

For the first Play release:

1. Create/confirm the Focus Raid app for package `com.madowaku.focusraid`.
2. Prepare the first release in an internal testing track.
3. Keep Play App Signing enabled with a Google-managed app-signing key.
4. Upload the locally signed AAB using the dedicated upload key.
5. After Play accepts the bundle, install the Play-generated build from the testing track and run the release smoke tests.

The upload key can be reset through Play Console if it is lost or compromised when Play App Signing is used. The app-signing key managed by Google is the identity used for final APKs delivered to users.

## 6. Versioning

The current development version is intentionally not treated as the final public version. Before the first public release, choose the final `versionName` and confirm `versionCode` is valid for Play.

Every later update must use a strictly higher `versionCode` and the same Play signing identity.

## CI behavior

CI runs `bundleRelease` without production upload-key credentials and uploads `focus-raid-release-aab-ci` as a compile artifact. This checks that the release variant actually builds, but it must not be confused with the signed production bundle.
