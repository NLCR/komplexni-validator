package nkp.pspValidator.gui;

import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.net.URL;

/**
 * Created by Martin Řehánek on 20.12.16.
 */
public abstract class AbstractDialog {

    final Stage stage;
    final Main main;
    /** kontroler naposledy otevřeného dialogu (pro SnapshotMode) */
    private DialogController controller;

    public AbstractDialog(Stage stage, Main main) {
        this.stage = stage;
        this.main = main;
    }

    /** Společné styly všech oken (barvy, kostra dialogu, tlačítka); vlastní CSS dialogu má přednost. */
    public static final String BASE_CSS = "/css/base.css";
    static final String BASE_MAC_CSS = "/css/base-mac.css";

    public abstract URL getFxmlResource();

    public abstract int getWidth();

    public abstract int getHeight();

    public abstract String getTitle();

    public abstract boolean isResizable();

    public abstract void setControllerData(DialogController controller);

    public void show() {
        prepare();
        if (!stage.isShowing()) {
            stage.showAndWait();
        }
    }

    /**
     * Otevře dialog bez blokování (pro SnapshotMode); vrátí použitý Stage.
     */
    public Stage showNonBlocking() {
        prepare();
        if (!stage.isShowing()) {
            stage.show();
        }
        return stage;
    }

    /** Kontroler dialogu po {@link #show()} nebo {@link #showNonBlocking()}, jinak null. */
    DialogController getController() {
        return controller;
    }

    /** Přidá společný base.css do scény (stylesheety uzlů v FXML mají přednost před stylesheety scény). */
    static void addBaseStylesheet(Scene scene) {
        URL css = AbstractDialog.class.getResource(BASE_CSS);
        if (css != null) {
            scene.getStylesheets().add(css.toExternalForm());
        }
        //na macOS JavaFX 17 nevykresli tucny rez systemoveho pisma, viz base-mac.css
        if (System.getProperty("os.name", "").toLowerCase().contains("mac")) {
            URL macCss = AbstractDialog.class.getResource(BASE_MAC_CSS);
            if (macCss != null) {
                scene.getStylesheets().add(macCss.toExternalForm());
            }
        }
    }

    private void prepare() {
        try {
            // FIXME: 10.4.18 nefunguje, u vetsiny dialogu vraci isResizable() false, presto se daji zvetsovat
            stage.setResizable(isResizable());
            stage.setTitle(getTitle());
            //set size; height is limited by the visible screen area so that bottom buttons stay reachable
            //(kv.maxDialogHeight is a dev hook to simulate a small screen, see SnapshotMode)
            Rectangle2D visualBounds = Screen.getPrimary().getVisualBounds();
            double maxHeight = visualBounds.getHeight();
            Integer devMaxHeight = Integer.getInteger("kv.maxDialogHeight");
            if (devMaxHeight != null) {
                maxHeight = Math.min(maxHeight, devMaxHeight);
            }
            double height = Math.min(getHeight(), maxHeight);
            stage.setWidth(getWidth());
            stage.setMinWidth(getWidth());
            stage.setHeight(height);
            stage.setMinHeight(Math.min(height, 400));
            //center in visible screen area
            stage.setX(visualBounds.getMinX() + (visualBounds.getWidth() - getWidth()) / 2);
            stage.setY(visualBounds.getMinY() + (visualBounds.getHeight() - height) / 2);

            FXMLLoader loader = new FXMLLoader(getFxmlResource());
            Parent root = (Parent) loader.load();
            Links.markExternalLinks(root);
            Scene scene = new Scene(root);
            addBaseStylesheet(scene);
            stage.setScene(scene);
            DialogController controller = loader.getController();
            this.controller = controller;
            stage.setOnCloseRequest(controller.getOnCloseEventHandler());
            controller.setMain(main);
            controller.setStage(stage);
            setControllerData(controller);
            controller.startNow();
        } catch (Exception e) {
            //should never happen
            throw new RuntimeException(e);
        }
    }
}
