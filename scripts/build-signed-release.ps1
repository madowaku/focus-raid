[CmdletBinding()]
param(
    [string]$KeystorePath = (Join-Path $HOME ".focus-raid\focus-raid-upload.jks"),
    [string]$Alias = "focusraid-upload",
    [switch]$SkipClean
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Get-JavaTool {
    param([Parameter(Mandatory = $true)][string]$Name)

    $command = Get-Command "$Name.exe" -ErrorAction SilentlyContinue
    if ($null -ne $command) {
        return $command.Source
    }

    if (-not [string]::IsNullOrWhiteSpace($env:JAVA_HOME)) {
        $candidate = Join-Path $env:JAVA_HOME "bin\$Name.exe"
        if (Test-Path $candidate) {
            return $candidate
        }
    }

    throw "$Name.exe was not found. Install/use JDK 17 and make sure JAVA_HOME or PATH points to it."
}

function Convert-SecureStringToPlainText {
    param([Parameter(Mandatory = $true)][Security.SecureString]$Value)
    return [System.Net.NetworkCredential]::new("", $Value).Password
}

$repoRoot = Split-Path -Parent $PSScriptRoot
$keytool = Get-JavaTool -Name "keytool"
$jarsigner = Get-JavaTool -Name "jarsigner"
$keystoreFullPath = [System.IO.Path]::GetFullPath($KeystorePath)
$keyDirectory = Split-Path -Parent $keystoreFullPath

$gradlewPath = Join-Path $repoRoot "gradlew.bat"
if (Test-Path $gradlewPath) {
    $gradleCommand = $gradlewPath
    Write-Host "Using Gradle Wrapper: $gradlewPath" -ForegroundColor DarkGray
} else {
    $gradle = Get-Command "gradle" -ErrorAction SilentlyContinue
    if ($null -eq $gradle) {
        throw "Neither gradlew.bat nor a system Gradle command was found. Install Gradle 9.5.0+ or add the Gradle Wrapper to the repository."
    }
    $gradleCommand = $gradle.Source
    Write-Host "Using system Gradle: $gradleCommand" -ForegroundColor DarkGray
}

if (-not (Test-Path $keystoreFullPath)) {
    New-Item -ItemType Directory -Force -Path $keyDirectory | Out-Null

    Write-Host "Creating Focus Raid upload key:" -ForegroundColor Cyan
    Write-Host "  $keystoreFullPath"
    Write-Host "You will be prompted for the keystore/key password. Keep it private and back it up." -ForegroundColor Yellow

    & $keytool `
        -genkeypair `
        -v `
        -keystore $keystoreFullPath `
        -alias $Alias `
        -keyalg RSA `
        -keysize 4096 `
        -validity 10000 `
        -storetype JKS `
        -dname "CN=Focus Raid Upload, OU=Android, O=madowaku, C=JP"

    if ($LASTEXITCODE -ne 0) {
        throw "keytool failed while creating the upload key."
    }

    Write-Host "Upload key created. Back up this file somewhere separate from this PC:" -ForegroundColor Green
    Write-Host "  $keystoreFullPath"
} else {
    Write-Host "Using existing upload key:" -ForegroundColor Cyan
    Write-Host "  $keystoreFullPath"
}

$storePasswordSecure = Read-Host "Keystore password" -AsSecureString
$keyPasswordSecure = Read-Host "Key password (press Enter if it is the same as the keystore password)" -AsSecureString
$storePassword = Convert-SecureStringToPlainText $storePasswordSecure
$keyPassword = Convert-SecureStringToPlainText $keyPasswordSecure

if ([string]::IsNullOrEmpty($storePassword)) {
    throw "Keystore password cannot be empty."
}
if ([string]::IsNullOrEmpty($keyPassword)) {
    $keyPassword = $storePassword
}

$signingEnvironment = @(
    "FOCUS_RAID_UPLOAD_KEYSTORE_PATH",
    "FOCUS_RAID_UPLOAD_STORE_PASSWORD",
    "FOCUS_RAID_UPLOAD_KEY_ALIAS",
    "FOCUS_RAID_UPLOAD_KEY_PASSWORD"
)

try {
    $env:FOCUS_RAID_UPLOAD_KEYSTORE_PATH = $keystoreFullPath
    $env:FOCUS_RAID_UPLOAD_STORE_PASSWORD = $storePassword
    $env:FOCUS_RAID_UPLOAD_KEY_ALIAS = $Alias
    $env:FOCUS_RAID_UPLOAD_KEY_PASSWORD = $keyPassword

    $productionConfig = @(
        "FOCUS_RAID_REVENUECAT_GOOGLE_API_KEY",
        "FOCUS_RAID_FIREBASE_PROJECT_ID",
        "FOCUS_RAID_FIREBASE_API_KEY",
        "FOCUS_RAID_FIREBASE_APP_ID",
        "FOCUS_RAID_PRIVACY_POLICY_URL"
    )
    $missingProductionConfig = @(
        $productionConfig | Where-Object {
            [string]::IsNullOrWhiteSpace([Environment]::GetEnvironmentVariable($_))
        }
    )

    if ($missingProductionConfig.Count -gt 0) {
        Write-Warning "The bundle can be signed, but it is not yet a public-release candidate because these production values are missing:"
        foreach ($name in $missingProductionConfig) {
            Write-Host "  - $name" -ForegroundColor Yellow
        }
    }

    Push-Location $repoRoot
    try {
        $gradleArgs = if ($SkipClean) {
            @("bundleRelease", "--stacktrace")
        } else {
            @("clean", "bundleRelease", "--stacktrace")
        }

        & $gradleCommand @gradleArgs
        if ($LASTEXITCODE -ne 0) {
            throw "Gradle bundleRelease failed."
        }
    } finally {
        Pop-Location
    }

    $aabPath = Join-Path $repoRoot "app\build\outputs\bundle\release\app-release.aab"
    if (-not (Test-Path $aabPath)) {
        throw "Signed AAB was not found at expected path: $aabPath"
    }

    Write-Host "Verifying AAB signature..." -ForegroundColor Cyan
    & $jarsigner -verify -verbose -certs $aabPath
    if ($LASTEXITCODE -ne 0) {
        throw "jarsigner verification failed. Do not upload this AAB."
    }

    $sha256 = (Get-FileHash -Path $aabPath -Algorithm SHA256).Hash

    Write-Host ""
    Write-Host "SIGNED AAB READY" -ForegroundColor Green
    Write-Host "  Path:   $aabPath"
    Write-Host "  SHA256: $sha256"
    Write-Host ""
    Write-Host "Keep the upload keystore and its passwords backed up. Never commit them to Git." -ForegroundColor Yellow
} finally {
    foreach ($name in $signingEnvironment) {
        Remove-Item "Env:$name" -ErrorAction SilentlyContinue
    }
    $storePassword = $null
    $keyPassword = $null
}
