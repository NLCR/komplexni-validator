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
            //String description = configurationManager.getStringOrDefault("dictionary." + dictionary + ".description", null);
            String description = configurationManager.getStringOrDefault(ConfigurationManager.propDictionaryDescription(dictionary), null);
            //String url = configurationManager.getStringOrDefault("dictionary." + dictionary + ".url", null);
            String url = configurationManager.getStringOrDefault(ConfigurationManager.propDictionaryUrl(dictionary), null);
            String lastSynchronized = configurationManager.getStringOrDefault(ConfigurationManager.propDictionaryLastSynchronized(dictionary), null);

            System.out.println("found dictionary " + dictionary);
            System.out.println("description: " + description);
            System.out.println("url: " + url);
            System.out.println("last synchronized: " + lastSynchronized);

            table.add(new Label(dictionary), 0, row);
            table.add(buildShowDataButton(dictionary), 1, row);
            if (description != null && !description.isEmpty()) {
                table.add(new Label(description), 2, row);
            }
            if (url != null) {
                table.add(buildSynchronizeButton(dictionary, url), 3, row);
            }

            row++;
        }
    }

    private Node buildSynchronizeButton(String dictionary, String url) {
        Button btn = new Button("Aktualizovat", new ImageView("/img/synchronize-16.png"));
        // TODO: 4.1.19 implement
        return btn;
    }

    private Button buildShowDataButton(String dictionary) {
        // TODO: 4.1.19 otestovat v produkci (jestli se chytaji odkazy na souobory obrazku)
        //jeste funguje new ImageView("img/popup-16.png")
        Button btn = new Button("Zobrazit", new ImageView("/img/popup-16.png"));
        btn.setOnAction(event -> main.showSiglaInstitutionCodes(dictionary));
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
