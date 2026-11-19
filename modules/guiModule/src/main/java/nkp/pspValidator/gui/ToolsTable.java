package nkp.pspValidator.gui;

import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.Scene;
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import javafx.util.Duration;
import nkp.pspValidator.gui.newvalidation.ExternalTools;
import nkp.pspValidator.gui.newvalidation.ToolChecks;
import nkp.pspValidator.gui.newvalidation.ToolStatus;
import nkp.pspValidator.gui.newvalidation.ToolsOverview;
import nkp.pspValidator.shared.externalUtils.ExternalUtil;
import nkp.pspValidator.shared.externalUtils.ExternalUtilManager;

import java.io.File;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Tabulka externích nástrojů (jeden řádek na nástroj z {@link ExternalUtil}): název, k čemu slouží, stav, nastavení
 * a zrušení adresáře nástroje, u nedostupného nástroje odkaz na podrobnosti. Používá ji dialog Externí nástroje (při
 * startu aplikace) i záložka Externí nástroje v Nastavení. Kontrola nástrojů běží v pracovních vláknech
 * ({@link ToolChecks}), texty a rozhodnutí jsou v {@link ToolsOverview}. Cesta k nástroji se ukládá hned po výběru
 * (stejné klíče {@link ConfigurationManager} a {@link ExternalUtilManager#setPath} jako dřív).
 * <p>
 * V úzkém rozložení ({@code compact}) je popis nástroje pod jeho názvem, ne ve vlastním sloupci.
 */
public class ToolsTable extends VBox {

    private static final double NAME_WIDTH = 110;
    private static final double DESCRIPTION_WIDTH = 165;
    private static final double COMPACT_NAME_WIDTH = 170;
    private static final double ACTIONS_WIDTH = 225;
    /** velikost okna s podrobnostmi kontroly nástroje */
    private static final double DETAILS_WIDTH = 640;
    private static final double DETAILS_HEIGHT = 400;

    private ConfigurationManager config;
    private ExternalUtilManager utilManager;
    private Supplier<Window> owner;
    private Consumer<String> urlOpener;
    private Runnable onChange;
    private boolean compact;
    private ToolChecks toolChecks;
    private final Map<ExternalUtil, Row> rows = new EnumMap<>(ExternalUtil.class);

    public ToolsTable() {
        getStyleClass().add("list-box");
    }

    /**
     * Sestaví řádky a spustí kontrolu všech nástrojů.
     *
     * @param owner      okno pro výběr adresáře a okno s podrobnostmi
     * @param urlOpener  otevření odkazu (návod k instalaci)
     * @param onChange   po každé změně stavu některého nástroje (souhrn, tlačítka okna), ve vlákně UI
     * @param threadName základ názvu pracovních vláken kontroly
     */
    public void init(ConfigurationManager config, ExternalUtilManager utilManager, Supplier<Window> owner,
                     Consumer<String> urlOpener, Runnable onChange, boolean compact, String threadName) {
        this.config = config;
        this.utilManager = utilManager;
        this.owner = owner;
        this.urlOpener = urlOpener;
        this.onChange = onChange;
        this.compact = compact;
        toolChecks = new ToolChecks(utilManager, threadName, (util, status) -> {
            updateRow(util);
            changed();
        });
        buildTable();
        checkAll();
    }

    /** Zkontroluje znovu všechny nástroje. */
    public void checkAll() {
        toolChecks.checkAll();
        refreshAll();
    }

    /** Stavy nástrojů (pohled jen pro čtení, mění se s výsledky kontrol). */
    public Map<ExternalUtil, ToolStatus> getStatuses() {
        return toolChecks.getStatuses();
    }

    public ToolsOverview.State getState() {
        return ToolsOverview.state(toolChecks.getStatuses());
    }

    private void changed() {
        if (onChange != null) {
            onChange.run();
        }
    }

    // ----- tabulka -----

    private void buildTable() {
        getChildren().clear();
        HBox header = compact
                ? new HBox(headerCell("Nástroj", COMPACT_NAME_WIDTH), headerCell("Stav", -1))
                : new HBox(headerCell("Nástroj", NAME_WIDTH), headerCell("K čemu slouží", DESCRIPTION_WIDTH), headerCell("Stav", -1));
        header.getStyleClass().addAll("list-row", "row-first", "table-header", "tools-table-header");
        getChildren().add(header);

        for (ExternalUtil util : ExternalUtil.values()) {
            Row row = new Row(util);
            rows.put(util, row);
            getChildren().add(row.box);
        }
    }

    private static Label headerCell(String text, double width) {
        Label label = new Label(text);
        label.getStyleClass().add("text-bold");
        if (width > 0) {
            fixWidth(label, width);
        } else {
            HBox.setHgrow(label, Priority.ALWAYS);
            label.setMaxWidth(Double.MAX_VALUE);
        }
        return label;
    }

    private static void fixWidth(Region node, double width) {
        node.setMinWidth(width);
        node.setPrefWidth(width);
        node.setMaxWidth(width);
    }

    private void refreshAll() {
        for (ExternalUtil util : ExternalUtil.values()) {
            updateRow(util);
        }
        changed();
    }

    private void updateRow(ExternalUtil util) {
        rows.get(util).update(util, toolChecks.getStatus(util), configuredDir(util));
    }

    // ----- cesta k nastroji -----

    private File configuredDir(ExternalUtil util) {
        return config.getFileOrNull(ExternalTools.configKey(util));
    }

    private void selectPath(ExternalUtil util) {
        String property = ExternalTools.configKey(util);
        File defaultDir = null;
        switch (config.getPlatform().getOperatingSystem()) {
            case WINDOWS:
                defaultDir = new File("C:\\Program Files");
                break;
        }
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle(String.format("Vyberte adresář se spustitelnými soubory %s", util.getUserFriendlyName()));
        File currentDir = config.getFileOrNull(property);
        if (currentDir != null && currentDir.exists()) {
            chooser.setInitialDirectory(currentDir);
        } else if (defaultDir != null && defaultDir.exists()) {
            chooser.setInitialDirectory(defaultDir);
        }
        File selectedDirectory = chooser.showDialog(owner.get());
        if (selectedDirectory != null) {
            config.setFile(property, selectedDirectory);
            utilManager.setPath(util, selectedDirectory);
            recheck(util);
        }
    }

    private void removePath(ExternalUtil util) {
        config.setFile(ExternalTools.configKey(util), null);
        utilManager.setPath(util, null);
        recheck(util);
    }

    private void recheck(ExternalUtil util) {
        toolChecks.check(util);
        updateRow(util);
        changed();
    }

    // ----- podrobnosti -----

    private void showDetails(ExternalUtil util) {
        openDetails(owner.get(), util, toolChecks.getStatus(util), configuredDir(util), urlOpener, true);
    }

    /**
     * Okno s podrobnostmi kontroly nedostupného nástroje (odkaz „Podrobnosti…“).
     *
     * @param wait čekat na zavření okna (false jen pro SnapshotMode)
     * @return okno s podrobnostmi
     */
    static Stage openDetails(Window owner, ExternalUtil util, ToolStatus status, File configuredDir,
                             Consumer<String> urlOpener, boolean wait) {
        Stage stage = new Stage(StageStyle.UTILITY);
        stage.initOwner(owner);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle(util.getUserFriendlyName());

        DialogFrame frame = new DialogFrame();
        frame.setTitle(ToolsOverview.detailsTitle(util, status));
        frame.setSubtitle("Výstup kontroly nástroje. Nástroj je volitelný, bez něj se kontroly, které ho potřebují, přeskočí.");
        TextArea text = new TextArea(ToolsOverview.detailsText(status, configuredDir));
        text.setEditable(false);
        text.setWrapText(true);
        text.setPrefRowCount(8);
        VBox.setVgrow(text, Priority.ALWAYS);
        Hyperlink installation = new Hyperlink("Návod k instalaci nástroje " + util.getUserFriendlyName());
        installation.getStyleClass().add("link");
        installation.setOnAction(e -> urlOpener.accept(ExternalTools.installationUrl(util)));
        frame.getContent().addAll(text, installation);
        Button close = new Button("Zavřít");
        close.getStyleClass().add("button-primary");
        close.setMinWidth(100);
        close.setDefaultButton(true);
        close.setCancelButton(true);
        close.setOnAction(e -> stage.close());
        frame.getFooterButtons().add(close);

        Scene scene = new Scene(frame, DETAILS_WIDTH, DETAILS_HEIGHT);
        AbstractDialog.addBaseStylesheet(scene);
        stage.setScene(scene);
        stage.setMinWidth(DETAILS_WIDTH);
        stage.setMinHeight(DETAILS_HEIGHT);
        if (wait) {
            stage.showAndWait();
        } else {
            stage.show();
        }
        return stage;
    }

    /** Řádek tabulky jednoho nástroje. */
    private class Row {
        final HBox box = new HBox();
        final Region dot = new Region();
        final Label statusText = new Label();
        final Tooltip statusTooltip = new Tooltip();
        final Hyperlink details = new Hyperlink("Podrobnosti…");
        final Button removePath = new Button("Zrušit cestu");
        final Button setPath = new Button("Nastavit cestu…");
        final Tooltip pathTooltip = new Tooltip();

        Row(ExternalUtil util) {
            Label name = new Label(util.getUserFriendlyName());
            name.getStyleClass().add("text-bold");

            Label description = new Label(ExternalTools.description(util));
            description.getStyleClass().add("text-muted");
            description.setTooltip(new Tooltip(description.getText()));

            statusText.setMinWidth(0);
            statusTooltip.setWrapText(true);
            statusTooltip.setMaxWidth(500);
            statusTooltip.setShowDuration(Duration.seconds(30));
            statusText.setTooltip(statusTooltip);
            details.getStyleClass().add("link");
            details.setMinWidth(Region.USE_PREF_SIZE);
            details.setOnAction(e -> showDetails(util));
            //v uzkem rozlozeni je odkaz na podrobnosti pod stavem, jinak by se text stavu nevesel
            HBox statusLine = compact ? new HBox(8, dot, statusText) : new HBox(8, dot, statusText, details);
            statusLine.setAlignment(Pos.CENTER_LEFT);
            statusLine.setMinWidth(0);
            Region status = statusLine;
            if (compact) {
                VBox statusBox = new VBox(2, statusLine, details);
                statusBox.setAlignment(Pos.CENTER_LEFT);
                statusBox.setMinWidth(0);
                status = statusBox;
            }
            HBox.setHgrow(status, Priority.ALWAYS);
            status.setMaxWidth(Double.MAX_VALUE);

            for (Button button : new Button[]{removePath, setPath}) {
                button.getStyleClass().add("button-secondary");
                button.setMinWidth(Region.USE_PREF_SIZE);
            }
            removePath.setTooltip(new Tooltip("Zrušit nastavený adresář, nástroj se pak spouští bez cesty"));
            removePath.setOnAction(e -> removePath(util));
            setPath.setTooltip(pathTooltip);
            setPath.setOnAction(e -> selectPath(util));
            HBox actions = new HBox(6, removePath, setPath);
            actions.setAlignment(Pos.CENTER_RIGHT);
            fixWidth(actions, ACTIONS_WIDTH);

            box.getStyleClass().add("list-row");
            if (compact) {
                description.setMinWidth(0);
                VBox nameBox = new VBox(2, name, description);
                fixWidth(nameBox, COMPACT_NAME_WIDTH);
                box.getChildren().addAll(nameBox, status, actions);
            } else {
                fixWidth(name, NAME_WIDTH);
                fixWidth(description, DESCRIPTION_WIDTH);
                box.getChildren().addAll(name, description, status, actions);
            }
        }

        void update(ExternalUtil util, ToolStatus status, File configuredDir) {
            dot.getStyleClass().setAll(ToolsOverview.dotStyleClass(status));
            statusText.setText(compact ? ToolsOverview.compactStatusText(util, status) : ToolsOverview.statusText(util, status));
            statusText.getStyleClass().removeAll("text-error", "text-muted");
            String textClass = ToolsOverview.textStyleClass(status);
            if (textClass != null) {
                statusText.getStyleClass().add(textClass);
            }
            statusTooltip.setText(ToolsOverview.statusTooltip(status));
            boolean hasDetails = ToolsOverview.hasDetails(status);
            details.setVisible(hasDetails);
            details.setManaged(hasDetails);
            boolean pathSet = configuredDir != null;
            removePath.setVisible(pathSet);
            removePath.setManaged(pathSet);
            pathTooltip.setText(ToolsOverview.pathTooltip(configuredDir));
        }
    }
}
