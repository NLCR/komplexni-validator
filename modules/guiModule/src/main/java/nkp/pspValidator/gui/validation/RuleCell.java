package nkp.pspValidator.gui.validation;

import javafx.scene.control.ListCell;
import javafx.scene.control.Tooltip;
import nkp.pspValidator.gui.results.ItemStatus;
import nkp.pspValidator.gui.results.ProblemFilter;

import java.util.function.Supplier;

/**
 * Řádek seznamu pravidel vybrané sekce v hlavním okně. Celý název a popis pravidla jsou v bublině.
 */
public class RuleCell extends ListCell<RuleWithState> {

    private final Supplier<ProblemFilter> filter;
    private final ResultRowSupport support = new ResultRowSupport();
    private final Tooltip tooltip = new Tooltip();

    public RuleCell(Supplier<ProblemFilter> filter) {
        this.filter = filter;
        setPrefWidth(0);
        tooltip.setWrapText(true);
        tooltip.setMaxWidth(480);
    }

    @Override
    protected void updateItem(RuleWithState rule, boolean empty) {
        super.updateItem(rule, empty);
        setText(null);
        if (empty || rule == null) {
            setGraphic(null);
            setTooltip(null);
        } else {
            ItemStatus status = ItemStatus.of(rule.getState(), rule.getErrors(), rule.getWarnings());
            support.populate(rule.getName(), status, rule.getErrors(), rule.getWarnings(), rule.getInfos(), filter.get());
            StringBuilder text = new StringBuilder(rule.getName());
            if (rule.getDescription() != null && !rule.getDescription().trim().isEmpty()) {
                text.append('\n').append(rule.getDescription().trim());
            }
            text.append('\n').append(status.getText() != null ? status.getText()
                    : ResultRowSupport.countsTooltip(rule.getErrors(), rule.getWarnings(), rule.getInfos()));
            tooltip.setText(text.toString());
            setTooltip(tooltip);
            setGraphic(support.row);
        }
    }
}
