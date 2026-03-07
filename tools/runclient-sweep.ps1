param(
    [string[]]$Profiles,
    [switch]$Strict,
    [switch]$WithStacktrace
)

$ErrorActionPreference = 'Stop'

$repoRoot = Split-Path -Parent $PSScriptRoot
$portsRoot = Join-Path $repoRoot 'ports'
if (-not (Test-Path $portsRoot)) {
    throw "Ports directory not found: $portsRoot"
}

$profilesJsonPath = Join-Path $portsRoot 'profiles.json'

$knownDaemonResetProfiles = @(
    'forge-1.15.2-1.16.5',
    'forge-1.17-1.18.2',
    'forge-1.19.x'
)

$allProfiles = @()
if (Test-Path $profilesJsonPath) {
    $profilesData = Get-Content -Path $profilesJsonPath -Raw | ConvertFrom-Json
    if ($profilesData -and $profilesData.profiles) {
        $allProfiles = $profilesData.profiles |
            Select-Object -ExpandProperty profile |
            Sort-Object -Unique
    }
}

if (-not $allProfiles -or $allProfiles.Count -eq 0) {
    $excludedDirs = @('logs')
    $allProfiles = Get-ChildItem -Path $portsRoot -Directory |
        Where-Object { $excludedDirs -notcontains $_.Name } |
        Sort-Object Name |
        Select-Object -ExpandProperty Name
}

if ($Profiles -and $Profiles.Count -gt 0) {
    $profileSet = [System.Collections.Generic.HashSet[string]]::new([System.StringComparer]::OrdinalIgnoreCase)
    foreach ($p in $Profiles) { [void]$profileSet.Add($p) }
    $targets = $allProfiles | Where-Object { $profileSet.Contains($_) }
} else {
    $targets = $allProfiles
}

if (-not $targets -or $targets.Count -eq 0) {
    Write-Host 'No profiles selected.'
    exit 0
}

$runArgs = @('runClient', '--no-daemon')
if ($WithStacktrace) {
    $runArgs += '--stacktrace'
}

$results = @()
$timestamp = Get-Date -Format 'yyyyMMdd-HHmmss'
$logsDir = Join-Path $portsRoot "logs/runclient-sweep-$timestamp"
New-Item -ItemType Directory -Path $logsDir -Force | Out-Null

foreach ($entry in $targets) {
    $profileDir = Join-Path $portsRoot $entry
    $logFile = Join-Path $logsDir "$entry.log"
    $stdoutFile = Join-Path $logsDir "$entry.stdout.log"
    $stderrFile = Join-Path $logsDir "$entry.stderr.log"

    Write-Host "==== $entry ===="
    Push-Location $profileDir
    try {
        $proc = Start-Process -FilePath '.\gradlew.bat' -ArgumentList $runArgs -NoNewWindow -Wait -PassThru -RedirectStandardOutput $stdoutFile -RedirectStandardError $stderrFile
        $rawExit = $proc.ExitCode
    } finally {
        Pop-Location
    }

    Get-Content -Path $stdoutFile, $stderrFile -ErrorAction SilentlyContinue | Set-Content -Path $logFile

    $hasDaemonDispatchError = Select-String -Path $logFile -SimpleMatch 'Could not dispatch a message to the daemon.' -Quiet
    $hasStopMarker = Select-String -Path $logFile -Pattern 'Minecraft\]: Stopping!|\(Minecraft\) Stopping!' -Quiet
    $isKnownDaemonResetProfile = $knownDaemonResetProfiles -contains $entry

    $daemonResetSoftPass = $false
    if (-not $Strict -and $rawExit -ne 0 -and $isKnownDaemonResetProfile -and $hasDaemonDispatchError -and $hasStopMarker) {
        $daemonResetSoftPass = $true
    }

    $effectiveExit = if ($daemonResetSoftPass) { 0 } else { $rawExit }

    $results += [pscustomobject]@{
        Profile = $entry
        RawExit = $rawExit
        EffectiveExit = $effectiveExit
        DaemonResetSoftPass = $daemonResetSoftPass
        Log = $logFile
    }

    if ($daemonResetSoftPass) {
        Write-Host "RESULT $entry raw=$rawExit effective=0 (daemon-reset soft pass)"
    } else {
        Write-Host "RESULT $entry raw=$rawExit effective=$effectiveExit"
    }
}

Write-Host "`n=== Summary ==="
$results | Format-Table -AutoSize

$failed = $results | Where-Object { $_.EffectiveExit -ne 0 }
if ($failed.Count -gt 0) {
    Write-Host "`nFailed profiles (effective):" -ForegroundColor Red
    $failed | ForEach-Object { Write-Host "- $($_.Profile) (raw=$($_.RawExit), effective=$($_.EffectiveExit))" }
    exit 1
}

exit 0
