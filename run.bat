@echo off
title RSA System
echo Starting RSA System...
echo.

REM Copy OpenSSL DLLs if not already there
if not exist "%~dp0cpp_server\build\libcrypto-3-x64.dll" (
    copy "C:\msys64\mingw64\bin\libcrypto-3-x64.dll" "%~dp0cpp_server\build\" > nul 2>&1
    copy "C:\msys64\mingw64\bin\libssl-3-x64.dll" "%~dp0cpp_server\build\" > nul 2>&1
)

REM Start C++ server
echo [1/2] Starting C++ RSA Server on port 9000...
start "RSA C++ Server" /min "%~dp0cpp_server\build\rsa_server.exe"
timeout /t 2 > nul

REM Build Java if bin does not exist
if not exist "%~dp0java_app\bin" (
    echo [1.5/2] Building Java app...
    mkdir "%~dp0java_app\bin"
    javac -encoding UTF-8 -d "%~dp0java_app\bin" "%~dp0java_app\src\rsa\*.java"
    if errorlevel 1 (
        echo [ERROR] Java build failed!
        pause & exit /b 1
    )
)

REM Launch Java UI
echo [2/2] Launching Java UI...
cd /d "%~dp0java_app"
java -cp bin rsa.AppFrame

echo.
echo Java app closed.
pause