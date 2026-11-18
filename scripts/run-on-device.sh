#!/usr/bin/env bash
# Build dress-up (debug), install it on a device and launch it. Adapted from ../d-player/scripts/run-on-device.sh.
#
# Device choice:
#   default   the local emulator, booted headless if needed
#   --phone   the user's connected physical phone (USB or wireless adb).
#             Only when the user asks for it; they may be using the phone (AGENTS.md).
#
# Usage: scripts/run-on-device.sh [--phone] [extra gradle args]
# Env:   DRESSUP_AVD     emulator to boot (default: Medium_Phone_API_36)
#        ANDROID_HOME    SDK location (default: ~/Android/Sdk)
set -euo pipefail

SDK="${ANDROID_HOME:-$HOME/Android/Sdk}"
AVD="${DRESSUP_AVD:-Medium_Phone_API_36}"
cd "$(dirname "$0")/.."
VERSION="$(grep -oP 'versionName = "\K[^"]+' app/build.gradle.kts)"
APK="app/build/outputs/apk/debug/dress-up-$VERSION-debug.apk"
PACKAGE="com.dressup.app.debug"

physical_device() {
    adb devices | awk 'NR > 1 && $2 == "device" && $1 !~ /^emulator-/ { print $1; exit }'
}

running_emulator() {
    adb devices | awk 'NR > 1 && $2 == "device" && $1 ~ /^emulator-/ { print $1; exit }'
}

boot_emulator() {
    echo "No emulator running; booting '$AVD' (headless)…" >&2
    nohup "$SDK/emulator/emulator" -avd "$AVD" -no-window -no-audio -no-boot-anim -no-snapshot-save \
        > /dev/null 2>&1 &
    # Wait for the emulator itself; `adb wait-for-device` would return at once if a phone is connected.
    local serial=""
    until [ -n "$serial" ] && [ "$(adb -s "$serial" shell getprop sys.boot_completed 2> /dev/null | tr -d '\r')" = "1" ]; do
        sleep 2
        serial="$(running_emulator)"
    done
    echo "$serial"
}

USE_PHONE=false
if [ "${1:-}" = "--phone" ]; then
    USE_PHONE=true
    shift
fi

# Build before booting an emulator: building while it runs can push a 16 GB machine into the
# out-of-memory killer, which then kills the emulator.
./gradlew --console=plain -q assembleDebug "$@"

if $USE_PHONE; then
    DEVICE="$(physical_device)"
    if [ -z "$DEVICE" ]; then
        echo "No phone connected. Ask the user to enable Wireless debugging and run: adb connect <ip>:<port>" >&2
        exit 1
    fi
else
    DEVICE="$(running_emulator)"
    [ -z "$DEVICE" ] && DEVICE="$(boot_emulator)"
fi
echo "Using device: $DEVICE ($(adb -s "$DEVICE" shell getprop ro.product.model | tr -d '\r'), Android $(adb -s "$DEVICE" shell getprop ro.build.version.release | tr -d '\r'))"

adb -s "$DEVICE" install -r "$APK" > /dev/null
adb -s "$DEVICE" shell am start -W -n "$PACKAGE/com.dressup.app.MainActivity" | grep -E 'Status|TotalTime'
echo "DEVICE=$DEVICE"
echo "When done with the emulator: adb -s $DEVICE emu kill"
