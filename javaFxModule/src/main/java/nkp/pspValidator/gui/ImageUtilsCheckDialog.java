package nkp.pspValidator.gui;

import javafx.stage.Stage;

import java.net.URL;

/**
 * Created by Martin Řehánek on 13.12.16.
 */
public class ImageUtilsCheckDialog extends AbstractDialog {

    private final boolean closeWhenFinished;
    private final String mainButtonText;

    public ImageUtilsCheckDialog(Stage stage, Main main, boolean closeWhenFinished, String mainButtonText) {
        super(stage, main);
        this.closeWhenFinished = closeWhenFinished;
        this.mainButtonText = mainButtonText;
    }

    @Override
    public URL getFxmlResource() {
        return getClass().getResource("/fxml/imageUtilsCheck.fxml");
    }

    @Override
    public int getWidth() {
        return 700;
    }

    @Override
    public int getHeight() {
        return 450;
    }

    @Override
    public String getTitle() {
        return "Kontrola dostupnosti nástrojů pro validaci obrázků";
    }

    @Override
    public boolean isResizable() {
        //TODO: just temporary, put all content into scroll view
        return true;
    }

    @Override
    public void setControllerData(DialogController controller) {
        ((ImageUtilsCheckDialogController) controller).setData(closeWhenFinished, mainButtonText);
    }

}
