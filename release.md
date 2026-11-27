# Vydání nové verze

Postup vydání Komplexního validátoru (CLI a GUI) jako GitHub release. Ověřeno na v2.8-beta (24. 9. 2026), aktualizováno pro v2.9
(8. 11. 2026) a pro verzování od 3.0 (12. 11. 2026).

## Assets

Každý release má šest souborů, `<tag>` je např. `v2.8`:

| soubor | obsah | kde se staví |
|---|---|---|
| `KomplexniValidatorCLI-<tag>.zip` | adresář stejného jména: `KomplexniValidatorCLI.jar` (jar se všemi závislostmi) a `validatorConfig/` | skript, Docker |
| `KomplexniValidatorGUI-<tag>.zip` | adresář stejného jména: `KomplexniValidatorGUI.jar` a `validatorConfig/`; běží na Javě 17+ s JavaFX (jar obsahuje nativní knihovny JavaFX jen pro Linux) | skript, Docker |
| `KomplexniValidatorGUI-<tag>_Linux_x64_portable.zip` | portable aplikace pro Linux x64 | skript, Docker |
| `KomplexniValidatorGUI-<tag>_Windows_x86_64_portable.zip` | portable aplikace pro Windows x64 | ručně na Windows |
| `KomplexniValidatorGUI-<tag>_Windows_x86_64_installer.exe` | instalátor pro Windows x64 | ručně na Windows |
| `KomplexniValidatorGUI-<tag>_macOS_AppleSilicon_installer.dmg` | instalátor pro macOS na Apple Silicon (arm64) | ručně na Macu s Apple Silicon |

**Portable** je aplikace, která se neinstaluje: zip se rozbalí kamkoli a spustí se `Komplexní validátor.exe` (Windows)
nebo `bin/KomplexniValidator` (Linux). Obsahuje vlastní Javu (adresář `runtime`), na stroji tedy nemusí být Java
a nejsou potřeba práva správce. Technicky jde o výstup `jpackage --type app-image`, tedy totéž, co instalátor
nakopíruje do cílového adresáře. Konfigurace validace je uvnitř v `app/validatorConfig` (Linux `lib/app/validatorConfig`),
`config.properties` a logy mimo aplikaci: Windows `%APPDATA%\KomplexniValidator`, Linux `~/.config/KomplexniValidator`
(nebo `$XDG_CONFIG_HOME/KomplexniValidator`).

**Windows instalátor** instaluje jen pro přihlášeného uživatele, bez práv správce, do
`%LOCALAPPDATA%\KomplexniValidator` (adresář lze v průvodci změnit). Vytvoří zástupce v nabídce Start (složka
„Komplexní validátor“) a na ploše (průvodce se zeptá). Instalace novější verze starší nahradí, i mezi předvydáními
téže verze (alpha, beta, finální), viz číselná verze instalátoru v kap. 1.

**macOS dmg** obsahuje aplikaci `KomplexniValidator.app` s vlastní Javou, uživatel ji přetáhne do Aplikací (práva
správce nejsou potřeba). Konfigurace validace je v bundlu (`Contents/app/validatorConfig`), `config.properties` a logy
v `~/Library/Application Support/KomplexniValidator`. Dmg není podepsané ani notarizované: po stažení ho macOS
zablokuje a aplikaci je třeba povolit v Nastavení → Soukromí a zabezpečení → „Přesto otevřít“. Pro Macy s procesorem
Intel se nevydává nic (lze použít GUI zip s Javou s JavaFX).

## 1. Příprava verze

- **Konvence verze:** `MAJOR.MINOR[.PATCH]` a volitelná přípona předvydání `-alpha[N]`, `-beta[N]`, `-rc[N]` nebo `-dev`:
  `3.0-alpha`, `3.0-alpha2`, `3.0-beta`, `3.0-beta2`, `3.0-rc1`, `3.0`, `3.0.1`. První předvydání daného stupně je bez
  čísla, další se číslují od 2. Přípona `-dev` označuje rozpracovaný stav mezi vydáními a nikdy se netaguje. Tag je
  vždy `v<appVersion>` (`v3.0-beta`, `v3.0`). Jiný tvar sestavení odmítne (`gradle/app-version.gradle`).
- **Číselná verze instalátoru** se z `appVersion` odvozuje sama. macOS a Linux: `MAJOR.MINOR.PATCH` bez přípony
  (`3.0-beta2` → 3.0.0). Windows: třetí složka je `PATCH × 1000 + stupeň` (`-dev` 0, alpha 100 + N, beta 200 + N,
  rc 300 + N, finální 900), tedy `3.0-alpha` → 3.0.101, `3.0-beta2` → 3.0.202, `3.0` → 3.0.900, `3.0.1` → 3.0.1900.
  Windows tak každé další sestavení téže verze bere jako novější a starší při instalaci nahradí; číslo je vidět
  v Nastavení → Aplikace. Kvůli tomu je PATCH nejvýš 64 a název ani výrobce produktu v instalátoru se nesmí měnit.
