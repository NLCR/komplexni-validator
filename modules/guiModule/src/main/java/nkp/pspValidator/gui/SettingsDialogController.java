package nkp.pspValidator.gui;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.WindowEvent;
import nkp.pspValidator.gui.newvalidation.DocumentTypes;
import nkp.pspValidator.gui.newvalidation.ExternalTools;
import nkp.pspValidator.gui.newvalidation.OutputSettings;
import nkp.pspValidator.gui.newvalidation.ToolsOverview;
import nkp.pspValidator.gui.newvalidation.VersionSettings;
import nkp.pspValidator.gui.settings.*;
import nkp.pspValidator.gui.skipping.SkippedManagerImpl;
import nkp.pspValidator.shared.Dmf;
import nkp.pspValidator.shared.engine.RulesSection;
import nkp.pspValidator.shared.externalUtils.ExternalUtil;
import nkp.pspValidator.shared.metadataProfile.DictionaryManager;

import java.io.File;
import java.util.*;
import java.util.function.Consumer;

/**
 * Kontroler okna Nastavení: seznam záložek vlevo, obsah vybrané záložky vpravo (vytváří se při prvním zobrazení).
 * Jen propojuje ovládací prvky s logikou v balíčku {@code nkp.pspValidator.gui.settings}.
 * <p>
 * Ukládání: všechna nastavení (výchozí verze DMF, cesty k externím nástrojům, přeskakovaná pravidla, adresáře
 * a protokoly) se drží v konceptu a uloží se až tlačítkem Uložit. Zavření okna s neuloženými změnami (Zrušit, Esc,
 * křížek) se zeptá „Uložit změny?“ ({@link CloseDecision}). Nová cesta k nástroji platí pro kontrolu hned
 * ({@link ToolPathsDraft}), při zahození změn se vrátí uložená a dotčené nástroje se zkontrolují znovu. Akce se
 * provedou hned a nejsou neuloženou změnou: kontrola nástrojů, aktualizace slovníku (vlastní dialog), otevření adresáře.
 */
public class SettingsDialogController extends DialogController {

    @FXML
    DialogFrame container;
    @FXML
    VBox nav;
    @FXML
    StackPane pageHolder;
    @FXML
    Button cancelButton;
    @FXML
    Button saveButton;

    private SettingsTab initialTab;
    private Dmf focusDmf;

    private final Map<SettingsTab, Label> navItems = new EnumMap<>(SettingsTab.class);
    private final Map<SettingsTab, Node> pages = new EnumMap<>(SettingsTab.class);
    private SettingsTab shownTab;

    private VersionSettings versionSettings;
    private DefaultVersions defaultVersions;
    private DirsDraft dirsDraft;
    /** null, dokud se sekce fDMF nenačtou (načítají se v pracovním vlákně při prvním zobrazení záložky) */
    private SkippingDraft skippingDraft;
    private ToolsTable toolsTable;
    /** cesty k nástrojům (neuložené platí jen pro kontrolu, do nastavení se zapíší tlačítkem Uložit) */
    private ToolPathsDraft toolPaths;
    /** otevřený dotaz „Uložit změny?“, jinak null */
    private Stage savePrompt;
    /** výběry výchozích verzí v záložce (po jejím prvním zobrazení) */
    private final Map<Dmf.Type, ChoiceBox<DefaultVersions.Choice>> versionBoxes = new EnumMap<>(Dmf.Type.class);
    private volatile boolean closed = false;

    public void setData(SettingsTab initialTab, Dmf focusDmf) {
        this.initialTab = initialTab;
        this.focusDmf = focusDmf;
    }

    @Override
    public EventHandler<WindowEvent> getOnCloseEventHandler() {
        //krizek: jako Zrusit (s neulozenymi zmenami dotaz)
        return event -> {
            event.consume();
            requestClose();
        };
    }

    @Override
    public void startNow() {
        container.getStyleClass().add("settings-frame");
        versionSettings = new VersionSettings(getConfigurationManager(), main.getValidationDataManager().getFdmfRegistry());
        defaultVersions = new DefaultVersions(versionSettings);
        dirsDraft = new DirsDraft(getConfigurationManager());
        toolPaths = ToolPathsDraft.deferred(getConfigurationManager(), main.getValidationDataManager().getExternalUtilManager()::setPath);
        for (SettingsTab tab : SettingsTab.values()) {
            Label item = new Label(tab.getLabel());
            item.getStyleClass().add("settings-nav-item");
            item.setMaxWidth(Double.MAX_VALUE);
            item.addEventHandler(MouseEvent.MOUSE_CLICKED, e -> showTab(tab));
            navItems.put(tab, item);
            nav.getChildren().add(item);
        }
        showTab(initialTab != null ? initialTab : SettingsTab.VERSIONS);
        updateFooter();
    }

