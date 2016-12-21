package nkp.pspValidator.gui;

import javafx.stage.Stage;

import java.net.URL;

/**
 * Created by martin on 13.12.16.
 */
public class PspValidationConfigurationDialog extends AbstractDialog {

    public PspValidationConfigurationDialog(Stage stage, Main main) {
        super(stage, main);
    }

    @Override
    URL getFxmlResource() {
        return getClass().getResource("/fxml/pspValidationConfigurationDialog.fxml");
    }

    @Override
    int getWidth() {
        return 650;
    }

    @Override
    int getHeight() {
        return 450;
    }

    @Override
    String getTitle() {
        return "Nastavení validace PSP balíku";
    }

    @Override
    boolean isResizable() {
        return false;
    }

    @Override
    void setControllerData(DialogController controller) {
        //nothing
    }

}
