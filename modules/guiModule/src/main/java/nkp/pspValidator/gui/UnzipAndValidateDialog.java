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
    /** náhled pro SnapshotMode: text stavu, nic se nerozbaluje; null = skutečné rozbalení */
    private String previewMessage;
    private boolean previewError;

    public UnzipAndValidateDialog(Stage stage, Main main, File pspZip, DmfDetector.Params dmfDetectorParams, boolean createTxtLog, boolean createXmlLog, int verbosity) {
        super(stage, main);
        this.pspZip = pspZip;
        this.dmfDetectorParams = dmfDetectorParams;
        this.createTxtLog = createTxtLog;
        this.createXmlLog = createXmlLog;
        this.verbosity = verbosity;
    }

    /** Náhled pro SnapshotMode: dialog jen ukáže text stavu (u chyby bez ukazatele průběhu) a nic nerozbalí. */
    UnzipAndValidateDialog preview(String message, boolean error) {
        this.previewMessage = message;
        this.previewError = error;
        return this;
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
        return 260;
    }

    @Override
    public String getTitle() {
        return "Rozbalení souboru ZIP";
    }

    @Override
    public boolean isResizable() {
        return false;
    }

    @Override
    public void setControllerData(DialogController controller) {
        ((UnzipAndValidateDialogController) controller).setData(pspZip, dmfDetectorParams, createTxtLog, createXmlLog, verbosity);
        if (previewMessage != null) {
            ((UnzipAndValidateDialogController) controller).setPreview(previewMessage, previewError);
        }
    }
}
