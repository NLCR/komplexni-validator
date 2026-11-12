#!/bin/bash
# Paralelní validace: spustí referenční VEP 2.4 a KV nad všemi balíčky ve vzorovém adresáři
# ve dvou konfiguracích nástrojů (off = veraPDF a EPUBCheck vypnuté, on = zapnuté).
# Textové protokoly (verbosity 3) ukládá do <OUT>/runs/<validátor>-<konfigurace>/<balíček>.txt.
#
# Použití: tools/parallel-validation/run-samples.sh <OUT> [konfigurace ...]
#   konfigurace: off, on (výchozí obě)
# Proměnné prostředí (výchozí hodnoty v závorce):
#   SAMPLES   adresář s rozbalenými balíčky (../official-samples/epub/unpacked vedle repa)
#   VEP_JAR   jar VEP 2.4 (.local/vep-2.4/cliModule-2.4.jar)
#   VEP_CFG   konfigurace VEP (.local/vep-2.4/validatorConfig)
#   KV_JAR    jar KV CLI (modules/cliModule/build/libs/cliModule-<verze>.jar, sestavit
#             ./gradlew :modules:cliModule:jar -x test)
#   KV_CFG    konfigurace KV (modules/sharedModule/src/main/resources/nkp/pspValidator/shared/validatorConfig)
#   JAVA      java 21 ($(/usr/libexec/java_home -v 21)/bin/java)
#   VERAPDF   adresář veraPDF ($(brew --prefix)/bin)
#   EPUBCHECK adresář EPUBCheck ($(brew --prefix epubcheck)/libexec)
#   JOBS      počet souběžných běhů (6)
#   FORCED    balíčky s vynucenou verzí e-periodik 2.3, regulární výraz na relativní cestu
#             (e-periodika/RDA, vzorky NK mají chybnou metadataversion 1.7.1)
#   ONLY_FORCED neprázdná = jen běhy s vynucenou verzí (doplnění do existujícího OUT)
#   FRESH     neprázdná = spustit i běhy, které už v OUT doběhly (jinak se přeskočí, lze tak navázat)
#
# Resolver URN:NBN: KV dostane --urnnbn-resolver-url http://127.0.0.1:9, VEP má adresu natvrdo
# (https://resolver.nkp.cz/api/v5), proto mu se nastaví HTTP(S) proxy 127.0.0.1:9. Oba tak skončí
# „Connection refused“ se stejnou hláškou a nikdo nejde do sítě.
set -u
REPO=$(cd "$(dirname "$0")/../.." && pwd)
OUT=${1:?Použití: run-samples.sh <OUT> [off] [on]}
shift
CONFIGS=${*:-off on}
SAMPLES=${SAMPLES:-$(cd "$REPO/.." && pwd)/official-samples/epub/unpacked}
VEP_JAR=${VEP_JAR:-$REPO/.local/vep-2.4/cliModule-2.4.jar}
VEP_CFG=${VEP_CFG:-$REPO/.local/vep-2.4/validatorConfig}
KV_JAR=${KV_JAR:-$(ls "$REPO"/modules/cliModule/build/libs/cliModule-*.jar 2>/dev/null | head -1)}
KV_CFG=${KV_CFG:-$REPO/modules/sharedModule/src/main/resources/nkp/pspValidator/shared/validatorConfig}
JAVA=${JAVA:-$(/usr/libexec/java_home -v 21)/bin/java}
VERAPDF=${VERAPDF:-$(brew --prefix)/bin}
EPUBCHECK=${EPUBCHECK:-$(brew --prefix epubcheck)/libexec}
JOBS=${JOBS:-6}
FORCED=${FORCED:-e-periodika/RDA}
ONLY_FORCED=${ONLY_FORCED:-}
FRESH=${FRESH:-}
export PATH="$(brew --prefix)/bin:$PATH"

for f in "$VEP_JAR" "$KV_JAR" "$JAVA"; do
  [ -e "$f" ] || { echo "Chybí $f" >&2; exit 1; }
