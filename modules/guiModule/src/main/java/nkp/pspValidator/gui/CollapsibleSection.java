package nkp.pspValidator.gui;

import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.function.Consumer;

/**
 * Sbalitelná sekce: hlavička (šipka, nadpis, souhrn, odkaz vpravo) a obsah, který je vidět jen v rozbaleném stavu.
 * Kliknutí na hlavičku nebo odkaz sekci přepne; o tom, které sekce jsou rozbalené, rozhoduje volající přes
 * {@link #setOnToggle(Consumer)} (v dialogu Nová validace je rozbalená vždy nejvýš jedna). Vzhled určují třídy
 * {@code collapsible-section}, {@code collapsible-header}, {@code collapsible-title}, {@code collapsible-body},
 * {@code chevron} a {@code expanded} z base.css; sekce se vkládají do VBox s třídou {@code collapsible-group}.
 */
public class CollapsibleSection extends VBox {

    private final Label summaryLabel = new Label();
    private final Hyperlink actionLink = new Hyperlink();
    private final VBox body = new VBox();
    private final String collapsedLinkText;
    private boolean expanded = false;
    private Consumer<CollapsibleSection> onToggle;

    public CollapsibleSection(String title, String collapsedLinkText) {
        this.collapsedLinkText = collapsedLinkText;
        getStyleClass().add("collapsible-section");

        Region chevron = new Region();
        chevron.getStyleClass().add("chevron");
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("collapsible-title");
        titleLabel.setMinWidth(Region.USE_PREF_SIZE);
        summaryLabel.getStyleClass().add("text-muted");
        summaryLabel.setMinWidth(0);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        actionLink.getStyleClass().add("link");
        actionLink.setMinWidth(Region.USE_PREF_SIZE);
        actionLink.setOnAction(e -> toggle());
        //klik na odkaz uz sekci prepnul pres onAction, do hlavicky se nesmi dostat podruhe
        actionLink.addEventHandler(MouseEvent.MOUSE_CLICKED, MouseEvent::consume);
        HBox header = new HBox(chevron, titleLabel, summaryLabel, spacer, actionLink);
        header.getStyleClass().add("collapsible-header");
        header.setOnMouseClicked(e -> toggle());

        body.getStyleClass().add("collapsible-body");
        getChildren().addAll(header, body);
        setExpanded(false);
    }

    private void toggle() {
        if (onToggle != null) {
            onToggle.accept(this);
        } else {
            setExpanded(!expanded);
        }
    }

    /** Volá se po kliknutí na hlavičku; bez obsluhy se sekce jen přepne. */
    public void setOnToggle(Consumer<CollapsibleSection> onToggle) {
        this.onToggle = onToggle;
    }

    public boolean isExpanded() {
        return expanded;
    }

    public void setExpanded(boolean expanded) {
        this.expanded = expanded;
        body.setVisible(expanded);
        body.setManaged(expanded);
        actionLink.setText(expanded ? "Sbalit" : collapsedLinkText);
        if (expanded) {
            if (!getStyleClass().contains("expanded")) {
                getStyleClass().add("expanded");
            }
        } else {
            getStyleClass().remove("expanded");
        }
    }

    public void setSummary(String summary) {
        summaryLabel.setText(summary);
    }

    public String getSummary() {
        return summaryLabel.getText();
    }

    /** Obsah rozbalené sekce. */
    public VBox getBody() {
        return body;
    }
}
