#!/usr/bin/env bash
# Snapshot del estado del stick: paquetes, propiedades y settings.
# Uso: ./backup.sh [carpeta-destino]
set -euo pipefail
source "$(dirname "$0")/env.sh"

DIR="${1:-$(dirname "$0")/../backup/$(date +%F)}"
mkdir -p "$DIR"

adb_tv shell "pm list packages -f"        > "$DIR/packages-full.txt"
adb_tv shell "pm list packages -d"          > "$DIR/packages-disabled.txt"
adb_tv shell "pm list packages -3"          > "$DIR/packages-user.txt"
adb_tv shell "getprop"                      > "$DIR/getprop.txt"
adb_tv shell "settings list global"         > "$DIR/settings-global.txt"
adb_tv shell "settings list system"         > "$DIR/settings-system.txt"
adb_tv shell "settings list secure"         > "$DIR/settings-secure.txt"

echo "Backup en $DIR"
ls -1 "$DIR"
