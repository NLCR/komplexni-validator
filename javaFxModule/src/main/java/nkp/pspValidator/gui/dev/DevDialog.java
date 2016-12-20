package nkp.pspValidator.gui.dev;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import nkp.pspValidator.gui.Main;

import java.io.IOException;

/**
 * Created by Lenovo on 19.12.2016.
 */
public class DevDialog {

    private final Stage stage;
    private final Main main;

    public DevDialog(Stage stage, Main main) {
        this.stage = stage;
        this.main = main;
    }

    public void show() {
        stage.setTitle("Test");
        int initialWidth = 650;
        int initialHeight = 450;

        stage.setWidth(initialWidth);
        stage.setMinWidth(initialWidth);
        stage.setHeight(initialHeight);
        stage.setMinHeight(initialHeight);

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/devDialog.fxml"));
            Parent root = (Parent) loader.load();
            stage.setScene(new Scene(root));
            DevDialogController controller = loader.getController();
            stage.setOnCloseRequest(controller.getOnCloseEventHandler());
            controller.setMain(main);
            controller.setStage(stage);
            controller.startNow();
        } catch (IOException e) {
            //should never happen
            throw new RuntimeException(e);
        }
        if (!stage.isShowing()) {
            stage.showAndWait();
        }
    }
}
