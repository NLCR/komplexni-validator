package nkp.pspValidator.gui;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Created by martin on 13.12.16.
 */
public class ImageUtilsCheckDialog {

    private final Stage stage;
    private final Main main;

    public ImageUtilsCheckDialog(Stage stage, Main main) {
        this.stage = stage;
        this.main = main;
    }

    public void show() {
        stage.setTitle("Kontrola dostupnosti nástrojů pro validaci obrázků");
        int initialWidth = 700;
        int initialHeight = 600;

        stage.setWidth(initialWidth);
        stage.setMinWidth(initialWidth);
        stage.setHeight(initialHeight);
        stage.setMinHeight(initialHeight);

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/imageUtilsCheck.fxml"));
            Parent root = (Parent) loader.load();
            stage.setScene(new Scene(root));
            ImageUtilsCheckDialogController controller = loader.getController();
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
