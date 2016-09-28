package rzehan.gui;

//import org.controlsfx.dialog.Dialog;
import rzehan.shared.SharedClass;

import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;


public class SomeClass extends Application{

    /*public static String neco() {
        return "neco woe";
    }

    public static void main(String[] args) {
        Dialog dialog;
        System.out.println(neco());
    }

    public static void main(String[] args) {
        launch(args);
    }*/

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Hello World!");

        Button btn2 = new Button("Button");


        Button btn = new Button();

        //String shared = "TODO";
        String shared =new SharedClass().toString();
        String text = "Hello from GUI, shared: " + shared;

        //btn.setText("Say 'Hello World'");
        btn.setText(text);
        btn.setOnAction(new EventHandler<ActionEvent>() {

            @Override
            public void handle(ActionEvent event) {
                System.out.println("Hello World!");

                //new TadaTransition(btn2).play();
            }
        });

        StackPane root = new StackPane();
        root.getChildren().add(btn);
        primaryStage.setScene(new Scene(root, 300, 250));
        primaryStage.show();
    }

}