package nkp.pspValidator.gui;

import javafx.stage.Stage;

import java.net.URL;

/**
 * Created by Martin Řehánek on 13.12.16.
 */
public class PspDirValidationConfigurationDialog extends AbstractDialog {

    public PspDirValidationConfigurationDialog(Stage stage, Main main) {
        super(stage, main);
    }

    @Override
    public URL getFxmlResource() {
        return getClass().getResource("/fxml/pspDirValidationConfigurationDialog.fxml");
    }

    @Override
    public int getWidth() {
        return 650;
    }

    @Override
    public int getHeight() {
        return 650;
    }

    @Override
    public String getTitle() {
        return "Nastavení validace PSP balíku";
    }

    @Override
    public boolean isResizable() {
        return false;
    }

    @Override
    public void setControllerData(DialogController controller) {
        //nothing
    }

}
