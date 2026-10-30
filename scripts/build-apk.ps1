<#
.SYNOPSIS
    ChordPlay Android Build & Version Automation Script

.DESCRIPTION
    Builds ChordPlay Android APK, automatically bumps versionCode and versionName in app/build.gradle.kts,
    and copies the resulting APKs into version/Android/ with consistent naming and version history logging.

.PARAMETER Variant
    Build variant to compile (default: devDebug).
    Options: devDebug, prodRelease, devRelease, prodDebug.

.PARAMETER Bump
    Version bump strategy (default: patch).
    Options: patch, minor, major, none.

.PARAMETER ExplicitVersionCode
    Override and specify an exact versionCode integer.

.PARAMETER ExplicitVersionName
    Override and specify an exact versionName string.

.PARAMETER Clean
    Run Gradle clean before building.

.EXAMPLE
    .\scripts\build-apk.ps1
    # Bumps versionCode + 1, bumps patch (e.g., 1.8 -> 1.8.1), builds devDebug, and copies to version/Android/

.EXAMPLE
    .\scripts\build-apk.ps1 -Bump minor
    # Bumps minor version (e.g., 1.8 -> 1.9), builds devDebug, and copies to version/Android/

.EXAMPLE
    .\scripts\build-apk.ps1 -Variant prodRelease
    # Bumps version and builds signed prod release APK.
#>

[CmdletBinding()]
param(
    [ValidateSet("devDebug", "prodRelease", "devRelease", "prodDebug")]
    [string]$Variant = "devDebug",

    [ValidateSet("patch", "minor", "major", "none")]
    [string]$Bump = "patch",

    [int]$ExplicitVersionCode = 0,

    [string]$ExplicitVersionName = "",

    [switch]$NoBump,

    [switch]$Clean,

    [switch]$Both,

    [string]$DestinationDir = "version/Android"
)

$ErrorActionPreference = "Stop"

# Ensure we are in project root
$rootDir = (Resolve-Path "$PSScriptRoot\..").Path
Set-Location $rootDir

if ($Both) {
    Write-Host "Building Both Production and Development APKs..." -ForegroundColor Magenta
    & "$PSScriptRoot\build-apk.ps1" -Variant "prodDebug" -Bump $Bump -ExplicitVersionCode $ExplicitVersionCode -ExplicitVersionName $ExplicitVersionName -NoBump:$NoBump -Clean:$Clean -DestinationDir $DestinationDir
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
    & "$PSScriptRoot\build-apk.ps1" -Variant "devDebug" -NoBump -DestinationDir $DestinationDir
    exit $LASTEXITCODE
}

Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "   ChordPlay Android Build & Version Automation" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "Workspace Root : $rootDir" -ForegroundColor Gray
Write-Host "Target Variant : $Variant" -ForegroundColor Gray

# --------------------------------------------------------------------------
# 1. Environment Verification (JDK 21 & CMake)
# --------------------------------------------------------------------------
$defaultJdk = "C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot"
if (-not $env:JAVA_HOME -or -not (Test-Path "$env:JAVA_HOME\bin\java.exe")) {
    if (Test-Path "$defaultJdk\bin\java.exe") {
        $env:JAVA_HOME = $defaultJdk
        $env:Path = "$defaultJdk\bin;$env:Path"
        Write-Host "Configured JAVA_HOME -> $env:JAVA_HOME" -ForegroundColor DarkCyan
    } else {
        $adoptiumDir = Get-ChildItem "C:\Program Files\Eclipse Adoptium" -Filter "jdk-21*" -ErrorAction SilentlyContinue | Select-Object -ExpandProperty FullName -First 1
        if ($adoptiumDir -and (Test-Path "$adoptiumDir\bin\java.exe")) {
            $env:JAVA_HOME = $adoptiumDir
            $env:Path = "$adoptiumDir\bin;$env:Path"
            Write-Host "Configured JAVA_HOME -> $env:JAVA_HOME" -ForegroundColor DarkCyan
        }
    }
}

if (-not (Get-Command cmake -ErrorAction SilentlyContinue)) {
    if (Test-Path "C:\Program Files\CMake\bin\cmake.exe") {
        $env:Path = "C:\Program Files\CMake\bin;$env:Path"
    }
}

# --------------------------------------------------------------------------
# 2. Read Current Version Info
# --------------------------------------------------------------------------
$gradleFile = Join-Path $rootDir "app\build.gradle.kts"
if (-not (Test-Path $gradleFile)) {
    Write-Error "Cannot find app build file at $gradleFile"
}

$gradleContent = [System.IO.File]::ReadAllText($gradleFile)

$currentCodeMatch = [regex]::Match($gradleContent, '(?m)^\s*versionCode\s*=\s*(\d+)')
$currentNameMatch = [regex]::Match($gradleContent, '(?m)^\s*versionName\s*=\s*"([^"]+)"')

if (-not $currentCodeMatch.Success) {
    Write-Error "Failed to parse versionCode from $gradleFile"
}
if (-not $currentNameMatch.Success) {
    Write-Error "Failed to parse versionName from $gradleFile"
}

