param(
    [string]$tagName = "v1.8.8",
    [string]$releaseTitle = "ChordPlay v1.8.8"
)

[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12

$credInput = @("protocol=https", "host=github.com", "")
$token = ($credInput | git credential fill | Select-String "password=(.*)").Matches.Groups[1].Value
if (-not $token) {
    Write-Error "Could not retrieve GitHub token from git credentials."
    exit 1
}

$repoOwner = "code4nigel"
$repoName = "ChordPlay"
$notesPath = Join-Path $PSScriptRoot "..\app\version\RELEASE_NOTES.md"
$fullNotes = if (Test-Path $notesPath) { [System.IO.File]::ReadAllText($notesPath) } else { "ChordPlay $tagName release" }
$releaseNotes = if ($fullNotes -match '(?s)(# ChordPlay v1\.8\.8.*?)(?=\r?\n---\r?\n# ChordPlay|\Z)') {
    $matches[1].Trim()
} else {
    $fullNotes
}

$headers = @{
    "Authorization" = "Bearer $token"
    "Accept"        = "application/vnd.github+json"
    "User-Agent"    = "ChordPlay-Release-Script"
}

# 1. Check if release already exists
$release = $null
try {
    $release = Invoke-RestMethod -Uri "https://api.github.com/repos/$repoOwner/$repoName/releases/tags/$tagName" -Headers $headers -Method Get
    Write-Host "Found existing release for $tagName (ID: $($release.id))"
} catch {
    Write-Host "No release exists yet for tag $tagName. Creating new release..."
}

if (-not $release) {
    $createBody = @{
        tag_name         = $tagName
        target_commitish = "main"
        name             = $releaseTitle
        body             = $releaseNotes
        draft            = $false
        prerelease       = $false
    } | ConvertTo-Json

    $release = Invoke-RestMethod -Uri "https://api.github.com/repos/$repoOwner/$repoName/releases" -Headers $headers -Method Post -Body $createBody
    Write-Host "Created release $tagName with ID $($release.id)"
}

Write-Host "Release HTML URL: $($release.html_url)"
Write-Host "Upload URL: $($release.upload_url)"

# 2. Verify APK signing certificates against permanent repository keystore
# Note for existing test devices:
# A one-time uninstall of the older test build on Tecno LC8 is required so it adopts the permanent keystore.
# Every future release APK downloaded via GitHub will then update in-place with zero signature mismatches.
Write-Host "Verifying APK signing certificates against permanent repository keystore..." -ForegroundColor Cyan

$apkFiles = @(
    (Join-Path $PSScriptRoot "..\app\version\chordplay-v1.8.8-build34-devDebug-universal.apk"),
    (Join-Path $PSScriptRoot "..\app\version\chordplay-v1.8.8-build34-devDebug-arm64.apk"),
    (Join-Path $PSScriptRoot "..\app\version\chordplay-v1.8.8-build34-devDebug.apk")
)

$localApp = if ($env:LOCALAPPDATA) { $env:LOCALAPPDATA } else { "" }
$apksignerCandidates = @(
    (Join-Path $localApp "Android\Sdk\build-tools\36.0.0\apksigner.bat"),
    (Join-Path $localApp "Android\Sdk\build-tools\35.0.0\apksigner.bat"),
    (Join-Path $localApp "Android\Sdk\build-tools\34.0.0\apksigner.bat"),
    "C:\Users\Pinea\AppData\Local\Android\Sdk\build-tools\36.0.0\apksigner.bat",
    "C:\Users\Pinea\AppData\Local\Android\Sdk\build-tools\35.0.0\apksigner.bat",
    "C:\Users\Pinea\AppData\Local\Android\Sdk\build-tools\34.0.0\apksigner.bat"
)
$apksigner = $apksignerCandidates | Where-Object { Test-Path $_ } | Select-Object -First 1

foreach ($apkPath in $apkFiles) {
    if (-not (Test-Path $apkPath)) {
        Write-Warning "File not found: $apkPath"
        continue
    }
    if ($apksigner) {
        $sigOutput = & $apksigner verify --print-certs "$apkPath" 2>&1 | Out-String
        if ($sigOutput -match "(?i)4e:?60:?01:?ab:?3c:?84:?25:?a8:?5c:?84:?a0:?13:?ec:?2a:?88:?3a:?f1:?f1:?9a:?7a:?fa:?54:?24:?6a:?80:?21:?eb:?fe:?75:?1e:?09:?83") {
            Write-Host "Verified $([System.IO.Path]::GetFileName($apkPath)): Signed with permanent ChordPlay repository keystore." -ForegroundColor Green
        } else {
            Write-Warning "APK $([System.IO.Path]::GetFileName($apkPath)) signing certificate does not match expected permanent keystore fingerprint."
        }
    } else {
        Write-Host "apksigner tool not located; skipping direct fingerprint verification for $([System.IO.Path]::GetFileName($apkPath))."
    }
}

Write-Host "Note for existing test devices: A one-time uninstall of the older test build on Tecno LC8 is required so it adopts the permanent keystore. Every future release APK downloaded via GitHub will then update in-place with zero signature mismatches." -ForegroundColor Yellow

# 3. Upload APK files from app/version

$uploadBase = $release.upload_url -replace '\{\?name,label\}', ''

Add-Type -AssemblyName System.Net.Http
$httpClient = [System.Net.Http.HttpClient]::new()
$httpClient.DefaultRequestHeaders.Authorization = [System.Net.Http.Headers.AuthenticationHeaderValue]::new("Bearer", $token)
$httpClient.DefaultRequestHeaders.Add("Accept", "application/vnd.github+json")
$httpClient.DefaultRequestHeaders.Add("User-Agent", "ChordPlay-Release-Script")
$httpClient.Timeout = [TimeSpan]::FromMinutes(15)

foreach ($apkPath in $apkFiles) {
    if (-not (Test-Path $apkPath)) {
        Write-Warning "File not found: $apkPath"
        continue
    }

    $fileName = [System.IO.Path]::GetFileName($apkPath)
    $fileInfo = Get-Item $apkPath
    $fileSizeMB = [Math]::Round($fileInfo.Length / 1MB, 2)
    Write-Host "Preparing to upload $fileName ($fileSizeMB MB)..."

    # Check if asset already exists in the release
    $currentRelease = Invoke-RestMethod -Uri "https://api.github.com/repos/$repoOwner/$repoName/releases/$($release.id)" -Headers $headers
    $existingAsset = $currentRelease.assets | Where-Object { $_.name -eq $fileName }
    if ($existingAsset) {
        Write-Host "Asset $fileName already exists (ID: $($existingAsset.id)). Deleting previous asset first..."
        $deleteUrl = "https://api.github.com/repos/$repoOwner/$repoName/releases/assets/$($existingAsset.id)"
        Invoke-RestMethod -Uri $deleteUrl -Headers $headers -Method Delete
        Start-Sleep -Seconds 2
    }

    $uploadUrl = "$uploadBase`?name=$fileName"
    Write-Host "Uploading to: $uploadUrl"

    $fileStream = [System.IO.File]::OpenRead($apkPath)
    try {
        $streamContent = [System.Net.Http.StreamContent]::new($fileStream)
        $streamContent.Headers.ContentType = [System.Net.Http.Headers.MediaTypeHeaderValue]::Parse("application/vnd.android.package-archive")

        $response = $httpClient.PostAsync($uploadUrl, $streamContent).Result
        if ($response.IsSuccessStatusCode) {
            Write-Host "Successfully uploaded $fileName!"
        } else {
            $errText = $response.Content.ReadAsStringAsync().Result
            Write-Error "Failed to upload $fileName. Status: $($response.StatusCode) - $errText"
        }
    } finally {
        $fileStream.Dispose()
    }
}

$httpClient.Dispose()
Write-Host "All assets processed for release $tagName."
