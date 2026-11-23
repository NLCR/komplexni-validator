# Uložené výstupy externích nástrojů

Vstupy pro testy parsování výstupů nástrojů bez samotných nástrojů (balíček
`nkp.pspValidator.shared.externalUtils`, viz `ExternalUtilsTestSupport`). Testy vezmou uložený
text, podstrčí ho místo spuštění procesu (`CannedOutputExternalUtilManager`) a vyhodnotí nad ním
profily binárních souborů z fDMF (`binaryFileProfiles/{mc,uc}/*.xml`, u WAV `binaryFileProfiles/mca/*.xml`, u veraPDF a EPUBCheck
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

## JHOVE a shntool pro WAV (reálné výstupy, issue #104)

- Pořízeno 20. 11. 2026 na macOS (Apple Silicon): JHOVE 1.34.0 (instalátor `jhove-installer-1.34.0.jar`), shntool
  3.0.10 z Homebrew (`shntool/version.txt` je první řádek `shntool -v`, vypisuje se na STDERR).
- Příkazy: `jhove -h XML -m WAVE-hul <soubor>` a `shntool info <soubor>` (stdout), odpovídají spuštěním `wav`
  v `externalUtils.xml`. Spuštěno v adresáři vstupu, v `repInfo/@uri` a `File name` je jen název souboru.
- Vstupy (nejsou v repozitáři): soubor `mastercopy_audio` vzorového balíku fonoválečků NK
  (`official-samples/vzorovy_balicek_fonograficke_valecky.zip`, mono, 24 bit, 96 kHz), přejmenovaný na
  `mca_1234567890_000N.wav`.
- `wav_ok_24bit_96kHz.*`: celý soubor, JHOVE `Well-Formed and valid`, shntool bez problému.
- `wav_truncated.*`: prvních 1 000 000 bajtů téhož souboru, JHOVE `Not well-formed` (WAVE-HUL-3), shntool
  `File probably truncated: yes (missing 34953334 bytes)`.
- `wav_16bit_44kHz.*`: syntetický vstup (stereo, 16 bit, 44,1 kHz, 0,1 s ticha, Python `wave`), výstupy reálné.
- `wav_mp3_renamed.*`: MP3 z `usercopy_audio` téhož balíku s příponou `.wav`; JHOVE `Not well-formed` (WAVE-HUL-1),
  shntool nic nevypíše na STDOUT (prázdný soubor), na STDERR `none of the builtin format modules handle input file`.

## Kakadu (reálné výstupy)

- `kdu_expand` v8.5 (`version.txt` je stdout `kdu_expand -version`).
- Příkaz: `kdu_expand -i <soubor>`, ukládá se stderr (jako v `externalUtils.xml`).
- `tools_mc.txt`, `tools_uc.txt`: bez chyby (prázdný stderr).
- `detekce_poskozeni_w0352gUC.txt`: chybějící EPH marker.
- `nevalidni_w7200f.txt`: SOP marker mimo pořadí.
- `synteticky_dve_chyby.txt`: **syntetický**, dvě chybové hlášky za sebou (spojení dvou reálných
  hlášek), pro ověření, že regulární výraz profilu rozdělí výstup na dvě položky.
- `nelze_otevrit_neexistuje.txt`, `nelze_otevrit_dlouha_cesta.txt`: pořízeno 23. 11. 2026 na macOS, `kdu_expand` v8.5
  nad neexistujícím souborem (spuštěno v `modules/sharedModule` s relativní cestou), `Error in Kakadu File Format
  Support: Unable to open input file`. Kakadu text zalamuje na šířku řádku (v dlouhé cestě místo mezery).
- `nelze_otevrit_windows_znakova_sada.txt`: výstup `kdu_expand.exe` 8.7 na Windows 11 (ověření 23. 11. 2026) u souboru
  `隨機中國文字.jp2`, jehož cesta obsahuje znaky mimo znakovou sadu systému (Kakadu je dostane jako `?`). Konce řádků
  CRLF jako v originálu; absolutní cesta je zkrácená na relativní a zalomení uvnitř cesty odstraněno, ostatní beze změny.

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
- `im69_*.txt`, `im71_*.txt` (reálné výstupy, ověření na Linuxu 23. 11. 2026, nález N1, test `ImageMagickProfileTest`):
  pořízeno v Dockeru, ImageMagick 6.9.12-98 Q16 z Ubuntu 24.04 (`imagemagick` 8:6.9.12.98+dfsg1-5.2build2, OpenJPEG
  2.5.0; první řádek `convert -version`: `Version: ImageMagick 6.9.12-98 Q16 aarch64 18038 https://legacy.imagemagick.org`)
  a 7.1.1-43 Q16 z Debianu 13 (OpenJPEG 2.5.3). Příkaz
  `identify -verbose <soubor>` spuštěný v `modules/sharedModule` s relativní cestou, ukládá se stderr, beze změny.
  - `im69_path_detekce_poskozeni_w0352gUC.txt`: 6.9 spuštěný z PATH (`identify`), hlášky začínají
    `identify-im6.q16:`. Ověřeno: spuštění absolutní cestou (`/usr/bin/identify`, `/etc/alternatives/identify`)
    dává `identify:`, spuštění názvem z PATH nebo relativní cestou (`./identify`) `identify-im6.q16:`, tedy název
    skutečného programu, na který vede odkaz `/usr/bin/identify` přes alternatives.
  - `im69_absolutni_cesta_detekce_poskozeni_w0352gUC.txt`: 6.9 spuštěný jako `/usr/bin/identify` (validátor se
    zadaným adresářem), hlášky začínají `identify:`.
  - `im69_path_tools_mc.txt`: 6.9 z PATH nad bezvadným `src/test/tools/jp2/mc.jp2`, prázdný stderr.
  - `im71_detekce_poskozeni_w0352gUC.txt`: 7.1 z PATH, hlášky `identify:` (stejně jako `magick identify`, absolutní
    cesta i 7.1.2-35 na macOS).

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
- `synteticky_compliant_PDFA-4.xml`: **syntetický**, `compliant_PDFA-2B.xml` s `profileName="PDF/A-4 validation profile"`
  (veraPDF u PDF/A-4, 4e a 4f neuvádí písmeno úrovně); skutečný soubor PDF/A-4 ve vzorcích není.
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
  1,8 kB, font `font/woff`), EPUBCheck hlásí verzi 3.4. Do výčtu `MediaTypes` je **ručně doplněn** typ
  `application/vnd.adobe-page-template+xml` (mimo core media types EPUB 3.3), aby měl test nepovolený typ.
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
