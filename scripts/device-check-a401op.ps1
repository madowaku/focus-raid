param(
    [Parameter(Mandatory = $true)]
    [ValidateNotNullOrEmpty()]
    [string]$Serial
)

$ErrorActionPreference = 'Stop'
$package = 'com.madowaku.focusraid'
$receiver = "$package/.DurabilityDebugReceiver"
$apk = Join-Path $PSScriptRoot '..\app\build\outputs\apk\debug\app-debug.apk'
$sdk = if ($env:ANDROID_HOME) { $env:ANDROID_HOME } elseif ($env:ANDROID_SDK_ROOT) { $env:ANDROID_SDK_ROOT } else { Join-Path $env:LOCALAPPDATA 'Android\Sdk' }
$adb = Join-Path $sdk 'platform-tools\adb.exe'

if (-not (Test-Path -LiteralPath $adb)) { throw "adb.exe not found at $adb" }
if ($Serial -match '(?i)^(emulator-|.*emulator.*|.*sdk_gphone.*)') { throw 'Emulator serial refused.' }
$devices = & $adb devices -l
if ($LASTEXITCODE -ne 0) { throw 'adb devices -l failed.' }
$line = @($devices | Where-Object { $_ -match "^$([regex]::Escape($Serial))\s+device\s" })
if ($line.Count -ne 1 -or $line[0] -notmatch '(^|\s)model:A401OP(\s|$)') { throw 'Serial is not an attached A401OP in adb devices -l.' }
$model = (& $adb -s $Serial shell getprop ro.product.model).Trim()
if ($LASTEXITCODE -ne 0 -or $model -ne 'A401OP') { throw "Device model verification failed: $model" }
$androidVersion = (& $adb -s $Serial shell getprop ro.build.version.release).Trim()
if ($LASTEXITCODE -ne 0 -or $androidVersion -ne '15') { throw "Expected Android 15 on A401OP; found $androidVersion" }
if (-not (Test-Path -LiteralPath $apk)) { throw "Debug APK missing: $apk. Run .\gradlew.bat assembleDebug first." }

Write-Host "Verified physical A401OP: $Serial. The check installs the debug app and replaces its active focus session."
Read-Host 'Use a test profile with no active focus session. Press Enter to continue'

$stamp = Get-Date -Format 'yyyyMMdd-HHmmss'
$outDir = Join-Path $PSScriptRoot "..\output\device-check\$stamp"
New-Item -ItemType Directory -Path $outDir -Force | Out-Null
$report = Join-Path $outDir 'results.md'
$rows = [System.Collections.Generic.List[string]]::new()
$rows.Add('# A401OP device check')
$rows.Add('')
$rows.Add("Date: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss zzz')")
$rows.Add("Serial: $Serial; model: $model")
$rows.Add('Debug APK: app/build/outputs/apk/debug/app-debug.apk')
$rows.Add('')
$rows.Add('| Check | Operator result | Notes |')
$rows.Add('| --- | --- | --- |')

function Invoke-Adb([string[]]$Arguments) {
    $value = & $adb -s $Serial @Arguments 2>&1
    if ($LASTEXITCODE -ne 0) { throw "adb $($Arguments -join ' ') failed: $value" }
    return ($value -join "`n").Trim()
}
function Broadcast([string]$Action, [string[]]$Extras = @()) {
    $value = Invoke-Adb (@('shell', 'am', 'broadcast', '-n', $receiver, '-a', "$package.debug.$Action") + $Extras)
    Write-Host $value
    return $value
}
function Save-Screenshot([string]$Name) {
    $remote = "/sdcard/Download/focus-raid-$stamp-$Name.png"
    $local = Join-Path $outDir "$Name.png"
    Invoke-Adb @('shell', 'screencap', '-p', $remote) | Out-Null
    Invoke-Adb @('pull', $remote, $local) | Out-Null
    Invoke-Adb @('shell', 'rm', $remote) | Out-Null
    Write-Host "Screenshot: $local"
}
function Record-Operator([string]$Name, [string]$Prompt, [string]$Screenshot = '') {
    Write-Host "`n$Prompt"
    Read-Host 'Press Enter when ready to capture/record' | Out-Null
    if ($Screenshot) { Save-Screenshot $Screenshot }
    do { $answer = (Read-Host 'Result (PASS / FAIL / BLOCKED / NOT TESTED)').Trim().ToUpperInvariant() }
    until ($answer -in @('PASS', 'FAIL', 'BLOCKED', 'NOT TESTED'))
    $note = (Read-Host 'Observed details (required; use n/a if blocked)').Trim()
    while (-not $note) { $note = (Read-Host 'Observed details are required').Trim() }
    $safeName = $Name.Replace('|', '/')
    $safeNote = $note.Replace('|', '/').Replace("`r", ' ').Replace("`n", ' ')
    $rows.Add("| $safeName | $answer | $safeNote |")
}
function Set-NotificationGrant([bool]$Grant) {
    $verb = if ($Grant) { 'grant' } else { 'revoke' }
    Invoke-Adb @('shell', 'pm', $verb, $package, 'android.permission.POST_NOTIFICATIONS') | Out-Null
}
function Timer-Scenario([string]$Name, [bool]$ExactAllowed, [bool]$Doze) {
    Broadcast 'CLEAR_TIMER' | Out-Null
    $mode = if ($ExactAllowed) { 'allow' } else { 'ignore' }
    Invoke-Adb @('shell', 'appops', 'set', $package, 'SCHEDULE_EXACT_ALARM', $mode) | Out-Null
    $seed = Broadcast 'SEED_TIMER' @('--el', 'duration_ms', '30000')
    if ($seed -notmatch 'seeded=true' -or $seed -notmatch "exact=$($ExactAllowed.ToString().ToLowerInvariant())") {
        throw "Unexpected seed or exact-alarm state: $seed"
    }
    Invoke-Adb @('shell', 'am', 'start', '-W', '-n', "$package/.MainActivity") | Out-Null
    Invoke-Adb @('shell', 'input', 'keyevent', 'KEYCODE_HOME') | Out-Null
    Invoke-Adb @('shell', 'input', 'keyevent', 'KEYCODE_SLEEP') | Out-Null
    if ($Doze) {
        Invoke-Adb @('shell', 'dumpsys', 'battery', 'unplug') | Out-Null
        $idle = Invoke-Adb @('shell', 'dumpsys', 'deviceidle', 'force-idle')
        Write-Host "Doze request: $idle"
        $state = Invoke-Adb @('shell', 'dumpsys', 'deviceidle')
        if ($state -notmatch 'mState=IDLE(\s|$)') { throw 'Device did not enter deep idle; Doze result cannot be claimed.' }
    }
    Start-Sleep -Seconds 40
    $probe = Broadcast 'PROBE_TIMER'
    $rows.Add("| $Name adb probe | OBSERVED | $($probe.Replace('|', '/').Replace("`n", ' ')) |")
    if ($Doze) {
        Invoke-Adb @('shell', 'dumpsys', 'deviceidle', 'unforce') | Out-Null
        Invoke-Adb @('shell', 'dumpsys', 'battery', 'reset') | Out-Null
    }
    Invoke-Adb @('shell', 'input', 'keyevent', 'KEYCODE_WAKEUP') | Out-Null
    Invoke-Adb @('shell', 'am', 'start', '-W', '-n', "$package/.MainActivity") | Out-Null
    $after = Broadcast 'PROBE_TIMER'
    $rows.Add("| $Name reopened probe | OBSERVED | $($after.Replace('|', '/').Replace("`n", ' ')) |")
    Record-Operator $Name 'Check timer completion, focus credit and notification behavior after screen-off/background. In denied exact-alarm mode, delivery may be delayed; record actual timing and eventual recovery.' "timer-$Name"
}

