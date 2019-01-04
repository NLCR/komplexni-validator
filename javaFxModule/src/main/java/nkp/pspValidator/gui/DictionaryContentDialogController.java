package nkp.pspValidator.gui;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.stage.WindowEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;


/**
 * Created by Martin Řehánek on 3.1.19.
 */
public class DictionaryContentDialogController extends DialogController {

    @FXML
    ListView<String> listView;

    @FXML
    Label labelDescription;

    private String dictionaryName;

    @Override
    public void startNow() {
        Set<String> values = main.getValidationDataManager().getValidatorConfigMgr().getDictionaryManager().getDictionaryValues(dictionaryName);
        List<String> valuesSorted = new ArrayList();
        valuesSorted.addAll(values);
        Collections.sort(valuesSorted);
        listView.getItems().addAll(valuesSorted);
        labelDescription.setText("Celkem " + valuesSorted.size() + " hodnot");
    }

    @Override
    public EventHandler<WindowEvent> getOnCloseEventHandler() {
        return null;
    }

    public void setData(String dictionaryName) {
        this.dictionaryName = dictionaryName;
    }

    public void closeDialog(ActionEvent actionEvent) {
        stage.close();
    }
}
