#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$ROOT/../.." && pwd)"
HOWL_TERM_ROOT="$REPO_ROOT/howl-term"
LOG_DIR="$ROOT/build/reports/android"
LOG_FILE="$LOG_DIR/deploy_iteration.log"
SUMMARY_FILE="$LOG_DIR/deploy_iteration.summary"
PKG="howl.term"
ACTIVITY="howl.term/.Main"
TIMEOUT_SECS="${TIMEOUT_SECS:-8}"
JNI_LIB_DIR="$ROOT/src/main/jniLibs/arm64-v8a"

mkdir -p "$LOG_DIR"
mkdir -p "$JNI_LIB_DIR"

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
    -Dsession-transport-variant=android_pty \
    -Dandroid-ndk-sysroot="$NDK_SYSROOT" \
    --sysroot "$NDK_SYSROOT"
)
cp "$HOWL_TERM_ROOT/zig-out/lib/libhowl_term.so" "$JNI_LIB_DIR/libhowl_term.so"

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
