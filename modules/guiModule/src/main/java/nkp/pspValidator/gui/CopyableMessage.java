package nkp.pspValidator.gui;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.text.Text;
import nkp.pspValidator.gui.validation.ProblemCell;

/**
 * Hláška (typicky chybová), kterou jde označit a zkopírovat: text v needitovatelném poli bez rámečku, vedle tlačítko
 * „Kopírovat“ (zkopíruje celý text). Výška pole se přizpůsobí zalomenému textu, nejvýš {@link #MAX_TEXT_HEIGHT},
 * delší text roluje. Použití i z FXML ({@code <CopyableMessage fx:id="..." error="true"/>}). Vzhled v base.css
 * (třídy {@code copyable-message}, {@code copyable-message-text}, s {@code error} červený text).
 */
public class CopyableMessage extends HBox {

    /** Nejvyšší výška textu, pak pole roluje. */
    static final double MAX_TEXT_HEIGHT = 160;
    /** Vnitřní okraje pole (shodné s base.css, třída copyable-message-text) a rezerva na posuvník. */
    private static final double TEXT_PADDING = 4;
    private static final double SCROLLBAR_RESERVE = 16;
    /** Rezerva výšky na vnitřní okraje posuvné plochy pole (bez ní se ukáže svislý posuvník). */
    private static final double HEIGHT_SLACK = 10;

    private final TextArea area = new TextArea();
    private final Button copy = new Button("Kopírovat");

    public CopyableMessage() {
        getStyleClass().add("copyable-message");
        setSpacing(10);
        setAlignment(Pos.TOP_LEFT);
        setMaxWidth(Double.MAX_VALUE);
        setMinHeight(Region.USE_PREF_SIZE);

        area.setEditable(false);
        area.setWrapText(true);
        area.getStyleClass().add("copyable-message-text");
        area.setMinHeight(Region.USE_PREF_SIZE);
        area.setMaxHeight(Region.USE_PREF_SIZE);
        area.setMinWidth(0);
        HBox.setHgrow(area, Priority.ALWAYS);
        area.textProperty().addListener((obs, oldValue, newValue) -> updateHeight());
        area.widthProperty().addListener((obs, oldValue, newValue) -> updateHeight());

        copy.getStyleClass().add("button-secondary");
        copy.setMinWidth(Region.USE_PREF_SIZE);
        copy.setTooltip(new Tooltip("Zkopíruje celý text hlášky do schránky."));
        copy.setOnAction(e -> ProblemCell.copyToClipboard(getText()));

        getChildren().addAll(area, copy);
        updateHeight();
    }

    public CopyableMessage(String text, boolean error) {
        this();
        setError(error);
        setText(text);
    }

    public String getText() {
        return area.getText();
    }

    /** Text hlášky; null nebo prázdný schová tlačítko Kopírovat. */
    public void setText(String text) {
        area.setText(text == null ? "" : text);
        boolean hasText = text != null && !text.isEmpty();
        copy.setVisible(hasText);
        copy.setManaged(hasText);
    }

    public boolean isError() {
        return area.getStyleClass().contains("error");
    }

    /** Červený text chybové hlášky. */
    public void setError(boolean error) {
        area.getStyleClass().remove("error");
        if (error) {
            area.getStyleClass().add("error");
        }
    }

    /** Pro SnapshotMode a kontroly: tlačítko Kopírovat je vidět. */
    boolean isCopyShown() {
        return copy.isVisible();
    }

    private void updateHeight() {
        double width = area.getWidth();
        Text measure = new Text(area.getText().isEmpty() ? " " : area.getText());
        measure.setFont(area.getFont());
        if (width > 0) {
            measure.setWrappingWidth(Math.max(10, width - 2 * TEXT_PADDING - SCROLLBAR_RESERVE));
        }
        double height = Math.ceil(measure.getLayoutBounds().getHeight()) + 2 * TEXT_PADDING + HEIGHT_SLACK;
        area.setPrefHeight(Math.min(MAX_TEXT_HEIGHT, height));
    }
}
