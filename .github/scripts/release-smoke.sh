#!/usr/bin/env bash
# Recorrido guiado de la versión de release (con R8) en el emulador:
# pulsa botones por su texto, completa el alta y visita las pestañas, guardando capturas.
set -u
OUT=${1:-report-preview}
PKG=com.callankan.poloapp
mkdir -p "$OUT"

shot() { adb exec-out screencap -p > "$OUT/release_$1.png" 2>/dev/null || true; }

alive() {
  if [ "$(adb shell echo ok 2>/dev/null | tr -d '\r')" != ok ]; then echo "Emulador desconectado tras: $1 (no es un fallo de la app)"; exit 2; fi
  if ! adb shell pidof "$PKG" >/dev/null 2>&1; then echo "La app se ha cerrado tras: $1"; exit 1; fi
}

dump_ui() {
  adb shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1
  adb shell cat /sdcard/ui.xml 2>/dev/null | tr '>' '\n'
}

center_of() {
  local bounds
  bounds=$(echo "$1" | grep -oE 'bounds="\[[0-9]+,[0-9]+\]\[[0-9]+,[0-9]+\]"' | grep -oE '[0-9]+' | tr '\n' ' ')
  [ -n "$bounds" ] || return 1
  # shellcheck disable=SC2086
  set -- $bounds
  echo "$(( ($1 + $3) / 2 )) $(( ($2 + $4) / 2 ))"
}

# El emulador de CI va justo de recursos y a veces el propio launcher del sistema
# muestra "X isn't responding" encima de la app. Ese diálogo no es de la app: se descarta con "Wait".
dismiss_system_dialog() {
  local ui node xy
  ui=$1
  echo "$ui" | grep -qE "isn't responding|no responde" || return 1
  node=$(echo "$ui" | grep -E 'text="(Wait|Esperar)"' | head -1)
  xy=$(center_of "$node") || { adb shell input keyevent 4; return 0; }
  echo "Descarto un diálogo del sistema: $(echo "$ui" | grep -oE "text=\"[^\"]*(isn't responding|no responde)\"" | head -1)"
  # shellcheck disable=SC2086
  adb shell input tap $xy
  sleep 2
}

# tap_text TEXTO [first|last]: "last" sirve para la barra inferior, que va al final del árbol.
tap_text() {
  local ui node xy attempt pick=${2:-first}
  for attempt in 1 2 3 4 5 6; do
    ui=$(dump_ui)
    if dismiss_system_dialog "$ui"; then continue; fi
    node=$(echo "$ui" | grep -F "text=\"$1\"" | if [ "$pick" = last ]; then tail -1; else head -1; fi)
    [ -n "$node" ] || node=$(echo "$ui" | grep -F -e "content-desc=\"$1\"" -e "hint=\"$1\"" | head -1)
    if xy=$(center_of "$node"); then
      # shellcheck disable=SC2086
      adb shell input tap $xy
      sleep 2
      return 0
    fi
    sleep 2
  done
  echo "No encuentro '$1' en pantalla"
  return 1
}

step() {
  local label=$1; shift
  "$@" || { shot "error_$label"; echo "Fallo en el paso: $label"; exit 1; }
  alive "$label"
  shot "$label"
}

adb shell settings put global hide_error_dialogs 1 >/dev/null 2>&1 || true
adb shell pm clear "$PKG" >/dev/null 2>&1 || true
adb shell am start -W -n "$PKG/.MainActivity"
sleep 6
alive "arranque"
dismiss_system_dialog "$(dump_ui)" || true
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
step 09_combustible tap_text "Combustible" last
step 10_taller tap_text "Taller" last
step 11_finanzas tap_text "Finanzas" last
step 12_mas tap_text "Más" last
step 13_informe tap_text "Informe PDF"
sleep 4
shot 13_informe
alive "informe"
echo "Recorrido de release completado sin cierres"
