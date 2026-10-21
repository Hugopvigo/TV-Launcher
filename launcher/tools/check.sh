#!/usr/bin/env bash
# Comprueba que el launcher funciona en el stick: HOME, version, apps y
# navegacion con el D-pad.
#
# La IP del stick cambia con el DHCP: se puede fijar con TV_SERIAL.
#   TV_SERIAL=192.168.3.26:5555 ./tools/check.sh
TV_SERIAL="${TV_SERIAL:-192.168.3.26:5555}"
PKG=es.hugoperezvigo.tvlauncher
GOOGLE_LAUNCHER=com.google.android.tvlauncher

if command -v adb >/dev/null 2>&1; then
  ADB=adb
elif [ -x "/mnt/c/Users/Hugop/AppData/Local/Android/Sdk/platform-tools/adb.exe" ]; then
  ADB="/mnt/c/Users/Hugop/AppData/Local/Android/Sdk/platform-tools/adb.exe"
else
  echo "No encuentro adb. Instalalo o ponlo en el PATH."; exit 1
fi
A=("$ADB" -s "$TV_SERIAL")

echo "== 1. HOME por defecto =="
"${A[@]}" shell "dumpsys package preferred-xml" 2>&1 | grep -E "com\." | tr -d '\r'

echo
echo "== 2. launcher de Google TV (debe estar deshabilitado) =="
"${A[@]}" shell "pm list packages -d" 2>&1 | grep "$GOOGLE_LAUNCHER" | tr -d '\r'

echo
echo "== 3. version instalada =="
"${A[@]}" shell "dumpsys package $PKG | grep -E 'versionName|lastUpdateTime'" | tr -d '\r'

echo
echo "== 4. proceso y RAM =="
"${A[@]}" shell "ps -A -o PID,RSS,NAME 2>/dev/null | grep -E 'tvlauncher'" | tr -d '\r'
"${A[@]}" shell "cat /proc/meminfo" | grep MemAvailable | tr -d '\r'

echo
echo "== 5. prueba de navegacion (D-pad) =="
"${A[@]}" shell "am force-stop $PKG; input keyevent 3" < /dev/null 2>&1
sleep 4
echo "  abajo:"; "${A[@]}" shell "input keyevent 20" < /dev/null 2>&1; sleep 1
echo "  derecha:"; "${A[@]}" shell "input keyevent 22" < /dev/null 2>&1; sleep 1
echo "  ok (deberia abrir una app):"; "${A[@]}" shell "input keyevent 23" < /dev/null 2>&1; sleep 4
"${A[@]}" shell "dumpsys activity activities | grep -E 'mResumedActivity'" | tr -d '\r'
"${A[@]}" shell "input keyevent 4" < /dev/null 2>&1

echo
echo "== 6. crashes =="
"${A[@]}" shell "logcat -d -b crash -t 100" 2>&1 | grep -E "FATAL|Process: $PKG" | tail -5 | tr -d '\r' || echo "  ninguno"

echo
echo "== 7. captura =="
SHOTS="$(cd "$(dirname "$0")/.." && pwd)/../backup/2026-10-20/screenshots"
mkdir -p "$SHOTS"
"${A[@]}" exec-out screencap -p > "$SHOTS/verificacion-launcher.png"
echo "$SHOTS/verificacion-launcher.png"
