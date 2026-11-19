package nkp.pspValidator.gui;

import javafx.stage.Stage;
import nkp.pspValidator.gui.settings.SettingsTab;
import nkp.pspValidator.shared.Dmf;

import java.net.URL;

/**
 * Okno Nastavení se záložkami (výchozí verze DMF, externí nástroje, přeskakovaná pravidla, slovníky, adresáře
 * a protokoly). Nahradilo dialogy Přeskakování validací a Slovníky a položky menu Nastavení; dialog Externí nástroje
 * zůstává jen pro start aplikace. Logika je v balíčku {@code nkp.pspValidator.gui.settings}.
 */
public class SettingsDialog extends AbstractDialog {

    private final SettingsTab initialTab;
    private final Dmf focusDmf;

    /**
     * @param initialTab záložka po otevření, null = první
     * @param focusDmf   fDMF, kterou záložka Přeskakovaná pravidla ukáže první (z dialogu Nová validace), nebo null
     */
    public SettingsDialog(Stage stage, Main main, SettingsTab initialTab, Dmf focusDmf) {
        super(stage, main);
        this.initialTab = initialTab;
        this.focusDmf = focusDmf;
    }

    @Override
    public URL getFxmlResource() {
        return getClass().getResource("/fxml/settingsDialog.fxml");
    }

    @Override
    public int getWidth() {
        return 820;
    }

    @Override
    public int getHeight() {
        return 620;
    }

    @Override
    public String getTitle() {
        return "Nastavení";
    }

    @Override
    public boolean isResizable() {
        return true;
    }

    @Override
    public void setControllerData(DialogController controller) {
        ((SettingsDialogController) controller).setData(initialTab, focusDmf);
    }
}
