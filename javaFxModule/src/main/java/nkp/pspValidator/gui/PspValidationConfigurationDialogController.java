package nkp.pspValidator.gui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.*;
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
    ChoiceBox forcedMonVersionChoiceBox;

    @FXML
    CheckBox forcedMonVersionCheckBox;

    @FXML
    ChoiceBox forcedPerVersionChoiceBox;

    @FXML
    CheckBox forcedPerVersionCheckBox;

    @FXML
    ChoiceBox preferedMonVersionChoiceBox;

    @FXML
    CheckBox preferedMonVersionCheckBox;

    @FXML
    ChoiceBox preferedPerVersionChoiceBox;

    @FXML
    CheckBox preferedPerVersionCheckBox;

    @FXML
    Label errorMessageLabel;

    @FXML
    CheckBox createTxtLog;

    @FXML
    CheckBox createXmlLog;

    @FXML
    ToggleButton verbosityLevel3;

    @FXML
    ToggleButton verbosityLevel2;

    @FXML
    ToggleButton verbosityLevel1;

    @FXML
    ToggleButton verbosityLevel0;

    @FXML
    private void initialize() {
        //System.out.println("initialize");
        //spousti se po Parent root = (Parent) loader.load();

        //verbosity toggle group
        ToggleGroup toggleGroup = new ToggleGroup();
        verbosityLevel0.setToggleGroup(toggleGroup);
        verbosityLevel1.setToggleGroup(toggleGroup);
        verbosityLevel2.setToggleGroup(toggleGroup);
        verbosityLevel3.setToggleGroup(toggleGroup);
        //znemožnění toho, aby nebyla vybrána žádná možnost
        toggleGroup.selectedToggleProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == null) {
                oldValue.setSelected(true);
            }
        });
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
        //forced
        boolean forcedMonVersionEnabled = mgr.getBooleanOrDefault(ConfigurationManager.PROP_FORCE_MON_VERSION_ENABLED, false);
        boolean forcedPerVersionEnabled = mgr.getBooleanOrDefault(ConfigurationManager.PROP_FORCE_PER_VERSION_ENABLED, false);
        forcedMonVersionCheckBox.setSelected(forcedMonVersionEnabled);
        forcedMonVersionChoiceBox.setDisable(!forcedMonVersionEnabled);
        forcedPerVersionCheckBox.setSelected(forcedPerVersionEnabled);
        forcedPerVersionChoiceBox.setDisable(!forcedPerVersionEnabled);
        //prefered
        boolean preferedMonVersionEnabled = mgr.getBooleanOrDefault(ConfigurationManager.PROP_PREFER_MON_VERSION_ENABLED, false);
        boolean preferedPerVersionEnabled = mgr.getBooleanOrDefault(ConfigurationManager.PROP_PREFER_PER_VERSION_ENABLED, false);
        preferedMonVersionCheckBox.setSelected(preferedMonVersionEnabled);
        preferedMonVersionChoiceBox.setDisable(!preferedMonVersionEnabled);
        preferedPerVersionCheckBox.setSelected(preferedPerVersionEnabled);
        preferedPerVersionChoiceBox.setDisable(!preferedPerVersionEnabled);

        //logs
        createTxtLog.setSelected(mgr.getBooleanOrDefault(ConfigurationManager.PROP_PSP_VALIDATION_CREATE_TXT_LOG, false));
        createXmlLog.setSelected(mgr.getBooleanOrDefault(ConfigurationManager.PROP_PSP_VALIDATION_CREATE_XML_LOG, false));
        //TODO; taky inicializovat vyrobu logu
        //init views from fdmf
        initChoiceBoxes();
    }

    private void initChoiceBoxes() {
        ConfigurationManager mgr = getConfigurationManager();
        //forced - mon
        Set<String> forcedMonVersions = main.getValidationDataManager().getFdmfRegistry().getMonographFdmfVersions();
        if (forcedMonVersions != null) {
            ObservableList<String> monVersionsObservable = FXCollections.observableArrayList(forcedMonVersions);
            forcedMonVersionChoiceBox.setItems(monVersionsObservable);
            String version = mgr.getStringOrDefault(ConfigurationManager.PROP_FORCE_MON_VERSION_CODE, null);
            boolean found = false;
            if (version != null) {
                for (int i = 0; i < monVersionsObservable.size(); i++) {
                    if (version.equals(monVersionsObservable.get(i))) {
                        forcedMonVersionChoiceBox.getSelectionModel().select(i);
                        found = true;
                    }
                }
            }
            if (!found) {
                forcedMonVersionChoiceBox.getSelectionModel().selectFirst();
            }
        }
        //forced - per
        Set<String> forcedPerVersions = main.getValidationDataManager().getFdmfRegistry().getPeriodicalFdmfVersions();
        if (forcedPerVersions != null) {
            ObservableList<String> perVersionsObservable = FXCollections.observableArrayList(forcedPerVersions);
            forcedPerVersionChoiceBox.setItems(perVersionsObservable);
            String version = mgr.getStringOrDefault(ConfigurationManager.PROP_FORCE_PER_VERSION_CODE, null);
            boolean found = false;
            if (version != null) {
                for (int i = 0; i < perVersionsObservable.size(); i++) {
                    if (version.equals(perVersionsObservable.get(i))) {
                        forcedPerVersionChoiceBox.getSelectionModel().select(i);
                        found = true;
                    }
                }
            }
            if (!found) {
                forcedPerVersionChoiceBox.getSelectionModel().selectFirst();
            }
        }
        //prefered - mon
        Set<String> preferedMonVersions = main.getValidationDataManager().getFdmfRegistry().getMonographFdmfVersions();
        if (preferedMonVersions != null) {
            ObservableList<String> monVersionsObservable = FXCollections.observableArrayList(preferedMonVersions);
            preferedMonVersionChoiceBox.setItems(monVersionsObservable);
            String version = mgr.getStringOrDefault(ConfigurationManager.PROP_PREFER_MON_VERSION_CODE, null);
            boolean found = false;
            if (version != null) {
                for (int i = 0; i < monVersionsObservable.size(); i++) {
                    if (version.equals(monVersionsObservable.get(i))) {
                        preferedMonVersionChoiceBox.getSelectionModel().select(i);
                        found = true;
                    }
                }
            }
            if (!found) {
                preferedMonVersionChoiceBox.getSelectionModel().selectFirst();
            }
        }
        Set<String> preferedPerVersions = main.getValidationDataManager().getFdmfRegistry().getPeriodicalFdmfVersions();
        if (preferedPerVersions != null) {
            ObservableList<String> perVersionsObservable = FXCollections.observableArrayList(preferedPerVersions);
            preferedPerVersionChoiceBox.setItems(perVersionsObservable);
            String version = mgr.getStringOrDefault(ConfigurationManager.PROP_PREFER_PER_VERSION_CODE, null);
            boolean found = false;
            if (version != null) {
                for (int i = 0; i < perVersionsObservable.size(); i++) {
                    if (version.equals(perVersionsObservable.get(i))) {
                        preferedPerVersionChoiceBox.getSelectionModel().select(i);
                        found = true;
                    }
                }
            }
            if (!found) {
                preferedPerVersionChoiceBox.getSelectionModel().selectFirst();
            }
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
                String forcedMonVersion = forcedMonVersionChoiceBox.isDisabled() ? null : (String) forcedMonVersionChoiceBox.getSelectionModel().getSelectedItem();
                String forcedPerVersion = forcedPerVersionChoiceBox.isDisabled() ? null : (String) forcedPerVersionChoiceBox.getSelectionModel().getSelectedItem();
                String preferedMonVersion = preferedMonVersionChoiceBox.isDisabled() ? null : (String) preferedMonVersionChoiceBox.getSelectionModel().getSelectedItem();
                String preferedPerVersion = preferedPerVersionChoiceBox.isDisabled() ? null : (String) preferedPerVersionChoiceBox.getSelectionModel().getSelectedItem();
                int verbosity = getSelectedVerbosity();
                stage.hide();
                main.runPspValidation(pspDir, preferedMonVersion, preferedPerVersion, forcedMonVersion, forcedPerVersion, createTxtLog.isSelected(), createXmlLog.isSelected(), verbosity);
            }
        }
    }

    private int getSelectedVerbosity() {
        // TODO: 10.1.19 jeste si v properties pamatovat posledni zvolenou hodnotu
        if (verbosityLevel0.isSelected()) {
            return 0;
        } else if (verbosityLevel1.isSelected()) {
            return 1;
        } else if (verbosityLevel2.isSelected()) {
            return 2;
        } else if (verbosityLevel3.isSelected()) {
            return 3;
        } else {
            throw new IllegalStateException("no ToggleButton selected");
        }
    }

    private void showError(String s) {
        errorMessageLabel.setText(s);
    }

    public void forcedMonVersionChanged(ActionEvent actionEvent) {
        boolean forced = forcedMonVersionCheckBox.isSelected();
        forcedMonVersionChoiceBox.setDisable(!forced);
        if (getConfigurationManager() != null) {
            getConfigurationManager().setBoolean(ConfigurationManager.PROP_FORCE_MON_VERSION_ENABLED, forced);
        }
    }

    public void forcedPerVersionChanged(ActionEvent actionEvent) {
        boolean forced = forcedPerVersionCheckBox.isSelected();
        forcedPerVersionChoiceBox.setDisable(!forced);
        if (getConfigurationManager() != null) {
            getConfigurationManager().setBoolean(ConfigurationManager.PROP_FORCE_PER_VERSION_ENABLED, forced);
        }
    }

    public void forcedMonVersionChoiceboxChanged(ActionEvent actionEvent) {
        String version = (String) forcedMonVersionChoiceBox.getSelectionModel().getSelectedItem();
        if (getConfigurationManager() != null) {

            getConfigurationManager().setString(ConfigurationManager.PROP_FORCE_MON_VERSION_CODE, version);
        }
    }

    public void forcedPerVersionChoiceboxChanged(ActionEvent actionEvent) {
        String version = (String) forcedPerVersionChoiceBox.getSelectionModel().getSelectedItem();
        if (getConfigurationManager() != null) {
            getConfigurationManager().setString(ConfigurationManager.PROP_FORCE_PER_VERSION_CODE, version);
        }
    }

    public void preferedMonVersionChoiceboxChanged(ActionEvent actionEvent) {
        String version = (String) preferedMonVersionChoiceBox.getSelectionModel().getSelectedItem();
        if (getConfigurationManager() != null) {
            getConfigurationManager().setString(ConfigurationManager.PROP_PREFER_MON_VERSION_CODE, version);
        }
    }

    public void preferedPerVersionChoiceboxChanged(ActionEvent actionEvent) {
        String version = (String) preferedPerVersionChoiceBox.getSelectionModel().getSelectedItem();
        if (getConfigurationManager() != null) {
            getConfigurationManager().setString(ConfigurationManager.PROP_PREFER_PER_VERSION_CODE, version);
        }
    }


    public void preferedMonVersionChanged(ActionEvent actionEvent) {
        boolean prefered = preferedMonVersionCheckBox.isSelected();
        preferedMonVersionChoiceBox.setDisable(!prefered);
        if (getConfigurationManager() != null) {
            getConfigurationManager().setBoolean(ConfigurationManager.PROP_PREFER_MON_VERSION_ENABLED, prefered);
        }
    }

    public void preferedPerVersionChanged(ActionEvent actionEvent) {
        boolean prefered = preferedPerVersionCheckBox.isSelected();
        preferedPerVersionChoiceBox.setDisable(!prefered);
        if (getConfigurationManager() != null) {
            getConfigurationManager().setBoolean(ConfigurationManager.PROP_PREFER_PER_VERSION_ENABLED, prefered);
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
