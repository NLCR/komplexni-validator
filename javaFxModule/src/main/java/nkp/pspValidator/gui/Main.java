package nkp.pspValidator.gui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import nkp.pspValidator.shared.Platform;
import nkp.pspValidator.shared.engine.exceptions.ValidatorConfigurationException;
import nkp.pspValidator.shared.imageUtils.ImageUtilManager;
import nkp.pspValidator.shared.imageUtils.ImageUtilManagerFactory;

import java.io.File;
import java.io.IOException;

public class Main extends Application {

    private Stage stage;

    @Override
    public void start(Stage stage) throws Exception {
        this.stage = stage;

        //System.out.println("pwd: " + new File(".").getAbsolutePath());

        //System.out.println("fdmf.dir:" + config.getFdmfDir().getAbsolutePath());

        //Font.loadFont(getClass().getResource("/fonts/VarelaRound-Regular.ttf").toExternalForm(), 10);
        /*Parent root = FXMLLoader.load(getClass().getResource("/fxml/sample.fxml"));
        primaryStage.setTitle("PSP validator");
        primaryStage.setScene(new Scene(root, 1000, 700));
        primaryStage.show();*/

        //init screen
        /*Parent root = FXMLLoader.load(getClass().getResource("/fxml/imageUtilsValidation.fxml"));
        primaryStage.setTitle("PSP validator");
        primaryStage.setScene(new Scene(root, 1000, 700));
        primaryStage.show();*/


        DataManager dataManager = new DataManager(Platform.detectOs());
        //checkValidationData(dataController);
        checkImageUtils(dataManager);
    }

    private void checkValidationData(DataManager dataManager) {
        //TODO
    }

    public void checkImageUtils(DataManager dataManager) throws ValidatorConfigurationException, IOException {
        //TODO: jen docasne
        File imageUtilConfig = null;
        switch (dataManager.getPlatform().getOperatingSystem()) {
            case LINUX:
                imageUtilConfig = new File("/home/martin/ssd/IdeaProjects/PspValidator/sharedModule/src/main/resources/nkp/pspValidator/shared/fDMF/imageUtils.xml");
                break;
            case MAC:
                imageUtilConfig = new File("/Users/martinrehanek/IdeaProjects/PspValidator/sharedModule/src/main/resources/nkp/pspValidator/shared/fDMF/imageUtils.xml");
                break;
        }

        ImageUtilManager imageUtilManager = new ImageUtilManagerFactory(imageUtilConfig).buildImageUtilManager(dataManager.getPlatform().getOperatingSystem());
        dataManager.setImageUtilManager(imageUtilManager);


        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/imageUtilsValidation.fxml"));
        Parent root = (Parent) loader.load();
        stage.setTitle("PSP Validátor");
        stage.setScene(new Scene(root, 1000, 700));
        stage.show();
        ImageUtilsValidationController controller = (ImageUtilsValidationController) loader.getController();
        controller.setDataManager(dataManager);
        //controller.setStageAndApplication(primaryStage, this);
        controller.startAllChecks();
    }


    public static void main(String[] args) {
        launch(args);
    }
}
