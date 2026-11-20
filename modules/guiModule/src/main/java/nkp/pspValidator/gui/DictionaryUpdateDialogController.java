package nkp.pspValidator.gui;

import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.stage.WindowEvent;
import nkp.pspValidator.gui.settings.SettingsTexts;
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
    CopyableMessage errorMessage;

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
        String description = getConfigMgr().getStringOrDefault(ConfigurationManager.propDictionaryDescription(dictionaryName), null);
        String title = SettingsTexts.dictionaryTitle(description, dictionaryName);
        lblDictName.setText(title.equals(dictionaryName) ? dictionaryName : title + " (" + dictionaryName + ")");
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
        hideError();
        lblStatus.setText("Aktualizuji…");
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
                File tmpFile = null;
                try {
                    //download dictionary into temporary file
                    tmpFile = File.createTempFile(dictionaryName, ".tmp");
                    //docasny soubor se musi zavrit driv, nez se kopiruje a maze: Windows otevreny soubor smazat
                    //nedovoli ("Proces nemá přístup k souboru, protože jej využívá jiný proces")
                    try (PrintWriter writer = new PrintWriter(new FileWriter(tmpFile));
                         Scanner reader = new Scanner(new URL(syncUrl).openStream())) {
                        while (reader.hasNext()) {
                            String line = reader.nextLine();
                            writer.println(line);
                        }
                        writer.flush();
                    }

                    //replace dictionary file
                    File dictFile = getDictMgr().getDictionaryFile(dictionaryName);
                    Files.copy(Paths.get(tmpFile.toURI()), Paths.get(dictFile.toURI()), StandardCopyOption.REPLACE_EXISTING);
                    updateMessage("finished");
                } catch (IOException ex) {
                    updateMessage("CHYBA: " + ex.getClass().getSimpleName() + ": " + ex.getMessage());
                    ex.printStackTrace();
                } finally {
                    if (tmpFile != null && !tmpFile.delete()) {
                        tmpFile.deleteOnExit();
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
            hideError();
            if (newValue.startsWith("CHYBA")) {
                showError(newValue);
            } else {
                lblStatus.setText("Slovník byl úspěšně aktualizován!");
                //update syncDate property
                main.getConfigurationManager().setString(ConfigurationManager.propDictionarySyncDate(dictionaryName), todayDate());
                //reload dictionary manager
                try {
                    getDictMgr().reload();
                } catch (ValidatorConfigurationException e) {
                    showError("CHYBA: " + e.getMessage());
                    e.printStackTrace();
                }
            }
            updateUi();
        });
        new Thread(task).start();
    }

    /** Chyba aktualizace: krátký stav a celý text (s adresou) v poli, ze kterého jde kopírovat. */
    void showError(String message) {
        lblStatus.setText("Aktualizace se nezdařila.");
        lblStatus.getStyleClass().remove("text-error");
        lblStatus.getStyleClass().add("text-error");
        errorMessage.setText(message + "\nAdresa: " + syncUrl);
        errorMessage.setVisible(true);
        errorMessage.setManaged(true);
    }

    private void hideError() {
        lblStatus.getStyleClass().remove("text-error");
        errorMessage.setText(null);
        errorMessage.setVisible(false);
        errorMessage.setManaged(false);
    }

    /** Pro SnapshotMode: aktualizace právě běží. */
    boolean isUpdatingNow() {
        return updatingNow;
    }

    /** Pro SnapshotMode: stav dialogu. */
    String describe() {
        return "status=" + lblStatus.getText() + ", lastUpdated=" + lblLastUpdated.getText()
                + ", values=" + lblTotalValues.getText()
                + ", error=" + (errorMessage.isVisible() ? errorMessage.getText().replace('\n', ' ') : "-")
                + ", copyShown=" + (errorMessage.isVisible() && errorMessage.isCopyShown());
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
            lblLastUpdated.setText("zatím neaktualizováno");
        } else {
            lblLastUpdated.setText(syncDate);
        }
        lblTotalValues.setText("" + values.size());

        if (updatingNow) {
            btnUpdate.setDisable(true);
            btnCloseOrCancel.setText("Zrušit");
        } else {
            btnUpdate.setDisable(false);
            btnCloseOrCancel.setText("Zavřít");
        }
        progressIndicator.setVisible(updatingNow);
        progressIndicator.setManaged(updatingNow);
    }

    public void closeOrCancel(ActionEvent actionEvent) {
        if (!updatingNow) {
            stage.close();
        } else {
            //Cancel button will have no effect for now
        }
    }
}
