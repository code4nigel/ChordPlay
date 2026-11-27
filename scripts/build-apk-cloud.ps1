<#
.SYNOPSIS
    ChordPlay Cloud Android Build & Version Automation (GitHub Actions)

.DESCRIPTION
    Interactive companion to scripts\build-apk.ps1 that bumps versionCode/versionName
    in app/build.gradle.kts, optionally commits & pushes to your branch, triggers
    .github/workflows/android-build.yml on GitHub Actions, watches progress, downloads
    the built APK(s) into version/Android/, logs history, and optionally installs via adb.
#>

[CmdletBinding()]
param(
    [ValidateSet("prodRelease", "prodBenchmark", "devRelease", "devBenchmark", "devDebug")]
    [string]$Variant = "prodRelease",

    [ValidateSet("patch", "minor", "major", "none")]
    [string]$Bump = "patch",

    [int]$ExplicitVersionCode = 0,

    [string]$ExplicitVersionName = "",

    [string]$Abi = "",

    [string]$Branch = "",

    [ValidateSet("3", "7", "14", "30")]
    [string]$RetentionDays = "7",

    [switch]$NoBump,

    [switch]$NoWait,

    [string]$DestinationDir = "version/Android"
)

$ErrorActionPreference = "Stop"

$rootDir = (Resolve-Path "$PSScriptRoot\..").Path
Set-Location $rootDir

function Get-AdbPath {
    $adbPath = "D:\ADB\adb.exe"
    if (Test-Path $adbPath) { return $adbPath }
    $adbCmd = Get-Command adb -ErrorAction SilentlyContinue
    if ($adbCmd) { return $adbCmd.Source }
    return $null
}

Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "   ChordPlay Cloud Android Build (GitHub Actions)" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "Workspace Root : $rootDir" -ForegroundColor Gray

# --------------------------------------------------------------------------
# 1. Verify gh CLI & authentication
# --------------------------------------------------------------------------
if (-not (Get-Command gh -ErrorAction SilentlyContinue)) {
    Write-Error "GitHub CLI ('gh') is not installed or not in PATH."
    exit 1
}

& gh auth status *> $null
if ($LASTEXITCODE -ne 0) {
    Write-Error "GitHub CLI is not authenticated. Run 'gh auth login' first."
    exit 1
}

# Detect current git branch
$currentBranch = (& git rev-parse --abbrev-ref HEAD).Trim()
if ($LASTEXITCODE -ne 0 -or [string]::IsNullOrWhiteSpace($currentBranch) -or $currentBranch -eq "HEAD") {
    Write-Error "Could not detect current git branch (detached HEAD?)."
    exit 1
}
if ([string]::IsNullOrWhiteSpace($Branch)) {
    $Branch = $currentBranch
}

# --------------------------------------------------------------------------
# 2. Read current version from app/build.gradle.kts
# --------------------------------------------------------------------------
$gradleFile = Join-Path $rootDir "app\build.gradle.kts"
if (-not (Test-Path $gradleFile)) {
    Write-Error "Cannot find app build file at $gradleFile"
    exit 1
}

$gradleContent = [System.IO.File]::ReadAllText($gradleFile)
$currentCodeMatch = [regex]::Match($gradleContent, '(?m)^\s*versionCode\s*=\s*(\d+)')
$currentNameMatch = [regex]::Match($gradleContent, '(?m)^\s*versionName\s*=\s*"([^"]+)"')

if (-not $currentCodeMatch.Success -or -not $currentNameMatch.Success) {
    Write-Error "Failed to parse versionCode or versionName from $gradleFile"
    exit 1
}

$currentVersionCode = [int]$currentCodeMatch.Groups[1].Value
$currentVersionName = $currentNameMatch.Groups[1].Value

Write-Host "`nCurrent Branch  : $currentBranch" -ForegroundColor Yellow
Write-Host "Current Version : v$currentVersionName (versionCode: $currentVersionCode)" -ForegroundColor Yellow

# --------------------------------------------------------------------------
# 3. Interactive prompts when run with no arguments
# --------------------------------------------------------------------------
$isInteractive = ($PSBoundParameters.Count -eq 0)
$waitForRun = -not $NoWait

