package nkp.pspValidator.gui;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.WindowEvent;

import java.io.File;

/**
 * Created by Martin Řehánek on 15.1.19.
 */
public class UnzipAndValidateDialogController extends DialogController {

    @FXML
    Label lblInfo;

    @FXML
    Label lblError;

    @FXML
    Button btnCancelOrClose;

    private File pspZip;

    @Override
    public void startNow() {
        lblInfo.setText("Rozbaluji soubor " + pspZip.getAbsolutePath());
        // TODO: 15.1.19 unzip into temporary dir
        // TODO: 15.1.19 validate this temporary dir just like normal psp zip 
    }

    @Override
    public EventHandler<WindowEvent> getOnCloseEventHandler() {
        return null;
    }


    public void setData(File pspZip) {
        this.pspZip = pspZip;
    }

    public void cancelOrClose(ActionEvent actionEvent) {
        // TODO: 15.1.19 cancel while unzipping, close after error
        stage.close();
    }
}
