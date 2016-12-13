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
        try {
            this.primaryStage = stage;
            this.dialogStage = initDialogStage();
            primaryStage.setTitle("PSP Validátor");
            configurationManager = new ConfigurationManager(Platform.detectOs());
            mainController = openMainWindow();
            initValidationData();
        } catch (Throwable e) {
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
        PspValidationConfigurationDialog dialog = new PspValidationConfigurationDialog(dialogStage, this);
        dialog.show();
    }

    public void runPspValidation(File pspDir, String monVersion, String perVersion) {
        mainController.runPspValidation(pspDir, monVersion, perVersion);
    }

    private Stage initDialogStage() {
        Stage dialogStage = new Stage();
        dialogStage.initStyle(StageStyle.UTILITY);
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.initOwner(primaryStage);
        return dialogStage;
    }

    public MainController openMainWindow() {
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
            MainController controller = loader.getController();
            controller.setMain(this);
            return controller;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public ConfigurationManager getConfigurationManager() {
        return configurationManager;
    }

    public ValidationDataManager getValidationDataManager() {
        return validationDataManager;
    }

    public void setValidationDataManager(ValidationDataManager validationDataManager) {
        this.validationDataManager = validationDataManager;
    }

}
