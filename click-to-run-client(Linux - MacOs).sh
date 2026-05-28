#!/bin/bash
# Auction Bidding System - Client (Linux/macOS)
cd "$(dirname "$0")"

# Auto-detect JAVA_HOME if not set
if [ -z "$JAVA_HOME" ]; then
    if [ -d "/usr/lib/jvm/java-25-openjdk" ]; then
        export JAVA_HOME="/usr/lib/jvm/java-25-openjdk"
    elif [ -d "/usr/lib/jvm/java-25-oracle" ]; then
        export JAVA_HOME="/usr/lib/jvm/java-25-oracle"
    elif [ -d "/usr/lib/jvm/jdk-25" ]; then
        export JAVA_HOME="/usr/lib/jvm/jdk-25"
    elif [ -d "/Library/Java/JavaVirtualMachines/jdk-25.jdk/Contents/Home" ]; then
        export JAVA_HOME="/Library/Java/JavaVirtualMachines/jdk-25.jdk/Contents/Home"
    fi
fi

clear
echo "===================================================="
echo " Auction Bidding System - Client"
echo "===================================================="
echo ""
read -p "Server IP (Enter for localhost): " SERVER_IP

# Build
echo ""
echo "===== Building project ====="
if [ -f "mvnw" ]; then
    chmod +x mvnw
    ./mvnw -q -DskipTests package
else
    mvn -q -DskipTests package
fi

if [ $? -ne 0 ]; then
    echo "BUILD FAILED! Check JDK 25 and JAVA_HOME."
    read -p "Press Enter to exit..."
    exit 1
fi

if [ ! -f "target/BiddingSystem-client.jar" ]; then
    echo "ERROR: target/BiddingSystem-client.jar not found"
    read -p "Press Enter to exit..."
    exit 1
fi

# Run
if [ -z "$SERVER_IP" ]; then
    echo ""
    echo "===== Step 1: Starting local Server ====="
    java -jar "target/BiddingSystem-server.jar" &
    SERVER_PID=$!
    sleep 3
    echo ""
    echo "===== Step 2: Starting Client (localhost) ====="
    java -jar "target/BiddingSystem-client.jar"
    kill $SERVER_PID 2>/dev/null
else
    echo ""
    echo "===== Connecting to Server: $SERVER_IP ====="
    java -jar "target/BiddingSystem-client.jar" "$SERVER_IP"
fi

echo ""
echo "===== Client finished ====="
read -p "Press Enter to exit..."
