package nkp.pspValidator.gui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import nkp.pspValidator.shared.Dmf;
import nkp.pspValidator.shared.Platform;

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

        //System.out.println("fdmf.dir:" + config.getFdmfConfig().getAbsolutePath());

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
            initValidationData();
            //checkImageUtils();
        } catch (IOException e) {
            e.printStackTrace();
            //TODO: a zobrazit dialog s tlacitkem OK a stack tracem, ktery apku zavre
        }
    }

    private void initValidationData() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/validationDataInitialization.fxml"));
        Parent root = (Parent) loader.load();
        stage.setScene(new Scene(root));
        stage.show();
        ValidationDataInitializationController controller = (ValidationDataInitializationController) loader.getController();
        controller.setApp(this);
        controller.setConfigurationManager(configurationManager);
        controller.startInitalization();
    }

    public void checkImageUtils() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/imageUtilsCheck.fxml"));
            Parent root = (Parent) loader.load();
            //stage.setScene(new Scene(root, 1000, 700));
            stage.setScene(new Scene(root));
            stage.show();
            ImageUtilsCheckController controller = (ImageUtilsCheckController) loader.getController();
            controller.setApp(this);
            controller.setValidationDataManager(validationDataManager);
            controller.setConfigurationManager(configurationManager);
            controller.startAllChecks();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void openMainWindow() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main.fxml"));
            Parent root = (Parent) loader.load();
            //stage.setScene(new Scene(root, 1000, 700));
            stage.setScene(new Scene(root));
            stage.show();
            MainController controller = (MainController) loader.getController();
            controller.setApp(this);
            controller.setValidationDataManager(validationDataManager);
            controller.setConfigurationManager(configurationManager);
            //controller.startAllChecks();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    public void setValidationDataManager(ValidationDataManager validationDataManager) {
        this.validationDataManager = validationDataManager;
    }

    /**
     * @param pspDir
     * @param preferedMonographDmf  can be null
     * @param preferedPeriodicalDmf can be null
     */
    public void validatePsp(File pspDir, Dmf preferedMonographDmf, Dmf preferedPeriodicalDmf) {
        //TODO
        System.out.println("validating " + pspDir.getAbsolutePath() + ", mon: " + preferedMonographDmf + ", per: " + preferedPeriodicalDmf);
    }
}
