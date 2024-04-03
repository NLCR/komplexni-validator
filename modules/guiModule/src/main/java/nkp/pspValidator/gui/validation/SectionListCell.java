package nkp.pspValidator.gui.validation;

/**
 * Created by Martin Řehánek on 16.12.16.
 */

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.image.ImageView;

import java.io.IOException;

public class SectionListCell extends ListCell<SectionWithState> {

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


    private FXMLLoader mLLoader;


    @Override
    protected void updateItem(SectionWithState section, boolean empty) {
        super.updateItem(section, empty);
        if (empty || section == null) {
            setGraphic(null);
        } else {
            if (mLLoader == null) {
                mLLoader = new FXMLLoader(getClass().getResource("/fxml/sectionItem.fxml"));
                mLLoader.setController(this);

                try {
                    mLLoader.load();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
            populate(section);
            setGraphic(container);
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
            case SKIPPED:
                progressIndicator.setVisible(false);
                imgFinished.setVisible(false);
                imgCanceled.setVisible(false);
                break;
        }
        infosLabel.setText(section.getInfos().toString());
        warningsLabel.setText(section.getWarnings().toString());
        errorsLabel.setText(section.getErrors().toString());
        infosContainer.setVisible(section.getInfos() != 0);
        warningsContainer.setVisible(section.getWarnings() != 0);
        errorsContainer.setVisible(section.getErrors() != 0);
    }

}
