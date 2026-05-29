#!/bin/bash
# Auction Bidding System - Server (Linux/macOS)
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

echo "===================================================="
echo " Auction Bidding System - Server"
echo "===================================================="
echo ""

# Build (only if JAR doesn't exist)
echo ""
if [ -f "target/BiddingSystem-server.jar" ]; then
    echo "===== JAR exists, skipping build ====="
else
    echo "===== Building project (first run) ====="
    if [ -f "mvnw" ]; then
        chmod +x mvnw
        ./mvnw -q -DskipTests package
    else
        mvn -q -DskipTests package
    fi

    if [ $? -ne 0 ]; then
        echo "BUILD FAILED!"
        exit 1
    fi

    if [ ! -f "target/BiddingSystem-server.jar" ]; then
        echo "ERROR: target/BiddingSystem-server.jar not found"
        exit 1
    fi
fi

echo ""
echo "===== Starting Server on port 9999 ====="
echo "Press Ctrl+C to stop"
echo ""
java -jar "target/BiddingSystem-server.jar"
