package nkp.pspValidator.gui;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.WindowEvent;
import javafx.util.Duration;
import nkp.pspValidator.gui.group.GroupRecognition;
import nkp.pspValidator.gui.group.GroupSummaries;
import nkp.pspValidator.gui.newvalidation.*;
import nkp.pspValidator.gui.settings.SettingsTab;
import nkp.pspValidator.shared.Dmf;
import nkp.pspValidator.shared.FdmfRegistry;
import nkp.pspValidator.shared.externalUtils.ExternalUtil;
import nkp.pspValidator.shared.externalUtils.ExternalUtilManager;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Kontroler dialogu Nová validace. Jen propojuje ovládací prvky s logikou v balíčku
 * {@code nkp.pspValidator.gui.newvalidation} (rozpoznání balíku, nastavení verzí a výstupů, nástroje, přeskočené
 * sekce, texty souhrnů, sestavení požadavku na validaci), u skupiny balíků s logikou v balíčku {@code gui.group}.
 * Čtení balíku, zjišťování nástrojů a čtení fDMF běží v pracovních vláknech; výsledky starších požadavků se zahazují
 * podle pořadového čísla.
 * <p>
 * Skupina balíků: karta rozpoznání ukazuje počet balíků a rozpad podle typu a verze (rozpoznává se v pracovním vlákně
 * s průběžným stavem, nejvýš {@link GroupRecognition#DEFAULT_LIMIT} balíků), sekce Verze DMF vždy tabulku všech typů,
 * nástroje a přeskočená pravidla se vztahují ke všem fDMF rozpoznaným ve skupině.
 */
public class NewValidationDialogController extends DialogController {

    @FXML
    DialogFrame container;
    @FXML
    ScrollPane scrollPane;
    @FXML
    VBox scrollContent;
    @FXML
    HBox sourceSwitch;
    @FXML
    Label pathLabel;
    @FXML
    TextField pathField;
    @FXML
    Label recognitionTitle;
    @FXML
    Label recognitionBadge;
    @FXML
    ProgressIndicator recognitionProgress;
    @FXML
    Label recognitionDetail;
    @FXML
    VBox sectionsBox;
    @FXML
    CheckBox createXmlLog;
    @FXML
    CheckBox createTxtLog;
    @FXML
    ComboBox<OutputSettings.Verbosity> verbosityComboBox;
    @FXML
    Button validateButton;

    private final ExecutorService worker = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "nova-validace");
        thread.setDaemon(true);
        return thread;
    });
    private final PauseTransition pathDebounce = new PauseTransition(Duration.millis(350));
    private final ToggleGroup sourceGroup = new ToggleGroup();

    private String initialPath;
    private NewValidationDialog.Expand initialExpand;
    private ValidationSource initialSource;

    private FdmfRegistry registry;
    private ExternalUtilManager utilManager;
    private VersionSettings versionSettings;
    private OutputSettings outputSettings;
    private SkippedSections skippedSections;
    private final PackageRecognizer recognizer = new PackageRecognizer();

    private ValidationSource source = ValidationSource.DIRECTORY;
    private Recognition recognition;
    private boolean recognizing = false;
    /** čte ho i pracovní vlákno rozpoznávání skupiny (zda výsledek ještě platí) */
    private volatile long recognitionGeneration = 0;
    /** skupina balíků: výsledek rozpoznání (null během rozpoznávání nebo u jiného zdroje) a průběh */
    private GroupRecognition groupRecognition;
    private int groupRecognized;
    private int groupToRecognize;
    /** skupina balíků: fDMF rozpoznané ve skupině a z nich odvozené přeskočené sekce (hodnota null: fDMF chybí) */
    private Set<Dmf> groupDmfs = Collections.emptySet();
    private Map<Dmf, List<String>> groupSkipped;
    /** fDMF, proti které se bude validovat, a z ní odvozené údaje (null, když není známá) */
    private Dmf effectiveDmf;
    private Set<ExternalUtil> relevantUtils;
    private List<String> skipped;
    private long dmfGeneration = 0;
    /** zobrazit v sekci verzí tabulku všech typů i u rozpoznaného balíku */
    private boolean showAllTypes = false;

    private ToolChecks toolChecks;
    /** stavy nástrojů z {@link #toolChecks} (pohled jen pro čtení) */
    private Map<ExternalUtil, ToolStatus> toolStatuses = Collections.emptyMap();
    private final Set<ExternalUtil> uncheckedByUser = EnumSet.noneOf(ExternalUtil.class);

    private CollapsibleSection versionsSection;
    private CollapsibleSection toolsSection;
    private CollapsibleSection skippedSection;
    private volatile boolean closed = false;

    /**
     * @param initialSource zdroj předvyplněné cesty, null = podle cesty (adresář, u přípony .zip soubor ZIP)
     */
    public void setData(String initialPath, NewValidationDialog.Expand initialExpand, ValidationSource initialSource) {
        this.initialPath = initialPath;
        this.initialExpand = initialExpand;
        this.initialSource = initialSource;
    }

    @Override
    public EventHandler<WindowEvent> getOnCloseEventHandler() {
        return event -> closed = true;
    }

    @Override
    public void startNow() {
        container.getStyleClass().add("dialog-scrolling");
        ConfigurationManager config = getConfigurationManager();
        registry = main.getValidationDataManager().getFdmfRegistry();
        utilManager = main.getValidationDataManager().getExternalUtilManager();
        //dostupnost zapisuje do manazeru i po zavreni dialogu, aby validace spustena pred dobehnutim kontroly znala
        //skutecny stav (u nedostupneho nastroje hlasi INFO)
        toolChecks = new ToolChecks(utilManager, "nova-validace-nastroj", (util, status) -> {
            if (!closed) {
                refreshToolsSection();
            }
        });
        toolStatuses = toolChecks.getStatuses();
        versionSettings = new VersionSettings(config, registry);
        outputSettings = new OutputSettings(config);
        skippedSections = new SkippedSections(config, registry);

        if (initialSource != null) {
            source = initialSource;
        } else if (initialPath != null && initialPath.toLowerCase(Locale.ROOT).endsWith(".zip")) {
            source = ValidationSource.ZIP;
        }
        initSourceSwitch();
        initSections();
        initOutputs();

        pathDebounce.setOnFinished(e -> recognize());
        pathField.textProperty().addListener((obs, oldValue, newValue) -> {
            updateValidateButton();
            invalidateRecognition();
            pathDebounce.playFromStart();
        });
        pathField.setOnAction(e -> {
            pathDebounce.stop();
            recognize();
        });
        if (initialPath != null) {
            pathField.setText(initialPath);
            pathDebounce.stop();
        }
        updateValidateButton();
        recognize();
        checkTools();
        if (initialExpand != null) {
            switch (initialExpand) {
                case VERSIONS:
                    expand(versionsSection);
                    break;
                case TOOLS:
                    expand(toolsSection);
                    break;
                case SKIPPED:
                    expand(skippedSection);
                    break;
            }
        }
    }

    // ===== zdroj a cesta =====

    private void initSourceSwitch() {
        ValidationSource[] sources = ValidationSource.values();
        for (int i = 0; i < sources.length; i++) {
            ValidationSource value = sources[i];
            ToggleButton button = new ToggleButton(value.getLabel());
            button.setUserData(value);
            button.setToggleGroup(sourceGroup);
            button.setMinWidth(Region.USE_PREF_SIZE);
            if (i == 0) {
                button.getStyleClass().add("segment-first");
            }
            if (i == sources.length - 1) {
                button.getStyleClass().add("segment-last");
            }
            button.setSelected(value == source);
            sourceSwitch.getChildren().add(button);
        }
        sourceGroup.selectedToggleProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue == null) {
                //vzdy je vybrany prave jeden zdroj
                oldValue.setSelected(true);
                return;
            }
            source = (ValidationSource) newValue.getUserData();
            pathLabel.setText(source.getPathLabel());
            container.setSubtitle(source.getDialogSubtitle());
            updateValidateButton();
            recognize();
        });
        pathLabel.setText(source.getPathLabel());
        container.setSubtitle(source.getDialogSubtitle());
    }

    public void selectPath(ActionEvent actionEvent) {
        ConfigurationManager config = getConfigurationManager();
        File last = config.getFileOrNull(source.getLastPathKey());
        File initialDir = null;
        if (last != null && last.exists()) {
            File parent = last.getParentFile();
            if (parent != null && parent.exists()) {
                initialDir = parent;
            }
        }
        File selected;
        if (source == ValidationSource.DIRECTORY || source == ValidationSource.GROUP) {
            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setTitle(source == ValidationSource.GROUP ? "Vyberte adresář s balíky" : "Vyberte kořenový adresář balíku");
            if (initialDir != null) {
                chooser.setInitialDirectory(initialDir);
            }
            selected = chooser.showDialog(stage);
        } else {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Vyberte soubor ZIP s balíkem");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Soubory ZIP", "*.zip"));
            if (initialDir != null) {
                chooser.setInitialDirectory(initialDir);
            }
            selected = chooser.showOpenDialog(stage);
        }
        if (selected != null) {
            config.setFile(source.getLastPathKey(), selected);
            try {
                pathField.setText(selected.getCanonicalPath());
            } catch (IOException e) {
                pathField.setText(selected.getAbsolutePath());
            }
            pathDebounce.stop();
            recognize();
        }
    }

    private void updateValidateButton() {
        boolean emptyGroup = source == ValidationSource.GROUP && groupRecognition != null
                && (!groupRecognition.isOk() || groupRecognition.getPackageCount() == 0);
        validateButton.setDisable(source.checkPath(pathField.getText()) != null || emptyGroup);
    }

    // ===== rozpoznani =====

    /**
     * Cesta se změnila: výsledek rozpoznání předchozí cesty i z něj odvozené sekce (verze, nástroje, přeskočené sekce)
     * přestávají platit hned, ne až po prodlevě před novým rozpoznáním. Běžící rozpoznání staré cesty se zahodí.
     */
    private void invalidateRecognition() {
        recognitionGeneration++;
        recognition = null;
        groupRecognition = null;
        groupRecognized = 0;
        groupToRecognize = 0;
        showAllTypes = false;
        recognizing = true;
        updateRecognitionCard();
        refreshVersionsSection();
        reloadDmfData();
    }

    private void recognize() {
        long generation = ++recognitionGeneration;
        ValidationSource currentSource = source;
        String path = pathField.getText();
        String problem = currentSource.checkPath(path);
        groupRecognition = null;
        if (problem != null) {
            recognizing = false;
            applyRecognition(Recognition.invalidPath(problem));
            return;
        }
        if (currentSource == ValidationSource.GROUP) {
            recognizeGroup(generation, path);
            return;
        }
        recognizing = true;
        updateRecognitionCard();
        refreshVersionsSection();
        reloadDmfData();
        worker.submit(() -> {
            Recognition result;
            try {
                result = recognizer.recognize(currentSource, path);
            } catch (RuntimeException e) {
                result = Recognition.notRecognized("Balík nelze přečíst: " + e.getMessage());
            }
            Recognition finalResult = result;
            Platform.runLater(() -> {
                if (generation == recognitionGeneration && !closed) {
                    recognizing = false;
                    applyRecognition(finalResult);
                }
            });
        });
    }

    private void applyRecognition(Recognition result) {
        recognition = result;
        updateRecognitionCard();
        refreshVersionsSection();
        reloadDmfData();
        updateValidateButton();
    }

    /** Skupina balíků: najde balíky a postupně je rozpozná; průběh i výsledek platí jen pro aktuální cestu. */
    private void recognizeGroup(long generation, String path) {
        recognition = null;
        recognizing = true;
        groupRecognized = 0;
        groupToRecognize = 0;
        updateRecognitionCard();
        refreshVersionsSection();
        reloadDmfData();
        updateValidateButton();
        File dir = new File(path.trim());
        worker.submit(() -> {
            GroupRecognition result;
            try {
                result = GroupRecognition.recognize(dir, GroupRecognition.DEFAULT_LIMIT,
                        () -> generation != recognitionGeneration || closed,
                        (done, toRecognize) -> Platform.runLater(() -> {
                            if (generation == recognitionGeneration && !closed && recognizing) {
                                groupRecognized = done;
                                groupToRecognize = toRecognize;
                                updateRecognitionCard();
                            }
                        }));
            } catch (RuntimeException e) {
                result = null;
                e.printStackTrace();
            }
            GroupRecognition finalResult = result;
            Platform.runLater(() -> {
                if (generation == recognitionGeneration && !closed && finalResult != null) {
                    recognizing = false;
                    groupRecognition = finalResult;
                    updateRecognitionCard();
                    refreshVersionsSection();
                    reloadDmfData();
                    updateValidateButton();
                }
            });
        });
    }

    private void updateRecognitionCard() {
        recognitionProgress.setVisible(recognizing);
        recognitionProgress.setManaged(recognizing);
        if (source == ValidationSource.GROUP) {
            updateGroupRecognitionCard();
            return;
        }
        if (recognizing) {
            recognitionTitle.setText("Rozpoznávám balík…");
            setBadge(null);
            recognitionDetail.setText(source == ValidationSource.ZIP
                    ? "Čtu METS a info.xml ze souboru ZIP."
                    : "Čtu METS a info.xml.");
            return;
        }
        if (recognition == null) {
            return;
        }
        recognitionTitle.setText(Summaries.recognitionTitle(recognition));
        recognitionBadge.setWrapText(false);
        recognitionBadge.setMinWidth(Region.USE_COMPUTED_SIZE);
        setBadge(Summaries.recognitionBadge(recognition));
        recognitionDetail.setText(Summaries.recognitionDetail(recognition, versionSettings));
    }

    private void updateGroupRecognitionCard() {
        //rozpad skupiny byva dlouhy: stitek se zalomi (u jednoho baliku zustava na jednom radku)
        recognitionBadge.setWrapText(true);
        recognitionBadge.setMinWidth(0);
        recognitionBadge.setMinHeight(Region.USE_PREF_SIZE);
        if (recognizing) {
            recognitionTitle.setText(GroupSummaries.recognizingTitle(groupRecognized, groupToRecognize));
            setBadge(null);
            recognitionDetail.setText("Hledám podadresáře a soubory ZIP a čtu jejich METS a info.xml.");
        } else if (groupRecognition != null) {
            recognitionTitle.setText(GroupSummaries.title(groupRecognition));
            setBadge(GroupSummaries.breakdown(groupRecognition, versionSettings));
            recognitionDetail.setText(GroupSummaries.detail(groupRecognition));
        } else if (recognition != null) {
            //neplatna cesta
            recognitionTitle.setText("Adresář s balíky není vybrán");
            setBadge(null);
            recognitionDetail.setText(recognition.getMessage());
        }
    }

    private void setBadge(String text) {
        recognitionBadge.setText(text);
        recognitionBadge.setVisible(text != null);
        recognitionBadge.setManaged(text != null);
    }

    /** Znovu určí fDMF, proti které se bude validovat, a v pracovním vlákně načte její nástroje a přeskočené sekce. */
    private void reloadDmfData() {
        long generation = ++dmfGeneration;
        if (source == ValidationSource.GROUP) {
            reloadGroupDmfData(generation);
            return;
        }
        groupDmfs = Collections.emptySet();
        groupSkipped = null;
        Dmf dmf = recognizing ? null : Summaries.effectiveDmf(recognition, versionSettings);
        effectiveDmf = dmf;
        if (dmf == null) {
            relevantUtils = null;
            skipped = null;
            refreshToolsSection();
            refreshSkippedSection();
            return;
        }
        worker.submit(() -> {
            Set<ExternalUtil> relevant = ExternalTools.relevantFor(registry, dmf);
            List<String> skippedNames = skippedSections.skipped(dmf);
            Platform.runLater(() -> {
                if (generation == dmfGeneration && !closed) {
                    relevantUtils = relevant;
                    skipped = skippedNames;
                    refreshToolsSection();
                    refreshSkippedSection();
                }
            });
        });
    }

    /** Skupina: nástroje relevantní pro kteroukoli fDMF ve skupině a přeskočené sekce každé z nich. */
    private void reloadGroupDmfData(long generation) {
        effectiveDmf = null;
        Set<Dmf> dmfs = recognizing ? Collections.<Dmf>emptySet() : GroupSummaries.effectiveDmfs(groupRecognition, versionSettings);
        groupDmfs = dmfs;
        relevantUtils = null;
        skipped = null;
        groupSkipped = null;
        refreshToolsSection();
        refreshSkippedSection();
        if (dmfs.isEmpty()) {
            return;
        }
        worker.submit(() -> {
            Set<ExternalUtil> relevant = EnumSet.noneOf(ExternalUtil.class);
            boolean known = false;
            Map<Dmf, List<String>> skippedByDmf = new LinkedHashMap<>();
            for (Dmf dmf : dmfs) {
                Set<ExternalUtil> utils = ExternalTools.relevantFor(registry, dmf);
                if (utils != null) {
                    relevant.addAll(utils);
                    known = true;
                }
                skippedByDmf.put(dmf, skippedSections.skipped(dmf));
            }
            Set<ExternalUtil> finalRelevant = known ? relevant : null;
            Platform.runLater(() -> {
                if (generation == dmfGeneration && !closed) {
                    relevantUtils = finalRelevant;
                    groupSkipped = skippedByDmf;
                    refreshToolsSection();
                    refreshSkippedSection();
                }
            });
        });
    }

    // ===== sekce =====

    private void initSections() {
        versionsSection = new CollapsibleSection("Verze DMF", "Změnit");
        toolsSection = new CollapsibleSection("Externí nástroje", "Zobrazit");
        skippedSection = new CollapsibleSection("Přeskočená pravidla", "Změnit");
        versionsSection.getStyleClass().add("section-first");
        for (CollapsibleSection section : Arrays.asList(versionsSection, toolsSection, skippedSection)) {
            section.setOnToggle(this::toggleSection);
            sectionsBox.getChildren().add(section);
        }
    }

    /** Rozbalená je vždy nejvýš jedna sekce. */
    private void toggleSection(CollapsibleSection section) {
        if (section.isExpanded()) {
            section.setExpanded(false);
        } else {
            expand(section);
        }
    }

    private void expand(CollapsibleSection section) {
        for (Node node : sectionsBox.getChildren()) {
            ((CollapsibleSection) node).setExpanded(node == section);
        }
        Platform.runLater(() -> scrollIntoView(section));
    }

    /** Posune obsah tak, aby byla rozbalená sekce vidět celá, a pokud se nevejde, aby byla vidět aspoň její hlavička. */
    private void scrollIntoView(CollapsibleSection section) {
        scrollContent.applyCss();
        scrollContent.layout();
        double contentHeight = scrollContent.getBoundsInLocal().getHeight();
        double viewportHeight = scrollPane.getViewportBounds().getHeight();
        if (contentHeight <= viewportHeight || viewportHeight <= 0) {
            return;
        }
        double top = sectionsBox.getBoundsInParent().getMinY() + section.getBoundsInParent().getMinY();
        double bottom = top + section.getBoundsInParent().getHeight();
        double maxScroll = contentHeight - viewportHeight;
        double current = scrollPane.getVvalue() * maxScroll;
        double target = current;
        if (bottom > current + viewportHeight) {
            target = bottom - viewportHeight;
        }
        if (top < target) {
            target = top;
        }
        scrollPane.setVvalue(Math.max(0, Math.min(1, target / maxScroll)));
    }

    // ----- verze DMF -----

    /** Položka výběru verze: null verze znamená „podle balíku“ (bez vynucení). */
    private static class VersionChoice {
        final String version;
        final String label;

        VersionChoice(String version, String label) {
            this.version = version;
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private String versionsSummary() {
        return source == ValidationSource.GROUP ? GroupSummaries.versionsSummary(versionSettings)
                : Summaries.versionsSummary(recognition, versionSettings);
    }

    private void refreshVersionsSection() {
        versionsSection.setSummary(recognizing ? "" : versionsSummary());
        VBox body = versionsSection.getBody();
        body.getChildren().clear();
        if (source == ValidationSource.GROUP) {
            //u skupiny vzdy tabulka vsech typu (i behem rozpoznavani, verze na rozpoznani nezavisi)
            body.getChildren().addAll(allTypesTable(), mutedWrapped("Každý balík se validuje podle svého typu. Vynucená verze platí "
                    + "pro všechny balíky daného typu ve skupině; když balík verzi neuvádí, použije se výchozí verze, kterou lze "
                    + "změnit v Nastavení."), defaultVersionsLink());
            return;
        }
        if (recognizing) {
            return;
        }
        boolean recognized = recognition != null && recognition.isRecognized();
        if (recognized && !showAllTypes) {
            Dmf.Type type = recognition.getType();
            String inPackage = recognition.getVersionInPackage();
            String packageChoice = inPackage != null
                    ? "podle balíku (" + inPackage + ")"
                    : "podle balíku (chybí, použije se " + versionSettings.versionWhenMissing(type) + ")";
            ChoiceBox<VersionChoice> choiceBox = versionChoiceBox(type, packageChoice);
            Label typeLabel = new Label(DocumentTypes.name(type));
            typeLabel.setMinWidth(170);
            Label info = new Label(inPackage != null ? "v balíku uvedena verze " + inPackage : "balík verzi neuvádí");
            info.getStyleClass().add("text-muted");
            HBox row = new HBox(16, typeLabel, choiceBox, info);
            row.setAlignment(Pos.CENTER_LEFT);
            Label note = mutedWrapped("Zobrazuje se jen typ rozpoznaný v balíku. Když balík verzi neuvádí, použije se výchozí verze, kterou lze změnit v Nastavení.");
            Hyperlink allTypes = link("Nastavit verze pro všechny typy dokumentů…", () -> {
                showAllTypes = true;
                refreshVersionsSection();
            });
            body.getChildren().addAll(row, note, new HBox(20, allTypes, defaultVersionsLink()));
        } else {
            body.getChildren().add(allTypesTable());
            if (recognized) {
                body.getChildren().add(link("Zobrazit jen rozpoznaný typ", () -> {
                    showAllTypes = false;
                    refreshVersionsSection();
                }));
            } else {
                body.getChildren().add(mutedWrapped("Typ balíku zatím není rozpoznán, proto se zobrazují všechny typy dokumentů. Vynucená verze platí pro všechny balíky daného typu."));
            }
        }
    }

    /** Odkaz do Nastavení na záložku Výchozí verze DMF (preferované verze se nastavují tam). */
    private Hyperlink defaultVersionsLink() {
        return link("Změnit výchozí verze…", () -> showSettings(SettingsTab.VERSIONS));
    }

    private GridPane allTypesTable() {
        //bez mezery mezi sloupci, aby zahlavi tvorilo jeden pruh; odsazeni obstaravaji okraje bunek
        GridPane grid = new GridPane();
        grid.setVgap(6);
        ColumnConstraints nameColumn = new ColumnConstraints();
        nameColumn.setMinWidth(220);
        nameColumn.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(nameColumn, new ColumnConstraints());
        Label typeHeader = new Label("Typ dokumentu");
        Label forcedHeader = new Label("Vynutit verzi");
        HBox header = new HBox(typeHeader);
        header.getStyleClass().add("table-header");
        typeHeader.getStyleClass().add("text-bold");
        forcedHeader.getStyleClass().add("text-bold");
        HBox forcedHeaderBox = new HBox(forcedHeader);
        forcedHeaderBox.getStyleClass().add("table-header");
        header.setMaxWidth(Double.MAX_VALUE);
        forcedHeaderBox.setMaxWidth(Double.MAX_VALUE);
        grid.add(header, 0, 0);
        grid.add(forcedHeaderBox, 1, 0);
        int row = 1;
        for (DocumentTypes.Group group : DocumentTypes.Group.values()) {
            Label groupLabel = new Label(group.getLabel().toUpperCase(new Locale("cs")));
            groupLabel.getStyleClass().add("table-group-label");
            grid.add(groupLabel, 0, row++, 2, 1);
            for (Dmf.Type type : DocumentTypes.typesOf(group)) {
                Label name = new Label(DocumentTypes.name(type));
                name.setPadding(new Insets(0, 0, 0, 10));
                grid.add(name, 0, row);
                ChoiceBox<VersionChoice> choiceBox = versionChoiceBox(type, "nevynucovat");
                GridPane.setMargin(choiceBox, new Insets(0, 0, 0, 10));
                grid.add(choiceBox, 1, row);
                row++;
            }
        }
        return grid;
    }

    private ChoiceBox<VersionChoice> versionChoiceBox(Dmf.Type type, String notForcedLabel) {
        ChoiceBox<VersionChoice> choiceBox = new ChoiceBox<>();
        VersionChoice notForced = new VersionChoice(null, notForcedLabel);
        choiceBox.getItems().add(notForced);
        String forced = versionSettings.forcedVersion(type);
        VersionChoice selected = notForced;
        for (String version : versionSettings.availableVersions(type)) {
            VersionChoice choice = new VersionChoice(version, "vynutit " + version);
            choiceBox.getItems().add(choice);
            if (version.equals(forced)) {
                selected = choice;
            }
        }
        choiceBox.setValue(selected);
        choiceBox.setPrefWidth(250);
        choiceBox.valueProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue != null) {
                versionSettings.setForcedVersion(type, newValue.version);
                versionsSection.setSummary(versionsSummary());
                updateRecognitionCard();
                reloadDmfData();
            }
        });
        return choiceBox;
    }

    // ----- externi nastroje -----

    private void checkTools() {
        toolChecks.checkAll();
        refreshToolsSection();
    }

    private void refreshToolsSection() {
        boolean group = source == ValidationSource.GROUP;
        String typeName = group ? (groupDmfs.isEmpty() ? null : "skupinu")
                : effectiveDmf == null ? null : DocumentTypes.name(effectiveDmf.getType());
        toolsSection.setSummary(Summaries.toolsSummary(typeName, relevantUtils, toolStatuses, uncheckedByUser));
        VBox body = toolsSection.getBody();
        body.getChildren().clear();

        GridPane grid = new GridPane();
        grid.setHgap(14);
        grid.setVgap(6);
        ColumnConstraints nameColumn = new ColumnConstraints();
        nameColumn.setMinWidth(105);
        ColumnConstraints descriptionColumn = new ColumnConstraints();
        descriptionColumn.setPrefWidth(250);
        ColumnConstraints statusColumn = new ColumnConstraints();
        statusColumn.setMinWidth(150);
        statusColumn.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(new ColumnConstraints(), nameColumn, descriptionColumn, statusColumn);

        List<ExternalUtil> unavailableRelevant = new ArrayList<>();
        int row = 0;
        for (ExternalUtil util : ExternalUtil.values()) {
            ToolStatus status = toolStatuses.getOrDefault(util, ToolStatus.checking());
            boolean unused = relevantUtils != null && !relevantUtils.contains(util);
            boolean unavailable = status.getState() == ToolStatus.State.NOT_FOUND || status.getState() == ToolStatus.State.ERROR;
            if (relevantUtils != null && !unused && unavailable) {
                unavailableRelevant.add(util);
            }

            CheckBox use = new CheckBox();
            use.setDisable(unused || unavailable);
            use.setSelected(!unused && !unavailable && !uncheckedByUser.contains(util));
            use.setTooltip(new Tooltip("Použít nástroj pro tuto validaci"));
            use.selectedProperty().addListener((obs, oldValue, newValue) -> {
                if (newValue) {
                    uncheckedByUser.remove(util);
                } else {
                    uncheckedByUser.add(util);
                }
                toolsSection.setSummary(Summaries.toolsSummary(typeName, relevantUtils, toolStatuses, uncheckedByUser));
            });
            Label name = new Label(util.getUserFriendlyName());
            name.getStyleClass().add("text-bold");
            Label description = new Label(ExternalTools.description(util)
                    + (unused ? group ? " (ve skupině se nepoužije)" : " (u tohoto typu se nepoužije)" : ""));
            description.setMinWidth(0);
            description.setTooltip(new Tooltip(description.getText()));
            Region dot = new Region();
            dot.getStyleClass().add(statusDotClass(status, unused));
            Label statusText = new Label(ToolsOverview.statusText(util, status));
            statusText.setMinWidth(0);
            //verze bývá dlouhá (např. ImageMagick vypisuje i architekturu), celá je v tooltipu
            statusText.setTooltip(new Tooltip(status.getDetail() != null ? status.getDetail() : status.getText()));
            HBox statusBox = new HBox(8, dot, statusText);
            statusBox.setAlignment(Pos.CENTER_LEFT);

            for (Node node : Arrays.asList(use, name, description, statusBox)) {
                if (unused) {
                    node.getStyleClass().add("row-unused");
                }
            }
            grid.addRow(row++, use, name, description, statusBox);
        }
        body.getChildren().add(grid);

        if (!unavailableRelevant.isEmpty()) {
            Label notice = new Label(unavailableNotice(unavailableRelevant));
            notice.getStyleClass().add("notice-warning");
            notice.setWrapText(true);
            notice.setMinHeight(Region.USE_PREF_SIZE);
            notice.setMaxWidth(Double.MAX_VALUE);
            body.getChildren().add(notice);
        }
        if (relevantUtils == null) {
            body.getChildren().add(mutedWrapped(group
                    ? "Které nástroje se použijí, závisí na typech balíků. Zrušením zaškrtnutí se nástroj vypne pro validaci celé skupiny."
                    : "Které nástroje se použijí, závisí na typu balíku. Zrušením zaškrtnutí se nástroj vypne jen pro tuto validaci."));
        } else if (group) {
            body.getChildren().add(mutedWrapped("Relevantní jsou nástroje všech typů rozpoznaných ve skupině. Zrušením zaškrtnutí se nástroj "
                    + "vypne pro validaci celé skupiny, protokoly to uvedou."));
        } else {
            body.getChildren().add(mutedWrapped("Zrušením zaškrtnutí se nástroj vypne jen pro tuto validaci, protokol to uvede."));
        }
        HBox links = new HBox(20,
                link("Nastavit cesty k nástrojům…", () -> showSettings(SettingsTab.TOOLS)),
                link("Zkontrolovat znovu", this::checkTools));
        body.getChildren().add(links);
    }

    private static String statusDotClass(ToolStatus status, boolean unused) {
        if (unused || status.getState() == ToolStatus.State.CHECKING) {
            return "status-dot-none";
        }
        return status.isAvailable() ? "status-dot-ok" : "status-dot-error";
    }

    private String unavailableNotice(List<ExternalUtil> utils) {
        if (utils.size() == 1) {
            ExternalUtil util = utils.get(0);
            String what = toolStatuses.get(util).getState() == ToolStatus.State.ERROR
                    ? util.getUserFriendlyName() + " se nepodařilo spustit."
                    : util.getUserFriendlyName() + " nebyl nalezen.";
            return what + " Kontroly tímto nástrojem se přeskočí a protokol to uvede.";
        }
        StringBuilder names = new StringBuilder();
        for (ExternalUtil util : utils) {
            names.append(names.length() == 0 ? "" : ", ").append(util.getUserFriendlyName());
        }
        return "Nedostupné nástroje: " + names + ". Kontroly těmito nástroji se přeskočí a protokol to uvede.";
    }

    // ----- preskocena pravidla -----

    private void refreshSkippedSection() {
        boolean group = source == ValidationSource.GROUP;
        //fDMF a jejich preskocene sekce (u jednoho balíku nejvys jedna fDMF)
        Map<Dmf, List<String>> byDmf = new LinkedHashMap<>();
        if (group) {
            if (groupSkipped != null) {
                byDmf.putAll(groupSkipped);
            }
            skippedSection.setSummary(recognizing ? "" : GroupSummaries.skippedSummary(groupDmfs.isEmpty() ? null
                    : groupSkipped == null ? Collections.<Dmf, List<String>>emptyMap() : groupSkipped));
        } else {
            if (effectiveDmf != null) {
                byDmf.put(effectiveDmf, skipped);
            }
            skippedSection.setSummary(recognizing ? "" : Summaries.skippedSummary(effectiveDmf, skipped));
        }
        VBox body = skippedSection.getBody();
        body.getChildren().clear();
        if (byDmf.isEmpty()) {
            body.getChildren().add(mutedWrapped(group
                    ? "Přeskakované sekce se nastavují pro typ a verzi DMF, zobrazí se po rozpoznání balíků."
                    : "Přeskakované sekce se nastavují pro typ a verzi DMF, zobrazí se po rozpoznání balíku."));
        }
        boolean anySkipped = false;
        for (Map.Entry<Dmf, List<String>> entry : byDmf.entrySet()) {
            Dmf dmf = entry.getKey();
            List<String> dmfSkipped = entry.getValue();
            if (dmfSkipped == null) {
                body.getChildren().add(mutedWrapped("Pro " + DocumentTypes.name(dmf.getType()) + " " + dmf.getVersion() + " není k dispozici fDMF."));
                continue;
            }
            anySkipped |= !dmfSkipped.isEmpty();
            if (group && dmfSkipped.isEmpty()) {
                body.getChildren().add(mutedWrapped(DocumentTypes.name(dmf.getType()) + " " + dmf.getVersion() + ": nic se nepřeskakuje."));
                continue;
            }
            body.getChildren().add(mutedWrapped(group
                    ? DocumentTypes.name(dmf.getType()) + " " + dmf.getVersion() + ": přeskočené sekce se nevalidují."
                    : "Platí pro " + DocumentTypes.name(dmf.getType()) + " " + dmf.getVersion()
                    + ". Přeskočené sekce se nevalidují. Přeskakovat lze jen celé sekce."));
            if (!dmfSkipped.isEmpty()) {
                body.getChildren().add(skippedList(dmf, dmfSkipped));
            }
        }
        HBox links = new HBox(20, link("Vybrat sekce k přeskočení…", () -> showSettings(SettingsTab.SKIPPING)));
        if (anySkipped) {
            links.getChildren().add(link("Zrušit všechna", () -> {
                for (Map.Entry<Dmf, List<String>> entry : byDmf.entrySet()) {
                    if (entry.getValue() != null && !entry.getValue().isEmpty()) {
                        skippedSections.unskipAll(entry.getKey());
                    }
                }
                reloadDmfData();
            }));
        }
        body.getChildren().add(links);
    }

    private VBox skippedList(Dmf dmf, List<String> dmfSkipped) {
        VBox list = new VBox();
        list.getStyleClass().add("list-box");
        for (int i = 0; i < dmfSkipped.size(); i++) {
            String sectionName = dmfSkipped.get(i);
            Label label = new Label("Celá sekce: " + sectionName);
            label.getStyleClass().add("text-bold");
            label.setMinWidth(0);
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            Button cancel = new Button("Zrušit");
            cancel.getStyleClass().add("button-secondary");
            cancel.setMinWidth(Region.USE_PREF_SIZE);
            cancel.setOnAction(e -> {
                skippedSections.unskip(dmf, sectionName);
                reloadDmfData();
            });
            HBox row = new HBox(label, spacer, cancel);
            row.getStyleClass().add("list-row");
            if (i == 0) {
                row.getStyleClass().add("row-first");
            }
            list.getChildren().add(row);
        }
        return list;
    }

    // ===== nastaveni =====

    /**
     * Otevře Nastavení na záložce (nad tímto dialogem) a po jeho zavření dialog obnoví: stav nástrojů, výchozí verze
     * (karta rozpoznání, sekce verzí a z verze odvozená fDMF), přeskočené sekce a výstupy.
     */
    private void showSettings(SettingsTab tab) {
        Dmf dmf = effectiveDmf;
        if (dmf == null && !groupDmfs.isEmpty()) {
            dmf = groupDmfs.iterator().next();
        }
        main.showSettingsDialogOverDialog(tab, dmf);
        if (closed) {
            return;
        }
        checkTools();
        updateRecognitionCard();
        refreshVersionsSection();
        reloadDmfData();
        reloadOutputs();
    }

    // ===== vystupy =====

    /** Výstupy znovu z nastavení (mohly se změnit v Nastavení, záložka Adresáře a protokoly). */
    private void reloadOutputs() {
        createXmlLog.setSelected(outputSettings.isCreateXmlLog());
        createTxtLog.setSelected(outputSettings.isCreateTxtLog());
        verbosityComboBox.setValue(outputSettings.getVerbosity());
        verbosityComboBox.setDisable(!createTxtLog.isSelected());
    }

    private void initOutputs() {
        createXmlLog.setSelected(outputSettings.isCreateXmlLog());
        createTxtLog.setSelected(outputSettings.isCreateTxtLog());
        createXmlLog.selectedProperty().addListener((obs, oldValue, newValue) -> outputSettings.setCreateXmlLog(newValue));
        createTxtLog.selectedProperty().addListener((obs, oldValue, newValue) -> {
            outputSettings.setCreateTxtLog(newValue);
            verbosityComboBox.setDisable(!newValue);
        });
        verbosityComboBox.getItems().setAll(OutputSettings.Verbosity.menuOrder());
        verbosityComboBox.setCellFactory(list -> new ListCell<OutputSettings.Verbosity>() {
            @Override
            protected void updateItem(OutputSettings.Verbosity item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getLabel());
            }
        });
        verbosityComboBox.setValue(outputSettings.getVerbosity());
        verbosityComboBox.setDisable(!createTxtLog.isSelected());
        verbosityComboBox.valueProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue != null) {
                outputSettings.setVerbosity(newValue);
            }
        });
    }

    // ===== paticka =====

    public void closeDialog(ActionEvent actionEvent) {
        closed = true;
        stage.close();
    }

    public void validate(ActionEvent actionEvent) {
        String path = pathField.getText();
        if (source.checkPath(path) != null) {
            updateValidateButton();
            return;
        }
        ValidationRequest request = ValidationRequest.create(source, path, versionSettings, outputSettings,
                ExternalTools.toDisable(uncheckedByUser, toolStatuses));
        if (source == ValidationSource.DIRECTORY || source == ValidationSource.GROUP) {
            //stejne jako drivejsi dialog adresare; u ZIPu dialog zustava pod dialogem rozbalovani (pri chybe se k nemu lze vratit)
            closed = true;
            stage.hide();
        }
        main.launchValidation(request);
    }

    // ===== pomocne =====

    private static Label mutedWrapped(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("text-muted");
        label.setWrapText(true);
        label.setMinHeight(Region.USE_PREF_SIZE);
        return label;
    }

    private static Hyperlink link(String text, Runnable action) {
        Hyperlink link = new Hyperlink(text);
        link.getStyleClass().add("link");
        link.setMinWidth(Region.USE_PREF_SIZE);
        link.setOnAction(e -> action.run());
        return link;
    }
}
