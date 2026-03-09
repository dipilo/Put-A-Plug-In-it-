param(
    [Parameter(Mandatory = $true)]
    [ValidatePattern('^\d+\.\d+\.\d+$')]
    [string]$Version,

    [switch]$UpdatePorts
)

$ErrorActionPreference = 'Stop'

$root = Split-Path -Parent $PSScriptRoot
$gradleProps = Join-Path $root 'gradle.properties'

if (-not (Test-Path $gradleProps)) {
    throw "Could not find root gradle.properties at $gradleProps"
}

$content = Get-Content -Path $gradleProps -Raw
if ($content -notmatch '(?m)^\s*mod_version\s*=') {
    throw "mod_version key not found in $gradleProps"
}

$updated = [regex]::Replace($content, '(?m)^\s*mod_version\s*=\s*.*$', "mod_version=$Version")
[System.IO.File]::WriteAllText($gradleProps, $updated, [System.Text.UTF8Encoding]::new($false))
Write-Host "Updated root mod_version to $Version"

if ($UpdatePorts) {
    $buildFiles = Get-ChildItem -Path (Join-Path $root 'ports') -Filter 'build.gradle' -Recurse -File
    foreach ($file in $buildFiles) {
        $lines = Get-Content -Path $file.FullName
        $newLines = $lines | ForEach-Object {
            if ($_ -match '^(\s*version\s*=\s*".*-(forge|fabric|neoforge)-)(\d+\.\d+\.\d+)("\s*)$') {
                "$($Matches[1])$Version$($Matches[4])"
            }
            else {
                $_
            }
        }
        [System.IO.File]::WriteAllText($file.FullName, ($newLines -join "`n"), [System.Text.UTF8Encoding]::new($false))
    }

    Write-Host "Updated port build.gradle versions to $Version"
}

Write-Host "Done."
