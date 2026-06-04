#!/bin/bash

# Gradle wrapper for building APK
export ANDROID_HOME="$HOME/android-sdk"
export ANDROID_SDK_ROOT="$ANDROID_HOME"

# Determine the project root
cd "$(dirname "$0")"

# Download Gradle wrapper jar if needed
WRAPPER_JAR="gradle/wrapper/gradle-wrapper.jar"
if [ ! -f "$WRAPPER_JAR" ]; then
    echo "Downloading Gradle wrapper..."
    # Download from the GitHub releases
    GRADLE_VERSION=$(grep distributionUrl gradle/wrapper/gradle-wrapper.properties | sed 's/.*gradle-\(.*\)-bin.zip/\1/')
    curl -sL -o "$WRAPPER_JAR" "https://raw.githubusercontent.com/gradle/gradle/v${GRADLE_VERSION}/gradle/wrapper/gradle-wrapper.jar" 2>/dev/null || \
    echo "Need to download gradle-wrapper.jar manually"
fi

# Use the system gradle as the wrapper
exec java -Xmx2048m -Xms256m \
    -Dorg.gradle.appname=gradlew \
    -jar /data/data/com.termux/files/usr/opt/gradle/lib/gradle-gradle-cli-main-9.5.1.jar \
    "$@"
