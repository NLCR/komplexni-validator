package nkp.pspValidator.gui;

import javafx.stage.Stage;

import java.net.URL;

/**
 * Created by Martin Řehánek on 20.12.16.
 */
public class AboutAppDialog extends AbstractDialog {

    public AboutAppDialog(Stage stage, Main main) {
        super(stage, main);
    }

    @Override
    public URL getFxmlResource() {
        return getClass().getResource("/fxml/aboutAppDialog.fxml");
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
        return "O programu";
    }

    @Override
    public boolean isResizable() {
        return false;
    }

    @Override
    public void setControllerData(DialogController controller) {
        //TODO
    }
}
