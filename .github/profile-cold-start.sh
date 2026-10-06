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

echo "PROFILE COLD-START TEST PASSED"
