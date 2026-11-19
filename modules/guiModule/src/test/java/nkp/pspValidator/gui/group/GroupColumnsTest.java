package nkp.pspValidator.gui.group;

import nkp.pspValidator.gui.results.ProblemCounts;
import nkp.pspValidator.shared.Dmf;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

/**
 * Řazení tabulky balíků podle sloupců, texty buněk a filtr „jen nevalidní“.
 */
public class GroupColumnsTest {

    private GroupRun run;
    private GroupItem alfa, beta, gama, delta, eps;

    /**
     * alfa: nevalidní, Periodika 2.1, 5/1/0, 2 s; beta: validní, Monografie 2.2, 0/3/9, 1 s; gama: chyba bez typu, 0,1 s;
     * delta: validní, Monografie 2.2, 0/0/9, 1 s (shoda s betou v době); eps: čeká.
     */
    @Before
    public void setUp() {
        List<GroupPackages.Entry> entries = new ArrayList<>();
        for (String name : new String[]{"alfa", "Beta", "gama", "delta", "eps"}) {
            entries.add(new GroupPackages.Entry(new File("/d", name), false));
        }
        run = new GroupRun(new GroupPackages.Listing(new File("/d"), entries, Collections.<String>emptyList()));
        alfa = run.getItems().get(0);
        beta = run.getItems().get(1);
        gama = run.getItems().get(2);
        delta = run.getItems().get(3);
        eps = run.getItems().get(4);
        run.runStarted(run.getItems(), 0);
        run.packageStarted(alfa);
        run.packageFinished(alfa, new Dmf(Dmf.Type.PERIODICAL, "2.1"), new ProblemCounts(5, 1, 0), false, 2000, null, null);
        run.packageStarted(beta);
        run.packageFinished(beta, new Dmf(Dmf.Type.MONOGRAPH, "2.2"), new ProblemCounts(0, 3, 9), true, 1000, null, null);
        run.packageStarted(gama);
        run.packageFailed(gama, null, null, "není balík", 100);
        run.packageStarted(delta);
        run.packageFinished(delta, new Dmf(Dmf.Type.MONOGRAPH, "2.2"), new ProblemCounts(0, 0, 9), true, 1000, null, null);
    }

    private List<String> sorted(GroupColumns column) {
        List<GroupItem> items = new ArrayList<>(run.getItems());
        items.sort(column.comparator());
        return items.stream().map(GroupItem::getName).collect(Collectors.toList());
    }

    @Test
    public void sortByName() {
        assertEquals(Arrays.asList("alfa", "Beta", "delta", "eps", "gama"), sorted(GroupColumns.NAME));
    }

    @Test
    public void sortByTypeEmptyLast() {
        //beta a delta stejny typ: podle poradi ve skupine
        assertEquals(Arrays.asList("Beta", "delta", "alfa", "gama", "eps"), sorted(GroupColumns.TYPE));
    }

    @Test
    public void sortByResultProblemsFirst() {
        assertEquals(Arrays.asList("alfa", "gama", "Beta", "delta", "eps"), sorted(GroupColumns.RESULT));
    }

    @Test
    public void sortByCountsEmptyLast() {
        assertEquals(Arrays.asList("Beta", "delta", "alfa", "gama", "eps"), sorted(GroupColumns.ERRORS));
        assertEquals(Arrays.asList("delta", "alfa", "Beta", "gama", "eps"), sorted(GroupColumns.WARNINGS));
        assertEquals(Arrays.asList("alfa", "Beta", "delta", "gama", "eps"), sorted(GroupColumns.INFOS));
    }

    @Test
    public void sortByDuration() {
        assertEquals(Arrays.asList("gama", "Beta", "delta", "alfa", "eps"), sorted(GroupColumns.DURATION));
    }

    @Test
    public void allStatusesOrder() {
        List<PackageStatus> statuses = new ArrayList<>(Arrays.asList(PackageStatus.values()));
        Collections.reverse(statuses);
        statuses.sort((x, y) -> Integer.compare(x.sortRank(), y.sortRank()));
        assertEquals(Arrays.asList(PackageStatus.INVALID, PackageStatus.ERROR, PackageStatus.CANCELED, PackageStatus.VALID,
                PackageStatus.RUNNING, PackageStatus.WAITING, PackageStatus.NOT_VALIDATED), statuses);
    }

    @Test
    public void cellTexts() {
        assertEquals("alfa", GroupColumns.NAME.text(alfa));
        assertEquals("Periodika 2.1", GroupColumns.TYPE.text(alfa));
        assertEquals("nevalidní", GroupColumns.RESULT.text(alfa));
        assertEquals("5", GroupColumns.ERRORS.text(alfa));
        assertEquals("1", GroupColumns.WARNINGS.text(alfa));
        assertEquals("0", GroupColumns.INFOS.text(alfa));
        assertEquals("2,0 s", GroupColumns.DURATION.text(alfa));
        assertEquals("", GroupColumns.TYPE.text(gama));
        assertEquals("chyba", GroupColumns.RESULT.text(gama));
        assertEquals("", GroupColumns.ERRORS.text(gama));
        assertEquals("0,1 s", GroupColumns.DURATION.text(gama));
        assertEquals("čeká", GroupColumns.RESULT.text(eps));
        assertEquals("", GroupColumns.DURATION.text(eps));
        assertEquals("Výsledek", GroupColumns.RESULT.getTitle());
    }

    @Test
    public void filterOnlyInvalid() {
        List<String> all = run.getItems().stream().filter(GroupColumns.filter(false)).map(GroupItem::getName).collect(Collectors.toList());
        assertEquals(Arrays.asList("alfa", "Beta", "gama", "delta", "eps"), all);
        List<String> invalid = run.getItems().stream().filter(GroupColumns.filter(true)).map(GroupItem::getName).collect(Collectors.toList());
        assertEquals(Arrays.asList("alfa", "gama"), invalid);
    }

    @Test
    public void problemAndDoneFlags() {
        for (PackageStatus status : PackageStatus.values()) {
            boolean expectedProblem = status == PackageStatus.INVALID || status == PackageStatus.ERROR;
            boolean expectedDone = status != PackageStatus.WAITING && status != PackageStatus.RUNNING;
            assertEquals(status.name(), expectedProblem, status.isProblem());
            assertEquals(status.name(), expectedDone, status.isDone());
        }
    }

    @Test
    public void shownText() {
        assertEquals("Zobrazeno: 2 z 12", GroupColumns.shownText(2, 12));
        assertEquals("Zobrazeno: 0 z 0", GroupColumns.shownText(0, 0));
    }
}
