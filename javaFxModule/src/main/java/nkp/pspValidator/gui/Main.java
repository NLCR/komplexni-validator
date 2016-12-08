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
    private ConfigurationManager configurationManager;
    private ValidationDataManager validationDataManager;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        this.stage = stage;
        stage.setTitle("PSP Validátor");

        //System.out.println("pwd: " + new File(".").getAbsolutePath());

        //System.out.println("fdmf.dir:" + config.getFdmfDir().getAbsolutePath());

        //Font.loadFont(getClass().getResource("/fonts/VarelaRound-Regular.ttf").toExternalForm(), 10);
        /*Parent root = FXMLLoader.load(getClass().getResource("/fxml/sample.fxml"));
        primaryStage.setTitle("PSP validator");
        primaryStage.setScene(new Scene(root, 1000, 700));
        primaryStage.show();*/

        //init screen
        /*Parent root = FXMLLoader.load(getClass().getResource("/fxml/imageUtilsCheck.fxml"));
        primaryStage.setTitle("PSP validator");
        primaryStage.setScene(new Scene(root, 1000, 700));
        primaryStage.show();*/

        try {
            configurationManager = new ConfigurationManager(Platform.detectOs());
            validationDataManager = initValidationData();
            checkImageUtils();
        } catch (IOException e) {
            e.printStackTrace();
            //TODO: a zobrazit dialog s tlacitkem OK a stack tracem, ktery apku zavre
        }
    }

    private ValidationDataManager initValidationData() {
        try {
            ValidationDataManager validationDataManager = new ValidationDataManager(configurationManager);
            //TODO: jen docasne
            File imageUtilConfig = null;
            switch (configurationManager.getPlatform().getOperatingSystem()) {
                case LINUX:
                    imageUtilConfig = new File("/home/martin/ssd/IdeaProjects/PspValidator/sharedModule/src/main/resources/nkp/pspValidator/shared/fDMF/imageUtils.xml");
                    break;
                case MAC:
                    imageUtilConfig = new File("/Users/martinrehanek/IdeaProjects/PspValidator/sharedModule/src/main/resources/nkp/pspValidator/shared/fDMF/imageUtils.xml");
                    break;
            }

            ImageUtilManager imageUtilManager = new ImageUtilManagerFactory(imageUtilConfig).buildImageUtilManager(configurationManager.getPlatform().getOperatingSystem());
            validationDataManager.setImageUtilManager(imageUtilManager);
            return validationDataManager;
        } catch (ValidatorConfigurationException e) {
            //TODO: tohle bude ve vlastni Aktivite
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        //TODO
    }

    public void checkImageUtils() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/imageUtilsCheck.fxml"));
        Parent root = (Parent) loader.load();
        stage.setScene(new Scene(root, 1000, 700));
        stage.show();
        ImageUtilsCheckController controller = (ImageUtilsCheckController) loader.getController();
        controller.setValidationDataManager(validationDataManager);
        controller.startAllChecks();
    }


}
