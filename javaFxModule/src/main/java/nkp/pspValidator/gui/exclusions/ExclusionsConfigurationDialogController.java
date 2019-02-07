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
import nkp.pspValidator.shared.Dmf;
import nkp.pspValidator.shared.engine.RulesSection;


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

    private SkippedManager skippedManager;

    @Override
    public void startNow() {
        container.prefWidthProperty().bind(stage.widthProperty());
        container.prefHeightProperty().bind(stage.heightProperty());

        // TODO: 10.4.18 inicializace SkippedManager muze trvat dlouho (pro kazdou DMF se inicializuje Engine a parsuji vsechny fDMF),
        // tak to delat ve vedlejsim vlakne a pridat nejaky progressbar
        skippedManager = new SkippedManagerImpl(main.getConfigurationManager(), main.getValidationDataManager());

        for (Dmf dmf : skippedManager.getDmfList()) {
            Tab tab = new Tab();
            tab.setText(dmf.toString());
            HBox hbox = new HBox();
            ListView<RulesSection> sectionList = buildSectionList(skippedManager.getSkippedForDmf(dmf));
            hbox.getChildren().add(sectionList);
            hbox.setAlignment(Pos.CENTER);
            tab.setContent(hbox);
            tab.setClosable(false);
            tabPane.getTabs().add(tab);
            sectionList.prefWidthProperty().bind(tabPane.widthProperty());
        }
    }

    private ListView<RulesSection> buildSectionList(Skipped skipped) {
        ListView<RulesSection> sectionList = new ListView<>();
        ObservableList<RulesSection> sectionsObservable = FXCollections.observableList(skipped.getAllSections());
        sectionList.setCellFactory(new Callback<ListView<RulesSection>, ListCell<RulesSection>>() {

            @Override
            public ListCell<RulesSection> call(javafx.scene.control.ListView<RulesSection> list) {
                return new ListCell<RulesSection>() {

                    @Override
                    protected void updateItem(RulesSection validationSection, boolean empty) {
                        super.updateItem(validationSection, empty);
                        if (empty || validationSection == null) {
                            setGraphic(null);
                        } else {
                            SectionItem item = new SectionItem(ExclusionsConfigurationDialogController.this);
                            item.populate(validationSection);
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
        skippedManager.save();
        stage.close();
    }
}
