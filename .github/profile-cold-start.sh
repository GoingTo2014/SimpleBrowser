#!/usr/bin/env bash
set -euo pipefail

PACKAGE="com.example.simplebrowser"
APK="$GITHUB_WORKSPACE/app/build/outputs/apk/release/app-release.apk"

adb install -r "$APK"
adb shell pm clear "$PACKAGE" >/dev/null
adb logcat -c

wait_for_package() {
  for i in $(seq 1 20); do
    if adb shell pidof "$PACKAGE" >/dev/null 2>&1 || adb shell pidof "$PACKAGE:profile1" >/dev/null 2>&1; then
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

assert_no_app_fatal() {
  FATAL_LOG="$(adb logcat -d | grep -E 'FATAL EXCEPTION|Process: com\\.example\\.simplebrowser( |$)' || true)"
  if [ -n "$FATAL_LOG" ]; then
    echo "Fatal SimpleBrowser crash evidence:"
    echo "$FATAL_LOG"
    adb logcat -d
    exit 1
  fi
}

echo "Launching Main profile..."
adb shell am start -W -n "$PACKAGE/.MainActivity"
wait_for_package
sleep 5
assert_no_app_fatal

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

echo "Switching into the new non-Main profile..."
tap_text "Browser settings"
sleep 1
tap_text "Profile 1"
sleep 2
tap_text "RuntimeProfile"
sleep 2
tap_text "Switch"
sleep 8

if ! adb shell pidof "$PACKAGE:profile1" >/dev/null 2>&1; then
  echo "Profile process did not start after switching"
  adb logcat -d
  exit 1
fi
assert_no_app_fatal

echo "Closing the non-Main profile normally with Back..."
adb shell input keyevent 4
sleep 4
assert_no_app_fatal

echo "Launching again after normal close with non-Main profile still active..."
adb shell am start -W -n "$PACKAGE/.MainActivity"
sleep 10

if ! wait_for_package; then
  echo "SimpleBrowser did not survive cold launch after a normal close"
  adb logcat -d
  exit 1
fi

echo "UI after normal-close cold start:"
dump_ui
cat /tmp/window.xml || true
assert_no_app_fatal

echo "Force-stopping and cold-starting the same non-Main profile..."
adb shell am force-stop "$PACKAGE"
adb logcat -c
sleep 2
adb shell am start -W -n "$PACKAGE/.MainActivity"
sleep 10

if ! wait_for_package; then
  echo "SimpleBrowser did not survive force-stop cold start with non-Main profile active"
  adb logcat -d
  exit 1
fi

echo "UI after force-stop cold start:"
dump_ui
cat /tmp/window.xml || true
assert_no_app_fatal

echo "NON-MAIN PROFILE NORMAL-CLOSE AND FORCE-STOP COLD-START TEST PASSED"
