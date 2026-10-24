[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12

$token = "protocol=https`nhost=github.com" | git credential fill | Select-String "password=(.*)" | ForEach-Object { $_.Matches.Groups[1].Value }
if (-not $token) {
    Write-Error "Could not retrieve GitHub token from git credentials."
    exit 1
}

$repoOwner = "code4nigel"
$repoName = "ChordPlay"
$tagName = "v1.8.2"
$releaseTitle = "ChordPlay v1.8.2 - Modular Settings, Custom Typography and Last.fm"
$notesPath = Join-Path $PSScriptRoot "..\app\version\RELEASE_NOTES.md"
$releaseNotes = if (Test-Path $notesPath) { [System.IO.File]::ReadAllText($notesPath) } else { "ChordPlay v1.8.2 release" }

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

# 2. Upload APK files from app/version
$apkFiles = @(
    (Join-Path $PSScriptRoot "..\app\version\chordplay-v1.8.2-build28-devDebug-universal.apk"),
    (Join-Path $PSScriptRoot "..\app\version\chordplay-v1.8.2-build28-devDebug-arm64.apk")
)

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
