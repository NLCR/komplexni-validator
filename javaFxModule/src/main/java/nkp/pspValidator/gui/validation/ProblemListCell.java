package nkp.pspValidator.gui.validation;

/**
 * Created by Martin Řehánek on 16.12.16.
 */

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;
import nkp.pspValidator.shared.engine.validationFunctions.ValidationProblem;

import java.io.IOException;

public class ProblemListCell extends ListCell<ValidationProblem> {

    @FXML
    private Node container;

    @FXML
    private StackPane iconPane;

    @FXML
    private ImageView imgInfo;
    @FXML
    private ImageView imgError;
    @FXML
    private ImageView imgWarning;

    @FXML
    private Text message;

    @FXML
    Pane buttonPane;

    @FXML
    private Button btnCopyToClipboard;

    private FXMLLoader mLLoader;

    private final ListView<ValidationProblem> containingList;

    public ProblemListCell(ListView<ValidationProblem> containingList) {
        this.containingList = containingList;
    }

    @Override
    protected void updateItem(ValidationProblem problem, boolean empty) {
        super.updateItem(problem, empty);
        if (empty || problem == null) {
            setGraphic(null);
        } else {
            if (mLLoader == null) {
                mLLoader = new FXMLLoader(getClass().getResource("/fxml/problemItem.fxml"));
                mLLoader.setController(this);

                try {
                    mLLoader.load();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            populate(problem);
            setGraphic(container);
        }
    }

    @FXML
    public void initialize() {
        final Tooltip tooltip = new Tooltip();
        tooltip.setText("Kopírovat chybu do schránky");
        btnCopyToClipboard.setTooltip(tooltip);
        //see http://stackoverflow.com/questions/29884894/how-to-make-listview-width-fit-width-of-its-cells#
        message.wrappingWidthProperty().bind(containingList.widthProperty().subtract(250));
        //TODO: nemuze tady byt memory leak kvuli bindovani zahazovanych ProblemListCell na ListView?
        //mozna v nejakem destruktoru odbindovat
    }

    public void populate(ValidationProblem problem) {
        message.setText(problem.getMessage());
        switch (problem.getLevel()) {
            case ERROR:
                imgError.setVisible(true);
                imgWarning.setVisible(false);
                imgInfo.setVisible(false);
                break;
            case WARNING:
                imgError.setVisible(false);
                imgWarning.setVisible(true);
                imgInfo.setVisible(false);
                break;

            case INFO:
                imgError.setVisible(false);
                imgWarning.setVisible(false);
                imgInfo.setVisible(true);
                break;
        }
    }

    public void copyToClipboard(ActionEvent actionEvent) {
        String text = message.getText();
        Clipboard clipboard = Clipboard.getSystemClipboard();
        ClipboardContent content = new ClipboardContent();
        content.putString(text);
        clipboard.setContent(content);
    }

}
