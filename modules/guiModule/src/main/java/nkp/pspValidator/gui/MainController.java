package nkp.pspValidator.gui;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import nkp.pspValidator.gui.group.DetailRetention;
import nkp.pspValidator.gui.group.GroupCsv;
import nkp.pspValidator.gui.group.GroupDetailTexts;
import nkp.pspValidator.gui.group.GroupItem;
import nkp.pspValidator.gui.group.GroupPackages;
import nkp.pspValidator.gui.group.GroupRun;
import nkp.pspValidator.gui.group.PackageStatus;
import nkp.pspValidator.gui.newvalidation.DmfParamsBuilder;
import nkp.pspValidator.gui.newvalidation.DocumentTypes;
import nkp.pspValidator.gui.newvalidation.ValidationRequest;
import nkp.pspValidator.gui.newvalidation.ValidationSource;
import nkp.pspValidator.gui.newvalidation.ZipUnpacker;
import nkp.pspValidator.gui.results.ProblemCounts;
import nkp.pspValidator.gui.results.ProblemFilter;
import nkp.pspValidator.gui.results.RerunState;
import nkp.pspValidator.gui.results.ResultHeader;
import nkp.pspValidator.gui.results.ResultTexts;
import nkp.pspValidator.gui.results.ValidationProgress;
import nkp.pspValidator.gui.validation.*;
import nkp.pspValidator.shared.*;
import nkp.pspValidator.shared.engine.Level;
import nkp.pspValidator.shared.engine.Rule;
import nkp.pspValidator.shared.engine.RulesSection;
import nkp.pspValidator.shared.engine.validationFunctions.ValidationProblem;
import nkp.pspValidator.shared.externalUtils.ExternalUtil;
import nkp.pspValidator.shared.externalUtils.ExternalUtilManager;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.*;
import java.util.logging.Logger;

/**
 * Hlavní okno: prázdný stav, horní lišta se souhrnem validace („Validovat znovu“, během běhu „Zrušit validaci“
 * a uplynulý čas), během běhu panel průběhu, po zrušení nebo chybě hláška, filtr a tři panely výsledků (sekce,
 * pravidla vybrané sekce, problémy vybraného pravidla) a stavový řádek. Logika souhrnu, průběhu, filtru a opakování
 * validace je v balíčku {@code gui.results} (bez JavaFX), počty a stavy sekcí a pravidel dodává jádro přes
 * {@link ValidationState.ProgressListener} do {@link ValidationStateManager} a {@link ValidationProgress}.
 * <p>
 * Skupina balíků: tabulka balíků nad stejnými třemi panely, které ukazují podrobné výsledky balíku vybraného
 * v tabulce; balíky se validují postupně v jednom vlákně, každý stejně jako jeden balík (ZIP se rozbalí přes
 * {@link ZipUnpacker} bez dialogu). Stav běhu a texty jsou v {@link GroupRun} (bez JavaFX), podrobné výsledky balíků
 * ({@link ValidationStateManager}) se drží v {@link DetailRetention} s rozpočtem na počet problémů. Panely ukazují
 * {@code shownManager}; události jádra mění {@code validationStateManager} běžícího balíku (u jednoho balíku je to totéž).
 * <p>
 * Události jádra chodí z vlákna validace a do vlákna UI se předávají přes {@code Platform.runLater} v pořadí, v jakém
 * přišly. Hotové sekce lze během běhu procházet: průběh výběr v seznamech nemění a seznam problémů se přestaví jen
 * při změně výběru nebo při dokončení právě vybraného pravidla.
 * <p>
 * Created by Martin Řehánek on 9.12.16.
 */
public class MainController extends AbstractController implements ValidationState.ProgressListener, ValidationState.ProgressController {

    private static Logger LOG = Logger.getLogger(MainController.class.getSimpleName());

    //TODO: nahradit konkretni wiki strankou
    private static final String URL_ONLINE_HELP = "https://github.com/NLCR/komplexni-validator/wiki";

    private static final String[] STATUS_DOT_CLASSES = {"status-dot-ok", "status-dot-error", "status-dot-none"};
    private static final String[] VERDICT_CLASSES = {"verdict-valid", "verdict-invalid", "verdict-none"};

    @FXML
    BorderPane container;

    //menu
    @FXML
    MenuBar menuBar;
    @FXML
    Menu menuValidate;
    @FXML
    MenuItem newValidationMenuItem;
    @FXML
    MenuItem rerunMenuItem;
    @FXML
    MenuItem cancelMenuItem;
    @FXML
    MenuItem exportCsvMenuItem;
    @FXML
    Menu menuSettings;
    @FXML
    Menu menuShow;
    @FXML
    MenuItem showLogTxtMenuItem;
    @FXML
    MenuItem showLogXmlMenuItem;

    //horni lista
    @FXML
    HBox header;
    @FXML
    Label headerTitle;
    @FXML
    Label headerSubtitle;
    @FXML
    HBox headerLinks;
    @FXML
    Hyperlink xmlLogLink;
    @FXML
    Hyperlink txtLogLink;
    @FXML
    Label verdictBadge;
    @FXML
    Label errorsBadge;
    @FXML
    Label warningsBadge;
    @FXML
    Label infosBadge;
    @FXML
    Label groupValidBadge;
    @FXML
    Label groupInvalidBadge;
    @FXML
    Label groupErrorBadge;
    @FXML
    Label groupWaitingBadge;
    @FXML
    Button rerunButton;
    @FXML
    Button cancelButton;

    //prubeh a hlaska po zruseni nebo chybe
    @FXML
    VBox progressPane;
    @FXML
    Label progressTitle;
    @FXML
    Label progressCounts;
    @FXML
    ProgressBar progressBar;
    @FXML
    Label progressDetail;
    @FXML
    HBox noticeBar;
    @FXML
    Label noticeText;

    //prazdny stav a hlaska
    @FXML
    VBox emptyState;
    @FXML
    Label emptyTitle;
    @FXML
    Label emptyFdmfInfo;
    @FXML
    Label emptyToolsInfo;
    @FXML
    VBox messageState;
    @FXML
    Label messageTitle;
    @FXML
    Label messageText;

    //vysledky
    @FXML
    VBox resultsPane;
    @FXML
    SplitPane groupSplit;
    @FXML
    VBox groupPane;
    @FXML
    CheckBox groupOnlyInvalid;
    @FXML
    Label groupShownText;
    @FXML
    Button exportCsvButton;
    @FXML
    TableView<GroupItem> groupTable;
    @FXML
    VBox detailPane;
    @FXML
    HBox packageBar;
    @FXML
    Label packageTitle;
    @FXML
    Label packageSubtitle;
    @FXML
    Hyperlink packageXmlLink;
    @FXML
    Hyperlink packageTxtLink;
    @FXML
    SplitPane resultsSplit;
    @FXML
    VBox packageMessagePane;
    @FXML
    Label packageMessage;
    @FXML
    CheckBox filterErrors;
    @FXML
    CheckBox filterWarnings;
    @FXML
    CheckBox filterInfos;
    @FXML
    CheckBox filterOnlyWithProblems;
    @FXML
    ListView<SectionWithState> sectionList;
    @FXML
    Label rulesHeader;
    @FXML
    Label sectionDescription;
    @FXML
    ListView<RuleWithState> ruleList;
    @FXML
    Label problemsHeader;
    @FXML
    Label ruleDescription;
    @FXML
    ListView<ValidationProblem> problemList;
    @FXML
    ProgressIndicator problemsProgressIndicator;

    //stavovy radek
    @FXML
    Label statusText;
    @FXML
    Region statusDot;

    //data probihajici nebo posledni validace
    private Task validationTask;
    /** výsledky běžícího (u jednoho balíku i zobrazeného) balíku, mění ho události jádra */
    private ValidationStateManager validationStateManager = null;
    /** výsledky zobrazené v panelech sekcí, pravidel a problémů */
    private ValidationStateManager shownManager = null;
    private SectionWithState selectedSection;
    private RuleWithState selectedRule;
    private File pspDir;
    private volatile Dmf dmf;
    /** průběh pro panel průběhu a průběžné počty, null do ohlášení pravidel jádrem */
    private ValidationProgress progress;
    private long runStartedAt;
    /** tiká po sekundách během běhu (uplynulý čas v horní liště) */
    private Timeline elapsedTimer;
    /** jen pro SnapshotMode: zastavení vlákna validace v definovaném bodě */
    private volatile RunHook runHook;
    private ValidationRequest currentRequest;

    private File logTxtFile;
    private File logXmlFile;

    private volatile boolean cancelValidation = false;
    /** vlakno validace uz vstoupilo do tela ulohy (zruseni pred tim se musi osetrit zvlast, telo se pak nespusti) */
    private volatile boolean validationTaskEntered = false;
    /**
     * Zobrazeni vysledku ohlaseneho jadrem (hotovo nebo zruseno). Jadro ho hlasi jeste pred zapisem protokolu, proto se
     * provede az po navratu z validatoru a uzavreni logu: teprve pak smi jit otevrit protokol a validovat znovu.
     */
    private volatile Runnable pendingOutcome = null;

    private final RerunState rerunState = new RerunState();
    private ProblemFilter filter = ProblemFilter.all();
    private FilteredList<SectionWithState> filteredSections;
    private FilteredList<RuleWithState> filteredRules;
    private FilteredList<ValidationProblem> filteredProblems;
    private TotalState totalState = TotalState.IDLE;
    private int finishedValidations = 0;
    private int startedValidations = 0;

    //skupina baliku (null mimo skupinu)
    private GroupTable groupTableModel;
    private GroupRun groupRun;
    private GroupParams groupParams;
    private final DetailRetention<GroupItem, ValidationStateManager> groupDetails = new DetailRetention<>(DetailRetention.DEFAULT_BUDGET);
    /** balík vybraný v tabulce (jeho výsledky jsou v panelech) */
    private GroupItem shownItem;
    /** balík, jehož události jádra právě chodí (jen vlákno UI) */
    private GroupItem runningItem;
    private long packageStartedAt;
    /** dokud uživatel nevybere balík sám, tabulka sleduje běžící balík */
    private boolean followRunning = true;
    /** výběr v tabulce mění program (ne uživatel) */
    private boolean selectingProgrammatically = false;
    /** balík, typ a výstupy běžícího balíku pro události jádra ve vlákně validace */
    private volatile GroupPackageContext groupContext;

