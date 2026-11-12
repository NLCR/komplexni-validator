# Paralelní validace VEP 2.4 × KV

Nástroj pro krok P6 portu e-publikací: spustí referenční Validátor e-publikací (VEP 2.4) a Komplexní
validátor (KV) nad stejnými balíčky a porovná výsledky pravidlo po pravidle.

## Soubory

- `run-samples.sh`: spustí oba validátory nad všemi balíčky ve vzorovém adresáři (balíček = adresář
  se souborem `info*.xml`), ve dvou konfiguracích nástrojů, paralelně. Ukládá textové protokoly
  s verbosity 3.
- `compare-vep.py`: porovná protokoly po pravidlech a zapíše `summary.json`, `tabulka.md` a `rozdily.md`.
- `known.py`: tabulka známých rozdílů (vzory hlášek a jejich kategorie). Nový známý rozdíl se přidává sem.

## Předpoklady

- Java 21 (VEP 2.4 je přeložený pro 21), `$(/usr/libexec/java_home -v 21)`.
- VEP 2.4: `.local/vep-2.4/cliModule-2.4.jar` a `.local/vep-2.4/validatorConfig/` (viz README tamtéž).
- KV CLI: `./gradlew :modules:cliModule:jar -x test`.
- Vzorky: `../official-samples/epub/unpacked/` vedle repa (28 balíčků NK).
- Pro konfiguraci `on`: veraPDF (`$(brew --prefix)/bin`) a EPUBCheck (`$(brew --prefix epubcheck)/libexec`).

Cesty jdou přepsat proměnnými prostředí (`SAMPLES`, `VEP_JAR`, `VEP_CFG`, `KV_JAR`, `KV_CFG`, `JAVA`,
`VERAPDF`, `EPUBCHECK`, `JOBS`), popis je v hlavičce `run-samples.sh`.

## Spuštění

```bash
OUT=/cesta/mimo/repo/p6
tools/parallel-validation/run-samples.sh "$OUT"            # obě konfigurace (off, on)
tools/parallel-validation/run-samples.sh "$OUT" off        # jen bez nástrojů
python3 tools/parallel-validation/compare-vep.py "$OUT"    # porovnání, výstup do $OUT
python3 tools/parallel-validation/compare-vep.py "$OUT" --report-dir jiny/adresar \
    --exclude-info --exclude-rule MODS_IDENTIFIERS_URN_NBN_REGISTERED
```

`run-samples.sh` přeskočí běhy, které už v `OUT` doběhly (soubor `.time` s `rc=0`); `FRESH=1` je spustí znovu.
Balíčky e-periodik 2.3 RDA (`FORCED`, výchozí `e-periodika/RDA`) se navíc spustí s vynucenou verzí 2.3
(`__forced-2.3` v názvu), protože vzorky NK mají v `info.xml` chybnou `metadataversion` 1.7.1.

Výstupy:

- `OUT/runs/<vep|kv>-<off|on>/<balíček>.txt` textový protokol, `.err` stderr, `.time` návratový kód a doba běhu
- `OUT/packages.txt`, `OUT/jobs.tsv` seznam balíčků a úloh
- `summary.json`, `tabulka.md`, `rozdily.md` z porovnání

## Konfigurace běhů

| | off | on |
|---|---|---|
| VEP | `--disable-verapdf --disable-epubcheck` | `--verapdf-path … --epubcheck-path …` |
| KV | totéž | totéž |

Resolver URN:NBN: KV dostane `--urnnbn-resolver-url http://127.0.0.1:9 --urnnbn-resolver-allow-http`.
VEP 2.4 adresu Resolveru nastavit neumí (natvrdo `https://resolver.nkp.cz/api/v5/urnnbn/`, `java.net.URL`),
proto se mu nastaví `-Dhttp(s).proxyHost=127.0.0.1 -Dhttp(s).proxyPort=9`. Oba validátory tak skončí
„Connection refused“ se stejnou hláškou ERROR v pravidle `MODS_IDENTIFIERS_URN_NBN_REGISTERED` a nikdo
nejde do sítě. Pravidlo se proto z porovnání nevyřazuje.

## Porovnání

Pro každý balíček, konfiguraci a pravidlo se porovnají počty INFO/WARNING/ERROR a multimnožina hlášek.
Normalizace hlášek:

- cesty ke vzorkům, ke konfiguraci a k `OUT` nahrazeny zástupci, názvy XSD sjednoceny (`<XSD>`)
- odstraněn prefix validační funkce VEP (`checkXxx: `) a prefix souboru KV (`mets_x.xml: `, `null: `)
- odstraněna přípona VEP `(soubor <cesta>)` u hlášek binárních profilů (KV soubor v textovém protokolu neuvádí)
- odstraněny uvozovky kolem hodnot bez mezer, sjednoceny názvy fDMF (`Elektronické monografie` → `E-monografie`)
- bílé znaky a koncová čárka; pořadí hlášek se ignoruje (je nedeterministické)

Přejmenovaná pravidla VEP → KV jsou v `RENAMED` v `compare-vep.py` (zatím `PSP_ID_DERIVED_FROM_TITLE_ID`
→ `PSP_ID_DERIVED_FROM_IE_ID`).

Rozdílná hláška (strana `vep` = jen ve VEP, `kv` = jen v KV, `level` = stejný text s jinou úrovní) se zařadí
podle `known.py` do kategorie *úmyslný známý*, *oprava chyby VEP*, nebo zůstane *nejasné*. `tabulka.md`
ukazuje u každého balíčku počty obou validátorů, shodu verdiktu a počet pravidel s rozdílem po kategoriích,
`rozdily.md` agreguje známé rozdíly a vypisuje nejasné s příklady.

## Doba běhu

Na MacBooku (14 jader, `JOBS=6`) 12. 11. 2026: 120 běhů (28 balíčků + 2 vynucené, 2 validátory,
2 konfigurace) zhruba za **15 minut** (běhy s nejdelším trváním se řadí napřed). Bez nástrojů 2–9 s na běh
(součet všech 60 běhů necelé 5 min), s nástroji většinou do 20 s, PDF příloh e-monografií 2.3 kolem 1 min, EPUB příloh
e-monografií a e-periodik 2.3 (velké EPUB, EPUBCheck) 8–13 min na běh. Porovnání trvá několik sekund.