if ($isInteractive) {
    Write-Host ""
    $typeChoice = Read-Host "Build type — [p] prodRelease, [d] devDebug, [pb] prodBenchmark, [dr] devRelease (default: p)"
    $Variant = switch -Regex ($typeChoice.Trim().ToLower()) {
        "^(d|dev|devdebug)$"                 { "devDebug" }
        "^(pb|prodbenchmark|benchmark)$"     { "prodBenchmark" }
        "^(dr|devrelease)$"                  { "devRelease" }
        "^(db|devbenchmark)$"                { "devBenchmark" }
        default                              { "prodRelease" }
    }

    $verInput = Read-Host "Version number (current: $currentVersionName, Enter to auto-bump, [k] to keep)"
    if (-not [string]::IsNullOrWhiteSpace($verInput)) {
        $trimmedVer = $verInput.Trim()
        if ($trimmedVer.ToLower() -eq "k") {
            $ExplicitVersionName = $currentVersionName
        } else {
            $ExplicitVersionName = $trimmedVer
        }
    }

    $codeInput = Read-Host "Version code (current: $currentVersionCode, Enter to auto-bump, [k] to keep)"
    if (-not [string]::IsNullOrWhiteSpace($codeInput)) {
        $trimmedCode = $codeInput.Trim()
        if ($trimmedCode.ToLower() -eq "k") {
            $ExplicitVersionCode = $currentVersionCode
        } else {
            $parsedCode = 0
            if ([int]::TryParse($trimmedCode, [ref]$parsedCode) -and $parsedCode -gt 0) {
                $ExplicitVersionCode = $parsedCode
            } else {
                Write-Warning "Invalid version code '$codeInput'; falling back to auto-bump."
            }
        }
    }

    $abiInput = Read-Host "Download ABI filter (e.g. x8a, v7a, x64) [optional, Enter for all]"
    if (-not [string]::IsNullOrWhiteSpace($abiInput)) {
        $Abi = $abiInput.Trim()
    }

    $branchInput = Read-Host "Git branch to build on GitHub (Enter for '$Branch')"
    if (-not [string]::IsNullOrWhiteSpace($branchInput)) {
        $Branch = $branchInput.Trim()
    }

    $watchInput = Read-Host "Watch build & auto-download APKs when done? [Y/n] (default: Y)"
    if ($watchInput.Trim().ToLower() -match "^(n|no)$") {
        $waitForRun = $false
    }
}

$resolvedAbiFilter = ""
if ($Abi) {
    $resolvedAbiFilter = switch -Regex ($Abi.ToLower().Trim()) {
        "^(x8a|arm64|arm64-v8a|aarch64)$" { "arm64-v8a" }
        "^(v7a|arm|armeabi-v7a|armv7)$"    { "armeabi-v7a" }
        "^(x64|x86_64|amd64)$"             { "x86_64" }
        "^(uni|universal)$"                { "universal" }
        default                            { $Abi.Trim() }
    }
}

# --------------------------------------------------------------------------
# 4. Calculate new versionCode & versionName
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
    $parts = $currentVersionName.Split('.')
    $major = if ($parts.Length -ge 1 -and [int]::TryParse($parts[0], [ref]$null)) { [int]$parts[0] } else { 1 }
    $minor = if ($parts.Length -ge 2 -and [int]::TryParse($parts[1], [ref]$null)) { [int]$parts[1] } else { 0 }
    $patch = if ($parts.Length -ge 3 -and [int]::TryParse($parts[2], [ref]$null)) { [int]$parts[2] } else { 0 }

    switch ($effectiveBump) {
        "patch" {
            if ($parts.Length -eq 2) {
                $newVersionName = "$major.$minor.1"
            } else {
                $newVersionName = "$major.$minor.$($patch + 1)"
            }
        }
        "minor" { $newVersionName = "$major.$($minor + 1).0" }
        "major" { $newVersionName = "$($major + 1).0.0" }
    }
}

Write-Host "`nTarget Build Plan:" -ForegroundColor Green
Write-Host "  Branch       : $Branch" -ForegroundColor Green
Write-Host "  Variant      : $Variant" -ForegroundColor Green
Write-Host "  versionCode  : $newVersionCode $(if ($newVersionCode -ne $currentVersionCode) { '(BUMPED)' } else { '(Unchanged)' })" -ForegroundColor Green
Write-Host "  versionName  : $newVersionName $(if ($newVersionName -ne $currentVersionName) { '(BUMPED)' } else { '(Unchanged)' })" -ForegroundColor Green
if ($resolvedAbiFilter) {
    Write-Host "  ABI Archive  : $resolvedAbiFilter only (cloud still builds variant splits)" -ForegroundColor Green
}

