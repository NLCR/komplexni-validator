package nkp.pspValidator.gui;

import javafx.event.EventHandler;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import java.util.logging.Logger;

/**
 * Created by Martin Řehánek on 9.12.16.
 */
public abstract class DialogController extends AbstractController {

    private static Logger LOG = Logger.getLogger(DialogController.class.getSimpleName());

    protected Stage stage;

    @Override
    public void start(Stage stage) throws Exception {
        this.stage = stage;
    }

    public ConfigurationManager getConfigurationManager() {
        return main.getConfigurationManager();
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public abstract void startNow();

    public abstract EventHandler<WindowEvent> getOnCloseEventHandler();

}
