package nkp.pspValidator.gui;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.stage.WindowEvent;
import nkp.pspValidator.shared.metadataProfile.DictionaryManager;

import java.util.Set;

/**
 * Created by Martin Řehánek on 20.12.16.
 */
public class DictionariesConfigurationDialogController extends DialogController {

    @FXML
    GridPane table;

    @Override
    public void startNow() {
        DictionaryManager dictionaryManager = main.getValidationDataManager().getValidatorConfigMgr().getDictionaryManager();
        ConfigurationManager configurationManager = main.getConfigurationManager();
        Set<String> dictionaries = dictionaryManager.getDictionaries();
        //pro kazdy slovnik:
        int row = 0;
        for (String dictionary : dictionaries) {
            int column = 0;
            String description = configurationManager.getStringOrDefault(ConfigurationManager.propDictionaryDescription(dictionary), null);
            String specUrl = configurationManager.getStringOrDefault(ConfigurationManager.propDictionarySpecUrl(dictionary), null);
            String syncUrl = configurationManager.getStringOrDefault(ConfigurationManager.propDictionarySyncUrl(dictionary), null);

            Label nameLbl = new Label(dictionary);
            nameLbl.getStyleClass().add("dict-name");
            table.add(nameLbl, column++, row);
            //separator
            table.add(new Label(" "), column++, row);
            //show data button
            table.add(buildShowDataButton(dictionary, description, specUrl), column++, row);
            if (syncUrl != null) {
                //separator
                table.add(new Label(" "), column++, row);
                //synchronize button
                table.add(buildSynchronizeButton(dictionary, syncUrl), column++, row);
            }
            if (description != null && !description.isEmpty()) {
                row++;
                Label descLbl = new Label(description);
                descLbl.getStyleClass().add("dict-desc");
                table.add(descLbl, 0, row);
            }
            row++;
            //one empty line to separate items
            table.add(new Label(""), 0, row);
            row++;
        }
    }

    private Node buildSynchronizeButton(String dictionary, String syncUrl) {
        Button btn = new Button("Aktualizace", new ImageView("/img/synchronize-16.png"));
        btn.setOnAction(event -> main.showDictionaryUpdateDialog(dictionary, syncUrl));
        return btn;
    }

    private Button buildShowDataButton(String dictionary, String description, String specUrl) {
        // TODO: 4.1.19 otestovat v produkci (jestli se chytaji odkazy na souobory obrazku)
        //jeste funguje new ImageView("img/popup-16.png")
        Button btn = new Button("Zobrazit", new ImageView("/img/popup-16.png"));
        btn.setOnAction(event -> main.showDictionaryContentDialog(dictionary, description, specUrl));
        return btn;
    }


    @Override
    public EventHandler<WindowEvent> getOnCloseEventHandler() {
        return null;
    }

    public void closeDialog(ActionEvent actionEvent) {
        stage.close();
    }

}
