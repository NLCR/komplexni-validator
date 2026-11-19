package nkp.pspValidator.gui;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.WindowEvent;
import javafx.util.Duration;
import nkp.pspValidator.gui.newvalidation.ExternalTools;
import nkp.pspValidator.gui.newvalidation.ToolChecks;
import nkp.pspValidator.gui.newvalidation.ToolStatus;
import nkp.pspValidator.gui.newvalidation.ToolsOverview;
import nkp.pspValidator.shared.externalUtils.ExternalUtil;
import nkp.pspValidator.shared.externalUtils.ExternalUtilManager;

import java.io.File;
import java.net.URL;
import java.util.EnumMap;
import java.util.Map;


/**
 * Kontroler dialogu Externí nástroje: tabulka s jedním řádkem na nástroj z {@link ExternalUtil} (název, k čemu slouží,
 * stav, nastavení a zrušení adresáře nástroje). Kontrola nástrojů běží v pracovních vláknech ({@link ToolChecks}),
 * texty a rozhodnutí jsou v {@link ToolsOverview}.
 */
public class ExternalUtilsCheckDialogController extends DialogController {

    private static final double NAME_WIDTH = 110;
    private static final double DESCRIPTION_WIDTH = 165;
    private static final double ACTIONS_WIDTH = 225;

    @FXML
    DialogFrame container;
    @FXML
    VBox toolsTable;
    @FXML
    Button recheckButton;
    @FXML
    Button mainButton;

    private boolean closeWhenFinished;
    private boolean exitAppOnCloseWithError;
    private ExternalUtilManager utilManager;
    private ToolChecks toolChecks;
    private final Map<ExternalUtil, Row> rows = new EnumMap<>(ExternalUtil.class);

    /**
     * @param closeWhenFinished       zavřít dialog sám, jakmile jsou všechny nástroje dostupné
     * @param mainButtonText          text hlavního tlačítka („Pokračovat“, „Zavřít“)
     * @param exitAppOnCloseWithError zavření okna křížkem, když některý nástroj chybí, ukončí aplikaci (jen dialog
     *                                při startu aplikace)
     */
    public void setData(boolean closeWhenFinished, String mainButtonText, boolean exitAppOnCloseWithError) {
        this.closeWhenFinished = closeWhenFinished;
        this.exitAppOnCloseWithError = exitAppOnCloseWithError;
        mainButton.setText(mainButtonText);
    }

    @Override
    public EventHandler<WindowEvent> getOnCloseEventHandler() {
        return event -> {
            switch (ToolsOverview.state(toolChecks.getStatuses())) {
                case CHECKING:
                    event.consume();
                    break;
                case SOME_UNAVAILABLE:
                    if (exitAppOnCloseWithError) {
                        closeApp();
                    }
                    break;
                case ALL_AVAILABLE:
                    event.consume();
                    continueInApp(null);
                    break;
            }
        };
    }

    @Override
    public void startNow() {
        container.getStyleClass().add("dialog-scrolling");
        utilManager = main.getValidationDataManager().getExternalUtilManager();
        toolChecks = new ToolChecks(utilManager, "externi-nastroje", (util, status) -> {
            updateRow(util);
            updateOverall();
        });
        buildTable();
        toolChecks.checkAll();
        refreshAll();
    }

    public void continueInApp(ActionEvent actionEvent) {
        stage.hide();
    }

    public void recheckAll(ActionEvent actionEvent) {
        toolChecks.checkAll();
        refreshAll();
    }

    public void showHelp(ActionEvent actionEvent) {
        openUrl(ExternalTools.INSTALLATION_HELP_URL);
    }

    // ----- tabulka -----

