# Komplexní validátor

Nástroj Národní knihovny ČR pro validaci SIP balíčků digitalizovaných dokumentů podle Definic metadatových formátů
(DMF) Standardu NDK. Kontroluje strukturu balíčku, soubor info.xml, METS, bibliografická metadata (MODS, DC),
technická a administrativní metadata (MIX, PREMIS, copyrightMD), soubory ALTO a OCR TXT a pomocí externích nástrojů
i obrazové a zvukové soubory. Výstupem je textový výpis a XML protokol.

Vydání, instalátory a seznamy změn: https://github.com/NLCR/komplexni-validator/releases. Standardy:
https://standardy.ndk.cz/. Hlášení chyb a dotazy: GitHub issues.

## Instalace a spuštění

Vydání obsahuje šest souborů (podrobně v [release.md](release.md)):

| soubor | pro koho | Java |
|---|---|---|
| `KomplexniValidatorGUI-<verze>_Windows_x86_64_installer.exe` | instalátor GUI pro Windows x64, instaluje pro přihlášeného uživatele bez práv správce do `%LOCALAPPDATA%\KomplexniValidator` | nemusí být nainstalovaná |
| `KomplexniValidatorGUI-<verze>_Windows_x86_64_portable.zip` | GUI pro Windows x64 bez instalace: rozbalit a spustit `KomplexniValidator.exe` | nemusí být nainstalovaná |
| `KomplexniValidatorGUI-<verze>_macOS_AppleSilicon_installer.dmg` | instalátor GUI pro macOS na Apple Silicon: aplikaci přetáhnout do Aplikací | nemusí být nainstalovaná |
| `KomplexniValidatorGUI-<verze>_Linux_x64_portable.zip` | GUI pro Linux x64 bez instalace: rozbalit a spustit `bin/KomplexniValidator` | nemusí být nainstalovaná |
| `KomplexniValidatorGUI-<verze>.zip` | GUI jako jar (`KomplexniValidatorGUI.jar`) a `validatorConfig/`, pro ostatní platformy | Java 17 nebo novější s JavaFX |
| `KomplexniValidatorCLI-<verze>.zip` | příkazová řádka (`KomplexniValidatorCLI.jar`) a `validatorConfig/` | Java 17 nebo novější |

Instalátory ani portable verze nejsou podepsané. Windows SmartScreen při prvním spuštění zobrazí varování: zvolit
„Více informací“ a „Přesto spustit“. macOS stažené dmg zablokuje: aplikaci povolit v Nastavení → Soukromí
a zabezpečení → „Přesto otevřít“.

Kde jsou soubory (instalované a portable GUI): konfigurace validace (`validatorConfig`) je součástí aplikace
(Windows `app\validatorConfig`, Linux `lib/app/validatorConfig`, macOS `Contents/app/validatorConfig`), nastavení
aplikace (`config.properties`) a logy jsou mimo ni: Windows `%APPDATA%\KomplexniValidator`, Linux
`~/.config/KomplexniValidator` (nebo `$XDG_CONFIG_HOME/KomplexniValidator`), macOS
`~/Library/Application Support/KomplexniValidator`. U GUI zipu (`java -jar KomplexniValidatorGUI.jar`) se
`validatorConfig` hledá vedle jaru, jinak v pracovním adresáři; `config.properties` a `logs` jsou v pracovním adresáři.

## Moduly

| modul | obsah |
|---|---|
| `modules/sharedModule` | jádro: engine pravidel, validační a evaluační funkce, detekce DMF, konfigurace (`src/main/resources/nkp/pspValidator/shared/validatorConfig`), testy a nástroje pro testovací balíčky (`src/test/tools`) |
| `modules/cliModule` | příkazová řádka (`nkp.pspValidator.cli.Main`) |
| `modules/guiModule` | desktopová aplikace (JavaFX, `nkp.pspValidator.gui.Launcher`) |

## Podporované DMF

Každé verzi DMF odpovídá adresář fDMF v `validatorConfig/fDMF/` (pravidla, proměnné, vzory názvů, XSD, profily
metadat, profily binárních souborů):

