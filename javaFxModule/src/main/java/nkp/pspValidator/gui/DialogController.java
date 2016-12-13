package nkp.pspValidator.gui;

import javafx.stage.Stage;

import java.util.logging.Logger;

/**
 * Created by martin on 9.12.16.
 */
public abstract class DialogController extends AbstractController {

    private static Logger LOG = Logger.getLogger(DialogController.class.getSimpleName());

    protected Stage stage;
    //@Deprecated
    //protected Main app;
    //protected ConfigurationManager configurationManager;
    //protected MainController mainController;


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

    /*@Deprecated
    public void setApp(Main app) {
        this.app = app;
    }*/


    @Deprecated
    public void setConfigurationManager(ConfigurationManager configurationManager) {
        //this.configurationManager = configurationManager;
        onConfigurationManagerSet();
    }


    abstract void onConfigurationManagerSet();


}
