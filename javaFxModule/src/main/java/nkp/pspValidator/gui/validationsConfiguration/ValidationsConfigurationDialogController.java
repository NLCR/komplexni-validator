package nkp.pspValidator.gui.validationsConfiguration;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.text.Text;
import javafx.stage.WindowEvent;
import javafx.util.Callback;
import nkp.pspValidator.gui.DialogController;


/**
 * Created by Martin Řehánek on 9.4.18.
 */
public class ValidationsConfigurationDialogController extends DialogController {

    @FXML
    Text titleText;

    @FXML
    ListView<Section> sectionList;

    @FXML
    Button btnSave;

    private ValidationsConfiguration validationsConfiguration;

    //observables
    private ObservableList<Section> sectionsObservable;

    @Override
    public void startNow() {
        titleText.setText("Monografie 1.2");
        if (validationsConfiguration != null) {
            //init observables
            sectionsObservable = FXCollections.observableList(validationsConfiguration.getSections());

            //sections
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

        }
    }

    @Override
    public EventHandler<WindowEvent> getOnCloseEventHandler() {
        return null;
    }

    public void setValidationsConfiguration(ValidationsConfiguration validationsConfiguration) {
        this.validationsConfiguration = validationsConfiguration;
    }

    public void notifyDataEdited() {
        btnSave.setDisable(false);
    }

    public void closeDialog(ActionEvent actionEvent) {
        stage.close();
    }

    public void saveData(ActionEvent actionEvent) {
        DevValidationsConfigurationFactory.getInstance().setTestConfiguration(validationsConfiguration);
        stage.close();
    }
}
