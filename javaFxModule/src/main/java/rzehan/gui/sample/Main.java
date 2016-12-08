package rzehan.gui.sample;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        System.out.println("pwd: " + new File(".").getAbsolutePath());
        File propertiesFile = new File("../../resources/main/config.properties");
        Config config = new Config(propertiesFile);

        System.out.println("fdmf.dir:" + config.getFdmfDir().getAbsolutePath());

        //Font.loadFont(getClass().getResource("/fonts/VarelaRound-Regular.ttf").toExternalForm(), 10);
       /* Parent root = FXMLLoader.load(getClass().getResource("/fxml/sample.fxml"));
        primaryStage.setTitle("PSP validator");
        primaryStage.setScene(new Scene(root, 1000, 700));
        primaryStage.show();*/

        //init screen
        Parent root = FXMLLoader.load(getClass().getResource("/fxml/initScreen.fxml"));
        primaryStage.setTitle("PSP validator");
        primaryStage.setScene(new Scene(root, 1000, 700));
        primaryStage.show();
    }


    public static void main(String[] args) {
        launch(args);
    }
}
