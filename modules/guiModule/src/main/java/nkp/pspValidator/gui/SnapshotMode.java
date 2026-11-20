package nkp.pspValidator.gui;

import javafx.animation.AnimationTimer;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.stage.PopupWindow;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Duration;
import nkp.pspValidator.gui.newvalidation.OutputSettings;
import nkp.pspValidator.gui.newvalidation.ValidationRequest;
import nkp.pspValidator.gui.newvalidation.ValidationSource;
import nkp.pspValidator.gui.newvalidation.VersionSettings;
import nkp.pspValidator.gui.newvalidation.DmfParamsBuilder;
import nkp.pspValidator.gui.settings.SettingsTab;
import nkp.pspValidator.shared.Dmf;
import nkp.pspValidator.shared.DmfDetector;
import nkp.pspValidator.shared.FdmfRegistry;
import nkp.pspValidator.shared.engine.Level;
import nkp.pspValidator.shared.engine.Rule;
import nkp.pspValidator.shared.engine.RulesSection;
import nkp.pspValidator.shared.engine.validationFunctions.ValidationProblem;
import nkp.pspValidator.shared.externalUtils.ExternalUtil;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * Vývojářský režim: po inicializaci validačních dat postupně otevře vybrané dialogy, uloží snímek každého z nich
 * do PNG a aplikaci ukončí. Slouží ke kontrole vzhledu napříč platformami (macOS, Windows) bez ručního klikání
 * a bez oprávnění ke snímání obrazovky; stejný příkaz lze pustit lokálně i v CI.
 * <p>
 * Zapíná se systémovou vlastností {@code -Dkv.snapshotDir=<adresář>}. Výběr dialogů volitelně přes
 * {@code -Dkv.snapshot=main,mainMenu,settingsMenu,newValidation,settings,externalUtils,toolDetails,about,dictionaryContent,
 * dictionaryUpdate,init,initError,unzip,unzipError} (výchozí je vše; navíc {@code dev}, vývojářský dialog),
 * prodleva před snímkem přes {@code -Dkv.snapshotDelayMs} (výchozí 800 ms, aby proběhl layout a asynchronní
 * inicializace dialogu). {@code mainMenu} otevře v hlavním okně menu Validovat a uloží snímek jeho rozbalovací nabídky,
 * {@code settingsMenu} totéž pro menu Nastavení. {@code toolDetails} je okno „Podrobnosti…“ u nenalezeného nástroje
 * (JHOVE s pevným výstupem kontroly), {@code dictionaryContent} a {@code dictionaryUpdate} dialogy slovníku
 * siglaInstitutionCodes; krok {@code dictionaryUpdateRun} v dialogu aktualizace slovníku skutečně spustí aktualizaci
 * z adresy v nastavení a vyfotí výsledek (soubor slovníku se přepíše, proto krok odmítne konfiguraci validátoru ve
 * zdrojích repozitáře: {@code validatorConfig.dir} musí ukazovat na kopii). {@code init}, {@code initError}, {@code unzip} a {@code unzipError} jsou náhledy dialogů
 * inicializace validačních dat a rozbalení ZIPu (běh a chyba): dialog jen ukáže stav, nic nenačítá ani nerozbaluje.
 * Snímkovací režim přeskakuje dialog nástrojů při startu, proto při spuštění sám zkontroluje nástroje, aby prázdný
 * stav hlavního okna neukazoval výchozí (nepravdivou) dostupnost.
 * <p>
 * Okno Nastavení (krok {@code settings}): záložky podle {@code -Dkv.snapshotTab=versions,tools,skipping,dictionaries,dirs}
 * (výchozí všechny), pro každou snímek {@code settings-<záložka>.png}. Krok {@code settingsSave} ověří propsání výchozí
 * verze: vypíše {@code DmfDetector.Params} z uloženého nastavení, v záložce Výchozí verze DMF vybere verzi podle
 * {@code -Dkv.snapshotDefaultVersion=<TYP>:<verze>} (typ jako {@code Dmf.Type}, verze {@code default} = výchozí
 * validátoru; výchozí {@code MONOGRAPH:1.4}) jako uživatel ({@code settings-changed.png}), uloží ji tlačítkem Uložit,
 * vypíše {@code DmfDetector.Params} znovu, okno otevře znovu a uloží {@code settings-reopened.png}.
 * <p>
 * Dialog Nová validace lze otevřít s předvyplněnou cestou ({@code -Dkv.snapshotPsp=<adresář nebo soubor .zip>})
 * a rozbalenou sekcí ({@code -Dkv.snapshotExpand=versions|tools|skipped}), aby šly nasnímat všechny stavy.
 * <p>
 * Skutečná validace z GUI: {@code -Dkv.snapshotValidate=<adresář balíku nebo soubor .zip>} a v seznamu kroků
 * {@code results} spustí validaci stejnou cestou jako dialog Nová validace (požadavek z uloženého nastavení verzí
 * a výstupů, {@code Main.launchValidation}), počká na její konec, vybere první sekci a pravidlo s problémem a uloží
 * snímek {@code results.png}. Volitelně {@code -Dkv.snapshotDisable=JHOVE,KAKADU} vypne nástroje pro tuto validaci
 * (jako zaškrtávátka v dialogu) a {@code -Dkv.snapshotRunningMs=<ms>} uloží i snímek {@code running.png} během běhu.
 * Krok {@code rerun} pak spustí „Validovat znovu“, počká na konec a uloží {@code rerun.png}; do výpisu napíše
 * požadavek opakované validace. Krok {@code setPreferred} s {@code -Dkv.snapshotRerunPreferred=<TYP>:<verze>} (typ jako
 * {@code Dmf.Type}, verze {@code default} = výchozí validátoru) změní výchozí verzi v nastavení, jako by ji uživatel
 * uložil v Nastavení (následné „Validovat znovu“ ji má použít). Kroky {@code helpMenu}, {@code showMenu}
 * a {@code validateMenu} vyfotí rozbalená menu Nápověda, Zobrazit a Validovat (u Validovat vypíše bublinu položky
 * „Validovat znovu“), krok {@code revealXml} skutečně spustí „Zobrazit ve složce“ u protokolu XML zobrazené validace
 * (otevře jedno okno správce souborů) a vypíše příkaz a výsledek. Krok {@code small} zmenší hlavní okno na 1000 × 640 a uloží {@code small.png}
 * (další kroky pak běží v malém okně). Krok {@code filtered} zapne filtr „chyby a varování, jen sekce a pravidla
 * s problémy“, vybere první problém a uloží {@code filtered.png}. Výsledek každé validace se vypíše na standardní výstup.
 * <p>
 * Průběh validace (krok {@code progress}, také s {@code -Dkv.snapshotValidate}): vlákno validace se přes háček
 * {@link MainController.RunHook} pozastaví po ohlášení pravidel ({@code initializing.png}, panel „Inicializuji…“)
 * a po N-tém dokončeném pravidle ({@code -Dkv.snapshotPauseAt}: podíl 0 až 1, výchozí 0.5, nebo počet pravidel).
 * V pauze uloží {@code running.png}, rozbalené menu Validovat {@code running-menu.png}, pak vybere první hotovou
 * sekci a pravidlo s problémem ({@code running-browse.png}), nechá doběhnout dalších asi 10 % pravidel a uloží
 * {@code running-browse-later.png} (do výpisu napíše, zda výběr a seznam problémů zůstaly). Konec podle
 * {@code -Dkv.snapshotProgressEnd}: {@code finish} (výchozí, doběhne a uloží {@code finished.png} bez změny výběru),
 * {@code cancel} (zruší validaci jako tlačítko, {@code canceled.png}) nebo {@code fail} (háček ve vlákně validace
 * vyhodí výjimku, jako by selhalo pravidlo, {@code failed.png}). Během validace se měří nejdelší prodleva mezi
 * snímky UI (pulzy JavaFX) a vypíše se.
 * <p>
 * Krok {@code stress} změří odezvu UI bez validátoru: pošle oknu syntetické události jádra (20 sekcí po 15 pravidlech,
 * první pravidlo s 5 000 problémy, ostatní po 20), po prvním pravidle ho vybere a pak co nejrychleji pošle zbytek;
 * vypíše nejdelší prodlevu UI, dobu zpracování a zda se seznam problémů vybraného pravidla nepřestavěl
 * ({@code stress.png}).
 * <p>
 * Soubor nastavení se výchozně čte z obvyklého umístění (při spuštění z Gradle je to {@code config.properties}
 * v pracovním adresáři modulu), takže snímky odrážejí osobní nastavení vývojáře. Pro snímky nad čistým nastavením
 * lze zadat vlastní soubor systémovou vlastností {@code -Dkv.configFile=<soubor>} (nemusí existovat, vznikne s výchozími
 * hodnotami). Při spuštění z Gradle ale chybí výchozí cesta ke konfiguraci validace, proto soubor musí obsahovat alespoň
 * {@code validatorConfig.dir=<adresář validatorConfig>} (například
 * {@code modules/sharedModule/src/main/resources/nkp/pspValidator/shared/validatorConfig}) a nejlépe i
 * {@code validation.log_dir}, jinak aplikace čeká v dialogu inicializace validačních dat.
 * <p>
 * Skupina balíků: {@code -Dkv.snapshotSource=group} zapne zdroj Skupina balíků v dialogu Nová validace
 * ({@code -Dkv.snapshotPsp} je pak adresář skupiny) i ve validaci ({@code -Dkv.snapshotValidate} je adresář skupiny).
 * Krok {@code group} spustí validaci skupiny, pozastaví vlákno validace v polovině pravidel balíku v polovině skupiny
 * ({@code group-running.png}), vybere hotový balík ({@code group-browse.png}), nechá skupinu doběhnout, vybere první
 * nevalidní balík ({@code group-results.png}), zapne filtr „jen nevalidní“ ({@code group-filtered.png}, pak ho vypne)
 * a vyexportuje souhrn do CSV ({@code -Dkv.snapshotCsv=<soubor>}, výchozí {@code group-souhrn.csv} v adresáři snímků).
 * Krok {@code groupRerunOne} validuje znovu jen první nevalidní balík, s {@code -Dkv.snapshotRerunPackage=<název>} balík
 * tohoto názvu ({@code group-rerun-one.png}), krok
 * {@code groupCancel} spustí skupinu znovu a v polovině ji zruší ({@code group-canceled.png}).
 * <p>
 * Přes gradle: {@code ./gradlew :modules:guiModule:runSnapshots -PsnapshotDir=/tmp/snap [-PsnapshotConfigFile=/tmp/snap-config.properties]}.
 */
