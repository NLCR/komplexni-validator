package nkp.pspValidator.gui.group;

import nkp.pspValidator.gui.results.ProblemCounts;
import nkp.pspValidator.gui.results.ResultTexts;

import java.util.Comparator;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Sloupce tabulky balíků skupiny: nadpis, text buňky a řazení, a filtr „jen nevalidní“. Řazení podle sloupce je
 * vzestupné (sestupné obrací tabulka); prázdné hodnoty (balík se ještě nevalidoval) jsou při vzestupném řazení na
 * konci; při shodě rozhoduje pořadí balíku ve skupině. Bez JavaFX.
 */
public enum GroupColumns {

    NAME("Balík", item -> item.getName(), Comparator.comparing(GroupItem::getName, GroupPackages.NAME_ORDER)),
    TYPE("Typ a verze", GroupItem::getTypeAndVersion, nullsLast(item -> item.getDmf() == null ? null : item.getTypeAndVersion(),
            Comparator.<String>naturalOrder())),
    RESULT("Výsledek", item -> item.getStatus().getLabel(), Comparator.comparingInt(item -> item.getStatus().sortRank())),
    ERRORS("Chyby", item -> count(item, ProblemCounts::getErrors), nullsLast(item -> countOrNull(item, ProblemCounts::getErrors), Comparator.<Integer>naturalOrder())),
    WARNINGS("Varování", item -> count(item, ProblemCounts::getWarnings), nullsLast(item -> countOrNull(item, ProblemCounts::getWarnings), Comparator.<Integer>naturalOrder())),
    INFOS("Informace", item -> count(item, ProblemCounts::getInfos), nullsLast(item -> countOrNull(item, ProblemCounts::getInfos), Comparator.<Integer>naturalOrder())),
    DURATION("Doba", item -> item.getDurationMs() == null ? "" : ResultTexts.duration(item.getDurationMs()),
            nullsLast(GroupItem::getDurationMs, Comparator.<Long>naturalOrder()));

    private final String title;
    private final Function<GroupItem, String> text;
    private final Comparator<GroupItem> comparator;

    GroupColumns(String title, Function<GroupItem, String> text, Comparator<GroupItem> comparator) {
        this.title = title;
        this.text = text;
        this.comparator = comparator.thenComparingInt(GroupItem::getOrder);
    }

    public String getTitle() {
        return title;
    }

    /** Text buňky (prázdný, když hodnota není známá). */
    public String text(GroupItem item) {
        return text.apply(item);
    }

    /** Vzestupné řazení podle sloupce; při shodě podle pořadí ve skupině. */
    public Comparator<GroupItem> comparator() {
        return comparator;
    }

    /** Filtr tabulky: všechny balíky, nebo jen nevalidní a ty, které nešlo validovat. */
    public static Predicate<GroupItem> filter(boolean onlyInvalid) {
        return onlyInvalid ? item -> item.getStatus().isProblem() : item -> true;
    }

    /** Řádek nad tabulkou při zapnutém filtru: „Zobrazeno: 2 z 12“. */
    public static String shownText(int shown, int total) {
        return "Zobrazeno: " + shown + " z " + total;
    }

    private static String count(GroupItem item, Function<ProblemCounts, Integer> part) {
        Integer value = countOrNull(item, part);
        return value == null ? "" : String.valueOf(value);
    }

    private static Integer countOrNull(GroupItem item, Function<ProblemCounts, Integer> part) {
        return item.getCounts() == null ? null : part.apply(item.getCounts());
    }

    private static <T> Comparator<GroupItem> nullsLast(Function<GroupItem, T> key, Comparator<T> order) {
        return Comparator.comparing(key, Comparator.nullsLast(order));
    }
}
