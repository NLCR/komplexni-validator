package nkp.pspValidator.gui;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.stage.DirectoryChooser;
import javafx.stage.WindowEvent;
import nkp.pspValidator.shared.FdmfRegistry;
import nkp.pspValidator.shared.ValidatorConfigurationManager;
import nkp.pspValidator.shared.engine.exceptions.ValidatorConfigurationException;
import nkp.pspValidator.shared.externalUtils.ExternalUtilManager;
import nkp.pspValidator.shared.externalUtils.ExternalUtilManagerFactory;

import java.io.File;
import java.util.logging.Logger;

/**
 * Created by Martin Řehánek on 9.12.16.
 */
public class ValidationDataInitializationDialogController extends DialogController {

    private static Logger LOG = Logger.getLogger(ValidationDataInitializationDialogController.class.getSimpleName());


    @FXML
    TextField rootDirTextfield;

    @FXML
    ProgressIndicator progressIndicator;

    @FXML
    ImageView imgError;

    @FXML
    Label errorLabel;

    @FXML
    Button btnSetFdmfsRootDir;

    //other data
    private DialogState state = DialogState.RUNNING;

    @Override
    public EventHandler<WindowEvent> getOnCloseEventHandler() {
        return event -> {
            switch (state) {
                case RUNNING:
                    event.consume();
                    break;
                case ERROR:
                    closeApp();
                    break;
                case FINISHED:
                    event.consume();
                    continueInApp(null);
            }
        };
    }

    @Override
    public void startNow() {
        state = DialogState.RUNNING;
        //show progress
        progressIndicator.setVisible(true);
        //hide images & buttons
        errorLabel.setVisible(false);
        imgError.setVisible(false);
        btnSetFdmfsRootDir.setVisible(false);
        getConfigurationManager();

        Task task = new Task<Void>() {
            private ValidationDataManager validationDataManager;

            @Override
            protected Void call() throws Exception {
                try {
                    Thread.sleep(300);
                    validationDataManager = new ValidationDataManager(getConfigurationManager());
                    File validatorConfigDir = getConfigurationManager().getFileOrNull(ConfigurationManager.PROP_VALIDATOR_CONFIG_DIR);
                    if (validatorConfigDir == null) {
                        processResult(new Result(false, "Není definován adresář s konfigurací Validátoru!"));
                    } else {
                        updateRootDir(validatorConfigDir.getCanonicalPath());
                        ValidatorConfigurationManager validatorConfigMgr = new ValidatorConfigurationManager(validatorConfigDir);
                        //Thread.sleep(5000);
                        ExternalUtilManager externalUtilManager = new ExternalUtilManagerFactory(validatorConfigMgr.getExternalUtilsConfigFile()).buildExternalUtilManager(getConfigurationManager().getPlatform().getOperatingSystem());
                        validationDataManager.setExternalUtilManager(externalUtilManager);
                        validationDataManager.setFdmfRegistry(new FdmfRegistry(validatorConfigMgr));
                        validationDataManager.setValidatorConfigMgr(validatorConfigMgr);
                        processResult(new Result(true, null));
                    }
                } catch (ValidatorConfigurationException e) {
                    processResult(new Result(false, e.getMessage()));
                } finally {
                    return null;
                }
            }

            private void updateRootDir(String text) {
                Platform.runLater(() -> {
                    rootDirTextfield.setText(text);
                });
            }

            private void processResult(Result result) {
                progressIndicator.setVisible(false);
                Platform.runLater(() -> {
                    progressIndicator.setVisible(false);
                    if (result.isOk()) {//ok
                        state = DialogState.FINISHED;
                        main.setValidationDataManager(validationDataManager);
                        continueInApp(null);
                    } else {//error
                        state = DialogState.ERROR;
                        imgError.setVisible(true);
                        errorLabel.setVisible(true);
                        errorLabel.setText(result.getError());
                        btnSetFdmfsRootDir.setVisible(true);
                    }
                });
            }
        };
        new Thread(task).start();
    }

    public void continueInApp(ActionEvent actionEvent) {
        boolean shown = getConfigurationManager().getBooleanOrDefault(ConfigurationManager.PROP_IMAGE_TOOLS_CHECK_SHOWN, false);
        main.showExternalUtilsCheckDialog(shown || ConfigurationManager.DEV_MODE, "Pokračovat");
    }

    public void setFdmfsRootDir(ActionEvent actionEvent) {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Vyberte adresář s konfigurací Validátoru");
        File currentDir = getConfigurationManager().getFileOrNull(ConfigurationManager.PROP_VALIDATOR_CONFIG_DIR);
        if (currentDir != null && currentDir.exists()) {
            chooser.setInitialDirectory(currentDir);
        }
        File selectedDirectory = chooser.showDialog(stage);
        if (selectedDirectory != null) {
            getConfigurationManager().setFile(ConfigurationManager.PROP_VALIDATOR_CONFIG_DIR, selectedDirectory);
            startNow();
        }
    }


    private static final class Result {
        private final boolean ok;
        private final String error;

        public Result(boolean available, String error) {
            this.ok = available;
            this.error = error;
        }

        public boolean isOk() {
            return ok;
        }

        public String getError() {
            return error;
        }
    }

    public static enum DialogState {
        RUNNING, FINISHED, ERROR;
    }

}
