package nkp.pspValidator.gui;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.stage.WindowEvent;
import nkp.pspValidator.gui.newvalidation.ExternalTools;
import nkp.pspValidator.gui.newvalidation.ToolsOverview;


/**
 * Kontroler dialogu Externí nástroje (při startu aplikace): tabulka nástrojů {@link ToolsTable}, souhrn v patičce
 * a tlačítka „Zkontrolovat znovu“ a „Pokračovat“. Tabulku používá i záložka Externí nástroje v Nastavení.
 */
public class ExternalUtilsCheckDialogController extends DialogController {

    @FXML
    DialogFrame container;
    @FXML
    ToolsTable toolsTable;
    @FXML
    Button recheckButton;
    @FXML
    Button mainButton;

    private boolean closeWhenFinished;
    private boolean exitAppOnCloseWithError;

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
            switch (toolsTable.getState()) {
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
        toolsTable.init(getConfigurationManager(), main.getValidationDataManager().getExternalUtilManager(),
                () -> stage, this::openUrl, this::updateOverall, false, "externi-nastroje");
    }

    public void continueInApp(ActionEvent actionEvent) {
        stage.hide();
    }

    public void recheckAll(ActionEvent actionEvent) {
        toolsTable.checkAll();
    }

    public void showHelp(ActionEvent actionEvent) {
        openUrl(ExternalTools.INSTALLATION_HELP_URL);
    }

    /** Souhrn v patičce a tlačítka podle celkového stavu (jako dosud: během kontroly nejde pokračovat). */
    private void updateOverall() {
        container.setFooterNote(ToolsOverview.summary(toolsTable.getStatuses()));
        switch (toolsTable.getState()) {
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
}
