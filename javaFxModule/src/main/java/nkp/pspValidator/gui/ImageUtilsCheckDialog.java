package nkp.pspValidator.gui;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Created by martin on 13.12.16.
 */
public class ImageUtilsCheckDialog extends AbstractDialog {

    private final boolean closeWhenFinished;
    private final String mainButtonText;

    public ImageUtilsCheckDialog(Stage stage, Main main, boolean closeWhenFinished, String mainButtonText) {
        super(stage, main);
        this.closeWhenFinished = closeWhenFinished;
        this.mainButtonText = mainButtonText;
    }

    public void show() {
        stage.setTitle("Kontrola dostupnosti nástrojů pro validaci obrázků");
        int initialWidth = 700;
        int initialHeight = 450;

        stage.setWidth(initialWidth);
        stage.setMinWidth(initialWidth);
        stage.setHeight(initialHeight);
        stage.setMinHeight(initialHeight);

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/imageUtilsCheck.fxml"));
            Parent root = (Parent) loader.load();
            stage.setScene(new Scene(root));
            ImageUtilsCheckDialogController controller = loader.getController();
            stage.setOnCloseRequest(controller.getOnCloseEventHandler());
            controller.setMain(main);
            controller.setStage(stage);
            controller.setData(closeWhenFinished, mainButtonText);
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
