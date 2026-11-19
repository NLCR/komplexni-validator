package nkp.pspValidator.gui.validation;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Tooltip;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import nkp.pspValidator.shared.engine.validationFunctions.ValidationProblem;

/**
 * Řádek seznamu problémů vybraného pravidla: štítek úrovně (všechny stejně široké), celé znění problému
 * (zalamované) a tlačítko „Kopírovat“ ve vlastním sloupci vpravo, které se ukáže při najetí myší nebo u vybraného
 * řádku (base.css). Sloupec tlačítka je vyhrazený i u skrytého tlačítka, takže tlačítko nikdy nepřekryje text.
 */
public class ProblemCell extends ListCell<ValidationProblem> {

    private static final String[] LEVEL_CLASSES = {"level-error", "level-warning", "level-info"};

    private final StackPane row = new StackPane();
    private final HBox content = new HBox();
    private final Label level = new Label();
    private final Label message = new Label();
    private final Button copyButton = new Button("Kopírovat");

    public ProblemCell(ListView<ValidationProblem> list) {
        setPrefWidth(0);
        row.getStyleClass().add("problem-row");
        content.getStyleClass().add("problem-content");
        content.setAlignment(Pos.TOP_LEFT);
        level.getStyleClass().add("level-badge");
        message.getStyleClass().add("problem-message");
        message.setWrapText(true);
        message.setMinHeight(Region.USE_PREF_SIZE);
        //sirka zpravy podle seznamu (bez stitku, tlacitka, mezer, odsazeni a posuvniku), jinak se text nezalomi
        message.prefWidthProperty().bind(list.widthProperty().subtract(level.widthProperty())
                .subtract(copyButton.widthProperty()).subtract(70));
        message.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(message, Priority.ALWAYS);
        copyButton.getStyleClass().addAll("button-secondary", "copy-button");
        copyButton.setMinWidth(Region.USE_PREF_SIZE);
        copyButton.setTooltip(new Tooltip("Kopírovat problém do schránky"));
        copyButton.setFocusTraversable(false);
        copyButton.setOnAction(e -> {
            if (getItem() != null) {
                copyToClipboard(getItem().getFullMessage());
            }
        });
        content.getChildren().addAll(level, message, copyButton);
        row.getChildren().add(content);
    }

    @Override
    protected void updateItem(ValidationProblem problem, boolean empty) {
        super.updateItem(problem, empty);
        setText(null);
        if (empty || problem == null) {
            setGraphic(null);
        } else {
            level.setText(problem.getLevel().name());
            level.getStyleClass().removeAll(LEVEL_CLASSES);
            level.getStyleClass().add("level-" + problem.getLevel().name().toLowerCase());
            message.setText(problem.getFullMessage());
            setGraphic(row);
        }
    }

    public static void copyToClipboard(String text) {
        ClipboardContent content = new ClipboardContent();
        content.putString(text);
        Clipboard.getSystemClipboard().setContent(content);
    }
}
