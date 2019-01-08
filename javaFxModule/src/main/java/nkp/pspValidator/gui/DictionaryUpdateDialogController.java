package nkp.pspValidator.gui;

import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.stage.WindowEvent;
import nkp.pspValidator.shared.engine.exceptions.ValidatorConfigurationException;
import nkp.pspValidator.shared.metadataProfile.DictionaryManager;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Scanner;
import java.util.Set;


/**
 * Created by Martin Řehánek on 7.1.19.
 */
public class DictionaryUpdateDialogController extends DialogController {


    private String dictionaryName;
    private String syncUrl;

    private boolean updatingNow = false;

    @FXML
    Label lblDictName;

    @FXML
    Label lblTotalValues;

    @FXML
    Label lblLastUpdated;

    @FXML
    Label lblStatus;

    @FXML
    Button btnUpdate;

    @FXML
    Button btnCloseOrCancel;

    @FXML
    ProgressIndicator progressIndicator;

    private DictionaryManager getDictMgr() {
        return main.getValidationDataManager().getValidatorConfigMgr().getDictionaryManager();
    }

    private ConfigurationManager getConfigMgr() {
        return main.getConfigurationManager();
    }

    @Override
    public void startNow() {
        lblDictName.setText(dictionaryName);
        updateUi();
    }

    @Override
    public EventHandler<WindowEvent> getOnCloseEventHandler() {
        return event -> {
            if (updatingNow) {
                //ignore event
                event.consume();
            }
        };
    }

    public void setData(String dictionaryName, String syncUrl) {
        this.dictionaryName = dictionaryName;
        this.syncUrl = syncUrl;
    }


    public void closeDialog(ActionEvent actionEvent) {
        stage.close();
    }

    public void update(ActionEvent actionEvent) {
        updatingNow = true;
        lblStatus.setText("Aktualizuji ...");
        updateUi();
        //https://docs.oracle.com/javafx/2/api/javafx/concurrent/Task.html
        Task task = new Task<Void>() {

            @Override
            protected Void call() {
                if (ConfigurationManager.DEV_MODE) {
                    try {
                        Thread.sleep(300);
                    } catch (InterruptedException e) {
                        //nothing
                    }
                }
                Scanner reader = null;
                PrintWriter writer = null;
                try {
                    //download dictionary into temporary file
                    File tmpFile = File.createTempFile(dictionaryName, ".tmp");
                    writer = new PrintWriter(new FileWriter(tmpFile));
                    reader = new Scanner(new URL(syncUrl).openStream());
                    while (reader.hasNext()) {
                        String line = reader.nextLine();
                        writer.println(line);
                    }
                    writer.flush();

                    //replace dictionary file
                    File dictFile = getDictMgr().getDictionaryFile(dictionaryName);
                    Files.copy(Paths.get(tmpFile.toURI()), Paths.get(dictFile.toURI()), StandardCopyOption.REPLACE_EXISTING);
                    Files.delete(Paths.get(tmpFile.toURI()));
                    updateMessage("finished");
                } catch (IOException ex) {
                    updateMessage("CHYBA: " + ex.getClass().getSimpleName() + ": " + ex.getMessage());
                    ex.printStackTrace();
                } finally {
                    if (reader != null) {
                        reader.close();
                    }
                    if (writer != null) {
                        writer.close();
                    }
                }
                return null;
            }

           /* @Override
            protected void succeeded() {
                super.succeeded();
                updateMessage("succeeded");
            }

            @Override
            protected void cancelled() {
                super.cancelled();
                updateMessage("cancelled");
            }

            @Override
            protected void failed() {
                super.failed();
                updateMessage("failed");
            }*/
        };
        task.messageProperty().addListener((observable, oldValue, newValue) -> {
            System.out.println(newValue);
            updatingNow = false;
            if (newValue.startsWith("CHYBA")) {
                lblStatus.setText(newValue);
            } else {
                lblStatus.setText("Slovník úspěšně aktualizován!");
                //update syncDate property
                main.getConfigurationManager().setString(ConfigurationManager.propDictionarySyncDate(dictionaryName), todayDate());
                //reload dictionary manager
                try {
                    getDictMgr().reload();
                } catch (ValidatorConfigurationException e) {
                    lblStatus.setText("CHYBA: " + e.getMessage());
                    e.printStackTrace();
                }
            }
            updateUi();
        });
        new Thread(task).start();
    }

    private String todayDate() {
        DateTime now = new DateTime();
        DateTimeFormatter fmt = DateTimeFormat.forPattern("dd. MM. yyyy");
        return fmt.print(now);
    }

    private void updateUi() {
        Set<String> values = getDictMgr().getDictionaryValues(dictionaryName);
        String syncDate = getConfigMgr().getStringOrDefault(ConfigurationManager.propDictionarySyncDate(dictionaryName), null);
        if (syncDate == null || syncDate.isEmpty()) {
            lblLastUpdated.setText("-");
        } else {
            lblLastUpdated.setText(syncDate);
        }
        lblTotalValues.setText("" + values.size());

        if (updatingNow) {
            btnUpdate.setDisable(true);
            btnCloseOrCancel.setText("Zrušit");
            progressIndicator.setVisible(true);
        } else {
            btnUpdate.setDisable(false);
            btnCloseOrCancel.setText("Zavřít");
            progressIndicator.setVisible(false);
        }
    }

    public void closeOrCancel(ActionEvent actionEvent) {
        if (!updatingNow) {
            stage.close();
        } else {
            //Cancel button will have no effect for now
        }
    }
}
