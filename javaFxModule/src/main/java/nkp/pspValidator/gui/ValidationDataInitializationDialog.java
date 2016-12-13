package nkp.pspValidator.gui;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Created by martin on 13.12.16.
 */
public class ValidationDataInitializationDialog {

    private final Stage stage;
    private final Main main;
    //private ValidationDataInitializationController controller;

    //    public ValidationDataInitializationDialog(Window window, MainController mainController, ConfigurationManager configurationManager, ValidationDataManager validationDataManager) {
    public ValidationDataInitializationDialog(Stage stage, Main main) {
        this.stage = stage;
        this.main = main;

       /* setTitle("Inicializace validačních dat");
        int initialWidth = 550;
        int initialHeight = 300;

        setWidth(initialWidth);
        setMinWidth(initialWidth);
        setHeight(initialHeight);
        setMinHeight(initialHeight);

        initStyle(StageStyle.UTILITY);
        initModality(Modality.WINDOW_MODAL);
        initOwner(window);

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/validationDataInitialization.fxml"));
            Parent root = (Parent) loader.load();
            setScene(new Scene(root));
            controller = loader.getController();
            controller.setMain(mainController);
            controller.setStage(this);
            *//*ValidationDataInitializationController controller = loader.getController();
            controller.setMain(mainController);
            controller.setStage(this);
            //controller.setWindow(window);
            //controller.setConfigurationManager(configurationManager);
            controller.startInitalization();*//*
        } catch (IOException e) {
            //should never happen
            throw new RuntimeException(e);
        }*/
    }

    /*public void start() {
        //controller.setMain(mainController);
        //controller.setWindow(window);
        //controller.setConfigurationManager(configurationManager);
        controller.startInitalization();
    }*/


    //TODO: nesmi byt closable
    public void show() {
        stage.setTitle("Inicializace validačních dat");
        int initialWidth = 550;
        int initialHeight = 300;

        stage.setWidth(initialWidth);
        stage.setMinWidth(initialWidth);
        stage.setHeight(initialHeight);
        stage.setMinHeight(initialHeight);

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/validationDataInitialization.fxml"));
            Parent root = (Parent) loader.load();
            stage.setScene(new Scene(root));
            ValidationDataInitializationController controller = loader.getController();
            controller.setMain(main);
            controller.setStage(stage);
            /*ValidationDataInitializationController controller = loader.getController();
            controller.setMain(mainController);
            controller.setStage(this);
            //controller.setWindow(window);
            //controller.setConfigurationManager(configurationManager);
            controller.startInitalization();*/
            controller.startInitalization();
        } catch (IOException e) {
            //should never happen
            throw new RuntimeException(e);
        }
        //stage.show();
        stage.showAndWait();
    }


   /* @Override
    public void showAndWait() {
        controller.startInitalization();
        super.showAndWait();
    }*/

}
