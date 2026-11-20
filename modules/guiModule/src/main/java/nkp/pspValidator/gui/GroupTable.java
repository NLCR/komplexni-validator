package nkp.pspValidator.gui;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import nkp.pspValidator.gui.group.GroupColumns;
import nkp.pspValidator.gui.group.GroupDetailTexts;
import nkp.pspValidator.gui.group.GroupItem;
import nkp.pspValidator.gui.group.PackageStatus;

import java.util.List;

/**
 * Tabulka balíků skupiny v hlavním okně: sloupce, texty a řazení z {@link GroupColumns} (bez JavaFX), filtr
 * „jen nevalidní“ a obnova řádku po změně stavu balíku. Řádky jsou objekty {@link GroupItem}, které se mění jen ve vlákně
 * UI; po změně je potřeba zavolat {@link #itemChanged(GroupItem)}, aby se řádek překreslil, přeřadil a přefiltroval.
 */
final class GroupTable {

    private final TableView<GroupItem> table;
    private final ObservableList<GroupItem> items = FXCollections.observableArrayList();
    private final FilteredList<GroupItem> filtered = new FilteredList<>(items, item -> true);
    private final SortedList<GroupItem> sorted = new SortedList<>(filtered);
    private boolean onlyInvalid;

    GroupTable(TableView<GroupItem> table) {
        this.table = table;
        sorted.comparatorProperty().bind(table.comparatorProperty());
        table.setItems(sorted);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        for (GroupColumns column : GroupColumns.values()) {
            table.getColumns().add(column(column));
        }
    }

    private static TableColumn<GroupItem, GroupItem> column(GroupColumns column) {
        TableColumn<GroupItem, GroupItem> result = new TableColumn<>(column.getTitle());
        result.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue()));
        result.setComparator(column.comparator());
        result.setReorderable(false);
        //sirka navic jde do nazvu a typu (ostatni sloupce maji maximum)
        switch (column) {
            case NAME:
                result.setPrefWidth(320);
                result.setMinWidth(160);
                break;
            case TYPE:
                result.setPrefWidth(230);
                result.setMinWidth(120);
                break;
            case RESULT:
                result.setPrefWidth(120);
                result.setMinWidth(118);
                result.setMaxWidth(140);
                break;
            case DURATION:
                result.setPrefWidth(80);
                result.setMinWidth(70);
                result.setMaxWidth(100);
                break;
            case ERRORS:
                result.setPrefWidth(70);
                result.setMinWidth(64);
                result.setMaxWidth(90);
                break;
            default:
                //Varovani, Informace: nadpis se musi vejit cely
                result.setPrefWidth(96);
                result.setMinWidth(92);
                result.setMaxWidth(110);
                break;
        }
        result.setCellFactory(col -> new TableCell<GroupItem, GroupItem>() {
            private final Label badge = new Label();

            @Override
            protected void updateItem(GroupItem item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().removeAll("group-number", "group-muted");
                setTooltip(null);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }
                switch (column) {
                    case RESULT:
                        badge.setText(item.getStatus().getLabel());
                        badge.getStyleClass().setAll("label", "status-badge", item.getStatus().getStyleClass());
                        setText(null);
                        setGraphic(badge);
                        if (item.getMessage() != null) {
                            setTooltip(new Tooltip(item.getMessage()));
                        }
                        break;
                    case NAME:
                        setText(column.text(item));
                        setGraphic(null);
                        setTooltip(new Tooltip(GroupDetailTexts.nameTooltip(item)));
                        break;
                    case TYPE:
                        setText(column.text(item));
                        setGraphic(null);
                        getStyleClass().add("group-muted");
                        break;
                    default:
                        setText(column.text(item));
                        setGraphic(null);
                        getStyleClass().add("group-number");
                        break;
                }
            }
        });
        return result;
    }

    /** Nová skupina (předchozí řádky pryč, řazení zůstává podle uživatele). */
    void setItems(List<GroupItem> groupItems) {
        items.setAll(groupItems);
    }

    /** Stav balíku se změnil: řádek se překreslí a znovu zařadí do řazení a filtru; výběr zůstává. */
    void itemChanged(GroupItem item) {
        int index = items.indexOf(item);
        if (index < 0) {
            return;
        }
        items.set(index, item);
        //tentyz objekt na radku: bunky by se jinak neprekreslily
        table.refresh();
    }

    void setOnlyInvalid(boolean onlyInvalid) {
        this.onlyInvalid = onlyInvalid;
        filtered.setPredicate(GroupColumns.filter(onlyInvalid)::test);
    }

    boolean isOnlyInvalid() {
        return onlyInvalid;
    }

    int shownCount() {
        return sorted.size();
    }

    int totalCount() {
        return items.size();
    }

    /** Řádky v pořadí, v jakém je tabulka zobrazuje (pro export CSV a výběr). */
    List<GroupItem> shownItems() {
        return sorted;
    }

    /** První zobrazený nevalidní balík nebo balík s chybou, jinak první zobrazený, null u prázdné tabulky. */
    GroupItem firstProblemOrFirst() {
        for (GroupItem item : sorted) {
            if (item.getStatus() == PackageStatus.INVALID || item.getStatus() == PackageStatus.ERROR) {
                return item;
            }
        }
        return sorted.isEmpty() ? null : sorted.get(0);
    }
}
