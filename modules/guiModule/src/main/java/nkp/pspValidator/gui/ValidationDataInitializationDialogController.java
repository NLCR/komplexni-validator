package nkp.pspValidator.gui;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
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
    DialogFrame container;

    @FXML
    TextField rootDirTextfield;

    @FXML
    ProgressIndicator progressIndicator;

    @FXML
    HBox progressBox;

    @FXML
    CopyableMessage errorLabel;

    @FXML
    Button btnSetFdmfsRootDir;

    //other data
    private DialogState state = DialogState.RUNNING;
    /** náhled pro SnapshotMode: nic se nenačítá, jen se ukáže stav */
    private boolean preview = false;
    private String previewError;

    /** Náhled pro SnapshotMode: s {@code error} chybový stav, jinak běžící inicializace. */
    void setPreview(String error) {
        this.preview = true;
        this.previewError = error;
    }

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
        container.setFooterNote("Po načtení se zkontroluje dostupnost externích nástrojů.");
        //show progress
        setVisibleManaged(progressBox, true);
        //hide error & button
        setVisibleManaged(errorLabel, false);
        setVisibleManaged(btnSetFdmfsRootDir, false);
        if (preview) {
            File dir = getConfigurationManager().getFileOrNull(ConfigurationManager.PROP_VALIDATOR_CONFIG_DIR);
            rootDirTextfield.setText(dir == null ? "" : dir.getAbsolutePath());
            if (previewError != null) {
                showError(previewError);
            }
            return;
        }
        getConfigurationManager();

        Task task = new Task<Void>() {
            private ValidationDataManager validationDataManager;

            @Override
            protected Void call() throws Exception {
                try {
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
                        //konfigurace jednotlivych fDMF se nacitaji (a validuji) az pri prvnim pouziti
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
                Platform.runLater(() -> {
                    setVisibleManaged(progressBox, false);
                    if (result.isOk()) {//ok
                        state = DialogState.FINISHED;
                        main.setValidationDataManager(validationDataManager);
                        if (SnapshotMode.isEnabled()) {
                            stage.close();
                            SnapshotMode.start(main);
                        } else {
                            continueInApp(null);
                        }
                    } else {//error
                        showError(result.getError());
                    }
                });
            }
        };
        new Thread(task).start();
    }

    private void showError(String error) {
        state = DialogState.ERROR;
        setVisibleManaged(progressBox, false);
        errorLabel.setText(error);
        setVisibleManaged(errorLabel, true);
        container.setFooterNote("Zavřením okna se aplikace ukončí.");
        setVisibleManaged(btnSetFdmfsRootDir, true);
    }

    private static void setVisibleManaged(Node node, boolean visible) {
        node.setVisible(visible);
        node.setManaged(visible);
    }

    public void continueInApp(ActionEvent actionEvent) {
        boolean shown = getConfigurationManager().getBooleanOrDefault(ConfigurationManager.PROP_EXTERNAL_TOOLS_CHECK_SHOWN, false);
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
