#!/bin/bash
# Vygeneruje ikony aplikace pro jpackage ze zdroje modules/guiModule/src/main/resources/img/appIcon/appIcon.svg:
#   appIcon.icns (macOS, iconutil), appIcon.ico (Windows, 16 az 256 px), appIcon.iconset/*.png (zdroj pro icns)
#   a appIcon-256.png (Linux, jpackage --icon). Velikosti 16 a 24 px (iconset i .ico) se renderuji ze zjednoduseneho
#   appIcon-small.svg (bez prstence, silnejsi zatrzitko), aby byly citelne.
# Potrebuje ImageMagick s podporou SVG pres librsvg (vlastni renderer ImageMagicku kresli zatrzitko spatne; na macOS
# balicek imagemagick-full, jinak lze cestu zadat promennou MAGICK=...) a na macOS iconutil. Spoustet z korene repa: tools/release/make-icons.sh
set -euo pipefail
cd "$(dirname "$0")/../.."
MAGICK="${MAGICK:-magick}"
if ! "$MAGICK" -list format | grep -q "RSVG"; then
    FULL="$(brew --prefix imagemagick-full 2>/dev/null || true)/bin/magick"
    if [ -x "$FULL" ] && "$FULL" -list format | grep -q "RSVG"; then
        MAGICK="$FULL"
    else
        echo "ImageMagick bez podpory SVG pres librsvg (RSVG), nainstaluj imagemagick-full nebo nastav MAGICK" >&2
        exit 1
    fi
fi
DIR=modules/guiModule/src/main/resources/img/appIcon
SVG="$DIR/appIcon.svg"
SET="$DIR/appIcon.iconset"
SVG_SMALL="$DIR/appIcon-small.svg"
# velikost 24 px a mensi: zjednodusena varianta
render() {
    local src="$SVG"
    if [ "$1" -le 24 ]; then src="$SVG_SMALL"; fi
    "$MAGICK" -background none -density 1024 "$src" -resize "$1x$1" "$2"
}

rm -rf "$SET"; mkdir -p "$SET"
for s in 16 32 128 256 512; do
    render "$s" "$SET/icon_${s}x${s}.png"
    render "$((s*2))" "$SET/icon_${s}x${s}@2x.png"
done

render 48 "$SET/icon_48x48.png"      # ikona okna aplikace (Main.addIconIfFound), nepatri k iconutil, ale nevadi mu
render 256 "$DIR/appIcon-256.png"

if command -v iconutil >/dev/null; then
    iconutil -c icns "$SET" -o "$DIR/appIcon.icns"
else
    echo "iconutil neni k dispozici (jen macOS), appIcon.icns nezmenen" >&2
fi

TMP=$(mktemp -d)
for s in 16 24 32 48 64 128 256; do render "$s" "$TMP/$s.png"; done
"$MAGICK" "$TMP"/16.png "$TMP"/24.png "$TMP"/32.png "$TMP"/48.png "$TMP"/64.png "$TMP"/128.png "$TMP"/256.png "$DIR/appIcon.ico"
rm -rf "$TMP"
ls -la "$DIR" "$SET"
