package nkp.pspValidator.gui.exclusions;

import javafx.stage.Stage;
import nkp.pspValidator.gui.AbstractDialog;
import nkp.pspValidator.gui.DialogController;
import nkp.pspValidator.gui.Main;

import java.net.URL;

/**
 * Created by Martin Řehánek on 9.4.18.
 */
public class ExclusionsConfigurationDialog extends AbstractDialog {


    public ExclusionsConfigurationDialog(Stage stage, Main main) {
        super(stage, main);
    }

    @Override
    public URL getFxmlResource() {
        return getClass().getResource("/fxml/exclusions/exclusionsConfigurationDialog.fxml");
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
        return false;
    }

    @Override
    public void setControllerData(DialogController controller) {
        //((ValidationsConfigurationDialogController) controller).setValidationsConfiguration(mgr);
    }
}
