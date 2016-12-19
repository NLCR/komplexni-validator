package nkp.pspValidator.gui;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Created by martin on 13.12.16.
 */
public class ValidationResultSummaryDialog {

    private final Stage stage;
    private final Main main;

    public ValidationResultSummaryDialog(Stage stage, Main main) {
        this.stage = stage;
        this.main = main;
    }

    public void show(ValidationResultSummary summary) {
        stage.setTitle("Sumarizace výsledků validace");
        int initialWidth = 700;
        int initialHeight = 400;
        stage.setWidth(initialWidth);
        stage.setMinWidth(initialWidth);
        stage.setHeight(initialHeight);
        stage.setMinHeight(initialHeight);
        //controller
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/validationResulSummaryDialog.fxml"));
            Parent root = (Parent) loader.load();
            stage.setScene(new Scene(root));
            ValidationResultsSummaryDialogController controller = loader.getController();
            stage.setOnCloseRequest(controller.getOnCloseEventHandler());
            controller.setMain(main);
            controller.setStage(stage);
            controller.setData(summary);
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
