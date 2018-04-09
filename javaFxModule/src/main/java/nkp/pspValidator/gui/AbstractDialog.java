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

    public AbstractDialog(Stage stage, Main main) {
        this.stage = stage;
        this.main = main;
    }

    public abstract URL getFxmlResource();

    public abstract int getWidth();

    public abstract int getHeight();

    public abstract String getTitle();

    public abstract boolean isResizable();

    public abstract void setControllerData(DialogController controller);

    public void show() {
        try {
            // FIXME: 10.4.18 nefunguje, u vetsiny dialogu vraci isResizable() false, presto se daji zvetsovat
            stage.setResizable(isResizable());
            stage.setTitle(getTitle());
            //set size
            stage.setWidth(getWidth());
            stage.setMinWidth(getWidth());
            stage.setHeight(getHeight());
            stage.setMinHeight(getHeight());
            //center in screen
            Rectangle2D visualBounds = Screen.getPrimary().getVisualBounds();
            stage.setX((visualBounds.getMaxX() - getWidth()) / 2);
            stage.setY((visualBounds.getMaxY() - getHeight()) / 2);

            FXMLLoader loader = new FXMLLoader(getFxmlResource());
            Parent root = (Parent) loader.load();
            stage.setScene(new Scene(root));
            DialogController controller = loader.getController();
            stage.setOnCloseRequest(controller.getOnCloseEventHandler());
            controller.setMain(main);
            controller.setStage(stage);
            setControllerData(controller);
            controller.startNow();
        } catch (Exception e) {
            //should never happen
            throw new RuntimeException(e);
        }
        if (!stage.isShowing()) {
            stage.showAndWait();
        }
    }
}
