package nkp.pspValidator.gui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;
import javafx.stage.Window;
import nkp.pspValidator.shared.Dmf;

import java.io.File;
import java.io.IOException;
import java.util.Set;


/**
 * Created by martin on 12.12.16.
 */
public class PspValidationConfigurationDialogController extends AbstractController {


    @FXML
    TextField pspDirTextField;

    @FXML
    ChoiceBox monVersionChoiceBox;

    @FXML
    ChoiceBox perVersionChoiceBox;
    private Window windows;

    @FXML
    Label errorMessageLabel;
    private ValidationDataManager validationDataManager;

    @FXML
    private void initialize() {
        //System.out.println("initialize");
        //spousti se po Parent root = (Parent) loader.load();
    }


    public void selectPspDir(ActionEvent actionEvent) {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Vyberte kořenový adresář PSP balíku");
        File lastPspDir = configurationManager.getFileOrNull(ConfigurationManager.PROP_LAST_PSP_DIR);
        if (lastPspDir != null && lastPspDir.exists()) {
            File parent = lastPspDir.getParentFile();
            if (parent != null && parent.exists()) {
                chooser.setInitialDirectory(parent);
            }
        }
        File selectedDir = chooser.showDialog(stage);
        if (selectedDir != null) {
            configurationManager.setFile(ConfigurationManager.PROP_LAST_PSP_DIR, selectedDir);
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
                String monVersion = (String) monVersionChoiceBox.getSelectionModel().getSelectedItem();
                String perVersion = (String) perVersionChoiceBox.getSelectionModel().getSelectedItem();
                stage.close();
                app.validatePsp(pspDir, new Dmf(Dmf.Type.MONOGRAPH, monVersion), new Dmf(Dmf.Type.PERIODICAL, perVersion));
            }
        }
    }

    private void showError(String s) {
        errorMessageLabel.setText(s);
    }

    public void setValidationDataManager(ValidationDataManager validationDataManager) {
        this.validationDataManager = validationDataManager;
        initChoicBoxes();
    }

    private void initChoicBoxes() {
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
}
