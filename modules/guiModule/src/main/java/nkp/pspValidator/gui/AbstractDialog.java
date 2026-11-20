package nkp.pspValidator.gui;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.Window;

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

    /**
     * Spustí akci, až bude okno {@code window} úplně zavřené (tlačítkem, Esc i křížkem). Pro akce, které zavírají
     * vlastníka okna (dotaz nad Nastavením zavře Nastavení), místo {@code setOnHidden}.
     * <p>
     * Událost {@code WINDOW_HIDDEN} JavaFX posílá uprostřed skrývání: nativní okno je už skryté, ale scéna a nativní okno
     * se uvolní až po obsluhách. Zavře-li obsluha vlastníka, Windows s ním zruší i vlastněná nativní okna, takže
     * dokončení skrytí (odpojení scény) hodí {@code IllegalStateException: The window has already been closed}
     * a scéna dotazu zůstane registrovaná u neexistujícího okna. Při jejím dalším vykreslení se výjimka opakuje
     * v {@code GlassScene.releaseSynchronization} dřív, než se uvolní zámek vykreslování ({@code ViewPainter.renderLock}),
     * vlákno JavaFX si ho ponechá a vykreslovací vlákno na něm navždy čeká: celé GUI zamrzne. Posluchač vlastnosti
     * {@code showing} se volá až po úplném zavření okna (uvolnění scény i nativního okna), tam už zavření vlastníka
     * nevadí. Na macOS se vlastněná okna se zavřením vlastníka nativně neruší, proto se to tam neprojeví.
     */
    public static void whenClosed(Window window, Runnable action) {
        window.showingProperty().addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                if (!newValue) {
                    observable.removeListener(this);
                    action.run();
                }
            }
        });
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