public class SnapshotMode {

    public static final String PROP_DIR = "kv.snapshotDir";
    public static final String PROP_DIALOGS = "kv.snapshot";
    public static final String PROP_DELAY_MS = "kv.snapshotDelayMs";
    public static final String PROP_PSP = "kv.snapshotPsp";
    public static final String PROP_EXPAND = "kv.snapshotExpand";
    public static final String PROP_VALIDATE = "kv.snapshotValidate";
    public static final String PROP_DISABLE = "kv.snapshotDisable";
    public static final String PROP_RUNNING_MS = "kv.snapshotRunningMs";
    public static final String PROP_PAUSE_AT = "kv.snapshotPauseAt";
    public static final String PROP_PROGRESS_END = "kv.snapshotProgressEnd";
    public static final String PROP_SOURCE = "kv.snapshotSource";
    public static final String PROP_CSV = "kv.snapshotCsv";
    public static final String PROP_TAB = "kv.snapshotTab";
    public static final String PROP_DEFAULT_VERSION = "kv.snapshotDefaultVersion";
    public static final String PROP_RERUN_PREFERRED = "kv.snapshotRerunPreferred";
    public static final String PROP_RERUN_PACKAGE = "kv.snapshotRerunPackage";
    private static final long VALIDATION_TIMEOUT_MS = 10 * 60 * 1000;
    public static final String ALL_DIALOGS = "main,mainMenu,settingsMenu,newValidation,settings,externalUtils,toolDetails,"
            + "about,dictionaryContent,dictionaryUpdate,init,initError,unzip,unzipError";
    /** slovník pro snímky dialogů obsahu a aktualizace slovníku */
    private static final String SNAPSHOT_DICTIONARY = "siglaInstitutionCodes";

    public static boolean isEnabled() {
        return System.getProperty(PROP_DIR) != null;
    }

    /**
     * Spustí sekvenci snímků. Volat z FX vlákna po nastavení ValidationDataManager.
     */
    public static void start(Main main) {
        File dir = new File(System.getProperty(PROP_DIR));
        dir.mkdirs();
        long delayMs = Long.parseLong(System.getProperty(PROP_DELAY_MS, "800"));
        List<String> names = new ArrayList<>(Arrays.asList(System.getProperty(PROP_DIALOGS, ALL_DIALOGS).split("\\s*,\\s*")));

        Map<String, Supplier<AbstractDialog>> dialogs = new LinkedHashMap<>();
        Stage dialogStage = main.getDialogStage();
        String psp = System.getProperty(PROP_PSP);
        NewValidationDialog.Expand expand = parseExpand(System.getProperty(PROP_EXPAND));
        dialogs.put("newValidation", () -> new NewValidationDialog(dialogStage, main, psp, expand, parseSource()));
        dialogs.put("externalUtils", () -> new ExternalUtilsCheckDialog(dialogStage, main, false, "Pokračovat", true));
        dialogs.put("about", () -> new AboutAppDialog(dialogStage, main));
        ConfigurationManager config = main.getConfigurationManager();
        dialogs.put("dictionaryContent", () -> new DictionaryContentDialog(dialogStage, main, SNAPSHOT_DICTIONARY,
                config.getStringOrDefault(ConfigurationManager.propDictionaryDescription(SNAPSHOT_DICTIONARY), null),
                config.getStringOrDefault(ConfigurationManager.propDictionarySpecUrl(SNAPSHOT_DICTIONARY), null)));
        dialogs.put("dictionaryUpdate", () -> new DictionaryUpdateDialog(dialogStage, main, SNAPSHOT_DICTIONARY,
                config.getStringOrDefault(ConfigurationManager.propDictionarySyncUrl(SNAPSHOT_DICTIONARY), null)));
        //nahledy: dialogy jen ukazou stav, nic nenacitaji ani nerozbaluji
        dialogs.put("init", () -> new ValidationDataInitializationDialog(dialogStage, main, true, null));
        dialogs.put("initError", () -> new ValidationDataInitializationDialog(dialogStage, main, true,
                "Není definován adresář s konfigurací Validátoru!"));
        File zip = new File(psp != null && psp.toLowerCase().endsWith(".zip") ? psp : "/data/baliky/nk-00027e.zip");
        File workDir = new File(System.getProperty("java.io.tmpdir"), "kv-rozbaleno-123456789");
        dialogs.put("unzip", () -> new UnzipAndValidateDialog(dialogStage, main, zip, null, false, false, 0)
                .preview(String.format("Rozbaluji %s do adresáře %s", zip.getAbsolutePath(),
                        new File(workDir, zip.getName().replaceAll("\\.zip$", "")).getAbsolutePath()), false));
        dialogs.put("unzipError", () -> new UnzipAndValidateDialog(dialogStage, main, zip, null, false, false, 0)
                .preview(String.format("Chyba: Soubor %s není korektní ZIP: zip END header not found!", zip.getAbsolutePath()), true));
        dialogs.put("dev", () -> new nkp.pspValidator.gui.dev.DevDialog(dialogStage, main));

        //snimkovaci rezim preskakuje dialog nastroju pri startu; bez kontroly by prazdny stav neznal jejich dostupnost
        main.getMainController().checkToolsForEmptyState();
        System.out.println("SNAPSHOT MODE: dir=" + dir.getAbsolutePath() + ", dialogs=" + names + ", delay=" + delayMs + " ms");
        new Runner(main, dir, delayMs, names, dialogs).next();
    }

