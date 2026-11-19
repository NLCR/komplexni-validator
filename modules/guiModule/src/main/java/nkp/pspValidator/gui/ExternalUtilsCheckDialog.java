package nkp.pspValidator.gui;

import javafx.stage.Stage;

import java.net.URL;

/**
 * Dialog Externí nástroje: stav všech nástrojů v tabulce a nastavení jejich adresářů. Otevírá se při startu aplikace,
 * z menu Nastavení a z dialogu Nová validace.
 * <p>
 * Created by Martin Řehánek on 13.12.16.
 */
public class ExternalUtilsCheckDialog extends AbstractDialog {

    private final boolean closeWhenFinished;
    private final String mainButtonText;
    private final boolean exitAppOnCloseWithError;

    /**
     * @param closeWhenFinished       zavřít dialog sám, jakmile jsou všechny nástroje dostupné
     * @param mainButtonText          text hlavního tlačítka
     * @param exitAppOnCloseWithError zavření okna křížkem, když některý nástroj chybí, ukončí aplikaci (dialog při
     *                                startu aplikace); false: okno se jen zavře
     */
    public ExternalUtilsCheckDialog(Stage stage, Main main, boolean closeWhenFinished, String mainButtonText, boolean exitAppOnCloseWithError) {
        super(stage, main);
        this.closeWhenFinished = closeWhenFinished;
        this.mainButtonText = mainButtonText;
        this.exitAppOnCloseWithError = exitAppOnCloseWithError;
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
        ((ExternalUtilsCheckDialogController) controller).setData(closeWhenFinished, mainButtonText, exitAppOnCloseWithError);
    }

}