# --------------------------------------------------------------------------
# 5. Update app/build.gradle.kts if version changed
# --------------------------------------------------------------------------
$originalGradleContent = $gradleContent
$versionChanged = ($newVersionCode -ne $currentVersionCode -or $newVersionName -ne $currentVersionName)

if ($versionChanged) {
    $updatedContent = [regex]::Replace($gradleContent, '(?m)^(\s*versionCode\s*=\s*)\d+', "`${1}$newVersionCode")
    $updatedContent = [regex]::Replace($updatedContent, '(?m)^(\s*versionName\s*=\s*)"[^"]+"', "`${1}`"$newVersionName`"")
    [System.IO.File]::WriteAllText($gradleFile, $updatedContent)
    Write-Host "`nUpdated app/build.gradle.kts to v$newVersionName (vc$newVersionCode)." -ForegroundColor Cyan
}

# --------------------------------------------------------------------------
# 6. Ensure target branch on GitHub has the workflow & latest local changes
# --------------------------------------------------------------------------
if ($Branch -eq $currentBranch) {
    # Ensure .github/workflows/android-build.yml exists on this branch too
    $workflowLocal = Join-Path $rootDir ".github\workflows\android-build.yml"
    if (-not (Test-Path $workflowLocal)) {
        Write-Host "Workflow file missing on '$Branch'; copying from origin/main..." -ForegroundColor Yellow
        & git fetch origin main --quiet
        & git checkout origin/main -- .github/workflows/android-build.yml
    }

    $dirtyLines = @(& git status --porcelain)
    if ($dirtyLines.Count -gt 0) {
        Write-Host "`nGitHub Actions only builds code pushed to 'origin/$Branch'." -ForegroundColor Yellow
        Write-Host "You have $($dirtyLines.Count) uncommitted change(s) on '$Branch'." -ForegroundColor Yellow
        $pushChoice = Read-Host "Commit & push all local changes to 'origin/$Branch' now? [Y/n] (default: Y)"
        if ($pushChoice.Trim().ToLower() -match "^(n|no)$") {
            if ($versionChanged) {
                $revertChoice = Read-Host "Revert local version bump in app/build.gradle.kts? [Y/n] (default: Y)"
                if (-not ($revertChoice.Trim().ToLower() -match "^(n|no)$")) {
                    [System.IO.File]::WriteAllText($gradleFile, $originalGradleContent)
                    Write-Host "Reverted app/build.gradle.kts." -ForegroundColor Yellow
                }
            }
            Write-Host "Continuing without pushing (GitHub will build existing remote 'origin/$Branch')." -ForegroundColor Gray
        } else {
            $defaultMsg = "chore(build): $Variant v$newVersionName (vc$newVersionCode)"
            $msgInput = Read-Host "Commit message (Enter for '$defaultMsg')"
            $commitMsg = if ([string]::IsNullOrWhiteSpace($msgInput)) { $defaultMsg } else { $msgInput.Trim() }

            & git add -A
            if ($LASTEXITCODE -ne 0) {
                if ($versionChanged) { [System.IO.File]::WriteAllText($gradleFile, $originalGradleContent) }
                Write-Error "git add failed."
                exit 1
            }

            & git commit -m $commitMsg
            if ($LASTEXITCODE -ne 0) {
                if ($versionChanged) { [System.IO.File]::WriteAllText($gradleFile, $originalGradleContent) }
                Write-Error "git commit failed."
                exit 1
            }

            Write-Host "Pushing '$Branch' to origin..." -ForegroundColor Cyan
            & git push -u origin $Branch
            if ($LASTEXITCODE -ne 0) {
                Write-Error "git push failed."
                exit 1
            }
        }
    } else {
        # No dirty files, but check if local branch is ahead of origin/$Branch
        $remoteRef = (& git ls-remote --heads origin $Branch)
        if (-not $remoteRef) {
            Write-Host "Pushing new branch '$Branch' to origin..." -ForegroundColor Cyan
            & git push -u origin $Branch
            if ($LASTEXITCODE -ne 0) { exit 1 }
        } else {
            $aheadCount = (& git rev-list --count "origin/$Branch..$Branch" 2>$null)
            if ($aheadCount -and [int]$aheadCount -gt 0) {
                $pushAhead = Read-Host "Local '$Branch' is ahead of origin/$Branch by $aheadCount commit(s). Push now? [Y/n] (default: Y)"
                if (-not ($pushAhead.Trim().ToLower() -match "^(n|no)$")) {
                    & git push origin $Branch
                    if ($LASTEXITCODE -ne 0) { exit 1 }
                }
            }
        }
    }
} else {
    Write-Host "`nBuilding remote branch '$Branch' directly (local workspace is on '$currentBranch')." -ForegroundColor Gray
}

# --------------------------------------------------------------------------
# 7. Trigger GitHub Actions workflow
# --------------------------------------------------------------------------
$beforeIds = @()
try {
    $beforeJson = & gh run list --workflow=android-build.yml --limit 10 --json databaseId 2>$null
    if ($LASTEXITCODE -eq 0 -and $beforeJson) {
        $beforeIds = @($beforeJson | ConvertFrom-Json | ForEach-Object { [long]$_.databaseId })
    }
} catch { }

Write-Host "`nTriggering GitHub Actions: gh workflow run android-build.yml --ref $Branch -f variant=$Variant -f retention_days=$RetentionDays" -ForegroundColor Cyan
& gh workflow run android-build.yml --ref $Branch -f "variant=$Variant" -f "retention_days=$RetentionDays"
if ($LASTEXITCODE -ne 0) {
    Write-Error "Failed to trigger workflow on branch '$Branch'. Make sure .github/workflows/android-build.yml exists on both 'main' and '$Branch'."
    exit 1
}

# Locate the newly created run ID
$runId = $null
$runUrl = $null
Write-Host "Waiting for GitHub to register the workflow run..." -ForegroundColor Gray
for ($attempt = 0; $attempt -lt 15; $attempt++) {
    Start-Sleep -Seconds 2
    try {
        $runsJson = & gh run list --workflow=android-build.yml --branch $Branch --event workflow_dispatch --limit 5 --json databaseId,url,status 2>$null
        if ($LASTEXITCODE -eq 0 -and $runsJson) {
            $runs = @($runsJson | ConvertFrom-Json)
            $newRun = $runs | Where-Object { $beforeIds -notcontains [long]$_.databaseId } | Select-Object -First 1
            if ($newRun) {
                $runId = [long]$newRun.databaseId
                $runUrl = $newRun.url
                break
            }
        }
    } catch { }
}

if (-not $runId) {
    Write-Host "`nWorkflow triggered! Could not auto-detect run ID yet." -ForegroundColor Yellow
    Write-Host "Check status with: gh run list --workflow=android-build.yml" -ForegroundColor Cyan
    exit 0
}

Write-Host "Run ID  : $runId" -ForegroundColor Green
Write-Host "Run URL : $runUrl" -ForegroundColor Gray

if (-not $waitForRun) {
    Write-Host "`nBuild is running on GitHub! Useful commands:" -ForegroundColor Cyan
    Write-Host "  Watch    : gh run watch $runId" -ForegroundColor White
    Write-Host "  Download : gh run download $runId -D version\Android\cloud-temp" -ForegroundColor White
    Write-Host "  Errors   : gh run view $runId --log-failed" -ForegroundColor White
    exit 0
}

# --------------------------------------------------------------------------
# 8. Watch workflow run
# --------------------------------------------------------------------------
Write-Host "`nWatching cloud build (Ctrl+C only stops watching; build continues on GitHub)..." -ForegroundColor Cyan
& gh run watch $runId --exit-status
$watchExit = $LASTEXITCODE

if ($watchExit -ne 0) {
    Write-Host "`n[ERROR] Cloud build failed (run $runId)!" -ForegroundColor Red
    Write-Host "View failed logs: gh run view $runId --log-failed" -ForegroundColor Yellow
    exit $watchExit
}

# --------------------------------------------------------------------------
# 9. Download APKs & archive into version/Android/<flavor>/
# --------------------------------------------------------------------------
$flavor = if ($Variant.StartsWith("dev")) { "dev" } else { "prod" }
$targetDir = if ($DestinationDir -eq "version/Android" -or $DestinationDir -eq "version\Android") {
    Join-Path $rootDir "$DestinationDir\$flavor"
} else {
    Join-Path $rootDir $DestinationDir
}
if (-not (Test-Path $targetDir)) {
    New-Item -ItemType Directory -Path $targetDir -Force | Out-Null
}

$tempDownloadDir = Join-Path ([System.IO.Path]::GetTempPath()) "chordplay-gh-$runId"
if (Test-Path $tempDownloadDir) {
    Remove-Item -Path $tempDownloadDir -Recurse -Force
}
New-Item -ItemType Directory -Path $tempDownloadDir -Force | Out-Null

Write-Host "`nDownloading APKs from run $runId..." -ForegroundColor Cyan
& gh run download $runId --dir $tempDownloadDir
if ($LASTEXITCODE -ne 0) {
    Write-Error "Failed to download artifacts for run $runId."
    exit 1
}

$downloadedApks = @(Get-ChildItem -Path $tempDownloadDir -Recurse -Filter "*.apk")
if ($resolvedAbiFilter -and $downloadedApks.Count -gt 0) {
    $filtered = @($downloadedApks | Where-Object { $_.Name -like "*$resolvedAbiFilter*" })
    if ($filtered.Count -gt 0) {
        $downloadedApks = $filtered
    } else {
        Write-Warning "No APK matched ABI '$resolvedAbiFilter'; archiving all downloaded APKs instead."
    }
}

if ($downloadedApks.Count -eq 0) {
    Write-Error "No .apk files found in downloaded artifact for run $runId."
    exit 1
}

Write-Host "`nArchiving APKs to: $targetDir" -ForegroundColor Cyan
$copiedFiles = @()
foreach ($apk in $downloadedApks) {
    $classifier = ""
    if ($apk.Name -match "app-(?:dev|prod)-(.+)-(?:debug|release|benchmark)\.apk") {
        $classifier = "-$($matches[1])"
    } elseif ($apk.Name -match "app-(?:dev|prod)-(?:debug|release|benchmark)\.apk") {
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
        Classifier = $classifier
    }

    if ($classifier -eq "-universal") {
        $latestName = "ChordPlay-latest-${Variant}.apk"
        Copy-Item -Path $apk.FullName -Destination (Join-Path $targetDir $latestName) -Force
    }
}

Remove-Item -Path $tempDownloadDir -Recurse -Force -ErrorAction SilentlyContinue

# Update VERSION_HISTORY.md & builds.json
$historyFile = Join-Path $targetDir "VERSION_HISTORY.md"
$timestamp = (Get-Date).ToString("yyyy-MM-dd HH:mm:ss")
$historyEntry = @"

### Build v$newVersionName (versionCode: $newVersionCode)
- **Timestamp**: $timestamp
- **Variant**: $Variant (Cloud · GitHub Actions run #$runId on ``$Branch``)
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
Write-Host "   CLOUD BUILD & ARCHIVE COMPLETE" -ForegroundColor Green
Write-Host "============================================================" -ForegroundColor Green
Write-Host "Version Code : $newVersionCode" -ForegroundColor White
Write-Host "Version Name : $newVersionName" -ForegroundColor White
Write-Host "Archived to  : $targetDir" -ForegroundColor White
Write-Host "============================================================`n" -ForegroundColor Green

# --------------------------------------------------------------------------
# 10. Optional adb install
# --------------------------------------------------------------------------
$adbPath = Get-AdbPath
if ($adbPath) {
    $preferredApk = $copiedFiles | Where-Object { $_.Classifier -eq "-arm64-v8a" } | Select-Object -First 1
    if (-not $preferredApk) { $preferredApk = $copiedFiles | Select-Object -First 1 }

    $installChoice = Read-Host "Install $($preferredApk.FileName) on connected device via adb? [y/N]"
    if ($installChoice.Trim().ToLower() -match "^(y|yes)$") {
        Write-Host "`nRunning: $adbPath install -r `"$($preferredApk.Path)`"" -ForegroundColor Cyan
        & $adbPath install -r $preferredApk.Path
        if ($LASTEXITCODE -eq 0) {
            Write-Host "`n[SUCCESS] Installed $($preferredApk.FileName)" -ForegroundColor Green
        } else {
            Write-Host "`n[ERROR] adb install failed with exit code $LASTEXITCODE" -ForegroundColor Red
            exit $LASTEXITCODE
        }
    }
}
