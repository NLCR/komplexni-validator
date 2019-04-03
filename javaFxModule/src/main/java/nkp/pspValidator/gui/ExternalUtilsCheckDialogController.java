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
import nkp.pspValidator.shared.externalUtils.CliCommand;
import nkp.pspValidator.shared.externalUtils.ExternalUtil;
import nkp.pspValidator.shared.externalUtils.ExternalUtilManager;

import java.io.File;
import java.util.HashMap;
import java.util.Map;


/**
 * Created by Martin Řehánek on 2.12.16.
 */
public class ExternalUtilsCheckDialogController extends DialogController {

    private static final String JPYLYZER_INSTALLATION_URL = "https://github.com/NLCR/komplexni-validator/wiki/Instalace#jpylyzer";
    private static final String JHOVE_INSTALLATION_URL = "https://github.com/NLCR/komplexni-validator/wiki/Instalace#jhove";
    private static final String IMAGE_MAGICK_INSTALLATION_URL = "https://github.com/NLCR/komplexni-validator/wiki/Instalace#imagemagick";
    private static final String KAKADU_INSTALLATION_URL = "https://github.com/NLCR/komplexni-validator/wiki/Instalace#kakadu";
    // TODO: 2019-04-02 doplnit wiki
    private static final String MP3VAL_INSTALLATION_URL = "https://github.com/NLCR/komplexni-validator/wiki/Instalace#mp3val";
    private static final String SHNTOOL_INSTALLATION_URL = "https://github.com/NLCR/komplexni-validator/wiki/Instalace#shntool";
    private static final String CHECKMATE_INSTALLATION_URL = "https://github.com/NLCR/komplexni-validator/wiki/Instalace#checkmate";

    private static final String HELP_URL = "https://github.com/NLCR/komplexni-validator/wiki/Instalace#instalace-n%C3%A1stroj%C5%AF-pro-validaci-obrazov%C3%BDch-soubor%C5%AF";

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


    /*MP3val*/

    @FXML
    Label mp3valStatusText;

    @FXML
    ProgressIndicator mp3valProgress;

    @FXML
    ImageView mp3valOkImg;

    @FXML
    ImageView mp3valErrorImg;

    @FXML
    Button mp3valBtnRetry;

    @FXML
    Button mp3valBtnSelectPath;

    @FXML
    Button mp3valBtnInstall;

    /*shntool*/

    @FXML
    Label shntoolStatusText;

    @FXML
    ProgressIndicator shntoolProgress;

    @FXML
    ImageView shntoolOkImg;

    @FXML
    ImageView shntoolErrorImg;

    @FXML
    Button shntoolBtnRetry;

    @FXML
    Button shntoolBtnSelectPath;

    @FXML
    Button shntoolBtnInstall;

    /*Checkmate*/

    @FXML
    Label checkmateStatusText;

    @FXML
    ProgressIndicator checkmateProgress;

    @FXML
    ImageView checkmateOkImg;

    @FXML
    ImageView checkmateErrorImg;

    @FXML
    Button checkmateBtnRetry;

    @FXML
    Button checkmateBtnSelectPath;

    @FXML
    Button checkmateBtnInstall;


    @FXML
    Button btnContinue;

    //other data
    private boolean closeWhenFinished;
    private ExternalUtilsCheckDialogControllerStateManager stateManager = new ExternalUtilsCheckDialogControllerStateManager();
    private Map<ExternalUtil, UtilsUi> utilsUi = new HashMap<>();


    @FXML
    public void initialize() {

    }


