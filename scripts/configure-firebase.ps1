[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [string]$GoogleServicesJson,
    [string]$PackageName = "com.madowaku.focusraid"
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$jsonPath = [System.IO.Path]::GetFullPath($GoogleServicesJson)
if (-not (Test-Path $jsonPath)) {
    throw "google-services.json was not found: $jsonPath"
}

$config = Get-Content $jsonPath -Raw | ConvertFrom-Json
$projectId = [string]$config.project_info.project_id

$client = @($config.client) | Where-Object {
    $_.client_info.android_client_info.package_name -eq $PackageName
} | Select-Object -First 1

if ($null -eq $client) {
    $foundPackages = @($config.client | ForEach-Object {
        $_.client_info.android_client_info.package_name
    }) -join ", "
    throw "No Firebase Android client for package '$PackageName' was found. Found: $foundPackages"
}

$appId = [string]$client.client_info.mobilesdk_app_id
$apiKey = [string](@($client.api_key) | Select-Object -First 1).current_key

if ([string]::IsNullOrWhiteSpace($projectId)) {
    throw "project_info.project_id is missing from google-services.json"
}
if ([string]::IsNullOrWhiteSpace($appId)) {
    throw "mobilesdk_app_id is missing for package '$PackageName'"
}
if ([string]::IsNullOrWhiteSpace($apiKey)) {
    throw "API key is missing for package '$PackageName'"
}

$gradleDir = Join-Path $HOME ".gradle"
$gradleProperties = Join-Path $gradleDir "gradle.properties"
New-Item -ItemType Directory -Force -Path $gradleDir | Out-Null

$managedKeys = @(
    "FOCUS_RAID_FIREBASE_PROJECT_ID",
    "FOCUS_RAID_FIREBASE_API_KEY",
    "FOCUS_RAID_FIREBASE_APP_ID"
)

$existingLines = if (Test-Path $gradleProperties) {
    @(Get-Content $gradleProperties)
} else {
    @()
}

$filteredLines = @($existingLines | Where-Object {
    $line = $_
    -not ($managedKeys | Where-Object { $line -match "^$([regex]::Escape($_))=" })
})

$newLines = @(
    $filteredLines
    "FOCUS_RAID_FIREBASE_PROJECT_ID=$projectId"
    "FOCUS_RAID_FIREBASE_API_KEY=$apiKey"
    "FOCUS_RAID_FIREBASE_APP_ID=$appId"
)

Set-Content -Path $gradleProperties -Value $newLines -Encoding UTF8

$maskedApiKey = if ($apiKey.Length -gt 8) {
    "$($apiKey.Substring(0,4))...$($apiKey.Substring($apiKey.Length - 4))"
} else {
    "(configured)"
}

Write-Host "Firebase configuration saved to user Gradle properties." -ForegroundColor Green
Write-Host "  Package:    $PackageName"
Write-Host "  Project ID: $projectId"
Write-Host "  App ID:     $appId"
Write-Host "  API key:    $maskedApiKey"
Write-Host "  File:       $gradleProperties"
Write-Host ""
Write-Host "The app does not require google-services.json at runtime. Keep or delete the downloaded file as you prefer; do not commit account-specific config to the repository." -ForegroundColor Yellow
