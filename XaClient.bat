@echo off
setlocal enabledelayedexpansion
title XaClient Launcher

set "REPO=Xani4kaGitHub/XaClient"

:: Enable ANSI Escape Codes
for /F %%a in ('echo prompt $E ^| cmd') do set "ESC=%%a"

cls
echo.
echo %ESC%[95m____  ___      _________ .__  .__               __
echo \   \/  /____  \_   ___ \^|  ^| ^|__^| ____   _____/  ^|_
echo  \     /\__  \ /    \  \/^|  ^| ^|  ^|/ __ \ /    \   __\
echo  /     \ / __ \\     \___^|  ^|_^|  \  ___/^|   ^|  \  ^|
echo /___/\  (____  /\______  /____/__^|\___  ^>___^|  /__^|
echo       \_/    \/        \/             \/     \/     %ESC%[0m
echo.

set /p nick="%ESC%[95m  >> %ESC%[97mEnter Nickname: %ESC%[0m"
set /p ram="%ESC%[95m  >> %ESC%[97mEnter RAM in MB (e.g. 4096): %ESC%[0m"
if "%nick%"=="" set "nick=Player"
if "%ram%"=="" set "ram=4096"

echo.
echo  %ESC%[95m[*]%ESC%[97m Preparing client (first launch downloads Minecraft, may take a while)...%ESC%[0m
echo.

set "NICK=%nick%"
set "RAM=%ram%"
pushd "%~dp0"
powershell -NoProfile -ExecutionPolicy Bypass -Command "& '.\XaLauncher.ps1'"
popd

echo.
pause
