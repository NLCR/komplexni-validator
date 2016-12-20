package nkp.pspValidator.gui;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
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

    abstract void setControllerData(DialogController controller);

    public void show() {
        try {
            stage.setTitle(getTitle());
            //width, height
            stage.setWidth(getWidth());
            stage.setMinWidth(getWidth());
            stage.setHeight(getHeight());
            stage.setMinHeight(getHeight());

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
