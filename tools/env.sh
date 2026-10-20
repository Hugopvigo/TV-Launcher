#!/usr/bin/env bash
# Entorno comun: adb nativo (WSL) apuntando al Mi TV Stick.
# Uso de adb:  adb_tv <args...>   (funcion, valida en bash y zsh)
export PATH="/tmp/opencode/pt/platform-tools:$PATH"
export TV_SERIAL="${TV_SERIAL:-192.168.3.124:5555}"

adb_tv() { adb -s "$TV_SERIAL" "$@"; }

# Conecta si hace falta.
adb_tv get-state >/dev/null 2>&1 || adb_tv connect "$TV_SERIAL" >/dev/null 2>&1
