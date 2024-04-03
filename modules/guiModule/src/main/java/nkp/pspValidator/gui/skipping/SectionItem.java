package nkp.pspValidator.gui.skipping;

/**
 * Created by Martin Řehánek on 16.12.16.
 */

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import nkp.pspValidator.shared.engine.RulesSection;

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

    private RulesSection section;

    private final SkippingConfigurationDialogController controller;


    public SectionItem(SkippingConfigurationDialogController controller) {
        this.controller = controller;
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/skipping/sectionItem.fxml"));
        fxmlLoader.setController(this);
        try {
            fxmlLoader.load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void populate(RulesSection section) {
        this.section = section;
        updateViews();
    }

    private void updateViews() {
        name.setText(section.getName());
        if (section.getDescription() == null) {
            description.setVisible(false);
            description.setManaged(false);
        } else {
            description.setVisible(true);
            description.setManaged(true);
            description.setText(section.getDescription());
        }
        checkBox.setSelected(section.isEnabled());
        checkBox.selectedProperty().addListener((observable, oldValue, newValue) -> {
            section.setEnabled(newValue);
            if (oldValue != newValue) {
                controller.notifyDataEdited();
            }
        });
    }

    public Node getContainer() {
        return container;
    }
}
