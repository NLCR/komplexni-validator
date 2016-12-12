package nkp.pspValidator.gui;

import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.MenuBar;
import javafx.scene.input.KeyEvent;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import nkp.pspValidator.shared.FdmfConfiguration;
import nkp.pspValidator.shared.Validator;
import nkp.pspValidator.shared.ValidatorFactory;

import java.io.File;
import java.io.IOException;

/**
 * Created by martin on 9.12.16.
 */
public class MainController extends AbstractController {

    //TODO: nahradit konkretni wiki strankou
    private static final String URL_ONLINE_HELP = "https://github.com/rzeh4n/psp-validator/wiki";

    private ValidationDataManager validationDataManager;

    @FXML
    MenuBar menuBar;

    @FXML
    Label status;

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
        showNewDialog();

        /*try {
            //TODO: vybrat pspDir
            //TODO: detekce verze
            FdmfConfiguration fdmfConfig = validationDataManager.getFdmfRegistry().getFdmfConfig(new Dmf(Dmf.Type.MONOGRAPH, "1.2"));
            File pspRoot = new File("/home/martin/ssd/IdeaProjects/PspValidator/sharedModule/src/test/resources/monograph_1.2/b50eb6b0-f0a4-11e3-b72e-005056827e52");
            validate(fdmfConfig, pspRoot);
        } catch (FdmfRegistry.UnknownFdmfException e) {
            e.printStackTrace();
        }*/
    }


    private void validate(FdmfConfiguration fdmfConfig, File pspRoot) {
        //TODO: vybrat adresář, a validovat
        Task task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                try {
                    //Thread.sleep(300);
                    Validator validator = ValidatorFactory.buildValidator(fdmfConfig, pspRoot, validationDataManager.getImageUtilManager());
                    validator.run(null, true, false, true, false, null);
                } catch (Throwable e) {
                    e.printStackTrace();
                }/* catch (ValidatorConfigurationException e) {
                    processResult(new CheckResult(false, String.format("chyba: %s", e.getMessage())));
                }*/ finally {
                    return null;
                }

            }
        };
        new Thread(task).start();
    }


    private void showNewDialog() {
        PspValidationConfigurationDialog dialog = new PspValidationConfigurationDialog(menuBar.getScene().getWindow());
        dialog.showAndWait();
    }

    @Override
    void onConfigurationManagerSet() {

    }


    public class PspValidationConfigurationDialog extends Stage {

        public PspValidationConfigurationDialog(Window window) {
            setTitle("Nastavení validace PSP balíku");
            int initialWidth = 650;
            int initialHeight = 300;

            setWidth(initialWidth);
            setMinWidth(initialWidth);
            setHeight(initialHeight);
            setMinHeight(initialHeight);

            initStyle(StageStyle.UTILITY);
            initModality(Modality.WINDOW_MODAL);
            initOwner(window);

            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/pspValidationConfigurationDialog.fxml"));
                Parent root = (Parent) loader.load();
                PspValidationConfigurationDialogController controller = (PspValidationConfigurationDialogController) loader.getController();
                controller.setApp(app);
                controller.setConfigurationManager(configurationManager);
                controller.setValidationDataManager(validationDataManager);
                controller.setStage(PspValidationConfigurationDialog.this);
                setScene(new Scene(root));
            } catch (IOException e) {
                //should never happen
                throw new RuntimeException(e);
            }
        }
    }

}
