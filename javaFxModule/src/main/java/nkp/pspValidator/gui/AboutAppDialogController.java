package nkp.pspValidator.gui;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.text.Text;
import javafx.stage.WindowEvent;
import nkp.pspValidator.shared.Version;

import java.io.File;

/**
 * Created by Martin Řehánek on 20.12.16.
 */
public class AboutAppDialogController extends DialogController {

    @FXML
    Text buildDateText;

    @FXML
    Text versionText;

    @FXML
    Label devModeLabel;

    @FXML
    Text configDirText;

    @FXML
    Text devModeText;

    @Override
    public void startNow() {
        versionText.setText(Version.VERSION_CODE);
        buildDateText.setText(Version.BUILD_DATE);
        if (!ConfigurationManager.DEV_MODE) {
            devModeLabel.setVisible(false);
            devModeText.setVisible(false);
        }
        File validatorConfigDir = getConfigurationManager().getFileOrNull(ConfigurationManager.PROP_VALIDATOR_CONFIG_DIR);
        configDirText.setText(validatorConfigDir.getAbsolutePath());
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

    public void openConfigDir(ActionEvent actionEvent) {
        openUrl("file://" + getConfigurationManager().getFileOrNull(ConfigurationManager.PROP_VALIDATOR_CONFIG_DIR).getAbsolutePath());
    }
}
