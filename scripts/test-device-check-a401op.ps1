# Device-free regression test for the A401OP acceptance script.
# This loads only its helper functions, never the top-level device-changing flow.
$ErrorActionPreference = 'Stop'
$source = Join-Path $PSScriptRoot 'device-check-a401op.ps1'
$tokens = $null
$parseErrors = $null
$ast = [System.Management.Automation.Language.Parser]::ParseFile(
    $source, [ref]$tokens, [ref]$parseErrors
)
if ($parseErrors.Count -gt 0) {
    throw "PowerShell parse errors: $($parseErrors -join '; ')"
}

foreach ($name in @('Invoke-Adb', 'Broadcast')) {
    $functionAst = $ast.Find({
        param($node)
        $node -is [System.Management.Automation.Language.FunctionDefinitionAst] -and
        $node.Name -eq $name
    }, $true)
    if (-not $functionAst) { throw "Missing helper function: $name" }
    . ([scriptblock]::Create($functionAst.Extent.Text))
}

# ADB commands must supply a single argv array to Invoke-Adb.
$commands = @($ast.FindAll({
    param($node)
    $node -is [System.Management.Automation.Language.CommandAst] -and
    $node.GetCommandName() -eq 'Invoke-Adb'
}, $true))
if ($commands.Count -lt 20) {
    throw "Expected at least 20 Invoke-Adb call sites, found $($commands.Count)"
}
foreach ($command in $commands) {
    if ($command.CommandElements.Count -ne 2 -or
        -not $command.CommandElements[1].Extent.Text.StartsWith('(')) {
        throw "Invoke-Adb must receive one parenthesized argv array at line $($command.Extent.StartLineNumber)"
    }
}

$script:captured = @()
$script:mockExitCode = 0
function mock-adb {
    $script:captured = @($args)
    $global:LASTEXITCODE = $script:mockExitCode
    if ($script:mockExitCode -eq 0) { 'ok' } else { 'adb failed' }
}
$adb = 'mock-adb'
$Serial = 'A401OP-TEST'
$package = 'com.madowaku.focusraid'
$receiver = "$package/.DurabilityDebugReceiver"

$result = Invoke-Adb (@('shell', 'input', 'keyevent', 'KEYCODE_HOME'))
$expected = '-s|A401OP-TEST|shell|input|keyevent|KEYCODE_HOME'
if ($result -ne 'ok' -or ($script:captured -join '|') -ne $expected) {
    throw "Invoke-Adb argument regression: $($script:captured -join '|')"
}

$null = Broadcast 'SEED_TIMER' @('--el', 'duration_ms', '30000')
$expected = "-s|A401OP-TEST|shell|am|broadcast|-n|$receiver|-a|$package.debug.SEED_TIMER|--el|duration_ms|30000"
if (($script:captured -join '|') -ne $expected) {
    throw "Broadcast argument regression: $($script:captured -join '|')"
}

$script:mockExitCode = 1
$rejected = $false
try {
    Invoke-Adb (@('shell', 'bad-command')) | Out-Null
} catch {
    $rejected = $_.Exception.Message -match 'adb shell bad-command failed'
}
if (-not $rejected) { throw 'Invoke-Adb failed to propagate a nonzero adb exit code' }

Write-Host "A401OP PowerShell parser, argv and error propagation: PASS ($($commands.Count) call sites)"
