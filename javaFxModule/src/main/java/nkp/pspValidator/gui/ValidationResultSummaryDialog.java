package nkp.pspValidator.gui;

import javafx.stage.Stage;

import java.net.URL;

/**
 * Created by Martin Řehánek on 13.12.16.
 */
public class ValidationResultSummaryDialog extends AbstractDialog {

    private final ValidationResultSummary summary;

    public ValidationResultSummaryDialog(Stage stage, Main main, ValidationResultSummary summary) {
        super(stage, main);
        this.summary = summary;
    }

    @Override
    public URL getFxmlResource() {
        return getClass().getResource("/fxml/validationResultsSummaryDialog.fxml");
    }

    @Override
    public int getWidth() {
        return 700;
    }

    @Override
    public int getHeight() {
        return 500;
    }

    @Override
    public String getTitle() {
        return "Sumarizace výsledků validace";
    }

    @Override
    public boolean isResizable() {
        return false;
    }

    @Override
    public void setControllerData(DialogController controller) {
        ((ValidationResultsSummaryDialogController) controller).setData(summary);
    }

}
