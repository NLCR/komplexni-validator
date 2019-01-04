package nkp.pspValidator.gui;

import javafx.stage.Stage;

import java.net.URL;

/**
 * Created by Martin Řehánek on 3.1.19.
 */
public class DictionaryContentDialog extends AbstractDialog {

    private final String dictionaryName;

    public DictionaryContentDialog(Stage stage, Main main, String dictionaryName) {
        super(stage, main);
        this.dictionaryName = dictionaryName;
    }

    @Override
    public URL getFxmlResource() {
        return getClass().getResource("/fxml/dictionaryContentDialog.fxml");
    }

    @Override
    public int getWidth() {
        return 300;
    }

    @Override
    public int getHeight() {
        return 500;
    }

    @Override
    public String getTitle() {
        return "slovník " + dictionaryName;
    }

    @Override
    public boolean isResizable() {
        return false;
    }

    @Override
    public void setControllerData(DialogController controller) {
        ((DictionaryContentDialogController) controller).setData(dictionaryName);
    }
}
