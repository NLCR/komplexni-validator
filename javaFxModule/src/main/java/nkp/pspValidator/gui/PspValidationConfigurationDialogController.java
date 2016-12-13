package nkp.pspValidator.gui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;

import java.io.File;
import java.io.IOException;
import java.util.Set;

/**
 * Created by martin on 13.12.16.
 */
public class PspValidationConfigurationDialogController extends DialogController {

    private ValidationDataManager validationDataManager;
    //private MainController mainController;

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
    private void initialize() {
        //System.out.println("initialize");
        //spousti se po Parent root = (Parent) loader.load();
    }


    @Override
    void startNow() {
        boolean monVersionForced = getConfigurationManager().getBooleanOrDefault(ConfigurationManager.PROP_FORCE_MON_VERSION, false);
        boolean perVersionForced = getConfigurationManager().getBooleanOrDefault(ConfigurationManager.PROP_FORCE_PER_VERSION, false);
        monVersionForcedCheckBox.setSelected(monVersionForced);
        monVersionChoiceBox.setDisable(!monVersionForced);
        perVersionForcedCheckBox.setSelected(perVersionForced);
        perVersionChoiceBox.setDisable(!perVersionForced);
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
                String monVersion = monVersionChoiceBox.isDisabled() ? null : (String) monVersionChoiceBox.getSelectionModel().getSelectedItem();
                String perVersion = perVersionChoiceBox.isDisabled() ? null : (String) perVersionChoiceBox.getSelectionModel().getSelectedItem();
                stage.hide();
                main.runPspValidation(pspDir, monVersion, perVersion);
            }
        }
    }

    private void showError(String s) {
        errorMessageLabel.setText(s);
    }

    public void setValidationDataManager(ValidationDataManager validationDataManager) {
        this.validationDataManager = validationDataManager;
        initChoiceBoxes();
    }

    private void initChoiceBoxes() {
        Set<String> monVersions = validationDataManager.getFdmfRegistry().getMonographFdmfVersions();
        if (monVersions != null) {
            ObservableList<String> monVersionsObservable = FXCollections.observableArrayList(monVersions);
            monVersionChoiceBox.setItems(monVersionsObservable);
            monVersionChoiceBox.getSelectionModel().selectFirst();
        }
        Set<String> perVersions = validationDataManager.getFdmfRegistry().getPeriodicalFdmfVersions();
        if (perVersions != null) {
            ObservableList<String> perVersionsObservable = FXCollections.observableArrayList(perVersions);
            perVersionChoiceBox.setItems(perVersionsObservable);
            perVersionChoiceBox.getSelectionModel().selectFirst();
        }
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

}
