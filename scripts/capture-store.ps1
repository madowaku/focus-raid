param(
    [string]$Serial = 'emulator-5554',
    [string]$Adb = "$env:LOCALAPPDATA/Android/Sdk/platform-tools/adb.exe"
)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
if ($Serial -notmatch '^emulator-\d+$') { throw 'Use a disposable Android emulator for fixture captures.' }
function Invoke-Adb {
    & $Adb -s $Serial @args
    if ($LASTEXITCODE -ne 0) { throw "adb failed: $args" }
}
Push-Location $projectRoot
try {
    $raw = 'store/google-play/v1.0/raw'
    New-Item -ItemType Directory -Force $raw,'output/playwright' | Out-Null
    Invoke-Adb shell wm size 1080x2400
    Invoke-Adb shell wm density 480
    Invoke-Adb shell settings put system font_scale 1.0
    # The capture host hides system bars; skip Android's one-time "Viewing full screen" hint over dialogs.
    Invoke-Adb shell settings put secure immersive_mode_confirmations confirmed
    & ./gradlew.bat assembleDebug assembleDebugAndroidTest --console=plain *> output/playwright/store-capture-build.log
    if ($LASTEXITCODE -ne 0) { throw 'Capture build failed. See output/playwright/store-capture-build.log.' }
    # Target the selected emulator explicitly; connectedAndroidTest can enumerate other devices.
    Invoke-Adb install -r -d app/build/outputs/apk/debug/app-debug.apk
    Invoke-Adb install -r -d app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
    # Clear only the dedicated previous export directory on this emulator.
    $remote = '/sdcard/Android/data/com.madowaku.focusraid/files/store-captures'
    Invoke-Adb shell rm -f "$remote/*.png"
    $result = Invoke-Adb shell am instrument -w -e class com.madowaku.focusraid.ui.StoreCaptureTest -e storeCapture true com.madowaku.focusraid.test/androidx.test.runner.AndroidJUnitRunner
    $result | Set-Content output/playwright/store-capture-tests.log -Encoding utf8
    if (($result -join "`n") -notmatch 'OK \(8 tests\)') { throw 'Capture tests failed. See output/playwright/store-capture-tests.log.' }
    Invoke-Adb pull "$remote/." $raw
    $captures = [ordered]@{}
    Get-ChildItem $raw -Filter '*.png' | Sort-Object Name | ForEach-Object { $captures[$_.Name] = (Get-FileHash $_.FullName -Algorithm SHA256).Hash.ToLowerInvariant() }
    $provenance = [ordered]@{
        productCommit = (& git rev-parse HEAD).Trim()
        capturedAtUtc = [DateTime]::UtcNow.ToString('o')
        device = $Serial
        viewport = '360x800 dp / 1080x2400 px / font scale 1'
        data = 'Local deterministic demo fixtures. No production users, timestamps, purchases or network calls.'
        tests = 'OK (8 tests)'
        testSourceSha256 = (Get-FileHash 'app/src/androidTest/java/com/madowaku/focusraid/ui/StoreCaptureTest.kt' -Algorithm SHA256).Hash.ToLowerInvariant()
        captures = $captures
    }
    $provenance | ConvertTo-Json -Depth 4 | Set-Content "$raw/capture-provenance.json" -Encoding utf8
} finally { Pop-Location }
