#!/usr/bin/env bash
# Restaura el stick a como estaba antes del debloat.
#
#   ./tools/restore.sh                    # usa tools/disabled.list
#   ./tools/restore.sh <fichero-lista>     # usa otra lista
#   ./tools/restore.sh <paquete>          # reactiva un paquete suelto
#
# La lista es la misma que se aplico: las lineas con prefijo ! son las que
# se desinstalaron y se recuperan con pm install-existing (si el APK sigue
# en /data/app) o reinstalandolas desde Play Store.
set -euo pipefail
cd "$(dirname "$0")/.."
source tools/env.sh

LIST_FILE="tools/disabled.list"
PKG=""

for arg in "$@"; do
  if [ -z "$PKG" ] && [ -f "$arg" ]; then
    LIST_FILE="$arg"
  elif [ -z "$PKG" ]; then
    PKG="$arg"
  fi
done

say()  { printf '\n\033[1m%s\033[0m\n' "$*"; }
info() { printf '   %s\n' "$*"; }

if [ -n "$PKG" ]; then
  say "Reactivando $PKG"
  adb_tv shell "pm enable --user 0 $PKG" < /dev/null | sed 's/^/   /'
  exit 0
fi

[ -f "$LIST_FILE" ] || { echo "no existe la lista: $LIST_FILE" >&2; exit 1; }

# ------------------------------------------------------- lectura de la lista
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

# --------------------------------------------------------------- reactivar
if [ ${#disable_list[@]} -gt 0 ]; then
  say "1. Reactivando los paquetes desactivados"
  for pkg in "${disable_list[@]}"; do
    [ -n "$pkg" ] || continue
    adb_tv shell "pm enable --user 0 $pkg" < /dev/null | sed 's/^/   /'
  done
fi

# -------------------------------------------------------------- reinstalar
if [ ${#uninstall_list[@]} -gt 0 ]; then
  say "2. Recuperando las apps desinstaladas"
  info "Si el APK ya no esta en /data/app, hay que instalarlas desde Play Store."
  for pkg in "${uninstall_list[@]}"; do
    [ -n "$pkg" ] || continue
    out=$(adb_tv shell "pm install-existing --user 0 $pkg" < /dev/null | tr -d '\r' || true)
    case "$out" in
      *"installed for user"*) info "$pkg: recuperada" ;;
      *) info "$pkg: ya no esta en el sistema -> reinstalar desde Play Store" ;;
    esac
  done
fi

# ------------------------------------------------- devolver el launcher original
say "3. Devolviendo el launcher de fabrica"
adb_tv shell "pm enable --user 0 com.google.android.tvlauncher" < /dev/null | sed 's/^/   /'
adb_tv shell "cmd package set-home-activity com.google.android.tvlauncher/.MainActivity" < /dev/null | sed 's/^/   /'

say "Estado final"
info "desactivados ahora: $(adb_tv shell 'pm list packages -d' | wc -l | tr -d ' ')"
info "apps de usuario ahora: $(adb_tv shell 'pm list packages -3' | wc -l | tr -d ' ')"
info "HOME: $(adb_tv shell 'dumpsys package preferred-xml' | grep -oE 'com\.[a-z.]+/[A-Za-z.]+' | head -1 | tr -d '\r')"