    /** Zdroj předvyplněné cesty v dialogu Nová validace: {@code group} = skupina balíků, jinak podle cesty. */
    private static ValidationSource parseSource() {
        return isGroupSource() ? ValidationSource.GROUP : null;
    }

    private static boolean isGroupSource() {
        return "group".equals(System.getProperty(PROP_SOURCE, "").trim());
    }

    private static NewValidationDialog.Expand parseExpand(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        switch (value.trim()) {
            case "versions":
                return NewValidationDialog.Expand.VERSIONS;
            case "tools":
                return NewValidationDialog.Expand.TOOLS;
            case "skipped":
                return NewValidationDialog.Expand.SKIPPED;
            default:
                System.err.println("SNAPSHOT MODE: unknown " + PROP_EXPAND + " '" + value + "', known: versions,tools,skipped");
                return null;
        }
    }

    private static class Runner {
        private final Main main;
        private final File dir;
        private final long delayMs;
        private final List<String> queue;
        private final Map<String, Supplier<AbstractDialog>> dialogs;
        private boolean failed = false;

        Runner(Main main, File dir, long delayMs, List<String> queue, Map<String, Supplier<AbstractDialog>> dialogs) {
            this.main = main;
            this.dir = dir;
            this.delayMs = delayMs;
            this.queue = queue;
            this.dialogs = dialogs;
        }

        void next() {
            if (queue.isEmpty()) {
                System.out.println("SNAPSHOT MODE: " + (failed ? "finished with errors" : "finished"));
                Platform.exit();
                return;
            }
            String name = queue.remove(0);
            if (!"rerun".equals(name) && !"results".equals(name) && !"progress".equals(name) && !"stress".equals(name)
                    && !name.startsWith("group")) {
                //ostatni kroky ocekavaji klid (zadna bezici validace)
                MainController controller = main.getMainController();
                if (controller.isValidationRunning()) {
                    queue.add(0, name);
                    after(this::next);
                    return;
                }
            }
            try {
                if ("main".equals(name)) {
                    Stage stage = main.getPrimaryStage();
                    after(() -> {
                        save(stage.getScene(), name);
                        next();
                    });
                } else if ("mainMenu".equals(name)) {
                    Menu menu = findMenu(main.getPrimaryStage().getScene(), "Validovat");
                    if (menu == null) {
                        System.err.println("SNAPSHOT MODE: menu Validovat not found");
                        failed = true;
                        next();
                        return;
                    }
                    menu.show();
                    after(() -> {
                        for (Window window : new ArrayList<>(Window.getWindows())) {
                            if (window instanceof PopupWindow && window.isShowing() && window.getScene() != null) {
                                save(window.getScene(), name);
                            }
                        }
                        menu.hide();
                        next();
                    });
                } else if ("toolDetails".equals(name)) {
                    //okno Podrobnosti… u nenalezeneho nastroje (vystup kontroly je pevny, aby snimek nezavisel na stroji)
                    Stage stage = ToolsTable.openDetails(main.getPrimaryStage(), ExternalUtil.JHOVE,
                            nkp.pspValidator.gui.newvalidation.ToolStatus.notFound(
                                    "Cannot run program \"jhove\": error=2, No such file or directory"),
                            null, url -> {
                            }, false);
                    after(() -> {
                        save(stage.getScene(), name);
                        stage.close();
                        next();
                    });
                } else if ("settingsMenu".equals(name)) {
                    saveMenu("Nastavení", name, this::next);
                } else if ("setPreferred".equals(name)) {
                    //zmena vychozi verze v Nastaveni mezi behem a opakovanim (jako ulozeni v zalozce Vychozi verze DMF)
                    String preferred = System.getProperty(PROP_RERUN_PREFERRED, "");
                    String[] parts = preferred.trim().split(":", 2);
                    if (parts.length != 2) {
                        System.err.println("SNAPSHOT MODE: step '" + name + "' needs -D" + PROP_RERUN_PREFERRED + "=<TYPE>:<version|default>");
                        failed = true;
                    } else {
                        Dmf.Type type = Dmf.Type.valueOf(parts[0]);
                        String version = "default".equals(parts[1]) ? null : parts[1];
                        new VersionSettings(main.getConfigurationManager(), main.getValidationDataManager().getFdmfRegistry())
                                .setPreferredVersion(type, version);
                        System.out.println("SNAPSHOT MODE: preferred version of " + type + " set to " + version);
                    }
                    next();
                } else if ("helpMenu".equals(name)) {
                    saveMenu("Nápověda", name, this::next);
                } else if ("showMenu".equals(name)) {
                    saveMenu("Zobrazit", name, this::next);
                } else if ("validateMenu".equals(name)) {
                    //menu Validovat po validaci (Validovat znovu dostupne) a kontrola bubliny polozky
                    saveMenu("Validovat", name, () -> System.out.println("SNAPSHOT MODE: rerun menu item tooltip: "
                            + main.getMainController().rerunMenuTooltip()), this::next);
                } else if ("revealXml".equals(name)) {
                    //„Zobrazit ve slozce“ u protokolu XML naposledy zobrazene validace: skutecne otevre spravce souboru
                    File xml = main.getMainController().shownXmlProtocol();
                    if (xml == null) {
                        System.err.println("SNAPSHOT MODE: step '" + name + "' needs a finished validation with XML protocol");
                        failed = true;
                        next();
                    } else {
                        nkp.pspValidator.gui.desktop.FileManager.revealFile(xml, (command, error) -> Platform.runLater(() -> {
                            System.out.println("SNAPSHOT MODE: reveal " + command + " -> " + (error == null ? "ok" : error));
                            next();
                        }));
                    }
                } else if ("settings".equals(name)) {
                    settings();
                } else if ("settingsSave".equals(name)) {
                    settingsSave();
                } else if ("dictionaryUpdateRun".equals(name)) {
                    dictionaryUpdateRun(name);
                } else if ("results".equals(name)) {
                    runValidation(name);
                } else if ("rerun".equals(name)) {
                    rerun(name);
                } else if ("progress".equals(name)) {
                    progress();
                } else if ("stress".equals(name)) {
                    stress(name);
                } else if ("group".equals(name)) {
                    group();
                } else if ("groupRerunOne".equals(name)) {
                    groupRerunOne();
                } else if ("groupCancel".equals(name)) {
                    groupCancel();
                } else if ("filtered".equals(name)) {
                    MainController controller = main.getMainController();
                    controller.setFilterChecks(true, true, false, true);
                    controller.selectFirstProblem();
                    after(() -> {
                        System.out.println("SNAPSHOT MODE: filtered: " + controller.describeShownResult());
                        save(main.getPrimaryStage().getScene(), name);
                        next();
                    });
                } else if ("small".equals(name)) {
                    Stage stage = main.getPrimaryStage();
                    stage.setWidth(1000);
                    stage.setHeight(640);
                    after(() -> {
                        System.out.println("SNAPSHOT MODE: window " + (int) stage.getWidth() + "x" + (int) stage.getHeight()
                                + ", scene " + (int) stage.getScene().getWidth() + "x" + (int) stage.getScene().getHeight());
                        save(stage.getScene(), name);
                        next();
                    });
                } else if (dialogs.containsKey(name)) {
                    AbstractDialog dialog = dialogs.get(name).get();
                    Stage stage = dialog.showNonBlocking();
                    after(() -> {
                        save(stage.getScene(), name);
                        stage.close();
                        next();
                    });
                } else {
                    System.err.println("SNAPSHOT MODE: unknown dialog '" + name + "', known: main,mainMenu,settingsMenu,setPreferred,helpMenu,showMenu,validateMenu,revealXml,settings,settingsSave,toolDetails,dictionaryUpdateRun,results,rerun,progress,stress,filtered,small,group,groupRerunOne,groupCancel," + String.join(",", dialogs.keySet()));
                    failed = true;
                    next();
                }
            } catch (Exception e) {
                System.err.println("SNAPSHOT MODE: failed to open '" + name + "': " + e);
                e.printStackTrace();
                failed = true;
                next();
            }
        }

