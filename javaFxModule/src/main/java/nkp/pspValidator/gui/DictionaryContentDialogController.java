package nkp.pspValidator.gui;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Hyperlink;
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
    Label lblItemsTotal;

    @FXML
    Label lblDescription;

    @FXML
    Hyperlink specHyperlink;

    private String dictionaryName;
    private String description;
    private String specUrl;

    @Override
    public void startNow() {
        Set<String> values = main.getValidationDataManager().getValidatorConfigMgr().getDictionaryManager().getDictionaryValues(dictionaryName);
        List<String> valuesSorted = new ArrayList();
        valuesSorted.addAll(values);
        Collections.sort(valuesSorted);
        listView.getItems().addAll(valuesSorted);
        lblItemsTotal.setText("Celkem " + valuesSorted.size() + " hodnot");
        lblDescription.setText(description);
        if (specUrl != null && !specUrl.isEmpty()) {
            specHyperlink.setText(specUrl);
        } else {
            specHyperlink.setVisible(false);
        }
    }

    @Override
    public EventHandler<WindowEvent> getOnCloseEventHandler() {
        return null;
    }

    public void setData(String dictionaryName, String description, String specUrl) {
        this.dictionaryName = dictionaryName;
        this.description = description;
        this.specUrl = specUrl;
    }

    public void closeDialog(ActionEvent actionEvent) {
        stage.close();
    }

    public void openHyperlink(ActionEvent actionEvent) {
        openUrl(((Hyperlink) actionEvent.getSource()).getText());
    }
}
