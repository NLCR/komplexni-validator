package nkp.pspValidator.gui.exclusions;

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
import nkp.pspValidator.gui.exclusions.data.ExcludedSection;
import nkp.pspValidator.gui.exclusions.data.ExclusionsConfiguration;
import nkp.pspValidator.shared.Dmf;

import java.util.HashMap;
import java.util.Map;


/**
 * Created by Martin Řehánek on 9.4.18.
 */
public class ExclusionsConfigurationDialogController extends DialogController {

    @FXML
    VBox container;

    @FXML
    TabPane tabPane;

    @FXML
    Button btnSave;

    private ExclusionsManager exclusionsManager;
    private Map<Dmf, ExclusionsConfiguration> configurations;

    @Override
    public void startNow() {
        container.prefWidthProperty().bind(stage.widthProperty());
        container.prefHeightProperty().bind(stage.heightProperty());

        ValidationDataManager mgr = main.getValidationDataManager();
        // TODO: 10.4.18 inicializace configurationManager muze trvat dlouho, tak to delat ve vedlejsim vlakne a pridat nejaky progressbar
        //configurationManager = new MockConfigurationManager();
        exclusionsManager = new ExclusionsManagerImpl(mgr);
        configurations = buildConfigMap();

        for (Dmf dmf : exclusionsManager.getDmfList()) {
            Tab tab = new Tab();
            tab.setText(dmf.toString());
            HBox hbox = new HBox();
            ListView<ExcludedSection> sectionList = buildSectionList(dmf, configurations.get(dmf));
            hbox.getChildren().add(sectionList);
            hbox.setAlignment(Pos.CENTER);
            tab.setContent(hbox);
            tabPane.getTabs().add(tab);
            sectionList.prefWidthProperty().bind(tabPane.widthProperty());
        }
    }

    private ListView<ExcludedSection> buildSectionList(Dmf dmf, ExclusionsConfiguration exclusionsConfiguration) {
        ListView<ExcludedSection> sectionList = new ListView<>();
        ObservableList<ExcludedSection> sectionsObservable = FXCollections.observableList(exclusionsConfiguration.getExcludedSections());
        sectionList.setCellFactory(new Callback<ListView<ExcludedSection>, ListCell<ExcludedSection>>() {

            @Override
            public ListCell<ExcludedSection> call(javafx.scene.control.ListView<ExcludedSection> list) {
                return new ListCell<ExcludedSection>() {

                    @Override
                    protected void updateItem(ExcludedSection excludedSection, boolean empty) {
                        super.updateItem(excludedSection, empty);
                        if (empty || excludedSection == null) {
                            setGraphic(null);
                        } else {
                            ExcludedSectionItem item = new ExcludedSectionItem(ExclusionsConfigurationDialogController.this);
                            item.populate(excludedSection);
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

    private Map<Dmf, ExclusionsConfiguration> buildConfigMap() {
        Map<Dmf, ExclusionsConfiguration> map = new HashMap<>();
        for (Dmf dmf : exclusionsManager.getDmfList()) {
            map.put(dmf, exclusionsManager.getConfiguration(dmf));
        }
        return map;
    }

    @Override
    public EventHandler<WindowEvent> getOnCloseEventHandler() {
        return null;
    }

    public void notifyDataEdited() {
        btnSave.setDisable(false);
    }

    public void closeDialog(ActionEvent actionEvent) {
        stage.close();
    }

    public void saveData(ActionEvent actionEvent) {
        for (Dmf dmf : configurations.keySet()) {
            exclusionsManager.setConfiguration(dmf, configurations.get(dmf));
        }
        stage.close();
    }
}
