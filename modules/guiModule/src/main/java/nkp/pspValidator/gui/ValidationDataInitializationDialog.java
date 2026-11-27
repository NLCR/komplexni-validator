package nkp.pspValidator.gui;

import javafx.stage.Stage;

import java.net.URL;

/**
 * Created by Martin Řehánek on 13.12.16.
 */
public class ValidationDataInitializationDialog extends AbstractDialog {

    private final boolean preview;
    private final String previewError;
    private final boolean previewManifestError;

    public ValidationDataInitializationDialog(Stage stage, Main main) {
        this(stage, main, false, null);
    }

    /**
     * Náhled pro SnapshotMode: dialog jen ukáže stav (inicializace běží, nebo chyba {@code previewError}) a nic
     * nenačítá.
     */
    ValidationDataInitializationDialog(Stage stage, Main main, boolean preview, String previewError) {
        this(stage, main, preview, previewError, false);
    }

    /** Jako výše; {@code previewManifestError} ukáže u chyby i tlačítko „Použít konfiguraci dodanou s aplikací“. */
    ValidationDataInitializationDialog(Stage stage, Main main, boolean preview, String previewError, boolean previewManifestError) {
        super(stage, main);
        this.preview = preview;
        this.previewError = previewError;
        this.previewManifestError = previewManifestError;
    }

    @Override
    public URL getFxmlResource() {
        return getClass().getResource("/fxml/validationDataInitialization.fxml");
    }

    @Override
    public int getWidth() {
        return 820;
    }

    @Override
    public int getHeight() {
        return 420;
    }

    @Override
    public String getTitle() {
        return "Inicializace validačních dat";
    }

    @Override
    public boolean isResizable() {
        return false;
    }

    @Override
    public void setControllerData(DialogController controller) {
        if (preview) {
            ((ValidationDataInitializationDialogController) controller).setPreview(previewError, previewManifestError);
        }
    }

}
