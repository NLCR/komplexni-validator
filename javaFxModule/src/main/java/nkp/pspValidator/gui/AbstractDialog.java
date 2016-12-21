package nkp.pspValidator.gui;

import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.net.URL;

/**
 * Created by martin on 20.12.16.
 */
public abstract class AbstractDialog {

    final Stage stage;
    final Main main;

    public AbstractDialog(Stage stage, Main main) {
        this.stage = stage;
        this.main = main;
    }

    abstract URL getFxmlResource();

    abstract int getWidth();

    abstract int getHeight();

    abstract String getTitle();

    abstract boolean isResizable();

    abstract void setControllerData(DialogController controller);

    public void show() {
        try {
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
