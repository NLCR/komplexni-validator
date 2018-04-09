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
        return 700;
    }

    @Override
    public int getHeight() {
        return 450;
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
        ValidationsConfiguration configuration = DevValidationsConfigurationFactory.getInstance().getTestConfiguration();
        ((ValidationsConfigurationDialogController) controller).setValidationsConfiguration(configuration);
    }
}
