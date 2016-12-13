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
    private ImageUtilsCheckController controller;

    //public ImageUtilsCheckDialog(Window parentWindow, MainController mainController, ConfigurationManager configurationManager, ValidationDataManager validationDataManager) {
    //public ImageUtilsCheckDialog(Window parentWindow, MainController mainController) {
    public ImageUtilsCheckDialog(Stage stage, Main main) {
        this.stage = stage;
        this.main = main;

        /*setTitle("Kontrola dostupnosti nástrojů pro validaci obrázků");
        int initialWidth = 700;
        int initialHeight = 500;

        setWidth(initialWidth);
        setMinWidth(initialWidth);
        setHeight(initialHeight);
        setMinHeight(initialHeight);

        initStyle(StageStyle.UTILITY);
        initModality(Modality.WINDOW_MODAL);
        initOwner(parentWindow);*/

        /*try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/imageUtilsCheck.fxml"));
            Parent root = (Parent) loader.load();
            setScene(new Scene(root));
            controller = loader.getController();
            controller.setMain(mainController);
            //System.out.println("stage: " + this);
            controller.setStage(this);
            //controller.setConfigurationManager(configurationManager);
            //controller.startAllChecks();
        } catch (IOException e) {
            //should never happen
            throw new RuntimeException(e);
        }*/
    }

   /* public void showAndWait(){
        controller.startAllChecks();
        super.showAndWait();
    }*/

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
            controller = loader.getController();
            controller.setMain(main);
            //System.out.println("stage: " + this);
            controller.setStage(stage);
            //controller.setConfigurationManager(configurationManager);
            controller.startAllChecks();
        } catch (IOException e) {
            //should never happen
            throw new RuntimeException(e);
        }
        //stage.show();
        stage.showAndWait();
    }

}
