package nkp.pspValidator.gui.validationsConfiguration;

/**
 * Created by Martin Řehánek on 16.12.16.
 */

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;

import java.io.IOException;

public class SectionItem {

    @FXML
    private Node container;
    @FXML
    private CheckBox checkBox;
    @FXML
    private Label name;
    @FXML
    private Label description;

    private Section section;

    private final ValidationsConfigurationDialogController controller;


    public SectionItem(ValidationsConfigurationDialogController controller) {
        this.controller = controller;
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/validationsConfiguration/sectionItem.fxml"));
        fxmlLoader.setController(this);
        try {
            fxmlLoader.load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void populate(Section section) {
        this.section = section;
        updateViews();
    }

    private void updateViews() {
        name.setText(section.getName());
        if (section.getDescription() == null) {
            description.setVisible(false);
        } else {
            description.setVisible(true);
            description.setText(section.getDescription());
        }
        switch (section.getState()) {
            case ENABLED:
                checkBox.setSelected(true);
                break;
            case DISABLED:
                checkBox.setSelected(false);
                break;
        }
        checkBox.selectedProperty().addListener((observable, oldValue, newValue) -> {
            section.setState(newValue ? Section.State.ENABLED : Section.State.DISABLED);
            if (oldValue != newValue) {
                controller.notifyDataEdited();
            }
        });
    }

    public Node getContainer() {
        return container;
    }
}
