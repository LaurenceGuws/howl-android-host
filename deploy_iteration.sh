#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$ROOT/../.." && pwd)"
HOWL_TERM_ROOT="$REPO_ROOT/howl-term"
LOG_DIR="$ROOT/build/reports/android"
LOG_FILE="$LOG_DIR/deploy_iteration.log"
SUMMARY_FILE="$LOG_DIR/deploy_iteration.summary"
PKG_DEFAULT="howl.term"
PKG="$PKG_DEFAULT"
ACTIVITY_DEFAULT="howl.term/.Main"
ACTIVITY="$ACTIVITY_DEFAULT"
TIMEOUT_SECS="${TIMEOUT_SECS:-8}"
ATTACH_LOGCAT="${ATTACH_LOGCAT:-0}"
JNI_LIB_DIR="$ROOT/src/main/jniLibs/arm64-v8a"
PKG_DATA_DIR="/data/data/$PKG/files"
PREFIX_DIR="$PKG_DATA_DIR/usr"
PM_PATH="$PREFIX_DIR/bin/howl-pm"
HOWL_PM_ROOT="$REPO_ROOT/utils/howl-pm"
HOWL_PM_LOCAL_BIN="${HOWL_PM_LOCAL_BIN:-$HOWL_PM_ROOT/dist/howl-pm-android-arm64}"

mkdir -p "$LOG_DIR"
mkdir -p "$JNI_LIB_DIR"

sync_howl_pm_binary() {
  if [ ! -x "$HOWL_PM_LOCAL_BIN" ]; then
    mkdir -p "$(dirname "$HOWL_PM_LOCAL_BIN")"
    (
      cd "$HOWL_PM_ROOT"
      GOOS=android GOARCH=arm64 CGO_ENABLED=0 go build -o "$HOWL_PM_LOCAL_BIN" ./cmd/howl-pm
    )
  fi
  if [ ! -x "$HOWL_PM_LOCAL_BIN" ]; then
    echo "howl_pm_sync=skip reason=local_bin_missing path=$HOWL_PM_LOCAL_BIN"
    return 0
  fi
  local local_ver
  local_ver="$(
    (
      cd "$HOWL_PM_ROOT"
      go run ./cmd/howl-pm version
    ) 2>/dev/null | tr -d '\r'
  )"
  local_ver="$(printf '%s\n' "$local_ver" | head -n1 | tr -d '[:space:]')"
  if ! [[ "$local_ver" =~ ^[0-9]+\.[0-9]+\.[0-9]+ ]]; then
    echo "howl_pm_sync=skip reason=local_version_invalid value=$local_ver"
    return 0
  fi
  if [ -z "$local_ver" ]; then
    echo "howl_pm_sync=skip reason=local_version_unavailable"
    return 0
  fi
  local installed_ver
  installed_ver="$(
    adb shell run-as "$PKG" "$PM_PATH" version 2>/dev/null \
      | tr -d '\r' | head -n1 | tr -d '[:space:]'
  )"
  if [ -n "$installed_ver" ] && ! [[ "$installed_ver" =~ ^[0-9]+\.[0-9]+\.[0-9]+ ]]; then
    installed_ver=""
  fi

  if [ -z "$installed_ver" ]; then
    adb push "$HOWL_PM_LOCAL_BIN" /data/local/tmp/howl-pm-android-arm64 >/dev/null
    adb shell run-as "$PKG" sh -lc "mkdir -p '$PREFIX_DIR/bin' && cp /data/local/tmp/howl-pm-android-arm64 '$PM_PATH' && chmod 755 '$PM_PATH'"
    adb shell rm -f /data/local/tmp/howl-pm-android-arm64 >/dev/null 2>&1 || true
    echo "howl_pm_sync=installed local_version=$local_ver installed_version=none"
    return 0
  fi

  local newest
  newest="$(printf '%s\n%s\n' "$installed_ver" "$local_ver" | sort -V | tail -n1)"
  if [ "$newest" = "$local_ver" ] && [ "$local_ver" != "$installed_ver" ]; then
    adb push "$HOWL_PM_LOCAL_BIN" /data/local/tmp/howl-pm-android-arm64 >/dev/null
    adb shell run-as "$PKG" sh -lc "mkdir -p '$PREFIX_DIR/bin' && cp /data/local/tmp/howl-pm-android-arm64 '$PM_PATH' && chmod 755 '$PM_PATH'"
    adb shell rm -f /data/local/tmp/howl-pm-android-arm64 >/dev/null 2>&1 || true
    echo "howl_pm_sync=replaced local_version=$local_ver installed_version=$installed_ver"
    return 0
  fi
  echo "howl_pm_sync=up_to_date local_version=$local_ver installed_version=$installed_ver"
}

