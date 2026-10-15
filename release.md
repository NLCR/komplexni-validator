# Vydání nové verze

Postup vydání Komplexního validátoru (CLI a GUI) jako GitHub release. Ověřeno na v2.8-beta (24. 9. 2026).

## Assets

Každý release má pět souborů, `<tag>` je např. `v2.8-beta`:

| soubor | obsah | kde se staví |
|---|---|---|
| `KomplexniValidatorCLI-<tag>.zip` | adresář stejného jména: `KomplexniValidatorCLI.jar` (jar se všemi závislostmi) a `validatorConfig/` | skript, Docker |
| `KomplexniValidatorGUI-<tag>.zip` | adresář stejného jména: `KomplexniValidatorGUI.jar` a `validatorConfig/`; běží na Javě 17+ s JavaFX (jar obsahuje nativní knihovny JavaFX jen pro Linux) | skript, Docker |
| `KomplexniValidatorGUI-<tag>_Linux_x64_portable.zip` | portable aplikace pro Linux x64 | skript, Docker |
| `KomplexniValidatorGUI-<tag>_Windows_x86_64_portable.zip` | portable aplikace pro Windows x64 | ručně na Windows |
| `KomplexniValidatorGUI-<tag>_Windows_x86_64_installer.exe` | instalátor pro Windows x64 | ručně na Windows |

**Portable** je aplikace, která se neinstaluje: zip se rozbalí kamkoli a spustí se `KomplexniValidator.exe` (Windows)
nebo `bin/KomplexniValidator` (Linux). Obsahuje vlastní Javu (adresář `runtime`), na stroji tedy nemusí být Java
a nejsou potřeba práva správce. Technicky jde o výstup `jpackage --type app-image`, tedy totéž, co instalátor
nakopíruje do Program Files. Konfigurace validace je uvnitř v `app/validatorConfig` (Linux `lib/app/validatorConfig`).

Pro macOS se assets nevydávají (na Macu lze použít GUI zip s Javou s JavaFX, nebo sestavit instalátor úlohou
`./gradlew :modules:guiModule:jpackage`, viz níže).

## 1. Příprava verze

- `gradle.properties`: `appVersion`, např. `2.8-beta`.
- `modules/sharedModule/src/main/java/nkp/pspValidator/shared/Version.java`: `VERSION_CODE` (stejně jako `appVersion`)
  a `BUILD_DATE` (datum vydání, formát `24. 9. 2026`).
- `ValidatorProtocolXmlBuilder.XSD_URL`: odkaz na XSD protokolu na tagu vydání (`refs/tags/<tag>/...`).
- Při změně minor verze (např. 2.7 → 2.8) navíc hromadná změna atributu `validatorVersion` ve všech konfiguračních
  souborech a namespace protokolu (vzor: commit 53e1404e „change to version 2.8“, 1451 souborů). Část `VERSION_CODE`
  před pomlčkou se musí shodovat s `validatorVersion`, jinak se konfigurace nenačte.
- Commit ve stylu „set version to 2.8-beta“.

## 2. Build a testy

    ./gradlew clean build

Všechny testy musí projít. Volitelně ruční průchod GUI (`./gradlew :modules:guiModule:run`) a snímky dialogů
(`./gradlew :modules:guiModule:runSnapshots -PsnapshotDir=/tmp/snap`, na Macu i na Windows).

## 3. Merge, tag, push

Vydání se taguje na `master`. Tagy jsou lightweight (`v2.7-beta`, `v2.7-beta2`, `v2.7`, `v2.8-beta`).

    git checkout master
    git merge --ff-only <vývojová větev>
    git tag v2.8-beta
    git push origin master
    git push origin v2.8-beta

## 4. Assets

### CLI zip, GUI zip, Linux portable (Mac nebo Linux s Dockerem)

    tools/release/build-assets.sh            # tag podle appVersion, výstup build/release/<tag>/
    tools/release/build-assets.sh v2.8-beta  # konkrétní tag

Skript vezme zdrojáky z tagu (`git archive`, necommitované změny se nepoužijí), v kontejneru `debian` pro
`linux/amd64` stáhne Liberica JDK 17 Full (JDK s JavaFX, jiná verze přes proměnnou `LIBERICA_URL`), sestaví jary
a portable aplikaci (`jpackage --type app-image`) a výsledek zabalí do tří zipů s názvy podle tabulky výše. Na Apple
Silicon běží přes emulaci amd64, trvá to několik minut. Testy se v kontejneru nespouštějí (krok 2).

Kontrola: CLI zip rozbalit a validovat testovací balíček, např.

    java -jar KomplexniValidatorCLI.jar --action VALIDATE_PSP \
        --psp <repo>/modules/sharedModule/src/test/resources/fund_unit_0.1/valid_clipping/nk-00027x \
        --config-dir validatorConfig --disable-imagemagick --disable-jhove --disable-jpylyzer \
        --disable-kakadu --disable-mp3val --disable-shntool --disable-checkmate

Výpis končí verzí a datem buildu z kroku 1.

### Windows portable a instalátor (ručně na Windows)

Současný postup, zatím bez skriptu (ladí se):

1. Na Windows x64 JDK 17 (u v2.7 a v2.8-beta JDK s JavaFX), pro instalátor WiX Toolset 3.x na PATH
   (jpackage 17 neumí WiX 4/5).
2. `git checkout <tag>`, `gradlew.bat clean :modules:guiModule:shadowJar`.
3. Zkopírovat `modules/sharedModule/src/main/resources/nkp/pspValidator/shared/validatorConfig` do
   `modules/guiModule/build/libs/`, aby se konfigurace dostala do aplikace (`app/validatorConfig`).
4. V `modules/guiModule/build.gradle`, úloha `jpackage`, dočasně přepnout řádky pro Windows: `iconPath` na
   `icon_48x48.ico` a `--type` na `exe` (instalátor) nebo `app-image` (portable). Změny necommitovat.
5. `gradlew.bat :modules:guiModule:jpackage`, výstup v `modules/guiModule/build/jpackage/`.
6. Přejmenovat podle tabulky: instalátor na `KomplexniValidatorGUI-<tag>_Windows_x86_64_installer.exe`, adresář
   app-image na `KomplexniValidatorGUI-<tag>_Windows_x86_64_portable` a ten zabalit do zipu stejného jména.
7. Kontrola: aplikace se spustí, „O aplikaci“ ukazuje verzi a datum buildu, instalátor projde.

## 5. GitHub release

Releases → Draft a new release, tag `<tag>`, název „Komplexní validátor `<tag>`“, u bety zaškrtnout
„Set as a pre-release“. Nahrát pět souborů z tabulky.

Text podle předchozích vydání, sekce v tomto pořadí (prázdné vynechat):

    # Seznam změn oproti v2.7

    ## Nové funkcionality:
    ## Podpora nových DMF:
    ## Oprava bugů:
    ## Změna úrovně chybové hlášky:

Položky ideálně s odkazem na issue (`issue #123: ...`).

## 6. Po vydání

- Finální verze po betě: zopakovat kroky 1 až 5 s verzí bez přípony (`VERSION_CODE`, `appVersion`, `BUILD_DATE`,
  `XSD_URL` na nový tag).
- Webový validátor (repozitáře `komplexni-validator-web-backend` a `-frontend`) spouští CLI jar s adresářem
  `validatorConfig` na serveru: nasadit obsah CLI zipu a ověřit, že backend čte namespace protokolu nové verze.
