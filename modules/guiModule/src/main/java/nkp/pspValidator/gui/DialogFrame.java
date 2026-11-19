package nkp.pspValidator.gui;

import javafx.beans.DefaultProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * Společná kostra dialogu: bílá hlavička (volitelná ikona, nadpis a podtitul), obsah a bílá patička s volitelnou
 * poznámkou vlevo a tlačítky vpravo. Vzhled určují třídy z {@code /css/base.css} ({@code dialog-root},
 * {@code dialog-header}, {@code dialog-title}, {@code dialog-subtitle}, {@code dialog-content}, {@code dialog-footer}),
 * který načítá {@link AbstractDialog} do scény.
 * <p>
 * Použití v FXML jako kořen dialogu:
 * <pre>
 * &lt;?import nkp.pspValidator.gui.DialogFrame?&gt;
 * &lt;DialogFrame xmlns:fx="http://javafx.com/fxml" fx:controller="..." title="Nadpis" subtitle="Podtitul"
 *              footerNote="Poznámka vlevo v patičce"&gt;
 *     &lt;headerGraphic&gt;&lt;ImageView .../&gt;&lt;/headerGraphic&gt;            &lt;!-- volitelné --&gt;
 *     &lt;content&gt; ...uzly obsahu... &lt;/content&gt;
 *     &lt;footerButtons&gt; &lt;Button styleClass="button-secondary" .../&gt; &lt;Button styleClass="button-primary" .../&gt; &lt;/footerButtons&gt;
 * &lt;/DialogFrame&gt;
 * </pre>
 * Obsah je svislý box s třídou {@code dialog-content}; uzel, který má vyplnit zbylou výšku, dostane
 * {@code VBox.vgrow="ALWAYS"}.
 */
@DefaultProperty("content")
public class DialogFrame extends BorderPane {

    private final StringProperty title = new SimpleStringProperty(this, "title", "");
    private final StringProperty subtitle = new SimpleStringProperty(this, "subtitle", "");
    private final StringProperty footerNote = new SimpleStringProperty(this, "footerNote", "");

    private final HBox header = new HBox();
    private final VBox titleBox = new VBox(2);
    private final Label titleLabel = new Label();
    private final Label subtitleLabel = new Label();
    private final VBox content = new VBox();
    private final HBox footer = new HBox();
    private final Label footerNoteLabel = new Label();
    private final HBox footerButtons = new HBox(10);
    private Node headerGraphic;

    public DialogFrame() {
        getStyleClass().add("dialog-root");

        titleLabel.getStyleClass().add("dialog-title");
        titleLabel.textProperty().bind(title);
        subtitleLabel.getStyleClass().add("dialog-subtitle");
        subtitleLabel.setWrapText(true);
        subtitleLabel.textProperty().bind(subtitle);
        subtitleLabel.managedProperty().bind(subtitleLabel.textProperty().isNotEmpty());
        subtitleLabel.visibleProperty().bind(subtitleLabel.textProperty().isNotEmpty());
        titleBox.getChildren().addAll(titleLabel, subtitleLabel);
        titleBox.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("dialog-header");
        header.getChildren().add(titleBox);
        HBox.setHgrow(titleBox, Priority.ALWAYS);
        setTop(header);

        content.getStyleClass().add("dialog-content");
        setCenter(content);

        footerNoteLabel.getStyleClass().add("text-muted");
        footerNoteLabel.setWrapText(true);
        footerNoteLabel.textProperty().bind(footerNote);
        footerNoteLabel.managedProperty().bind(footerNoteLabel.textProperty().isNotEmpty());
        footerNoteLabel.visibleProperty().bind(footerNoteLabel.textProperty().isNotEmpty());
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        footerButtons.setAlignment(Pos.CENTER_RIGHT);
        footer.getStyleClass().add("dialog-footer");
        footer.getChildren().addAll(footerNoteLabel, spacer, footerButtons);
        setBottom(footer);
    }

    /** Uzly obsahu (výchozí vlastnost v FXML). */
    public ObservableList<Node> getContent() {
        return content.getChildren();
    }

    /** Tlačítka v patičce vpravo, v pořadí zleva doprava (primární akce poslední). */
    public ObservableList<Node> getFooterButtons() {
        return footerButtons.getChildren();
    }

    public StringProperty titleProperty() {
        return title;
    }

    public String getTitle() {
        return title.get();
    }

    public void setTitle(String value) {
        title.set(value);
    }

    public StringProperty subtitleProperty() {
        return subtitle;
    }

    public String getSubtitle() {
        return subtitle.get();
    }

    public void setSubtitle(String value) {
        subtitle.set(value);
    }

    public StringProperty footerNoteProperty() {
        return footerNote;
    }

    public String getFooterNote() {
        return footerNote.get();
    }

    public void setFooterNote(String value) {
        footerNote.set(value);
    }

    /** Volitelná ikona vlevo v hlavičce. */
    public Node getHeaderGraphic() {
        return headerGraphic;
    }

    public void setHeaderGraphic(Node graphic) {
        if (headerGraphic != null) {
            header.getChildren().remove(headerGraphic);
        }
        headerGraphic = graphic;
        if (graphic != null) {
            header.getChildren().add(0, graphic);
        }
    }
}
