#!/usr/bin/env bash
set -euo pipefail

PACKAGE="com.example.simplebrowser"
APK="$GITHUB_WORKSPACE/app/build/outputs/apk/release/app-release.apk"

adb install -r "$APK"
adb shell pm clear "$PACKAGE" >/dev/null
adb logcat -c

wait_for_package() {
  for i in $(seq 1 20); do
    if adb shell pidof "$PACKAGE" >/dev/null 2>&1; then
      return 0
    fi
    sleep 1
  done
  return 1
}

dump_ui() {
  adb shell uiautomator dump /sdcard/window.xml >/dev/null 2>&1 || true
  adb exec-out cat /sdcard/window.xml 2>/dev/null > /tmp/window.xml || true
}

find_center() {
  python3 - "$1" <<'PY'
import re
import sys
import xml.etree.ElementTree as ET

target = sys.argv[1]
root = ET.parse("/tmp/window.xml").getroot()

for node in root.iter():
    text = node.attrib.get("text", "")
    desc = node.attrib.get("content-desc", "")
    if target == text or target == desc or target in text or target in desc:
        bounds = node.attrib.get("bounds", "")
        match = re.match(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]", bounds)
        if match:
            x1, y1, x2, y2 = map(int, match.groups())
            print((x1 + x2) // 2, (y1 + y2) // 2)
            raise SystemExit(0)

raise SystemExit(1)
PY
}

tap_text() {
  target="$1"
  dump_ui
  center="$(find_center "$target")" || {
    echo "Could not find UI text: $target"
    cat /tmp/window.xml || true
    exit 1
  }
  set -- $center
  adb shell input tap "$1" "$2"
}

echo "Launching SimpleBrowser initially..."
adb shell am start -W -n "$PACKAGE/.MainActivity"
wait_for_package || {
  echo "SimpleBrowser died on initial launch"
  adb logcat -d
  exit 1
}
sleep 5

echo "Opening profile manager..."
tap_text "Browser settings"
sleep 1
tap_text "Profile 1"
sleep 2
tap_text "Create profile"
sleep 1
adb shell input text "RuntimeProfile"
tap_text "SAVE"
sleep 3

echo "Created profile; now switching into the new profile before closing..."
if ! adb shell pidof "$PACKAGE" >/dev/null 2>&1; then
  echo "SimpleBrowser died immediately after profile creation"
  adb logcat -d
  exit 1
fi

tap_text "Browser settings"
sleep 1
tap_text "Profile 1"
sleep 2
tap_text "Switch"
sleep 8

echo "Verifying the new profile process before cold start..."
if ! adb shell pidof "$PACKAGE:profile1" >/dev/null 2>&1; then
  echo "Profile process did not start after switching into the new profile"
  adb logcat -d
  exit 1
fi

echo "Cold-starting while the non-Main profile is active..."
adb shell am force-stop "$PACKAGE"
adb logcat -c
sleep 2

adb shell am start -W -n "$PACKAGE/.MainActivity"
sleep 10

if ! wait_for_package; then
  echo "SimpleBrowser died after cold-starting into a non-Main profile"
  adb logcat -d
  exit 1
fi

echo "UI after cold-start with non-Main profile active:"
dump_ui
cat /tmp/window.xml || true

FATAL_LOG="$(adb logcat -d | grep -E 'FATAL EXCEPTION|AndroidRuntime.*FATAL|Process: com\\.example\\.simplebrowser|chromium|libwebviewchromium' || true)"
if [ -n "$FATAL_LOG" ]; then
  echo "Crash/fatal evidence found during non-Main cold start:"
  echo "$FATAL_LOG"
  adb logcat -d
  exit 1
fi

echo "NON-MAIN PROFILE COLD-START TEST PASSED"

echo "Created profile; checking process and active UI..."
if ! adb shell pidof "$PACKAGE" >/dev/null 2>&1; then
  echo "SimpleBrowser died immediately after profile creation"
  adb logcat -d
  exit 1
fi

echo "Force-stopping for cold-start test..."
adb shell am force-stop "$PACKAGE"
adb logcat -c

echo "Launching SimpleBrowser after profile creation..."
adb shell am start -W -n "$PACKAGE/.MainActivity"
sleep 8

if ! wait_for_package; then
  echo "SimpleBrowser died after creating a profile and force-stop"
  adb logcat -d
  exit 1
fi

echo "Second launch UI:"
dump_ui
cat /tmp/window.xml || true

FATAL_LOG="$(adb logcat -d | grep -E 'FATAL EXCEPTION|AndroidRuntime.*FATAL|Process: com\.example\.simplebrowser' || true)"
if [ -n "$FATAL_LOG" ]; then
  echo "Fatal Android exception found:"
  echo "$FATAL_LOG"
  adb logcat -d
  exit 1
fi

echo "Testing one-tap switch into the new profile..."
tap_text "Browser settings"
sleep 1
tap_text "Profile 1"
sleep 2
tap_text "Switch"
sleep 7

if ! adb shell pidof "$PACKAGE:profile1" >/dev/null 2>&1; then
  echo "Profile process did not start after one switch tap"
  adb logcat -d
  exit 1
fi

echo "Testing one-tap switch back to Main..."
tap_text "Browser settings"
sleep 1
tap_text "RuntimeProfileProfile 2"
sleep 2
tap_text "Switch"
sleep 7

if ! adb shell pidof "$PACKAGE" >/dev/null 2>&1; then
  echo "Main process did not return after one switch tap"
  adb logcat -d
  exit 1
fi

sleep 2
if adb shell pidof "$PACKAGE:profile1" >/dev/null 2>&1; then
  echo "Old profile process is still alive after returning to Main"
  adb logcat -d
  exit 1
fi

FATAL_LOG="$(adb logcat -d | grep -E 'FATAL EXCEPTION|AndroidRuntime.*FATAL|Process: com\.example\.simplebrowser' || true)"
if [ -n "$FATAL_LOG" ]; then
  echo "Fatal Android exception found during profile switching:"
  echo "$FATAL_LOG"
  adb logcat -d
  exit 1
fi

echo "PROFILE COLD-START AND ONE-TAP SWITCH TEST PASSED"
