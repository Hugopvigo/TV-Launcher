#!/usr/bin/env bash
# Restaura el launcher de Google TV como inicio y desinstala el nuestro.
# Uso: ./tools/uninstall-launcher.sh
export PATH="/tmp/opencode/pt/platform-tools:/usr/bin:/bin"
A=(adb -s 192.168.3.124:5555)

echo "== 1. devuelvo el HOME al launcher de Google TV =="
"${A[@]}" shell "cmd package set-home-activity com.google.android.tvlauncher/.MainActivity" 2>&1 | tr -d '\r'

echo
echo "== 2. reactivo el launcher de Google TV =="
"${A[@]}" shell "pm enable --user 0 com.google.android.tvlauncher" 2>&1 | tr -d '\r'

echo
echo "== 3. desinstalo mi launcher =="
"${A[@]}" shell "pm uninstall es.hugoperezvigo.tvlauncher" 2>&1 | tr -d '\r'

echo
echo "== 4. reinicio y compruebo =="
"${A[@]}" shell "reboot" < /dev/null 2>&1
sleep 30
for i in $(seq 1 30); do
  st=$("${A[@]}" get-state 2>/dev/null)
  [ "$st" = "device" ] && { echo "vuelve (iter $i)"; break; }
  "${A[@]}" connect 192.168.3.124:5555 > /dev/null 2>&1
  sleep 5
done
sleep 40
"${A[@]}" shell "dumpsys activity activities | grep -E 'topResumedActivity'" | head -2 | tr -d '\r'
"${A[@]}" shell "pm list packages -e com.google.android.tvlauncher" | tr -d '\r'
"${A[@]}" shell "pm list packages es.hugoperezvigo.tvlauncher" | tr -d '\r' || echo "  mi launcher ya no esta"
