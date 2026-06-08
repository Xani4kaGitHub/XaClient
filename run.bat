@echo off
title XaClient Launcher

:: Enable ANSI Escape Codes
for /F %%a in ('echo prompt $E ^| cmd') do set "ESC=%%a"

echo.
echo %ESC%[95m____  ___      _________ .__  .__               __   
echo \   \/  /____  \_   ___ \^|  ^| ^|__^| ____   _____/  ^|_ 
echo  \     /\__  \ /    \  \/^|  ^| ^|  ^|/ __ \ /    \   __\
echo  /     \ / __ \\     \___^|  ^|_^|  \  ___/^|   ^|  \  ^|  
echo /___/\  (____  /\______  /____/__^|\___  ^>___^|  /__^|  
echo       \_/    \/        \/             \/     \/     %ESC%[0m
echo.
echo  %ESC%[95m::%ESC%[97m Launching development client...
echo.

set /p nick="%ESC%[95m» %ESC%[97mEnter Nickname: %ESC%[0m"
set /p ram="%ESC%[95m» %ESC%[97mEnter RAM in MB (e.g. 4096): %ESC%[0m"

echo.
echo  %ESC%[95m» %ESC%[97mUsername:      %ESC%[37m%nick%%ESC%[0m
echo  %ESC%[95m» %ESC%[97mAllocated RAM: %ESC%[37m%ram% MB%ESC%[0m
echo.
echo  %ESC%[95m[*]%ESC%[97m Building and running client...
echo.

call gradlew.bat runClient -Pusername="%nick%" -Pram="%ram%"

pause
