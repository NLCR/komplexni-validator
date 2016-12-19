package nkp.pspValidator.gui;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.image.ImageView;
import javafx.stage.DirectoryChooser;
import javafx.stage.WindowEvent;
import nkp.pspValidator.shared.engine.exceptions.ValidatorConfigurationException;
import nkp.pspValidator.shared.imageUtils.CliCommand;
import nkp.pspValidator.shared.imageUtils.ImageUtil;
import nkp.pspValidator.shared.imageUtils.ImageUtilManager;

import java.io.File;
import java.util.HashMap;
import java.util.Map;


/**
 * Created by martin on 2.12.16.
 */
public class ImageUtilsCheckDialogController extends DialogController {

    private static final String JPYLYZER_INSTALLATION_URL = "https://github.com/rzeh4n/psp-validator/wiki/Instalace#jpylyzer";
    private static final String JHOVE_INSTALLATION_URL = "https://github.com/rzeh4n/psp-validator/wiki/Instalace#jhove";
    private static final String IMAGE_MAGICK_INSTALLATION_URL = "https://github.com/rzeh4n/psp-validator/wiki/Instalace#imagemagick";
    private static final String KAKADU_INSTALLATION_URL = "https://github.com/rzeh4n/psp-validator/wiki/Instalace#kakadu";
    private static final String HELP_URL = "https://github.com/rzeh4n/psp-validator/wiki/Instalace#instalace-n%C3%A1stroj%C5%AF-pro-validaci-obrazov%C3%BDch-soubor%C5%AF";

    /*jhove*/

    @FXML
    Label jhoveStatusText;

    @FXML
    ProgressIndicator jhoveProgress;

    @FXML
    ImageView jhoveOkImg;

    @FXML
    ImageView jhoveErrorImg;

    @FXML
    Button jhoveBtnRetry;

    @FXML
    Button jhoveBtnSelectPath;

    @FXML
    Button jhoveBtnInstall;

    /*jpylyzer*/

    @FXML
    Label jpylyzerStatusText;

    @FXML
    ProgressIndicator jpylyzerProgress;

    @FXML
    ImageView jpylyzerOkImg;

    @FXML
    ImageView jpylyzerErrorImg;

    @FXML
    Button jpylyzerBtnRetry;

    @FXML
    Button jpylyzerBtnSelectPath;

    @FXML
    Button jpylyzerBtnInstall;

    /*imageMagick*/

    @FXML
    Label imageMagickStatusText;

    @FXML
    ProgressIndicator imageMagickProgress;

    @FXML
    ImageView imageMagickOkImg;

    @FXML
    ImageView imageMagickErrorImg;

    @FXML
    Button imageMagickBtnRetry;

    @FXML
    Button imageMagickBtnSelectPath;

    @FXML
    Button imageMagickBtnInstall;


    /*kakadu*/

    @FXML
    Label kakaduStatusText;

    @FXML
    ProgressIndicator kakaduProgress;

    @FXML
    ImageView kakaduOkImg;

    @FXML
    ImageView kakaduErrorImg;

    @FXML
    Button kakaduBtnRetry;

    @FXML
    Button kakaduBtnSelectPath;

    @FXML
    Button kakaduBtnInstall;

    @FXML
    Button btnContinue;

    //other data
    private final Map<ImageUtil, Boolean> utilsFinished = new HashMap<>();
    private DialogState state = DialogState.RUNNING;
    private boolean closeWhenFinished;

    @Override
    EventHandler<WindowEvent> getOnCloseEventHandler() {
        return event -> {
            switch (state) {
                case RUNNING:
                    event.consume();
                    break;
                case ERROR:
                    closeApp();
                    break;
                case FINISHED:
                    event.consume();
                    continueInApp(null);
            }
        };
    }

    @FXML
    public void initialize() {

    }

    public ImageUtilsCheckDialogController() {
        synchronized (utilsFinished) {
            for (ImageUtil util : ImageUtil.values()) {
                utilsFinished.put(util, false);
            }
        }
    }

    //must be thread safe
    private boolean isAllUtilsFinished() {
        synchronized (utilsFinished) {
            for (boolean utilFinished : utilsFinished.values()) {
                if (!utilFinished) {
                    return false;
                }
            }
            return true;
        }
    }

