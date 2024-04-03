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
import nkp.pspValidator.gui.skipping.SkippingConfigurationDialog;
import nkp.pspValidator.shared.DmfDetector;
import nkp.pspValidator.shared.Platform;
import nkp.pspValidator.shared.Version;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

public class Main extends Application {

    private static Logger LOG = Logger.getLogger(Main.class.getSimpleName());

    private Stage primaryStage;
    private Stage dialogStage;
    private Stage dialogStageLevel2;
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
            this.dialogStage = initDialogStage(primaryStage);
            this.dialogStageLevel2 = initDialogStage(dialogStage);
            configurationManager = new ConfigurationManager(Platform.detectOs());
            mainController = openMainWindow();
            showValidationInitializationDialog();
        } catch (Throwable e) {
            e.printStackTrace();
            javafx.application.Platform.exit();
            //TODO: a zobrazit dialog s tlacitkem OK a stack tracem, ktery apku zavre
        }
    }

    private Stage initDialogStage(Stage ownerStage) {
        Stage dialogStage = new Stage();
        dialogStage.initStyle(StageStyle.UTILITY);
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.initOwner(ownerStage);
        return dialogStage;
    }

    public MainController openMainWindow() {
        //System.out.println("mainWindow: " + primaryStage);
        try {
            //title
            primaryStage.setTitle("Komplexní validátor " + Version.VERSION_CODE);
            //icon
            addIconIfFound(primaryStage, "img/appIcon/appIcon.iconset/icon_16x16.png");
            addIconIfFound(primaryStage, "img/appIcon/appIcon.iconset/icon_32x32.png");
            addIconIfFound(primaryStage, "img/appIcon/appIcon.iconset/icon_48x48.png");

            //window size and position
            primaryStage.setResizable(true);
            Rectangle2D visualBounds = Screen.getPrimary().getVisualBounds();
            int minWidth = 1200;
            int minHeight = 600;
            int preferredWidth = (int) (visualBounds.getWidth() * 0.85);
            int preferredHeight = (int) (visualBounds.getHeight() * 0.85);
            int width = Math.max(minWidth, preferredWidth);
            int height = Math.max(minHeight, preferredHeight);
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

    public void runPspDirValidation(File pspDir, DmfDetector.Params dmDetectorParams, boolean createTxtLog, boolean createXmlLog, int verbosity) {

        //because the possible unzip dialog cannot close it's parent dialog
        dialogStage.close();
        mainController.runPspDirValidation(pspDir, dmDetectorParams, createTxtLog, createXmlLog, verbosity);
    }

    public void unzipAndRunPspZipValidation(File pspZip, DmfDetector.Params dmfDetectorParams, boolean createTxtLog, boolean createXmlLog, int verbosity) {
        new UnzipAndValidateDialog(dialogStageLevel2, this, pspZip, dmfDetectorParams, createTxtLog, createXmlLog, verbosity).show();
    }

    //DIALOGS

    private void showValidationInitializationDialog() {
        new ValidationDataInitializationDialog(dialogStage, this).show();
    }

    public void showExternalUtilsCheckDialog(boolean closeWhenFinished, String mainButtonText) {
        new ExternalUtilsCheckDialog(dialogStage, this, closeWhenFinished, mainButtonText).show();
    }

    public void showSkippingConfigurationDialog() {
        new SkippingConfigurationDialog(dialogStage, this).show();
    }

    public void showNewPspDirValidationConfigurationDialog() {
        new PspDirValidationConfigurationDialog(dialogStage, this).show();
    }

    public void showNewPspZipValidationConfigurationDialog() {
        new PspZipValidationConfigurationDialog(dialogStage, this).show();
    }

    public void showValidationResultSummaryDialog(ValidationResultSummary summary) {
        new ValidationResultSummaryDialog(dialogStage, this, summary).show();
    }

    public void showAboutAppDialog() {
        new AboutAppDialog(dialogStage, this).show();
    }

    public void showDictionariesConfigurationDialog() {
        new DictionariesConfigurationDialog(dialogStage, this).show();
    }

    public void showDictionaryContentDialog(String dictionaryName, String description, String specUrl) {
        new DictionaryContentDialog(dialogStageLevel2, this, dictionaryName, description, specUrl).show();
    }

    public void showDictionaryUpdateDialog(String dictionaryName, String syncUrl) {
        new DictionaryUpdateDialog(dialogStageLevel2, this, dictionaryName, syncUrl).show();
    }

    public void showTestDialog() {
        new DevDialog(dialogStage, this).show();
    }

}
