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
import nkp.pspValidator.gui.newvalidation.ValidationRequest;
import nkp.pspValidator.gui.skipping.SkippingConfigurationDialog;
import nkp.pspValidator.shared.DmfDetector;
import nkp.pspValidator.shared.Platform;
import nkp.pspValidator.shared.Version;
import nkp.pspValidator.shared.externalUtils.ExternalUtil;

import java.io.File;
import java.io.IOException;
import java.util.Set;
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

    /** Požadavek právě spouštěné validace (u ZIPu čeká na rozbalení), předá se hlavnímu oknu pro „Validovat znovu“. */
    private ValidationRequest pendingRequest;

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
            addIconIfFound(primaryStage, "img/appIcon/appIcon.iconset/icon_128x128.png");
            addIconIfFound(primaryStage, "img/appIcon/appIcon.iconset/icon_256x256.png");

            //window size and position
            primaryStage.setResizable(true);
            Rectangle2D visualBounds = Screen.getPrimary().getVisualBounds();
            //okno musi byt pouzitelne od 1000 x 640 (vysledky ve trech panelech)
            int minWidth = 1000;
            int minHeight = 640;
            int preferredWidth = (int) (visualBounds.getWidth() * 0.85);
            int preferredHeight = (int) (visualBounds.getHeight() * 0.85);
            int width = Math.max(minWidth, preferredWidth);
            int height = Math.max(minHeight, preferredHeight);
            //int height = minHeight;
            //int width = minWidth;
            //set size
            primaryStage.setHeight(height);
            primaryStage.setMinHeight(Math.min(minHeight, visualBounds.getHeight()));
            primaryStage.setWidth(width);
            primaryStage.setMinWidth(Math.min(minWidth, visualBounds.getWidth()));
            //center in screen
            primaryStage.setX((visualBounds.getMaxX() - width) / 2);
            primaryStage.setY((visualBounds.getMaxY() - height) / 2);

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main.fxml"));
            Parent root = (Parent) loader.load();
            Scene scene = new Scene(root);
            AbstractDialog.addBaseStylesheet(scene);
            primaryStage.setScene(scene);
            primaryStage.show();

            MainController controller = loader.getController();
            controller.setMain(this);
            controller.refreshEmptyStateInfo();
            return controller;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void addIconIfFound(Stage stage, String path) {
        // chybějící ikona nesmí shodit start aplikace (Image bez resource hází IllegalArgumentException)
        try {
            stage.getIcons().add(new Image(path));
        } catch (IllegalArgumentException e) {
            System.err.println("ikona okna nenalezena: " + path);
        }
    }

    public Stage getPrimaryStage() {
        return primaryStage;
    }

    public Stage getDialogStage() {
        return dialogStage;
    }

    public ConfigurationManager getConfigurationManager() {
        return configurationManager;
    }

    public ValidationDataManager getValidationDataManager() {
        return validationDataManager;
    }

    public void setValidationDataManager(ValidationDataManager validationDataManager) {
        this.validationDataManager = validationDataManager;
        mainController.refreshEmptyStateInfo();
    }

    MainController getMainController() {
        return mainController;
    }

    /**
     * Spustí validaci podle požadavku (z dialogu Nová validace i „Validovat znovu“): nastaví nástroje vypnuté pro tuto
     * validaci a adresář validuje hned, ZIP nejdřív rozbalí.
     */
    public void launchValidation(ValidationRequest request) {
        pendingRequest = request;
        request.launch(new ValidationRequest.Launcher() {
            @Override
            public void setUtilsDisabled(Set<ExternalUtil> utils) {
                validationDataManager.getExternalUtilManager().setUtilsDisabled(utils);
            }

            @Override
            public void runPspDirValidation(File pspDir, DmfDetector.Params params, boolean createTxtLog, boolean createXmlLog, int verbosity) {
                Main.this.runPspDirValidation(pspDir, params, createTxtLog, createXmlLog, verbosity);
            }

            @Override
            public void unzipAndRunPspZipValidation(File pspZip, DmfDetector.Params params, boolean createTxtLog, boolean createXmlLog, int verbosity) {
                Main.this.unzipAndRunPspZipValidation(pspZip, params, createTxtLog, createXmlLog, verbosity);
            }
        });
    }

    public void runPspDirValidation(File pspDir, DmfDetector.Params dmDetectorParams, boolean createTxtLog, boolean createXmlLog, int verbosity) {

        //because the possible unzip dialog cannot close it's parent dialog
        dialogStage.close();
        ValidationRequest request = pendingRequest;
        pendingRequest = null;
        mainController.runPspDirValidation(pspDir, dmDetectorParams, createTxtLog, createXmlLog, verbosity, request);
    }

    public void unzipAndRunPspZipValidation(File pspZip, DmfDetector.Params dmfDetectorParams, boolean createTxtLog, boolean createXmlLog, int verbosity) {
        //z dialogu Nova validace nad nim, pri „Validovat znovu“ (zadny dialog neni otevreny) v prvni urovni oken
        Stage stage = dialogStage.isShowing() ? dialogStageLevel2 : dialogStage;
        new UnzipAndValidateDialog(stage, this, pspZip, dmfDetectorParams, createTxtLog, createXmlLog, verbosity).show();
    }

    //DIALOGS

    private void showValidationInitializationDialog() {
        new ValidationDataInitializationDialog(dialogStage, this).show();
    }

    /**
     * Dialog externích nástrojů v hlavní úrovni oken (při startu aplikace a z menu Nastavení).
     *
     * @param exitAppOnCloseWithError zavření okna křížkem, když některý nástroj chybí, ukončí aplikaci (jen při startu)
     */
    public void showExternalUtilsCheckDialog(boolean closeWhenFinished, String mainButtonText, boolean exitAppOnCloseWithError) {
        new ExternalUtilsCheckDialog(dialogStage, this, closeWhenFinished, mainButtonText, exitAppOnCloseWithError).show();
        //dostupnost nastroju v prazdnem stavu hlavniho okna
        mainController.refreshEmptyStateInfo();
    }

    public void showSkippingConfigurationDialog() {
        new SkippingConfigurationDialog(dialogStage, this).show();
    }

    public void showNewValidationDialog() {
        new NewValidationDialog(dialogStage, this).show();
    }

    /**
     * Dialog externích nástrojů otevřený z dialogu Nová validace (druhá úroveň oken). Zavření okna při chybějícím
     * nástroji tady aplikaci neukončí, jen dialog zavře.
     */
    public void showExternalUtilsCheckDialogOverDialog() {
        new ExternalUtilsCheckDialog(dialogStageLevel2, this, false, "Zavřít", false).show();
    }

    /** Dialog přeskakování validací otevřený z dialogu Nová validace (druhá úroveň oken). */
    public void showSkippingConfigurationDialogOverDialog() {
        new SkippingConfigurationDialog(dialogStageLevel2, this).show();
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