        /** Požadavek na validaci balíku z {@code kv.snapshotValidate}, null (a chyba), když chybí. */
        private ValidationRequest buildRequest(String step) {
            String path = System.getProperty(PROP_VALIDATE);
            if (path == null || path.trim().isEmpty()) {
                System.err.println("SNAPSHOT MODE: step '" + step + "' needs -D" + PROP_VALIDATE + "=<package dir or zip>");
                failed = true;
                return null;
            }
            ValidationSource source = isGroupSource() ? ValidationSource.GROUP
                    : path.trim().toLowerCase().endsWith(".zip") ? ValidationSource.ZIP : ValidationSource.DIRECTORY;
            ConfigurationManager config = main.getConfigurationManager();
            FdmfRegistry registry = main.getValidationDataManager().getFdmfRegistry();
            return ValidationRequest.create(source, path, new VersionSettings(config, registry),
                    new OutputSettings(config), parseDisabled(System.getProperty(PROP_DISABLE)));
        }

        private void runValidation(String name) {
            ValidationRequest request = buildRequest(name);
            if (request == null) {
                next();
                return;
            }
            System.out.println("SNAPSHOT MODE: validating " + describe(request));
            MainController controller = main.getMainController();
            int startedBefore = controller.getStartedValidations();
            long started = System.currentTimeMillis();
            //mimo animaci (u ZIPu se otevre modalni dialog rozbalovani)
            Platform.runLater(() -> main.launchValidation(request));
            String runningMs = System.getProperty(PROP_RUNNING_MS);
            if (runningMs != null) {
                PauseTransition pause = new PauseTransition(Duration.millis(Long.parseLong(runningMs)));
                pause.setOnFinished(e -> {
                    System.out.println("SNAPSHOT MODE: while running: " + controller.describeShownResult());
                    save(main.getPrimaryStage().getScene(), "running");
                });
                pause.play();
            }
            FrameMonitor monitor = new FrameMonitor();
            monitor.start();
            waitForEnd(controller, startedBefore, started, () -> {
                monitor.stop();
                System.out.println("SNAPSHOT MODE: UI during validation: " + monitor);
                afterValidation(name, controller, started);
            });
        }

        // ===== prubeh validace =====

        private void progress() {
            ValidationRequest request = buildRequest("progress");
            if (request == null) {
                next();
                return;
            }
            String end = System.getProperty(PROP_PROGRESS_END, "finish").trim();
            System.out.println("SNAPSHOT MODE: progress (end=" + end + ") of " + describe(request));
            MainController controller = main.getMainController();
            Gate gate = new Gate(System.getProperty(PROP_PAUSE_AT, "0.5"));
            controller.setRunHook(gate);
            int startedBefore = controller.getStartedValidations();
            long started = System.currentTimeMillis();
            FrameMonitor monitor = new FrameMonitor();
            monitor.start();
            Platform.runLater(() -> main.launchValidation(request));
            Runnable finish = () -> {
                monitor.stop();
                controller.setRunHook(null);
                System.out.println("SNAPSHOT MODE: validation ended after " + (System.currentTimeMillis() - started) + " ms");
                System.out.println("SNAPSHOT MODE: UI during validation (pauses included): " + monitor);
                after(() -> {
                    System.out.println("SNAPSHOT MODE: shown: " + controller.describeShownResult());
                    save(main.getPrimaryStage().getScene(), "cancel".equals(end) ? "canceled" : "fail".equals(end) ? "failed" : "finished");
                    next();
                });
            };
            //1. pauza po ohlaseni pravidel: inicializace
            whenPaused(gate, started, () -> after(() -> {
                System.out.println("SNAPSHOT MODE: initializing: " + controller.describeShownResult());
                save(main.getPrimaryStage().getScene(), "initializing");
                gate.resume();
                //2. pauza v polovine
                whenPaused(gate, started, () -> after(() -> {
                    System.out.println("SNAPSHOT MODE: paused after " + gate.finished.get() + " of " + gate.total + " rules");
                    System.out.println("SNAPSHOT MODE: running: " + controller.describeShownResult());
                    save(main.getPrimaryStage().getScene(), "running");
                    saveMenu("Validovat", "running-menu", () -> {
                        boolean selected = controller.selectFirstProblem(true);
                        after(() -> {
                            String before = controller.describeShownResult();
                            Object problemsBefore = controller.getShownProblems();
                            System.out.println("SNAPSHOT MODE: browse (rule with problem selected=" + selected + "): " + before);
                            save(main.getPrimaryStage().getScene(), "running-browse");
                            //3. dalsich asi 10 % pravidel
                            gate.pauseAt(gate.finished.get() + Math.max(5, gate.total / 10));
                            gate.resume();
                            whenPaused(gate, started, () -> after(() -> {
                                String later = controller.describeShownResult();
                                System.out.println("SNAPSHOT MODE: browse later, paused after " + gate.finished.get() + " rules: " + later);
                                System.out.println("SNAPSHOT MODE: selection kept: " + selectionPart(before).equals(selectionPart(later))
                                        + ", problem list object kept: " + (problemsBefore == controller.getShownProblems()));
                                save(main.getPrimaryStage().getScene(), "running-browse-later");
                                //4. konec
                                if ("cancel".equals(end)) {
                                    controller.stopValidation(null);
                                } else if ("fail".equals(end)) {
                                    gate.failOnResume = true;
                                }
                                gate.pauseAt(-1);
                                gate.resume();
                                waitForEnd(controller, startedBefore, started, finish);
                            }));
                        });
                    });
                }));
            }));
        }

