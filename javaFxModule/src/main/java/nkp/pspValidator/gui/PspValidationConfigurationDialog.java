package nkp.pspValidator.gui;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Created by martin on 13.12.16.
 */
public class PspValidationConfigurationDialog {

    private final Stage stage;
    private final Main main;

    /*public PspValidationConfigurationDialog(Window window, MainController mainController, ConfigurationManager configurationManager, ValidationDataManager validationDataManager) {
        setTitle("Nastavení validace PSP balíku");
        int initialWidth = 650;
        int initialHeight = 300;

        setWidth(initialWidth);
        setMinWidth(initialWidth);
        setHeight(initialHeight);
        setMinHeight(initialHeight);

        initStyle(StageStyle.UTILITY);
        initModality(Modality.WINDOW_MODAL);
        initOwner(window);

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/pspValidationConfigurationDialog.fxml"));
            Parent root = (Parent) loader.load();
            PspValidationConfigurationDialogController controller = loader.getController();
            controller.setMainController(mainController);
            controller.setConfigurationManager(configurationManager);
            controller.setValidationDataManager(validationDataManager);
            controller.setStage(this);
            setScene(new Scene(root));
        } catch (IOException e) {
            //should never happen
            throw new RuntimeException(e);
        }
    }*/

    public PspValidationConfigurationDialog(Stage stage, Main main) {
        this.stage = stage;
        this.main = main;
    }

    public void show() {
        stage.setTitle("Nastavení validace PSP balíku");
        int initialWidth = 650;
        int initialHeight = 300;

        stage.setWidth(initialWidth);
        stage.setMinWidth(initialWidth);
        stage.setHeight(initialHeight);
        stage.setMinHeight(initialHeight);

        /*initStyle(StageStyle.UTILITY);
        initModality(Modality.WINDOW_MODAL);
        initOwner(window);*/

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/pspValidationConfigurationDialog.fxml"));
            Parent root = (Parent) loader.load();
            stage.setScene(new Scene(root));
            PspValidationConfigurationDialogController controller = loader.getController();
            controller.setMain(main);
            controller.setStage(stage);

            /*controller.setConfigurationManager(configurationManager);
            controller.setValidationDataManager(validationDataManager);
            controller.setStage(this);
            setScene(new Scene(root));*/
        } catch (IOException e) {
            //should never happen
            throw new RuntimeException(e);
        }
        //stage.show();
        stage.showAndWait();
    }
}
