package nkp.pspValidator.gui.dev;

import javafx.stage.Stage;
import nkp.pspValidator.gui.AbstractDialog;
import nkp.pspValidator.gui.DialogController;
import nkp.pspValidator.gui.Main;

import java.net.URL;

/**
 * Vývojářský dialog (Nápověda → Online nápověda ve vývojářském módu): pracovní adresář aplikace.
 * <p>
 * Created by Lenovo on 19.12.2016.
 */
public class DevDialog extends AbstractDialog {

    public DevDialog(Stage stage, Main main) {
        super(stage, main);
    }

    @Override
    public URL getFxmlResource() {
        return getClass().getResource("/fxml/devDialog.fxml");
    }

    @Override
    public int getWidth() {
        return 650;
    }

    @Override
    public int getHeight() {
        return 260;
    }

    @Override
    public String getTitle() {
        return "Test";
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
