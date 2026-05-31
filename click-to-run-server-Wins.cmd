@echo off
setlocal enabledelayedexpansion
cd /d "%~dp0"

REM Auto-detect JAVA_HOME if not set
if "!JAVA_HOME!"=="" (
    if exist "C:\Program Files\Java\latest\jdk-25" (
        set "JAVA_HOME=C:\Program Files\Java\latest\jdk-25"
    ) else if exist "C:\Program Files\Java\jdk-25" (
        set "JAVA_HOME=C:\Program Files\Java\jdk-25"
    ) else if exist "C:\Program Files\Eclipse Adoptium\jdk-25.0.2.10-hotspot" (
        set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-25.0.2.10-hotspot"
    )
)

cls
echo ====================================================
echo  Auction Bidding System - Server
echo ====================================================
echo.

REM Build (only if JAR doesn't exist)
echo.
if exist "target\BiddingSystem-server.jar" (
    echo ===== JAR exists, skipping build =====
) else (
    echo ===== Building project (first run) =====
    if exist "mvnw.cmd" (
        call mvnw.cmd -q -DskipTests package
    ) else (
        mvn -q -DskipTests package
    )
    if errorlevel 1 (
        echo BUILD FAILED!
        pause
        exit /b 1
    )
    if not exist "target\BiddingSystem-server.jar" (
        echo ERROR: Khong tim thay target\BiddingSystem-server.jar
        pause
        exit /b 1
    )
)

echo.
echo ===== Khoi dong Server tai port 9999 =====
echo Nhan Ctrl+C de dung Server
echo.
java -jar "target\BiddingSystem-server.jar"

pause
endlocal
