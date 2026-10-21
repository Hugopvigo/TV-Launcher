#!/usr/bin/env bash
# Instala el launcher en el stick y lo deja como inicio.
# El APK se compila antes: ./build.sh assembleRelease
#
# El stick ("HA TV dormitorio") cambia de IP con el DHCP: se localiza por su
# MAC con tools/tv.sh. Se puede fijar a mano:
#   TV_SERIAL=192.168.3.69:5555 ./tools/install.sh
_TV_SH="$(cd "$(dirname "$0")/../../tools" && pwd)/tv.sh"
if [ -f "$_TV_SH" ]; then
  . "$_TV_SH"
  TV_SERIAL="${TV_SERIAL:-$(tv_serial)}"
else
  TV_SERIAL="${TV_SERIAL:-192.168.3.69:5555}"
fi
PKG=es.hugoperezvigo.tvlauncher
GOOGLE_LAUNCHER=com.google.android.tvlauncher
PROJ="$(cd "$(dirname "$0")/.." && pwd)"
APK="$PROJ/app/build/outputs/apk/release/app-release.apk"

# adb: el del PATH, o el del SDK de Android en Windows.
if command -v adb >/dev/null 2>&1; then
  ADB=adb
elif [ -x "/mnt/c/Users/Hugop/AppData/Local/Android/Sdk/platform-tools/adb.exe" ]; then
  ADB="/mnt/c/Users/Hugop/AppData/Local/Android/Sdk/platform-tools/adb.exe"
else
  echo "No encuentro adb. Instalalo o ponlo en el PATH."; exit 1
fi
A=("$ADB" -s "$TV_SERIAL")

[ -f "$APK" ] || { echo "Falta el APK: compila con ./build.sh assembleRelease"; exit 1; }

echo "== 0. conecto con el stick ($TV_SERIAL) =="
"$ADB" connect "$TV_SERIAL" 2>&1 | tr -d '\r'

echo
echo "== 1. instalo el APK =="
"${A[@]}" install -r "$APK" 2>&1 | tail -2 | tr -d '\r'

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
echo "   $ADB -s $TV_SERIAL reboot"