    private void setUtilFinished(ImageUtil util, boolean finished) {
        synchronized (utilsFinished) {
            utilsFinished.put(util, finished);
        }
    }

    @Override
    void startNow() {
        state = DialogState.RUNNING;
        checkJhove();
        checkJpylyzer();
        checkImageMagick();
        checkKakadu();
    }

    public void continueInApp(ActionEvent actionEvent) {
        stage.hide();
        //app.openMainWindow();
    }

    @FXML
    private void checkJhove() {
        checkImageUtil(ImageUtil.JHOVE, jhoveProgress, jhoveStatusText, jhoveOkImg, jhoveErrorImg, jhoveBtnRetry, jhoveBtnSelectPath, jhoveBtnInstall);
    }

    @FXML
    private void checkJpylyzer() {
        checkImageUtil(ImageUtil.JPYLYZER, jpylyzerProgress, jpylyzerStatusText, jpylyzerOkImg, jpylyzerErrorImg, jpylyzerBtnRetry, jpylyzerBtnSelectPath, jpylyzerBtnInstall);
    }

    @FXML
    private void checkImageMagick() {
        checkImageUtil(ImageUtil.IMAGE_MAGICK, imageMagickProgress, imageMagickStatusText, imageMagickOkImg, imageMagickErrorImg, imageMagickBtnRetry, imageMagickBtnSelectPath, imageMagickBtnInstall);
    }

    @FXML
    private void checkKakadu() {
        checkImageUtil(ImageUtil.KAKADU, kakaduProgress, kakaduStatusText, kakaduOkImg, kakaduErrorImg, kakaduBtnRetry, kakaduBtnSelectPath, kakaduBtnInstall);
    }

    private void checkImageUtil(ImageUtil util,
                                ProgressIndicator progresIndicator,
                                Label statusLabel,
                                ImageView imgOk, ImageView imgError,
                                Button btnRetry, Button btnSelectPath, Button btnInstall
    ) {
        //show progress indicator
        progresIndicator.setVisible(true);
        //hide buttons, texts, images
        imgOk.setVisible(false);
        imgError.setVisible(false);
        statusLabel.setVisible(false);
        btnRetry.setVisible(false);
        btnSelectPath.setVisible(false);
        btnInstall.setVisible(false);
        setUtilFinished(util, false);
        btnContinue.setDisable(true);
        main.getValidationDataManager().getImageUtilManager().toString();

        Task task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                ImageUtilManager utilManager = main.getValidationDataManager().getImageUtilManager();
                try {
                    Thread.sleep(300);
                    //Thread.sleep(new Random().nextInt(3000));
                    if (!utilManager.isVersionDetectionDefined(util)) {
                        processResult(new UtilCheckResult(false, String.format("Detekce verze není definována pro %s!", util)));
                    } else {
                        String version = utilManager.runUtilVersionDetection(util);
                        //System.out.println(version);
                        main.getValidationDataManager().getFdmfRegistry().initJ2kProfiles(utilManager);
                        processResult(new UtilCheckResult(true, version));
                        utilManager.setUtilAvailable(util, true);
                    }
                } catch (CliCommand.CliCommandException e) {
                    utilManager.setUtilAvailable(util, false);
                    processResult(new UtilCheckResult(false, String.format("Chyba: %s!", e.getMessage())));
                } catch (ValidatorConfigurationException e) {
                    utilManager.setUtilAvailable(util, false);
                    processResult(new UtilCheckResult(false, String.format("Chyba: %s!", e.getMessage())));
                }
                return null;
            }

