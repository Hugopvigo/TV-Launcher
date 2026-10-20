#!/usr/bin/env bash
# Reactiva los paquetes desactivados por este debloat.
# Uso: ./restore.sh            -> reactiva todo lo de tools/disabled.list
#      ./restore.sh <paquete>  -> reactiva solo ese paquete
set -euo pipefail
source "$(dirname "$0")/env.sh"

LIST="$(dirname "$0")/disabled.list"

reactivate() {
  local pkg="$1"
  echo "-> pm enable $pkg"
  adb_tv shell "pm enable --user 0 $pkg" || echo "   (aviso: no se pudo reactivar $pkg)"
}

if [ $# -ge 1 ]; then
  reactivate "$1"
  exit 0
fi

if [ ! -s "$LIST" ]; then
  echo "No hay nada en $LIST que reactivar."
  exit 0
fi

echo "Reactivando los paquetes de $LIST:"
while read -r pkg; do
  [ -n "$pkg" ] || continue
  case "$pkg" in \#*) continue ;; esac
  reactivate "$pkg"
done < "$LIST"

echo
echo "--- Estado actual de paquetes desactivados ---"
adb_tv shell "pm list packages -d"
