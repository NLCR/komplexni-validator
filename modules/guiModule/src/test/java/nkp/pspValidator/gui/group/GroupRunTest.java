package nkp.pspValidator.gui.group;

import nkp.pspValidator.gui.results.ProblemCounts;
import nkp.pspValidator.shared.Dmf;
import org.junit.Test;

import java.io.File;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Stav běhu skupiny: stavy balíků, chyba uprostřed, zrušení, opakování jednoho balíku, podíl průběhu a texty horní
 * lišty a panelu průběhu. Očekávané hodnoty jsou spočítané ručně.
 */
public class GroupRunTest {

    private static final Dmf MON = new Dmf(Dmf.Type.MONOGRAPH, "2.2");
    private static final LocalDateTime END = LocalDateTime.of(2026, 11, 19, 14, 2);

    private static GroupRun run(String... names) {
        List<GroupPackages.Entry> entries = new ArrayList<>();
        for (String name : names) {
            entries.add(new GroupPackages.Entry(new File("/data/davka", name), name.endsWith(".zip")));
        }
        return new GroupRun(new GroupPackages.Listing(new File("/data/davka"), entries, Collections.<String>emptyList()));
    }

    private static List<PackageStatus> statuses(GroupRun run) {
        List<PackageStatus> result = new ArrayList<>();
        for (GroupItem item : run.getItems()) {
            result.add(item.getStatus());
        }
        return result;
    }

    @Test
    public void emptyGroup() {
        GroupRun run = run();
        assertEquals("davka", run.getTitle());
        assertEquals(0, run.total());
        run.runStarted(Collections.<GroupItem>emptyList(), 1000);
        assertTrue(run.isRunning());
        assertEquals(1.0, run.fraction(0.5), 0.0);
        assertEquals("Validuji skupinu…", run.progressTitle());
        assertEquals("hotovo 0 z 0", run.progressCounts());
        run.runEnded(1000, END);
        assertFalse(run.isRunning());
        assertEquals("Skupina 0 balíků, validováno 19. 11. 2026 14:02, trvalo 0,0 s", run.subtitle(5000));
        assertEquals("0 validních", run.validText());
        assertEquals("0 nevalidních", run.invalidText());
        assertEquals("0 s chybou", run.errorText());
        assertEquals("0 čeká", run.waitingText());
        assertEquals("0 nevalidováno", run.notValidatedText());
    }

    @Test
    public void beforeFirstRun() {
        GroupRun run = run("a");
        assertEquals("Skupina 1 balík, zatím nevalidováno", run.subtitle(0));
        assertEquals(Collections.singletonList(PackageStatus.WAITING), statuses(run));
    }

    @Test
    public void onePackageValid() {
        GroupRun run = run("a");
        GroupItem a = run.getItems().get(0);
        run.runStarted(run.getItems(), 0);
        assertEquals("1 čeká", run.waitingText());
        run.packageStarted(a);
        assertSame(a, run.getCurrent());
        assertEquals(1, run.currentPosition());
        assertEquals("Balík 1 z 1: a", run.progressTitle());
        assertEquals(PackageStatus.RUNNING, a.getStatus());
        //bezici balík se pocita mezi cekajici
        assertEquals("1 čeká", run.waitingText());
        run.packageResolved(a, MON);
        assertEquals("Monografie 2.2", a.getTypeAndVersion());
        File xml = new File("/logy/a.xml");
        run.packageFinished(a, MON, new ProblemCounts(0, 2, 7), true, 3400, xml, null);
        assertNull(run.getCurrent());
        assertEquals(0, run.currentPosition());
        assertEquals(PackageStatus.VALID, a.getStatus());
        assertEquals(new ProblemCounts(0, 2, 7), a.getCounts());
        assertEquals(Long.valueOf(3400), a.getDurationMs());
        assertEquals(xml, a.getXmlProtocol());
        assertNull(a.getTxtLog());
        assertNull(a.getMessage());
        run.runEnded(4000, END);
        assertEquals("1 validní", run.validText());
        assertEquals(1, run.done());
        assertEquals("Skupina 1 balík, validováno 19. 11. 2026 14:02, trvalo 4,0 s", run.subtitle(10_000));
        assertEquals("Validace skupiny " + new File("/data/davka").getAbsolutePath() + " hotova.", run.statusText());
    }

