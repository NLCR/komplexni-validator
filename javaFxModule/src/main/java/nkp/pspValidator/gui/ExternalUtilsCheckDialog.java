package nkp.pspValidator.gui;

import javafx.stage.Stage;

import java.net.URL;

/**
 * Created by Martin Řehánek on 13.12.16.
 */
public class ExternalUtilsCheckDialog extends AbstractDialog {

    private final boolean closeWhenFinished;
    private final String mainButtonText;

    public ExternalUtilsCheckDialog(Stage stage, Main main, boolean closeWhenFinished, String mainButtonText) {
        super(stage, main);
        this.closeWhenFinished = closeWhenFinished;
        this.mainButtonText = mainButtonText;
    }

    @Override
    public URL getFxmlResource() {
        return getClass().getResource("/fxml/externalUtilsCheck.fxml");
    }

    @Override
    public int getWidth() {
        return 790;
    }

    @Override
    public int getHeight() {
        return 650;
    }

    @Override
    public String getTitle() {
        return "Kontrola dostupnosti externích nástrojů";
    }

    @Override
    public boolean isResizable() {
        //TODO: just temporary, put all content into scroll view
        return true;
    }

    @Override
    public void setControllerData(DialogController controller) {
        ((ExternalUtilsCheckDialogController) controller).setData(closeWhenFinished, mainButtonText);
    }

}
