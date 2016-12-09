package nkp.pspValidator.gui;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.image.ImageView;
import javafx.stage.DirectoryChooser;
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
public class ImageUtilsCheckController extends AbstractController {

    //TODO: nahradit odkazy na WIKI a jeste podle OS rozdelit
    private static final String JPYLYZER_INSTALLATION_URL = "https://github.com/openpreserve/jpylyzer/releases";
    private static final String JHOVE_INSTALLATION_URL = "http://openpreservation.org/news/jhove-1-14-released/";
    private static final String IMAGE_MAGICK_INSTALLATION_URL = "http://www.imagemagick.org/script/binary-releases.php";
    private static final String KAKADU_INSTALLATION_URL = "http://kakadusoftware.com/software/";
    private static final String HELP_URL = "http://github.com";

    @FXML
    Button btnTest;


    @FXML
    ProgressIndicator fdmfProgress;

    @FXML
    ImageView fdmfOkImg;

    @FXML
    ImageView fdmfErrorImg;

    @FXML
    Label fdmfErrorText;


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

    private ValidationDataManager validationDataManager;
    private final Map<ImageUtil, Boolean> utilsFinished = new HashMap<>();

    public ImageUtilsCheckController() {
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

    public void setValidationDataManager(ValidationDataManager validationDataManager) {
        this.validationDataManager = validationDataManager;
    }

    public void startAllChecks() {
        retryAll(null);
    }

    public void retryAll(ActionEvent actionEvent) {
        checkJhove();
        checkJpylyzer();
        checkImageMagick();
        checkKakadu();
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

        Task task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                ImageUtilManager utilManager = validationDataManager.getImageUtilManager();
                try {
                    Thread.sleep(300);
                    if (!utilManager.isVersionDetectionDefined(util)) {
                        processResult(new CheckResult(false, String.format("detekce verze není definována pro %s", util)));
                    } else {
                        String version = utilManager.runUtilVersionDetection(util);
                        //System.out.println(version);
                        validationDataManager.getFdmfRegistry().initJ2kProfiles(utilManager);
                        processResult(new CheckResult(true, version));
                        utilManager.setUtilAvailable(util, true);
                    }
                } catch (CliCommand.CliCommandException e) {
                    utilManager.setUtilAvailable(util, false);
                    processResult(new CheckResult(false, String.format("chyba: %s", e.getMessage())));
                } catch (ValidatorConfigurationException e) {
                    utilManager.setUtilAvailable(util, false);
                    processResult(new CheckResult(false, String.format("chyba: %s", e.getMessage())));
                }
                return null;
            }

            private void processResult(CheckResult result) {
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
                        btnContinue.setDisable(false);
                    }
                });
            }
        };
        new Thread(task).start();
    }

    public void selectJpylyzerPath(ActionEvent actionEvent) {
        selectImageUtilPath(ConfigurationManager.PROP_JPYLYZER_DIR, ImageUtil.JPYLYZER, () -> checkJpylyzer());
    }


    public void selectJhovePath(ActionEvent actionEvent) {
        selectImageUtilPath(ConfigurationManager.PROP_JHOVE_DIR, ImageUtil.JHOVE, () -> checkJhove());
    }

    public void selectImageMagickPath(ActionEvent actionEvent) {
        selectImageUtilPath(ConfigurationManager.PROP_IMAGE_MAGICK_DIR, ImageUtil.IMAGE_MAGICK, () -> checkImageMagick());
    }

    public void selectKakaduPath(ActionEvent actionEvent) {
        selectImageUtilPath(ConfigurationManager.PROP_KAKADU_DIR, ImageUtil.KAKADU, () -> checkKakadu());
    }

    private void selectImageUtilPath(String property, ImageUtil util, MyListener listener) {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle(String.format("Vyberte adresář se spustitelnými soubory %s", util.getUserFriendlyName()));
        File currentDir = configurationManager.getFileOrNull(property);
        if (currentDir != null) {
            chooser.setInitialDirectory(currentDir);
        }
        File selectedDirectory = chooser.showDialog(stage);
        if (selectedDirectory != null) {
            configurationManager.setFile(property, selectedDirectory);
            validationDataManager.getImageUtilManager().setPath(util, selectedDirectory);
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

    public void continueInApp(ActionEvent actionEvent) {
        app.openMainWindow();
    }

    private static final class CheckResult {
        private final boolean available;
        private final String message;

        public CheckResult(boolean available, String message) {
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


}
