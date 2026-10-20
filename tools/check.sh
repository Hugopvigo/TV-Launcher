#!/usr/bin/env bash
# Comprueba que el stick sigue respondiendo: launcher en primer plano, sin crash
# recientes y con la red (ADB) viva.
set -euo pipefail
source "$(dirname "$0")/env.sh"

echo "== estado adb: $(adb_tv get-state 2>&1)"
echo "== actividad en primer plano:"
adb_tv shell "dumpsys activity activities | grep -E 'topResumedActivity|mResumedActivity'" | head -3
echo "== crashes recientes:"
adb_tv shell "logcat -d -b crash -t 40 2>/dev/null | grep -E 'FATAL EXCEPTION|Process: '" | tail -5 || true
echo "== paquetes desactivados: $(adb_tv shell 'pm list packages -d' | wc -l)"
