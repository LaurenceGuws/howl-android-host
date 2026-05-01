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
ATTACH_LOGCAT="${ATTACH_LOGCAT:-0}"
JNI_LIB_DIR="$ROOT/src/main/jniLibs/arm64-v8a"
STRINGS_XML="$ROOT/src/main/res/values/strings.xml"
PKG_DATA_DIR="/data/data/$PKG/files"
PREFIX_DIR="$PKG_DATA_DIR/usr"
PM_PATH="$PREFIX_DIR/bin/howl-pm"

mkdir -p "$LOG_DIR"
mkdir -p "$JNI_LIB_DIR"

manifest_url_from_xml() {
  sed -n 's:.*<string name="userland_manifest_url">\(.*\)</string>.*:\1:p' "$STRINGS_XML" | head -n 1
}

sync_howl_pm_binary() {
  local manifest_url
  manifest_url="$(manifest_url_from_xml)"
  if [ -z "$manifest_url" ]; then
    echo "howl_pm_sync=skip reason=manifest_url_missing"
    return 0
  fi
  local release_prefix
  release_prefix="$(sed -E 's#(https://github.com/[^/]+/[^/]+/releases/download/[^/]+)/.*#\1#' <<<"$manifest_url")"
  if [ -z "$release_prefix" ] || [ "$release_prefix" = "$manifest_url" ]; then
    echo "howl_pm_sync=skip reason=manifest_url_unexpected"
    return 0
  fi
  local latest_url="$release_prefix/howl-pm-android-arm64"
  local tmp_bin
  tmp_bin="$(mktemp)"
  if ! curl -fL --connect-timeout 10 --max-time 60 "$latest_url" -o "$tmp_bin" >/dev/null 2>&1; then
    rm -f "$tmp_bin"
    echo "howl_pm_sync=skip reason=download_failed url=$latest_url"
    return 0
  fi
  local latest_sha
  latest_sha="$(sha256sum "$tmp_bin" | awk '{print $1}')"
  local installed_sha
  installed_sha="$(
    adb shell run-as "$PKG" sh -lc "if [ -f '$PM_PATH' ]; then sha256sum '$PM_PATH' | awk '{print \\\$1}'; fi" 2>/dev/null \
      | tr -d '\r'
  )"
  if [ -n "$installed_sha" ] && [ "$installed_sha" = "$latest_sha" ]; then
    rm -f "$tmp_bin"
    echo "howl_pm_sync=up_to_date sha256=$latest_sha"
    return 0
  fi
  adb push "$tmp_bin" /data/local/tmp/howl-pm-android-arm64 >/dev/null
  adb shell run-as "$PKG" sh -lc "mkdir -p '$PREFIX_DIR/bin' && cp /data/local/tmp/howl-pm-android-arm64 '$PM_PATH' && chmod 755 '$PM_PATH'"
  adb shell rm -f /data/local/tmp/howl-pm-android-arm64 >/dev/null 2>&1 || true
  rm -f "$tmp_bin"
  echo "howl_pm_sync=replaced old_sha=${installed_sha:-none} new_sha=$latest_sha"
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