resolve_runtime_ids() {
  if ! command -v aapt >/dev/null 2>&1; then
    echo "runtime_pkg=$PKG runtime_activity=$ACTIVITY (aapt_missing)"
    return 0
  fi
  local app_id launcher
  app_id="$(aapt dump badging "$APK" | sed -n "s/^package: name='\\([^']*\\)'.*/\\1/p" | head -n1)"
  if [ -n "$app_id" ]; then
    PKG="$app_id"
    local launchable
    launchable="$(aapt dump badging "$APK" | sed -n "s/^launchable-activity: name='\\([^']*\\)'.*/\\1/p" | head -n1)"
    if [ -n "$launchable" ]; then
      ACTIVITY="$PKG/$launchable"
    fi
  fi
  PKG_DATA_DIR="/data/data/$PKG/files"
  PREFIX_DIR="$PKG_DATA_DIR/usr"
  PM_PATH="$PREFIX_DIR/bin/howl-pm"
  echo "runtime_pkg=$PKG runtime_activity=$ACTIVITY"
}

pick_ndk_root() {
  if [ -n "${ANDROID_NDK_HOME:-}" ] && [ -d "$ANDROID_NDK_HOME" ]; then
    printf '%s' "$ANDROID_NDK_HOME"
    return 0
  fi
  if [ -n "${ANDROID_NDK_ROOT:-}" ] && [ -d "$ANDROID_NDK_ROOT" ]; then
    printf '%s' "$ANDROID_NDK_ROOT"
    return 0
  fi
  if [ -d "${ANDROID_SDK_ROOT:-}/ndk" ]; then
    ls -d "${ANDROID_SDK_ROOT}/ndk/"* 2>/dev/null | sort -V | tail -n 1
    return 0
  fi
  if [ -d "${ANDROID_HOME:-}/ndk" ]; then
    ls -d "${ANDROID_HOME}/ndk/"* 2>/dev/null | sort -V | tail -n 1
    return 0
  fi
  if [ -d "$HOME/.local/share/zide-android-sdk/ndk" ]; then
    ls -d "$HOME/.local/share/zide-android-sdk/ndk/"* 2>/dev/null | sort -V | tail -n 1
    return 0
  fi
  return 1
}

NDK_ROOT="$(pick_ndk_root || true)"
if [ -z "$NDK_ROOT" ] || [ ! -d "$NDK_ROOT" ]; then
  echo "android_ndk_missing=1"
  exit 1
fi
NDK_SYSROOT="$NDK_ROOT/toolchains/llvm/prebuilt/linux-x86_64/sysroot"
if [ ! -d "$NDK_SYSROOT" ]; then
  echo "android_ndk_sysroot_missing=1 path=$NDK_SYSROOT"
  exit 1
fi

(
  cd "$HOWL_TERM_ROOT"
  zig build \
    -Dtarget=aarch64-linux-android \
    -Drender-variant=gles \
    -Dsession-pty-variant=android_pty \
    -Dandroid-ndk-sysroot="$NDK_SYSROOT" \
    --sysroot "$NDK_SYSROOT"
)
cp "$HOWL_TERM_ROOT/zig-out/lib/libhowl_term.so" "$JNI_LIB_DIR/libhowl_term.so"

./gradlew :assembleDebug --no-daemon
APK="$ROOT/build/outputs/apk/debug/howl-term-debug.apk"
resolve_runtime_ids
adb install -r "$APK"
sync_howl_pm_binary
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
echo "hint: adb logcat -v time -s howl.term.runtime:I howl.term.native:I AndroidRuntime:E '*:S'"

if [ "$ATTACH_LOGCAT" = "1" ]; then
  echo "attach_logcat=1"
  exec adb logcat -v time -s howl.term.runtime:I howl.term.native:I AndroidRuntime:E '*:S'
fi