    /** Zobrazí záložku (pro snímky i zvenku). */
    public void showTab(SettingsTab tab) {
        shownTab = tab;
        for (Map.Entry<SettingsTab, Label> entry : navItems.entrySet()) {
            entry.getValue().getStyleClass().remove("settings-nav-item-selected");
            if (entry.getKey() == tab) {
                entry.getValue().getStyleClass().add("settings-nav-item-selected");
            }
        }
        Node page = pages.get(tab);
        if (page == null) {
            page = buildPage(tab);
            pages.put(tab, page);
        }
        pageHolder.getChildren().setAll(page);
    }

    public SettingsTab getShownTab() {
        return shownTab;
    }

    private Node buildPage(SettingsTab tab) {
        switch (tab) {
            case VERSIONS:
                return buildVersionsPage();
            case TOOLS:
                return buildToolsPage();
            case SKIPPING:
                return buildSkippingPage();
            case DICTIONARIES:
                return buildDictionariesPage();
            case DIRS:
                return buildDirsPage();
            default:
                throw new IllegalStateException("neznámá záložka " + tab);
        }
    }

    /** Stránka záložky: posuvný obsah s odsazením. */
    private static ScrollPane page(VBox content) {
        content.getStyleClass().add("settings-page-content");
        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.getStyleClass().add("dialog-scroll");
        return scroll;
    }

    // ===== patička =====

    /** Části nastavení s neuloženými změnami (v pořadí záložek). */
    private List<SettingsTexts.Part> changedParts() {
        List<SettingsTexts.Part> changed = new ArrayList<>();
        if (defaultVersions.isChanged()) {
            changed.add(SettingsTexts.Part.VERSIONS);
        }
        if (toolPaths.isChanged()) {
            changed.add(SettingsTexts.Part.TOOLS);
        }
        if (skippingDraft != null && skippingDraft.isChanged()) {
            changed.add(SettingsTexts.Part.SKIPPING);
        }
        if (dirsDraft.isChanged()) {
            changed.add(SettingsTexts.Part.DIRS);
        }
        return changed;
    }

    private void updateFooter() {
        List<SettingsTexts.Part> changed = changedParts();
        String note = SettingsTexts.unsavedNote(changed);
        container.setFooterNote(note == null ? "" : note);
        saveButton.setDisable(changed.isEmpty());
        saveButton.setDefaultButton(!changed.isEmpty());
        cancelButton.setText(changed.isEmpty() ? "Zavřít" : "Zrušit");
    }

    /** Tlačítko Zrušit/Zavřít a Esc: jako křížek. */
    public void cancel(ActionEvent actionEvent) {
        requestClose();
    }

    public void save(ActionEvent actionEvent) {
        saveChanges();
        closeWindow();
    }

    /** Zavření okna: bez neuložených změn hned, jinak dotaz „Uložit změny?“. */
    private void requestClose() {
        if (savePrompt != null) {
            return;
        }
        apply(CloseDecision.onCloseRequest(!changedParts().isEmpty()));
    }

    private void apply(CloseDecision.Action action) {
        switch (action) {
            case CLOSE:
                closeWindow();
                break;
            case ASK:
                savePrompt = openSavePrompt(choice -> {
                    savePrompt = null;
                    apply(CloseDecision.afterPrompt(choice));
                });
                break;
            case SAVE_AND_CLOSE:
                saveChanges();
                closeWindow();
                break;
            case DISCARD_AND_CLOSE:
                discardChanges();
                closeWindow();
                break;
            case STAY:
                break;
        }
    }

    private void saveChanges() {
        if (defaultVersions.isChanged()) {
            defaultVersions.save();
        }
        if (toolPaths.isChanged()) {
            toolPaths.save();
        }
        if (skippingDraft != null && skippingDraft.isChanged()) {
            skippingDraft.save();
        }
        if (dirsDraft.isChanged()) {
            dirsDraft.save();
        }
    }

    /**
     * Zahodí neuložené změny. Koncepty verzí, přeskakování a adresářů se jen zapomenou, cesty k nástrojům se vrátí
     * manažeru nástrojů na uložené a dotčené nástroje se zkontrolují znovu (výsledek dřívější kontroly s neuloženou
     * cestou se zahodí, viz {@code ToolChecks}).
     */
    private void discardChanges() {
        Set<ExternalUtil> reverted = toolPaths.discard();
        if (!reverted.isEmpty() && toolsTable != null) {
            toolsTable.recheck(reverted);
        }
    }

    private void closeWindow() {
        closed = true;
        stage.close();
    }

