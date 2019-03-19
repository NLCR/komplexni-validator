package nkp.pspValidator.gui;

import javafx.stage.Stage;
import nkp.pspValidator.shared.DmfDetector;

import java.io.File;
import java.net.URL;

/**
 * Created by Martin Řehánek on 15.1.19.
 */
public class UnzipAndValidateDialog extends AbstractDialog {

    private final File pspZip;
    private final DmfDetector.Params dmfDetectorParams;
    private final boolean createTxtLog;
    private final boolean createXmlLog;
    private final int verbosity;

    public UnzipAndValidateDialog(Stage stage, Main main, File pspZip, DmfDetector.Params dmfDetectorParams, boolean createTxtLog, boolean createXmlLog, int verbosity) {
        super(stage, main);
        this.pspZip = pspZip;
        this.dmfDetectorParams = dmfDetectorParams;
        this.createTxtLog = createTxtLog;
        this.createXmlLog = createXmlLog;
        this.verbosity = verbosity;
    }

    @Override
    public URL getFxmlResource() {
        return getClass().getResource("/fxml/unzipAndValidateDialog.fxml");
    }

    @Override
    public int getWidth() {
        return 800;
    }

    @Override
    public int getHeight() {
        return 250;
    }

    @Override
    public String getTitle() {
        return "Rozbalení ZIP balíku před validací";
    }

    @Override
    public boolean isResizable() {
        return false;
    }

    @Override
    public void setControllerData(DialogController controller) {
        ((UnzipAndValidateDialogController) controller).setData(pspZip, dmfDetectorParams, createTxtLog, createXmlLog, verbosity);
    }
}
