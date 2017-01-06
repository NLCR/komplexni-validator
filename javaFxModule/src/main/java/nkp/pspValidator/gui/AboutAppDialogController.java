package nkp.pspValidator.gui;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Hyperlink;
import javafx.scene.text.Text;
import javafx.stage.WindowEvent;
import nkp.pspValidator.shared.Version;

/**
 * Created by martin on 20.12.16.
 */
public class AboutAppDialogController extends DialogController {

    @FXML
    Text buildDateText;

    @FXML
    Text versionText;

    @Override
    public void startNow() {
        versionText.setText(Version.VERSION_CODE);
        buildDateText.setText(Version.BUILD_DATE);
    }

    @Override
    public EventHandler<WindowEvent> getOnCloseEventHandler() {
        return null;
    }

    public void closeDialog(ActionEvent actionEvent) {
        stage.close();
    }

    public void openHyperlink(ActionEvent actionEvent) {
        openUrl(((Hyperlink) actionEvent.getSource()).getText());
    }
}