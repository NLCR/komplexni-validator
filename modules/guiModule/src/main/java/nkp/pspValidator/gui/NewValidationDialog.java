package nkp.pspValidator.gui;

import javafx.stage.Stage;
import nkp.pspValidator.gui.newvalidation.ValidationSource;

import java.net.URL;

/**
 * Dialog Nová validace: jeden dialog pro balík v adresáři i v souboru ZIP (nahradil dialogy PSP balík (adresář)
 * a PSP balík (zip)) a pro skupinu balíků. Logika je v balíčcích {@code nkp.pspValidator.gui.newvalidation}
 * a {@code nkp.pspValidator.gui.group}.
 */
public class NewValidationDialog extends AbstractDialog {

    /** Sekce, která se má po otevření rozbalit (snímky dialogu). */
    public enum Expand {
        VERSIONS, TOOLS, SKIPPED
    }

    private final String initialPath;
    private final Expand initialExpand;
    private final ValidationSource initialSource;

    public NewValidationDialog(Stage stage, Main main) {
        this(stage, main, null, null, null);
    }

    /**
     * @param initialPath   předvyplněná cesta (ZIP podle přípony .zip), nebo null
     * @param initialExpand rozbalená sekce, nebo null
     * @param initialSource zdroj předvyplněné cesty, nebo null (podle cesty)
     */
    public NewValidationDialog(Stage stage, Main main, String initialPath, Expand initialExpand, ValidationSource initialSource) {
        super(stage, main);
        this.initialPath = initialPath;
        this.initialExpand = initialExpand;
        this.initialSource = initialSource;
    }

    @Override
    public URL getFxmlResource() {
        return getClass().getResource("/fxml/newValidationDialog.fxml");
    }

    @Override
    public int getWidth() {
        return 760;
    }

    @Override
    public int getHeight() {
        return 640;
    }

    @Override
    public String getTitle() {
        return "Nová validace";
    }

    @Override
    public boolean isResizable() {
        return true;
    }

    @Override
    public void setControllerData(DialogController controller) {
        ((NewValidationDialogController) controller).setData(initialPath, initialExpand, initialSource);
    }
}
