package nkp.pspValidator.gui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import nkp.pspValidator.shared.Platform;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

public class Main extends Application {

    private static Logger LOG = Logger.getLogger(Main.class.getSimpleName());

    private Stage primaryStage;
    private Stage dialogStage;
    private ConfigurationManager configurationManager;
    private ValidationDataManager validationDataManager;

    //controllers
    private MainController mainController;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        LOG.info("start");
        this.primaryStage = stage;
        this.dialogStage = initDialogStage();
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
            //initValidationData();
            openMainWindow();
            initValidationData();

            //checkImageUtils();
        } catch (IOException e) {
            e.printStackTrace();
            //TODO: a zobrazit dialog s tlacitkem OK a stack tracem, ktery apku zavre
        }
    }

    private void initValidationData() {
        ValidationDataInitializationDialog dialog = new ValidationDataInitializationDialog(dialogStage, this);
        dialog.show();
    }

    public void checkImageUtils() {
        ImageUtilsCheckDialog dialog = new ImageUtilsCheckDialog(dialogStage, this);
        dialog.show();
    }


    public void showNewValidationConfigurationDialog() {
        /*PspValidationConfigurationDialog dialog = new PspValidationConfigurationDialog(getWindow(), this, configurationManager, validationDataManager);
        dialog.showAndWait();*/
        PspValidationConfigurationDialog dialog = new PspValidationConfigurationDialog(dialogStage, this);
        dialog.show();
    }

    private Stage initDialogStage() {
        Stage dialogStage = new Stage();
        dialogStage.initStyle(StageStyle.UTILITY);
        dialogStage.initModality(Modality.WINDOW_MODAL);
        dialogStage.initOwner(primaryStage);
        return dialogStage;
    }

    /*private void initValidationData() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/validationDataInitialization.fxml"));
        Parent root = (Parent) loader.load();
        primaryStage.setScene(new Scene(root));
        primaryStage.show();
        ValidationDataInitializationController controller = (ValidationDataInitializationController) loader.getController();
        controller.setApp(this);
        controller.setConfigurationManager(configurationManager);
        controller.startInitalization();
    }*/

    public void openMainWindow() {
        LOG.info("openMainWindow");
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main.fxml"));
            Parent root = (Parent) loader.load();
            //primaryStage.setScene(new Scene(root, 1000, 700));
            primaryStage.setScene(new Scene(root));
            primaryStage.show();
            primaryStage.setHeight(700);
            primaryStage.setMinHeight(700);
            primaryStage.setWidth(650);
            primaryStage.setMinWidth(650);
            mainController = (MainController) loader.getController();
            mainController.setMain(this);
            //mainController.setApp(this);
            //TODO: zatim nebude k dispozici
            //mainController.setConfigurationManager(configurationManager);
            //mainController.setMain(mainController);
            //mainController.setValidationDataManager(validationDataManager);
            //mainController.startAllChecks();
            //controller.startAllChecks();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public ConfigurationManager getConfigurationManager() {
        return configurationManager;
    }


    /*public void setValidationDataManager(ValidationDataManager validationDataManager) {
        this.validationDataManager = validationDataManager;
    }*/


    /*public void validatePsp(File pspDir, String focedMonographVersion, String forcedPeriodicalVersion) {
        mainController.validatePsp(pspDir, focedMonographVersion, forcedPeriodicalVersion);
    }*/
    public ValidationDataManager getValidationDataManager() {
        return validationDataManager;
    }

    public void setValidationDataManager(ValidationDataManager validationDataManager) {
        this.validationDataManager = validationDataManager;
    }


    public void validatePsp(File pspDir, String monVersion, String perVersion) {
        mainController.validatePsp(pspDir, monVersion, perVersion);
    }
}
