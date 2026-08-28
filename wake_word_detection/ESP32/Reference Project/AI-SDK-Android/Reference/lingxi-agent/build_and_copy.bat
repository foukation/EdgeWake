@echo off
setlocal

echo ==========================================
echo Starting Release APK Build and Copy
echo ==========================================

REM Clean previous builds
echo 1. Cleaning old build files...
call gradlew.bat clean
if %errorlevel% neq 0 (
    echo ERROR: Clean failed! Please check error messages.
    pause
    exit /b 1
)

REM Build Release APK
echo 2. Building Release APK...
call gradlew.bat assembleRelease
if %errorlevel% neq 0 (
    echo ERROR: Build failed! Please check error messages.
    pause
    exit /b 1
)

echo SUCCESS: Build completed!

REM Output directory (arg1 or env OUTPUT_DIR or default "release")
if not "%1"=="" (
  set "OUTPUT_DIR=%~1"
) else (
  if "%OUTPUT_DIR%"=="" set "OUTPUT_DIR=release"
)
if not exist "%OUTPUT_DIR%" mkdir "%OUTPUT_DIR%"

echo.
echo 3. Searching for Release APKs...

REM Collect release APKs under app\build\outputs\apk\*\release\*.apk, excluding unsigned/unaligned
setlocal enabledelayedexpansion
set "APK_LIST="
for /f "delims=" %%F in ('dir /s /b app\build\outputs\apk\*\release\*.apk ^| findstr /vi "unsigned unaligned"') do (
  set "APK_LIST=!APK_LIST!;%%F"
)

if "%APK_LIST%"=="" (
  echo ERROR: No release APK found under app\build\outputs\apk\*\release\*.apk
  echo Please check your variant/flavor output paths.
  pause
  exit /b 1
)

echo Found APKs:
for %%P in (%APK_LIST:;= %) do (
  if not "%%P"=="" echo   - %%P
)

echo.
echo 4. Copying APKs to "%OUTPUT_DIR%" ...
set "COPIED_COUNT=0"
for %%P in (%APK_LIST:;= %) do (
  if not "%%P"=="" (
    for %%A in ("%%P") do set "BASENAME=%%~nxA"
    copy /y "%%P" "%OUTPUT_DIR%\%BASENAME%" >nul
    if %errorlevel% neq 0 (
      echo ERROR: Copy failed: %%P
      pause
      exit /b 1
    ) else (
      echo   Copied: %OUTPUT_DIR%\%BASENAME%
      set /a COPIED_COUNT+=1
    )
  )
)

echo.
echo 5. Creating README file...
(
  echo Release APK File List:
  echo ========================
  echo Build time: %date% %time%
  echo Output dir: %OUTPUT_DIR%
  echo.
  for %%P in (%APK_LIST:;= %) do (
    if not "%%P"=="" (
      for %%A in ("%%P") do (
        set "BASENAME=%%~nxA"
        set "FULLPATH=%OUTPUT_DIR%\!BASENAME!"
        call :__file_size "!FULLPATH!" SIZE
        echo File: !BASENAME! ^(Size: !SIZE!^)
      )
    )
  )
  echo.
  echo Package: com.fxzs.lingxiagent
  echo Signature: Release signature ^(yidiong.jks^)
) > "%OUTPUT_DIR%\README.txt"

echo.
echo ==========================================
echo Release APK Build Completed Successfully!
echo ==========================================
echo Output dir: %OUTPUT_DIR%
echo Files copied: %COPIED_COUNT%
echo See: %OUTPUT_DIR%\README.txt
echo.
pause
exit /b 0

:__file_size
setlocal
set "_f=%~1"
for %%A in ("%_f%") do (
  set "_bytes=%%~zA"
)
set /a _mb=_bytes/1024/1024
endlocal & set "%2=%_mb% MB"
goto :eof
