package nkp.pspValidator.gui;

import javafx.stage.Stage;

import java.net.URL;

/**
 * Created by Martin Řehánek on 7.1.19.
 */
public class DictionaryUpdateDialog extends AbstractDialog {

    private final String dictionaryName;
    private final String syncUrl;

    public DictionaryUpdateDialog(Stage stage, Main main, String dictionaryName, String syncUrl) {
        super(stage, main);
        this.dictionaryName = dictionaryName;
        this.syncUrl = syncUrl;
    }

    @Override
    public URL getFxmlResource() {
        return getClass().getResource("/fxml/dictionaryUpdateDialog.fxml");
    }

    @Override
    public int getWidth() {
        return 560;
    }

    @Override
    public int getHeight() {
        return 400;
    }

    @Override
    public String getTitle() {
        return "Aktualizace slovníku";
    }

    @Override
    public boolean isResizable() {
        return false;
    }

    @Override
    public void setControllerData(DialogController controller) {
        ((DictionaryUpdateDialogController) controller).setData(dictionaryName, syncUrl);
    }
}
