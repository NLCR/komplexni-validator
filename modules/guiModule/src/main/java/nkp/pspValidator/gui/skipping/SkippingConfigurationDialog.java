package nkp.pspValidator.gui.skipping;

import javafx.stage.Stage;
import nkp.pspValidator.gui.AbstractDialog;
import nkp.pspValidator.gui.DialogController;
import nkp.pspValidator.gui.Main;

import java.net.URL;

/**
 * Created by Martin Řehánek on 9.4.18.
 */
public class SkippingConfigurationDialog extends AbstractDialog {


    public SkippingConfigurationDialog(Stage stage, Main main) {
        super(stage, main);
    }

    @Override
    public URL getFxmlResource() {
        return getClass().getResource("/fxml/skipping/configurationDialog.fxml");
    }

    @Override
    public int getWidth() {
        return 1200;
    }

    @Override
    public int getHeight() {
        return 850;
    }

    @Override
    public String getTitle() {
        return "Přeskakování validací";
    }

    @Override
    public boolean isResizable() {
        return false;
    }

    @Override
    public void setControllerData(DialogController controller) {
        //((ValidationsConfigurationDialogController) controller).setValidationsConfiguration(mgr);
    }
}
