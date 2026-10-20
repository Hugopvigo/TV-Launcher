#!/usr/bin/env bash
# Build del launcher desde WSL: delega en build.bat (toolchain de Windows).
set -euo pipefail
cd "$(dirname "$0")"
printf 'sdk.dir=/mnt/c/Users/Hugop/AppData/Local/Android/Sdk\n' > local.properties
cmd.exe /c "C:\\Users\\Hugop\\Dev\\firestick-debloat\\launcher\\build.bat" "$@" 2>&1
