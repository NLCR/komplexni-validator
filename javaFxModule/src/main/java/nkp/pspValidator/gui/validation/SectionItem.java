package nkp.pspValidator.gui.validation;

/**
 * Created by Martin Řehánek on 16.12.16.
 */

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.image.ImageView;

import java.io.IOException;

public class SectionItem {

    @FXML
    private Node container;
    @FXML
    private Label name;
    @FXML
    private ProgressIndicator progressIndicator;
    @FXML
    private ImageView imgFinished;
    @FXML
    private ImageView imgCanceled;


    @FXML
    private Node errorsContainer;
    @FXML
    private Label errorsLabel;

    @FXML
    private Node warningsContainer;
    @FXML
    private Label warningsLabel;

    @FXML
    private Node infosContainer;
    @FXML
    private Label infosLabel;

    public SectionItem() {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/sectionItem.fxml"));
        fxmlLoader.setController(this);
        try {
            fxmlLoader.load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void populate(SectionWithState section) {
        name.setText(section.getName());
        switch (section.getState()) {
            case WAITING:
                progressIndicator.setVisible(false);
                imgFinished.setVisible(false);
                imgCanceled.setVisible(false);
                break;
            case RUNNING:
                progressIndicator.setVisible(true);
                imgFinished.setVisible(false);
                imgCanceled.setVisible(false);
                break;
            case FINISHED:
                progressIndicator.setVisible(false);
                imgFinished.setVisible(true);
                imgCanceled.setVisible(false);
                break;
            case CANCELED:
                progressIndicator.setVisible(false);
                imgFinished.setVisible(false);
                imgCanceled.setVisible(true);
                break;
        }

        infosLabel.setText(section.getInfos().toString());
        warningsLabel.setText(section.getWarnings().toString());
        errorsLabel.setText(section.getErrors().toString());
        infosContainer.setVisible(section.getInfos() != 0);
        warningsContainer.setVisible(section.getWarnings() != 0);
        errorsContainer.setVisible(section.getErrors() != 0);
    }

    public Node getContainer() {
        return container;
    }
}
