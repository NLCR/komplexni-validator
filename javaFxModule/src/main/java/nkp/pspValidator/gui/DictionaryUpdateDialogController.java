package nkp.pspValidator.gui;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.stage.WindowEvent;

import java.util.Set;


/**
 * Created by Martin Řehánek on 7.1.19.
 */
public class DictionaryUpdateDialogController extends DialogController {


    private String dictionaryName;
    private String syncUrl;
    private String syncDate;

    private boolean updatingNow = false;

    @FXML
    Label lblDictName;

    @FXML
    Label lblTotalValues;

    @FXML
    Label lblLastUpdated;

    @FXML
    Button btnUpdate;

    @FXML
    Button btnCloseOrCancel;

    @FXML
    ProgressIndicator progressIndicator;

    @Override
    public void startNow() {
        lblDictName.setText(dictionaryName);
        updateUi();
    }

    @Override
    public EventHandler<WindowEvent> getOnCloseEventHandler() {
        return event -> {
            if (updatingNow) {
                //ignore event
                event.consume();
            }
        };
    }

    public void setData(String dictionaryName, String syncUrl, String syncDate) {
        this.dictionaryName = dictionaryName;
        this.syncUrl = syncUrl;
        this.syncDate = syncDate;
    }


    public void closeDialog(ActionEvent actionEvent) {
        stage.close();
    }

    public void update(ActionEvent actionEvent) {
        // TODO: 7.1.19 implement properly
        updatingNow = true;
        updateUi();
    }

    private void updateUi() {
        Set<String> values = main.getValidationDataManager().getValidatorConfigMgr().getDictionaryManager().getDictionaryValues(dictionaryName);
        if (syncDate == null) {
            lblLastUpdated.setText("-");
        }
        lblTotalValues.setText("" + values.size());

        if (updatingNow) {
            btnUpdate.setDisable(true);
            btnCloseOrCancel.setText("Zrušit");
            progressIndicator.setVisible(true);
        } else {
            btnUpdate.setDisable(false);
            btnCloseOrCancel.setText("Zavřít");
            progressIndicator.setVisible(false);
        }
    }

    public void closeOrCancel(ActionEvent actionEvent) {
        if (!updatingNow) {
            stage.close();
        } else {
            // TODO: 7.1.19 implement properly
            updatingNow = false;
            updateUi();
        }
    }
}
