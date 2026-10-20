#!/usr/bin/env bash
# Screenshot del stick: shot.sh <nombre-fichero>
set -euo pipefail
source "$(dirname "$0")/env.sh"
OUT="${1:?falta nombre}"
DIR="$(dirname "$0")/../backup/2026-10-20/screenshots"
mkdir -p "$DIR"
adb_tv exec-out screencap -p > "$DIR/$OUT.png"
printf '%s (%s bytes)\n' "$DIR/$OUT.png" "$(stat -c%s "$DIR/$OUT.png")"
