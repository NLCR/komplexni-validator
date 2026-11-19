package nkp.pspValidator.gui.validation;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import nkp.pspValidator.gui.results.ItemStatus;
import nkp.pspValidator.gui.results.ProblemFilter;
import nkp.pspValidator.gui.results.ResultTexts;

/**
 * Společný řádek seznamu sekcí a pravidel v hlavním okně: tečka stavu (během běhu malý ukazatel), název a vpravo
 * počty problémů zobrazených úrovní, nebo stav („čeká“, „probíhá“, „přeskočeno“, „zrušeno“, „nedokončeno“). Běžící položka má tučný název. Vzhled
 * v base.css.
 */
class ResultRowSupport {

    private static final String[] DOT_CLASSES = {
            "status-dot-none", "status-dot-running", "status-dot-ok", "status-dot-warning", "status-dot-error"
    };

    final HBox row = new HBox();
    private final Region dot = new Region();
    private final ProgressIndicator spinner = new ProgressIndicator();
    final Label name = new Label();
    private final Label errors = new Label();
    private final Label warnings = new Label();
    private final Label infos = new Label();
    private final Label stateText = new Label();

    ResultRowSupport() {
        row.getStyleClass().add("result-row");
        row.setAlignment(Pos.CENTER_LEFT);
        spinner.getStyleClass().add("row-spinner");
        StackPane marker = new StackPane(dot, spinner);
        marker.getStyleClass().add("row-marker");
        name.getStyleClass().add("row-name");
        name.setMinWidth(0);
        name.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(name, Priority.ALWAYS);
        errors.getStyleClass().add("count-error");
        warnings.getStyleClass().add("count-warning");
        infos.getStyleClass().add("count-info");
        stateText.getStyleClass().add("row-state");
        for (Label label : new Label[]{errors, warnings, infos, stateText}) {
            label.setMinWidth(Region.USE_PREF_SIZE);
        }
        row.getChildren().addAll(marker, name, errors, warnings, infos, stateText);
    }

    void populate(String text, ItemStatus status, int errorCount, int warningCount, int infoCount, ProblemFilter filter) {
        name.setText(text);
        dot.getStyleClass().removeAll(DOT_CLASSES);
        dot.getStyleClass().add(status.getDotStyleClass());
        boolean running = status == ItemStatus.RUNNING;
        spinner.setVisible(running);
        dot.setVisible(!running);
        boolean finished = status.getText() == null;
        show(errors, finished && filter.isErrors() && errorCount > 0, errorCount);
        show(warnings, finished && filter.isWarnings() && warningCount > 0, warningCount);
        show(infos, finished && filter.isInfos() && infoCount > 0, infoCount);
        boolean showState = !finished;
        stateText.setText(showState ? status.getText() : null);
        stateText.setVisible(showState);
        stateText.setManaged(showState);
        name.getStyleClass().removeAll("row-name-muted", "row-name-running");
        stateText.getStyleClass().remove("row-state-running");
        if (status == ItemStatus.WAITING || status == ItemStatus.SKIPPED || status == ItemStatus.CANCELED
                || status == ItemStatus.INTERRUPTED) {
            name.getStyleClass().add("row-name-muted");
        } else if (running) {
            //bezici polozka je zvyraznena, ale neni vybrana (vyber patri uzivateli)
            name.getStyleClass().add("row-name-running");
            stateText.getStyleClass().add("row-state-running");
        }
    }

    private static void show(Label label, boolean visible, int count) {
        label.setText(visible ? String.valueOf(count) : null);
        label.setVisible(visible);
        label.setManaged(visible);
    }

    /** Text bubliny s počty všech úrovní (bez ohledu na filtr). */
    static String countsTooltip(int errorCount, int warningCount, int infoCount) {
        return ResultTexts.errors(errorCount) + ", "
                + ResultTexts.warnings(warningCount) + ", "
                + ResultTexts.infos(infoCount);
    }
}
