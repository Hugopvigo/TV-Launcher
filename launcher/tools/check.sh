#!/usr/bin/env bash
# Comprueba que el launcher funciona: apps, dock, navegacion y arranque.
export PATH="/tmp/opencode/pt/platform-tools:/usr/bin:/bin"
A=(adb -s 192.168.3.124:5555)
PKG=es.hugoperezvigo.tvlauncher
SHOTS="$(cd "$(dirname "$0")/.." && pwd)/../backup/2026-10-20/screenshots"

echo "== 1. HOME por defecto =="
"${A[@]}" shell "dumpsys package preferred-xml" 2>&1 | grep -E "com\." | tr -d '\r'

echo
echo "== 2. launcher de Google TV (debe estar deshabilitado) =="
"${A[@]}" shell "pm list packages -d" 2>&1 | grep "tvlauncher" | tr -d '\r'

echo
echo "== 3. proceso y RAM =="
"${A[@]}" shell "ps -A -o PID,RSS,NAME 2>/dev/null | grep -E 'suop'" | tr -d '\r'
"${A[@]}" shell "cat /proc/meminfo" | grep MemAvailable | tr -d '\r'

echo
echo "== 4. apps que ve =="
"${A[@]}" shell "logcat -d -s TVLauncher:I" 2>&1 | grep -c "app:" | tr -d '\r'

echo
echo "== 5. crashes =="
"${A[@]}" shell "logcat -d -b crash -t 100" 2>&1 | grep -E "FATAL|Process: com.suop" | tail -5 | tr -d '\r' || echo "  ninguno"

echo
echo "== 6. captura =="
mkdir -p "$SHOTS"
"${A[@]}" exec-out screencap -p > "$SHOTS/verificacion-launcher.png"
echo "$SHOTS/verificacion-launcher.png"
