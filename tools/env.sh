#!/usr/bin/env bash
# Entorno comun: adb apuntando al Mi TV Stick.
# Uso de adb:  adb_tv <args...>   (funcion, valida en bash y zsh)
#
# El stick ("HA TV dormitorio") cambia de IP con el DHCP: se localiza por su
# MAC con tools/tv.sh. Para verla:
#   source tools/env.sh && echo $TV_SERIAL
_DIR="$(cd "$(dirname "${BASH_SOURCE[0]:-$0}")" && pwd)"
. "$_DIR/tv.sh"

export TV_SERIAL="${TV_SERIAL:-$(tv_serial)}"

# adb: el del PATH, o el del SDK de Android en Windows.
if command -v adb >/dev/null 2>&1; then
  ADB=adb
elif [ -x "/mnt/c/Users/Hugop/AppData/Local/Android/Sdk/platform-tools/adb.exe" ]; then
  ADB="/mnt/c/Users/Hugop/AppData/Local/Android/Sdk/platform-tools/adb.exe"
else
  ADB=adb
fi
export ADB

adb_tv() { "$ADB" -s "$TV_SERIAL" "$@"; }

# Conecta si hace falta.
adb_tv get-state >/dev/null 2>&1 || adb_tv connect "$TV_SERIAL" >/dev/null 2>&1
