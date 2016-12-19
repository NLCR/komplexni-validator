package nkp.pspValidator.gui.validation;

/**
 * Created by martin on 16.12.16.
 */

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.image.ImageView;

import java.io.IOException;

public class RuleItem {

    @FXML
    private Node container;
    @FXML
    private Label name;
    @FXML
    private ProgressIndicator progressIndicator;
    @FXML
    private ImageView imgFinished;


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

    public RuleItem() {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/ruleItem.fxml"));
        fxmlLoader.setController(this);
        try {
            fxmlLoader.load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void populate(RuleWithState rule) {
        name.setText(rule.getName());
        switch (rule.getState()) {
            case WAITING:
                imgFinished.setVisible(false);
                progressIndicator.setVisible(false);
                break;
            case RUNNING:
                imgFinished.setVisible(false);
                progressIndicator.setVisible(true);
                break;
            case FINISHED:
                imgFinished.setVisible(true);
                progressIndicator.setVisible(false);
                break;
        }

        infosLabel.setText(rule.getInfos().toString());
        warningsLabel.setText(rule.getWarnings().toString());
        errorsLabel.setText(rule.getErrors().toString());
        infosContainer.setVisible(rule.getInfos() != 0);
        warningsContainer.setVisible(rule.getWarnings() != 0);
        errorsContainer.setVisible(rule.getErrors() != 0);
    }

    public Node getContainer() {
        return container;
    }
}
