# Uložené výstupy externích nástrojů

Vstupy pro testy parsování výstupů nástrojů bez samotných nástrojů (balíček
`nkp.pspValidator.shared.externalUtils`, viz `ExternalUtilsTestSupport`). Testy vezmou uložený
text, podstrčí ho místo spuštění procesu (`CannedOutputExternalUtilManager`) a vyhodnotí nad ním
profily binárních souborů z fDMF (`binaryFileProfiles/{mc,uc}/*.xml`, u veraPDF a EPUBCheck
`binaryFileProfiles/{pdf,epub}/*.xml` z `emonograph_3.0`), extrakce a pravidla.

Obrazové nástroje pořízeny 8. 11. 2026 na macOS (Apple Silicon), veraPDF a EPUBCheck 12. 11. 2026. Absolutní cesty ve výstupech jsou nahrazené cestou
relativní k `modules/sharedModule` (např. `src/test/tools/jp2/mc.jp2`), jinak jsou výstupy beze změny.

Názvy souborů obrazových nástrojů: `<adresář vstupu>_<název vstupu>`, vstupy jsou v `src/test/resources/jpeg2000/<adresář>/`,
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

## veraPDF (reálné výstupy)

- Pořízeno 12. 11. 2026 na macOS (Apple Silicon), Homebrew formule `verapdf` 1.30.2, Temurin 17.0.19.
  `version.txt` je stdout `verapdf --version` (`veraPDF 1.30.0`: aplikační část má 1.30.0, knihovny 1.30.2, viz
  `releaseDetails` ve výstupech; regulární výraz detekce verze `veraPDF [0-9\.]+`).
- Příkaz: `verapdf <soubor>` (stdout, bez `--format`, jako spuštění `pdf` v `externalUtils.xml`). V elementu
  `item/name` je místo absolutní cesty jen název souboru.
- Vstupy `dummy*.pdf` jsou z test resources VEP (`validator-e-publikaci`, `modules/sharedModule/src/test/resources/pdf/`).
- `compliant_PDFA-1A.xml`, `compliant_PDFA-1B.xml`, `compliant_PDFA-2B.xml`, `compliant_PDFA-2U.xml`,
  `compliant_PDFA-3B.xml`: `dummy_PDFA-*.pdf`, `isCompliant="true"`, `profileName` s malým písmenem úrovně
  (`PDF/A-1b validation profile`, riziko R1).
- `noncompliant_dummy.xml`: `dummy.pdf` (bez PDF/A), veraPDF zvolí PDF/A-1b, `isCompliant="false"`, exit 1.
- `noncompliant_dummy_flavour_2b.xml`, `noncompliant_dummy_flavour_3b.xml`: totéž s vynuceným profilem
  `verapdf -f 2b`, resp. `-f 3b` (validátor `-f` nepoužívá, slouží jen jako nevyhovující PDF/A-2b a 3b).
- `empty.xml` (prázdný soubor) a `not_pdf.xml` (textový soubor s příponou .pdf): bez `validationReport`,
  `taskException type="PARSE"`, exit 7, varování na stderr.

## EPUBCheck (reálné výstupy)

- Pořízeno 12. 11. 2026 na macOS, Homebrew formule `epubcheck` 5.4.0, Temurin 17.0.19. `version.txt` je stdout
  `java -jar epubcheck.jar --version`.
- Příkaz: `java -jar epubcheck.jar --out - -q <soubor>` (stdout, jako spuštění `epub` v `externalUtils.xml`),
  spuštěno v adresáři vstupu (v `repInfo/@uri` je relativní cesta). Exit 1 u chyb v EPUB.
- `wellformed_epub2_Sway.xml`, `notwellformed_epub2_sample1.xml`: `Sway.epub` a `sample1.epub` z test resources VEP
  (`modules/sharedModule/src/test/resources/epub/`).
- `notwellformed_epub2_emono30.xml`: vzorek e-monografie 3.0 z `official-samples/epub` (`oc_aba007-0000f8.epub`,
  213 kB), 48× RSC-005.
- `wellformed_epub3_synteticky.xml`: syntetický EPUB 3 (`src/test/resources/epublikace/synteticky_epub3.epub`,
  1,8 kB, font `font/woff`), EPUBCheck hlásí verzi 3.4.
- `empty.xml` (prázdný soubor) a `not_epub.xml` (textový soubor s příponou .epub): Not well-formed, PKG-008
  a PKG-003, bez `version`.
- `synteticky_drm_RSC-004.xml`: **syntetický**, `wellformed_epub2_Sway.xml` s doplněnou hláškou RSC-004. EPUBCheck
  5.4.0 má RSC-004 jako INFO („File … is encrypted, its content will not be checked“) a do XML výstupu ho
  nevypíše (ověřeno se syntetickým EPUB s `META-INF/encryption.xml`), validace „Kontrola DRM“ profilu VEP proto
  s aktuální verzí nic nenajde.

## Profily e-publikací

Testy berou profily veraPDF a EPUBCheck přímo z fDMF `emonograph_3.0` (port z VEP 2.4, krok P4). Profily všech
e-fDMF jsou až na atribut `dmf` (a název validace DRM u e-periodik) shodné. Proti VEP je opravené riziko R1
(veraPDF 1.30 vypisuje `PDF/A-1b` malým písmenem, profil přijímá obě varianty); známá vada
`application/font‐wof` (R3) zůstává.

## Obnovení

Výstupy lze pořídit znovu se stejnými příkazy. Kakadu na macOS hledá knihovnu relativně
(`../../lib/...`), proto se `kdu_expand` spouští z adresáře `bin/Mac-arm-64-gcc`.
