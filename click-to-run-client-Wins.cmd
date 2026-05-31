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
echo  Auction Bidding System - Client
echo ====================================================
echo.

REM Build (only if JAR doesn't exist)
echo.
if exist "target\BiddingSystem-client.jar" (
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
        echo Kiem tra JDK 25 va JAVA_HOME.
        pause
        exit /b 1
    )
    if not exist "target\BiddingSystem-client.jar" (
        echo ERROR: Khong tim thay target\BiddingSystem-client.jar
        pause
        exit /b 1
    )
)

REM Kiem tra IP (Da them co xoa sach WARNING)
set "JVM_OPTS=--enable-native-access=ALL-UNNAMED --add-opens=java.base/sun.misc=ALL-UNNAMED -Xlint:none"

if "!SERVER_IP!"=="" (
    echo.
    echo ===== Step 1: Khoi dong Server local =====
    start "BiddingServer" cmd /k "java !JVM_OPTS! -jar target\BiddingSystem-server.jar"
    timeout /t 3 /nobreak >nul
    echo.
    echo ===== Step 2: Khoi dong Client (localhost) =====
    java !JVM_OPTS! -jar "target\BiddingSystem-client.jar"
) else (
    echo.
    echo ===== Ket noi toi Server: !SERVER_IP! =====
    java !JVM_OPTS! -jar "target\BiddingSystem-client.jar" "!SERVER_IP!"
)

echo.
echo ===== Client da ket thuc =====
pause
endlocal
