#!/usr/bin/env bash
# Instala el launcher en el stick y lo deja como inicio.
# El APK se compila antes: ./build.sh assembleDebug
export PATH="/tmp/opencode/pt/platform-tools:/usr/bin:/bin"
A=(adb -s 192.168.3.124:5555)
PROJ="$(cd "$(dirname "$0")/.." && pwd)"
APK="$PROJ/app/build/outputs/apk/debug/app-debug.apk"
PKG=com.suop.tvlauncher
GOOGLE_LAUNCHER=com.google.android.tvlauncher

[ -f "$APK" ] || { echo "Falta el APK: compila con ./build.sh assembleDebug"; exit 1; }

echo "== 1. instalo el APK =="
"${A[@]}" install -r -g "$APK" 2>&1 | tail -2

echo
echo "== 2. lo pongo como inicio =="
"${A[@]}" shell "cmd package set-home-activity $PKG/.LauncherActivity" 2>&1 | tr -d '\r'

echo
echo "== 3. deshabilito el launcher de Google TV =="
# Imprescindible: mientras siga habilitado, el sistema lo relanza y se pone
# delante del nuestro (mitv.service corre como uid 1000 y es PERSISTENT).
"${A[@]}" shell "pm disable-user --user 0 $GOOGLE_LAUNCHER" 2>&1 | tr -d '\r'

echo
echo "== 4. compruebo que el HOME es el nuestro =="
"${A[@]}" shell "dumpsys package preferred-xml" 2>&1 | grep -E "com\." | tr -d '\r'
"${A[@]}" shell "pm list packages -d" 2>&1 | grep "$GOOGLE_LAUNCHER" | tr -d '\r'

echo
echo "== listo. Reinicia el stick para verlo desde el arranque =="
echo "   adb -s 192.168.3.124:5555 reboot"
