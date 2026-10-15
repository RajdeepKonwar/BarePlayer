@echo off
rem Builds, installs and opens BarePlayer. Usage: launch.bat Debug^|Release
rem If several devices are connected, set ANDROID_SERIAL first (see "adb devices").
setlocal EnableDelayedExpansion

set "VARIANT=%~1"
if /i "%VARIANT%"=="Debug" goto :variant_ok
if /i "%VARIANT%"=="Release" goto :variant_ok
echo Usage: %~nx0 Debug^|Release
exit /b 1
:variant_ok

set "PACKAGE=com.rajdeepkonwar.bareplayer"
set "AVD=BarePlayer_API37"
cd /d "%~dp0.."

if not defined ANDROID_HOME set "ANDROID_HOME=%LOCALAPPDATA%\Android\Sdk"
set "ADB=%ANDROID_HOME%\platform-tools\adb.exe"
set "EMULATOR=%ANDROID_HOME%\emulator\emulator.exe"

if not defined JAVA_HOME (
    echo JAVA_HOME is not set. Install JDK 17+ and set JAVA_HOME, then try again.
    goto :fail
)
if not exist "%ADB%" (
    echo adb not found at "%ADB%". Set ANDROID_HOME to your Android SDK folder.
    goto :fail
)

"%ADB%" start-server >nul 2>&1

rem Count connected devices (lines whose state is "device", after the header).
set "COUNT=0"
for /f "skip=1 tokens=1,2" %%a in ('"%ADB%" devices') do (
    if "%%b"=="device" set /a COUNT+=1
)

if "%COUNT%"=="1" goto :install
if "%COUNT%"=="0" goto :start_emulator
if defined ANDROID_SERIAL goto :install
echo %COUNT% devices are connected. Pick one first, for example:
echo     set ANDROID_SERIAL=emulator-5554
"%ADB%" devices
goto :fail

:start_emulator
if not exist "%EMULATOR%" (
    echo No device connected and the emulator isn't installed.
    echo Connect a phone with USB debugging on, then try again.
    goto :fail
)
echo No device connected. Starting emulator %AVD%...
start "BarePlayer emulator" "%EMULATOR%" -avd %AVD% -no-boot-anim
"%ADB%" wait-for-device
echo Waiting for Android to finish booting...
:wait_boot
set "BOOTED="
for /f %%b in ('"%ADB%" shell getprop sys.boot_completed 2^>nul') do set "BOOTED=%%b"
if "!BOOTED!"=="1" goto :wait_package_manager
rem ~3 second pause; unlike "timeout", this also works when input is redirected.
ping -n 4 127.0.0.1 >nul
goto :wait_boot

rem After a quick-boot snapshot restore, installs can fail until the package manager is up.
:wait_package_manager
"%ADB%" shell pm path android >nul 2>&1
if not errorlevel 1 goto :install
ping -n 3 127.0.0.1 >nul
goto :wait_package_manager

:install
echo.
echo Building and installing the %VARIANT% build...
call gradlew.bat install%VARIANT% --console=plain
if errorlevel 1 (
    echo.
    echo Install failed. If the error mentions INSTALL_FAILED_UPDATE_INCOMPATIBLE, the installed
    echo app was signed with a different key. Uninstall it first ^(this clears playlists and settings^):
    echo     "%ADB%" uninstall %PACKAGE%
    goto :fail
)

echo.
echo Opening BarePlayer...
"%ADB%" shell am start -n %PACKAGE%/.MainActivity >nul 2>&1
if errorlevel 1 goto :fail
echo Done.
endlocal
exit /b 0

:fail
echo.
pause
endlocal
exit /b 1