    @Override
    public void start(Stage primaryStage) throws Exception {
    }

    @FXML
    public void initialize() {
        emptyTitle.setText("Komplexní validátor " + Version.VERSION_CODE);
        sectionList.setCellFactory(list -> new SectionCell(() -> filter));
        ruleList.setCellFactory(list -> new RuleCell(() -> filter));
        problemList.setCellFactory(list -> new ProblemCell(problemList));
        sectionList.setPlaceholder(placeholder("Sekce se načítají."));
        ruleList.setPlaceholder(placeholder("Vyberte sekci."));
        problemList.setPlaceholder(placeholder("Vyberte pravidlo."));
        sectionList.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, nowSelected) -> {
            if (nowSelected == null) {
                selectSection(null);
            } else if (!nowSelected.equals(selectedSection)) {
                selectSection(nowSelected);
            }
        });
        ruleList.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, nowSelected) -> {
            if (nowSelected == null) {
                selectRule(null);
            } else if (!nowSelected.equals(selectedRule)) {
                selectRule(nowSelected);
            }
        });
        for (CheckBox box : new CheckBox[]{filterErrors, filterWarnings, filterInfos, filterOnlyWithProblems}) {
            box.selectedProperty().addListener((obs, oldValue, newValue) -> applyFilter());
        }
        initCopyActions();
        initGroupTable();
        cancelMenuItem.setAccelerator(cancelAccelerator());
        elapsedTimer = new Timeline(new KeyFrame(Duration.seconds(1), e -> refreshRunningHeader()));
        elapsedTimer.setCycleCount(Timeline.INDEFINITE);
        showCenter(emptyState);
        setGroupLayout(false);
        updateStatus("Připraveno", TotalState.IDLE);
    }

    /**
     * Zkratka „Zrušit validaci“ podle zvyklostí platformy: na macOS Cmd+. (Esc tam zavírá okna a nabídky), jinde Esc.
     * Ostatní zkratky hlavního okna jsou Cmd/Ctrl+N a Cmd/Ctrl+R, nekolidují.
     */
    private static KeyCombination cancelAccelerator() {
        boolean mac = System.getProperty("os.name", "").toLowerCase().contains("mac");
        return mac ? new KeyCodeCombination(KeyCode.PERIOD, KeyCombination.SHORTCUT_DOWN) : new KeyCodeCombination(KeyCode.ESCAPE);
    }

    private static Label placeholder(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("text-muted");
        label.setWrapText(true);
        return label;
    }

    /** Kopírování do schránky: kontextové nabídky seznamů a Cmd/Ctrl+C v seznamu problémů. */
    private void initCopyActions() {
        MenuItem copyProblem = new MenuItem("Kopírovat problém");
        copyProblem.setOnAction(e -> copySelectedProblem());
        MenuItem copyAllProblems = new MenuItem("Kopírovat všechny zobrazené problémy");
        copyAllProblems.setOnAction(e -> copyAllShownProblems());
        problemList.setContextMenu(new ContextMenu(copyProblem, copyAllProblems));
        KeyCombination copyKey = new KeyCodeCombination(KeyCode.C, KeyCombination.SHORTCUT_DOWN);
        problemList.setOnKeyPressed(e -> {
            if (copyKey.match(e)) {
                copySelectedProblem();
                e.consume();
            }
        });

        MenuItem copyRule = new MenuItem("Kopírovat název a popis pravidla");
        copyRule.setOnAction(e -> {
            RuleWithState rule = ruleList.getSelectionModel().getSelectedItem();
            if (rule != null) {
                ProblemCell.copyToClipboard(nameAndDescription(rule.getName(), rule.getDescription()));
            }
        });
        ruleList.setContextMenu(new ContextMenu(copyRule));

        MenuItem copySection = new MenuItem("Kopírovat název a popis sekce");
        copySection.setOnAction(e -> {
            SectionWithState section = sectionList.getSelectionModel().getSelectedItem();
            if (section != null) {
                ProblemCell.copyToClipboard(nameAndDescription(section.getName(), section.getDescription()));
            }
        });
        sectionList.setContextMenu(new ContextMenu(copySection));
    }

    private static String nameAndDescription(String name, String description) {
        return description == null || description.trim().isEmpty() ? name : name + "\n" + description.trim();
    }

    private void copySelectedProblem() {
        ValidationProblem problem = problemList.getSelectionModel().getSelectedItem();
        if (problem != null) {
            ProblemCell.copyToClipboard(problem.getFullMessage());
        }
    }

    private void copyAllShownProblems() {
        if (problemList.getItems() == null || problemList.getItems().isEmpty()) {
            return;
        }
        StringBuilder text = new StringBuilder();
        for (ValidationProblem problem : problemList.getItems()) {
            text.append(problem.getLevel().name()).append(": ").append(problem.getFullMessage()).append('\n');
        }
        ProblemCell.copyToClipboard(text.toString());
    }

    // ===== prazdny stav =====

    /**
     * Doplní do prázdného stavu počet fDMF a dostupnost nástrojů (po inicializaci validačních dat a po kontrole
     * nástrojů). Dokud nástroje nejsou zkontrolované, řádek o nástrojích se nezobrazí.
     */
    public void refreshEmptyStateInfo() {
        ValidationDataManager data = main == null ? null : main.getValidationDataManager();
        if (data == null) {
            setTextOrHide(emptyFdmfInfo, null);
            setTextOrHide(emptyToolsInfo, null);
            return;
        }
        int fdmfs = 0;
        if (data.getFdmfRegistry() != null) {
            for (Dmf.Type type : Dmf.Type.values()) {
                fdmfs += DocumentTypes.availableVersions(data.getFdmfRegistry(), type).size();
            }
        }
        setTextOrHide(emptyFdmfInfo, fdmfs == 0 ? null : ResultTexts.fdmfCount(fdmfs));
        Integer available = availableTools(data.getExternalUtilManager());
        setTextOrHide(emptyToolsInfo, available == null ? null : ResultTexts.toolsAvailable(available, ExternalUtil.values().length));
    }

    /** Počet dostupných nástrojů, null, když některý ještě nebyl zkontrolován. */
    private static Integer availableTools(ExternalUtilManager manager) {
        if (manager == null) {
            return null;
        }
        int available = 0;
        for (ExternalUtil util : ExternalUtil.values()) {
            try {
                if (manager.isUtilAvailable(util)) {
                    available++;
                }
            } catch (NullPointerException e) {
                //dostupnost nastroje se jeste nezjistovala
                return null;
            }
        }
        return available;
    }

    private static void setTextOrHide(Label label, String text) {
        label.setText(text);
        label.setVisible(text != null);
        label.setManaged(text != null);
    }

    private void showCenter(VBox pane) {
        for (VBox candidate : new VBox[]{emptyState, messageState, resultsPane}) {
            candidate.setVisible(candidate == pane);
        }
    }

    // ===== spusteni validace =====

    private void initBeforeValidation() {
        //data spojena s konkretnim behem validace
        validationStateManager = null;
        shownManager = null;
        progress = null;
        pspDir = null;
        dmf = null;
        //stav validace
        selectedSection = null;
        selectedRule = null;
        filteredSections = null;
        filteredRules = null;
        filteredProblems = null;
        //views
        sectionList.setItems(null);
        sectionList.setPlaceholder(placeholder("Sekce se načítají."));
        ruleList.setItems(null);
        problemList.setItems(null);
        selectSection(null);
        showCenter(resultsPane);
        hideNotice();
    }

    /**
     * Spustí validaci adresáře balíku (u ZIPu už rozbaleného).
     *
     * @param request požadavek, ze kterého validace vznikla (pro „Validovat znovu“), nebo null
     */
    public void runPspDirValidation(File pspDir, DmfDetector.Params dmfDetectorParams, boolean createTxtLog, boolean createXmlLog,
                                    int verbosity, ValidationRequest request) {
        leaveGroupMode();
        startRun(pspDir, request, createTxtLog ? buildTxtLogFile(pspDir) : null, createXmlLog ? buildXmlLogFile(pspDir) : null);

        validationTask = new Task<Void>() {

            @Override
            protected Void call() throws Exception {
                validationTaskEntered = true;
                PrintStream out = null;
                try {
                    dmf = new DmfDetector().resolveDmf(pspDir, dmfDetectorParams);
                    Validator validator = Utils.buildValidator(main.getValidationDataManager(), dmf, pspDir);
                    out = buildTxtLogPrintstream();
                    Validator.DevParams devParams = devParams();
                    if (isCancelled() || cancelValidation) {
                        //zruseno pred spustenim validatoru (jadro pak onValidationsCancel nezavola)
                        pendingOutcome = MainController.this::onStopped;
                        return null;
                    }
                    validator.run(pspDir,
                            logXmlFile, out,
                            verbosity,
                            devParams,
                            toSetOfSkippedSectionNames(validator, dmf),
                            MainController.this,
                            MainController.this, dmf);
                } catch (Exception e) {
                    String message = e.getMessage();
                    //chyba az po ohlaseni vysledku (napr. pri zapisu protokolu) ma prednost pred vysledkem
                    pendingOutcome = () -> onFailed(message);
                    e.printStackTrace();
                } finally {
                    if (out != null) {
                        out.close();
                    }
                    Runnable outcome = pendingOutcome;
                    pendingOutcome = null;
                    if (outcome != null) {
                        Platform.runLater(outcome);
                    }
                }
                return null;
            }

            private PrintStream buildTxtLogPrintstream() {
                if (logTxtFile == null) {
                    return null;
                } else {
                    try {
                        return new PrintStream(logTxtFile);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                        return null;
                    }
                }
            }
        };
        new Thread(validationTask).start();
    }

    /** Jen ve vývojovém režimu: validují se jen vybrané sekce. */
    private static Validator.DevParams devParams() {
        Validator.DevParams devParams = null;
        if (ConfigurationManager.DEV_MODE && ConfigurationManager.DEV_MODE_ONLY_SELECTED_SECTIONS) {
            devParams = new Validator.DevParams();
            //devParams.getSectionsToRun().add("Bibliografická metadata");
            devParams.getSectionsToRun().add("Identifikátory");
            devParams.getSectionsToRun().add("Soubor CHECKSUM");
            devParams.getSectionsToRun().add("Soubor info");
            devParams.getSectionsToRun().add("Struktura souborů");
            devParams.getSectionsToRun().add("Primární METS filesec");
            //devParams.getSectionsToRun().add("Obrazová data");
        }
        return devParams;
    }

    /** Okno do stavu „běží“: data předchozí validace pryč, horní lišta, panel průběhu, uplynulý čas. */
    private void startRun(File pspDir, ValidationRequest request, File logTxtFile, File logXmlFile) {
        initBeforeValidation();
        this.pspDir = pspDir;
        this.currentRequest = request;
        this.logTxtFile = logTxtFile;
        this.logXmlFile = logXmlFile;
        this.cancelValidation = false;
        this.validationTaskEntered = false;
        this.pendingOutcome = null;
        rerunState.validationStarted(request);
        startedValidations++;
        runStartedAt = System.currentTimeMillis();
        updateStatus(String.format("Inicializuji balík %s.", pspDir.getAbsolutePath()), TotalState.RUNNING);
        refreshRunningHeader();
        refreshProgress();
        setVisibleManaged(progressPane, true);
        elapsedTimer.playFromStart();
    }

    /**
     * Jen pro SnapshotMode (měření výkonu UI): okno přejde do stavu „běží“ bez spuštění validátoru; události jádra pak
     * posílá SnapshotMode sám přes metody {@link ValidationState.ProgressListener} a nakonec zavolá
     * {@link #completeSyntheticRun()}.
     */
    void beginSyntheticRun(File fakePspDir) {
        leaveGroupMode();
        startRun(fakePspDir, null, null, null);
    }

    /** Jen pro SnapshotMode: zobrazí výsledek ohlášený událostmi (jako po návratu z validátoru). */
    void completeSyntheticRun() {
        Runnable outcome = pendingOutcome;
        pendingOutcome = null;
        if (outcome != null) {
            Platform.runLater(outcome);
        }
    }

    /**
     * Nazvy sekci pouzite fDMF, ktere jsou v konfiguraci GUI oznacene k preskoceni (stejne jako dialog preskakovani:
     * jen sekce, ktere fDMF skutecne ma). Sekce se berou z uz postaveneho validatoru, neni potreba stavet validatory
     * pro vsechny fDMF.
     */
    private Set<String> toSetOfSkippedSectionNames(Validator validator, Dmf dmf) {
        ConfigurationManager configurationManager = main.getConfigurationManager();
        Set<String> skippedInConfig = configurationManager.getStringSet(configurationManager.propSkippedValidationSections(dmf));
        List<RulesSection> sections = validator.getEngine().getRuleSections();
        Set<String> result = new HashSet<>(sections.size());
        for (RulesSection section : sections) {
            if (skippedInConfig.contains(section.getName())) {
                result.add(section.getName());
            }
        }
        return result;
    }

    private void onFailed(String message) {
        endRunUi();
        updateStatus(message == null ? "Validace skončila chybou." : String.format("Chyba: %s", message), TotalState.ERROR);
        showHeader(ResultHeader.failed(pspDir.getName(), dmf, foundSoFar()));
        if (validationStateManager == null) {
            messageTitle.setText("Validace skončila chybou");
            messageText.setText(message == null ? "Podrobnosti jsou ve výpisu aplikace." : message);
            showCenter(messageState);
        } else {
            validationStateManager.markUnfinished(ProcessingState.INTERRUPTED);
            refreshSelectedRule();
            showNotice("notice-failed", "Validace skončila chybou: " + (message == null ? "podrobnosti jsou ve výpisu aplikace" : message)
                    + "\nVýsledky jsou neúplné, sekce a pravidla, která nedoběhla, jsou označená „nedokončeno“.");
        }
    }

    private void onStopped() {
        endRunUi();
        updateStatus("Validace balíku zrušena.", TotalState.STOPPED);
        showHeader(ResultHeader.stopped(pspDir.getName(), dmf, foundSoFar()));
        if (validationStateManager == null) {
            messageTitle.setText("Validace zrušena");
            messageText.setText("Validace byla zrušena před kontrolou prvního pravidla.");
            showCenter(messageState);
        } else {
            validationStateManager.markUnfinished(ProcessingState.CANCELED);
            refreshSelectedRule();
            showNotice("notice-stopped", "Validace byla zrušena. Výsledky jsou neúplné, sekce a pravidla, která nedoběhla, jsou označená „zrušeno“.");
        }
    }

    /** Průběžné počty pro horní lištu po zrušení nebo chybě, null, když se pravidla nenačetla. */
    private ProblemCounts foundSoFar() {
        return progress == null ? null : progress.getFound();
    }

    /** Konec běhu: zastaví uplynulý čas a skryje panel průběhu. */
    private void endRunUi() {
        elapsedTimer.stop();
        setVisibleManaged(progressPane, false);
    }

    /** Po změně stavu vybraného pravidla na konci validace (zástupný text seznamu problémů). */
    private void refreshSelectedRule() {
        if (selectedRule != null) {
            selectRule(selectedRule);
        } else {
            updatePlaceholders();
        }
    }

    private void showNotice(String styleClass, String text) {
        noticeBar.getStyleClass().removeAll("notice-stopped", "notice-failed");
        noticeBar.getStyleClass().add(styleClass);
        noticeText.setText(text);
        setVisibleManaged(noticeBar, true);
    }

    private void hideNotice() {
        setVisibleManaged(noticeBar, false);
        noticeText.setText(null);
    }

    // ===== prubeh =====

    /** Horní lišta během běhu: podtitul s uplynulým časem a průběžné počty (volá se i každou sekundu). */
    private void refreshRunningHeader() {
        if (groupRun != null) {
            showGroupHeader();
            return;
        }
        if (totalState != TotalState.RUNNING || pspDir == null) {
            return;
        }
        showHeader(ResultHeader.running(pspDir.getName(), dmf, System.currentTimeMillis() - runStartedAt, foundSoFar()));
    }

    /** Panel průběhu: před ohlášením pravidel inicializace s neurčitým ukazatelem, pak sekce, pravidlo a podíl. */
    private void refreshProgress() {
        String title;
        String counts;
        String detail;
        double fraction;
        if (groupRun != null) {
            title = groupRun.progressTitle();
            counts = groupRun.progressCounts();
            if (groupRun.getCurrent() == null) {
                detail = null;
            } else if (progress == null) {
                detail = groupRun.getCurrent().isZip() ? "Rozbaluji soubor ZIP, rozpoznávám typ a verzi balíku a připravuji validátor."
                        : ValidationProgress.INITIALIZING_DETAIL;
            } else {
                detail = progress.getDetail() == null ? progress.getTitle() : progress.getTitle() + " – " + progress.getDetail();
            }
            fraction = groupRun.fraction(progress == null ? 0 : progress.getFraction());
        } else if (progress == null) {
            title = ValidationProgress.INITIALIZING_TITLE;
            counts = null;
            detail = ValidationProgress.INITIALIZING_DETAIL;
            fraction = ValidationProgress.INDETERMINATE;
        } else {
            title = progress.getTitle();
            counts = progress.getCountsText();
            detail = progress.getDetail();
            fraction = progress.getFraction();
        }
        progressTitle.setText(title);
        setTextOrHide(progressCounts, counts);
        //radek zustava i bez textu, aby panel neposkakoval
        progressDetail.setText(detail == null ? "" : detail);
        progressBar.setProgress(fraction);
    }

    // ===== horni lista =====

    private void showHeader(ResultHeader content) {
        header.setVisible(true);
        header.setManaged(true);
        headerTitle.setText(content.getTitle());
        //bublina jen pri zmene (behem behu se lista obnovuje kazdou sekundu)
        String source = sourceDescription();
        if (headerTitle.getTooltip() == null || !source.equals(headerTitle.getTooltip().getText())) {
            headerTitle.setTooltip(new Tooltip(source));
        }
        headerSubtitle.setText(content.getSubtitle());

        boolean hasVerdict = content.getVerdictText() != null;
        verdictBadge.setText(content.getVerdictText());
        verdictBadge.getStyleClass().removeAll(VERDICT_CLASSES);
        if (hasVerdict) {
            verdictBadge.getStyleClass().add(content.getVerdictStyleClass());
        }
        setVisibleManaged(verdictBadge, hasVerdict);

        boolean counts = content.isCountsVisible();
        errorsBadge.setText(content.getErrorsText());
        warningsBadge.setText(content.getWarningsText());
        infosBadge.setText(content.getInfosText());
        setVisibleManaged(errorsBadge, counts);
        setVisibleManaged(warningsBadge, counts);
        setVisibleManaged(infosBadge, counts);
        for (Label badge : new Label[]{groupValidBadge, groupInvalidBadge, groupErrorBadge, groupWaitingBadge}) {
            setVisibleManaged(badge, false);
        }

        boolean running = content.isRunning();
        setVisibleManaged(cancelButton, running);
        setVisibleManaged(rerunButton, !running);

        //odkazy na vystupy, ktere si validace vyzadala (protokol XML jadro dopisuje az po ohlaseni konce validace)
        boolean finished = content.getPhase() == ResultHeader.Phase.FINISHED;
        setVisibleManaged(xmlLogLink, finished && logXmlFile != null);
        setVisibleManaged(txtLogLink, finished && logTxtFile != null);
        setVisibleManaged(headerLinks, finished && (logXmlFile != null || logTxtFile != null));
    }

    /** Bublina u názvu balíku: odkud se validovalo. */
    private String sourceDescription() {
        if (currentRequest != null && currentRequest.getSource() == ValidationSource.GROUP) {
            return "Adresář skupiny: " + pspDir.getAbsolutePath();
        }
        if (currentRequest != null && currentRequest.getSource() == ValidationSource.ZIP) {
            return "Soubor ZIP: " + currentRequest.getFile().getAbsolutePath() + "\nRozbaleno do: " + pspDir.getAbsolutePath();
        }
        return "Adresář balíku: " + pspDir.getAbsolutePath();
    }

    private static void setVisibleManaged(javafx.scene.Node node, boolean visible) {
        node.setVisible(visible);
        node.setManaged(visible);
    }

    // ===== stav =====

    private void updateStatus(String text, TotalState state) {
        totalState = state;
        statusText.setText(text);
        boolean running = state == TotalState.RUNNING;
        //menu: Validovat zustava dostupne kvuli zruseni
        newValidationMenuItem.setDisable(running);
        cancelMenuItem.setDisable(!running);
        menuSettings.setDisable(running);
        menuShow.setDisable(running);
        //stavovy radek (prubeh behem behu ukazuje panel prubehu)
        statusDot.getStyleClass().removeAll(STATUS_DOT_CLASSES);
        switch (state) {
            case FINISHED:
                statusDot.getStyleClass().add("status-dot-ok");
                break;
            case ERROR:
                statusDot.getStyleClass().add("status-dot-error");
                break;
            case STOPPED:
                statusDot.getStyleClass().add("status-dot-none");
                break;
            default:
                break;
        }
        statusDot.setVisible(state == TotalState.FINISHED || state == TotalState.ERROR || state == TotalState.STOPPED);
        //logy v menu Zobrazit jen po dokoncene validaci
        boolean finished = state == TotalState.FINISHED;
        showLogTxtMenuItem.setDisable(!finished || logTxtFile == null);
        showLogXmlMenuItem.setDisable(!finished || logXmlFile == null);
        //Validovat znovu
        if (!running && state != TotalState.IDLE) {
            rerunState.validationEnded();
        }
        boolean canRerun = rerunState.canRerun();
        rerunMenuItem.setDisable(!canRerun);
        rerunButton.setDisable(!canRerun);
        refreshGroupMenus();
    }

    /** naposledy pouzity zaklad nazvu vystupu a pocet jeho pouziti (pro odliseni vystupu se stejnym zakladem) */
    private String lastLogNameBase = null;
    private File lastLogNamePspDir = null;
    private String lastLogName = null;

    /**
     * Nazev vystupu (protokol a log) pro validaci baliku: cas na sekundy a nazev adresare baliku. Protokol i log tehoz
     * baliku dostanou stejny nazev. Kdyz by vystup se stejnym nazvem uz v adresari logu byl (dva baliky skupiny se
     * stejne pojmenovanym adresarem validovane v teze sekunde), prida se poradove cislo, aby se neprepsal.
     */
    private synchronized String buildValidationLogName(File pspDir) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss_");
        String base = dateFormat.format(new Date()) + pspDir.getName();
        if (base.equals(lastLogNameBase) && pspDir.equals(lastLogNamePspDir)) {
            //druhy vystup (log k protokolu) teze validace
            return lastLogName;
        }
        File logDir = getLogDir();
        String name = base;
        int order = 2;
        while (logDir != null && (new File(logDir, name + ".xml").exists() || new File(logDir, name + ".txt").exists())) {
            name = base + "_" + order++;
        }
        lastLogNameBase = base;
        lastLogNamePspDir = pspDir;
        lastLogName = name;
        return name;
    }

    private File getLogDir() {
        File logDir = main.getConfigurationManager().getFileOrNull(ConfigurationManager.PROP_LOG_DIR);
        if (logDir != null) {
            if (!logDir.exists()) {
                logDir.mkdirs();
            }
            return logDir;
        } else {
            return null;
        }
    }

    private File buildXmlLogFile(File pspDir) {
        File logDir = getLogDir();
        if (logDir != null) {
            return new File(logDir, buildValidationLogName(pspDir) + ".xml");
        } else {
            return null;
        }
    }

    private File buildTxtLogFile(File pspDir) {
        File logDir = getLogDir();
        if (logDir != null) {
            return new File(logDir, buildValidationLogName(pspDir) + ".txt");
        } else {
            return null;
        }
    }

    // ===== udalosti z jadra (pracovni vlakno) =====

    @Override
    public void onValidationsFinish(int globalProblemsTotal, Map<Level, Integer> globalProblemsByLevel, boolean valid, long duration) {
        LocalDateTime finishedAt = LocalDateTime.now();
        Map<Level, Integer> byLevel = globalProblemsByLevel == null ? null : new HashMap<>(globalProblemsByLevel);
        GroupPackageContext context = groupContext;
        if (context != null) {
            long packageDuration = System.currentTimeMillis() - context.startedAt;
            pendingOutcome = () -> groupPackageFinished(context, ProblemCounts.of(byLevel), valid, packageDuration);
            return;
        }
        pendingOutcome = () -> {
            endRunUi();
            updateStatus(String.format("Validace balíku %s hotova.", pspDir.getAbsolutePath()), TotalState.FINISHED);
            showHeader(ResultHeader.finished(pspDir.getName(), dmf, byLevel, valid, duration, finishedAt));
            finishedValidations++;
            if (selectedSection == null) {
                selectFirstProblem();
            }
        };
    }

    @Override
    public void onValidationsCancel() {
        GroupPackageContext context = groupContext;
        if (context != null) {
            long packageDuration = System.currentTimeMillis() - context.startedAt;
            pendingOutcome = () -> groupPackageCanceled(context.item, context.dmf, packageDuration);
            return;
        }
        pendingOutcome = this::onStopped;
    }

    @Override
    public void onInitialization(List<RulesSection> sections, Map<RulesSection, List<Rule>> rules) {
        ValidationStateManager manager = new ValidationStateManager(sections, rules);
        ValidationProgress newProgress = ValidationProgress.of(sections, rules);
        Platform.runLater(() -> {
            validationStateManager = manager;
            progress = newProgress;
            refreshRunningHeader();
            refreshProgress();
            if (groupRun != null) {
                groupPackageInitialized(manager, newProgress.getTotalRules());
                return;
            }
            showManager(manager);
            //fokus do seznamu sekci (ne na prvni zaskrtavatko filtru)
            sectionList.requestFocus();
        });
        RunHook hook = runHook;
        if (hook != null) {
            hook.initialized(newProgress.getTotalRules());
        }
    }

    @Override
    public void onValidationsStart() {
        Platform.runLater(() -> {
            if (groupRun != null) {
                if (runningItem != null) {
                    updateStatus(groupPackageStatus(runningItem), TotalState.RUNNING);
                }
                return;
            }
            updateStatus(String.format("Validuji balík %s.", pspDir.getAbsolutePath()), TotalState.RUNNING);
        });
    }

    @Override
    public void onSectionSkipped(int sectionId) {
        Platform.runLater(() -> {
            validationStateManager.updateSectionStatus(sectionId, ProcessingState.SKIPPED);
            progress.sectionSkipped(sectionId);
            refreshProgress();
        });
    }

    @Override
    public void onSectionStart(int sectionId) {
        Platform.runLater(() -> {
            validationStateManager.updateSectionStatus(sectionId, ProcessingState.RUNNING);
            progress.sectionStarted(sectionId);
            refreshProgress();
        });
    }

    @Override
    public void onSectionFinish(int sectionId, long duration) {
        Platform.runLater(() -> {
            validationStateManager.updateSectionStatus(sectionId, ProcessingState.FINISHED);
        });
    }

    @Override
    public void onSectionCancel(int sectionId) {
        Platform.runLater(() -> {
            validationStateManager.updateSectionStatus(sectionId, ProcessingState.CANCELED);
        });
    }

    @Override
    public void onRuleStart(int sectionId, int ruleId) {
        Platform.runLater(() -> {
            validationStateManager.updateRuleState(sectionId, ruleId, ProcessingState.RUNNING);
            refreshSelectedRuleIf(sectionId, ruleId);
            progress.ruleStarted(sectionId, ruleId);
            refreshProgress();
        });
    }

    @Override
    public void onRuleFinish(int sectionId, Map<Level, Integer> sectionProblemsByLevel, int sectionProblemsTotal, int ruleId, Map<Level, Integer> ruleProblemsByLevel, int ruleProblemsTotal, List<ValidationProblem> problems) {
        Platform.runLater(() -> {
            validationStateManager.updateSectionProblems(sectionId, sectionProblemsByLevel);
            //vysledky pred stavem: dokoncene pravidlo bez poctu by filtr "jen s problemy" na okamzik vyradil i s vyberem
            validationStateManager.setRuleResults(sectionId, ruleId, ruleProblemsByLevel, problems);
            validationStateManager.updateRuleState(sectionId, ruleId, ProcessingState.FINISHED);
            //aby se nacetly problemy prave dokonceneho vybraneho pravidla
            refreshSelectedRuleIf(sectionId, ruleId);
            progress.ruleFinished(sectionId, ruleId, ProblemCounts.of(ruleProblemsByLevel));
            refreshProgress();
            refreshRunningHeader();
        });
        RunHook hook = runHook;
        if (hook != null) {
            hook.ruleFinished();
        }
    }

    @Override
    public void onRuleCancel(int sectionId, int ruleId) {
        Platform.runLater(() -> {
            validationStateManager.updateRuleState(sectionId, ruleId, ProcessingState.CANCELED);
            refreshSelectedRuleIf(sectionId, ruleId);
        });
    }

    private void refreshSelectedRuleIf(int sectionId, int ruleId) {
        if (shownManager == validationStateManager && selectedRule != null && selectedRule.getSectionId() == sectionId && selectedRule.getId() == ruleId) {
            selectRule(selectedRule);
        }
    }

    @Override
    public boolean shouldCancel() {
        return cancelValidation;
    }

    // ===== vyber sekce a pravidla, filtr =====

    private void selectSection(SectionWithState section) {
        selectedSection = section;
        if (section != null) {
            rulesHeader.setText("Pravidla sekce " + section.getName());
            setTextOrHide(sectionDescription, emptyToNull(section.getDescription()));
            filteredRules = new FilteredList<>(shownManager.getRulesObervable(section.getId()), filter::showRule);
            ruleList.setItems(filteredRules);
        } else {
            rulesHeader.setText("Pravidla");
            setTextOrHide(sectionDescription, null);
            filteredRules = null;
            ruleList.setItems(null);
        }
        rulesHeader.setTooltip(new Tooltip(rulesHeader.getText()));
        selectRule(null);
        updatePlaceholders();
    }

    private void selectRule(RuleWithState rule) {
        problemsProgressIndicator.setVisible(false);
        filteredProblems = null;
        if (rule != null && selectedSection != null) {
            selectedRule = rule;
            problemsHeader.setText("Problémy pravidla " + rule.getName());
            setTextOrHide(ruleDescription, emptyToNull(rule.getDescription()));
            ObservableList<ValidationProblem> problems = shownManager.getProblems(rule.getSectionId(), rule.getId());
            if (rule.getState() == ProcessingState.FINISHED && problems != null) {
                filteredProblems = new FilteredList<>(problems, problem -> filter.accepts(problem.getLevel()));
                problemList.setItems(filteredProblems);
            } else {
                problemList.setItems(null);
                problemsProgressIndicator.setVisible(rule.getState() == ProcessingState.RUNNING);
            }
        } else {
            selectedRule = null;
            problemsHeader.setText("Problémy");
            setTextOrHide(ruleDescription, null);
            problemList.setItems(null);
        }
        problemsHeader.setTooltip(new Tooltip(problemsHeader.getText()));
        updatePlaceholders();
    }

    private static String emptyToNull(String text) {
        return text == null || text.trim().isEmpty() ? null : text.trim();
    }

    private void applyFilter() {
        filter = new ProblemFilter(filterErrors.isSelected(), filterWarnings.isSelected(), filterInfos.isSelected(),
                filterOnlyWithProblems.isSelected());
        if (filteredSections != null) {
            filteredSections.setPredicate(filter::showSection);
        }
        if (filteredRules != null) {
            filteredRules.setPredicate(filter::showRule);
        }
        if (filteredProblems != null) {
            filteredProblems.setPredicate(problem -> filter.accepts(problem.getLevel()));
        }
        //pocty v radcich zavisi na filtru
        sectionList.refresh();
        ruleList.refresh();
        updatePlaceholders();
    }

    private void updatePlaceholders() {
        if (shownManager == null) {
            sectionList.setPlaceholder(placeholder("Sekce se načítají."));
        } else {
            sectionList.setPlaceholder(placeholder("Žádná sekce nemá problémy zobrazených úrovní."));
        }
        if (selectedSection == null) {
            ruleList.setPlaceholder(placeholder("Vyberte sekci."));
        } else {
            ruleList.setPlaceholder(placeholder("Žádné pravidlo nemá problémy zobrazených úrovní."));
        }
        if (selectedRule == null) {
            problemList.setPlaceholder(placeholder("Vyberte pravidlo."));
        } else if (selectedRule.getState() == ProcessingState.FINISHED) {
            problemList.setPlaceholder(placeholder(selectedRule.getErrors() + selectedRule.getWarnings() + selectedRule.getInfos() == 0
                    ? "Pravidlo nenašlo žádný problém." : "Pravidlo nemá problémy zobrazených úrovní."));
        } else if (selectedRule.getState() == ProcessingState.RUNNING) {
            problemList.setPlaceholder(new Label());
        } else if (selectedRule.getState() == ProcessingState.CANCELED) {
            problemList.setPlaceholder(placeholder("Kontrola pravidla byla zrušena."));
        } else if (selectedRule.getState() == ProcessingState.INTERRUPTED) {
            problemList.setPlaceholder(placeholder("Kontrola pravidla nebyla dokončena, validace skončila chybou."));
        } else {
            problemList.setPlaceholder(placeholder("Pravidlo se ještě nekontrolovalo."));
        }
    }

    /**
     * Vybere první zobrazenou sekci s problémem a v ní první pravidlo s problémem (chyby mají přednost); když problém
     * není nikde, vybere první sekci.
     *
     * @return true, když se vybralo pravidlo s problémem
     */
    boolean selectFirstProblem() {
        return selectFirstProblem(false);
    }

    /**
     * @param finishedOnly jen mezi dokončenými sekcemi (pro SnapshotMode: procházení hotové sekce během běhu)
     */
    boolean selectFirstProblem(boolean finishedOnly) {
        if (filteredSections == null || filteredSections.isEmpty()) {
            return false;
        }
        List<SectionWithState> candidates = filteredSections;
        if (finishedOnly) {
            candidates = new ArrayList<>();
            for (SectionWithState section : filteredSections) {
                if (section.getState() == ProcessingState.FINISHED) {
                    candidates.add(section);
                }
            }
        }
        SectionWithState section = firstWithProblem(candidates, true);
        if (section == null) {
            section = firstWithProblem(candidates, false);
        }
        if (section == null) {
            sectionList.getSelectionModel().select(filteredSections.get(0));
            return false;
        }
        sectionList.getSelectionModel().select(section);
        sectionList.scrollTo(section);
        if (filteredRules == null) {
            return false;
        }
        for (boolean errorsOnly : new boolean[]{true, false}) {
            for (RuleWithState rule : filteredRules) {
                int count = errorsOnly ? rule.getErrors() : filter.visibleCount(rule.getErrors(), rule.getWarnings(), rule.getInfos());
                if (count > 0) {
                    ruleList.getSelectionModel().select(rule);
                    ruleList.scrollTo(rule);
                    return true;
                }
            }
        }
        return false;
    }

    private SectionWithState firstWithProblem(List<SectionWithState> sections, boolean errorsOnly) {
        for (SectionWithState section : sections) {
            int count = errorsOnly ? section.getErrors() : filter.visibleCount(section.getErrors(), section.getWarnings(), section.getInfos());
            if (count > 0) {
                return section;
            }
        }
        return null;
    }

    // ===== skupina baliku =====

    /** Nastavení validace skupiny z požadavku (pro všechny balíky i pro „Validovat znovu jen tento balík“). */
    private static final class GroupParams {
        final DmfDetector.Params params;
        final boolean createTxtLog;
        final boolean createXmlLog;
        final int verbosity;

        GroupParams(DmfDetector.Params params, boolean createTxtLog, boolean createXmlLog, int verbosity) {
            this.params = DmfParamsBuilder.copy(params);
            this.createTxtLog = createTxtLog;
            this.createXmlLog = createXmlLog;
            this.verbosity = verbosity;
        }
    }

    /** Běžící balík skupiny pro události jádra (vlákno validace). */
    private static final class GroupPackageContext {
        final GroupItem item;
        final File xmlProtocol;
        final File txtLog;
        final Dmf dmf;
        final long startedAt;

        GroupPackageContext(GroupItem item, File xmlProtocol, File txtLog, Dmf dmf, long startedAt) {
            this.item = item;
            this.xmlProtocol = xmlProtocol;
            this.txtLog = txtLog;
            this.dmf = dmf;
            this.startedAt = startedAt;
        }
    }

    private void initGroupTable() {
        groupTableModel = new GroupTable(groupTable);
        groupTable.setPlaceholder(placeholder("Žádný balík (podadresář nebo soubor ZIP) k zobrazení."));
        groupTable.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, nowSelected) -> {
            if (selectingProgrammatically || nowSelected == null || nowSelected == shownItem) {
                return;
            }
            //uzivatel vybral balik: tabulka uz nesleduje bezici balik
            followRunning = false;
            showPackage(nowSelected);
        });
        groupOnlyInvalid.selectedProperty().addListener((obs, oldValue, newValue) -> {
            selectingProgrammatically = true;
            try {
                groupTableModel.setOnlyInvalid(newValue);
                restoreGroupSelection();
            } finally {
                selectingProgrammatically = false;
            }
            refreshGroupShownText();
        });
        MenuItem rerunOne = new MenuItem("Validovat znovu jen tento balík");
        rerunOne.setOnAction(e -> rerunPackage(groupTable.getSelectionModel().getSelectedItem()));
        MenuItem openXml = new MenuItem("Otevřít protokol XML");
        openXml.setOnAction(e -> openOutput(groupTable.getSelectionModel().getSelectedItem(), true));
        MenuItem openTxt = new MenuItem("Otevřít textový log");
        openTxt.setOnAction(e -> openOutput(groupTable.getSelectionModel().getSelectedItem(), false));
        MenuItem copyPath = new MenuItem("Kopírovat cestu k balíku");
        copyPath.setOnAction(e -> {
            GroupItem item = groupTable.getSelectionModel().getSelectedItem();
            if (item != null) {
                ProblemCell.copyToClipboard(item.getFile().getAbsolutePath());
            }
        });
        ContextMenu menu = new ContextMenu(rerunOne, new SeparatorMenuItem(), openXml, openTxt, copyPath);
        menu.setOnShowing(e -> {
            GroupItem item = groupTable.getSelectionModel().getSelectedItem();
            boolean running = groupRun == null || groupRun.isRunning();
            rerunOne.setDisable(item == null || running);
            openXml.setDisable(item == null || !item.getStatus().isDone() || item.getXmlProtocol() == null);
            openTxt.setDisable(item == null || !item.getStatus().isDone() || item.getTxtLog() == null);
            copyPath.setDisable(item == null);
        });
        groupTable.setContextMenu(menu);
    }

    /**
     * Uspořádání výsledků: u jednoho balíku jen panely výsledků (jako dřív), u skupiny tabulka balíků nad nimi
     * a řádek s vybraným balíkem.
     */
    private void setGroupLayout(boolean group) {
        if (group) {
            if (!groupSplit.getItems().contains(detailPane)) {
                resultsPane.getChildren().remove(detailPane);
                groupSplit.getItems().setAll(groupPane, detailPane);
                groupSplit.setDividerPositions(0.42);
            }
            if (!resultsPane.getChildren().contains(groupSplit)) {
                resultsPane.getChildren().setAll(groupSplit);
            }
        } else {
            groupSplit.getItems().remove(detailPane);
            resultsPane.getChildren().setAll(detailPane);
            VBox.setVgrow(detailPane, javafx.scene.layout.Priority.ALWAYS);
        }
        refreshPackageBar();
    }

    /** Konec režimu skupiny (spouští se validace jednoho balíku): data skupiny pryč, okno jako u jednoho balíku. */
    private void leaveGroupMode() {
        if (groupRun == null) {
            return;
        }
        groupRun = null;
        groupParams = null;
        groupDetails.clear();
        shownItem = null;
        runningItem = null;
        groupContext = null;
        followRunning = true;
        groupTableModel.setItems(Collections.<GroupItem>emptyList());
        setGroupLayout(false);
    }

    /**
     * Spustí validaci skupiny: přečte adresář (balíky jsou jeho podadresáře a soubory .zip) a validuje balíky postupně.
     *
     * @param request požadavek, ze kterého validace vznikla (pro „Validovat znovu“), nebo null
     */
    public void runGroupValidation(File groupDir, DmfDetector.Params dmfDetectorParams, boolean createTxtLog, boolean createXmlLog,
                                   int verbosity, ValidationRequest request) {
        GroupPackages.Listing listing;
        try {
            listing = GroupPackages.list(groupDir);
        } catch (IOException e) {
            failGroupStart(groupDir, request, e.getMessage());
            return;
        }
        leaveGroupMode();
        groupRun = new GroupRun(listing);
        groupParams = new GroupParams(dmfDetectorParams, createTxtLog, createXmlLog, verbosity);
        groupDetails.clear();
        shownItem = null;
        runningItem = null;
        followRunning = true;
        groupTableModel.setItems(groupRun.getItems());
        setGroupLayout(true);
        startGroupRun(groupRun.getItems(), request);
    }

    /** Adresář skupiny nejde přečíst: chyba jako u jednoho balíku, „Validovat znovu“ zůstane dostupné. */
    private void failGroupStart(File groupDir, ValidationRequest request, String message) {
        leaveGroupMode();
        initBeforeValidation();
        pspDir = groupDir;
        currentRequest = request;
        logTxtFile = null;
        logXmlFile = null;
        rerunState.validationStarted(request);
        startedValidations++;
        updateStatus(String.format("Chyba: %s", message), TotalState.ERROR);
        showHeader(ResultHeader.failed(groupDir.getName(), null, null));
        messageTitle.setText("Skupinu balíků nelze validovat");
        messageText.setText(message);
        showCenter(messageState);
    }

    /** Okno do stavu „běží“ a start vlákna, které balíky běhu validuje jeden po druhém. */
    private void startGroupRun(List<GroupItem> toRun, ValidationRequest request) {
        initBeforeValidation();
        long now = System.currentTimeMillis();
        currentRequest = request;
        pspDir = groupRun.getGroupDir();
        logTxtFile = null;
        logXmlFile = null;
        cancelValidation = false;
        validationTaskEntered = false;
        pendingOutcome = null;
        groupContext = null;
        runningItem = null;
        for (GroupItem item : toRun) {
            groupDetails.remove(item);
        }
        groupRun.runStarted(toRun, now);
        rerunState.validationStarted(request);
        startedValidations++;
        runStartedAt = now;
        for (GroupItem item : toRun) {
            groupItemChanged(item);
        }
        updateStatus(String.format("Validuji skupinu %s.", groupRun.getGroupDir().getAbsolutePath()), TotalState.RUNNING);
        showGroupHeader();
        refreshProgress();
        setVisibleManaged(progressPane, true);
        elapsedTimer.playFromStart();
        showPackage(shownItem);
        refreshGroupShownText();

        List<GroupItem> queue = new ArrayList<>(toRun);
        GroupParams params = groupParams;
        validationTask = new Task<Void>() {
            @Override
            protected Void call() {
                validationTaskEntered = true;
                try {
                    for (int i = 0; i < queue.size(); i++) {
                        if (cancelValidation) {
                            break;
                        }
                        validateGroupPackage(queue.get(i), i + 1, queue.size(), params);
                    }
                } finally {
                    groupContext = null;
                    Platform.runLater(MainController.this::onGroupEnded);
                }
                return null;
            }
        };
        new Thread(validationTask).start();
    }

    /**
     * Validace jednoho balíku skupiny ve vlákně validace, stejně jako jednoho balíku (ZIP se rozbalí, typ a verze se
     * rozpoznají, sestaví se validátor). Chyba balíku běh nezastaví: balík dostane stav „chyba“ s hláškou. Rozbalený
     * ZIP se po validaci smaže.
     */
    private void validateGroupPackage(GroupItem item, int position, int total, GroupParams params) {
        RunHook hook = runHook;
        if (hook != null) {
            hook.packageStarted(position, total);
        }
        long startedAt = System.currentTimeMillis();
        Platform.runLater(() -> groupPackageStarted(item));
        pendingOutcome = null;
        groupContext = null;
        ZipUnpacker.Result unpacked = null;
        PrintStream out = null;
        Dmf resolved = null;
        try {
            File dir;
            if (item.isZip()) {
                unpacked = new ZipUnpacker().unpack(item.getFile(), () -> cancelValidation, null);
                dir = unpacked == null ? null : unpacked.getPspDir();
            } else {
                dir = item.getFile();
            }
            if (dir == null || cancelValidation) {
                pendingOutcome = canceledOutcome(item, null, startedAt);
            } else {
                File xml = params.createXmlLog ? buildXmlLogFile(dir) : null;
                File txt = params.createTxtLog ? buildTxtLogFile(dir) : null;
                resolved = new DmfDetector().resolveDmf(dir, DmfParamsBuilder.copy(params.params));
                Dmf resolvedDmf = resolved;
                Platform.runLater(() -> groupPackageResolved(item, resolvedDmf));
                Validator validator = Utils.buildValidator(main.getValidationDataManager(), resolved, dir);
                out = openTxtLog(txt);
                if (cancelValidation) {
                    pendingOutcome = canceledOutcome(item, resolved, startedAt);
                } else {
                    groupContext = new GroupPackageContext(item, xml, txt, resolved, startedAt);
                    validator.run(dir, xml, out, params.verbosity, devParams(), toSetOfSkippedSectionNames(validator, resolved),
                            this, this, resolved);
                }
            }
        } catch (ZipUnpacker.UnpackException e) {
            pendingOutcome = failedOutcome(item, null, e.getMessage(), startedAt);
        } catch (Exception e) {
            e.printStackTrace();
            //chyba az po ohlaseni vysledku (napr. pri zapisu protokolu) ma prednost pred vysledkem
            pendingOutcome = failedOutcome(item, resolved, e.getMessage(), startedAt);
        } finally {
            if (out != null) {
                out.close();
            }
            groupContext = null;
            Runnable outcome = pendingOutcome;
            pendingOutcome = null;
            if (outcome == null) {
                outcome = failedOutcome(item, resolved, "Validace skončila bez výsledku.", startedAt);
            }
            Platform.runLater(outcome);
            if (unpacked != null) {
                ZipUnpacker.deleteWorkDir(unpacked.getContainerDir());
            }
        }
    }

    private Runnable failedOutcome(GroupItem item, Dmf resolved, String message, long startedAt) {
        long duration = System.currentTimeMillis() - startedAt;
        return () -> groupPackageFailed(item, resolved, message, duration);
    }

    private Runnable canceledOutcome(GroupItem item, Dmf resolved, long startedAt) {
        long duration = System.currentTimeMillis() - startedAt;
        return () -> groupPackageCanceled(item, resolved, duration);
    }

    private static PrintStream openTxtLog(File txt) {
        if (txt == null) {
            return null;
        }
        try {
            return new PrintStream(txt);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }


    // ----- udalosti behu skupiny (vlakno UI) -----

    private void groupPackageStarted(GroupItem item) {
        runningItem = item;
        validationStateManager = null;
        progress = null;
        dmf = null;
        packageStartedAt = System.currentTimeMillis();
        groupDetails.remove(item);
        groupRun.packageStarted(item);
        updatePins();
        groupItemChanged(item);
        if (followRunning) {
            selectGroupItem(item);
        } else if (item == shownItem) {
            showPackage(item);
        }
        updateStatus(groupPackageStatus(item), TotalState.RUNNING);
        refreshProgress();
        showGroupHeader();
    }

    private String groupPackageStatus(GroupItem item) {
        return String.format("Validuji balík %s (%d z %d).", item.getFile().getAbsolutePath(), groupRun.currentPosition(),
                groupRun.getSession().size());
    }

    private void groupPackageResolved(GroupItem item, Dmf resolved) {
        if (item != runningItem) {
            return;
        }
        dmf = resolved;
        groupRun.packageResolved(item, resolved);
        groupItemChanged(item);
    }

    /** Jádro ohlásilo pravidla běžícího balíku: jeho výsledky se začnou plnit (a zobrazí se, je-li balík vybraný). */
    private void groupPackageInitialized(ValidationStateManager manager, int totalRules) {
        if (runningItem == null) {
            return;
        }
        groupDetails.put(runningItem, manager, totalRules);
        updatePins();
        if (shownItem == runningItem) {
            showManager(manager);
            refreshPackageBar();
        }
    }

    private void groupPackageFinished(GroupPackageContext context, ProblemCounts counts, boolean valid, long duration) {
        groupRun.packageFinished(context.item, context.dmf, counts, valid, duration, context.xmlProtocol, context.txtLog);
        storeDetail(context.item, counts);
        afterGroupPackage(context.item);
    }

    private void groupPackageFailed(GroupItem item, Dmf resolved, String message, long duration) {
        if (validationStateManager != null && item == runningItem) {
            validationStateManager.markUnfinished(ProcessingState.INTERRUPTED);
        }
        ProblemCounts found = item == runningItem ? foundSoFar() : null;
        groupRun.packageFailed(item, resolved, found, message, duration);
        storeDetail(item, found);
        afterGroupPackage(item);
    }

    private void groupPackageCanceled(GroupItem item, Dmf resolved, long duration) {
        if (validationStateManager != null && item == runningItem) {
            validationStateManager.markUnfinished(ProcessingState.CANCELED);
        }
        ProblemCounts found = item == runningItem ? foundSoFar() : null;
        groupRun.packageCanceled(item, resolved, found, duration);
        storeDetail(item, found);
        afterGroupPackage(item);
    }

    /** Podrobné výsledky doběhlého balíku zůstávají v paměti s vahou podle počtu pravidel a problémů. */
    private void storeDetail(GroupItem item, ProblemCounts counts) {
        if (validationStateManager == null || item != runningItem) {
            return;
        }
        long weight = (progress == null ? 0 : progress.getTotalRules())
                + (counts == null ? 0 : (long) counts.getErrors() + counts.getWarnings() + counts.getInfos());
        groupDetails.put(item, validationStateManager, weight);
    }

    private void afterGroupPackage(GroupItem item) {
        boolean shown = item == shownItem && shownManager != null && shownManager == validationStateManager;
        if (item == runningItem) {
            runningItem = null;
            validationStateManager = null;
            progress = null;
        }
        updatePins();
        groupItemChanged(item);
        if (shown) {
            refreshSelectedRule();
        }
        refreshProgress();
        showGroupHeader();
    }

    /** Konec běhu skupiny (všechny balíky prošly nebo zrušení). */
    private void onGroupEnded() {
        if (groupRun == null || !groupRun.isRunning()) {
            return;
        }
        groupRun.runEnded(System.currentTimeMillis(), LocalDateTime.now());
        runningItem = null;
        validationStateManager = null;
        progress = null;
        groupContext = null;
        endRunUi();
        for (GroupItem item : groupRun.getSession()) {
            groupItemChanged(item);
        }
        boolean canceled = groupRun.isCanceled();
        if (!canceled) {
            finishedValidations++;
        }
        updateStatus(groupRun.statusText(), canceled ? TotalState.STOPPED : TotalState.FINISHED);
        showGroupHeader();
        //po zruseni zustava vybrany rozpracovany balik, jinak prvni nevalidni (pokud uzivatel sam nic nevybral)
        if (followRunning && !canceled || shownItem == null) {
            GroupItem first = groupTableModel.firstProblemOrFirst();
            if (first != null) {
                selectGroupItem(first);
            }
        }
        refreshPackageBar();
        updatePins();
        if (canceled) {
            showNotice("notice-stopped", "Validace skupiny byla zrušena. Rozpracovaný balík je označený „zrušeno“, balíky, na které "
                    + "nedošla řada, „nevalidováno“; jejich výsledky v souhrnu chybí.");
        }
    }

    private void stopGroupValidation() {
        groupRun.cancelRequested();
        updateStatus("Ruším validaci skupiny…", TotalState.RUNNING);
        if (validationTask != null && !validationTaskEntered && validationTask.cancel()) {
            //zruseno driv, nez vlakno zacalo: nikdo jiny konec behu neohlasi
            onGroupEnded();
        }
        //jinak vlakno validace zrusi bezici balik pres shouldCancel a dalsi baliky uz nespusti (vlakno se neprerusuje,
        //aby se rozpracovane cteni souboru neohlasilo jako chyba baliku)
    }

    /** „Validovat znovu jen tento balík“: nový běh jen s tímto balíkem, výsledky ostatních zůstávají. */
    private void rerunPackage(GroupItem item) {
        if (item == null || groupRun == null || groupRun.isRunning() || groupParams == null) {
            return;
        }
        if (currentRequest != null) {
            //stejne vypnute nastroje jako pri behu cele skupiny
            main.getValidationDataManager().getExternalUtilManager().setUtilsDisabled(currentRequest.getDisabledUtils());
        }
        followRunning = false;
        selectGroupItem(item);
        startGroupRun(Collections.singletonList(item), currentRequest);
    }

    // ----- zobrazeni skupiny -----

    /** Řádek balíku se změnil: překreslí se, přeřadí a přefiltruje, výběr zůstane. */
    private void groupItemChanged(GroupItem item) {
        selectingProgrammatically = true;
        try {
            groupTableModel.itemChanged(item);
            restoreGroupSelection();
        } finally {
            selectingProgrammatically = false;
        }
        refreshGroupShownText();
        if (item == shownItem) {
            refreshPackageBar();
        }
    }

    private void restoreGroupSelection() {
        if (shownItem != null && groupTableModel.shownItems().contains(shownItem)
                && groupTable.getSelectionModel().getSelectedItem() != shownItem) {
            groupTable.getSelectionModel().select(shownItem);
        }
    }

    /** Vybere balík v tabulce (programově, nemění sledování běžícího balíku) a zobrazí jeho výsledky. */
    private void selectGroupItem(GroupItem item) {
        selectingProgrammatically = true;
        try {
            if (groupTableModel.shownItems().contains(item)) {
                groupTable.getSelectionModel().select(item);
                groupTable.scrollTo(item);
            } else {
                groupTable.getSelectionModel().clearSelection();
            }
        } finally {
            selectingProgrammatically = false;
        }
        showPackage(item);
    }

    /** Panely ukážou výsledky balíku (běžícího živě, hotového z paměti), nebo hlášku, když je balík nemá. */
    private void showPackage(GroupItem item) {
        shownItem = item;
        updatePins();
        ValidationStateManager manager = item == null ? null : item == runningItem ? validationStateManager : groupDetails.get(item);
        if (manager == null || manager != shownManager) {
            showManager(manager);
        }
        refreshPackageBar();
    }

    /** Panely sekcí, pravidel a problémů ukážou dané výsledky (výběr sekce a pravidla se zruší). */
    private void showManager(ValidationStateManager manager) {
        shownManager = manager;
        selectedSection = null;
        selectedRule = null;
        filteredSections = manager == null ? null : new FilteredList<>(manager.getSectionsObservable(), filter::showSection);
        sectionList.setItems(filteredSections);
        selectSection(null);
        updatePlaceholders();
    }

    private void updatePins() {
        Set<GroupItem> pinned = new HashSet<>();
        if (runningItem != null) {
            pinned.add(runningItem);
        }
        if (shownItem != null) {
            pinned.add(shownItem);
        }
        groupDetails.setPinned(pinned);
    }

    /** Řádek s vybraným balíkem nad výsledky a hláška místo výsledků (jen u skupiny). */
    private void refreshPackageBar() {
        boolean group = groupRun != null;
        setVisibleManaged(packageBar, group && shownItem != null);
        String message = null;
        if (group && shownItem == null) {
            message = groupRun.total() == 0 ? "Adresář neobsahuje žádný balík (podadresář nebo soubor ZIP)." : "Vyberte balík v tabulce.";
        } else if (group) {
            packageTitle.setText(shownItem.getName());
            String subtitle = GroupDetailTexts.subtitle(shownItem);
            packageSubtitle.setText(subtitle);
            packageSubtitle.setTooltip(new Tooltip(subtitle));
            boolean done = shownItem.getStatus().isDone();
            setVisibleManaged(packageXmlLink, done && shownItem.getXmlProtocol() != null);
            setVisibleManaged(packageTxtLink, done && shownItem.getTxtLog() != null);
            message = GroupDetailTexts.message(shownItem, shownManager != null, groupDetails.wasEvicted(shownItem));
        }
        packageMessage.setText(message);
        packageMessagePane.setVisible(message != null);
        resultsSplit.setVisible(message == null);
        refreshGroupMenus();
    }

    private void refreshGroupShownText() {
        if (groupRun == null) {
            return;
        }
        boolean filtered = groupTableModel.isOnlyInvalid();
        groupShownText.setText(filtered ? nkp.pspValidator.gui.group.GroupColumns.shownText(groupTableModel.shownCount(), groupTableModel.totalCount()) : "");
    }

    /** Horní lišta skupiny: název adresáře, podtitul s průběhem nebo dobou a počty balíků podle výsledku. */
    private void showGroupHeader() {
        if (groupRun == null) {
            return;
        }
        header.setVisible(true);
        header.setManaged(true);
        headerTitle.setText(groupRun.getTitle());
        String source = "Adresář skupiny: " + groupRun.getGroupDir().getAbsolutePath();
        if (headerTitle.getTooltip() == null || !source.equals(headerTitle.getTooltip().getText())) {
            headerTitle.setTooltip(new Tooltip(source));
        }
        headerSubtitle.setText(groupRun.subtitle(System.currentTimeMillis()));
        for (Label badge : new Label[]{verdictBadge, errorsBadge, warningsBadge, infosBadge}) {
            setVisibleManaged(badge, false);
        }
        setVisibleManaged(headerLinks, false);
        boolean running = groupRun.isRunning();
        groupValidBadge.setText(groupRun.validText());
        groupInvalidBadge.setText(groupRun.invalidText());
        groupErrorBadge.setText(groupRun.errorText());
        setVisibleManaged(groupValidBadge, true);
        setVisibleManaged(groupInvalidBadge, true);
        setVisibleManaged(groupErrorBadge, groupRun.count(PackageStatus.ERROR) > 0);
        int waiting = groupRun.count(PackageStatus.WAITING) + groupRun.count(PackageStatus.RUNNING);
        int notValidated = groupRun.count(PackageStatus.NOT_VALIDATED) + groupRun.count(PackageStatus.CANCELED);
        if (running || waiting > 0) {
            groupWaitingBadge.setText(groupRun.waitingText());
            setVisibleManaged(groupWaitingBadge, true);
        } else if (notValidated > 0) {
            groupWaitingBadge.setText(groupRun.notValidatedText());
            setVisibleManaged(groupWaitingBadge, true);
        } else {
            setVisibleManaged(groupWaitingBadge, false);
        }
        setVisibleManaged(cancelButton, running);
        setVisibleManaged(rerunButton, !running);
    }

    private String groupBadgesText() {
        StringBuilder text = new StringBuilder();
        for (Label badge : new Label[]{groupValidBadge, groupInvalidBadge, groupErrorBadge, groupWaitingBadge}) {
            if (badge.isVisible()) {
                text.append(text.length() == 0 ? "" : " / ").append(badge.getText());
            }
        }
        return text.toString();
    }

    /** Menu a tlačítko exportu, protokol a log v menu Zobrazit u skupiny podle vybraného balíku. */
    private void refreshGroupMenus() {
        boolean group = groupRun != null;
        exportCsvMenuItem.setDisable(!group);
        exportCsvButton.setDisable(!group);
        if (group) {
            boolean done = shownItem != null && shownItem.getStatus().isDone();
            showLogXmlMenuItem.setDisable(!done || shownItem.getXmlProtocol() == null);
            showLogTxtMenuItem.setDisable(!done || shownItem.getTxtLog() == null);
        }
    }

    private File shownOutput(boolean xml) {
        if (shownItem == null || !shownItem.getStatus().isDone()) {
            return null;
        }
        return xml ? shownItem.getXmlProtocol() : shownItem.getTxtLog();
    }

    private void openOutput(GroupItem item, boolean xml) {
        File file = item == null ? null : xml ? item.getXmlProtocol() : item.getTxtLog();
        if (file != null) {
            openUrl(file.toURI().toString());
        }
    }

    /** „Exportovat souhrn do CSV…“: všechny balíky skupiny v pořadí ve skupině. */
    public void exportGroupCsv(ActionEvent actionEvent) {
        if (groupRun == null) {
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Exportovat souhrn skupiny do CSV");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Soubory CSV", "*.csv"));
        chooser.setInitialFileName(groupRun.getTitle() + "-souhrn.csv");
        File parent = groupRun.getGroupDir().getParentFile();
        if (parent != null && parent.isDirectory()) {
            chooser.setInitialDirectory(parent);
        }
        File file = chooser.showSaveDialog(main.getPrimaryStage());
        if (file != null) {
            exportGroupCsvTo(file);
        }
    }

    /** Zapíše souhrn skupiny do souboru; výsledek oznámí ve stavovém řádku. */
    boolean exportGroupCsvTo(File file) {
        try (OutputStream out = new FileOutputStream(file)) {
            GroupCsv.write(groupRun.getItems(), out);
            statusText.setText("Souhrn skupiny uložen do " + file.getAbsolutePath() + ".");
            return true;
        } catch (IOException e) {
            statusText.setText("Souhrn skupiny nelze uložit: " + e.getMessage());
            return false;
        }
    }

    // ----- pro SnapshotMode -----

    boolean isGroupMode() {
        return groupRun != null;
    }

    /** Vybere první balík s daným stavem (jako uživatel), vrátí jeho název nebo null. */
    String selectGroupPackage(PackageStatus status) {
        for (GroupItem item : groupTableModel.shownItems()) {
            if (item.getStatus() == status) {
                groupTable.getSelectionModel().select(item);
                groupTable.scrollTo(item);
                return item.getName();
            }
        }
        return null;
    }

    /** „Validovat znovu jen tento balík“ pro první balík s daným stavem; vrátí jeho název nebo null. */
    String rerunGroupPackage(PackageStatus status) {
        for (GroupItem item : groupRun.getItems()) {
            if (item.getStatus() == status) {
                rerunPackage(item);
                return item.getName();
            }
        }
        return null;
    }

    void setGroupOnlyInvalid(boolean onlyInvalid) {
        groupOnlyInvalid.setSelected(onlyInvalid);
    }

    /** Počty a stavy balíků pro výpis SnapshotMode. */
    String describeGroupItems() {
        StringBuilder text = new StringBuilder();
        for (GroupItem item : groupRun.getItems()) {
            text.append("\n  ").append(item).append(item.getDurationMs() == null ? "" : ", " + item.getDurationMs() + " ms")
                    .append(groupDetails.get(item) != null ? ", detail in memory" : "");
        }
        return text.toString();
    }

    // ===== akce =====

    public void stopValidation(ActionEvent actionEvent) {
        cancelValidation = true;
        if (groupRun != null) {
            stopGroupValidation();
            return;
        }
        if (validationTask != null) {
            boolean cancelled = validationTask.cancel();
            if (cancelled && !validationTaskEntered) {
                //zruseno driv, nez vlakno validace zacalo: telo ulohy se uz nespusti a nikdo jiny by zruseni neohlasil
                onStopped();
            }
        }
    }

    /**
     * „Validovat znovu“ (issue #87): spustí validaci stejného zdroje se stejným požadavkem jako naposledy, bez dialogu.
     */
    public void rerunValidation(ActionEvent actionEvent) {
        if (!rerunState.canRerun()) {
            return;
        }
        main.launchValidation(rerunState.requestForRerun());
    }

    /** Pro SnapshotMode: seznam zobrazený v panelu problémů (kontrola, že se při událostech nepřestavuje). */
    ObservableList<ValidationProblem> getShownProblems() {
        return problemList.getItems();
    }

    boolean isValidationRunning() {
        return totalState == TotalState.RUNNING;
    }

    int getStartedValidations() {
        return startedValidations;
    }

    int getFinishedValidations() {
        return finishedValidations;
    }

    ValidationRequest getLastRequest() {
        return currentRequest;
    }

    /** Nastaví zaškrtávátka filtru (jako uživatel); pro SnapshotMode. */
    void setFilterChecks(boolean errors, boolean warnings, boolean infos, boolean onlyWithProblems) {
        filterErrors.setSelected(errors);
        filterWarnings.setSelected(warnings);
        filterInfos.setSelected(infos);
        filterOnlyWithProblems.setSelected(onlyWithProblems);
    }

    boolean canRerun() {
        return rerunState.canRerun() && !rerunMenuItem.isDisable() && !rerunButton.isDisable();
    }

    /** Popis zobrazeného výsledku pro výpis SnapshotMode. */
    String describeShownResult() {
        return "state=" + totalState
                + (groupRun != null ? ", group=[" + headerSubtitle.getText() + " | badges " + groupBadgesText()
                + " | table " + groupTableModel.shownCount() + "/" + groupTableModel.totalCount()
                + (groupTableModel.isOnlyInvalid() ? " only invalid" : "")
                + " | shown " + (shownItem == null ? "-" : shownItem.getName() + " " + shownItem.getStatus())
                + (packageMessagePane.isVisible() ? " | message " + packageMessage.getText() : "") + "]" : "")
                + (progressPane.isVisible() ? ", progress=[" + progressTitle.getText() + " | " + progressCounts.getText()
                + " | " + progressDetail.getText() + " | " + String.format(Locale.ROOT, "%.2f", progressBar.getProgress()) + "]" : "")
                + (noticeBar.isVisible() ? ", notice=" + noticeText.getText().replace('\n', ' ') : "")
                + ", title=" + headerTitle.getText()
                + ", subtitle=" + headerSubtitle.getText()
                + ", verdict=" + (verdictBadge.isVisible() ? verdictBadge.getText() : "-")
                + ", badges=" + (errorsBadge.isVisible() ? errorsBadge.getText() + " / " + warningsBadge.getText() + " / " + infosBadge.getText() : "-")
                + ", status=" + statusText.getText()
                + ", sections=" + (filteredSections == null ? 0 : filteredSections.size())
                + ", selectedSection=" + (selectedSection == null ? "-" : selectedSection.getName())
                + ", selectedRule=" + (selectedRule == null ? "-" : selectedRule.getName())
                + ", problemsShown=" + (problemList.getItems() == null ? 0 : problemList.getItems().size());
    }

    /** Pro SnapshotMode: háček volaný z vlákna validace (smí ho pozastavit). */
    void setRunHook(RunHook hook) {
        this.runHook = hook;
    }

    /**
     * Háček pro SnapshotMode volaný z vlákna validace: po ohlášení pravidel a po každém dokončeném pravidle (události
     * pro UI jsou už odeslané). Slouží k pozastavení validace v definovaném bodě; v běžném provozu není nastaven.
     */
    interface RunHook {
        void initialized(int totalRules);

        void ruleFinished();

        /** Skupina balíků: začíná validace balíku (pořadí v běhu od 1). */
        default void packageStarted(int position, int total) {
        }
    }

    // ===== dialogy =====

    public void showExternalUtilsCheckDialog(ActionEvent actionEvent) {
        //zavreni krizkem tu aplikaci neukonci ani pri chybejicim nastroji
        main.showExternalUtilsCheckDialog(false, "Zavřít", false);
    }

    public void showSkippingConfigurationDialog(ActionEvent actionEvent) {
        main.showSkippingConfigurationDialog();
    }

    public void showAboutAppDialog(ActionEvent actionEvent) {
        main.showAboutAppDialog();
    }

    public void showNewValidationDialog(ActionEvent actionEvent) {
        main.showNewValidationDialog();
    }

    public void showDictionariesConfigurationDialog(ActionEvent actionEvent) {
        main.showDictionariesConfigurationDialog();
    }

    // ===== odkazy =====

    public void showOnlineHelp(ActionEvent actionEvent) {
        if (ConfigurationManager.DEV_MODE) {
            main.showTestDialog();
        } else {
            openUrl(URL_ONLINE_HELP);
        }
    }

    public void showLogTxt(ActionEvent actionEvent) {
        File file = groupRun != null ? shownOutput(false) : logTxtFile;
        if (file != null) {
            openUrl(file.toURI().toString());
        }
    }

    public void showLogXml(ActionEvent actionEvent) {
        File file = groupRun != null ? shownOutput(true) : logXmlFile;
        if (file != null) {
            openUrl(file.toURI().toString());
        }
    }

    private enum TotalState {
        IDLE, RUNNING, FINISHED, ERROR, STOPPED;
    }
}
