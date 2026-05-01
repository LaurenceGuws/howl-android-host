#!/usr/bin/env bash
set -euo pipefail

# Usage:
#   ./tail_runtime_logs.sh                 # all runtime + native payload lines
#   ./tail_runtime_logs.sh HST,TRM,RDI     # only selected module tags
#
# Output format strips Android prefix like:
# 05-01 00:25:39.851 I/howl.term.runtime( 8016):

mods="${1:-}"

if [[ -n "$mods" ]]; then
  # Build regex: ^...,(HST|TRM|RDI),
  mod_re="$(printf '%s' "$mods" | sed 's/,/|/g')"
  adb logcat -v time -s howl.term.runtime howl.term.native \
    | awk -v mr="$mod_re" '
      {
        line=$0
        sub(/^[0-9][0-9]-[0-9][0-9] [0-9:.]+ [VDIWEF]\/howl\.term\.(runtime|native)\([[:space:]]*[0-9]+\):[[:space:]]*/, "", line)
        if (line ~ /^[0-9]+S-[0-9]+u,/) {
          if (line ~ ("^[0-9]+S-[0-9]+u,(" mr "),")) print line
        }
      }
    '
else
  adb logcat -v time -s howl.term.runtime howl.term.native \
    | awk '
      {
        line=$0
        sub(/^[0-9][0-9]-[0-9][0-9] [0-9:.]+ [VDIWEF]\/howl\.term\.(runtime|native)\([[:space:]]*[0-9]+\):[[:space:]]*/, "", line)
        if (line ~ /^[0-9]+S-[0-9]+u,/) print line
      }
    '
fi
