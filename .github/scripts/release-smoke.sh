#!/usr/bin/env bash
# Recorrido guiado de la versión de release (con R8) en el emulador:
# pulsa botones por su texto, completa el alta y visita las pestañas, guardando capturas.
set -u
OUT=${1:-report-preview}
PKG=com.callankan.poloapp
mkdir -p "$OUT"

shot() { adb exec-out screencap -p > "$OUT/release_$1.png" 2>/dev/null || true; }

alive() {
  if ! adb get-state >/dev/null 2>&1; then echo "Emulador desconectado tras: $1"; exit 2; fi
  if ! adb shell pidof "$PKG" >/dev/null 2>&1; then echo "La app se ha cerrado tras: $1"; exit 1; fi
}

tap_text() {
  adb shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1
  local node bounds
  node=$(adb shell cat /sdcard/ui.xml | tr '>' '\n' | grep -F "text=\"$1\"" | head -1)
  bounds=$(echo "$node" | grep -oE 'bounds="\[[0-9]+,[0-9]+\]\[[0-9]+,[0-9]+\]"' | grep -oE '[0-9]+' | tr '\n' ' ')
  if [ -z "$bounds" ]; then echo "No encuentro '$1' en pantalla"; return 1; fi
  # shellcheck disable=SC2086
  set -- $bounds
  adb shell input tap $(( ($1 + $3) / 2 )) $(( ($2 + $4) / 2 ))
  sleep 2
}

step() {
  local label=$1; shift
  "$@" || { shot "error_$label"; echo "Fallo en el paso: $label"; exit 1; }
  alive "$label"
  shot "$label"
}

adb shell pm clear "$PKG" >/dev/null 2>&1 || true
adb shell am start -W -n "$PKG/.MainActivity"
sleep 6
alive "arranque"
shot 01_bienvenida

step 02_coche tap_text "Empezar"
step 03_preset tap_text "Volkswagen Polo Mk5"
step 04_identidad tap_text "Continuar"
step 05_situacion tap_text "Continuar"
step 06_km tap_text "Km actuales"
adb shell input text 147980
adb shell input keyevent 4
sleep 1
step 07_plan tap_text "Continuar"
step 08_dashboard tap_text "Crear mi garaje"
sleep 3
shot 08_dashboard
step 09_combustible tap_text "Combustible"
step 10_taller tap_text "Taller"
step 11_finanzas tap_text "Finanzas"
step 12_mas tap_text "Más"
step 13_informe tap_text "Informe PDF"
sleep 4
shot 13_informe
alive "informe"
echo "Recorrido de release completado sin cierres"
