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
import nkp.pspValidator.shared.engine.Utils;

import java.io.File;
import java.io.IOException;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

/**
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
                    updateMessage("Zpracovávám soubor " + pspZip.getAbsolutePath());
                    busyWait(2000);
                    if (isCancelled()) {
                        return null;
                    }

                    //check, that ok ZIP
                    updateMessage(String.format("Kontroluji soubor %s", pspZip.getAbsolutePath()));
                    ZipFile zipFile = new ZipFile(pspZip);
                    busyWait(1000);
                    if (isCancelled()) {
                        return null;
                    }

                    //check tmp dir
                    File tmpDir = new File(System.getProperty("java.io.tmpdir"));
                    updateMessage(String.format("Kontroluji adresář %s", tmpDir.getAbsolutePath()));
                    busyWait(1000);
                    if (!tmpDir.exists()) {
                        showError(String.format("Chyba: adresář %s neexistuje!", tmpDir.getAbsolutePath()));
                        return null;
                    } else if (!tmpDir.isDirectory()) {
                        showError(String.format("Chyba: soubor %s není adresář!", tmpDir.getAbsolutePath()));
                        return null;
                    } else if (!tmpDir.canWrite()) {
                        showError(String.format("Chyba: nemůžu zapisovat do adresáře %s!", tmpDir.getAbsolutePath()));
                        return null;
                    } else {
                        File containerDir = new File(tmpDir, pspZip.getName().substring(0, pspZip.getName().length() - ".zip".length()));
                        //System.out.println("container dir:" + containerDir.getAbsolutePath());
                        if (isCancelled()) {
                            return null;
                        }

                        //delete unzipped container dir from previous run
                        if (containerDir.exists()) {
                            updateMessage(String.format("Mažu adresář %s", containerDir.getAbsolutePath()));
                            Utils.deleteNonemptyDir(containerDir);
                            busyWait(2000);
                            if (isCancelled()) {
                                return null;
                            }
                        }

                        //unzip into container dir
                        updateMessage(String.format("Rozbaluji %s do adresáře %s", pspZip.getAbsolutePath(), containerDir.getAbsolutePath()));
                        Utils.unzip(pspZip, containerDir);
                        busyWait(1000);
                        if (isCancelled()) {
                            return null;
                        }

                        File pspDir = containerDir;
                        File[] filesInContainer = containerDir.listFiles();
                        if (filesInContainer.length == 1) {
                            pspDir = filesInContainer[0];
                        }
                        //System.out.println("psp dir: " + pspDir.getAbsolutePath());

                        if (!isCancelled()) {
                            finishOkAndContinue(pspDir);
                        }
                        return null;
                    }
                } catch (ZipException e) {
                    showError(String.format("Chyba: Soubor %s není korektní ZIP: %s!", pspZip.getAbsolutePath(), e.getMessage()));
                    //e.printStackTrace();
                    return null;
                } catch (IOException e) {
                    showError(String.format("Chyba zpracování ZIP souboru %s: %s!", pspZip.getAbsolutePath(), e.getMessage()));
                    //e.printStackTrace();
                    return null;
                }
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
