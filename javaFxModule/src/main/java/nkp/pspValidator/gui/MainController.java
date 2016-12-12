package nkp.pspValidator.gui;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.MenuBar;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyEvent;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import nkp.pspValidator.shared.*;

import java.io.*;

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

    @FXML
    TextArea textArea;

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

    public void openNewValidationDialog(ActionEvent actionEvent) {
        showNewDialog();
    }

    private void showNewDialog() {
        PspValidationConfigurationDialog dialog = new PspValidationConfigurationDialog(menuBar.getScene().getWindow());
        dialog.showAndWait();
    }

    @Override
    void onConfigurationManagerSet() {

    }

    public void validatePsp(File pspDir, String focedMonographVersion, String forcedPeriodicalVersion) {
        textArea.clear();
        Task task = new Task<Void>() {

            @Override
            protected Void call() throws Exception {
                //System.out.println("validating " + pspDir.getAbsolutePath() + ", mon: " + focedMonographVersion + ", per: " + forcedPeriodicalVersion);
                try {
                    updateStatus("validuji " + pspDir.getAbsolutePath());
                    Dmf dmf = selectDmf(pspDir, focedMonographVersion, forcedPeriodicalVersion);
                    System.out.println(dmf);
                    FdmfConfiguration fdmfConfig = validationDataManager.getFdmfRegistry().getFdmfConfig(dmf);
                    Validator validator = ValidatorFactory.buildValidator(fdmfConfig, pspDir, validationDataManager.getImageUtilManager());
                    PrintStream out = textAreaPrintStream();//System.out;
                    //TODO: v produkci odstraint
                    Validator.DevParams devParams = new Validator.DevParams();
                    //devParams.getSectionsToRun().add("Bibliografická metadata");
                    //devParams.getSectionsToRun().add("Identifikátory");
                    //devParams.getSectionsToRun().add("JPEG 2000");

                    validator.run(null, out, true, true, true, true, devParams);
                    updateStatus("validace balíku " + pspDir.getAbsolutePath() + " hotova");
                } catch (Exception e) {
                    //TODO: handle in UI
                    e.printStackTrace();
                    updateStatus("chyba: " + e.getMessage());
                } finally {
                    return null;
                }
            }

            private PrintStream textAreaPrintStream() {
                OutputStream out = new OutputStream() {

                    @Override
                    public void write(int b) throws IOException {
                        //TODO: rozbite kodovani, ale stejne je to jen docasne reseni
                        Platform.runLater(() -> textArea.appendText(String.valueOf((char) b)));
                    }
                };
                try {
                    return new PrintStream(out, true, "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    throw new RuntimeException(e);
                }
            }

            private Dmf selectDmf(File pspDir, String focedMonographVersion, String forcedPeriodicalVersion) throws Exception {
                DmfDetector dmfDetector = new DmfDetector();
                Dmf.Type type = dmfDetector.detectDmfType(pspDir);
                switch (type) {
                    case MONOGRAPH:
                        if (focedMonographVersion != null) {
                            return new Dmf(Dmf.Type.MONOGRAPH, focedMonographVersion);
                        } else {
                            String version = dmfDetector.detectDmfVersionFromInfoOrDefault(Dmf.Type.MONOGRAPH, pspDir);
                            return new Dmf(Dmf.Type.MONOGRAPH, version);
                        }
                    case PERIODICAL:
                        if (focedMonographVersion != null) {
                            return new Dmf(Dmf.Type.PERIODICAL, forcedPeriodicalVersion);
                        } else {
                            String version = dmfDetector.detectDmfVersionFromInfoOrDefault(Dmf.Type.PERIODICAL, pspDir);
                            return new Dmf(Dmf.Type.PERIODICAL, version);
                        }
                    default:
                        throw new Exception("nepodporovaný typ " + type);
                }
            }

            private void updateStatus(String statusText) {
                Platform.runLater(() -> {
                    status.setText(statusText);
                });
            }
        };
        new Thread(task).start();
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
