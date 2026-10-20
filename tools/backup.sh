#!/usr/bin/env bash
# Snapshot del estado del stick: paquetes, propiedades y settings.
# Uso: ./backup.sh [carpeta-destino]
set -euo pipefail
source "$(dirname "$0")/env.sh"

DIR="${1:-$(dirname "$0")/../backup/$(date +%F)}"
mkdir -p "$DIR"

adb_tv shell "pm list packages -f"        < /dev/null > "$DIR/packages-full.txt"
adb_tv shell "pm list packages -d"          < /dev/null > "$DIR/packages-disabled.txt"
adb_tv shell "pm list packages -3"          < /dev/null > "$DIR/packages-user.txt"
adb_tv shell "getprop"                      < /dev/null > "$DIR/getprop.txt"
adb_tv shell "settings list global"         < /dev/null > "$DIR/settings-global.txt"
adb_tv shell "settings list system"         < /dev/null > "$DIR/settings-system.txt"
adb_tv shell "settings list secure"         < /dev/null > "$DIR/settings-secure.txt"

echo "Backup en $DIR"
ls -1 "$DIR"
