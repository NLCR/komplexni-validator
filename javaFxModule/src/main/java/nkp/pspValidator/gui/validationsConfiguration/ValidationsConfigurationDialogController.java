package nkp.pspValidator.gui.validationsConfiguration;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.WindowEvent;
import javafx.util.Callback;
import nkp.pspValidator.gui.DialogController;
import nkp.pspValidator.gui.ValidationDataManager;
import nkp.pspValidator.shared.Dmf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Created by Martin Řehánek on 9.4.18.
 */
public class ValidationsConfigurationDialogController extends DialogController {

    @FXML
    VBox container;

    @FXML
    TabPane tabPane;

    @FXML
    Button btnSave;

    private ConfigurationManager configurationManager;
    private Map<Dmf, ValidationsConfiguration> configurations;

    @Override
    public void startNow() {
        container.prefWidthProperty().bind(stage.widthProperty());
        container.prefHeightProperty().bind(stage.heightProperty());

        ValidationDataManager mgr = main.getValidationDataManager();
        List<Dmf> dmfs = buildDmfList(mgr);
        configurations = buildConfigMap(dmfs);
        for (Dmf dmf : dmfs) {
            Tab tab = new Tab();
            tab.setText(dmf.toString());
            HBox hbox = new HBox();
            ListView<Section> sectionList = buildSectionList(dmf, configurations.get(dmf));
            hbox.getChildren().add(sectionList);
            hbox.setAlignment(Pos.CENTER);
            tab.setContent(hbox);
            tabPane.getTabs().add(tab);
            sectionList.prefWidthProperty().bind(tabPane.widthProperty());
        }
    }

    private ListView<Section> buildSectionList(Dmf dmf, ValidationsConfiguration validationsConfiguration) {
        ListView<Section> sectionList = new ListView<>();
        ObservableList<Section> sectionsObservable = FXCollections.observableList(validationsConfiguration.getSections());
        sectionList.setCellFactory(new Callback<ListView<Section>, ListCell<Section>>() {

            @Override
            public ListCell<Section> call(javafx.scene.control.ListView<Section> list) {
                return new ListCell<Section>() {

                    @Override
                    protected void updateItem(Section section, boolean empty) {
                        super.updateItem(section, empty);
                        if (empty || section == null) {
                            setGraphic(null);
                        } else {
                            SectionItem item = new SectionItem(ValidationsConfigurationDialogController.this);
                            item.populate(section);
                            setGraphic(item.getContainer());
                        }
                    }
                };
            }
        });
        sectionList.setItems(sectionsObservable);
        sectionList.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newSection) -> {
            if (newSection == null) {
                //selectSection(null);
            } else {
                    /*if (!newSection.equals(selectedSection)) {
                        selectSection(newSection);
                    }*/
            }
        });
        return sectionList;
    }

    private Map<Dmf, ValidationsConfiguration> buildConfigMap(List<Dmf> dmfs) {
        Map<Dmf, ValidationsConfiguration> map = new HashMap<>();
        for (Dmf dmf : dmfs) {
            map.put(dmf, configurationManager.getConfiguration(dmf));
        }
        return map;
    }

    private List<Dmf> buildDmfList(ValidationDataManager mgr) {
        List<Dmf> result = new ArrayList<>();
        for (String monVersion : mgr.getFdmfRegistry().getMonographFdmfVersions()) {
            result.add(new Dmf(Dmf.Type.MONOGRAPH, monVersion));
        }
        for (String perVersion : mgr.getFdmfRegistry().getPeriodicalFdmfVersions()) {
            result.add(new Dmf(Dmf.Type.PERIODICAL, perVersion));
        }
        return result;
    }

    @Override
    public EventHandler<WindowEvent> getOnCloseEventHandler() {
        return null;
    }

    public void setValidationsConfiguration(ConfigurationManager somethingManager) {
        this.configurationManager = somethingManager;
    }

    public void notifyDataEdited() {
        btnSave.setDisable(false);
    }

    public void closeDialog(ActionEvent actionEvent) {
        stage.close();
    }

    public void saveData(ActionEvent actionEvent) {
        for (Dmf dmf : configurations.keySet()) {
            configurationManager.setConfiguration(dmf, configurations.get(dmf));
        }
        stage.close();
    }
}
