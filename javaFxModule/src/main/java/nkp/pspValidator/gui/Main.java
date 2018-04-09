package nkp.pspValidator.gui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import nkp.pspValidator.gui.dev.DevDialog;
import nkp.pspValidator.gui.exclusions.ExclusionsConfigurationDialog;
import nkp.pspValidator.shared.Platform;
import nkp.pspValidator.shared.Version;

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
            //System.out.println("working dir: " + new File(".").getAbsolutePath());
            this.primaryStage = stage;
            this.dialogStage = initDialogStage();
            configurationManager = new ConfigurationManager(Platform.detectOs());
            mainController = openMainWindow();
            initValidationData();
        } catch (Throwable e) {
            e.printStackTrace();
            javafx.application.Platform.exit();
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

    public void openExclusionsConfigurationDialog() {
        ExclusionsConfigurationDialog dialog = new ExclusionsConfigurationDialog(dialogStage, this);
        dialog.show();
    }

    public void showNewValidationConfigurationDialog() {
        PspValidationConfigurationDialog dialog = new PspValidationConfigurationDialog(dialogStage, this);
        dialog.show();
    }

    public void openValidationResultSummaryDialog(ValidationResultSummary summary) {
        ValidationResultSummaryDialog dialog = new ValidationResultSummaryDialog(dialogStage, this, summary);
        dialog.show();
    }

    public void showAboutAppDialog() {
        AboutAppDialog dialog = new AboutAppDialog(dialogStage, this);
        dialog.show();
    }

    public void showTestDialog() {
        DevDialog dialog = new DevDialog(dialogStage, this);
        dialog.show();
    }

    public void runPspValidation(File pspDir, String preferedMonVersion, String preferedPerVersion, String forcedMonVersion, String forcedPerVersion, boolean createTxtLog, boolean createXmlLog) {
        mainController.runPspValidation(pspDir, preferedMonVersion, preferedPerVersion, forcedMonVersion, forcedPerVersion, createTxtLog, createXmlLog);
    }


    public MainController openMainWindow() {
        //System.out.println("mainWindow: " + primaryStage);
        try {
            //title
            primaryStage.setTitle("Komplexní validátor " + Version.VERSION_CODE);
            //icon
            addIconIfFound(primaryStage, "img/appIcon/appIcon16.png");
            addIconIfFound(primaryStage, "img/appIcon/appIcon32.png");
            addIconIfFound(primaryStage, "img/appIcon/appIcon48.png");

            //window size and position
            primaryStage.setResizable(true);
            Rectangle2D visualBounds = Screen.getPrimary().getVisualBounds();
            int minWidth = 1200;
            int minHeight = 600;
            int preferedWidth = (int) (visualBounds.getWidth() * 0.85);
            int preferedHeight = (int) (visualBounds.getHeight() * 0.85);
            int width = Math.max(minWidth, preferedWidth);
            int height = Math.max(minHeight, preferedHeight);
            //int height = minHeight;
            //int width = minWidth;
            //set size
            primaryStage.setHeight(height);
            primaryStage.setMinHeight(minHeight);
            primaryStage.setWidth(width);
            primaryStage.setMinWidth(minWidth);
            //center in screen
            primaryStage.setX((visualBounds.getMaxX() - width) / 2);
            primaryStage.setY((visualBounds.getMaxY() - height) / 2);

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main.fxml"));
            Parent root = (Parent) loader.load();
            primaryStage.setScene(new Scene(root));
            primaryStage.show();

            MainController controller = loader.getController();
            controller.setMain(this);
            return controller;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void addIconIfFound(Stage stage, String path) {
        Image icon = new Image(path);
        if (icon != null) {
            stage.getIcons().add(icon);
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
