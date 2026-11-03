# UTA-WAVE Windows Desktop Runner (PowerShell)
param(
    [switch]$Dev,
    [switch]$Build
)

$ErrorActionPreference = "Stop"
$RepoDir = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $RepoDir

$JarPath = Join-Path $RepoDir "desktopApp\build\compose\jars\UTA-wave-windows-x64-1.0.0.jar"

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "         UTA-WAVE - Windows Desktop Runner                " -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host ""

if ($Dev) {
    Write-Host "[UTA-WAVE] Launching in developer mode via Gradle :desktopApp:run..." -ForegroundColor Yellow
    & .\gradlew.bat :desktopApp:run
    exit $LASTEXITCODE
}

if ($Build -or (-not (Test-Path $JarPath))) {
    Write-Host "[UTA-WAVE] Packaging standalone desktop bundle with Gradle..." -ForegroundColor Yellow
    & .\gradlew.bat :desktopApp:packageUberJarForCurrentOS
    if ($LASTEXITCODE -ne 0) {
        Write-Error "[UTA-WAVE] Packaging failed with exit code $LASTEXITCODE."
        exit $LASTEXITCODE
    }
}

if (Test-Path $JarPath) {
    Write-Host "[UTA-WAVE] Launching native Windows desktop client..." -ForegroundColor Green
    Start-Process -FilePath "javaw" -ArgumentList "-jar", "`"$JarPath`""
    Write-Host "[UTA-WAVE] Window launched in background." -ForegroundColor Green
} else {
    Write-Error "[UTA-WAVE] Jar file not found at $JarPath."
}
