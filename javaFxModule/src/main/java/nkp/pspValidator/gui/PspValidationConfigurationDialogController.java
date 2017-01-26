package nkp.pspValidator.gui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;
import javafx.stage.WindowEvent;

import java.io.File;
import java.io.IOException;
import java.util.Set;

/**
 * Created by Martin Řehánek on 13.12.16.
 */
public class PspValidationConfigurationDialogController extends DialogController {

    @FXML
    TextField pspDirTextField;

    @FXML
    ChoiceBox monVersionChoiceBox;

    @FXML
    CheckBox monVersionForcedCheckBox;

    @FXML
    ChoiceBox perVersionChoiceBox;

    @FXML
    CheckBox perVersionForcedCheckBox;

    @FXML
    Label errorMessageLabel;

    @FXML
    CheckBox createTxtLog;

    @FXML
    CheckBox createXmlLog;

    @FXML
    private void initialize() {
        //System.out.println("initialize");
        //spousti se po Parent root = (Parent) loader.load();
    }

    @Override
    public EventHandler<WindowEvent> getOnCloseEventHandler() {
        return new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent event) {
                //nothing, event not consumed, so just closes
            }
        };
    }


    @Override
    public void startNow() {
        //init views from configuration
        ConfigurationManager mgr = getConfigurationManager();
        boolean monVersionForced = mgr.getBooleanOrDefault(ConfigurationManager.PROP_FORCE_MON_VERSION, false);
        boolean perVersionForced = mgr.getBooleanOrDefault(ConfigurationManager.PROP_FORCE_PER_VERSION, false);
        monVersionForcedCheckBox.setSelected(monVersionForced);
        monVersionChoiceBox.setDisable(!monVersionForced);
        perVersionForcedCheckBox.setSelected(perVersionForced);
        perVersionChoiceBox.setDisable(!perVersionForced);
        createTxtLog.setSelected(mgr.getBooleanOrDefault(ConfigurationManager.PROP_PSP_VALIDATION_CREATE_TXT_LOG, false));
        createXmlLog.setSelected(mgr.getBooleanOrDefault(ConfigurationManager.PROP_PSP_VALIDATION_CREATE_XML_LOG, false));
        //TODO; taky inicializovat vyrobu logu
        //init views from fdmf
        initChoiceBoxes();


    }

    private void initChoiceBoxes() {
        Set<String> monVersions = main.getValidationDataManager().getFdmfRegistry().getMonographFdmfVersions();
        if (monVersions != null) {
            ObservableList<String> monVersionsObservable = FXCollections.observableArrayList(monVersions);
            monVersionChoiceBox.setItems(monVersionsObservable);
            monVersionChoiceBox.getSelectionModel().selectFirst();
        }
        Set<String> perVersions = main.getValidationDataManager().getFdmfRegistry().getPeriodicalFdmfVersions();
        if (perVersions != null) {
            ObservableList<String> perVersionsObservable = FXCollections.observableArrayList(perVersions);
            perVersionChoiceBox.setItems(perVersionsObservable);
            perVersionChoiceBox.getSelectionModel().selectFirst();
        }
    }

    public void selectPspDir(ActionEvent actionEvent) {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Vyberte kořenový adresář PSP balíku");
        File lastPspDir = getConfigurationManager().getFileOrNull(ConfigurationManager.PROP_LAST_PSP_DIR);
        if (lastPspDir != null && lastPspDir.exists()) {
            File parent = lastPspDir.getParentFile();
            if (parent != null && parent.exists()) {
                chooser.setInitialDirectory(parent);
            }
        }
        File selectedDir = chooser.showDialog(stage);
        if (selectedDir != null) {
            getConfigurationManager().setFile(ConfigurationManager.PROP_LAST_PSP_DIR, selectedDir);
            try {
                pspDirTextField.setText(selectedDir.getCanonicalPath());
            } catch (IOException e) {
                //should never happen
                throw new RuntimeException(e);
            }
        }
    }

    public void closeDialog(ActionEvent actionEvent) {
        stage.close();
    }


    public void validate(ActionEvent actionEvent) {
        String pspDirTxt = pspDirTextField.getText();
        if (pspDirTxt == null || pspDirTxt.isEmpty()) {
            showError("Prázdný adresář PSP balíku!");
        } else {
            File pspDir = new File(pspDirTxt.trim());
            if (!pspDir.exists()) {
                showError(String.format("Adresář '%s' neexistuje!", pspDirTxt));
            } else if (!pspDir.isDirectory()) {
                showError(String.format("Soubor '%s' není adresář!", pspDirTxt));
            } else if (!pspDir.canRead()) {
                showError(String.format("Nelze číst obsah adresáře '%s'!", pspDirTxt));
            } else {
                String forcedMonVersion = monVersionChoiceBox.isDisabled() ? null : (String) monVersionChoiceBox.getSelectionModel().getSelectedItem();
                String forcedPerVersion = perVersionChoiceBox.isDisabled() ? null : (String) perVersionChoiceBox.getSelectionModel().getSelectedItem();
                stage.hide();
                main.runPspValidation(pspDir, forcedMonVersion, forcedPerVersion, createTxtLog.isSelected(), createXmlLog.isSelected());
            }
        }
    }

    private void showError(String s) {
        errorMessageLabel.setText(s);
    }

    public void monVersionForcedChanged(ActionEvent actionEvent) {
        boolean force = monVersionForcedCheckBox.isSelected();
        monVersionChoiceBox.setDisable(!force);
        if (getConfigurationManager() != null) {
            getConfigurationManager().setBoolean(ConfigurationManager.PROP_FORCE_MON_VERSION, force);
        }
    }

    public void perVersionForcedChanged(ActionEvent actionEvent) {
        boolean force = perVersionForcedCheckBox.isSelected();
        perVersionChoiceBox.setDisable(!force);
        if (getConfigurationManager() != null) {
            getConfigurationManager().setBoolean(ConfigurationManager.PROP_FORCE_PER_VERSION, force);
        }
    }

    public void createXmlLogChanged(ActionEvent actionEvent) {
        boolean create = createXmlLog.isSelected();
        getConfigurationManager().setBoolean(ConfigurationManager.PROP_PSP_VALIDATION_CREATE_XML_LOG, create);
    }

    public void createTxtLogChanged(ActionEvent actionEvent) {
        boolean create = createTxtLog.isSelected();
        getConfigurationManager().setBoolean(ConfigurationManager.PROP_PSP_VALIDATION_CREATE_TXT_LOG, create);
    }
}
