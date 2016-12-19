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

    private Stage initDialogStage() {
        Stage dialogStage = new Stage();
        dialogStage.initStyle(StageStyle.UTILITY);
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.initOwner(primaryStage);
        return dialogStage;
    }

    private void initValidationData() {
        ValidationDataInitializationDialog dialog = new ValidationDataInitializationDialog(dialogStage, this);
        dialog.show();
    }

    public void checkImageUtils(boolean closeWhenFinished, String mainButtonText) {
        ImageUtilsCheckDialog dialog = new ImageUtilsCheckDialog(dialogStage, this, closeWhenFinished, mainButtonText);
        dialog.show();
    }


    public void showNewValidationConfigurationDialog() {
        PspValidationConfigurationDialog dialog = new PspValidationConfigurationDialog(dialogStage, this);
        dialog.show();
    }

    public void showValidationResultsDialog(ValidationResultSummary summary) {
        ValidationResultSummaryDialog dialog = new ValidationResultSummaryDialog(dialogStage, this);
        dialog.show(summary);
    }

    public void runPspValidation(File pspDir, String monVersion, String perVersion, boolean createTxtLog, boolean createXmlLog) {
        mainController.runPspValidation(pspDir, monVersion, perVersion, createTxtLog, createXmlLog);
    }


    public MainController openMainWindow() {
        LOG.info("openMainWindow");
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main.fxml"));
            Parent root = (Parent) loader.load();
            //primaryStage.setScene(new Scene(root, 1000, 700));
            int width = 1000;
            int height = 700;
            primaryStage.setScene(new Scene(root));
            primaryStage.show();
            primaryStage.setHeight(1000);
            primaryStage.setMinHeight(height);
            primaryStage.setWidth(1500);
            primaryStage.setMinWidth(width);
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
