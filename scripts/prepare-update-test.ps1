# Primes a connected device with the previous stable release so the in-app
# updater has something to offer when the app is opened, and keeps a copy of
# the latest release APK.
#
# Why a clean reinstall instead of a downgrade:
#   - `adb install -d` only allows a versionCode downgrade for debuggable
#     builds, and the release APK is not debuggable.
#   - The launch check is throttled by the last successful check stored in the
#     app's private preferences. A release build cannot be inspected with
#     `run-as`, so the only reliable reset is starting from a clean install.
#   - App data (theme, price alerts) is therefore reset. That is the cost of
#     reproducing the first-launch update dialog against the real release.
#
# Caveat: the previous release must already contain the updater. The 1.30.0
# release predates it, so 1.31.0 can only be demoed with a build of the current
# code carrying an older versionName; from 1.32.0 onward this script alone is
# enough.
#
# Usage:
#   powershell -ExecutionPolicy Bypass -File scripts/prepare-update-test.ps1
#   powershell -ExecutionPolicy Bypass -File scripts/prepare-update-test.ps1 -KeepDir D:\apks
param(
    [string]$Repo = "YourNelvi/Dolita",
    [string]$Package = "com.example.erp",
    [string]$Adb = "",
    [string]$KeepDir = ""
)

$ErrorActionPreference = "Stop"

if (-not $Adb) {
    $candidates = @()
    if ($env:LOCALAPPDATA) {
        $candidates += (Join-Path $env:LOCALAPPDATA "Android\Sdk\platform-tools\adb.exe")
    }
    if ($env:ANDROID_HOME) {
        $candidates += (Join-Path $env:ANDROID_HOME "platform-tools\adb.exe")
    }
    if ($env:ANDROID_SDK_ROOT) {
        $candidates += (Join-Path $env:ANDROID_SDK_ROOT "platform-tools\adb.exe")
    }
    $Adb = $candidates | Where-Object { Test-Path $_ } | Select-Object -First 1
}

if (-not $Adb -or -not (Test-Path $Adb)) {
    throw "adb not found. Pass -Adb <path to adb.exe>."
}

# ConvertFrom-Json on Windows PowerShell emits the whole array as one object;
# assigning it first is what lets the later cmdlets see the releases one by one.
$parsed = gh release list --repo $Repo --limit 20 --json tagName,isDraft,isPrerelease,publishedAt |
    ConvertFrom-Json
$releases = @(
    $parsed |
        Where-Object { -not $_.isDraft -and -not $_.isPrerelease } |
        Sort-Object { [datetime]$_.publishedAt } -Descending
)

if ($releases.Count -lt 2) {
    throw "At least one published release before the latest is required."
}

$latest = $releases[0]
$previous = $releases[1]

if (-not $KeepDir) {
    if ($env:USERPROFILE) {
        $KeepDir = Join-Path $env:USERPROFILE "Downloads"
    } else {
        $KeepDir = [System.IO.Path]::GetTempPath()
    }
}

$latestAssets = gh release view $latest.tagName --repo $Repo --json assets | ConvertFrom-Json
$latestApkName = ($latestAssets.assets | Where-Object { $_.name -like "*.apk" } | Select-Object -First 1).name
if (-not $latestApkName) {
    throw "Release $($latest.tagName) has no APK asset."
}

New-Item -ItemType Directory -Force -Path $KeepDir | Out-Null
gh release download $latest.tagName --repo $Repo --pattern $latestApkName --dir $KeepDir --clobber | Out-Null

Write-Host "Latest release:   $($latest.tagName)"
Write-Host "Saved APK:        $(Join-Path $KeepDir $latestApkName)"
Write-Host "Installing:       $($previous.tagName) (so $($latest.tagName) is offered)"

$directory = Join-Path ([System.IO.Path]::GetTempPath()) ("dolita-update-test-" + [guid]::NewGuid().ToString("N"))
New-Item -ItemType Directory -Path $directory | Out-Null

try {
    gh release download $previous.tagName --repo $Repo --pattern "*.apk" --dir $directory | Out-Null
    $apk = Get-ChildItem $directory -Filter *.apk | Select-Object -First 1
    if (-not $apk) {
        throw "Release $($previous.tagName) has no APK asset."
    }

    # A missing app is not an error here, and a success is expected.
    & $Adb uninstall $Package | Out-Null

    & $Adb install $apk.FullName
    if ($LASTEXITCODE -ne 0) {
        throw "adb install failed. Is the device connected and unlocked?"
    }

    & $Adb shell am start -n "$Package/.MainActivity" | Out-Null

    Write-Host ""
    Write-Host "Ready. The app should offer $($latest.tagName) on its own."
    Write-Host "App data was reset by the clean install."
} finally {
    Remove-Item -Recurse -Force $directory -ErrorAction SilentlyContinue
}
