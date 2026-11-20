package nkp.pspValidator.gui;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.stage.WindowEvent;
import nkp.pspValidator.gui.newvalidation.ExternalTools;
import nkp.pspValidator.gui.newvalidation.ToolsOverview;
import nkp.pspValidator.gui.settings.ToolPathsDraft;
import nkp.pspValidator.shared.externalUtils.ExternalUtilManager;


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

    /**
     * @param closeWhenFinished zavřít dialog sám, jakmile jsou všechny nástroje dostupné
     * @param mainButtonText    text hlavního tlačítka („Pokračovat“)
     */
    public void setData(boolean closeWhenFinished, String mainButtonText) {
        this.closeWhenFinished = closeWhenFinished;
        mainButton.setText(mainButtonText);
    }

    /** Zavření okna (křížek) je totéž jako „Pokračovat“; během kontroly nástrojů okno zavřít nejde (ani tlačítkem). */
    @Override
    public EventHandler<WindowEvent> getOnCloseEventHandler() {
        return event -> {
            event.consume();
            if (ToolsOverview.canContinue(toolsTable.getState())) {
                continueInApp(null);
            }
        };
    }

    @Override
    public void startNow() {
        container.getStyleClass().add("dialog-scrolling");
        ExternalUtilManager utilManager = main.getValidationDataManager().getExternalUtilManager();
        //dialog nema tlacitko Ulozit: cesta k nastroji se uklada hned po vyberu
        toolsTable.init(getConfigurationManager(), utilManager, ToolPathsDraft.immediate(getConfigurationManager(), utilManager::setPath),
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
        ToolsOverview.State state = toolsTable.getState();
        boolean canContinue = ToolsOverview.canContinue(state);
        mainButton.setDisable(!canContinue);
        recheckButton.setDisable(!canContinue);
        if (state == ToolsOverview.State.ALL_AVAILABLE) {
            getConfigurationManager().setBoolean(ConfigurationManager.PROP_EXTERNAL_TOOLS_CHECK_SHOWN, true);
            mainButton.requestFocus();
            if (closeWhenFinished) {
                continueInApp(null);
            }
        }
    }
}
