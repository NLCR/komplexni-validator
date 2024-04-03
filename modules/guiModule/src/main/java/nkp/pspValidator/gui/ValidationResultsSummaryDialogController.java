package nkp.pspValidator.gui;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.DirectoryChooser;
import javafx.stage.WindowEvent;
import nkp.pspValidator.shared.StringUtils;
import nkp.pspValidator.shared.engine.Level;

import java.io.File;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Created by Martin Řehánek on 9.12.16.
 */
public class ValidationResultsSummaryDialogController extends DialogController {

    private static Logger LOG = Logger.getLogger(ValidationResultsSummaryDialogController.class.getSimpleName());


    @FXML
    Text pspDirText;
    @FXML
    Text dmfVersion;

    @FXML
    Text totalTimeText;
    @FXML
    Text totalErrors;
    @FXML
    Text totalWarnings;
    @FXML
    Text totalInfos;
    @FXML
    Text validationResult;

    private ValidationResultSummary summary;


    public void setData(ValidationResultSummary summary) {
        this.summary = summary;
    }

    @Override
    public void startNow() {
        pspDirText.setText(summary.getPspDir().getAbsolutePath());
        dmfVersion.setText(summary.getDmf().toString());

        totalTimeText.setText(StringUtils.formatMilliseconds(summary.getTotalTime()));
        totalErrors.setText(problemsOrZero(summary.getGlobalProblemsByLevel(), Level.ERROR));
        totalWarnings.setText(problemsOrZero(summary.getGlobalProblemsByLevel(), Level.WARNING));
        totalInfos.setText(problemsOrZero(summary.getGlobalProblemsByLevel(), Level.INFO));
        if (summary.isValid()) {
            validationResult.setText("VALIDNÍ");
            validationResult.setFill(Color.GREEN);
        } else {
            validationResult.setText("NEVALIDNÍ");
            validationResult.setFill(Color.RED);
        }
    }

    private String problemsOrZero(Map<Level, Integer> map, Level level) {
        Integer fromMap = map.get(level);
        int count = fromMap == null ? 0 : fromMap;
        return count + "x";
    }

    @Override
    public EventHandler<WindowEvent> getOnCloseEventHandler() {
        return null;
    }

    public void continueInApp(ActionEvent actionEvent) {
        boolean shown = getConfigurationManager().getBooleanOrDefault(ConfigurationManager.PROP_EXTERNAL_TOOLS_CHECK_SHOWN, false);
        main.showExternalUtilsCheckDialog(shown, "Pokračovat");
    }

    public void setFdmfsRootDir(ActionEvent actionEvent) {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Vyberte adresář s konfigurací Validátoru");
        File currentDir = getConfigurationManager().getFileOrNull(ConfigurationManager.PROP_VALIDATOR_CONFIG_DIR);
        if (currentDir != null && currentDir.exists()) {
            chooser.setInitialDirectory(currentDir);
        }
        File selectedDirectory = chooser.showDialog(stage);
        if (selectedDirectory != null) {
            getConfigurationManager().setFile(ConfigurationManager.PROP_VALIDATOR_CONFIG_DIR, selectedDirectory);
            startNow();
        }
    }

    public void closeDialog(ActionEvent actionEvent) {
        stage.close();
    }
}
