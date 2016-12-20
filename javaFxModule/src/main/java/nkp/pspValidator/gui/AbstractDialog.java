package nkp.pspValidator.gui;

import javafx.stage.Stage;

/**
 * Created by martin on 20.12.16.
 */
public class AbstractDialog {

    final Stage stage;
    final Main main;

    public AbstractDialog(Stage stage, Main main) {
        this.stage = stage;
        this.main = main;
    }
}
