@echo off
setlocal
title UTA-WAVE Desktop Runner

echo ==========================================================
echo          UTA-WAVE - Windows Desktop Runner
echo ==========================================================
echo.

set "REPO_DIR=%~dp0"
cd /d "%REPO_DIR%"

set "JAR_PATH=%REPO_DIR%desktopApp\build\compose\jars\UTA-wave-windows-x64-1.0.0.jar"

if "%1"=="--dev" goto RUN_GRADLE
if "%1"=="--build" goto BUILD_JAR

if exist "%JAR_PATH%" (
    echo [UTA-WAVE] Launching pre-packaged standalone desktop client...
    start "" javaw -jar "%JAR_PATH%"
    if %ERRORLEVEL% EQU 0 (
        echo [UTA-WAVE] Application launched successfully.
        exit /b 0
    )
    echo [UTA-WAVE] javaw detached launch failed, running in current console...
    java -jar "%JAR_PATH%"
    exit /b 0
)

:BUILD_JAR
echo [UTA-WAVE] Standalone bundle not found. Building with Gradle...
call gradlew.bat :desktopApp:packageUberJarForCurrentOS
if %ERRORLEVEL% NEQ 0 (
    echo [UTA-WAVE] Build failed.
    pause
    exit /b %ERRORLEVEL%
)

if exist "%JAR_PATH%" (
    echo [UTA-WAVE] Launching freshly built standalone desktop client...
    start "" javaw -jar "%JAR_PATH%"
    exit /b 0
)

:RUN_GRADLE
echo [UTA-WAVE] Launching in developer mode via Gradle :desktopApp:run...
call gradlew.bat :desktopApp:run
