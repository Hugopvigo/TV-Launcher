#!/usr/bin/env bash
# Entorno comun: adb apuntando al Mi TV Stick.
# Uso de adb:  adb_tv <args...>   (funcion, valida en bash y zsh)
#
# La IP del stick cambia con el DHCP. Para verla:
#   adb mdns services
# y se puede fijar:
#   TV_SERIAL=192.168.3.26:5555
export TV_SERIAL="${TV_SERIAL:-192.168.3.26:5555}"

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
