package nkp.pspValidator.gui.validation;

import javafx.scene.control.ListCell;
import javafx.scene.control.Tooltip;
import nkp.pspValidator.gui.results.ItemStatus;
import nkp.pspValidator.gui.results.ProblemFilter;

import java.util.function.Supplier;

/**
 * Řádek seznamu sekcí v hlavním okně.
 */
public class SectionCell extends ListCell<SectionWithState> {

    private final Supplier<ProblemFilter> filter;
    private final ResultRowSupport support = new ResultRowSupport();
    private final Tooltip tooltip = new Tooltip();

    public SectionCell(Supplier<ProblemFilter> filter) {
        this.filter = filter;
        //bunka nesmi roztahovat seznam do sirky (dlouhy nazev se zkrati)
        setPrefWidth(0);
        tooltip.setWrapText(true);
        tooltip.setMaxWidth(480);
    }

    @Override
    protected void updateItem(SectionWithState section, boolean empty) {
        super.updateItem(section, empty);
        setText(null);
        if (empty || section == null) {
            setGraphic(null);
            setTooltip(null);
        } else {
            ItemStatus status = ItemStatus.of(section.getState(), section.getErrors(), section.getWarnings());
            support.populate(section.getName(), status, section.getErrors(), section.getWarnings(), section.getInfos(), filter.get());
            StringBuilder text = new StringBuilder(section.getName());
            if (section.getDescription() != null && !section.getDescription().trim().isEmpty()) {
                text.append('\n').append(section.getDescription().trim());
            }
            text.append('\n').append(status.getText() != null ? status.getText()
                    : ResultRowSupport.countsTooltip(section.getErrors(), section.getWarnings(), section.getInfos()));
            tooltip.setText(text.toString());
            setTooltip(tooltip);
            setGraphic(support.row);
        }
    }
}