- `gradle.properties`: `appVersion`, např. `3.0-beta` nebo `3.0.1`. Je to jediné místo s verzí aplikace: Gradle
  z něj při buildu vygeneruje `version.properties` (verze a datum buildu, v `modules/sharedModule/build/generated/`,
  ne v gitu) a z něj ji čte `Version` (`VERSION_CODE`, `BUILD_DATE`) pro CLI `--version`, GUI a protokoly. Datum
  buildu je den sestavení, ručně se nenastavuje. Při běhu z IDE bez Gradle buildu je verze `0.0.0-dev`, datum
  „neznámé“ a kontrola `validatorVersion` je vypnutá.
- Atribut `validatorVersion` konfiguračních souborů (fDMF, profily metadat, profily binárních souborů) se porovnává
  jen s **major** verzí aplikace: aplikace 3.x (3.0-alpha, 3.0, 3.0.1, 3.2) načte konfiguraci s `validatorVersion`
  „3.0“ i „3.1“, s „2.9“ ne. Nové konfigurace nesou verzi platnou při vzniku (dnes „3.0“), při minor a patch vydání
  se atribut nemění.
- **Manifest konfigurace:** `modules/sharedModule/src/main/resources/nkp/pspValidator/shared/validatorConfig/manifest.properties`
  nese `configVersion=MAJOR.MINOR` (bez PATCH a přípony předvydání). Při změně **MINOR** nebo **MAJOR** verze aplikace
  ho uprav na novou hodnotu (3.0-beta → 3.0 ji nemění, 3.0 → 3.1 ano); hlídá to test `ConfigManifestVersionTest`.
  Validátor při načtení adresáře konfigurace manifest kontroluje: jiná major verze, chybějící nebo neplatný manifest je
  chyba, jiná minor verze jen varování.
- Hromadná změna (atribut `validatorVersion` a namespace `pspValidator/X.Y/` v konfiguraci, XSD a protokolu, vzor:
  commit 36094cb5 „set version to 2.9“) je potřeba jen při změně **major** verze, příště u 4.0.
- `ValidatorProtocolXmlBuilder.XSD_URL`: předvydání (alpha, beta) odkazují na větev `master`
  (`.../komplexni-validator/master/...`); při finálním vydání se přepne na tag vydání (`refs/tags/v3.0/...`)
  a po vydání zpět na `master`.
- Commit ve stylu „set version to 3.0-beta“.

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

### Windows instalátor a portable (ručně na Windows x64)

Úloha `jpackage` v `modules/guiModule/build.gradle` volí typ balíčku, ikonu a volby podle OS a výstup sama pojmenuje
podle tabulky výše. V `build.gradle` se před buildem nic ručně nemění. `validatorConfig` přidá úloha
`syncValidatorConfigForJpackage` (společně s jarem do `build/jpackage-input`), testy se nespouštějí (krok 2).

1. Jednorázová příprava stroje (Windows 10 nebo 11 x64):
   - **JDK 17 nebo 21 x64**, např. Temurin z <https://adoptium.net/temurin/releases/> (instalátor `.msi`,
     v průvodci zapnout „Set JAVA_HOME variable“ a „Add to PATH“). JDK s JavaFX není potřeba; kompilace je vždy pro Javu 17
     (`options.release = 17`), instalátor si přibalí runtime použitého JDK.
   - **WiX Toolset 3.14** z <https://github.com/wixtoolset/wix3/releases> (soubor `wix314.exe`, nebo `wix314-binaries.zip`
     rozbalený např. do `C:\wix314`). Adresář s `candle.exe` a `light.exe` přidat do proměnné PATH (Start → „Upravit
     proměnné prostředí systému“ → Proměnné prostředí → Path → Nový; instalátor `wix314.exe` ho přidává do
     `C:\Program Files (x86)\WiX Toolset v3.14\bin`). jpackage 17 neumí WiX 4 ani 5.
   - Kontrola v novém okně příkazového řádku (`cmd`): `java -version` ukazuje 17, `candle -?` vypíše nápovědu WiX.
2. `git checkout <tag>`, v kořeni repozitáře:

        gradlew.bat clean :modules:guiModule:jpackage
        gradlew.bat :modules:guiModule:jpackage -PjpackageType=app-image

   Úloha na začátku vypíše použitý `jpackage` (z `JAVA_HOME`, jinak z PATH) a jeho verzi. Výstup v
   `modules\guiModule\build\jpackage\`: `KomplexniValidatorGUI-<tag>_Windows_x86_64_installer.exe`,
   adresář `KomplexniValidatorGUI-<tag>_Windows_x86_64_portable` a zip stejného jména. Jiný typ instalátoru
   `-PjpackageType=msi`.
3. Volitelně podpis: NK podepisuje exe ručně mimo build svým certifikátem (např.
   `signtool sign /fd SHA256 /tr http://timestamp.digicert.com /td SHA256 /a <soubor>.exe`), před nahráním do release.
   Název souboru se podpisem nemění.
