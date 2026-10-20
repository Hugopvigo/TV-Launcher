#!/usr/bin/env bash
# Sanea los ficheros del repo antes de publicarlos: sustituye los
# identificadores unicos del dispositivo (MAC, android_id, numero de serie)
# por marcadores. Es reversible: el backup original esta en el commit
# previo de la sesion y en la copia local.
set -euo pipefail
cd "$(dirname "$0")/.."

REDACT="<REDACTADO>"

echo "== sustituyo identificadores en los textos del backup =="
for f in backup/*/*.txt; do
  [ -f "$f" ] || continue
  before=$(md5sum "$f" | cut -d' ' -f1)
  sed -i -E \
    -e 's/([0-9a-fA-F]{2}:){5}[0-9a-fA-F]{2}/'"$REDACT"'/g' \
    -e 's/android_id=[0-9a-fA-F]+/android_id='"$REDACT"'/g' \
    -e 's/26919700000118211/'"$REDACT"'/g' \
    -e 's/267dad9cebd1cb452bd6d645097f86678f55a957/'"$REDACT"'/g' \
    "$f"
  after=$(md5sum "$f" | cut -d' ' -f1)
  [ "$before" != "$after" ] && echo "   saneado: $f"
done

echo
echo "== comprobacion: queda algun identificador? =="
if grep -rqE "([0-9a-fA-F]{2}:){5}[0-9a-fA-F]{2}|android_id=[0-9a-f]{8,}|26919700000118211" backup/ 2>/dev/null; then
  echo "   ¡OJO! quedan identificadores:"
  grep -rnE "([0-9a-fA-F]{2}:){5}[0-9a-fA-F]{2}|android_id=[0-9a-f]{8,}" backup/ | head -5
else
  echo "   limpio: no queda ningun identificador"
fi
