package nkp.pspValidator.gui.exclusions;

/**
 * Created by Martin Řehánek on 16.12.16.
 */

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import nkp.pspValidator.gui.exclusions.data.ExcludedSection;

import java.io.IOException;

public class ExcludedSectionItem {

    @FXML
    private Node container;
    @FXML
    private CheckBox checkBox;
    @FXML
    private Label name;
    @FXML
    private Label description;

    private ExcludedSection excludedSection;

    private final ExclusionsConfigurationDialogController controller;


    public ExcludedSectionItem(ExclusionsConfigurationDialogController controller) {
        this.controller = controller;
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/exclusions/excludedSectionItem.fxml"));
        fxmlLoader.setController(this);
        try {
            fxmlLoader.load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void populate(ExcludedSection excludedSection) {
        this.excludedSection = excludedSection;
        updateViews();
    }

    private void updateViews() {
        name.setText(excludedSection.getName());
        if (excludedSection.getDescription() == null) {
            description.setVisible(false);
            description.setManaged(false);
        } else {
            description.setVisible(true);
            description.setManaged(true);
            description.setText(excludedSection.getDescription());
        }
        switch (excludedSection.getState()) {
            case ENABLED:
                checkBox.setSelected(true);
                break;
            case DISABLED:
                checkBox.setSelected(false);
                break;
        }
        checkBox.selectedProperty().addListener((observable, oldValue, newValue) -> {
            excludedSection.setState(newValue ? ExcludedSection.State.ENABLED : ExcludedSection.State.DISABLED);
            if (oldValue != newValue) {
                controller.notifyDataEdited();
            }
        });
    }

    public Node getContainer() {
        return container;
    }
}