4. Kontrola instalátoru: spustit, projít průvodce (cíl `%LOCALAPPDATA%\KomplexniValidator`, bez dotazu na práva
   správce), aplikace je v nabídce Start ve složce „Komplexní validátor“ (a na ploše, pokud byl zástupce zvolen),
   spustí se, „O aplikaci“ ukazuje verzi, datum buildu a cestu ke konfiguraci v instalaci
   (`...\KomplexniValidator\app\validatorConfig`), validace balíčku proběhne. Odinstalace přes Nastavení → Aplikace.
   Instalace novějšího sestavení přes starší ho nahradí, protože číselná verze instalátoru roste i mezi
   předvydáními (kap. 1). Sestavení se stejnou číselnou verzí se přes sebe neinstalují: starší je nutné nejdřív
   odinstalovat (dvě různá sestavení téže verze se proto nevydávají).
5. Kontrola portable: zip rozbalit jinam, spustit `Komplexní validátor.exe`, „O aplikaci“ jako výše.

### macOS dmg (ručně na Macu s Apple Silicon)

1. JDK 17 arm64 (např. Temurin 17); `jpackage` se bere z `JAVA_HOME`, jinak z PATH (úloha vypíše, který). JavaFX pro
   macOS arm64 dodá gradle plugin do jaru, JDK s JavaFX není potřeba. Gradle a jpackage musí běžet nativně na arm64,
   jinak vznikne balík pro Intel (pak má název `_macOS_Intel_`).
2. `git checkout <tag>`, `JAVA_HOME=$(/usr/libexec/java_home -v 17) ./gradlew clean :modules:guiModule:jpackage`,
   výstup `modules/guiModule/build/jpackage/KomplexniValidatorGUI-<tag>_macOS_AppleSilicon_installer.dmg`.
   Dmg není podepsané ani notarizované (případný `codesign` a notarizace by šly ručně po buildu).
   `Info.plist` aplikace vzniká z `modules/guiModule/src/main/jpackage/macos/Info.plist` (šablona jpackage 17
   s češtinou jako lokalizací aplikace, aby systémové dialogy výběru souboru a adresáře měly česká tlačítka); kontrola
   `plutil -p KomplexniValidator.app/Contents/Info.plist` ukazuje `CFBundleDevelopmentRegion` = `cs`
   a `CFBundleLocalizations` = `cs` vedle verze, identifikátoru a ikony.
   Název „Komplexní validátor“ ve Finderu, v Docku a při přepínání aplikací dodává lokalizace
   `src/main/jpackage/macos/cs.lproj/InfoPlist.strings`: úloha `jpackage` ji přidá do hotové aplikace a znovu ji
   podepíše (ad hoc), instalátor dmg proto staví ve dvou krocích. Při podpisu vlastním certifikátem se musí
   podepisovat až po tomto kroku.
3. Kontrola bez instalace a bez zápisu do domovského adresáře: dmg připojit (`hdiutil attach -readonly`), aplikaci
   zkopírovat do dočasného adresáře a spustit z `/` v režimu snímků:

        cd / && JAVA_TOOL_OPTIONS="-Duser.home=<tmp>/home -Dkv.snapshotDir=<tmp>/snap" \
            <tmp>/KomplexniValidator.app/Contents/MacOS/KomplexniValidator

   V `<tmp>/snap` musí vzniknout snímky všech dialogů, `about.png` ukazuje verzi, datum a cestu ke konfiguraci v bundlu.

`-PjpackageType=app-image` na Macu dá `KomplexniValidator.app` (název zůstává, jinak by ji Finder nebral jako
aplikaci) a zip `KomplexniValidatorGUI-<tag>_macOS_AppleSilicon_portable.zip`; nevydává se.

## 5. GitHub release

Releases → Draft a new release, tag `<tag>`, název „Komplexní validátor `<tag>`“, u bety zaškrtnout
„Set as a pre-release“. Nahrát soubory z tabulky.

Text podle předchozích vydání, sekce v tomto pořadí (prázdné vynechat):

    # Seznam změn oproti v2.7

    ## Nové funkcionality:
    ## Podpora nových DMF:
    ## Oprava bugů:
    ## Změna úrovně chybové hlášky:

Položky ideálně s odkazem na issue (`issue #123: ...`).

## 6. Po vydání

- Finální verze po betě: zopakovat kroky 1 až 5 s verzí bez přípony (`appVersion`, `XSD_URL` na nový tag).
- Po finálním vydání vrátit `XSD_URL` na větev `master` a `appVersion` na vývojovou verzi další verze (např. `3.1-dev`).
- Webový validátor (repozitáře `komplexni-validator-web-backend` a `-frontend`) spouští CLI jar s adresářem
  `validatorConfig` na serveru: nasadit obsah CLI zipu a ověřit, že backend čte namespace protokolu nové verze.
