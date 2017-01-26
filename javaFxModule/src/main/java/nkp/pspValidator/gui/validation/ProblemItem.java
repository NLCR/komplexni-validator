package nkp.pspValidator.gui.validation;

/**
 * Created by Martin Řehánek on 16.12.16.
 */

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
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

public class ProblemItem {

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

    public ProblemItem() {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/problemItem.fxml"));
        fxmlLoader.setController(this);
        try {
            fxmlLoader.load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @FXML
    public void initialize() {
        final Tooltip tooltip = new Tooltip();
        tooltip.setText("Kopírovat chybu do schránky");
        btnCopyToClipboard.setTooltip(tooltip);
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

    public Node getContainer() {
        return container;
    }

    public void copyToClipboard(ActionEvent actionEvent) {
        String text = message.getText();
        Clipboard clipboard = Clipboard.getSystemClipboard();
        ClipboardContent content = new ClipboardContent();
        content.putString(text);
        clipboard.setContent(content);
    }

    public Text getMessage() {
        return message;
    }

    public void bindTextWidthToListWidth(ListView<ValidationProblem> list) {
        //max. velikost sirka Text (z wrappingWidth) je sirka ListView - 250 px
        // (priblizne soucet sirek oblasti s ikonou vlevo a s tlacitkem vpravo)
        message.wrappingWidthProperty().bind(list.widthProperty().subtract(250));
        //nefunguje
        //message.wrappingWidthProperty().bind(list.widthProperty().subtract(iconPane.widthProperty()).subtract(buttonPane.widthProperty()));
        //TODO: nemuze tady byt memory leak kvuli bindovani zahazovanych ProblemItem na ListView?
        //mozna v nejakem destruktoru odbindovat
    }
}