| typ dokumentu | METS `TYPE` | verze DMF |
|---|---|---|
| monografie | `Monograph` | 1.0, 1.2, 1.3, 1.3.1, 1.3.2, 1.4, 2.0, 2.1, 2.2, 2.3 |
| periodika | `Periodical` | 1.4, 1.6, 1.7, 1.7.1, 1.8, 1.9, 2.0, 2.1, 2.2 |
| zvuk, gramodesky | `sound recording` | 0.3, 0.4, 0.5, 1.0 |
| zvuk, fonoválečky | `audio cylinder` | 0.3, 1.0 |
| zvuk, kompaktní disky | `audio disc` | 1.0 |
| zvuk bez nosiče | `digital audio` | 1.0 |
| datové disky | `data_disc` | 0.1 |
| jednotky fondu | `Clipping`, `Clipping index`, `Card index` | 0.1, 0.2 |
| e-monografie | `electronic_monograph` | 2.3, 2.4, 3.0, 3.1 |
| e-periodika | `electronic_periodical` | 2.3, 2.4, 2.5, 2.6 |
| skládaná e-periodika | `electronic_coll_journal` | 1.3 |

Celkem 39 konfigurací. Když balíček verzi neuvádí, použije se výchozí (konstanty `DEFAULT_*_VERSION` v
`DmfDetector.java`): monografie 2.3, periodika 2.2, gramodesky 1.0, fonoválečky 1.0, kompaktní disky 1.0, zvuk bez
nosiče 1.0, datové disky 0.1, jednotky fondu 0.2, e-monografie 3.1, e-periodika 2.6, skládaná e-periodika 1.3.

Typ DMF se určuje z METS `TYPE`, verze z `metadataversion` v info.xml. Pořadí: `--forced-dmf-<typ>-version`
(vynutí verzi vždy), jinak `metadataversion` z info.xml, jinak `--preferred-dmf-<typ>-version`, jinak výchozí verze
(typ je `mon`, `per`, `adg`, `adf`, `adi`, `adn`, `dad`, `fdu`, `emo`, `epe`, `sep`, např. `--forced-dmf-fdu-version 0.1`).
Volby Validátoru e-publikací `--forced-dmf-emon-version`, `--forced-dmf-eper-version`, `--forced-dmf-comper-version`
(a obdobně `--preferred-…`) se přijímají jako zastaralé aliasy voleb pro `emo`, `epe`, `sep`.
Verze ALTO a jejich kontrola: viz [ALTO.md](ALTO.md).
Kontrola registrace URN:NBN se ptá produkčního Resolveru (https://resolver.nkp.cz); parametr `--urnnbn-resolver-url`
ji přepne na jinou instalaci, např. testovací https://resolver-test.nkp.cz. Adresa http:// (lokální testovací instalace
bez TLS) je povolená jen s přepínačem `--urnnbn-resolver-allow-http`; přesměrování z http na https validátor neprovádí,
je třeba zadat cílovou adresu.

## Sestavení a spuštění

Vyžaduje JDK 17 nebo novější (např. jen JDK 21); kompilace je vždy pro Javu 17 (`options.release = 17`), výsledek
běží na Javě 17 a novější. Gradle toolchain se nepoužívá.

    ./gradlew clean build                    # sestavení a testy
    ./gradlew :modules:cliModule:jar         # CLI jar: modules/cliModule/build/libs/cliModule-<verze>.jar
    ./gradlew :modules:guiModule:run         # spuštění GUI
    ./gradlew :modules:guiModule:shadowJar   # GUI jar se závislostmi (podklad pro jpackage)

Instalátory a portable aplikace (jpackage) a vydané zipy se sestavují podle [release.md](release.md), v `build.gradle`
se před tím nic ručně nemění. Verze ALTO a jejich kontrola: [ALTO.md](ALTO.md).

CLI (nápověda `--help`, verze `--version`), příklad validace balíčku s konfigurací z repozitáře a bez externích nástrojů:

    java -jar cliModule-<verze>.jar \
        --action VALIDATE_PSP --psp /cesta/k/balicku \
        --config-dir modules/sharedModule/src/main/resources/nkp/pspValidator/shared/validatorConfig \
        --xml-protocol-file protokol.xml \
        --disable-imagemagick --disable-jhove --disable-jpylyzer --disable-kakadu \
        --disable-mp3val --disable-shntool --disable-checkmate --disable-verapdf --disable-epubcheck

Akce (`-a`, `--action`): `VALIDATE_PSP`, `VALIDATE_PSP_GROUP` (`--psp-group`), `VALIDATE_METADATA_BY_PROFILE`
(`--metadata-profile-id`, `--metadata-file`), `BUILD_MINIFIED_PACKAGE` (`--psp`, `--minified-psp-dir`). XML protokol:
`--xml-protocol-file` (jeden balíček) nebo `--xml-protocol-dir` (protokol pro každý balíček); ZIP balíčky vyžadují
`--tmp-dir` (`-t`). Podrobnost výpisu `-v` (`--verbosity`, 0 až 3, výchozí 2). Vydaný CLI zip obsahuje jar se všemi
závislostmi (`KomplexniValidatorCLI.jar`) a adresář `validatorConfig`.

