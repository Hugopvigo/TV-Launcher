#!/usr/bin/env bash
# Restaura el stick a su estado original.
#   ./restore.sh              -> reactiva los deshabilitados y reinstala los desinstalados
#   ./restore.sh <paquete>    -> reactiva un paquete concreto
#
# Los desinstalados se recuperan con 'pm install-existing' (el APK sigue en
# /data/app; no hace falta volver a descargarlos de Play Store).
set -euo pipefail
source "$(dirname "$0")/env.sh"
DIR="$(dirname "$0")"
LIST_DISABLED="$DIR/disabled.list"
LIST_UNINSTALLED="$DIR/uninstalled.list"

if [ $# -ge 1 ]; then
  echo "-> pm enable --user 0 $1"
  adb_tv shell "pm enable --user 0 $1" || true
  exit 0
fi

echo "=== 1. Reactivando paquetes deshabilitados ==="
if [ -s "$LIST_DISABLED" ]; then
  while read -r pkg; do
    [ -n "$pkg" ] || continue
    case "$pkg" in \#*) continue ;; esac
    # Los que desinstalamos ya no se reactivan (se restauran en el paso 2).
    if grep -qxF "$pkg" "$LIST_UNINSTALLED" 2>/dev/null; then
      echo "-- $pkg  (desinstalado: se restaura en el paso 2)"
      continue
    fi
    echo "-> pm enable --user 0 $pkg"
    # </dev/null es imprescindible: sin el, adb shell se come el resto de la lista.
    adb_tv shell "pm enable --user 0 $pkg" < /dev/null || echo "   (aviso: no se pudo reactivar $pkg)"
  done < "$LIST_DISABLED"
else
  echo "  (nada que reactivar)"
fi

echo
echo "=== 2. Reinstalando apps desinstaladas ==="
if [ -s "$LIST_UNINSTALLED" ]; then
  while read -r pkg; do
    [ -n "$pkg" ] || continue
    case "$pkg" in \#*) continue ;; esac
    echo "-> pm install-existing $pkg"
    adb_tv shell "pm install-existing --user 0 $pkg" < /dev/null || echo "   (aviso: $pkg ya no esta en /data/app; reinstallar desde Play Store)"
  done < "$LIST_UNINSTALLED"
else
  echo "  (nada que reinstalar)"
fi

echo
echo "=== 3. Estado final ==="
echo "-- paquetes deshabilitados:"
adb_tv shell "pm list packages -d"
echo "-- apps de usuario:"
adb_tv shell "pm list packages -3"
