package rzehan.gui.sample;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        //Font.loadFont(getClass().getResource("/fonts/VarelaRound-Regular.ttf").toExternalForm(), 10);
        System.err.println("START");
        Parent root = FXMLLoader.load(getClass().getResource("/fxml/sample.fxml"));
        System.err.println("HERE");
        primaryStage.setTitle("Nkp validator");
        primaryStage.setScene(new Scene(root, 1000, 700));
        primaryStage.show();
        //
    }


    public static void main(String[] args) {
        launch(args);
    }
}
