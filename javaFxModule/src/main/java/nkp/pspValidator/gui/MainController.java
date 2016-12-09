package nkp.pspValidator.gui;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.scene.input.KeyEvent;
import nkp.pspValidator.shared.Dmf;
import nkp.pspValidator.shared.FdmfConfiguration;
import nkp.pspValidator.shared.Validator;
import nkp.pspValidator.shared.ValidatorFactory;
import nkp.pspValidator.shared.engine.exceptions.ValidatorConfigurationException;
import nkp.pspValidator.shared.imageUtils.CliCommand;
import nkp.pspValidator.shared.imageUtils.ImageUtilManager;

import java.io.File;

/**
 * Created by martin on 9.12.16.
 */
public class MainController extends AbstractController {

    //TODO: nahradit
    private static final String URL_ONLINE_HELP = "https://github.com";

    private ValidationDataManager validationDataManager;

    public void setValidationDataManager(ValidationDataManager validationDataManager) {
        this.validationDataManager = validationDataManager;
    }

    public void handleKeyInput(KeyEvent keyEvent) {

    }

    public void showOnlineHelp(ActionEvent actionEvent) {
        openUrl(URL_ONLINE_HELP);
    }

    public void showAboutApp(ActionEvent actionEvent) {
        //TODO: dialog
    }

    public void validate(ActionEvent actionEvent) {


        //TODO: vybrat adresář, a validovat
        Task task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                try {
                    //TODO: vybrat pspDir
                    //TODO: detekce verze
                    //Thread.sleep(300);
                    File pspRoot = new File("/home/martin/ssd/IdeaProjects/PspValidator/sharedModule/src/test/resources/monograph_1.2/b50eb6b0-f0a4-11e3-b72e-005056827e52");
                    FdmfConfiguration fdmfConfig = validationDataManager.getFdmfRegistry().getFdmfConfig(new Dmf(Dmf.Type.MONOGRAPH, "1.2"));
                    Validator validator = ValidatorFactory.buildValidator(fdmfConfig, pspRoot, validationDataManager.getImageUtilManager());
                    validator.run(null, true, false, true, false, null);
                } catch (Throwable e) {
                    e.printStackTrace();
                }/* catch (ValidatorConfigurationException e) {
                    processResult(new CheckResult(false, String.format("chyba: %s", e.getMessage())));
                }*/finally {
                    return null;
                }

            }

            /*private void processResult(CheckResult result) {
                Platform.runLater(() -> {
                    progresIndicator.setVisible(false);
                    statusLabel.setVisible(true);
                    if (result.isAvailable()) {
                        //ok
                        imgOk.setVisible(true);
                        imgError.setVisible(false);
                        statusLabel.setText("verze: " + result.getMessage());
                    } else {
                        //error
                        imgOk.setVisible(false);
                        imgError.setVisible(true);
                        btnRetry.setVisible(true);
                        btnSelectPath.setVisible(true);
                        btnInstall.setVisible(true);
                        statusLabel.setText(result.getMessage());
                    }
                    setUtilFinished(util, true);
                    if (isAllUtilsFinished()) {
                        btnContinue.setDisable(false);
                    }
                });
            }*/
        };
        new Thread(task).start();
    }
}
