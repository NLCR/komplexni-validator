# Komplexní validátor

Nástroj Národní knihovny ČR pro validaci SIP balíčků digitalizovaných dokumentů podle Definic metadatových formátů
(DMF) Standardu NDK. Kontroluje strukturu balíčku, soubor info.xml, METS, bibliografická metadata (MODS, DC),
technická a administrativní metadata (MIX, PREMIS, copyrightMD), soubory ALTO a OCR TXT a pomocí externích nástrojů
i obrazové a zvukové soubory. Od verze 3.0 validuje také e-publikace (e-monografie, e-periodika a skládaná
e-periodika): soubory PDF kontroluje veraPDF a soubory EPUB EPUBCheck. Výstupem je textový výpis a XML protokol.

Vydání, instalátory a seznamy změn: https://github.com/NLCR/komplexni-validator/releases. Standardy:
https://standardy.ndk.cz/. Uživatelská dokumentace je ve wiki: https://github.com/NLCR/komplexni-validator/wiki. Hlášení chyb a dotazy: GitHub issues.

## Instalace a spuštění

Vydání obsahuje šest souborů (podrobně v [release.md](release.md)):

| soubor | pro koho | Java |
|---|---|---|
| `KomplexniValidatorGUI-<verze>_Windows_x86_64_installer.exe` | instalátor GUI pro Windows x64, instaluje pro přihlášeného uživatele bez práv správce do `%LOCALAPPDATA%\KomplexniValidator` | nemusí být nainstalovaná |
| `KomplexniValidatorGUI-<verze>_Windows_x86_64_portable.zip` | GUI pro Windows x64 bez instalace: rozbalit a spustit `Komplexní validátor.exe` | nemusí být nainstalovaná |
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
v PATH. Nástroj, jehož verzi se nepodaří zjistit, se pro validaci vypne: pravidlo pak hlásí jednu informaci „nástroj … není dostupný“ (dříve CLI hlásilo chybu spuštění
u každého souboru). Když nástroj při kontrole souboru spadne, nespustí se nebo skončí neobvyklým návratovým kódem
(běžné kódy každého nástroje uvádí `validatorConfig/externalUtils.xml`, element `exitCodes`), validace u souboru
ohlásí chybu „nástroj … soubor nezkontroloval“ s kódem a začátkem výstupu nástroje; dříve mohl takový soubor projít bez
hlášky. Cesty k nástrojům i k balíkům smějí obsahovat mezery a českou diakritiku (ověřeno
na Windows 11 a macOS se všemi nástroji kromě Kakadu). Na Windows ale JHOVE, shntool, MP3val a Kakadu nenajdou soubor, jehož
cesta obsahuje znak mimo znakovou sadu systému (v českých Windows středoevropskou, tedy například řecké nebo ruské
písmeno v názvu adresáře); validace pak u takového balíku hlásí chyby těchto nástrojů, i když soubory jsou
v pořádku. Balíky proto na Windows ukládejte do cest bez takových znaků. Podporované verze nástrojů a odkud je brát:
[Podporované verze nástrojů](#podporované-verze-nástrojů).

### Podporované verze nástrojů

<!-- Na tento nadpis (kotva #podporované-verze-nástrojů) odkazuje aplikace (ExternalTools.SUPPORTED_VERSIONS_URL)
a wiki, nadpis neměnit. Podporované verze nástrojů se vedou jen tady. -->

Výstup nástroje (a tedy i to, jak ho validátor čte) se může mezi verzemi lišit. Validátor proto zná ověřené řady
verzí každého nástroje podle platformy: řada 7.1 znamená libovolnou verzi 7.1.x (například 7.1.2-35 i 7.1.3-1),
ne 7.10 ani 7.2. Zjištěná verze, která do žádné ověřené řady nepatří (nebo kterou nejde z výstupu nástroje rozpoznat),
se označí jako neověřená: CLI to vypíše pod řádkem s verzí nástroje, GUI ukáže u nástroje „neověřená verze“ a žlutou
tečku a protokol validace má u každého pravidla, které nástroj používá, jednu informaci (INFO), například „nástroj
JHOVE je ve verzi 1.36.0, která nebyla s validátorem ověřena (ověřené řady: 1.29, 1.34); výsledky kontroly se mohou
lišit“. Validace přitom běží dál a výsledky ani verdikt se nemění. Seznam je v konfiguraci validátoru
(`validatorConfig/externalUtils.xml`, element `verifiedVersions`) a aktualizuje se s novou verzí validátoru;
konfigurace bez seznamu (starší nebo vlastní) nic neoznačuje. Ověřené řady (v závorce přesná verze, na které se
ověřovalo):

<!-- overene-verze:zacatek (tabulku hlídá test ReadmeVerifiedVersionsTest proti externalUtils.xml) -->

| nástroj | macOS | Windows | Linux |
|---|---|---|---|
| ImageMagick | 7.1 (7.1.2-35) | 7.1 (7.1.2-35) | 6.9 (6.9.12-98), 7.1 (7.1.1-43) |
| JHOVE | 1.29 (1.29.0-SNAPSHOT), 1.34 (1.34.0) | 1.34 (1.34.0) | 1.28 (1.28.0), 1.34 (1.34.0) |
| Jpylyzer | 2.2 (2.2.1) | 2.2 (2.2.1) | 2.2 (2.2.1) |
| Kakadu | 8.5 (8.5), 8.7 (8.7) | 7.9 (7.9.1), 8.5 (8.5), 8.7 (8.7) | 8.1 (8.1), 8.5 (8.5), 8.6 (8.6.2), 8.7 (8.7) |
| MP3val | 0.1 (0.1.8) | 0.1 (0.1.8) | 0.1 (0.1.8) |
| shntool | 3.0 (3.0.10) | 3.0 (3.0.10) | 3.0 (3.0.10) |
| Checkmate | 0.22 (0.22) | 0.22 (0.22) | 0.22 (0.22) |
| veraPDF | 1.30 (1.30.2) | 1.30 (1.30.4) | 1.30 (1.30.4) |
| EPUBCheck | 5.4 (5.4.0) | 5.4 (5.4.0) | 5.4 (5.4.0) |

<!-- overene-verze:konec -->

Linux je ověřený na Debianu 13 (arm64 a x86-64) a Ubuntu 24.04 (arm64) s nástroji z distribuce, kde v ní jsou (ImageMagick,
JHOVE 1.28, MP3val, shntool), jinak s oficiálními balíky a instalátory (JHOVE 1.34, Jpylyzer, Checkmate, veraPDF,
EPUBCheck). ImageMagick řady 6.9 je ověřený na Ubuntu 24.04 (6.9.12-98), z PATH i se zadaným adresářem `/usr/bin`
(validátor od verze 3.0 zachytí i hlášky s názvem programu `identify-im6.q16`, který ImageMagick 6 z Debianu
a Ubuntu vypisuje při spuštění z PATH). Kakadu je ověřené na sestaveních ze zdrojového SDK výrobce (s licencí), demo binárky výrobce ověřené
nejsou. Řada Kakadu 8.2 na Linuxu nevyhovuje: při vícevláknovém dekódování hlásí i u bezvadných souborů falešnou
chybu („System is entering deadlock“).

Požadavky zjištěné při ověřování:

- ImageMagick musí umět JPEG 2000: ve výpisu `magick -version` (na Linuxu a macOS `convert -version`) obsahuje řádek
  `Delegates` hodnotu `jp2`. Bez ní validátor hlásí chybu u každého souboru JP2. Běžná formule Homebrew `imagemagick`
  JPEG 2000 neumí, formule `imagemagick-full` ano. Na Windows validátor volá `magick.exe` (ImageMagick 7), na Linuxu
  a macOS příkazy `convert` a `identify`.
- JHOVE nejméně 1.28. Běží na Javě (8 nebo novější).
- Jpylyzer řady 2.x: výstup Jpylyzeru 1.x validátor nepřečte. Spouští se příkaz `jpylyzer` (Windows `jpylyzer.exe`),
  ne skript `jpylyzer.py`.
- Kakadu je komerční software: potřebuje vlastní licenci, nebo demo od výrobce s jeho podmínkami použití. Nástroj je
  volitelný, soubory JPEG 2000 kontrolují i JHOVE, Jpylyzer a ImageMagick.
  Knihovny dodané s nástrojem (u Kakadu adresář `lib/<platforma>` vedle `bin/<platforma>`, nebo přímo vedle
  programu) si validátor na macOS a Linuxu najde sám, stačí zadat adresář s programem `kdu_expand`; proměnnou
  `DYLD_LIBRARY_PATH` ani `LD_LIBRARY_PATH` není potřeba nastavovat.
- veraPDF nejméně 1.24 (sjednocený formát XML výstupu), EPUBCheck nejméně 5.0 (EPUB 3.3). Oba potřebují Javu (veraPDF
  8, 11, 17 nebo 21, EPUBCheck 8 nebo novější), EPUBCheck ji bere z PATH (viz
  [veraPDF a EPUBCheck](#verapdf-a-epubcheck-e-publikace)).
- Linux, nástroje z oficiálních instalátorů: veraPDF (instalátor, např. do `/opt/verapdf`) a EPUBCheck (rozbalený zip) a
  Kakadu v `PATH` nejsou. CLI hlásí veraPDF jako nenalezený (`Cannot run program "verapdf"`) a EPUBCheck se bez cesty hledá
  jako `epubcheck.jar` v pracovním adresáři, tedy také nenajde. Adresář proto vždy zadejte, například
  `--verapdf-path /opt/verapdf --epubcheck-path /opt/epubcheck-5.4.0 --kakadu-path <adresář s kdu_expand>`; v GUI
  Nastavení → Externí nástroje. Nástroje z balíčků distribuce (ImageMagick, JHOVE, MP3val, shntool) bývají v `/usr/bin`
  a v `PATH` jsou.
- Linux, výpis CLI a kódování: spouštějte s UTF-8 locale, například `LANG=C.UTF-8 java -jar KomplexniValidatorCLI.jar …`.
  V locale `C` nebo `POSIX` (kontejner, služba, cron) se česká diakritika ve výpisu zobrazí jako otazníky a Java
  nenajde ani balík, jehož cesta obsahuje diakritiku. Výpis (ne cesty) opraví i volba
  `-Dstdout.encoding=UTF-8` (ověřeno s Javou 21); `-Dfile.encoding=UTF-8` samotná nestačí. XML protokol (`--xml-protocol-file`) je v UTF-8
  vždy, bez ohledu na locale.

Nástroje se instalují z oficiálních stránek projektů: [ImageMagick](https://imagemagick.org/),
[JHOVE](https://jhove.openpreservation.org/), [Jpylyzer](https://jpylyzer.openpreservation.org/),
[Kakadu](https://kakadusoftware.com/), [MP3val](https://mp3val.sourceforge.net/),
[shntool](http://shnutils.freeshell.org/shntool/), [Checkmate](https://github.com/Sjord/checkmate),
[veraPDF](https://verapdf.org/), [EPUBCheck](https://github.com/w3c/epubcheck); instalátory nejsou součástí
repozitáře ani vydaných zipů. Postup instalace pro Windows, macOS a Linux je na wiki, stránka
[Instalace](https://github.com/NLCR/komplexni-validator/wiki/Instalace).

### veraPDF a EPUBCheck (e-publikace)

Soubory PDF a EPUB e-publikací kontrolují [veraPDF](https://verapdf.org/) (validace PDF/A) a
[EPUBCheck](https://github.com/w3c/epubcheck). Podporované verze a potřebná Java:
[Podporované verze nástrojů](#podporované-verze-nástrojů).

| nástroj | parametr CLI | adresář, který se zadává (CLI i GUI: Nastavení → Externí nástroje, „Nastavit cestu…“) |
|---|---|---|
| veraPDF | `--verapdf-path` | instalační adresář se skriptem `verapdf` (Windows `verapdf.bat`) |
| EPUBCheck | `--epubcheck-path` | rozbalený zip s `epubcheck.jar` a podadresářem `lib` |

Instalace na macOS přes Homebrew (`brew install verapdf epubcheck`):

    --verapdf-path "$(brew --prefix)/bin"
    --epubcheck-path "$(brew --prefix epubcheck)/libexec"

U EPUBCheck se nesmí zadat `$(brew --prefix)/bin`: je tam jen spouštěcí skript `epubcheck` bez `epubcheck.jar`.
Homebrew formule veraPDF má verzi 1.30.2, `verapdf --version` ale vypíše `veraPDF 1.30.0` (verze aplikační části,
knihovny jsou 1.30.2); jde o stejnou instalaci. Jinde: veraPDF instalátorem ze stránky projektu (spustit `java -jar`
na rozbalený jar), EPUBCheck jako zip ze stránky vydání projektu na GitHubu.

Adresář EPUBChecku smí mít v cestě mezery (například `C:\Program Files\epubcheck-5.4.0`), platí to na Windows
i na macOS. EPUBCheck se spouští příkazem `java -jar <adresář>/epubcheck.jar`, kde `java` je program z PATH, ne Java, ve které
běží validátor. Na stroji bez `java` v PATH (typicky GUI z instalátoru, které má vlastní přibalenou Javu) EPUBCheck
nepoběží a validace ho ohlásí jako nedostupný; je potřeba nainstalovat Javu 8 nebo novější a přidat ji do PATH.

## Konfigurace validace

Pravidla jsou datová (XML) a jsou popsaná přímo v souborech fDMF: `rules.xml` (pravidla), `variables.xml`
(proměnné), `patterns.xml` (vzory názvů souborů), `namespaces.xml`, adresáře `xsd/`, `biblioProfiles/`,
`techProfiles/`, `metsProfiles/`, `binaryFileProfiles/`. Dostupné validační a evaluační funkce s parametry jsou
vypsané ve `validatorConfig/functions/`. Jednotlivé soubory konfigurace verzi nenesou, verzi celé konfigurace určuje
jen `manifest.properties` (viz níže).

Adresář konfigurace obsahuje soubor `manifest.properties` s klíčem `configVersion` (MAJOR.MINOR verze validátoru, se
kterou byla konfigurace vydána, např. `3.0`). Validátor ho kontroluje hned při načtení adresáře: validátor 3.x načte
jen konfiguraci 3.x. Konfigurace z verze 2.x a starší manifest nemá, takže ji validátor odmítne s hláškou, jakou
verzi potřebuje. Kdo má vlastní kopii konfigurace z verze 2.x, musí ji nahradit konfigurací z verze 3.x (nebo ji
převést ručně a doplnit manifest). Jiná minor verze (např. konfigurace 3.0 pro validátor 3.1) není chyba: validátor na ni upozorní (GUI při startu a v Nastavení, CLI řádkem „Upozornění:“) a pokračuje.

## Verze

Verze aplikace je jen v `gradle.properties` (`appVersion`), verzi a datum buildu z ní při buildu generuje Gradle
pro třídu `Version`. Vydání se značí tagem `v<appVersion>` (například `v3.0-beta`, `v3.0`, `v3.0.1`) a publikuje jako GitHub release; postup vydání, assets a jejich
sestavení (`tools/release/build-assets.sh`): viz [release.md](release.md). Konvence verze (včetně předvydání
a číselné verze instalátoru) je popsaná v `gradle/app-version.gradle`.

Předvydání (`-alpha`, `-beta`, `-rc`) jsou určená k testování a mají na GitHubu příznak předvydání. Mohou obsahovat
chyby; nalezené chyby prosím hlaste jako issue na https://github.com/NLCR/komplexni-validator/issues.

## Licence

Apache License 2.0, viz [LICENSE](LICENSE).
