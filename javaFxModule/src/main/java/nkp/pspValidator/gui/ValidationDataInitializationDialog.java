package nkp.pspValidator.gui;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Created by martin on 13.12.16.
 */
public class ValidationDataInitializationDialog extends AbstractDialog {

    public ValidationDataInitializationDialog(Stage stage, Main main) {
        super(stage, main);
    }

    public void show() {
        stage.setTitle("Inicializace validačních dat");
        int initialWidth = 1000;
        int initialHeight = 300;
        stage.setWidth(initialWidth);
        stage.setMinWidth(initialWidth);
        stage.setHeight(initialHeight);
        stage.setMinHeight(initialHeight);
        //controller
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/validationDataInitialization.fxml"));
            Parent root = (Parent) loader.load();
            stage.setScene(new Scene(root));
            ValidationDataInitializationDialogController controller = loader.getController();
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
