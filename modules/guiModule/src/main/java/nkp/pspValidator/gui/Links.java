package nkp.pspValidator.gui;

import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.stage.WindowEvent;
import nkp.pspValidator.gui.desktop.FileManager;

import java.io.File;
import java.util.function.Supplier;

/**
 * Společné prvky odkazů: značka odkazu ven z aplikace (web), ikona a akce „Zobrazit ve složce“ u odkazu na soubor,
 * otevření adresáře ve správci souborů a bublina u položky menu. Tvary ikon jsou SVG cesty v base.css (třídy
 * {@code external-link-icon}, {@code folder-icon}, {@code reveal-link}).
 */
public final class Links {

    /** Mezera mezi odkazem na soubor a ikonou „Zobrazit ve složce“. */
    private static final double REVEAL_LINK_GAP = 4;

    /** Text bubliny u tlačítka „Otevřít“ u adresáře. */
    public static final String OPEN_DIRECTORY_TOOLTIP = "Otevře adresář ve správci souborů";

    /** Text bubliny u akce „Zobrazit ve složce“. */
    public static final String REVEAL_TOOLTIP = "Zobrazit ve složce (ve správci souborů)";

    private Links() {
    }

    /** Malá ikona „šipka ven z rámečku“: odkaz vede na web mimo aplikaci. */
    public static Region externalLinkIcon() {
        Region icon = new Region();
        icon.getStyleClass().add("external-link-icon");
        return icon;
    }

    public static Region folderIcon() {
        Region icon = new Region();
        icon.getStyleClass().add("folder-icon");
        return icon;
    }

    /** Třída odkazu na web; v FXML stačí ji uvést, značku doplní {@link #markExternalLinks(Parent)}. */
    public static final String EXTERNAL_LINK_CLASS = "external-link";

    /** Odkaz na web: značka za textem. */
    public static Hyperlink markExternal(Hyperlink link) {
        if (!link.getStyleClass().contains(EXTERNAL_LINK_CLASS)) {
            link.getStyleClass().add(EXTERNAL_LINK_CLASS);
        }
        link.setGraphic(externalLinkIcon());
        link.setContentDisplay(ContentDisplay.RIGHT);
        link.setGraphicTextGap(4);
        return link;
    }

    /** Doplní značku všem odkazům s třídou {@value #EXTERNAL_LINK_CLASS} (po načtení FXML). */
    public static void markExternalLinks(Parent root) {
        for (Node node : root.lookupAll("." + EXTERNAL_LINK_CLASS)) {
            if (node instanceof Hyperlink && ((Hyperlink) node).getGraphic() == null) {
                markExternal((Hyperlink) node);
            }
        }
    }

    /** Položka menu vedoucí na web: značka jako ikona položky. */
    public static MenuItem markExternal(MenuItem item) {
        item.setGraphic(externalLinkIcon());
        return item;
    }

    /** Položka menu „Zobrazit ve složce“ s ikonou složky. */
    public static MenuItem revealMenuItem(String text, Supplier<File> file) {
        MenuItem item = new MenuItem(text, folderIcon());
        item.setOnAction(e -> reveal(file.get()));
        return item;
    }

    /** Odkaz jen s ikonou složky: ukáže soubor ve správci souborů. */
    public static Hyperlink revealLink(Supplier<File> file) {
        Hyperlink link = new Hyperlink();
        link.setGraphic(folderIcon());
        link.getStyleClass().add("reveal-link");
        link.setTooltip(new Tooltip(REVEAL_TOOLTIP));
        link.setFocusTraversable(false);
        link.setMinWidth(Region.USE_PREF_SIZE);
        link.setOnAction(e -> reveal(file.get()));
        return link;
    }

    /**
     * Vloží za odkaz na soubor odkaz „Zobrazit ve složce“; je vidět, jen když je vidět odkaz na soubor.
     *
     * @return vložený odkaz
     */
    public static Hyperlink addRevealLinkAfter(Hyperlink fileLink, Supplier<File> file) {
        Hyperlink reveal = revealLink(file);
        reveal.visibleProperty().bind(fileLink.visibleProperty());
        reveal.managedProperty().bind(fileLink.managedProperty());
        reveal.disableProperty().bind(fileLink.disableProperty());
        Parent parent = fileLink.getParent();
        if (!(parent instanceof Pane)) {
            throw new IllegalStateException("odkaz na soubor musí být v Pane");
        }
        ObservableList<Node> children = ((Pane) parent).getChildren();
        children.add(children.indexOf(fileLink) + 1, reveal);
        if (parent instanceof HBox) {
            //ikona blize ke svemu odkazu nez k dalsimu odkazu v rade (mezera rady se nastavuje v CSS)
            HBox row = (HBox) parent;
            Runnable margin = () -> HBox.setMargin(reveal, new Insets(0, 0, 0, Math.min(0, REVEAL_LINK_GAP - row.getSpacing())));
            margin.run();
            row.spacingProperty().addListener(o -> margin.run());
        }
        return reveal;
    }

    /** Ukáže soubor ve správci souborů (Finder, Průzkumník); null nic nedělá. */
    public static void reveal(File file) {
        if (file != null) {
            FileManager.revealFile(file, null);
        }
    }

    /** Otevře adresář ve správci souborů (ne v prohlížeči); null nic nedělá. */
    public static void openDirectory(File dir) {
        if (dir != null) {
            FileManager.openDirectory(dir, null);
        }
    }

    /**
     * Bublina u položky menu (JavaFX ji u MenuItem nepodporuje): po zobrazení menu se nainstaluje na uzel položky.
     */
    public static void installTooltip(MenuItem item, String text) {
        Tooltip tooltip = new Tooltip(text);
        tooltip.setWrapText(true);
        tooltip.setMaxWidth(420);
        Runnable install = () -> {
            Node node = item.getStyleableNode();
            if (node != null) {
                Tooltip.install(node, tooltip);
            }
        };
        Menu parentMenu = item.getParentMenu();
        if (parentMenu != null) {
            parentMenu.addEventHandler(Menu.ON_SHOWN, e -> install.run());
        } else {
            ContextMenu popup = item.getParentPopup();
            if (popup == null) {
                throw new IllegalStateException("položka musí být v menu");
            }
            popup.addEventHandler(WindowEvent.WINDOW_SHOWN, e -> install.run());
        }
    }
}