$currentVersionCode = [int]$currentCodeMatch.Groups[1].Value
$currentVersionName = $currentNameMatch.Groups[1].Value

Write-Host "`nCurrent Version in app/build.gradle.kts:" -ForegroundColor Yellow
Write-Host "  versionCode : $currentVersionCode" -ForegroundColor Yellow
Write-Host "  versionName : $currentVersionName" -ForegroundColor Yellow

# --------------------------------------------------------------------------
# 3. Calculate New Version Code & Name
# --------------------------------------------------------------------------
$effectiveBump = if ($NoBump) { "none" } else { $Bump }

$newVersionCode = $currentVersionCode
$newVersionName = $currentVersionName

if ($ExplicitVersionCode -gt 0) {
    $newVersionCode = $ExplicitVersionCode
} elseif ($effectiveBump -ne "none") {
    $newVersionCode = $currentVersionCode + 1
}

if ($ExplicitVersionName -ne "") {
    $newVersionName = $ExplicitVersionName
} elseif ($effectiveBump -ne "none") {
    # Parse version segments (e.g. "1.8" or "1.8.1")
    $parts = $currentVersionName.Split('.')
    $major = if ($parts.Length -ge 1 -and [int]::TryParse($parts[0], [ref]$null)) { [int]$parts[0] } else { 1 }
    $minor = if ($parts.Length -ge 2 -and [int]::TryParse($parts[1], [ref]$null)) { [int]$parts[1] } else { 0 }
    $patch = if ($parts.Length -ge 3 -and [int]::TryParse($parts[2], [ref]$null)) { [int]$parts[2] } else { 0 }

    switch ($effectiveBump) {
        "patch" {
            if ($parts.Length -eq 2) {
                # 1.8 -> 1.8.1
                $newVersionName = "$major.$minor.1"
            } else {
                # 1.8.1 -> 1.8.2
                $newVersionName = "$major.$minor.$($patch + 1)"
            }
        }
        "minor" {
            $newVersionName = "$major.$($minor + 1).0"
        }
        "major" {
            $newVersionName = "$($major + 1).0.0"
        }
    }
}

Write-Host "`nTarget Version for this Build:" -ForegroundColor Green
Write-Host "  versionCode : $newVersionCode $(if ($newVersionCode -ne $currentVersionCode) { '(BUMPED +1)' } else { '(Unchanged)' })" -ForegroundColor Green
Write-Host "  versionName : $newVersionName $(if ($newVersionName -ne $currentVersionName) { '(BUMPED)' } else { '(Unchanged)' })" -ForegroundColor Green

