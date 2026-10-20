#!/usr/bin/env bash
# Desactiva los paquetes de una lista de debloat.
#
#   ./tools/disable.sh                          # usa tools/disabled.list
#   ./tools/disable.sh examples/xiaomi-mitv-aesp0/debloat.txt
#   ./tools/disable.sh <fichero> <paquete>...   # añade paquetes a la lista
#
# Formato de la lista: un paquete por linea. Las lineas que empiezan por # son
# comentarios. Un prefijo ! marca un paquete que se DESINSTALA en vez de
# desactivarse (solo para apps de usuario: es la unica forma de liberar disco).
#
# Nada se borra del sistema: desactivar no ocupa espacio, y las apps de
# usuario desinstaladas se pueden recuperar con pm install-existing (si su
# APK sigue en /data/app) o reinstalando desde Play Store.
set -euo pipefail
cd "$(dirname "$0")/.."
source tools/env.sh

LIST_FILE=""
PACKAGES=()

for arg in "$@"; do
  if [ -z "$LIST_FILE" ] && [ -f "$arg" ]; then
    LIST_FILE="$arg"
  else
    PACKAGES+=("$arg")
  fi
done

[ -n "$LIST_FILE" ] || LIST_FILE="tools/disabled.list"
[ -f "$LIST_FILE" ] || { echo "no existe la lista: $LIST_FILE" >&2; exit 1; }

BATCH_SIZE=5
REGISTRY="tools/disabled.list"

say()  { printf '\n\033[1m%s\033[0m\n' "$*"; }
info() { printf '   %s\n' "$*"; }

# --------------------------------------------------------- lectura de la lista
disable_list=()
uninstall_list=()

while IFS= read -r line || [ -n "$line" ]; do
  line="${line%%$'\r'}"
  # Los comentarios en linea tambien se ignoran: "pkg  # nota"
  line="${line%%#*}"
  line="${line%"${line##*[![:space:]]}"}"
  case "$line" in
    '') continue ;;
  esac
  if [ "${line#!}" != "$line" ]; then
    uninstall_list+=("${line#!}")
  else
    disable_list+=("$line")
  fi
done < "$LIST_FILE"

# Los paquetes pasados por linea de orden mandan sobre la lista.
for p in "${PACKAGES[@]}"; do
  [ -n "$p" ] || continue
  disable_list+=("$p")
done

say "Lista: $LIST_FILE"
info "desactivar: ${#disable_list[@]}   desinstalar: ${#uninstall_list[@]}"

# ------------------------------------------------------------------ desactivar
if [ ${#disable_list[@]} -gt 0 ]; then
  say "Desactivando (pm disable-user --user 0), en lotes de $BATCH_SIZE"
  n=0
  for pkg in "${disable_list[@]}"; do
    [ -n "$pkg" ] || continue
    adb_tv shell "pm disable-user --user 0 $pkg" < /dev/null | sed 's/^/   /'
    grep -qxF "$pkg" "$REGISTRY" 2>/dev/null || echo "$pkg" >> "$REGISTRY"
    n=$((n + 1))
    if [ $((n % BATCH_SIZE)) -eq 0 ]; then
      alive=$(adb_tv shell "pidof com.google.android.tvlauncher" 2>/dev/null | tr -d '\r' || true)
      [ -n "$alive" ] || info "aviso: el launcher de Google TV no corre ahora mismo"
    fi
  done
fi

# ---------------------------------------------------------------- desinstalar
if [ ${#uninstall_list[@]} -gt 0 ]; then
  say "Desinstalando apps de usuario (pm uninstall --user 0)"
  info "OJO: el borrado del APK es asincrono. Para revertir:"
  info "     pm install-existing <paquete> (si el APK sigue ahi) o Play Store."
  for pkg in "${uninstall_list[@]}"; do
    [ -n "$pkg" ] || continue
    adb_tv shell "pm uninstall --user 0 $pkg" < /dev/null | sed "s|^|   $pkg: |"
    grep -qxF "$pkg" "$REGISTRY" 2>/dev/null || echo "$pkg" >> "$REGISTRY"
  done
fi

say "Hecho"
info "desactivados ahora: $(adb_tv shell 'pm list packages -d' | wc -l | tr -d ' ')"
info "apps de usuario ahora: $(adb_tv shell 'pm list packages -3' | wc -l | tr -d ' ')"
info "para revertir: ./tools/restore.sh"
