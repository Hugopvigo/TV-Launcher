#!/usr/bin/env bash
# Descarga las tipografias Outfit (OFL) desde la API de Google Fonts.
set -euo pipefail
cd "$(dirname "$0")/../app/src/main/res/font"

CSS=$(curl -sL --max-time 60 -A "Mozilla/4.0" \
  "https://fonts.googleapis.com/css2?family=Outfit:wght@200;300;400;500;600&display=swap")

mapfile -t URLS < <(printf '%s\n' "$CSS" | grep -oE "https://[^)]*\.ttf")
NAMES=(outfit_extralight outfit_light outfit_regular outfit_medium outfit_semibold)

for i in "${!URLS[@]}"; do
  name="${NAMES[$i]}"
  code=$(curl -sL --max-time 60 -o "${name}.ttf" -w "%{http_code}" "${URLS[$i]}")
  size=$(stat -c%s "${name}.ttf" 2>/dev/null || echo 0)
  echo "$name -> HTTP $code  ${size} bytes"
  if [ "$code" != "200" ] || [ "$size" -lt 10000 ]; then
    rm -f "${name}.ttf"
    echo "   (fallo, se omite)"
  fi
done
