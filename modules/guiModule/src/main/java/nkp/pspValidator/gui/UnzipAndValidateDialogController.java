package nkp.pspValidator.gui;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.stage.WindowEvent;
import nkp.pspValidator.shared.DmfDetector;
import nkp.pspValidator.gui.newvalidation.ZipUnpacker;

import java.io.File;

/**
 * Dialog rozbalování ZIPu před validací jednoho balíku. Samotné rozbalení je v {@link ZipUnpacker} (používá ho i běh
 * skupiny balíků bez dialogu).
 * <p>
 * Created by Martin Řehánek on 15.1.19.
 */
public class UnzipAndValidateDialogController extends DialogController {

    @FXML
    Label lblInfo;

    @FXML
    ProgressIndicator progressIndicator;

    @FXML
    Button btnCancelOrClose;

    private Task task = null;

    private File pspZip;
    private DmfDetector.Params dmfDetectorParams;
    private boolean createTxtLog;
    private boolean createXmlLog;
    private int verbosity;

    @Override
    public void startNow() {
        task = new Task<Void>() {

            @Override
            protected Void call() throws Exception {
                try {
                    ZipUnpacker.Result result = new ZipUnpacker().unpack(pspZip, this::isCancelled, message -> {
                        updateMessage(message);
                        busyWait(1000);
                    });
                    if (result != null && !isCancelled()) {
                        finishOkAndContinue(result.getPspDir());
                    }
                } catch (ZipUnpacker.UnpackException e) {
                    showError(e.getMessage());
                }
                return null;
            }

            private void finishOkAndContinue(File pspDir) {
                Platform.runLater(() -> {
                    lblInfo.setText("Soubor ZIP byl rozbalen do adresáře " + pspDir.getAbsolutePath());
                    progressIndicator.setVisible(false);
                    task = null;
                    btnCancelOrClose.setText("Zavřít");
                    stage.close();
                    main.runPspDirValidation(pspDir, dmfDetectorParams, createTxtLog, createXmlLog, verbosity);
                });
            }

            private void busyWait(int millis) {
                if (ConfigurationManager.DEV_MODE) {
                    try {
                        Thread.sleep(millis);
                    } catch (InterruptedException e) {
                        //e.printStackTrace();
                    }
                }
            }

            private void showError(String message) {
                updateMessage(message);
                Platform.runLater(() -> {
                    //lblInfo.setText(message);
                    progressIndicator.setVisible(false);
                    task = null;
                    btnCancelOrClose.setText("Zavřít");
                });
            }

            @Override
            protected void updateMessage(String message) {
                super.updateMessage(message);
                //podle dokumentace by melo vzdy bezet na FX application thread
                Platform.runLater(() -> {
                    lblInfo.setText(message);
                });
            }

            @Override
            protected void cancelled() {
                super.cancelled();
                updateMessage("Rozbalování ZIP souboru zrušeno");
                Platform.runLater(() -> {
                    progressIndicator.setVisible(false);
                    task = null;
                    btnCancelOrClose.setText("Zavřít");
                });
            }
        };
        new Thread(task).start();
    }

    @Override
    public EventHandler<WindowEvent> getOnCloseEventHandler() {
        return event -> {
            if (task != null) {
                //ignore event
                event.consume();
            }
        };
    }

    public void setData(File pspZip, DmfDetector.Params dmfDetectorParams, boolean createTxtLog, boolean createXmlLog, int verbosity) {
        this.pspZip = pspZip;
        this.dmfDetectorParams = dmfDetectorParams;
        this.createTxtLog = createTxtLog;
        this.createXmlLog = createXmlLog;
        this.verbosity = verbosity;
    }

    public void cancelOrClose(ActionEvent actionEvent) {
        if (task != null) {
            //cancel
            task.cancel();
        } else {
            //close
            stage.close();
        }
    }
}
