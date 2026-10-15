#!/bin/bash
# Běží uvnitř kontejneru debian linux/amd64, spouští ho build-assets.sh.
# Vstup: /w/src (zdrojáky z tagu), proměnná VERSION (např. 2.8-beta).
# Výstup: /w/out/{cliModule-<verze>.jar, guiModule-<verze>.jar, img/KomplexniValidator/}
set -euxo pipefail

# Kontejner běží jako root; soubory v připojeném /w vrátit hostitelskému uživateli, aby je šlo smazat
# (na Linuxu, Docker Desktop na Macu vlastnictví mapuje sám).
trap 'chown -R "${HOST_UID:-0}:${HOST_GID:-0}" /w' EXIT

# JDK 17 s JavaFX, aby runtime portable verze obsahoval moduly javafx.* (jako ve v2.7)
LIBERICA_URL="${LIBERICA_URL:-https://github.com/bell-sw/Liberica/releases/download/17.0.20.1+1/bellsoft-jdk17.0.20.1+1-linux-amd64-full.tar.gz}"

apt-get update -qq
apt-get install -y -qq curl binutils >/dev/null   # binutils: objcopy pro jlink --strip-debug
curl -sL -o /tmp/jdk.tgz "$LIBERICA_URL"
mkdir -p /opt/jdk
tar -xzf /tmp/jdk.tgz -C /opt/jdk --strip-components=1
export JAVA_HOME=/opt/jdk PATH=/opt/jdk/bin:$PATH
export GRADLE_USER_HOME=/w/gradle-home

cd /w/src
./gradlew --no-daemon -q :modules:cliModule:jar :modules:guiModule:jar :modules:guiModule:shadowJar -x test

LIBS=modules/guiModule/build/libs
cp -R modules/sharedModule/src/main/resources/nkp/pspValidator/shared/validatorConfig "$LIBS/"
# jpackage bere jen čísla: 2.8-beta -> 2.8
APP_VERSION="$(echo "$VERSION" | sed -E 's/[^0-9]+/./g; s/^\.+|\.+$//g')"

mkdir -p /w/out
jpackage --type app-image --dest /w/out/img --name KomplexniValidator --input "$LIBS" \
    --main-jar "guiModule-$VERSION-shadow.jar" --main-class nkp.pspValidator.gui.Launcher \
    --app-version "$APP_VERSION" \
    --icon modules/guiModule/src/main/resources/img/appIcon/appIcon.iconset/icon_48x48.png

cp "modules/cliModule/build/libs/cliModule-$VERSION.jar" "$LIBS/guiModule-$VERSION.jar" /w/out/