        /** Výběr sekce a pravidla z popisu okna (pro porovnání před a po). */
        private static String selectionPart(String description) {
            int from = description.indexOf("selectedSection=");
            return from < 0 ? description : description.substring(from);
        }

        /** Počká, až se vlákno validace zastaví v háčku (nebo validace skončí). */
        private void whenPaused(Gate gate, long started, Runnable then) {
            PauseTransition pause = new PauseTransition(Duration.millis(100));
            pause.setOnFinished(e -> {
                if (gate.paused) {
                    then.run();
                } else if (!main.getMainController().isValidationRunning() && main.getMainController().getStartedValidations() > 0
                        && System.currentTimeMillis() - started > 2000) {
                    System.err.println("SNAPSHOT MODE: validation ended before the pause point");
                    failed = true;
                    main.getMainController().setRunHook(null);
                    next();
                } else if (System.currentTimeMillis() - started > VALIDATION_TIMEOUT_MS) {
                    System.err.println("SNAPSHOT MODE: pause timeout");
                    failed = true;
                    next();
                } else {
                    whenPaused(gate, started, then);
                }
            });
            pause.play();
        }

        // ===== nastaveni =====

        /** Okno Nastavení, snímek každé záložky z {@code kv.snapshotTab} (výchozí všechny). */
        private void settings() {
            List<SettingsTab> tabs = new ArrayList<>();
            String value = System.getProperty(PROP_TAB, "");
            if (value.trim().isEmpty()) {
                tabs.addAll(Arrays.asList(SettingsTab.values()));
            } else {
                for (String key : value.split("\\s*,\\s*")) {
                    SettingsTab tab = SettingsTab.fromKey(key);
                    if (tab == null) {
                        System.err.println("SNAPSHOT MODE: unknown " + PROP_TAB + " '" + key + "', known: versions,tools,skipping,dictionaries,dirs");
                        failed = true;
                    } else {
                        tabs.add(tab);
                    }
                }
            }
            if (tabs.isEmpty()) {
                next();
                return;
            }
            SettingsDialog dialog = new SettingsDialog(main.getDialogStage(), main, tabs.get(0), null);
            Stage stage = dialog.showNonBlocking();
            SettingsDialogController controller = (SettingsDialogController) dialog.getController();
            settingsTabs(stage, controller, tabs, 0);
        }

        private void settingsTabs(Stage stage, SettingsDialogController controller, List<SettingsTab> tabs, int index) {
            if (index >= tabs.size()) {
                stage.close();
                next();
                return;
            }
            SettingsTab tab = tabs.get(index);
            controller.showTab(tab);
            after(() -> {
                save(stage.getScene(), "settings-" + tab.getKey());
                settingsTabs(stage, controller, tabs, index + 1);
            });
        }

        /** Změna výchozí verze v Nastavení jako uživatel, uložení a výpis parametrů, které by se předaly validaci. */
        private void settingsSave() {
            String value = System.getProperty(PROP_DEFAULT_VERSION, "MONOGRAPH:1.4").trim();
            int colon = value.indexOf(':');
            Dmf.Type type;
            String version;
            try {
                type = Dmf.Type.valueOf(value.substring(0, colon).trim());
                version = value.substring(colon + 1).trim();
            } catch (RuntimeException e) {
                System.err.println("SNAPSHOT MODE: bad " + PROP_DEFAULT_VERSION + " '" + value + "', expected <TYPE>:<version|default>");
                failed = true;
                next();
                return;
            }
            String selected = "default".equals(version) ? null : version;
            System.out.println("SNAPSHOT MODE: params before: " + describeParams());
            SettingsDialog dialog = new SettingsDialog(main.getDialogStage(), main, SettingsTab.VERSIONS, null);
            Stage stage = dialog.showNonBlocking();
            SettingsDialogController controller = (SettingsDialogController) dialog.getController();
            if (!controller.selectDefaultVersionForSnapshot(type, selected)) {
                System.err.println("SNAPSHOT MODE: version '" + version + "' not offered for " + type);
                failed = true;
            }
            after(() -> {
                save(stage.getScene(), "settings-changed");
                controller.saveForSnapshot();
                System.out.println("SNAPSHOT MODE: params after save: " + describeParams());
                SettingsDialog reopened = new SettingsDialog(main.getDialogStage(), main, SettingsTab.VERSIONS, null);
                Stage reopenedStage = reopened.showNonBlocking();
                after(() -> {
                    save(reopenedStage.getScene(), "settings-reopened");
                    reopenedStage.close();
                    next();
                });
            });
        }

        /** Vynucené a preferované verze, které by dialog Nová validace předal validaci (jen nenulové). */
        private String describeParams() {
            ConfigurationManager config = main.getConfigurationManager();
            FdmfRegistry registry = main.getValidationDataManager().getFdmfRegistry();
            DmfDetector.Params params = DmfParamsBuilder.build(new VersionSettings(config, registry));
            StringBuilder text = new StringBuilder();
            try {
                for (java.lang.reflect.Field field : DmfDetector.Params.class.getFields()) {
                    Object fieldValue = field.get(params);
                    if (fieldValue != null && !java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                        text.append(text.length() == 0 ? "" : ", ").append(field.getName()).append('=').append(fieldValue);
                    }
                }
            } catch (IllegalAccessException e) {
                return e.toString();
            }
            return text.length() == 0 ? "(nic nevynuceno ani preferováno)" : text.toString();
        }

        /** Rozbalí menu hlavního okna, uloží snímek nabídky a menu zavře. */
        private void saveMenu(String text, String name, Runnable then) {
            saveMenu(text, name, null, then);
        }

        /** @param whileShown co udělat ještě při zobrazeném menu (po snímku), nebo null */
        private void saveMenu(String text, String name, Runnable whileShown, Runnable then) {
            Menu menu = findMenu(main.getPrimaryStage().getScene(), text);
            if (menu == null) {
                System.err.println("SNAPSHOT MODE: menu " + text + " not found");
                failed = true;
                then.run();
                return;
            }
            menu.show();
            after(() -> {
                for (Window window : new ArrayList<>(Window.getWindows())) {
                    if (window instanceof PopupWindow && window.isShowing() && window.getScene() != null) {
                        save(window.getScene(), name);
                    }
                }
                if (whileShown != null) {
                    whileShown.run();
                }
                menu.hide();
                resetMenuBar(main.getPrimaryStage().getScene());
                //dalsi menu se po obnoveni listy otevre az po vykresleni
                after(then);
            });
        }