    /**
     * Dotaz „Uložit změny?“ nad oknem Nastavení (modální, neblokuje volajícího). Zpět, Esc i křížek dotazu nechají
     * Nastavení otevřené.
     *
     * @param onChoice volba (null = dotaz zavřen bez volby)
     */
    private Stage openSavePrompt(Consumer<CloseDecision.Choice> onChoice) {
        Stage prompt = new Stage(StageStyle.UTILITY);
        prompt.initOwner(stage);
        prompt.initModality(Modality.APPLICATION_MODAL);
        prompt.setTitle("Nastavení");
        prompt.setResizable(false);
        CloseDecision.Choice[] chosen = new CloseDecision.Choice[1];

        DialogFrame frame = new DialogFrame();
        frame.setTitle("Uložit změny?");
        frame.setSubtitle(SettingsTexts.unsavedNote(changedParts()));
        frame.getContent().add(wrapped("Neukládat: změny se zahodí a okno se zavře. Zpět: Nastavení zůstane otevřené.", "text-muted"));
        Button back = new Button("Zpět");
        back.getStyleClass().add("button-secondary");
        back.setMinWidth(90);
        back.setCancelButton(true);
        Button discard = new Button("Neukládat");
        discard.getStyleClass().add("button-secondary");
        discard.setMinWidth(Region.USE_PREF_SIZE);
        Button saveNow = new Button("Uložit");
        saveNow.getStyleClass().add("button-primary");
        saveNow.setMinWidth(90);
        saveNow.setDefaultButton(true);
        back.setOnAction(e -> {
            chosen[0] = CloseDecision.Choice.BACK;
            prompt.close();
        });
        discard.setOnAction(e -> {
            chosen[0] = CloseDecision.Choice.DISCARD;
            prompt.close();
        });
        saveNow.setOnAction(e -> {
            chosen[0] = CloseDecision.Choice.SAVE;
            prompt.close();
        });
        frame.getFooterButtons().addAll(back, discard, saveNow);
        //volba se vyhodnoti az po zavreni dotazu (tlacitkem i krizkem)
        prompt.setOnHidden(e -> onChoice.accept(chosen[0]));

        Scene scene = new Scene(frame, SAVE_PROMPT_WIDTH, SAVE_PROMPT_HEIGHT);
        AbstractDialog.addBaseStylesheet(scene);
        prompt.setScene(scene);
        prompt.show();
        saveNow.requestFocus();
        return prompt;
    }

    /** velikost dotazu „Uložit změny?“ */
    private static final double SAVE_PROMPT_WIDTH = 520;
    private static final double SAVE_PROMPT_HEIGHT = 220;

    // ===== výchozí verze DMF =====

    private Node buildVersionsPage() {
        VBox content = new VBox();
        content.getChildren().add(wrapped("Verze, proti které se balík validuje, když ji sám neuvádí. Balík s uvedenou verzí "
                + "se validuje podle ní, pokud ji v dialogu validace nevynutíte.", "text-muted"));

        GridPane grid = new GridPane();
        grid.getStyleClass().add("settings-table");
        ColumnConstraints nameColumn = new ColumnConstraints();
        nameColumn.setHgrow(Priority.ALWAYS);
        nameColumn.setMinWidth(150);
        ColumnConstraints choiceColumn = new ColumnConstraints();
        choiceColumn.setMinWidth(200);
        ColumnConstraints usedColumn = new ColumnConstraints();
        usedColumn.setMinWidth(90);
        grid.getColumnConstraints().addAll(nameColumn, choiceColumn, usedColumn);
        grid.add(headerCell("Typ dokumentu"), 0, 0);
        grid.add(headerCell("Výchozí verze"), 1, 0);
        grid.add(headerCell("Použije se"), 2, 0);

        Map<Dmf.Type, ChoiceBox<DefaultVersions.Choice>> boxes = versionBoxes;
        Map<Dmf.Type, Label> usedLabels = new EnumMap<>(Dmf.Type.class);
        Map<Dmf.Type, Label> notes = new EnumMap<>(Dmf.Type.class);
        int row = 1;
        for (DocumentTypes.Group group : DocumentTypes.Group.values()) {
            Label groupLabel = new Label(group.getLabel().toUpperCase(new Locale("cs")));
            groupLabel.getStyleClass().addAll("table-group-label", "settings-group-label");
            grid.add(groupLabel, 0, row++, 3, 1);
            for (Dmf.Type type : DocumentTypes.typesOf(group)) {
                Label name = new Label(DocumentTypes.name(type));
                name.getStyleClass().add("settings-cell");
                grid.add(name, 0, row);

                ChoiceBox<DefaultVersions.Choice> box = new ChoiceBox<>();
                box.getItems().setAll(defaultVersions.choices(type));
                box.setValue(defaultVersions.selected(type));
                box.setPrefWidth(190);
                box.setDisable(!defaultVersions.hasVersions(type));
                GridPane.setMargin(box, new Insets(4, 10, 4, 10));
                grid.add(box, 1, row);
                boxes.put(type, box);

                Label used = new Label();
                used.getStyleClass().add("settings-cell");
                grid.add(used, 2, row);
                usedLabels.put(type, used);
                row++;

                Label note = wrapped("", "settings-note");
                GridPane.setMargin(note, new Insets(0, 10, 4, 10));
                grid.add(note, 0, row++, 3, 1);
                notes.put(type, note);

                box.valueProperty().addListener((obs, oldValue, newValue) -> {
                    if (newValue != null && !newValue.equals(defaultVersions.selected(type))) {
                        defaultVersions.select(type, newValue.version);
                    }
                    refreshVersionRow(type, usedLabels.get(type), notes.get(type));
                    updateFooter();
                });
                refreshVersionRow(type, used, note);
            }
        }
        VBox table = new VBox(grid);
        table.getStyleClass().add("list-box");
        content.getChildren().add(table);
        content.getChildren().add(link("Vrátit výchozí hodnoty validátoru", () -> {
            defaultVersions.resetAll();
            for (Map.Entry<Dmf.Type, ChoiceBox<DefaultVersions.Choice>> entry : boxes.entrySet()) {
                entry.getValue().setValue(defaultVersions.selected(entry.getKey()));
                refreshVersionRow(entry.getKey(), usedLabels.get(entry.getKey()), notes.get(entry.getKey()));
            }
            updateFooter();
        }));
        return page(content);
    }