    @Test
    public void errorInTheMiddleDoesNotStopGroup() {
        GroupRun run = run("a", "b", "c");
        GroupItem a = run.getItems().get(0);
        GroupItem b = run.getItems().get(1);
        GroupItem c = run.getItems().get(2);
        run.runStarted(run.getItems(), 0);
        run.packageStarted(a);
        run.packageFinished(a, MON, new ProblemCounts(3, 0, 0), false, 100, null, null);
        run.packageStarted(b);
        run.packageFailed(b, null, null, "nenalezen soubor PRIMARY-METS", 5);
        run.packageStarted(c);
        run.packageFinished(c, MON, new ProblemCounts(0, 0, 0), true, 200, null, null);
        run.runEnded(1000, END);
        assertEquals(Arrays.asList(PackageStatus.INVALID, PackageStatus.ERROR, PackageStatus.VALID), statuses(run));
        assertEquals("nenalezen soubor PRIMARY-METS", b.getMessage());
        assertNull(b.getCounts());
        assertNull(b.getDmf());
        assertEquals("", b.getTypeAndVersion());
        assertEquals(Long.valueOf(5), b.getDurationMs());
        assertEquals(3, run.done());
        assertEquals("1 validní", run.validText());
        assertEquals("1 nevalidní", run.invalidText());
        assertEquals("1 s chybou", run.errorText());
        assertFalse(run.isCanceled());
    }

    @Test
    public void failureWithoutMessageAndWithPartialCounts() {
        GroupRun run = run("a");
        GroupItem a = run.getItems().get(0);
        run.runStarted(run.getItems(), 0);
        run.packageStarted(a);
        run.packageFailed(a, MON, new ProblemCounts(1, 0, 0), null, 50);
        assertEquals(PackageStatus.ERROR, a.getStatus());
        assertEquals("Validace skončila chybou.", a.getMessage());
        assertEquals(new ProblemCounts(1, 0, 0), a.getCounts());
        assertEquals(MON, a.getDmf());
    }

    @Test
    public void cancelMarksRunningCanceledAndRestNotValidated() {
        GroupRun run = run("a", "b", "c", "d");
        GroupItem a = run.getItems().get(0);
        GroupItem b = run.getItems().get(1);
        run.runStarted(run.getItems(), 0);
        run.packageStarted(a);
        run.packageFinished(a, MON, ProblemCounts.ZERO, true, 10, null, null);
        run.packageStarted(b);
        run.cancelRequested();
        run.packageCanceled(b, MON, new ProblemCounts(2, 0, 0), 41_000);
        run.runEnded(41_000, END);
        assertTrue(run.isCanceled());
        assertEquals(Arrays.asList(PackageStatus.VALID, PackageStatus.CANCELED, PackageStatus.NOT_VALIDATED, PackageStatus.NOT_VALIDATED),
                statuses(run));
        assertEquals("3 nevalidováno", run.notValidatedText());
        assertEquals("0 čeká", run.waitingText());
        assertEquals(1, run.done());
        assertEquals("Skupina 4 balíky, validace zrušena, trvalo 41,0 s", run.subtitle(50_000));
        assertEquals("Validace skupiny " + new File("/data/davka").getAbsolutePath() + " zrušena.", run.statusText());
    }

    @Test
    public void cancelBeforePackageReportedItsEnd() {
        GroupRun run = run("a", "b");
        run.runStarted(run.getItems(), 0);
        run.packageStarted(run.getItems().get(0));
        run.cancelRequested();
        run.runEnded(10, END);
        assertEquals(Arrays.asList(PackageStatus.CANCELED, PackageStatus.NOT_VALIDATED), statuses(run));
    }