        // ===== skupina baliku =====

        private void group() {
            ValidationRequest request = buildRequest("group");
            if (request == null) {
                next();
                return;
            }
            System.out.println("SNAPSHOT MODE: group " + describe(request));
            MainController controller = main.getMainController();
            GroupGate gate = new GroupGate();
            controller.setRunHook(gate);
            int startedBefore = controller.getStartedValidations();
            long started = System.currentTimeMillis();
            FrameMonitor monitor = new FrameMonitor();
            monitor.start();
            Platform.runLater(() -> main.launchValidation(request));
            whenGroupPaused(gate, started, () -> after(() -> {
                System.out.println("SNAPSHOT MODE: group paused in package " + gate.position + " of " + gate.total
                        + " after " + gate.finished.get() + " of " + gate.pauseAt * 2 + " rules");
                System.out.println("SNAPSHOT MODE: group running: " + controller.describeShownResult());
                save(main.getPrimaryStage().getScene(), "group-running");
                String browsed = controller.selectGroupPackage(nkp.pspValidator.gui.group.PackageStatus.INVALID);
                if (browsed == null) {
                    browsed = controller.selectGroupPackage(nkp.pspValidator.gui.group.PackageStatus.VALID);
                }
                if (browsed != null) {
                    controller.selectFirstProblem();
                }
                String selected = browsed;
                after(() -> {
                    System.out.println("SNAPSHOT MODE: group browse (" + selected + "): " + controller.describeShownResult());
                    save(main.getPrimaryStage().getScene(), "group-browse");
                    gate.resume();
                    waitForEnd(controller, startedBefore, started, () -> {
                        monitor.stop();
                        controller.setRunHook(null);
                        System.out.println("SNAPSHOT MODE: group ended after " + (System.currentTimeMillis() - started) + " ms; UI: " + monitor);
                        String invalid = controller.selectGroupPackage(nkp.pspValidator.gui.group.PackageStatus.INVALID);
                        controller.selectFirstProblem();
                        after(() -> {
                            System.out.println("SNAPSHOT MODE: group results (" + invalid + "): " + controller.describeShownResult()
                                    + controller.describeGroupItems());
                            save(main.getPrimaryStage().getScene(), "group-results");
                            controller.setGroupOnlyInvalid(true);
                            after(() -> {
                                System.out.println("SNAPSHOT MODE: group filtered: " + controller.describeShownResult());
                                save(main.getPrimaryStage().getScene(), "group-filtered");
                                controller.setGroupOnlyInvalid(false);
                                File csv = new File(System.getProperty(PROP_CSV, new File(dir, "group-souhrn.csv").getPath()));
                                boolean ok = controller.exportGroupCsvTo(csv);
                                System.out.println("SNAPSHOT MODE: group CSV " + (ok ? "written" : "FAILED") + ": " + csv.getAbsolutePath());
                                if (!ok) {
                                    failed = true;
                                }
                                next();
                            });
                        });
                    });
                });
            }));
        }

        private void groupRerunOne() {
            MainController controller = main.getMainController();
            if (!controller.isGroupMode() || controller.isValidationRunning()) {
                System.err.println("SNAPSHOT MODE: groupRerunOne needs a finished group");
                failed = true;
                next();
                return;
            }
            String name = controller.rerunGroupPackage(nkp.pspValidator.gui.group.PackageStatus.INVALID,
                    System.getProperty(PROP_RERUN_PACKAGE));
            System.out.println("SNAPSHOT MODE: group rerun one: " + name);
            int startedBefore = controller.getStartedValidations() - 1;
            long started = System.currentTimeMillis();
            waitForEnd(controller, startedBefore, started, () -> {
                controller.selectFirstProblem();
                after(() -> {
                    System.out.println("SNAPSHOT MODE: group rerun one done: " + controller.describeShownResult() + controller.describeGroupItems());
                    save(main.getPrimaryStage().getScene(), "group-rerun-one");
                    next();
                });
            });
        }

        private void groupCancel() {
            ValidationRequest request = buildRequest("groupCancel");
            if (request == null) {
                next();
                return;
            }
            MainController controller = main.getMainController();
            GroupGate gate = new GroupGate();
            controller.setRunHook(gate);
            int startedBefore = controller.getStartedValidations();
            long started = System.currentTimeMillis();
            Platform.runLater(() -> main.launchValidation(request));
            whenGroupPaused(gate, started, () -> after(() -> {
                System.out.println("SNAPSHOT MODE: group cancel in package " + gate.position + " of " + gate.total);
                controller.stopValidation(null);
                gate.resume();
                waitForEnd(controller, startedBefore, started, () -> {
                    controller.setRunHook(null);
                    after(() -> {
                        System.out.println("SNAPSHOT MODE: group canceled: " + controller.describeShownResult() + controller.describeGroupItems());
                        save(main.getPrimaryStage().getScene(), "group-canceled");
                        next();
                    });
                });
            }));
        }

        private void whenGroupPaused(GroupGate gate, long started, Runnable then) {
            PauseTransition pause = new PauseTransition(Duration.millis(100));
            pause.setOnFinished(e -> {
                if (gate.paused) {
                    then.run();
                } else if (System.currentTimeMillis() - started > 2000 && !main.getMainController().isValidationRunning()) {
                    System.err.println("SNAPSHOT MODE: group ended before the pause point");
                    failed = true;
                    main.getMainController().setRunHook(null);
                    next();
                } else if (System.currentTimeMillis() - started > VALIDATION_TIMEOUT_MS) {
                    System.err.println("SNAPSHOT MODE: group pause timeout");
                    failed = true;
                    next();
                } else {
                    whenGroupPaused(gate, started, then);
                }
            });
            pause.play();
        }

        // ===== syntheticka zatez =====

