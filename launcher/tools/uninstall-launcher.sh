#!/usr/bin/env bash
# Restaura el launcher de Google TV como inicio y desinstala el nuestro.
# Uso: ./tools/uninstall-launcher.sh
# La IP del stick cambia con el DHCP: se puede fijar con TV_SERIAL.
TV_SERIAL="${TV_SERIAL:-192.168.3.26:5555}"

if command -v adb >/dev/null 2>&1; then
  ADB=adb
elif [ -x "/mnt/c/Users/Hugop/AppData/Local/Android/Sdk/platform-tools/adb.exe" ]; then
  ADB="/mnt/c/Users/Hugop/AppData/Local/Android/Sdk/platform-tools/adb.exe"
else
  echo "No encuentro adb. Instalalo o ponlo en el PATH."; exit 1
fi
A=("$ADB" -s "$TV_SERIAL")

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
  "$ADB" connect "$TV_SERIAL" > /dev/null 2>&1
  sleep 5
done
sleep 40
"${A[@]}" shell "dumpsys activity activities | grep -E 'topResumedActivity'" | head -2 | tr -d '\r'
"${A[@]}" shell "pm list packages -e com.google.android.tvlauncher" | tr -d '\r'
"${A[@]}" shell "pm list packages es.hugoperezvigo.tvlauncher" | tr -d '\r' || echo "  mi launcher ya no esta"