    private void refreshVersionRow(Dmf.Type type, Label used, Label note) {
        used.setText(defaultVersions.usedVersion(type));
        String text = defaultVersions.note(type);
        note.setText(text);
        note.setVisible(text != null);
        note.setManaged(text != null);
    }

    // ===== externí nástroje =====

    private Node buildToolsPage() {
        VBox content = new VBox();
        content.getChildren().add(wrapped("Nástroje jsou volitelné. Bez nástroje se kontroly souborů, které ho potřebují, "
                + "přeskočí a protokol to uvede. Nastavená cesta k nástroji se hned zkontroluje, uloží se tlačítkem Uložit.", "text-muted"));
        toolsTable = new ToolsTable();
        Label summary = new Label();
        summary.getStyleClass().add("text-muted");
        summary.setMinWidth(0);
        Button recheck = new Button("Zkontrolovat znovu");
        recheck.getStyleClass().add("button-secondary");
        recheck.setMinWidth(Region.USE_PREF_SIZE);
        recheck.setOnAction(e -> toolsTable.checkAll());
        Runnable onChange = () -> {
            summary.setText(ToolsOverview.summary(toolsTable.getStatuses()));
            ToolsOverview.State state = toolsTable.getState();
            recheck.setDisable(state == ToolsOverview.State.CHECKING);
            if (state == ToolsOverview.State.ALL_AVAILABLE) {
                //jako dialog nastroju: pri startu se pak dialog uz neukaze
                getConfigurationManager().setBoolean(ConfigurationManager.PROP_EXTERNAL_TOOLS_CHECK_SHOWN, true);
            }
            //nastavena nebo zrusena cesta je neulozena zmena
            if (!closed) {
                updateFooter();
            }
        };
        toolsTable.init(getConfigurationManager(), main.getValidationDataManager().getExternalUtilManager(), toolPaths,
                () -> stage, this::openUrl, onChange, true, "nastaveni-nastroje");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox bottom = new HBox(10, summary, spacer, Links.markExternal(link("Návod k instalaci nástrojů", () -> openUrl(ExternalTools.INSTALLATION_HELP_URL))), recheck);
        bottom.setAlignment(Pos.CENTER_LEFT);
        content.getChildren().addAll(toolsTable, bottom);
        return page(content);
    }

    // ===== přeskakovaná pravidla =====

    private Node buildSkippingPage() {
        VBox content = new VBox();
        content.getChildren().add(wrapped("Zaškrtnuté sekce se při validaci přeskočí a protokol je uvede jako přeskočené. "
                + "Každá verze DMF má vlastní seznam sekcí.", "text-muted"));
        ProgressIndicator progress = new ProgressIndicator();
        progress.setMaxSize(40, 40);
        Label loading = new Label("Načítám sekce pravidel…");
        loading.getStyleClass().add("text-muted");
        HBox loadingBox = new HBox(10, progress, loading);
        loadingBox.setAlignment(Pos.CENTER_LEFT);
        content.getChildren().add(loadingBox);

        Thread thread = new Thread(() -> {
            try {
                SkippedManagerImpl manager = new SkippedManagerImpl(getConfigurationManager(), main.getValidationDataManager());
                SkippingDraft draft = new SkippingDraft(manager);
                Platform.runLater(() -> {
                    if (!closed) {
                        skippingDraft = draft;
                        content.getChildren().remove(loadingBox);
                        fillSkippingPage(content);
                    }
                });
            } catch (Throwable e) {
                e.printStackTrace();
                //chyba se musi ukazat, jinak by se ukazatel tocil donekonecna
                String message = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
                Platform.runLater(() -> {
                    content.getChildren().remove(loadingBox);
                    content.getChildren().add(wrapped("Seznam sekcí pravidel nelze načíst: " + message, "text-error"));
                });
            }
        }, "nastaveni-preskakovani");
        thread.setDaemon(true);
        thread.start();
        return page(content);
    }