    @Override
    public EventHandler<WindowEvent> getOnCloseEventHandler() {
        return event -> {
            switch (stateManager.getState()) {
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


    private void reflectUtilsViews() {
        //state per Util
        Map<ExternalUtil, UtilCheckResult> utilsResult = stateManager.getUtilsResult();
        for (ExternalUtil util : utilsResult.keySet()) {
            UtilCheckResult utilCheckResult = utilsResult.get(util);
            UtilsUi utilsUi = this.utilsUi.get(util);
            if (utilCheckResult == null) {//running
                //show progress indicator
                utilsUi.progresIndicator.setVisible(true);
                //hide buttons, texts, images
                utilsUi.imgOk.setVisible(false);
                utilsUi.imgError.setVisible(false);
                utilsUi.statusLabel.setVisible(false);
                utilsUi.btnRetry.setVisible(false);
                utilsUi.btnSelectPath.setVisible(false);
                utilsUi.btnInstall.setVisible(false);
            } else if (utilCheckResult.isAvailable()) {//finished
                //hide progress indicator
                utilsUi.progresIndicator.setVisible(false);
                //show ok stuff
                utilsUi.imgOk.setVisible(true);
                utilsUi.statusLabel.setVisible(true);
                utilsUi.statusLabel.setText("verze: " + utilCheckResult.getMessage());
                //hide error stuff
                utilsUi.imgError.setVisible(false);
                utilsUi.btnRetry.setVisible(false);
                utilsUi.btnSelectPath.setVisible(false);
                utilsUi.btnInstall.setVisible(false);
            } else { //error
                //hide progress indicator
                utilsUi.progresIndicator.setVisible(false);
                //hide ok stuff
                utilsUi.imgOk.setVisible(false);
                //show error stuff
                utilsUi.imgError.setVisible(true);
                utilsUi.statusLabel.setVisible(true);
                utilsUi.statusLabel.setText("chyba: " + utilCheckResult.getMessage());
                utilsUi.btnRetry.setVisible(true);
                utilsUi.btnSelectPath.setVisible(true);
                utilsUi.btnInstall.setVisible(true);
            }
        }

        //total state
        ExternalUtilsCheckDialogControllerStateManager.DialogState state = stateManager.getState();
        switch (state) {
            case RUNNING:
                btnContinue.setDisable(true);
                break;
            case FINISHED:
                getConfigurationManager().setBoolean(ConfigurationManager.PROP_EXTERNAL_TOOLS_CHECK_SHOWN, true);
                btnContinue.setDisable(false);
                btnContinue.requestFocus();
                if (closeWhenFinished) {
                    continueInApp(null);
                }
                break;
            case ERROR:
                btnContinue.setDisable(false);
                break;
        }
    }

    @Override
    public void startNow() {
        checkJhove();
        checkJpylyzer();
        checkImageMagick();
        checkKakadu();
        checkMp3val();
        checkShntool();
        checkCheckmate();
    }

    public void continueInApp(ActionEvent actionEvent) {
        stage.hide();
        //app.openMainWindow();
    }

    @FXML
    private void checkJhove() {
        checkUtil(ExternalUtil.JHOVE, jhoveProgress, jhoveStatusText, jhoveOkImg, jhoveErrorImg, jhoveBtnRetry, jhoveBtnSelectPath, jhoveBtnInstall);
    }

    @FXML
    private void checkJpylyzer() {
        checkUtil(ExternalUtil.JPYLYZER, jpylyzerProgress, jpylyzerStatusText, jpylyzerOkImg, jpylyzerErrorImg, jpylyzerBtnRetry, jpylyzerBtnSelectPath, jpylyzerBtnInstall);
    }

    @FXML
    private void checkImageMagick() {
        checkUtil(ExternalUtil.IMAGE_MAGICK, imageMagickProgress, imageMagickStatusText, imageMagickOkImg, imageMagickErrorImg, imageMagickBtnRetry, imageMagickBtnSelectPath, imageMagickBtnInstall);
    }

    @FXML
    private void checkKakadu() {
        checkUtil(ExternalUtil.KAKADU, kakaduProgress, kakaduStatusText, kakaduOkImg, kakaduErrorImg, kakaduBtnRetry, kakaduBtnSelectPath, kakaduBtnInstall);
    }

    @FXML
    public void checkMp3val() {
        checkUtil(ExternalUtil.MP3VAL, mp3valProgress, mp3valStatusText, mp3valOkImg, mp3valErrorImg, mp3valBtnRetry, mp3valBtnSelectPath, mp3valBtnInstall);
    }

    public void checkShntool() {
        checkUtil(ExternalUtil.SHNTOOL, shntoolProgress, shntoolStatusText, shntoolOkImg, shntoolErrorImg, shntoolBtnRetry, shntoolBtnSelectPath, shntoolBtnInstall);
    }

    public void checkCheckmate() {
        checkUtil(ExternalUtil.CHECKMATE, checkmateProgress, checkmateStatusText, checkmateOkImg, checkmateErrorImg, checkmateBtnRetry, checkmateBtnSelectPath, checkmateBtnInstall);
    }

    private void checkUtil(ExternalUtil util,
                           ProgressIndicator progresIndicator,
                           Label statusLabel,
                           ImageView imgOk, ImageView imgError,
                           Button btnRetry, Button btnSelectPath, Button btnInstall
    ) {

        utilsUi.put(util, new UtilsUi(progresIndicator, statusLabel, imgOk, imgError, btnRetry, btnSelectPath, btnInstall));
        stateManager.registerUtil(util);
        reflectUtilsViews();
        main.getValidationDataManager().getExternalUtilManager().toString();

        Task task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                ExternalUtilManager utilManager = main.getValidationDataManager().getExternalUtilManager();
                try {
                    Thread.sleep(300);
                    //Thread.sleep(new Random().nextInt(1000));
                    if (!utilManager.isVersionDetectionDefined(util)) {
                        processResult(new UtilCheckResult(util, false, String.format("Detekce verze není definována pro %s!", util)));
                    } else {
                        String version = utilManager.runUtilVersionDetection(util);
                        //System.out.println(version);
                        main.getValidationDataManager().getFdmfRegistry().initBinaryFileProfiles(utilManager);
                        utilManager.setUtilAvailable(util, true);
                        processResult(new UtilCheckResult(util, true, version));
                    }
                } catch (CliCommand.CliCommandException e) {
                    utilManager.setUtilAvailable(util, false);
                    processResult(new UtilCheckResult(util, false, String.format("Chyba: %s!", e.getMessage())));
                /*} catch (ValidatorConfigurationException e) {
                    utilManager.setUtilAvailable(util, false);
                    processResult(new UtilCheckResult(util, false, true, String.format("Chyba: %s!", e.getMessage())));
                */
                } catch (Throwable e) {
                    utilManager.setUtilAvailable(util, false);
                    processResult(new UtilCheckResult(util, false, String.format("Chyba: %s!", e.getMessage())));
                    e.printStackTrace();
                } finally {
                    return null;
                }
            }

            private void processResult(UtilCheckResult result) {
                stateManager.setUtilsResult(util, result);
                Platform.runLater(() -> {
                    reflectUtilsViews();

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
        selectUtilPath(ConfigurationManager.PROP_JPYLYZER_DIR, defaultDir, ExternalUtil.JPYLYZER, () -> checkJpylyzer());
    }


    public void selectJhovePath(ActionEvent actionEvent) {
        File defaultDir = null;
        switch (getConfigurationManager().getPlatform().getOperatingSystem()) {
            case WINDOWS:
                defaultDir = new File("C:\\Program Files");
                break;
        }
        selectUtilPath(ConfigurationManager.PROP_JHOVE_DIR, defaultDir, ExternalUtil.JHOVE, () -> checkJhove());
    }

    public void selectImageMagickPath(ActionEvent actionEvent) {
        File defaultDir = null;
        switch (getConfigurationManager().getPlatform().getOperatingSystem()) {
            case WINDOWS:
                defaultDir = new File("C:\\Program Files");
                break;
        }
        selectUtilPath(ConfigurationManager.PROP_IMAGE_MAGICK_DIR, defaultDir, ExternalUtil.IMAGE_MAGICK, () -> checkImageMagick());
    }

    public void selectKakaduPath(ActionEvent actionEvent) {
        File defaultDir = null;
        switch (getConfigurationManager().getPlatform().getOperatingSystem()) {
            case WINDOWS:
                defaultDir = new File("C:\\Program Files");
                break;
        }
        selectUtilPath(ConfigurationManager.PROP_KAKADU_DIR, defaultDir, ExternalUtil.KAKADU, () -> checkKakadu());
    }

    public void selectMp3valPath(ActionEvent actionEvent) {
        File defaultDir = null;
        switch (getConfigurationManager().getPlatform().getOperatingSystem()) {
            case WINDOWS:
                defaultDir = new File("C:\\Program Files");
                break;
        }
        selectUtilPath(ConfigurationManager.PROP_MP3VAL_DIR, defaultDir, ExternalUtil.MP3VAL, () -> checkMp3val());
    }


    public void selectShntoolPath(ActionEvent actionEvent) {
        File defaultDir = null;
        switch (getConfigurationManager().getPlatform().getOperatingSystem()) {
            case WINDOWS:
                defaultDir = new File("C:\\Program Files");
                break;
        }
        selectUtilPath(ConfigurationManager.PROP_SHNTOOL_DIR, defaultDir, ExternalUtil.SHNTOOL, () -> checkShntool());
    }

    public void selectCheckmatePath(ActionEvent actionEvent) {
        File defaultDir = null;
        switch (getConfigurationManager().getPlatform().getOperatingSystem()) {
            case WINDOWS:
                defaultDir = new File("C:\\Program Files");
                break;
        }
        selectUtilPath(ConfigurationManager.PROP_CHECKMATE_DIR, defaultDir, ExternalUtil.CHECKMATE, () -> checkCheckmate());
    }

    private void selectUtilPath(String property, File defaultDir, ExternalUtil util, MyListener listener) {
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
            main.getValidationDataManager().getExternalUtilManager().setPath(util, selectedDirectory);
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

    public void installMp3val(ActionEvent actionEvent) {
        openUrl(MP3VAL_INSTALLATION_URL);
    }

    public void installShntool(ActionEvent actionEvent) {
        openUrl(SHNTOOL_INSTALLATION_URL);
    }

    public void installCheckmate(ActionEvent actionEvent) {
        openUrl(CHECKMATE_INSTALLATION_URL);
    }

    public void showHelp(ActionEvent actionEvent) {
        openUrl(HELP_URL);
    }

    public void setData(boolean closeWhenFinished, String mainButtonText) {
        this.closeWhenFinished = closeWhenFinished;
        btnContinue.setText(mainButtonText);
    }

    static final class UtilCheckResult {
        private final ExternalUtil util;
        private final boolean available;
        private final String message;

        public UtilCheckResult(ExternalUtil util, boolean available, String message) {
            this.util = util;
            this.available = available;
            this.message = message;
        }

        public ExternalUtil getUtil() {
            return util;
        }

        public boolean isAvailable() {
            return available;
        }

        public String getMessage() {
            return message;
        }
    }

    private static class UtilsUi {
        final ProgressIndicator progresIndicator;
        final Label statusLabel;
        private final ImageView imgOk;
        private final ImageView imgError;
        private final Button btnRetry;
        private final Button btnSelectPath;
        private final Button btnInstall;

        public UtilsUi(ProgressIndicator progresIndicator, Label statusLabel, ImageView imgOk, ImageView imgError, Button btnRetry, Button btnSelectPath, Button btnInstall) {
            this.progresIndicator = progresIndicator;
            this.statusLabel = statusLabel;
            this.imgOk = imgOk;
            this.imgError = imgError;
            this.btnRetry = btnRetry;
            this.btnSelectPath = btnSelectPath;
            this.btnInstall = btnInstall;
        }
    }


    private interface MyListener {
        void onFinished();
    }


}
