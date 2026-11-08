#!/bin/bash
# Vygeneruje ikony aplikace pro jpackage ze zdroje modules/guiModule/src/main/resources/img/appIcon/appIcon.svg:
#   appIcon.icns (macOS, iconutil), appIcon.ico (Windows, 16 az 256 px), appIcon.iconset/*.png (zdroj pro icns)
#   a appIcon-256.png (Linux, jpackage --icon).
# Potrebuje ImageMagick (magick) a na macOS iconutil. Spoustet z korene repa: tools/release/make-icons.sh
set -euo pipefail
cd "$(dirname "$0")/../.."
DIR=modules/guiModule/src/main/resources/img/appIcon
SVG="$DIR/appIcon.svg"
SET="$DIR/appIcon.iconset"
render() { magick -background none -density 1024 "$SVG" -resize "$1x$1" "$2"; }

rm -rf "$SET"; mkdir -p "$SET"
for s in 16 32 128 256 512; do
    render "$s" "$SET/icon_${s}x${s}.png"
    render "$((s*2))" "$SET/icon_${s}x${s}@2x.png"
done

render 256 "$DIR/appIcon-256.png"

if command -v iconutil >/dev/null; then
    iconutil -c icns "$SET" -o "$DIR/appIcon.icns"
else
    echo "iconutil neni k dispozici (jen macOS), appIcon.icns nezmenen" >&2
fi

TMP=$(mktemp -d)
for s in 16 24 32 48 64 128 256; do render "$s" "$TMP/$s.png"; done
magick "$TMP"/16.png "$TMP"/24.png "$TMP"/32.png "$TMP"/48.png "$TMP"/64.png "$TMP"/128.png "$TMP"/256.png "$DIR/appIcon.ico"
rm -rf "$TMP"
ls -la "$DIR" "$SET"
