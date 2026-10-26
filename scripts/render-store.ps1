param([switch]$SkipServer)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
Push-Location $projectRoot
try {
    foreach ($directory in @('store/google-play/v1.0/ja-JP', 'store/google-play/v1.0/en-US', 'store/google-play/v1.0/review', 'output/playwright')) {
        New-Item -ItemType Directory -Force $directory | Out-Null
    }
    if (-not $SkipServer) {
        $server = Start-Process -FilePath (Get-Command python).Source -ArgumentList '-m','http.server','8731','--bind','127.0.0.1' -WorkingDirectory $projectRoot -WindowStyle Hidden -PassThru -RedirectStandardOutput 'output/playwright/store-server.log' -RedirectStandardError 'output/playwright/store-server-error.log'
    }
    & npx --yes --package '@playwright/cli@0.1.19' playwright-cli -s=focus-raid-store open 'http://127.0.0.1:8731/store/google-play/v1.0/creative.html'
    if ($LASTEXITCODE -ne 0) { throw 'Playwright did not open the renderer.' }
    & npx --yes --package '@playwright/cli@0.1.19' playwright-cli -s=focus-raid-store run-code --filename scripts/render-store-browser.js *> output/playwright/store-render.log
    if ($LASTEXITCODE -ne 0 -or (Select-String -Path output/playwright/store-render.log -Pattern '^### Error')) { throw 'Store rendering failed. See output/playwright/store-render.log.' }
    python scripts/verify-store.py --normalize
    if ($LASTEXITCODE -ne 0) { throw 'Store asset verification failed.' }
} finally {
    & npx --yes --package '@playwright/cli@0.1.19' playwright-cli -s=focus-raid-store close
    if ($server -and -not $server.HasExited) { Stop-Process -Id $server.Id }
    Pop-Location
}
