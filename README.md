# Komplexní validátor

Nástroj Národní knihovny ČR pro validaci SIP balíčků digitalizovaných dokumentů podle Definic metadatových formátů
(DMF) Standardu NDK. Kontroluje strukturu balíčku, soubor info.xml, METS, bibliografická metadata (MODS, DC),
technická a administrativní metadata (MIX, PREMIS, copyrightMD), soubory ALTO a OCR TXT a pomocí externích nástrojů
i obrazové a zvukové soubory. Výstupem je textový výpis a XML protokol.

Vydání, instalátory a seznamy změn: https://github.com/NLCR/komplexni-validator/releases. Standardy:
https://standardy.ndk.cz/. Hlášení chyb a dotazy: GitHub issues.

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
| jednotky fondu | `Clipping`, `Clipping index`, `Card index` | 0.1 |

Verze DMF se určuje z METS `TYPE` a z `metadataversion` v info.xml; parametry `--preferred-dmf-<typ>-version`
a `--forced-dmf-<typ>-version` (typ `mon`, `per`, `adg`, `adf`, `adi`, `adn`, `dad`, `fdu`) ji upřednostní nebo vynutí.
Verze ALTO a jejich kontrola: viz [ALTO.md](ALTO.md).
Kontrola registrace URN:NBN se ptá produkčního Resolveru (https://resolver.nkp.cz); parametr `--urnnbn-resolver-url`
ji přepne na jinou instalaci, např. testovací https://resolver-test.nkp.cz.

## Sestavení a spuštění

Vyžaduje JDK 17 pro kompilaci (Gradle toolchain), běh na Javě 17 nebo 21.

    ./gradlew build                          # sestavení a testy
    ./gradlew :modules:cliModule:jar         # CLI jar: modules/cliModule/build/libs/cliModule-<verze>.jar
    ./gradlew :modules:guiModule:run         # spuštění GUI
    ./gradlew :modules:guiModule:shadowJar   # GUI jar se závislostmi (podklad pro jpackage)

CLI (nápověda `--help`), příklad validace balíčku s konfigurací z repozitáře a bez externích nástrojů:

    java -cp "cliModule-<verze>.jar:<závislosti>" nkp.pspValidator.cli.Main \
        --action VALIDATE_PSP --psp /cesta/k/balicku \
        --config-dir modules/sharedModule/src/main/resources/nkp/pspValidator/shared/validatorConfig \
        --xml-protocol-file protokol.xml \
        --disable-imagemagick --disable-jhove --disable-jpylyzer --disable-kakadu \
        --disable-mp3val --disable-shntool --disable-checkmate

Akce: `VALIDATE_PSP`, `VALIDATE_PSP_GROUP`, `VALIDATE_METADATA_BY_PROFILE`, `BUILD_MINIFIED_PACKAGE`. Vydaný CLI
zip obsahuje spouštěcí skript se všemi závislostmi. Externí nástroje (ImageMagick, JHOVE, Jpylyzer, Kakadu, mp3val,
shntool, Checkmate) se zadávají cestou (`--<nástroj>-path`) nebo vypínají (`--disable-<nástroj>`); instalátory
JHOVE a Jpylyzer jsou v `installers/`.

## Konfigurace validace

Pravidla jsou datová (XML) a jsou popsaná přímo v souborech fDMF: `rules.xml` (pravidla), `variables.xml`
(proměnné), `patterns.xml` (vzory názvů souborů), `namespaces.xml`, adresáře `xsd/`, `biblioProfiles/`,
`techProfiles/`, `metsProfiles/`, `binaryFileProfiles/`. Dostupné validační a evaluační funkce s parametry jsou
vypsané ve `validatorConfig/functions/`. Atribut `validatorVersion` konfiguračních souborů musí odpovídat verzi
aplikace (`Version.java`, část před pomlčkou).

## Verze

Verze aplikace je v `gradle.properties` (`appVersion`) a v `modules/sharedModule/.../Version.java` (`VERSION_CODE`,
`BUILD_DATE`). Vydání se značí tagem `vX.Y` a publikuje jako GitHub release.

## Licence

Apache License 2.0, viz [LICENSE](LICENSE).
