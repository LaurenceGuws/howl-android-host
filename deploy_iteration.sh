#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LOG_DIR="$ROOT/build/reports/android"
LOG_FILE="$LOG_DIR/deploy_iteration.log"
SUMMARY_FILE="$LOG_DIR/deploy_iteration.summary"
PKG="howl.term"
ACTIVITY="howl.term/.Main"
TIMEOUT_SECS="${TIMEOUT_SECS:-8}"

mkdir -p "$LOG_DIR"

./gradlew :assembleDebug --no-daemon
APK="$ROOT/build/outputs/apk/debug/howl-term-debug.apk"
adb install -r "$APK"
adb logcat -c
adb shell am start -n "$ACTIVITY"

resume_seen=0
displayed_seen=0
crash_seen=0

for _ in $(seq 1 "$TIMEOUT_SECS"); do
  dump="$(adb logcat -d -v time || true)"
  grep -q "HowlMain.*onResume: window resumed" <<<"$dump" && resume_seen=1
  grep -q "Displayed howl.term/.Main" <<<"$dump" && displayed_seen=1
  grep -q "AndroidRuntime" <<<"$dump" && crash_seen=1
  if [ "$resume_seen" -eq 1 ]; then
    break
  fi
  sleep 1
done

PID="$(adb shell pidof "$PKG" 2>/dev/null | tr -d '\r' || true)"
adb logcat -d -v time > "$LOG_FILE" || true

launch_ok=0
if [ "$resume_seen" -eq 1 ] || [ "$displayed_seen" -eq 1 ]; then
  launch_ok=1
fi

{
  echo "launch_ok=$launch_ok"
  echo "pid=${PID:-none}"
  echo "displayed_seen=$displayed_seen"
  echo "resume_seen=$resume_seen"
  echo "crash_seen=$crash_seen"
  echo "log_file=$LOG_FILE"
} > "$SUMMARY_FILE"

echo "launch_ok=$launch_ok pid=${PID:-none} displayed=$displayed_seen resume=$resume_seen crash=$crash_seen"
echo "log=$LOG_FILE"