            private void processResult(UtilCheckResult result) {
                Platform.runLater(() -> {
                    progresIndicator.setVisible(false);
                    statusLabel.setVisible(true);
                    if (result.isAvailable()) {
                        //ok
                        imgOk.setVisible(true);
                        imgError.setVisible(false);
                        statusLabel.setText("verze: " + result.getMessage());
                    } else {
                        //error
                        imgOk.setVisible(false);
                        imgError.setVisible(true);
                        btnRetry.setVisible(true);
                        btnSelectPath.setVisible(true);
                        btnInstall.setVisible(true);
                        statusLabel.setText(result.getMessage());
                    }
                    setUtilFinished(util, true);
                    if (isAllUtilsFinished()) {
                        getConfigurationManager().setBoolean(ConfigurationManager.PROP_IMAGE_TOOLS_CHECK_SHOWN, true);
                        state = DialogState.FINISHED;
                        btnContinue.setDisable(false);
                        btnContinue.requestFocus();
                        if (closeWhenFinished) {
                            continueInApp(null);
                        }
                    }
                });
            }
        };
        new Thread(task).start();
    }

    public void selectJpylyzerPath(ActionEvent actionEvent) {
        File defaultDir = null;
        switch (getConfigurationManager().getPlatform().getOperatingSystem()) {
            case WINDOWS:
                defaultDir = new File("C:\\Program Files");
                break;
        }
        selectImageUtilPath(ConfigurationManager.PROP_JPYLYZER_DIR, defaultDir, ImageUtil.JPYLYZER, () -> checkJpylyzer());
    }


    public void selectJhovePath(ActionEvent actionEvent) {
        File defaultDir = null;
        switch (getConfigurationManager().getPlatform().getOperatingSystem()) {
            case WINDOWS:
                defaultDir = new File("C:\\Program Files");
                break;
        }
        selectImageUtilPath(ConfigurationManager.PROP_JHOVE_DIR, defaultDir, ImageUtil.JHOVE, () -> checkJhove());
    }

    public void selectImageMagickPath(ActionEvent actionEvent) {
        File defaultDir = null;
        switch (getConfigurationManager().getPlatform().getOperatingSystem()) {
            case WINDOWS:
                defaultDir = new File("C:\\Program Files");
                break;
        }
        selectImageUtilPath(ConfigurationManager.PROP_IMAGE_MAGICK_DIR, defaultDir, ImageUtil.IMAGE_MAGICK, () -> checkImageMagick());
    }

    public void selectKakaduPath(ActionEvent actionEvent) {
        File defaultDir = null;
        switch (getConfigurationManager().getPlatform().getOperatingSystem()) {
            case WINDOWS:
                defaultDir = new File("C:\\Program Files");
                break;
        }
        selectImageUtilPath(ConfigurationManager.PROP_KAKADU_DIR, defaultDir, ImageUtil.KAKADU, () -> checkKakadu());
    }

    private void selectImageUtilPath(String property, File defaultDir, ImageUtil util, MyListener listener) {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle(String.format("Vyberte adresář se spustitelnými soubory %s", util.getUserFriendlyName()));
        File currentDir = getConfigurationManager().getFileOrNull(property);
        if (currentDir != null && currentDir.exists()) {
            chooser.setInitialDirectory(currentDir);
        } else if (defaultDir != null && defaultDir.exists()) {
            chooser.setInitialDirectory(defaultDir);
        }
        File selectedDirectory = chooser.showDialog(stage);
        if (selectedDirectory != null) {
            getConfigurationManager().setFile(property, selectedDirectory);
            main.getValidationDataManager().getImageUtilManager().setPath(util, selectedDirectory);
            listener.onFinished();
        }
    }


    public void installJpylyzer(ActionEvent actionEvent) {
        openUrl(JPYLYZER_INSTALLATION_URL);
    }

    public void installJhove(ActionEvent actionEvent) {
        openUrl(JHOVE_INSTALLATION_URL);
    }

    public void installImageMagick(ActionEvent actionEvent) {
        openUrl(IMAGE_MAGICK_INSTALLATION_URL);
    }

    public void installKakadu(ActionEvent actionEvent) {
        openUrl(KAKADU_INSTALLATION_URL);
    }

    public void showHelp(ActionEvent actionEvent) {
        openUrl(HELP_URL);
    }

    public void setData(boolean closeWhenFinished, String mainButtonText) {
        this.closeWhenFinished = closeWhenFinished;
        btnContinue.setText(mainButtonText);
    }

    private static final class UtilCheckResult {
        private final boolean available;
        private final String message;

        public UtilCheckResult(boolean available, String message) {
            this.available = available;
            this.message = message;
        }

        public boolean isAvailable() {
            return available;
        }

        public String getMessage() {
            return message;
        }
    }

    private static interface MyListener {
        void onFinished();
    }


    public static enum DialogState {
        RUNNING, FINISHED, ERROR;
    }

}
