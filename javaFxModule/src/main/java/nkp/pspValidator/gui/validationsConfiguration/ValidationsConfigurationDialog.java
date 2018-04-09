package nkp.pspValidator.gui.validationsConfiguration;

import javafx.stage.Stage;
import nkp.pspValidator.gui.AbstractDialog;
import nkp.pspValidator.gui.DialogController;
import nkp.pspValidator.gui.Main;

import java.net.URL;

/**
 * Created by Martin Řehánek on 9.4.18.
 */
public class ValidationsConfigurationDialog extends AbstractDialog {


    public ValidationsConfigurationDialog(Stage stage, Main main) {
        super(stage, main);
    }

    @Override
    public URL getFxmlResource() {
        return getClass().getResource("/fxml/validationsConfiguration/validationsConfigurationDialog.fxml");
    }

    @Override
    public int getWidth() {
        return 1400;
    }

    @Override
    public int getHeight() {
        return 800;
    }

    @Override
    public String getTitle() {
        return "Vypnutí/zapnutí validací";
    }

    @Override
    public boolean isResizable() {
        //TODO: just temporary, put all content into scroll view
        return true;
    }

    @Override
    public void setControllerData(DialogController controller) {
        ((ValidationsConfigurationDialogController) controller).setValidationsConfiguration(MockConfigurationManager.getInstance());
    }
}
