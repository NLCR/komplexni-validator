package nkp.pspValidator.gui.dev;

import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.text.Text;
import javafx.stage.WindowEvent;
import nkp.pspValidator.gui.DialogController;

import java.io.File;

/**
 * Created by Lenovo on 19.12.2016.
 */
public class DevDialogController extends DialogController {

    @FXML
    Text text;

    @Override
    public void startNow() {
        File pwd = new File(".");
        text.setText("pwd: " + pwd.getAbsolutePath());
    }

    @Override
    public EventHandler<WindowEvent> getOnCloseEventHandler() {
        return new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent event) {
                //nothing, event not consumed, so just closes
            }
        };
    }

}
