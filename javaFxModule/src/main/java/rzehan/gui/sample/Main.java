package rzehan.gui.sample;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import nkp.pspValidator.shared.Platform;
import nkp.pspValidator.shared.imageUtils.ImageUtilManager;
import nkp.pspValidator.shared.imageUtils.ImageUtilManagerFactory;
import rzehan.gui.DataController;
import rzehan.gui.InitScreenController;

import java.io.File;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        //System.out.println("pwd: " + new File(".").getAbsolutePath());

        //System.out.println("fdmf.dir:" + config.getFdmfDir().getAbsolutePath());

        //Font.loadFont(getClass().getResource("/fonts/VarelaRound-Regular.ttf").toExternalForm(), 10);
        /*Parent root = FXMLLoader.load(getClass().getResource("/fxml/sample.fxml"));
        primaryStage.setTitle("PSP validator");
        primaryStage.setScene(new Scene(root, 1000, 700));
        primaryStage.show();*/

        //init screen
        /*Parent root = FXMLLoader.load(getClass().getResource("/fxml/initScreen.fxml"));
        primaryStage.setTitle("PSP validator");
        primaryStage.setScene(new Scene(root, 1000, 700));
        primaryStage.show();*/


        DataController dataController = new DataController(Platform.detectOs());

        //TODO: jen docasne
        File imageUtilConfig = null;
        switch (dataController.getPlatform().getOperatingSystem()) {
            case LINUX:
                imageUtilConfig = new File("/home/martin/ssd/IdeaProjects/PspValidator/sharedModule/src/main/resources/nkp/pspValidator/shared/fDMF/imageUtils.xml");
                break;
            case MAC:
                imageUtilConfig = new File("/Users/martinrehanek/IdeaProjects/PspValidator/sharedModule/src/main/resources/nkp/pspValidator/shared/fDMF/imageUtils.xml");
                break;
        }

        ImageUtilManager imageUtilManager = new ImageUtilManagerFactory(imageUtilConfig).buildImageUtilManager(dataController.getPlatform().getOperatingSystem());
        dataController.setImageUtilManager(imageUtilManager);


        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/initScreen.fxml"));
        Parent root = (Parent) loader.load();
        primaryStage.setTitle("Kontrola dostupnosti nástrojů pro validaci obrázků");
        primaryStage.setScene(new Scene(root, 1000, 700));
        primaryStage.show();
        InitScreenController controller = (InitScreenController) loader.getController();
        controller.setDataController(dataController);
        //controller.setStageAndApplication(primaryStage, this);
        controller.startAllChecks();
    }


    public static void main(String[] args) {
        launch(args);
    }
}
