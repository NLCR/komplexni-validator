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
    Button jhoveBtnInstall;

    @FXML
    Button jhoveBtnSelectPath;

    @FXML
    Button jhoveBtnRemovePath;

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
    Button jpylyzerBtnInstall;

    @FXML
    Button jpylyzerBtnSelectPath;

    @FXML
    Button jpylyzerBtnRemovePath;

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
    Button imageMagickBtnInstall;

    @FXML
    Button imageMagickBtnSelectPath;

    @FXML
    Button imageMagickBtnRemovePath;

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
    Button kakaduBtnInstall;

    @FXML
    Button kakaduBtnSelectPath;

    @FXML
    Button kakaduBtnRemovePath;

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
    Button mp3valBtnInstall;

    @FXML
    Button mp3valBtnSelectPath;

    @FXML
    Button mp3valBtnRemovePath;

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
    Button shntoolBtnInstall;

    @FXML
    Button shntoolBtnSelectPath;

    @FXML
    Button shntoolBtnRemovePath;

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
    Button checkmateBtnInstall;

    @FXML
    Button checkmateBtnSelectPath;

    @FXML
    Button checkmateBtnRemovePath;


    @FXML
    Button btnContinue;

    //other data
    private boolean closeWhenFinished;
    private ExternalUtilsCheckDialogControllerStateManager stateManager = new ExternalUtilsCheckDialogControllerStateManager();
    private Map<ExternalUtil, UtilsUi> utilsUi = new HashMap<>();


    @FXML
    public void initialize() {

    }

    public void setData(boolean closeWhenFinished, String mainButtonText) {
        this.closeWhenFinished = closeWhenFinished;
        btnContinue.setText(mainButtonText);
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
        checkUtil(ExternalUtil.JHOVE, jhoveProgress, jhoveStatusText, jhoveOkImg, jhoveErrorImg, jhoveBtnRetry, jhoveBtnInstall, jhoveBtnSelectPath, jhoveBtnRemovePath);
    }

    @FXML
    private void checkJpylyzer() {
        checkUtil(ExternalUtil.JPYLYZER, jpylyzerProgress, jpylyzerStatusText, jpylyzerOkImg, jpylyzerErrorImg, jpylyzerBtnRetry, jpylyzerBtnInstall, jpylyzerBtnSelectPath, jpylyzerBtnRemovePath);
    }

    @FXML
    private void checkImageMagick() {
        checkUtil(ExternalUtil.IMAGE_MAGICK, imageMagickProgress, imageMagickStatusText, imageMagickOkImg, imageMagickErrorImg, imageMagickBtnRetry, imageMagickBtnInstall, imageMagickBtnSelectPath, imageMagickBtnRemovePath);
    }

    @FXML
    private void checkKakadu() {
        checkUtil(ExternalUtil.KAKADU, kakaduProgress, kakaduStatusText, kakaduOkImg, kakaduErrorImg, kakaduBtnRetry, kakaduBtnInstall, kakaduBtnSelectPath, kakaduBtnRemovePath);
    }

    @FXML
    public void checkMp3val() {
        checkUtil(ExternalUtil.MP3VAL, mp3valProgress, mp3valStatusText, mp3valOkImg, mp3valErrorImg, mp3valBtnRetry, mp3valBtnInstall, mp3valBtnSelectPath, mp3valBtnRemovePath);
    }

    public void checkShntool() {
        checkUtil(ExternalUtil.SHNTOOL, shntoolProgress, shntoolStatusText, shntoolOkImg, shntoolErrorImg, shntoolBtnRetry, shntoolBtnInstall, shntoolBtnSelectPath, shntoolBtnRemovePath);
    }

    public void checkCheckmate() {
        checkUtil(ExternalUtil.CHECKMATE, checkmateProgress, checkmateStatusText, checkmateOkImg, checkmateErrorImg, checkmateBtnRetry, checkmateBtnInstall, checkmateBtnSelectPath, checkmateBtnRemovePath);
    }

    private void checkUtil(ExternalUtil util,
                           ProgressIndicator progresIndicator,
                           Label statusLabel,
                           ImageView imgOk, ImageView imgError,
                           Button btnRetry, Button btnInstall,
                           Button btnSelectPath, Button btnRemovePath
    ) {

        utilsUi.put(util, new UtilsUi(progresIndicator, statusLabel, imgOk, imgError, btnRetry, btnInstall, btnSelectPath, btnRemovePath));
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
                utilsUi.btnInstall.setVisible(false);
                utilsUi.btnSelectPath.setVisible(false);
                utilsUi.btnRemovePath.setVisible(false);
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
                utilsUi.btnInstall.setVisible(false);
                utilsUi.btnSelectPath.setVisible(false);
                utilsUi.btnRemovePath.setVisible(false);
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
                utilsUi.btnInstall.setVisible(true);
                utilsUi.btnSelectPath.setVisible(true);
                utilsUi.btnRemovePath.setVisible(true);
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


    public void selectJhovePath(ActionEvent actionEvent) {
        selectUtilPath(ConfigurationManager.PROP_JHOVE_DIR, ExternalUtil.JHOVE, () -> checkJhove());
    }

    public void selectJpylyzerPath(ActionEvent actionEvent) {
        selectUtilPath(ConfigurationManager.PROP_JPYLYZER_DIR, ExternalUtil.JPYLYZER, () -> checkJpylyzer());
    }

    public void selectImageMagickPath(ActionEvent actionEvent) {
        selectUtilPath(ConfigurationManager.PROP_IMAGE_MAGICK_DIR, ExternalUtil.IMAGE_MAGICK, () -> checkImageMagick());
    }

    public void selectKakaduPath(ActionEvent actionEvent) {
        selectUtilPath(ConfigurationManager.PROP_KAKADU_DIR, ExternalUtil.KAKADU, () -> checkKakadu());
    }

    public void selectMp3valPath(ActionEvent actionEvent) {
        selectUtilPath(ConfigurationManager.PROP_MP3VAL_DIR, ExternalUtil.MP3VAL, () -> checkMp3val());
    }


    public void selectShntoolPath(ActionEvent actionEvent) {
        selectUtilPath(ConfigurationManager.PROP_SHNTOOL_DIR, ExternalUtil.SHNTOOL, () -> checkShntool());
    }

    public void selectCheckmatePath(ActionEvent actionEvent) {
        selectUtilPath(ConfigurationManager.PROP_CHECKMATE_DIR, ExternalUtil.CHECKMATE, () -> checkCheckmate());
    }

    private void selectUtilPath(String property, ExternalUtil util, MyListener listener) {
        File defaultDir = null;
        switch (getConfigurationManager().getPlatform().getOperatingSystem()) {
            case WINDOWS:
                defaultDir = new File("C:\\Program Files");
                break;
        }
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

    private void removeUtilPath(String property, ExternalUtil util, MyListener listener) {
        getConfigurationManager().setFile(property, null);
        main.getValidationDataManager().getExternalUtilManager().setPath(util, null);
        listener.onFinished();
    }

    public void removeJhovePath(ActionEvent actionEvent) {
        removeUtilPath(ConfigurationManager.PROP_JHOVE_DIR, ExternalUtil.JHOVE, () -> checkJhove());
    }

    public void removeJpylyzerPath(ActionEvent actionEvent) {
        removeUtilPath(ConfigurationManager.PROP_JPYLYZER_DIR, ExternalUtil.JPYLYZER, () -> checkJpylyzer());
    }

    public void removeImageMagickPath(ActionEvent actionEvent) {
        removeUtilPath(ConfigurationManager.PROP_IMAGE_MAGICK_DIR, ExternalUtil.IMAGE_MAGICK, () -> checkImageMagick());
    }

    public void removeKakaduPath(ActionEvent actionEvent) {
        removeUtilPath(ConfigurationManager.PROP_KAKADU_DIR, ExternalUtil.KAKADU, () -> checkKakadu());
    }

    public void removeMp3valPath(ActionEvent actionEvent) {
        removeUtilPath(ConfigurationManager.PROP_MP3VAL_DIR, ExternalUtil.MP3VAL, () -> checkMp3val());
    }

    public void removeShntoolPath(ActionEvent actionEvent) {
        removeUtilPath(ConfigurationManager.PROP_SHNTOOL_DIR, ExternalUtil.SHNTOOL, () -> checkShntool());
    }

    public void removeCheckmatePath(ActionEvent actionEvent) {
        removeUtilPath(ConfigurationManager.PROP_CHECKMATE_DIR, ExternalUtil.CHECKMATE, () -> checkCheckmate());
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
        private final Button btnInstall;
        private final Button btnSelectPath;
        private final Button btnRemovePath;

        public UtilsUi(ProgressIndicator progresIndicator, Label statusLabel, ImageView imgOk, ImageView imgError, Button btnRetry, Button btnInstall, Button btnSelectPath, Button btnRemovePath) {
            this.progresIndicator = progresIndicator;
            this.statusLabel = statusLabel;
            this.imgOk = imgOk;
            this.imgError = imgError;
            this.btnRetry = btnRetry;
            this.btnInstall = btnInstall;
            this.btnSelectPath = btnSelectPath;
            this.btnRemovePath = btnRemovePath;
        }
    }


    private interface MyListener {
        void onFinished();
    }


}
