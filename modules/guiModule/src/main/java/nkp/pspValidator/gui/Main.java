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
import nkp.pspValidator.gui.settings.SettingsTab;
import nkp.pspValidator.shared.Dmf;
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
    /** nad Nastavením otevřeným z dialogu Nová validace (obsah slovníku, aktualizace slovníku) */
    private Stage dialogStageLevel3;
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
            this.dialogStageLevel3 = initDialogStage(dialogStageLevel2);
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
     * validaci a adresář validuje hned, ZIP nejdřív rozbalí, skupinu balíků validuje v hlavním okně.
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

            @Override
            public void runGroupValidation(File groupDir, DmfDetector.Params params, boolean createTxtLog, boolean createXmlLog, int verbosity) {
                Main.this.runGroupValidation(groupDir, params, createTxtLog, createXmlLog, verbosity);
            }
        });
    }

    /** Validace skupiny balíků v hlavním okně (ZIPy ve skupině se rozbalují bez dialogu). */
    public void runGroupValidation(File groupDir, DmfDetector.Params params, boolean createTxtLog, boolean createXmlLog, int verbosity) {
        dialogStage.close();
        ValidationRequest request = pendingRequest;
        pendingRequest = null;
        mainController.runGroupValidation(groupDir, params, createTxtLog, createXmlLog, verbosity, request);
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
     * Dialog externích nástrojů při startu aplikace (nástroje se dál nastavují v Nastavení, záložka Externí nástroje).
     *
     * @param exitAppOnCloseWithError zavření okna křížkem, když některý nástroj chybí, ukončí aplikaci
     */
    public void showExternalUtilsCheckDialog(boolean closeWhenFinished, String mainButtonText, boolean exitAppOnCloseWithError) {
        new ExternalUtilsCheckDialog(dialogStage, this, closeWhenFinished, mainButtonText, exitAppOnCloseWithError).show();
        //dostupnost nastroju v prazdnem stavu hlavniho okna
        mainController.refreshEmptyStateInfo();
    }

    public void showNewValidationDialog() {
        new NewValidationDialog(dialogStage, this).show();
    }

    /**
     * Okno Nastavení z hlavního okna (menu, prázdný stav). Vrátí se po zavření okna; prázdný stav hlavního okna se pak
     * obnoví (dostupnost nástrojů).
     */
    public void showSettingsDialog(SettingsTab tab) {
        new SettingsDialog(dialogStage, this, tab, null).show();
        mainController.refreshEmptyStateInfo();
    }

    /**
     * Okno Nastavení otevřené z dialogu Nová validace (druhá úroveň oken). Vrátí se po zavření okna, volající si pak
     * obnoví, co se mohlo změnit.
     *
     * @param dmf fDMF balíku v dialogu (záložka přeskakovaných pravidel ji ukáže první), nebo null
     */
    public void showSettingsDialogOverDialog(SettingsTab tab, Dmf dmf) {
        new SettingsDialog(dialogStageLevel2, this, tab, dmf).show();
        mainController.refreshEmptyStateInfo();
    }

    /** Okno pro dialog otevřený nad oknem {@code stage} (o úroveň výš). */
    Stage stageAbove(Stage stage) {
        if (stage == dialogStage) {
            return dialogStageLevel2;
        } else if (stage == dialogStageLevel2) {
            return dialogStageLevel3;
        }
        throw new IllegalArgumentException("nad oknem " + stage.getTitle() + " už další úroveň oken není");
    }

    public void showAboutAppDialog() {
        new AboutAppDialog(dialogStage, this).show();
    }

    /** Obsah slovníku nad oknem {@code over} (záložka Slovníky v Nastavení). */
    public void showDictionaryContentDialog(Stage over, String dictionaryName, String description, String specUrl) {
        new DictionaryContentDialog(stageAbove(over), this, dictionaryName, description, specUrl).show();
    }

    /** Aktualizace slovníku nad oknem {@code over} (záložka Slovníky v Nastavení). */
    public void showDictionaryUpdateDialog(Stage over, String dictionaryName, String syncUrl) {
        new DictionaryUpdateDialog(stageAbove(over), this, dictionaryName, syncUrl).show();
    }

    public void showTestDialog() {
        new DevDialog(dialogStage, this).show();
    }

}
