package nkp.pspValidator.gui;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.MenuBar;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import javafx.stage.Window;
import nkp.pspValidator.shared.*;

import java.io.*;
import java.util.logging.Logger;

/**
 * Created by martin on 9.12.16.
 */
public class MainController extends AbstractController {

    private static Logger LOG = Logger.getLogger(MainController.class.getSimpleName());

    //TODO: nahradit konkretni wiki strankou
    private static final String URL_ONLINE_HELP = "https://github.com/rzeh4n/psp-validator/wiki";

    @FXML
    BorderPane container;

    @FXML
    MenuBar menuBar;

    @FXML
    Label status;

    @FXML
    TextArea textArea;

    @Override
    public void start(Stage primaryStage) throws Exception {

    }

    public void handleKeyInput(KeyEvent keyEvent) {
        //TODO: zpracovani menu
    }

    public void showOnlineHelp(ActionEvent actionEvent) {
        openUrl(URL_ONLINE_HELP);
    }

    public void showAboutApp(ActionEvent actionEvent) {
        //TODO: dialog
    }

    public void openNewValidationDialog(ActionEvent actionEvent) {
        main.showNewValidationConfigurationDialog();
    }

    private Window getWindow() {
        return container.getScene().getWindow();
    }

    /**
     * @param pspDir
     * @param focedMonographVersion   can be null
     * @param forcedPeriodicalVersion can be null
     */
    public void runPspValidation(File pspDir, String focedMonographVersion, String forcedPeriodicalVersion) {
        textArea.clear();
        Task task = new Task<Void>() {

            @Override
            protected Void call() throws Exception {
                //System.out.println("validating " + pspDir.getAbsolutePath() + ", mon: " + focedMonographVersion + ", per: " + forcedPeriodicalVersion);
                try {
                    updateStatus("validuji " + pspDir.getAbsolutePath());
                    Dmf dmf = selectDmf(pspDir, focedMonographVersion, forcedPeriodicalVersion);
                    System.out.println(dmf);

                    FdmfConfiguration fdmfConfig = main.getValidationDataManager().getFdmfRegistry().getFdmfConfig(dmf);
                    Validator validator = ValidatorFactory.buildValidator(fdmfConfig, pspDir, main.getValidationDataManager().getImageUtilManager());
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

}