        private void stress(String name) {
            MainController controller = main.getMainController();
            int sectionCount = 20;
            int rulesPerSection = 15;
            int bigRuleProblems = 5000;
            int otherRuleProblems = 20;
            List<RulesSection> sections = new ArrayList<>();
            Map<RulesSection, List<Rule>> rules = new HashMap<>();
            for (int s = 0; s < sectionCount; s++) {
                RulesSection section = new RulesSection(s, "Sekce " + (s + 1), "Syntetická sekce pro měření odezvy UI.");
                sections.add(section);
                List<Rule> sectionRules = new ArrayList<>();
                for (int r = 0; r < rulesPerSection; r++) {
                    sectionRules.add(new Rule(s, r, "PRAVIDLO_" + (s + 1) + "_" + (r + 1), "Syntetické pravidlo.", null));
                }
                rules.put(section, sectionRules);
            }
            CountDownLatch selected = new CountDownLatch(1);
            long[] floodStartedAt = new long[1];
            Thread worker = new Thread(() -> {
                controller.onInitialization(sections, rules);
                controller.onValidationsStart();
                Map<Level, Integer> global = new EnumMap<>(Level.class);
                for (Level level : Level.values()) {
                    global.put(level, 0);
                }
                int total = 0;
                for (int s = 0; s < sectionCount; s++) {
                    controller.onSectionStart(s);
                    Map<Level, Integer> sectionCounts = new EnumMap<>(Level.class);
                    int sectionTotal = 0;
                    for (int r = 0; r < rulesPerSection; r++) {
                        controller.onRuleStart(s, r);
                        int count = s == 0 && r == 0 ? bigRuleProblems : otherRuleProblems;
                        List<ValidationProblem> problems = new ArrayList<>(count);
                        Map<Level, Integer> ruleCounts = new EnumMap<>(Level.class);
                        for (int i = 0; i < count; i++) {
                            Level level = Level.values()[i % 3];
                            problems.add(new ValidationProblem(level, "Syntetický problém č. " + (i + 1) + " pravidla "
                                    + (s + 1) + "/" + (r + 1) + ": hodnota neodpovídá profilu, soubor usercopy/uc_" + i + ".jp2"));
                            ruleCounts.merge(level, 1, Integer::sum);
                            sectionCounts.merge(level, 1, Integer::sum);
                            global.merge(level, 1, Integer::sum);
                        }
                        sectionTotal += count;
                        total += count;
                        controller.onRuleFinish(s, new EnumMap<>(sectionCounts), sectionTotal, r, ruleCounts, count, problems);
                        if (s == 0 && r == 0) {
                            try {
                                selected.await();
                            } catch (InterruptedException e) {
                                return;
                            }
                            floodStartedAt[0] = System.currentTimeMillis();
                        }
                    }
                    controller.onSectionFinish(s, 0);
                }
                controller.onValidationsFinish(total, global, false, 1234);
                controller.completeSyntheticRun();
            });
            worker.setDaemon(true);
            int startedBefore = controller.getStartedValidations();
            long started = System.currentTimeMillis();
            controller.beginSyntheticRun(new File(dir, "synteticky-balik"));
            worker.start();
            //po prvnim pravidle vybrat pravidlo s 5 000 problemy, pak spustit zaplavu udalosti
            after(() -> {
                //prvni sekce jeste bezi: vyber v bezici sekci je narocnejsi pripad (radek sekce se meni s kazdym pravidlem)
                controller.selectFirstProblem(false);
                Object problemsBefore = controller.getShownProblems();
                int shown = problemsBefore == null ? 0 : controller.getShownProblems().size();
                System.out.println("SNAPSHOT MODE: stress: selected rule shows " + shown + " problems: " + controller.describeShownResult());
                FrameMonitor monitor = new FrameMonitor();
                monitor.start();
                selected.countDown();
                waitForEnd(controller, startedBefore, started, () -> {
                    monitor.stop();
                    System.out.println("SNAPSHOT MODE: stress: " + (sectionCount * rulesPerSection - 1) + " rules flooded, all events processed "
                            + (System.currentTimeMillis() - floodStartedAt[0]) + " ms after the flood started; UI: " + monitor);
                    System.out.println("SNAPSHOT MODE: stress: problem list object kept: " + (problemsBefore == controller.getShownProblems())
                            + ", shown: " + controller.describeShownResult());
                    after(() -> {
                        save(main.getPrimaryStage().getScene(), name);
                        next();
                    });
                });
            });
        }

        private void rerun(String name) {
            MainController controller = main.getMainController();
            ValidationRequest before = controller.getLastRequest();
            boolean available = controller.canRerun();
            System.out.println("SNAPSHOT MODE: rerun available=" + available + ", last request: " + (before == null ? "-" : describe(before)));
            if (!available) {
                failed = true;
                next();
                return;
            }
            int startedBefore = controller.getStartedValidations();
            long started = System.currentTimeMillis();
            Platform.runLater(() -> controller.rerunValidation(null));
            waitForEnd(controller, startedBefore, started, () -> {
                ValidationRequest after = controller.getLastRequest();
                System.out.println("SNAPSHOT MODE: rerun request: " + (after == null ? "-" : describe(after)));
                afterValidation(name, controller, started);
            });
        }

        /** Čeká, až validace začne a skončí (dokončená, chyba nebo zrušení). */
        private void waitForEnd(MainController controller, int startedBefore, long started, Runnable then) {
            PauseTransition pause = new PauseTransition(Duration.millis(250));
            pause.setOnFinished(e -> {
                if (controller.getStartedValidations() > startedBefore && !controller.isValidationRunning()) {
                    then.run();
                } else if (System.currentTimeMillis() - started > VALIDATION_TIMEOUT_MS) {
                    System.err.println("SNAPSHOT MODE: validation timeout");
                    failed = true;
                    next();
                } else {
                    waitForEnd(controller, startedBefore, started, then);
                }
            });
            pause.play();
        }

        private void afterValidation(String name, MainController controller, long started) {
            System.out.println("SNAPSHOT MODE: validation ended after " + (System.currentTimeMillis() - started) + " ms");
            controller.selectFirstProblem();
            after(() -> {
                System.out.println("SNAPSHOT MODE: shown: " + controller.describeShownResult());
                save(main.getPrimaryStage().getScene(), name);
                next();
            });
        }

        private static Set<ExternalUtil> parseDisabled(String value) {
            Set<ExternalUtil> result = EnumSet.noneOf(ExternalUtil.class);
            if (value != null) {
                for (String item : value.split("\\s*,\\s*")) {
                    if (!item.trim().isEmpty()) {
                        result.add(ExternalUtil.valueOf(item.trim()));
                    }
                }
            }
            return result;
        }

        private static String describe(ValidationRequest request) {
            return request.getSource() + " " + request.getFile().getAbsolutePath()
                    + ", txtLog=" + request.isCreateTxtLog() + ", xmlLog=" + request.isCreateXmlLog()
                    + ", verbosity=" + request.getVerbosity() + ", disabledTools=" + request.getDisabledUtils()
                    + ", forcedFundUnit=" + request.getParams().forcedDmfFduVersion
                    + ", preferredFundUnit=" + request.getParams().preferredDmfFduVersion
                    + ", forcedMonograph=" + request.getParams().forcedDmfMonVersion
                    + ", preferredMonograph=" + request.getParams().preferredDmfMonVersion;
        }

        /**
         * Skutečná aktualizace slovníku z dialogu (tlačítko Aktualizovat) s adresou z nastavení; snímek výsledku
         * (úspěch s datem, nebo chyba). Soubor slovníku se přepíše, proto krok odmítne konfiguraci ve zdrojích repozitáře.
         */
        private void dictionaryUpdateRun(String name) {
            ConfigurationManager config = main.getConfigurationManager();
            String url = config.getStringOrDefault(ConfigurationManager.propDictionarySyncUrl(SNAPSHOT_DICTIONARY), null);
            File dictFile = main.getValidationDataManager().getValidatorConfigMgr().getDictionaryManager()
                    .getDictionaryFile(SNAPSHOT_DICTIONARY);
            System.out.println("SNAPSHOT MODE: dictionary update from " + url + " into " + dictFile.getAbsolutePath()
                    + ", syncDate before=" + config.getStringOrDefault(ConfigurationManager.propDictionarySyncDate(SNAPSHOT_DICTIONARY), null));
            if (dictFile.getAbsolutePath().replace('\\', '/').contains("/src/main/resources/")) {
                System.err.println("SNAPSHOT MODE: step '" + name + "' refuses to overwrite dictionary in sources, use a copy of validatorConfig");
                failed = true;
                next();
                return;
            }
            DictionaryUpdateDialog dialog = new DictionaryUpdateDialog(main.getDialogStage(), main, SNAPSHOT_DICTIONARY, url);
            Stage stage = dialog.showNonBlocking();
            DictionaryUpdateDialogController controller = (DictionaryUpdateDialogController) dialog.getController();
            long started = System.currentTimeMillis();
            controller.update(null);
            waitForDictionary(controller, started, () -> after(() -> {
                System.out.println("SNAPSHOT MODE: dictionary update: " + controller.describe()
                        + ", syncDate after=" + config.getStringOrDefault(ConfigurationManager.propDictionarySyncDate(SNAPSHOT_DICTIONARY), null));
                save(stage.getScene(), name);
                stage.close();
                next();
            }));
        }

