#!/bin/bash
# Sestaví assets GitHub release z tagu: CLI zip, GUI zip a Linux portable (viz release.md).
# Windows assets se stavějí zvlášť na Windows.
#
# Použití: tools/release/build-assets.sh [tag] [výstupní adresář]
#   tag       výchozí v<appVersion z gradle.properties>, např. v2.8-beta
#   výstup    výchozí build/release/<tag>
#
# Potřebuje git, zip a Docker (sestavení běží v kontejneru linux/amd64, na Apple Silicon přes emulaci).
# Zdrojáky se berou z tagu přes git archive, necommitované změny se do assets nedostanou.
# Testy se v kontejneru nespouštějí, pusťte před vydáním ./gradlew build.
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
SCRIPT_DIR="$REPO_ROOT/tools/release"
cd "$REPO_ROOT"

APP_VERSION="$(grep -E '^appVersion=' gradle.properties | cut -d= -f2)"
TAG="${1:-v$APP_VERSION}"
VERSION="${TAG#v}"
OUT="${2:-$REPO_ROOT/build/release/$TAG}"

git rev-parse -q --verify "refs/tags/$TAG" >/dev/null || { echo "tag $TAG neexistuje" >&2; exit 1; }
docker info >/dev/null 2>&1 || { echo "Docker neběží" >&2; exit 1; }

mkdir -p "$OUT"
OUT="$(cd "$OUT" && pwd)"   # absolutní cesta: bind mount Dockeru i zip po cd
WORK="$OUT/.work"

echo "== $TAG -> $OUT"
rm -rf "$WORK/src" "$WORK/out" "$WORK/pack"
mkdir -p "$WORK/src" "$WORK/pack" "$WORK/gradle-home"
git archive "$TAG" | tar -x -C "$WORK/src"

# Gradle cache zůstává ve $WORK/gradle-home, další běh je rychlejší.
docker run --rm --platform linux/amd64 \
    -v "$WORK:/w" -v "$SCRIPT_DIR/docker-build-linux.sh:/docker-build-linux.sh:ro" \
    -e VERSION="$VERSION" -e HOST_UID="$(id -u)" -e HOST_GID="$(id -g)" ${LIBERICA_URL:+-e LIBERICA_URL="$LIBERICA_URL"} \
    debian:bookworm-slim /docker-build-linux.sh

CFG="$WORK/src/modules/sharedModule/src/main/resources/nkp/pspValidator/shared/validatorConfig"

# CLI zip: KomplexniValidatorCLI-<tag>/{KomplexniValidatorCLI.jar, validatorConfig/}
N="KomplexniValidatorCLI-$TAG"
mkdir -p "$WORK/pack/$N"
cp "$WORK/out/cliModule-$VERSION.jar" "$WORK/pack/$N/KomplexniValidatorCLI.jar"
cp -R "$CFG" "$WORK/pack/$N/validatorConfig"

# GUI zip: KomplexniValidatorGUI-<tag>/{KomplexniValidatorGUI.jar, validatorConfig/}
N="KomplexniValidatorGUI-$TAG"
mkdir -p "$WORK/pack/$N"
cp "$WORK/out/guiModule-$VERSION.jar" "$WORK/pack/$N/KomplexniValidatorGUI.jar"
cp -R "$CFG" "$WORK/pack/$N/validatorConfig"

# Linux portable: jpackage app-image přejmenovaný na KomplexniValidatorGUI-<tag>_Linux_x64_portable
N="KomplexniValidatorGUI-${TAG}_Linux_x64_portable"
cp -R "$WORK/out/img/KomplexniValidator" "$WORK/pack/$N"

for N in "KomplexniValidatorCLI-$TAG" "KomplexniValidatorGUI-$TAG" "KomplexniValidatorGUI-${TAG}_Linux_x64_portable"; do
    rm -f "$OUT/$N.zip"
    (cd "$WORK/pack" && zip -q -r -X -y "$OUT/$N.zip" "$N")
done

echo "== hotovo"
ls -l "$OUT"/*.zip