done
mkdir -p "$OUT/runs"
OUT=$(cd "$OUT" && pwd)

# seznam balíčků = adresáře se souborem info*.xml
find "$SAMPLES" -type f -iname 'info*.xml' -print0 | xargs -0 -n1 dirname | sort > "$OUT/packages.txt"

# úlohy: validátor<TAB>konfigurace<TAB>název<TAB>cesta<TAB>vynucená verze
JOBFILE="$OUT/jobs.tsv"
: > "$JOBFILE"
while read -r p; do
  rel=${p#"$SAMPLES"/}
  name=$(echo "$rel" | tr '/' '_')
  for c in $CONFIGS; do
    for v in vep kv; do
      [ -z "$ONLY_FORCED" ] && printf '%s\t%s\t%s\t%s\t%s\n' "$v" "$c" "$name" "$p" "" >> "$JOBFILE"
      if echo "$rel" | grep -Eq "$FORCED"; then
        printf '%s\t%s\t%s\t%s\t%s\n' "$v" "$c" "${name}__forced-2.3" "$p" "2.3" >> "$JOBFILE"
      fi
    done
  done
done < "$OUT/packages.txt"
# dlouhé běhy (EPUB se zapnutými nástroji) napřed, ať se paralelismus využije
{ grep $'\ton\t.*EPUB' "$JOBFILE"; grep -v $'\ton\t.*EPUB' "$JOBFILE"; } > "$JOBFILE.sorted" && mv "$JOBFILE.sorted" "$JOBFILE"

run_one() {
  IFS=$'\t' read -r v c name p forced <<< "$1"
  dir="$OUT/runs/$v-$c"; mkdir -p "$dir"
  # hotové běhy (existuje .time s rc=0) přeskočit, pokud není FRESH
  if [ -z "$FRESH" ] && grep -q '^rc=0 ' "$dir/$name.time" 2>/dev/null; then return; fi
  tmp="$OUT/tmp/$v-$c-$name"; mkdir -p "$tmp"
  if [ "$c" = on ]; then tools=(--verapdf-path "$VERAPDF" --epubcheck-path "$EPUBCHECK")
  else tools=(--disable-verapdf --disable-epubcheck); fi
  if [ "$v" = vep ]; then
    cmd=("$JAVA" -Dhttp.proxyHost=127.0.0.1 -Dhttp.proxyPort=9 -Dhttps.proxyHost=127.0.0.1 -Dhttps.proxyPort=9
         -jar "$VEP_JAR" --config-dir "$VEP_CFG")
    [ -n "$forced" ] && cmd+=(--forced-dmf-eper-version "$forced")
  else
    cmd=("$JAVA" -jar "$KV_JAR" --config-dir "$KV_CFG"
         --urnnbn-resolver-url http://127.0.0.1:9 --urnnbn-resolver-allow-http)
    [ -n "$forced" ] && cmd+=(--forced-dmf-epe-version "$forced")
  fi
  start=$(date +%s)
  "${cmd[@]}" -a VALIDATE_PSP --psp "$p" -t "$tmp" -v 3 "${tools[@]}" > "$dir/$name.txt" 2> "$dir/$name.err"
  rc=$?
  echo "rc=$rc sec=$(( $(date +%s) - start ))" > "$dir/$name.time"
  rm -rf "$tmp"
  echo "$(date +%T) $v-$c $name rc=$rc $(( $(date +%s) - start ))s"
}
export -f run_one
export OUT FRESH VEP_JAR VEP_CFG KV_JAR KV_CFG JAVA VERAPDF EPUBCHECK

echo "Úloh: $(wc -l < "$JOBFILE"), souběžně $JOBS, výstup $OUT"
t0=$(date +%s)
# BSD xargs má u -I limit 255 bajtů na argument, proto -n1 a $1
tr '\n' '\0' < "$JOBFILE" | xargs -0 -n1 -P "$JOBS" bash -c 'run_one "$1"' _
echo "Hotovo za $(( ($(date +%s) - t0) / 60 )) min"