    private void buildTable() {
        toolsTable.getChildren().clear();
        HBox header = new HBox(
                headerCell("Nástroj", NAME_WIDTH),
                headerCell("K čemu slouží", DESCRIPTION_WIDTH),
                headerCell("Stav", -1));
        header.getStyleClass().addAll("list-row", "row-first", "table-header", "tools-table-header");
        toolsTable.getChildren().add(header);

        for (ExternalUtil util : ExternalUtil.values()) {
            Row row = new Row(util);
            rows.put(util, row);
            toolsTable.getChildren().add(row.box);
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
        updateOverall();
    }

    private void updateRow(ExternalUtil util) {
        rows.get(util).update(toolChecks.getStatus(util), configuredDir(util));
    }

    /** Souhrn v patičce a tlačítka podle celkového stavu (jako dosud: během kontroly nejde pokračovat). */
    private void updateOverall() {
        ToolsOverview.State state = ToolsOverview.state(toolChecks.getStatuses());
        container.setFooterNote(ToolsOverview.summary(toolChecks.getStatuses()));
        switch (state) {
            case CHECKING:
                mainButton.setDisable(true);
                recheckButton.setDisable(true);
                break;
            case ALL_AVAILABLE:
                getConfigurationManager().setBoolean(ConfigurationManager.PROP_EXTERNAL_TOOLS_CHECK_SHOWN, true);
                mainButton.setDisable(false);
                recheckButton.setDisable(false);
                mainButton.requestFocus();
                if (closeWhenFinished) {
                    continueInApp(null);
                }
                break;
            case SOME_UNAVAILABLE:
                mainButton.setDisable(false);
                recheckButton.setDisable(false);
                break;
        }
    }

    // ----- cesta k nastroji -----

    private File configuredDir(ExternalUtil util) {
        return getConfigurationManager().getFileOrNull(ExternalTools.configKey(util));
    }

    private void selectPath(ExternalUtil util) {
        String property = ExternalTools.configKey(util);
        File defaultDir = null;
        switch (getConfigurationManager().getPlatform().getOperatingSystem()) {
            case WINDOWS:
                defaultDir = new File("C:\\Program Files");
                break;
        }
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle(String.format("Vyberte adresář se spustitelnými soubory %s", util.getUserFriendlyName()));
        File currentDir = getConfigurationManager().getFileOrNull(property);
        if (currentDir != null && currentDir.exists()) {
            chooser.setInitialDirectory(currentDir);
        } else if (defaultDir != null && defaultDir.exists()) {
            chooser.setInitialDirectory(defaultDir);
        }
        File selectedDirectory = chooser.showDialog(stage);
        if (selectedDirectory != null) {
            getConfigurationManager().setFile(property, selectedDirectory);
            utilManager.setPath(util, selectedDirectory);
            recheck(util);
        }
    }

    private void removePath(ExternalUtil util) {
        getConfigurationManager().setFile(ExternalTools.configKey(util), null);
        utilManager.setPath(util, null);
        recheck(util);
    }

    private void recheck(ExternalUtil util) {
        toolChecks.check(util);
        updateRow(util);
        updateOverall();
    }

    // ----- podrobnosti -----

    private void showDetails(ExternalUtil util) {
        ToolStatus status = toolChecks.getStatus(util);
        Alert alert = new Alert(Alert.AlertType.NONE, "", ButtonType.CLOSE);
        alert.initOwner(stage);
        alert.setTitle(util.getUserFriendlyName());
        alert.setHeaderText(ToolsOverview.detailsTitle(util, status));
        TextArea text = new TextArea(ToolsOverview.detailsText(status, configuredDir(util)));
        text.setEditable(false);
        text.setWrapText(true);
        text.setPrefColumnCount(60);
        text.setPrefRowCount(8);
        Hyperlink installation = new Hyperlink("Návod k instalaci nástroje " + util.getUserFriendlyName());
        installation.getStyleClass().add("link");
        installation.setOnAction(e -> openUrl(ExternalTools.installationUrl(util)));
        alert.getDialogPane().setContent(new VBox(10, text, installation));
        URL css = getClass().getResource(AbstractDialog.BASE_CSS);
        if (css != null) {
            alert.getDialogPane().getStylesheets().add(css.toExternalForm());
        }
        alert.showAndWait();
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
            fixWidth(name, NAME_WIDTH);

            Label description = new Label(ExternalTools.description(util));
            description.getStyleClass().add("text-muted");
            description.setTooltip(new Tooltip(description.getText()));
            fixWidth(description, DESCRIPTION_WIDTH);

            statusText.setMinWidth(0);
            statusTooltip.setWrapText(true);
            statusTooltip.setMaxWidth(500);
            statusTooltip.setShowDuration(Duration.seconds(30));
            statusText.setTooltip(statusTooltip);
            details.getStyleClass().add("link");
            details.setMinWidth(Region.USE_PREF_SIZE);
            details.setOnAction(e -> showDetails(util));
            HBox status = new HBox(8, dot, statusText, details);
            status.setAlignment(Pos.CENTER_LEFT);
            status.setMinWidth(0);
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
            box.getChildren().addAll(name, description, status, actions);
        }

        void update(ToolStatus status, File configuredDir) {
            dot.getStyleClass().setAll(ToolsOverview.dotStyleClass(status));
            statusText.setText(ToolsOverview.statusText(status));
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