$originalFont = $null
try {
    $originalFont = Invoke-Adb @('shell', 'settings', 'get', 'system', 'font_scale')
    $rows.Add("| Android version | OBSERVED | $(Invoke-Adb @('shell', 'getprop', 'ro.build.version.release')) |")
    $rows.Add("| Display size | OBSERVED | $(Invoke-Adb @('shell', 'wm', 'size')) |")
    Invoke-Adb @('install', '-r', $apk) | Out-Null
    Invoke-Adb @('shell', 'settings', 'put', 'system', 'font_scale', '1.5') | Out-Null
    Write-Host "Original font_scale=$originalFont; set to 1.5."

    Set-NotificationGrant $false
    Record-Operator 'Notification denied' 'Open Focus Raid, deny notifications, complete a short focus and check the timer still finishes. No notification is expected.' 'notification-denied'
    Set-NotificationGrant $true
    Record-Operator 'Notification re-granted' 'Open Focus Raid and verify notification permission is restored; note whether a completion notification appears.' 'notification-granted'

    Timer-Scenario 'exact-allowed-doze' $true $true
    Timer-Scenario 'exact-denied-screen-off' $false $false
    Record-Operator 'Exact-alarm permission UI' 'Use Android app special access UI to deny, then re-grant exact alarms. Check the app remains usable in both states. Record the observed state.' 'exact-alarm-permission'
    Record-Operator 'Normal focus session' 'Start a normal focus session from the UI. Background the app and turn the screen off. Reopen after the timer ends; record selected duration, start/end times, completion and focus credit.' 'normal-focus-session'

    foreach ($screen in @('home', 'focusing', 'walking-notebook', 'paywall', 'consent-dialog')) {
        Record-Operator "Font 1.5 $screen" "Navigate to $screen at font scale 1.5. Check 360x800-class fit, scrolling, clipping and reachable controls. If this device has a different viewport, record its actual size." "font-$screen"
    }

    Record-Operator 'READ_STEPS deny/revoke/re-grant' 'In Health Connect UI, deny READ_STEPS, grant it, revoke it, then re-grant it. After each state, refresh the notebook and record the actual app response.' 'health-permission'
    Record-Operator 'Health Connect rationale' 'Open Focus Raid permission usage/rationale from Health Connect. Verify the full Privacy Policy link is visible and opens an https page; record the URL and result.' 'health-rationale'
    Record-Operator 'Real walking and step comparison' 'Walk with the A401OP. Compare Focus Raid notebook count with Health Connect own count for the same day and time zone. Record both numbers, source and refresh time.' 'walking-count'
} finally {
    try { Invoke-Adb @('shell', 'dumpsys', 'deviceidle', 'unforce') | Out-Null } catch { Write-Warning $_ }
    try { Invoke-Adb @('shell', 'dumpsys', 'battery', 'reset') | Out-Null } catch { Write-Warning $_ }
    try { Invoke-Adb @('shell', 'input', 'keyevent', 'KEYCODE_WAKEUP') | Out-Null } catch { Write-Warning $_ }
    if ($originalFont -and $originalFont -ne 'null') {
        try { Invoke-Adb @('shell', 'settings', 'put', 'system', 'font_scale', $originalFont) | Out-Null } catch { Write-Warning "Could not restore font scale: $_" }
    }
    $rows.Add('')
    $rows.Add("Font scale restoration attempted: $originalFont")
    if ($report) { $rows | Set-Content -LiteralPath $report -Encoding UTF8; Write-Host "Results: $report" }
}
