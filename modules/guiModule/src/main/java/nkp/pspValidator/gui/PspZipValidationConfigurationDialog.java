package nkp.pspValidator.gui;

import javafx.stage.Stage;

import java.net.URL;

/**
 * Created by Martin Řehánek on 13.12.16.
 */
public class PspZipValidationConfigurationDialog extends AbstractDialog {

    public PspZipValidationConfigurationDialog(Stage stage, Main main) {
        super(stage, main);
    }

    @Override
    public URL getFxmlResource() {
        return getClass().getResource("/fxml/pspZipValidationConfigurationDialog.fxml");
    }

    @Override
    public int getWidth() {
        return 750;
    }

    @Override
    public int getHeight() {
        return 930;
    }

    @Override
    public String getTitle() {
        return "Nastavení validace PSP balíku (zip)";
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
