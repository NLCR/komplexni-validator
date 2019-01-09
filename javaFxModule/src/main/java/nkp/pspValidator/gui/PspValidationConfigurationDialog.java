package nkp.pspValidator.gui;

import javafx.stage.Stage;

import java.net.URL;

/**
 * Created by Martin Řehánek on 13.12.16.
 */
public class PspValidationConfigurationDialog extends AbstractDialog {

    public PspValidationConfigurationDialog(Stage stage, Main main) {
        super(stage, main);
    }

    @Override
    public URL getFxmlResource() {
        return getClass().getResource("/fxml/pspValidationConfigurationDialog.fxml");
    }

    @Override
    public int getWidth() {
        return 650;
    }

    @Override
    public int getHeight() {
        return 600;
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
