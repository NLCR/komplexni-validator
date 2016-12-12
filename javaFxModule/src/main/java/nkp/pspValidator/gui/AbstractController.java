package nkp.pspValidator.gui;

import com.sun.deploy.uitoolkit.impl.fx.HostServicesFactory;
import com.sun.javafx.application.HostServicesDelegate;
import javafx.application.Application;
import javafx.stage.Stage;
import nkp.pspValidator.shared.imageUtils.ImageUtil;

/**
 * Created by martin on 9.12.16.
 */
public abstract class AbstractController extends Application {

    protected Stage stage;
    protected Main app;
    protected ConfigurationManager configurationManager;

    @Override
    public void start(Stage stage) throws Exception {
        this.stage = stage;
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public void setApp(Main app) {
        this.app = app;
    }

    public void setConfigurationManager(ConfigurationManager configurationManager) {
        this.configurationManager = configurationManager;
    }

    protected void openUrl(String url) {
        HostServicesDelegate hostServices = HostServicesFactory.getInstance(this);
        getHostServices().showDocument(url);
    }
}
