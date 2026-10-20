#!/usr/bin/env bash
# Aplica el debloat completo al stick: desactiva los paquetes de
# tools/disabled.list por lotes y desinstala los marcados con prefijo !.
#
#   ./tools/apply-debloat.sh              # aplica (pregunta antes)
#   ./tools/apply-debloat.sh --yes        # sin preguntas
#   ./tools/apply-debloat.sh --dry-run    # solo muestra lo que haria
#
# Antes de tocar nada hace un backup (tools/backup.sh) y comprueba entre
# lotes que el launcher sigue en pie. Es reversible: ./tools/restore.sh
set -euo pipefail
cd "$(dirname "$0")/.."
source tools/env.sh

ASSUME_YES=0
DRY_RUN=0
for arg in "$@"; do
  case "$arg" in
    --yes) ASSUME_YES=1 ;;
    --dry-run) DRY_RUN=1 ;;
    *) echo "opcion desconocida: $arg"; exit 1 ;;
  esac
done

LIST="tools/disabled.list"
BATCH_SIZE=5

say()  { printf '\n\033[1m%s\033[0m\n' "$*"; }
info() { printf '   %s\n' "$*"; }
warn() { printf '   \033[33m! %s\033[0m\n' "$*"; }
die()  { printf '\033[31m%s\033[0m\n' "$*" >&2; exit 1; }

# ---------------------------------------------------------------- comprobaciones
say "1. Comprobaciones previas"

if [ "$DRY_RUN" -eq 1 ] || [ "$ASSUME_YES" -eq 1 ]; then
  :
else
  echo "   Se va a modificar el dispositivo. Es reversible (tools/restore.sh)."
  read -r -p "   ¿Continuar? [s/N] " answer
  case "$answer" in [sSyY]*) ;; *) die "cancelado" ;; esac
fi

state=$(adb_tv get-state 2>/dev/null || true)
[ "$state" = "device" ] || die "no hay ningun dispositivo conectado por ADB"

model=$(adb_tv shell getprop ro.product.model | tr -d '\r')
release=$(adb_tv shell getprop ro.build.version.release | tr -d '\r')
sdk=$(adb_tv shell getprop ro.build.version.sdk | tr -d '\r')
info "dispositivo: $model  (Android $release, API $sdk)"

# Si el setup guardo la red Wi-Fi, no hay que desactivar esa app: se quedaria
# sin red (y sin ADB) a mitad del proceso.
cname=$(adb_tv shell "dumpsys wifi | grep -o 'cname=[^ ]*'" 2>/dev/null | head -1 | tr -d '\r' || true)
if [ -n "$cname" ]; then
  info "red Wi-Fi guardada por: ${cname#cname=}"
  if [ "${cname#cname=}" = "com.google.android.tungsten.setupwraith" ]; then
    info "esa app NO se toca: desactivarla borraria la red y dejaria el stick sin ADB"
  fi
fi

# ---------------------------------------------------------------- lectura
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
done < "$LIST"

# ---------------------------------------------------------------- backup
say "2. Backup del estado actual"
if [ "$DRY_RUN" -eq 1 ]; then
  info "se haria: ./tools/backup.sh"
else
  ./tools/backup.sh | tail -3
fi

# ---------------------------------------------------------------- lotes
say "3. Desactivando $(( ${#disable_list[@]} )) paquetes en lotes de $BATCH_SIZE"

n=0
for pkg in "${disable_list[@]}"; do
  [ -n "$pkg" ] || continue
  if [ "$DRY_RUN" -eq 1 ]; then
    info "desactivaria: $pkg"
  else
    adb_tv shell "pm disable-user --user 0 $pkg" < /dev/null | sed 's/^/   /'
  fi
  n=$((n + 1))
  if [ $((n % BATCH_SIZE)) -eq 0 ] && [ "$DRY_RUN" -eq 0 ]; then
    alive=$(adb_tv shell "pidof com.google.android.tvlauncher" 2>/dev/null | tr -d '\r' || true)
    [ -n "$alive" ] || warn "el launcher de Google TV no corre ahora mismo: revisa el estado"
  fi
done

# ---------------------------------------------------------------- desinstalar
say "4. Desinstalando $(( ${#uninstall_list[@]} )) apps de usuario"
info "OJO: pm uninstall borra el APK de /data/app de forma asincrona."
info "     Para recuperarlas: pm install-existing (si el APK sigue ahi) o Play Store."
for pkg in "${uninstall_list[@]}"; do
  [ -n "$pkg" ] || continue
  if [ "$DRY_RUN" -eq 1 ]; then
    info "desinstalaria: $pkg"
  else
    adb_tv shell "pm uninstall --user 0 $pkg" < /dev/null | sed "s|^|   $pkg: |"
  fi
done

# ---------------------------------------------------------------- verificar
say "5. Verificacion"
if [ "$DRY_RUN" -eq 1 ]; then
  info "se comprobaria el estado con: ./tools/check.sh"
  exit 0
fi

info "paquetes desactivados ahora: $(adb_tv shell 'pm list packages -d' | wc -l | tr -d ' ')"
info "apps de usuario ahora: $(adb_tv shell 'pm list packages -3' | wc -l | tr -d ' ')"

crashes=$(adb_tv shell "logcat -d -b crash -t 60" 2>/dev/null | grep -c "FATAL" || true)
if [ "${crashes:-0}" != "0" ]; then
  warn "hay crashes recientes: logcat -d -b crash | tail -30"
else
  info "sin crashes recientes"
fi

info "reboot para comprobar que todo arranca: adb reboot"
say "Hecho. Para revertir: ./tools/restore.sh"
