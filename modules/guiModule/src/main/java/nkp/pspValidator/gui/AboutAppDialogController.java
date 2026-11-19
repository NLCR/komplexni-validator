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
    Text logsDirText;

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
        configDirText.setText(getConfigFilePathOrNull(ConfigurationManager.PROP_VALIDATOR_CONFIG_DIR));
        logsDirText.setText(getConfigFilePathOrNull(ConfigurationManager.PROP_LOG_DIR));
    }

    private String getConfigFilePathOrNull(String propertyName) {
        File file = getConfigurationManager().getFileOrNull(propertyName);
        if (file == null) {
            return "null";
        } else {
            return file.getAbsolutePath();
        }
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
        openDirFromConfigProperty(ConfigurationManager.PROP_VALIDATOR_CONFIG_DIR);
    }

    public void openLogsDir(ActionEvent actionEvent) {
        openDirFromConfigProperty(ConfigurationManager.PROP_LOG_DIR);
    }

    private void openDirFromConfigProperty(String propertyName) {
        File dirFile = getConfigurationManager().getFileOrNull(propertyName);
        if (dirFile != null) {
            String path = dirFile.getAbsolutePath();
            openUrl("file://" + path);
        }
    }

}