    @Test
    public void cancelRequestOutsideRunIsIgnored() {
        GroupRun run = run("a");
        run.cancelRequested();
        assertFalse(run.isCanceled());
    }

    @Test
    public void fractionAndProgressTexts() {
        GroupRun run = run("a", "b", "c", "d");
        GroupItem a = run.getItems().get(0);
        GroupItem b = run.getItems().get(1);
        run.runStarted(run.getItems(), 0);
        assertEquals(0.0, run.fraction(0.9), 1e-9);
        run.packageStarted(a);
        run.packageFinished(a, MON, ProblemCounts.ZERO, true, 10, null, null);
        run.packageStarted(b);
        assertEquals(0.375, run.fraction(0.5), 1e-9);
        assertEquals(0.25, run.fraction(-1), 1e-9);
        assertEquals(0.5, run.fraction(7), 1e-9);
        assertEquals("Balík 2 z 4: b", run.progressTitle());
        assertEquals("hotovo 1 z 4", run.progressCounts());
        assertEquals("Skupina 4 balíky, hotovo 1, běží 1:23", run.subtitle(83_000));
        assertEquals("3 čekají", run.waitingText());
    }

    @Test
    public void waitingPlurals() {
        GroupRun run = run("a", "b", "c", "d", "e");
        run.runStarted(run.getItems(), 0);
        assertEquals("5 čeká", run.waitingText());
        GroupRun two = run("a", "b");
        two.runStarted(two.getItems(), 0);
        assertEquals("2 čekají", two.waitingText());
    }

    @Test
    public void rerunOnePackageKeepsOthersAndAddsTime() {
        GroupRun run = run("a", "b", "c");
        GroupItem a = run.getItems().get(0);
        GroupItem b = run.getItems().get(1);
        GroupItem c = run.getItems().get(2);
        run.runStarted(run.getItems(), 0);
        for (GroupItem item : run.getItems()) {
            run.packageStarted(item);
            run.packageFinished(item, MON, new ProblemCounts(1, 0, 0), false, 100, null, null);
        }
        run.runEnded(1000, END);

        run.runStarted(Collections.singletonList(b), 5000);
        assertEquals(Arrays.asList(PackageStatus.INVALID, PackageStatus.WAITING, PackageStatus.INVALID), statuses(run));
        assertNull(b.getCounts());
        assertEquals(new ProblemCounts(1, 0, 0), a.getCounts());
        assertEquals(0.0, run.fraction(0), 1e-9);
        run.packageStarted(b);
        assertEquals("Balík 1 z 1: b", run.progressTitle());
        assertEquals("hotovo 0 z 1", run.progressCounts());
        //celkova doba behem behu: predchozi beh 1 s a 2,5 s noveho
        assertEquals("Skupina 3 balíky, hotovo 2, běží 0:03", run.subtitle(7500));
        run.packageFinished(b, MON, ProblemCounts.ZERO, true, 300, null, null);
        run.runEnded(5500, END);
        assertEquals(Arrays.asList(PackageStatus.INVALID, PackageStatus.VALID, PackageStatus.INVALID), statuses(run));
        assertEquals("Skupina 3 balíky, validováno 19. 11. 2026 14:02, trvalo 1,5 s", run.subtitle(9000));
        assertEquals(new ProblemCounts(1, 0, 0), c.getCounts());
    }

    @Test(expected = IllegalStateException.class)
    public void secondRunWhileRunningIsRejected() {
        GroupRun run = run("a");
        run.runStarted(run.getItems(), 0);
        run.runStarted(run.getItems(), 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void packageOutsideRunIsRejected() {
        GroupRun run = run("a", "b");
        run.runStarted(Collections.singletonList(run.getItems().get(0)), 0);
        run.packageStarted(run.getItems().get(1));
    }

    @Test(expected = IllegalArgumentException.class)
    public void packageOfOtherGroupIsRejected() {
        GroupRun run = run("a");
        GroupRun other = run("a");
        run.runStarted(other.getItems(), 0);
    }
}
