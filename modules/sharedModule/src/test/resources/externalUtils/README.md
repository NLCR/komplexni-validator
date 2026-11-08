# Uložené výstupy externích nástrojů

Vstupy pro testy parsování výstupů nástrojů bez samotných nástrojů (balíček
`nkp.pspValidator.shared.externalUtils`, viz `ExternalUtilsTestSupport`). Testy vezmou uložený
text, podstrčí ho místo spuštění procesu (`CannedOutputExternalUtilManager`) a vyhodnotí nad ním
profily binárních souborů z fDMF (`binaryFileProfiles/{mc,uc}/*.xml`), extrakce a pravidla.

Pořízeno 8. 11. 2026 na macOS (Apple Silicon). Absolutní cesty ve výstupech jsou nahrazené cestou
relativní k `modules/sharedModule` (např. `src/test/tools/jp2/mc.jp2`), jinak jsou výstupy beze změny.

Názvy souborů: `<adresář vstupu>_<název vstupu>`, vstupy jsou v `src/test/resources/jpeg2000/<adresář>/`,
`tools_mc` a `tools_uc` jsou `src/test/tools/jp2/mc.jp2` a `uc.jp2`.

## Jpylyzer (reálné výstupy)

- Verze 2.2.0 (`version.txt`), balík `installers/Linux/opf-jpylyzer_2.2.0_all.deb` rozbalený
  a spuštěný pod Pythonem 3 (čistý Python, bez instalace do systému).
- Příkaz: `jpylyzer <soubor>` (stdout), odpovídá spuštění `jp2k` v `externalUtils.xml`.
- Soubory: `tools_mc.xml`, `tools_uc.xml`, `ok_uc_UC1.xml`, `odlis_od_prof_mc_LRCP.xml`,
  `odlis_od_prof_uc_bezSOP.xml`, `nevalidni_u0911e.xml`, `nevalidni_u5463f.xml` (prázdný soubor).

## JHOVE (reálné výstupy)

- Verze 1.29.0-SNAPSHOT z 18. 2. 2024 (`version.txt`, první řádek výstupu `jhove --version`),
  instalátor `installers/Mac/jhove-xplt-installer-1.29.0-SNAPSHOT.jar` nainstalovaný do dočasného
  adresáře.
- Příkaz: `jhove -h XML -m jpeg2000-hul -k <soubor>` (stdout).
- Soubory: stejná sada vstupů jako u Jpylyzeru.
- `harvard_ns_rel1.6_mc.xml`: historický výstup JHOVE 1.6 (2011) se starým jmenným prostorem
  `http://hul.harvard.edu/ois/xml/ns/jhove`, převzatý z `modules/guiModule/src/main/xml/jhove_output_mc.xml`
  (obsahuje ruční komentáře SPEC/HODNOTY, data jsou původní).

## Kakadu (reálné výstupy)

- `kdu_expand` v8.5 (`version.txt` je stdout `kdu_expand -version`).
- Příkaz: `kdu_expand -i <soubor>`, ukládá se stderr (jako v `externalUtils.xml`).
- `tools_mc.txt`, `tools_uc.txt`: bez chyby (prázdný stderr).
- `detekce_poskozeni_w0352gUC.txt`: chybějící EPH marker.
- `nevalidni_w7200f.txt`: SOP marker mimo pořadí.
- `synteticky_dve_chyby.txt`: **syntetický**, dvě chybové hlášky za sebou (spojení dvou reálných
  hlášek), pro ověření, že regulární výraz profilu rozdělí výstup na dvě položky.

## ImageMagick

- Verze 7.1.2-24 (`version.txt` je stdout `convert -version`). Instalace z Homebrew nemá JP2
  delegáta (OpenJPEG), takže reálný výstup je jen chyba „no decode delegate“.
- Příkaz: `identify -verbose <soubor>`, ukládá se stderr.
- `no_delegate_tools_mc.txt`: reálný výstup bez JP2 delegáta.
- `synteticky_ok.txt`: **syntetický**, prázdný stderr (bezchybné načtení).
- `synteticky_poskozeny.txt`: **syntetický**, varování a chyba ve tvaru, jaký vypisuje ImageMagick
  s OpenJPEG u poškozeného souboru (`identify: ... @ warning/jp2.c/...`).
- `synteticky_error_radek.txt`: **syntetický**, řádek začínající `Error`, na který míří první
  regulární výraz profilu `imageMagick.xml`.

## Obnovení

Výstupy lze pořídit znovu se stejnými příkazy. Kakadu na macOS hledá knihovnu relativně
(`../../lib/...`), proto se `kdu_expand` spouští z adresáře `bin/Mac-arm-64-gcc`.
