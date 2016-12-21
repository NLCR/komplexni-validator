package nkp.pspValidator.gui;

import javafx.stage.Stage;

import java.net.URL;

/**
 * Created by martin on 13.12.16.
 */
public class ValidationResultSummaryDialog extends AbstractDialog {

    private final ValidationResultSummary summary;

    public ValidationResultSummaryDialog(Stage stage, Main main, ValidationResultSummary summary) {
        super(stage, main);
        this.summary = summary;
    }

    @Override
    URL getFxmlResource() {
        return getClass().getResource("/fxml/validationResulSummaryDialog.fxml");
    }

    @Override
    int getWidth() {
        return 700;
    }

    @Override
    int getHeight() {
        return 500;
    }

    @Override
    String getTitle() {
        return "Sumarizace výsledků validace";
    }

    @Override
    boolean isResizable() {
        return false;
    }

    @Override
    void setControllerData(DialogController controller) {
        ((ValidationResultsSummaryDialogController) controller).setData(summary);
    }

}
