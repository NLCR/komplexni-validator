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
    URL getFxmlResource() {
        return getClass().getResource("/fxml/aboutAppDialog.fxml");
    }

    @Override
    int getWidth() {
        return 600;
    }

    @Override
    int getHeight() {
        return 400;
    }

    @Override
    String getTitle() {
        return "O programu";
    }

    @Override
    boolean isResizable() {
        return false;
    }

    @Override
    void setControllerData(DialogController controller) {
        //TODO
    }
}
