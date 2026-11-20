package nkp.pspValidator.gui;

import javafx.stage.Stage;

import java.net.URL;

/**
 * Dialog Externí nástroje: stav všech nástrojů v tabulce ({@link ToolsTable}) a nastavení jejich adresářů. Otevírá se
 * jen při startu aplikace; z menu a z dialogu Nová validace se nástroje nastavují v okně Nastavení (záložka Externí
 * nástroje, stejná tabulka). Dialog nemá tlačítko Uložit, cesta k nástroji se proto ukládá hned po výběru. Zavření
 * okna je totéž jako „Pokračovat“: aplikace pokračuje do hlavního okna, i když některý nástroj chybí.
 * <p>
 * Created by Martin Řehánek on 13.12.16.
 */
public class ExternalUtilsCheckDialog extends AbstractDialog {

    private final boolean closeWhenFinished;
    private final String mainButtonText;

    /**
     * @param closeWhenFinished zavřít dialog sám, jakmile jsou všechny nástroje dostupné
     * @param mainButtonText    text hlavního tlačítka
     */
    public ExternalUtilsCheckDialog(Stage stage, Main main, boolean closeWhenFinished, String mainButtonText) {
        super(stage, main);
        this.closeWhenFinished = closeWhenFinished;
        this.mainButtonText = mainButtonText;
    }

    @Override
    public URL getFxmlResource() {
        return getClass().getResource("/fxml/externalUtilsCheckDialog.fxml");
    }

    @Override
    public int getWidth() {
        return 860;
    }

    @Override
    public int getHeight() {
        return 620;
    }

    @Override
    public String getTitle() {
        return "Externí nástroje";
    }

    @Override
    public boolean isResizable() {
        return true;
    }

    @Override
    public void setControllerData(DialogController controller) {
        ((ExternalUtilsCheckDialogController) controller).setData(closeWhenFinished, mainButtonText);
    }

}