# --------------------------------------------------------------------------
# 4. Update app/build.gradle.kts
# --------------------------------------------------------------------------
$originalGradleContent = $gradleContent
if ($newVersionCode -ne $currentVersionCode -or $newVersionName -ne $currentVersionName) {
    $updatedContent = [regex]::Replace($gradleContent, '(?m)^(\s*versionCode\s*=\s*)\d+', "`${1}$newVersionCode")
    $updatedContent = [regex]::Replace($updatedContent, '(?m)^(\s*versionName\s*=\s*)"[^"]+"', "`${1}`"$newVersionName`"")
    [System.IO.File]::WriteAllText($gradleFile, $updatedContent)
    Write-Host "`nUpdated app/build.gradle.kts successfully." -ForegroundColor Cyan
}

# --------------------------------------------------------------------------
# 5. Execute Gradle Build
# --------------------------------------------------------------------------
$taskName = switch ($Variant) {
    "devDebug"    { ":app:assembleDevDebug" }
    "prodRelease" { ":app:assembleProdRelease" }
    "devRelease"  { ":app:assembleDevRelease" }
    "prodDebug"   { ":app:assembleProdDebug" }
}

$gradleArgs = @()
if ($Clean) {
    $gradleArgs += "clean"
}
$gradleArgs += $taskName
$gradleArgs += "--stacktrace"

Write-Host "`nRunning Gradle Build: .\gradlew.bat $($gradleArgs -join ' ')" -ForegroundColor Cyan
$sw = [System.Diagnostics.Stopwatch]::StartNew()

& ".\gradlew.bat" @gradleArgs
$buildExitCode = $LASTEXITCODE
$sw.Stop()

if ($buildExitCode -ne 0) {
    Write-Host "`n[ERROR] Gradle build failed with exit code $buildExitCode!" -ForegroundColor Red
    Write-Host "Reverting app/build.gradle.kts changes..." -ForegroundColor Yellow
    [System.IO.File]::WriteAllText($gradleFile, $originalGradleContent)
    exit $buildExitCode
}

Write-Host "`nGradle Build Succeeded in $([math]::Round($sw.Elapsed.TotalSeconds, 1))s!" -ForegroundColor Green

# --------------------------------------------------------------------------
# 6. Locate Built APKs
# --------------------------------------------------------------------------
$flavor = if ($Variant.StartsWith("dev")) { "dev" } else { "prod" }
$buildType = if ($Variant.EndsWith("Debug")) { "debug" } else { "release" }
$outputDir = Join-Path $rootDir "app\build\outputs\apk\$flavor\$buildType"

if (-not (Test-Path $outputDir)) {
    Write-Error "Expected build outputs directory not found at: $outputDir"
}

$apkFiles = Get-ChildItem -Path $outputDir -Filter "*.apk"
if ($apkFiles.Count -eq 0) {
    Write-Error "No APK files found in $outputDir"
}

# --------------------------------------------------------------------------
# 7. Copy APKs to version/Android/
# --------------------------------------------------------------------------
$targetDir = if ($DestinationDir -eq "version/Android" -or $DestinationDir -eq "version\Android") {
    Join-Path $rootDir "$DestinationDir\$flavor"
} else {
    Join-Path $rootDir $DestinationDir
}
if (-not (Test-Path $targetDir)) {
    New-Item -ItemType Directory -Path $targetDir -Force | Out-Null
    Write-Host "Created directory: $targetDir" -ForegroundColor Cyan
}

Write-Host "`nArchiving APKs to: $targetDir" -ForegroundColor Cyan

$copiedFiles = @()
foreach ($apk in $apkFiles) {
    # Extract ABI classifier if present (e.g. app-dev-arm64-v8a-debug.apk -> arm64-v8a)
    $classifier = ""
    if ($apk.Name -match "app-(?:dev|prod)-(.+)-(?:debug|release)\.apk") {
        $classifier = "-$($matches[1])"
    } elseif ($apk.Name -match "app-(?:dev|prod)-(?:debug|release)\.apk") {
        $classifier = ""
    } else {
        $classifier = "-$($apk.BaseName)"
    }

    $destFileName = "ChordPlay-v${newVersionName}-vc${newVersionCode}-${Variant}${classifier}.apk"
    $destPath = Join-Path $targetDir $destFileName

    Copy-Item -Path $apk.FullName -Destination $destPath -Force
    $fileSizeMB = [math]::Round($apk.Length / 1MB, 2)
    Write-Host "  -> $destFileName ($fileSizeMB MB)" -ForegroundColor Green

    $copiedFiles += [PSCustomObject]@{
        FileName = $destFileName
        SizeMB   = $fileSizeMB
        Path     = $destPath
        IsUniversal = ($classifier -eq "-universal" -or $classifier -eq "")
    }

    # Also maintain latest convenience copy for universal APK
    if ($classifier -eq "-universal") {
        $latestName = "ChordPlay-latest-${Variant}.apk"
        Copy-Item -Path $apk.FullName -Destination (Join-Path $targetDir $latestName) -Force
    }
}

# --------------------------------------------------------------------------
# 8. Record Version History
# --------------------------------------------------------------------------
$historyFile = Join-Path $targetDir "VERSION_HISTORY.md"
$timestamp = (Get-Date).ToString("yyyy-MM-dd HH:mm:ss")
$historyEntry = @"

### Build v$newVersionName (versionCode: $newVersionCode)
- **Timestamp**: $timestamp
- **Variant**: $Variant
- **Status**: SUCCESS
- **Archived Files**:
"@

foreach ($file in $copiedFiles) {
    $historyEntry += "`n  - ``$($file.FileName)`` ($($file.SizeMB) MB)"
}

if (-not (Test-Path $historyFile)) {
    $header = @"
# ChordPlay Android APK Build History

This directory contains versioned APK builds generated by the automated build script.

"@
    [System.IO.File]::WriteAllText($historyFile, $header + $historyEntry)
} else {
    [System.IO.File]::AppendAllText($historyFile, "`n" + $historyEntry)
}

# Also write JSON metadata for programmatic consumption
$jsonHistoryFile = Join-Path $targetDir "builds.json"
$records = @()
if (Test-Path $jsonHistoryFile) {
    try {
        $records = Get-Content $jsonHistoryFile -Raw | ConvertFrom-Json
        if ($records -isnot [System.Collections.IList]) { $records = @($records) }
    } catch {
        $records = @()
    }
}

$newRecord = [PSCustomObject]@{
    timestamp   = $timestamp
    versionCode = $newVersionCode
    versionName = $newVersionName
    variant     = $Variant
    files       = ($copiedFiles | Select-Object FileName, SizeMB)
}
$records = @($newRecord) + $records
$records | ConvertTo-Json -Depth 5 | Set-Content $jsonHistoryFile

Write-Host "`n============================================================" -ForegroundColor Green
Write-Host "   BUILD & ARCHIVE COMPLETE" -ForegroundColor Green
Write-Host "============================================================" -ForegroundColor Green
Write-Host "Version Code : $newVersionCode" -ForegroundColor White
Write-Host "Version Name : $newVersionName" -ForegroundColor White
Write-Host "Archived to  : $targetDir" -ForegroundColor White
Write-Host "History Log  : $historyFile" -ForegroundColor White
Write-Host "============================================================`n" -ForegroundColor Green