Externí nástroje (ImageMagick, JHOVE, Jpylyzer, Kakadu, MP3val, shntool, Checkmate, veraPDF, EPUBCheck) se zadávají
adresářem (`--<nástroj>-path`, např. `--jhove-path`) nebo vypínají (`--disable-<nástroj>`); bez cesty se hledají
v PATH. Validátor verze nástrojů nekontroluje, jen je vypíše. Nástroj, jehož verzi se nepodaří zjistit, se pro
validaci vypne: pravidlo pak hlásí jednu informaci „nástroj … není dostupný“ (dříve CLI hlásilo chybu spuštění
u každého souboru). Nástroje se instalují z oficiálních zdrojů, například
[JHOVE](https://jhove.openpreservation.org/) (doporučená verze 1.34.0, nejméně 1.28) a
[Jpylyzer](https://jpylyzer.openpreservation.org/) (doporučená verze 2.2.1, nejméně 2.0); instalátory nejsou součástí
repozitáře ani vydaných zipů.

### veraPDF a EPUBCheck (e-publikace)

Soubory PDF a EPUB e-publikací kontrolují [veraPDF](https://verapdf.org/) (validace PDF/A) a
[EPUBCheck](https://github.com/w3c/epubcheck). Doporučené minimální verze: veraPDF 1.24 (sjednocený formát XML
výstupu), EPUBCheck 5.0 (EPUB 3.3); ověřeno s veraPDF 1.30 a EPUBCheck 5.4.0. Oba nástroje potřebují Javu
(veraPDF 8, 11, 17 nebo 21, EPUBCheck 8 a novější).

| nástroj | parametr CLI | adresář, který se zadává (CLI i GUI: Nastavení → Externí nástroje, „Nastavit cestu…“) |
|---|---|---|
| veraPDF | `--verapdf-path` | instalační adresář se skriptem `verapdf` (Windows `verapdf.bat`) |
| EPUBCheck | `--epubcheck-path` | rozbalený zip s `epubcheck.jar` a podadresářem `lib` |

Instalace na macOS přes Homebrew (`brew install verapdf epubcheck`):

    --verapdf-path "$(brew --prefix)/bin"
    --epubcheck-path "$(brew --prefix epubcheck)/libexec"

U EPUBCheck se nesmí zadat `$(brew --prefix)/bin`: je tam jen spouštěcí skript `epubcheck` bez `epubcheck.jar`.
Homebrew formule veraPDF má verzi 1.30.2, `verapdf --version` ale vypíše `veraPDF 1.30.0` (verze aplikační části,
knihovny jsou 1.30.2); jde o stejnou instalaci. Jinde: veraPDF z https://software.verapdf.org/releases/verapdf-installer.zip
(instalátor, spustit `java -jar` na rozbalený jar), EPUBCheck jako zip z https://github.com/w3c/epubcheck/releases.

EPUBCheck se spouští příkazem `java -jar <adresář>/epubcheck.jar`, kde `java` je program z PATH, ne Java, ve které
běží validátor. Na stroji bez `java` v PATH (typicky GUI z instalátoru, které má vlastní přibalenou Javu) EPUBCheck
nepoběží a validace ho ohlásí jako nedostupný; je potřeba nainstalovat Javu 8 nebo novější a přidat ji do PATH.

## Konfigurace validace

Pravidla jsou datová (XML) a jsou popsaná přímo v souborech fDMF: `rules.xml` (pravidla), `variables.xml`
(proměnné), `patterns.xml` (vzory názvů souborů), `namespaces.xml`, adresáře `xsd/`, `biblioProfiles/`,
`techProfiles/`, `metsProfiles/`, `binaryFileProfiles/`. Dostupné validační a evaluační funkce s parametry jsou
vypsané ve `validatorConfig/functions/`. Atribut `validatorVersion` konfiguračních souborů musí mít stejnou major
verzi jako aplikace (konfigurace „3.0“ i „3.1“ projde s validátorem 3.x).

## Verze

Verze aplikace je jen v `gradle.properties` (`appVersion`), verzi a datum buildu z ní při buildu generuje Gradle
pro třídu `Version`. Vydání se značí tagem `vX.Y` a publikuje jako GitHub release; postup vydání, assets a jejich
sestavení (`tools/release/build-assets.sh`): viz [release.md](release.md).

## Licence

Apache License 2.0, viz [LICENSE](LICENSE).
