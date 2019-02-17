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
import java.util.*;

/**
 * Created by Martin Řehánek on 13.12.16.
 */
public class PspDirValidationConfigurationDialogController extends DialogController {

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
    ChoiceBox preferredMonVersionChoiceBox;

    @FXML
    CheckBox preferredMonVersionCheckBox;

    @FXML
    ChoiceBox preferredPerVersionChoiceBox;

    @FXML
    CheckBox preferredPerVersionCheckBox;

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
        //preferred
        boolean preferredMonVersionEnabled = mgr.getBooleanOrDefault(ConfigurationManager.PROP_PREFER_MON_VERSION_ENABLED, false);
        boolean preferredPerVersionEnabled = mgr.getBooleanOrDefault(ConfigurationManager.PROP_PREFER_PER_VERSION_ENABLED, false);
        preferredMonVersionCheckBox.setSelected(preferredMonVersionEnabled);
        preferredMonVersionChoiceBox.setDisable(!preferredMonVersionEnabled);
        preferredPerVersionCheckBox.setSelected(preferredPerVersionEnabled);
        preferredPerVersionChoiceBox.setDisable(!preferredPerVersionEnabled);
        //logs
        createTxtLog.setSelected(mgr.getBooleanOrDefault(ConfigurationManager.PROP_PSP_VALIDATION_CREATE_TXT_LOG, false));
        createXmlLog.setSelected(mgr.getBooleanOrDefault(ConfigurationManager.PROP_PSP_VALIDATION_CREATE_XML_LOG, false));
        //log verbosity
        Integer textLogVerbosity = mgr.getIntegerOrNull(ConfigurationManager.PROP_TEXT_LOG_VERBOSITY);
        if (textLogVerbosity != null) {
            switch (textLogVerbosity) {
                case 0:
                    verbosityLevel0.setSelected(true);
                    break;
                case 1:
                    verbosityLevel1.setSelected(true);
                    break;
                case 2:
                    verbosityLevel2.setSelected(true);
                    break;
                case 3:
                    verbosityLevel3.setSelected(true);
                    break;
            }
        }

        //init views from fdmf
        initChoiceBoxes();
    }

    private void initChoiceBoxes() {
        ConfigurationManager mgr = getConfigurationManager();
        //forced - mon
        List<String> forcedMonVersions = new ArrayList<>();
        forcedMonVersions.addAll(main.getValidationDataManager().getFdmfRegistry().getMonographFdmfVersions());
        Collections.sort(forcedMonVersions, new VersionComparator());
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
        List<String> forcedPerVersions = new ArrayList<>();
        forcedPerVersions.addAll(main.getValidationDataManager().getFdmfRegistry().getPeriodicalFdmfVersions());
        Collections.sort(forcedPerVersions, new VersionComparator());
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
        //preferred - mon
        List<String> preferredMonVersions = new ArrayList<>();
        preferredMonVersions.addAll(main.getValidationDataManager().getFdmfRegistry().getMonographFdmfVersions());
        Collections.sort(preferredMonVersions, new VersionComparator());
        if (preferredMonVersions != null) {
            ObservableList<String> monVersionsObservable = FXCollections.observableArrayList(preferredMonVersions);
            preferredMonVersionChoiceBox.setItems(monVersionsObservable);
            String version = mgr.getStringOrDefault(ConfigurationManager.PROP_PREFER_MON_VERSION_CODE, null);
            boolean found = false;
            if (version != null) {
                for (int i = 0; i < monVersionsObservable.size(); i++) {
                    if (version.equals(monVersionsObservable.get(i))) {
                        preferredMonVersionChoiceBox.getSelectionModel().select(i);
                        found = true;
                    }
                }
            }
            if (!found) {
                preferredMonVersionChoiceBox.getSelectionModel().selectFirst();
            }
        }
        //preferred - per
        List<String> preferredPerVersions = new ArrayList<>();
        preferredPerVersions.addAll(main.getValidationDataManager().getFdmfRegistry().getPeriodicalFdmfVersions());
        Collections.sort(preferredPerVersions, new VersionComparator());
        if (preferredPerVersions != null) {
            ObservableList<String> perVersionsObservable = FXCollections.observableArrayList(preferredPerVersions);
            preferredPerVersionChoiceBox.setItems(perVersionsObservable);
            String version = mgr.getStringOrDefault(ConfigurationManager.PROP_PREFER_PER_VERSION_CODE, null);
            boolean found = false;
            if (version != null) {
                for (int i = 0; i < perVersionsObservable.size(); i++) {
                    if (version.equals(perVersionsObservable.get(i))) {
                        preferredPerVersionChoiceBox.getSelectionModel().select(i);
                        found = true;
                    }
                }
            }
            if (!found) {
                preferredPerVersionChoiceBox.getSelectionModel().selectFirst();
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
            showError("Prázdné jméno adresáře PSP balíku!");
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
                String preferredMonVersion = preferredMonVersionChoiceBox.isDisabled() ? null : (String) preferredMonVersionChoiceBox.getSelectionModel().getSelectedItem();
                String preferredPerVersion = preferredPerVersionChoiceBox.isDisabled() ? null : (String) preferredPerVersionChoiceBox.getSelectionModel().getSelectedItem();
                int verbosity = getSelectedVerbosity();
                stage.hide();
                main.runPspDirValidation(pspDir, preferredMonVersion, preferredPerVersion, forcedMonVersion, forcedPerVersion, createTxtLog.isSelected(), createXmlLog.isSelected(), verbosity);
            }
        }
    }

    private int getSelectedVerbosity() {
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

    public void preferredMonVersionChoiceboxChanged(ActionEvent actionEvent) {
        String version = (String) preferredMonVersionChoiceBox.getSelectionModel().getSelectedItem();
        if (getConfigurationManager() != null) {
            getConfigurationManager().setString(ConfigurationManager.PROP_PREFER_MON_VERSION_CODE, version);
        }
    }

    public void preferredPerVersionChoiceboxChanged(ActionEvent actionEvent) {
        String version = (String) preferredPerVersionChoiceBox.getSelectionModel().getSelectedItem();
        if (getConfigurationManager() != null) {
            getConfigurationManager().setString(ConfigurationManager.PROP_PREFER_PER_VERSION_CODE, version);
        }
    }


    public void preferredMonVersionChanged(ActionEvent actionEvent) {
        boolean preferred = preferredMonVersionCheckBox.isSelected();
        preferredMonVersionChoiceBox.setDisable(!preferred);
        if (getConfigurationManager() != null) {
            getConfigurationManager().setBoolean(ConfigurationManager.PROP_PREFER_MON_VERSION_ENABLED, preferred);
        }
    }

    public void preferredPerVersionChanged(ActionEvent actionEvent) {
        boolean preferred = preferredPerVersionCheckBox.isSelected();
        preferredPerVersionChoiceBox.setDisable(!preferred);
        if (getConfigurationManager() != null) {
            getConfigurationManager().setBoolean(ConfigurationManager.PROP_PREFER_PER_VERSION_ENABLED, preferred);
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

    public void onVerbositySwitched(ActionEvent actionEvent) {
        Object source = actionEvent.getSource();
        Integer verbosity = null;
        if (source == verbosityLevel0) {
            verbosity = 0;
        } else if (source == verbosityLevel1) {
            verbosity = 1;
        } else if (source == verbosityLevel2) {
            verbosity = 2;
        } else if (source == verbosityLevel3) {
            verbosity = 3;
        }
        getConfigurationManager().setInteger(ConfigurationManager.PROP_TEXT_LOG_VERBOSITY, verbosity);
    }
}