    private void fillSkippingPage(VBox content) {
        SkippingDraft draft = skippingDraft;
        Dmf initial = draft.initialDmf(focusDmf);
        if (initial == null) {
            content.getChildren().add(wrapped("Není k dispozici žádná fDMF.", "text-muted"));
            return;
        }
        ChoiceBox<Dmf.Type> typeBox = new ChoiceBox<>();
        typeBox.getItems().setAll(draft.types());
        ComboBox<String> versionBox = new ComboBox<>();
        versionBox.setPrefWidth(200);
        Label summary = new Label();
        summary.getStyleClass().add("text-muted");
        Hyperlink unskipAll = link("Nepřeskakovat nic", () -> {
        });
        VBox list = new VBox();
        list.getStyleClass().add("list-box");

        Label typeLabel = new Label("Typ dokumentu");
        typeLabel.getStyleClass().add("text-bold");
        typeLabel.setMinWidth(Region.USE_PREF_SIZE);
        Label versionLabel = new Label("Verze");
        versionLabel.getStyleClass().add("text-bold");
        versionLabel.setMinWidth(Region.USE_PREF_SIZE);
        HBox selectors = new HBox(10, typeLabel, typeBox, versionLabel, versionBox);
        selectors.setAlignment(Pos.CENTER_LEFT);
        HBox.setMargin(versionLabel, new Insets(0, 0, 0, 10));
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox summaryLine = new HBox(10, summary, spacer, unskipAll);
        summaryLine.setAlignment(Pos.CENTER_LEFT);
        content.getChildren().addAll(selectors, summaryLine, list);

        //popisky verzi s poctem preskocenych sekci (meni se se zaskrtnutim)
        Runnable[] refreshVersionLabels = new Runnable[1];
        //behem plneni vyberu verzi se seznam sekci neprestavuje
        boolean[] filling = {false};
        versionBox.setCellFactory(lv -> new ListCell<String>() {
            @Override
            protected void updateItem(String version, boolean empty) {
                super.updateItem(version, empty);
                setText(empty || version == null ? null
                        : SettingsTexts.skippingVersionLabel(version, draft.skippedCount(new Dmf(typeBox.getValue(), version))));
            }
        });
        versionBox.setButtonCell(versionBox.getCellFactory().call(null));

        Runnable showSections = () -> {
            Dmf.Type type = typeBox.getValue();
            String version = versionBox.getValue();
            list.getChildren().clear();
            if (type == null || version == null) {
                return;
            }
            Dmf dmf = new Dmf(type, version);
            List<RulesSection> sections = draft.sections(dmf);
            Runnable updateSummary = () -> {
                int skipped = draft.skippedCount(dmf);
                summary.setText(SettingsTexts.skippingSummary(skipped, sections.size()));
                unskipAll.setVisible(skipped > 0);
                refreshVersionLabels[0].run();
                updateFooter();
            };
            //zahlavi „Preskocit“ ma sirku sloupce zaskrtavatek, aby „Sekce“ licovala s nazvy sekci
            Label skipHeader = headerLabel("Přeskočit");
            fixWidth(skipHeader, SKIP_COLUMN_WIDTH);
            skipHeader.setAlignment(Pos.CENTER);
            HBox header = new HBox(10, skipHeader, headerLabel("Sekce"));
            header.getStyleClass().addAll("list-row", "row-first", "table-header", "tools-table-header");
            list.getChildren().add(header);
            List<CheckBox> checks = new ArrayList<>();
            for (RulesSection section : sections) {
                CheckBox check = new CheckBox();
                check.setSelected(draft.isSkipped(section));
                check.setMinWidth(Region.USE_PREF_SIZE);
                check.selectedProperty().addListener((obs, oldValue, newValue) -> {
                    draft.setSkipped(section, newValue);
                    updateSummary.run();
                });
                checks.add(check);
                Label name = new Label(section.getName());
                name.getStyleClass().add("text-bold");
                name.setMinWidth(0);
                VBox texts = new VBox(2, name);
                if (section.getDescription() != null && !section.getDescription().trim().isEmpty()) {
                    texts.getChildren().add(wrapped(section.getDescription(), "text-muted"));
                }
                texts.setMinWidth(0);
                HBox.setHgrow(texts, Priority.ALWAYS);
                //sloupec zaskrtavatek ma sirku zahlavi „Preskocit“
                StackPane checkCell = new StackPane(check);
                fixWidth(checkCell, SKIP_COLUMN_WIDTH);
                checkCell.setAlignment(Pos.TOP_CENTER);
                HBox rowBox = new HBox(10, checkCell, texts);
                rowBox.getStyleClass().add("list-row");
                rowBox.setAlignment(Pos.TOP_LEFT);
                list.getChildren().add(rowBox);
            }
            unskipAll.setOnAction(e -> {
                draft.unskipAll(dmf);
                for (CheckBox check : checks) {
                    check.setSelected(false);
                }
                updateSummary.run();
            });
            updateSummary.run();
        };
        refreshVersionLabels[0] = () -> {
            //prekresli popisky polozek i vybrane hodnoty
            boolean wasFilling = filling[0];
            filling[0] = true;
            String selected = versionBox.getValue();
            List<String> items = new ArrayList<>(versionBox.getItems());
            versionBox.setButtonCell(versionBox.getCellFactory().call(null));
            versionBox.getItems().setAll(items);
            versionBox.setValue(selected);
            filling[0] = wasFilling;
        };
        typeBox.setConverter(new javafx.util.StringConverter<Dmf.Type>() {
            @Override
            public String toString(Dmf.Type type) {
                return type == null ? "" : DocumentTypes.name(type);
            }

            @Override
            public Dmf.Type fromString(String string) {
                return null;
            }
        });
        typeBox.valueProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue == null) {
                return;
            }
            filling[0] = true;
            versionBox.getItems().setAll(draft.versions(newValue));
            versionBox.setValue(versionBox.getItems().isEmpty() ? null : versionBox.getItems().get(0));
            filling[0] = false;
            showSections.run();
        });
        versionBox.valueProperty().addListener((obs, oldValue, newValue) -> {
            if (!filling[0] && newValue != null && !newValue.equals(oldValue)) {
                showSections.run();
            }
        });
        filling[0] = true;
        typeBox.setValue(initial.getType());
        versionBox.getItems().setAll(draft.versions(initial.getType()));
        versionBox.setValue(initial.getVersion());
        filling[0] = false;
        showSections.run();
    }

    // ===== slovníky =====

    private Node buildDictionariesPage() {
        VBox content = new VBox();
        content.getChildren().add(wrapped("Slovníky hodnot, podle kterých validace kontroluje kódy v metadatech. "
                + "Slovník s adresou pro aktualizaci lze stáhnout znovu; aktualizace proběhne hned ve vlastním okně, "
                + "tlačítka Uložit se netýká.", "text-muted"));
        VBox list = new VBox();
        list.getStyleClass().add("list-box");
        content.getChildren().add(list);
        fillDictionaries(list);
        return page(content);
    }

    private void fillDictionaries(VBox list) {
        list.getChildren().clear();
        DictionaryManager dictionaryManager = main.getValidationDataManager().getValidatorConfigMgr().getDictionaryManager();
        ConfigurationManager config = getConfigurationManager();
        List<String> names = new ArrayList<>(dictionaryManager.getDictionaries());
        Collections.sort(names);
        for (String dictionary : names) {
            String description = config.getStringOrDefault(ConfigurationManager.propDictionaryDescription(dictionary), null);
            String specUrl = config.getStringOrDefault(ConfigurationManager.propDictionarySpecUrl(dictionary), null);
            String syncUrl = config.getStringOrDefault(ConfigurationManager.propDictionarySyncUrl(dictionary), null);
            String syncDate = config.getStringOrDefault(ConfigurationManager.propDictionarySyncDate(dictionary), null);

            Label title = new Label(SettingsTexts.dictionaryTitle(description, dictionary));
            title.getStyleClass().add("text-bold");
            title.setMinWidth(0);
            StringBuilder detail = new StringBuilder(dictionary).append(", ")
                    .append(SettingsTexts.dictionaryValues(dictionaryManager.getDictionaryValues(dictionary).size()));
            String lastSync = SettingsTexts.dictionaryLastSync(syncUrl, syncDate);
            if (lastSync != null) {
                //udaj o aktualizaci se zalomi jako celek (datum nerozdelene)
                detail.append(", ").append(lastSync.replace(' ', '\u00A0'));
            }
            Label detailLabel = new Label(detail.toString());
            detailLabel.getStyleClass().add("text-muted");
            detailLabel.setMinWidth(0);
            //u slovniku s aktualizaci je radek delsi (datum), vedle jsou dve tlacitka
            detailLabel.setWrapText(true);
            detailLabel.setMinHeight(Region.USE_PREF_SIZE);
            VBox texts = new VBox(2, title, detailLabel);
            if (specUrl != null && !specUrl.trim().isEmpty()) {
                Hyperlink spec = Links.markExternal(link(SettingsTexts.dictionaryLinkLabel(specUrl), () -> openUrl(specUrl)));
                spec.setTooltip(new Tooltip(specUrl));
                texts.getChildren().add(spec);
            }
            texts.setMinWidth(0);
            HBox.setHgrow(texts, Priority.ALWAYS);

            Button show = new Button("Zobrazit hodnoty…");
            show.getStyleClass().add("button-secondary");
            show.setMinWidth(Region.USE_PREF_SIZE);
            show.setOnAction(e -> main.showDictionaryContentDialog(stage, dictionary, description, specUrl));
            HBox buttons = new HBox(6);
            buttons.setAlignment(Pos.CENTER_RIGHT);
            buttons.setMinWidth(Region.USE_PREF_SIZE);
            if (syncUrl != null && !syncUrl.trim().isEmpty()) {
                Button update = new Button("Aktualizovat…");
                update.getStyleClass().add("button-secondary");
                update.setMinWidth(Region.USE_PREF_SIZE);
                update.setOnAction(e -> {
                    main.showDictionaryUpdateDialog(stage, dictionary, syncUrl);
                    //pocet hodnot a datum aktualizace
                    fillDictionaries(list);
                });
                buttons.getChildren().add(update);
            }
            buttons.getChildren().add(show);

            HBox row = new HBox(10, texts, buttons);
            row.getStyleClass().add("list-row");
            if (list.getChildren().isEmpty()) {
                row.getStyleClass().add("row-first");
            }
            list.getChildren().add(row);
        }
        if (names.isEmpty()) {
            Label empty = new Label("Konfigurace validátoru neobsahuje žádné slovníky.");
            empty.getStyleClass().addAll("list-row", "row-first", "text-muted");
            list.getChildren().add(empty);
        }
    }

    // ===== adresáře a protokoly =====

    private Node buildDirsPage() {
        VBox content = new VBox();

        Label logDirPath = pathLabel(dirsDraft.getLogDir());
        Button openLogDir = openDirButton(() -> dirsDraft.getLogDir());
        Button changeLogDir = secondaryButton("Změnit…", () -> {
            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setTitle("Vyberte adresář pro protokoly a logy validace");
            File current = dirsDraft.getLogDir();
            if (current != null && current.isDirectory()) {
                chooser.setInitialDirectory(current);
            }
            File selected = chooser.showDialog(stage);
            if (selected != null) {
                dirsDraft.setLogDir(selected);
                logDirPath.setText(selected.getAbsolutePath());
                updateFooter();
            }
        });
        content.getChildren().add(dirCard("Protokoly a logy validace",
                "Sem se ukládá protokol v XML a textový log, když se vytvářejí.",
                logDirPath, openLogDir, changeLogDir));

        content.getChildren().add(dirCard("Konfigurace validátoru",
                "Pravidla validace (fDMF) a slovníky. Adresář se volí při startu aplikace, když konfigurace chybí.",
                pathLabel(dirsDraft.getValidatorConfigDir()),
                openDirButton(() -> dirsDraft.getValidatorConfigDir())));

        CheckBox xml = new CheckBox("Protokol v XML");
        xml.setSelected(dirsDraft.isCreateXmlLog());
        CheckBox txt = new CheckBox("Textový log");
        txt.setSelected(dirsDraft.isCreateTxtLog());
        ComboBox<OutputSettings.Verbosity> verbosity = new ComboBox<>();
        verbosity.getItems().setAll(OutputSettings.Verbosity.menuOrder());
        verbosity.setCellFactory(lv -> new ListCell<OutputSettings.Verbosity>() {
            @Override
            protected void updateItem(OutputSettings.Verbosity item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getLabel());
            }
        });
        verbosity.setValue(dirsDraft.getVerbosity());
        verbosity.setDisable(!txt.isSelected());
        xml.selectedProperty().addListener((obs, oldValue, newValue) -> {
            dirsDraft.setCreateXmlLog(newValue);
            updateFooter();
        });
        txt.selectedProperty().addListener((obs, oldValue, newValue) -> {
            dirsDraft.setCreateTxtLog(newValue);
            verbosity.setDisable(!newValue);
            updateFooter();
        });
        verbosity.valueProperty().addListener((obs, oldValue, newValue) -> {
            dirsDraft.setVerbosity(newValue);
            updateFooter();
        });
        HBox outputs = new HBox(16, xml, txt, verbosity);
        outputs.setAlignment(Pos.CENTER_LEFT);
        VBox outputsCard = new VBox(cardTitle("Výstupy validace"),
                wrapped("Tytéž volby jsou v dialogu Nová validace; změna na jednom místě platí i na druhém.", "text-muted"),
                outputs);
        outputsCard.getStyleClass().add("card");
        content.getChildren().add(outputsCard);
        return page(content);
    }

    private VBox dirCard(String title, String description, Label path, Button... buttons) {
        HBox.setHgrow(path, Priority.ALWAYS);
        HBox line = new HBox(8, path);
        line.getChildren().addAll(buttons);
        line.setAlignment(Pos.CENTER_LEFT);
        VBox card = new VBox(cardTitle(title), wrapped(description, "text-muted"), line);
        card.getStyleClass().add("card");
        return card;
    }

    private static Label cardTitle(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("text-bold");
        return label;
    }

    private static Label pathLabel(File file) {
        Label label = new Label(file == null ? "nenastaveno" : file.getAbsolutePath());
        label.getStyleClass().add("settings-path");
        label.setWrapText(true);
        label.setMinWidth(0);
        label.setMinHeight(Region.USE_PREF_SIZE);
        label.setMaxWidth(Double.MAX_VALUE);
        return label;
    }

    /** „Otevřít“ u adresáře: ve správci souborů (Finder, Průzkumník), ne v prohlížeči. */
    private static Button openDirButton(java.util.function.Supplier<File> dir) {
        Button button = secondaryButton("Otevřít", () -> Links.openDirectory(dir.get()));
        button.setTooltip(new Tooltip(Links.OPEN_DIRECTORY_TOOLTIP));
        return button;
    }

    // ===== pomocné =====

    private static Label headerCell(String text) {
        Label label = new Label(text);
        label.getStyleClass().addAll("table-header", "text-bold");
        label.setMaxWidth(Double.MAX_VALUE);
        return label;
    }

    /** šířka sloupce zaškrtávátek „Přeskočit“ v záložce Přeskakovaná pravidla (záhlaví i řádky) */
    private static final double SKIP_COLUMN_WIDTH = 62;

    private static void fixWidth(Region region, double width) {
        region.setMinWidth(width);
        region.setPrefWidth(width);
        region.setMaxWidth(width);
    }

    private static Label headerLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("text-bold");
        label.setMinWidth(Region.USE_PREF_SIZE);
        return label;
    }

    private static Label wrapped(String text, String styleClass) {
        Label label = new Label(text);
        label.getStyleClass().add(styleClass);
        label.setWrapText(true);
        label.setMinHeight(Region.USE_PREF_SIZE);
        label.setMinWidth(0);
        return label;
    }

    private static Hyperlink link(String text, Runnable action) {
        Hyperlink link = new Hyperlink(text);
        link.getStyleClass().add("link");
        link.setMinWidth(Region.USE_PREF_SIZE);
        link.setOnAction(e -> action.run());
        return link;
    }

    private static Button secondaryButton(String text, Runnable action) {
        Button button = new Button(text);
        button.getStyleClass().add("button-secondary");
        button.setMinWidth(Region.USE_PREF_SIZE);
        button.setOnAction(e -> action.run());
        return button;
    }

    // ===== snímky (SnapshotMode) =====

    DefaultVersions getDefaultVersions() {
        return defaultVersions;
    }

    /**
     * Vybere výchozí verzi typu v záložce Výchozí verze DMF stejně, jako by ji vybral uživatel (null = výchozí
     * validátoru); záložka se zobrazí. Vrátí false, když verze v nabídce není.
     */
    boolean selectDefaultVersionForSnapshot(Dmf.Type type, String version) {
        showTab(SettingsTab.VERSIONS);
        ChoiceBox<DefaultVersions.Choice> box = versionBoxes.get(type);
        for (DefaultVersions.Choice choice : box.getItems()) {
            if (Objects.equals(choice.version, version)) {
                box.setValue(choice);
                return true;
            }
        }
        return false;
    }

    /** Nastaví cestu k nástroji v záložce Externí nástroje jako uživatel (záložka se zobrazí). */
    void setToolPathForSnapshot(ExternalUtil util, File dir) {
        showTab(SettingsTab.TOOLS);
        toolsTable.setPathForSnapshot(util, dir);
    }

    /** Požadavek na zavření okna jako křížek; vrátí otevřený dotaz „Uložit změny?“, nebo null (okno se zavřelo). */
    Stage requestCloseForSnapshot() {
        requestClose();
        return savePrompt;
    }

    /** Tabulka nástrojů (záložka se zobrazí). */
    ToolsTable toolsTableForSnapshot() {
        showTab(SettingsTab.TOOLS);
        return toolsTable;
    }

    /** Stav nástrojů v záložce Externí nástroje (pro výpis), null, dokud se záložka neukázala. */
    String describeToolsForSnapshot() {
        if (toolsTable == null) {
            return null;
        }
        StringBuilder text = new StringBuilder();
        for (Map.Entry<ExternalUtil, nkp.pspValidator.gui.newvalidation.ToolStatus> entry : toolsTable.getStatuses().entrySet()) {
            text.append(text.length() == 0 ? "" : ", ").append(entry.getKey()).append('=').append(entry.getValue().getState())
                    .append(" (cesta ").append(toolPaths.getPath(entry.getKey())).append(')');
        }
        return text.toString();
    }

    /** Uloží koncept jako tlačítko Uložit (snímky ověřují propsání výchozí verze do validace). */
    void saveForSnapshot() {
        save(null);
    }
}
