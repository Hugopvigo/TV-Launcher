#!/usr/bin/env bash
# Desactiva un lote de paquetes y comprueba que el stick sigue vivo.
# Uso: ./disable.sh <paquete> [paquete...]
# Reglas: una orden pm por llamada; nunca desinstalar; registro en disabled.list
set -euo pipefail
source "$(dirname "$0")/env.sh"
LIST="$(dirname "$0")/disabled.list"

[ $# -ge 1 ] || { echo "uso: $0 <paquete> [paquete...]"; exit 1; }

for pkg in "$@"; do
  echo "== pm disable-user --user 0 $pkg"
  adb_tv shell "pm disable-user --user 0 $pkg" < /dev/null || echo "   !! fallo al desactivar $pkg"
  grep -qxF "$pkg" "$LIST" 2>/dev/null || echo "$pkg" >> "$LIST"
done

echo
echo "--- Comprobación de que el launcher sigue en pie ---"
adb_tv shell "dumpsys activity activities | grep -E 'topResumedActivity|mResumedActivity'" | head -3
echo "--- Paquetes desactivados ahora mismo ---"
adb_tv shell "pm list packages -d" | wc -l