        private void waitForDictionary(DictionaryUpdateDialogController controller, long started, Runnable then) {
            PauseTransition pause = new PauseTransition(Duration.millis(250));
            pause.setOnFinished(e -> {
                if (!controller.isUpdatingNow()) {
                    then.run();
                } else if (System.currentTimeMillis() - started > 60_000) {
                    System.err.println("SNAPSHOT MODE: dictionary update timeout");
                    failed = true;
                    next();
                } else {
                    waitForDictionary(controller, started, then);
                }
            });
            pause.play();
        }

        /**
         * Po programovém otevření a zavření zůstane položka lišty zvýrazněná (skin lišty si ji pamatuje jako vybranou),
         * což by bylo vidět na dalších snímcích; nové vložení položek lišty skin obnoví.
         */
        private static void resetMenuBar(Scene scene) {
            for (Node node : scene.getRoot().lookupAll(".menu-bar")) {
                if (node instanceof MenuBar) {
                    MenuBar bar = (MenuBar) node;
                    List<Menu> menus = new ArrayList<>(bar.getMenus());
                    bar.getMenus().clear();
                    bar.getMenus().setAll(menus);
                }
            }
        }

        private static Menu findMenu(Scene scene, String text) {
            for (Node node : scene.getRoot().lookupAll(".menu-bar")) {
                if (node instanceof MenuBar) {
                    for (Menu menu : ((MenuBar) node).getMenus()) {
                        if (text.equals(menu.getText())) {
                            return menu;
                        }
                    }
                }
            }
            return null;
        }

        private void after(Runnable action) {
            PauseTransition pause = new PauseTransition(Duration.millis(delayMs));
            pause.setOnFinished(e -> action.run());
            pause.play();
        }

        private void save(Scene scene, String name) {
            File file = new File(dir, name + ".png");
            try {
                WritableImage image = scene.snapshot(null);
                writePng(image, file);
                System.out.println("SNAPSHOT MODE: " + name + " -> " + file.getAbsolutePath()
                        + " (" + (int) image.getWidth() + "x" + (int) image.getHeight() + ")");
            } catch (Exception e) {
                System.err.println("SNAPSHOT MODE: failed to save '" + name + "': " + e);
                failed = true;
            }
        }

        /**
         * Bez závislosti na javafx.swing (SwingFXUtils): pixely se přepíší do BufferedImage ručně.
         */
        private static void writePng(WritableImage image, File file) throws IOException {
            int w = (int) image.getWidth();
            int h = (int) image.getHeight();
            BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            PixelReader reader = image.getPixelReader();
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    out.setRGB(x, y, reader.getArgb(x, y));
                }
            }
            ImageIO.write(out, "png", file);
        }
    }

    /**
     * Háček ve vlákně validace: zastaví ho po ohlášení pravidel a po zadaném počtu dokončených pravidel, dokud ho
     * vlákno UI nepustí dál. Přerušení vlákna (zrušení validace) pauzu ukončí.
     */
    private static class Gate implements MainController.RunHook {
        final AtomicInteger finished = new AtomicInteger();
        private final String pauseAtSpec;
        private final Semaphore permits = new Semaphore(0);
        volatile int total;
        private volatile int pauseAt = -1;
        volatile boolean paused;
        volatile boolean failOnResume;

        Gate(String pauseAtSpec) {
            this.pauseAtSpec = pauseAtSpec.trim();
        }

        void pauseAt(int rules) {
            pauseAt = rules;
        }

        void resume() {
            permits.release();
        }

        @Override
        public void initialized(int totalRules) {
            total = totalRules;
            double spec = Double.parseDouble(pauseAtSpec);
            pauseAt = spec <= 1 ? Math.max(1, (int) Math.round(totalRules * spec)) : (int) spec;
            block();
        }

        @Override
        public void ruleFinished() {
            if (finished.incrementAndGet() == pauseAt) {
                block();
                if (failOnResume) {
                    throw new IllegalStateException("Simulovaná chyba při validaci (snímkovací režim)");
                }
            }
        }

        private void block() {
            paused = true;
            try {
                permits.acquire();
            } catch (InterruptedException e) {
                //zruseni validace preruší vlakno; validace pak pokracuje ke zruseni
            } finally {
                paused = false;
            }
        }
    }

    /**
     * Háček pro skupinu: zastaví vlákno validace v polovině pravidel prvního balíku od poloviny skupiny, který dojde
     * k pravidlům (balík, který není balíkem, pravidla nemá), dokud ho vlákno UI nepustí dál.
     */
    private static class GroupGate implements MainController.RunHook {
        final AtomicInteger finished = new AtomicInteger();
        private final Semaphore permits = new Semaphore(0);
        volatile int position;
        volatile int total;
        volatile int pauseAt = -1;
        volatile boolean paused;
        private volatile boolean pausedOnce;

        @Override
        public void packageStarted(int position, int total) {
            this.position = position;
            this.total = total;
            finished.set(0);
            if (!pausedOnce) {
                pauseAt = -1;
            }
        }

        @Override
        public void initialized(int totalRules) {
            if (!pausedOnce && position >= (total + 1) / 2) {
                pauseAt = Math.max(1, totalRules / 2);
            }
        }

        @Override
        public void ruleFinished() {
            if (!pausedOnce && pauseAt > 0 && finished.incrementAndGet() == pauseAt) {
                pausedOnce = true;
                paused = true;
                try {
                    permits.acquire();
                } catch (InterruptedException e) {
                    //pokracuje
                } finally {
                    paused = false;
                }
            }
        }

        void resume() {
            permits.release();
        }
    }

    /** Měří odezvu UI: nejdelší prodlevu mezi pulzy JavaFX a počet prodlev delších než 100 ms. */
    private static class FrameMonitor extends AnimationTimer {
        private long last = -1;
        private long maxGapNs;
        private int frames;
        private int slowFrames;

        @Override
        public void handle(long now) {
            if (last >= 0) {
                long gap = now - last;
                maxGapNs = Math.max(maxGapNs, gap);
                if (gap > 100_000_000L) {
                    slowFrames++;
                }
            }
            last = now;
            frames++;
        }

        @Override
        public String toString() {
            return "frames=" + frames + ", max frame gap=" + (maxGapNs / 1_000_000) + " ms, gaps over 100 ms=" + slowFrames;
        }
    }
}
