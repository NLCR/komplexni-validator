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
import javafx.stage.Stage;
import javafx.util.Duration;
import nkp.pspValidator.gui.newvalidation.DocumentTypes;
import nkp.pspValidator.gui.newvalidation.ValidationRequest;
import nkp.pspValidator.gui.newvalidation.ValidationSource;
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
    private ValidationStateManager validationStateManager = null;
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
        cancelMenuItem.setAccelerator(cancelAccelerator());
        elapsedTimer = new Timeline(new KeyFrame(Duration.seconds(1), e -> refreshRunningHeader()));
        elapsedTimer.setCycleCount(Timeline.INDEFINITE);
        showCenter(emptyState);
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
                    //DEV
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
        if (progress == null) {
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
    }

    private String buildValidationLogName(File pspDir) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss_");
        Date date = new Date();
        return dateFormat.format(date) + pspDir.getName();
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
            filteredSections = new FilteredList<>(manager.getSectionsObservable(), filter::showSection);
            sectionList.setItems(filteredSections);
            updatePlaceholders();
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
        if (selectedRule != null && selectedRule.getSectionId() == sectionId && selectedRule.getId() == ruleId) {
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
            filteredRules = new FilteredList<>(validationStateManager.getRulesObervable(section.getId()), filter::showRule);
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
            ObservableList<ValidationProblem> problems = validationStateManager.getProblems(rule.getSectionId(), rule.getId());
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
        if (validationStateManager == null) {
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

    // ===== akce =====

    public void stopValidation(ActionEvent actionEvent) {
        cancelValidation = true;
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
        if (logTxtFile != null) {
            openUrl(logTxtFile.toURI().toString());
        }
    }

    public void showLogXml(ActionEvent actionEvent) {
        if (logXmlFile != null) {
            openUrl(logXmlFile.toURI().toString());
        }
    }

    private enum TotalState {
        IDLE, RUNNING, FINISHED, ERROR, STOPPED;
    }
}
