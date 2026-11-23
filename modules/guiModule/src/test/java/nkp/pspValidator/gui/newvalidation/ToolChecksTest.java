package nkp.pspValidator.gui.newvalidation;

import nkp.pspValidator.shared.externalUtils.ExternalUtil;
import nkp.pspValidator.shared.externalUtils.ExternalUtilManager;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

import static nkp.pspValidator.shared.externalUtils.ExternalUtil.*;
import static org.junit.Assert.*;

/**
 * Kontrola nástrojů sdílená dialogy Nová validace a Externí nástroje: výsledek se zapíše do manažeru a ohlásí jen
 * ve vlákně UI a jen pro poslední spuštěnou kontrolu nástroje. Pracovní vlákna i vlákno UI tu nahrazují fronty
 * spouštěné ručně, aby šlo určit pořadí dokončení.
 */
public class ToolChecksTest {

    private final List<Runnable> workerQueue = new ArrayList<>();
    private final List<Runnable> uiQueue = new ArrayList<>();
    private final Executor workers = workerQueue::add;
    private final Executor ui = uiQueue::add;
    private final List<String> reported = new ArrayList<>();
    /** výsledek, který kontrola vrátí pro nástroj (čte se až při běhu v pracovním vlákně) */
    private final Map<ExternalUtil, ToolStatus> nextResult = new EnumMap<>(ExternalUtil.class);
    private ExternalUtilManager manager;
    private ToolChecks checks;

    @Before
    public void setUp() {
        manager = new ExternalUtilManager(Collections.emptyMap(), Collections.emptyMap());
        checks = new ToolChecks(manager, util -> {
            ToolStatus status = nextResult.get(util);
            if (status == null) {
                throw new IllegalStateException("bez výsledku pro " + util);
            }
            return status;
        }, workers, ui, (util, status) -> reported.add(util + "=" + status.getState()));
    }

    private static void runAll(List<Runnable> queue) {
        List<Runnable> copy = new ArrayList<>(queue);
        queue.clear();
        for (Runnable runnable : copy) {
            runnable.run();
        }
    }

    @Test
    public void checkAllStartsEveryToolAsChecking() {
        assertEquals(ToolStatus.State.CHECKING, checks.getStatus(KAKADU).getState());
        assertTrue(checks.getStatuses().isEmpty());
        checks.checkAll();
        assertEquals(ExternalUtil.values().length, workerQueue.size());
        assertEquals(ExternalUtil.values().length, checks.getStatuses().size());
        for (ExternalUtil util : ExternalUtil.values()) {
            assertEquals(ToolStatus.State.CHECKING, checks.getStatus(util).getState());
        }
        assertTrue(reported.isEmpty());
    }

    @Test
    public void resultIsWrittenOnlyInUiThread() {
        nextResult.put(JHOVE, ToolStatus.notFound("Cannot run program \"jhove\""));
        checks.check(JHOVE);
        runAll(workerQueue);
        //pracovni vlakno dobehlo, ale nic nezapsalo
        assertTrue(manager.isUtilAvailable(JHOVE));
        assertEquals(ToolStatus.State.CHECKING, checks.getStatus(JHOVE).getState());
        assertTrue(reported.isEmpty());
        assertEquals(1, uiQueue.size());

        runAll(uiQueue);
        assertFalse(manager.isUtilAvailable(JHOVE));
        assertEquals(ToolStatus.State.NOT_FOUND, checks.getStatus(JHOVE).getState());
        assertEquals("Cannot run program \"jhove\"", checks.getStatus(JHOVE).getDetail());
        assertEquals(Collections.singletonList("JHOVE=NOT_FOUND"), reported);
        //ostatni nastroje beze zmeny
        assertTrue(manager.isUtilAvailable(KAKADU));
        assertFalse(checks.getStatuses().containsKey(KAKADU));
    }

    @Test
    public void staleResultIsDropped() {
        //prvni kontrola (napr. pred zmenou cesty) skonci az po druhe a nesmi ji prepsat
        nextResult.put(KAKADU, ToolStatus.found("8.4"));
        checks.check(KAKADU);
        Runnable first = workerQueue.remove(0);
        checks.check(KAKADU);
        nextResult.put(KAKADU, ToolStatus.error("prázdný výstup (návratový kód 134)"));
        runAll(workerQueue);
        runAll(uiQueue);
        assertFalse(manager.isUtilAvailable(KAKADU));
        assertEquals(ToolStatus.State.ERROR, checks.getStatus(KAKADU).getState());

        nextResult.put(KAKADU, ToolStatus.found("8.4"));
        first.run();
        runAll(uiQueue);
        assertFalse("zastaralý výsledek nesmí změnit dostupnost", manager.isUtilAvailable(KAKADU));
        assertEquals(ToolStatus.State.ERROR, checks.getStatus(KAKADU).getState());
        assertEquals(Collections.singletonList("KAKADU=ERROR"), reported);
    }

    @Test
    public void staleResultDroppedWhileNewCheckStillRunning() {
        nextResult.put(VERAPDF, ToolStatus.found("1.30.0"));
        checks.check(VERAPDF);
        runAll(workerQueue);
        checks.check(VERAPDF);
        //vysledek prvni kontroly dorazi do vlakna UI az po spusteni druhe
        runAll(uiQueue);
        assertTrue(reported.isEmpty());
        assertEquals(ToolStatus.State.CHECKING, checks.getStatus(VERAPDF).getState());
        assertTrue(manager.isUtilAvailable(VERAPDF));

        manager.setUtilAvailable(VERAPDF, false);
        runAll(workerQueue);
        runAll(uiQueue);
        assertTrue(manager.isUtilAvailable(VERAPDF));
        assertEquals("1.30.0", checks.getStatus(VERAPDF).getVersion());
        assertEquals(Collections.singletonList("VERAPDF=FOUND"), reported);
    }

    @Test
    public void versionIsWrittenWithAvailabilityAndClearedWhenUnavailable() {
        nextResult.put(JHOVE, ToolStatus.found("Jhove (Rel. 1.36.0, 2026-01-15)"));
        checks.check(JHOVE);
        runAll(workerQueue);
        //pracovni vlakno verzi nezapisuje
        assertNull(manager.getUtilVersion(JHOVE));
        runAll(uiQueue);
        assertEquals("Jhove (Rel. 1.36.0, 2026-01-15)", manager.getUtilVersion(JHOVE));

        nextResult.put(JHOVE, ToolStatus.notFound("Cannot run program \"jhove\""));
        checks.check(JHOVE);
        runAll(workerQueue);
        runAll(uiQueue);
        assertNull(manager.getUtilVersion(JHOVE));
        assertFalse(manager.isUtilAvailable(JHOVE));
    }

    @Test
    public void staleResultDoesNotWriteVersion() {
        nextResult.put(JHOVE, ToolStatus.found("Rel. 1.34.0"));
        checks.check(JHOVE);
        runAll(workerQueue);
        checks.check(JHOVE);
        runAll(uiQueue);
        assertNull(manager.getUtilVersion(JHOVE));
        nextResult.put(JHOVE, ToolStatus.found("Rel. 1.36.0"));
        runAll(workerQueue);
        runAll(uiQueue);
        assertEquals("Rel. 1.36.0", manager.getUtilVersion(JHOVE));
    }

    @Test
    public void foundToolBecomesAvailableAgain() {
        manager.setUtilAvailable(EPUBCHECK, false);
        nextResult.put(EPUBCHECK, ToolStatus.found("5.1.0"));
        checks.check(EPUBCHECK);
        runAll(workerQueue);
        runAll(uiQueue);
        assertTrue(manager.isUtilAvailable(EPUBCHECK));
        assertEquals(ToolStatus.State.FOUND, checks.getStatus(EPUBCHECK).getState());
    }

    @Test
    public void failingCheckEndsAsError() {
        //kontrola, ktera vyhodi (i Error), nesmi nechat nastroj navzdy ve stavu "kontroluji"
        checks = new ToolChecks(manager, util -> {
            throw new NoClassDefFoundError("x/Y");
        }, workers, ui, (util, status) -> reported.add(util + "=" + status.getState()));
        checks.check(SHNTOOL);
        runAll(workerQueue);
        runAll(uiQueue);
        assertEquals(ToolStatus.State.ERROR, checks.getStatus(SHNTOOL).getState());
        assertEquals("x/Y", checks.getStatus(SHNTOOL).getDetail());
        assertFalse(manager.isUtilAvailable(SHNTOOL));
        assertEquals(Collections.singletonList("SHNTOOL=ERROR"), reported);
    }

    @Test
    public void failingCheckWithoutMessageKeepsExceptionName() {
        checks = new ToolChecks(manager, util -> {
            throw new IllegalStateException();
        }, workers, ui, (util, status) -> reported.add(util + "=" + status.getState()));
        checks.check(MP3VAL);
        runAll(workerQueue);
        runAll(uiQueue);
        assertEquals("java.lang.IllegalStateException", checks.getStatus(MP3VAL).getDetail());
    }

    @Test
    public void realCheckWithoutVersionDetectionIsError() {
        //vychozi kontrola (ExternalTools.check) nad manazerem bez definice detekce verze
        checks = new ToolChecks(manager, util -> ExternalTools.check(manager, util), workers, ui,
                (util, status) -> reported.add(util + "=" + status.getState()));
        checks.check(CHECKMATE);
        runAll(workerQueue);
        runAll(uiQueue);
        assertEquals(ToolStatus.State.ERROR, checks.getStatus(CHECKMATE).getState());
        assertEquals("detekce verze není definována", checks.getStatus(CHECKMATE).getDetail());
        assertFalse(manager.isUtilAvailable(CHECKMATE));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void statusesAreReadOnly() {
        checks.getStatuses().put(JHOVE, ToolStatus.found("1"));
    }

    /**
     * Manažer je společný pro oba dialogy: výsledek starší kontroly z jedné instance nesmí přepsat dostupnost
     * zjištěnou novější kontrolou z druhé instance, i když pro svou instanci platí a posluchači se ohlásí.
     */
    @Test
    public void olderCheckFromAnotherInstanceDoesNotOverwriteAvailability() {
        List<String> reportedByOther = new ArrayList<>();
        Map<ExternalUtil, ToolStatus> otherResult = new EnumMap<>(ExternalUtil.class);
        ToolChecks other = new ToolChecks(manager, otherResult::get, workers, ui,
                (util, status) -> reportedByOther.add(util + "=" + status.getState()));

        //starsi kontrola v prvni instanci (napr. zavrena Nova validace), nastroj jeste nenalezen
        nextResult.put(KAKADU, ToolStatus.notFound("Cannot run program"));
        checks.check(KAKADU);
        List<Runnable> olderWorker = new ArrayList<>(workerQueue);
        workerQueue.clear();

        //novejsi kontrola v druhe instanci po nastaveni cesty, nastroj nalezen
        otherResult.put(KAKADU, ToolStatus.found("Kakadu 8.5"));
        other.check(KAKADU);
        runAll(workerQueue);
        runAll(uiQueue);
        assertTrue(manager.isUtilAvailable(KAKADU));
        assertEquals(Collections.singletonList("KAKADU=FOUND"), reportedByOther);

        //starsi kontrola dobehne az potom
        for (Runnable runnable : olderWorker) {
            runnable.run();
        }
        runAll(uiQueue);
        assertTrue("starší výsledek z jiné instance nesmí přepsat dostupnost", manager.isUtilAvailable(KAKADU));
        assertEquals(Collections.singletonList("KAKADU=NOT_FOUND"), reported);
    }

    // ----- zjistena dostupnost pro prazdny stav hlavniho okna -----

    @Test
    public void toolIsCheckedOnlyAfterResultIsWritten() {
        //manazer jadra ma do prvni kontroly vsechny nastroje "dostupne", to neni zjistena dostupnost
        assertTrue(manager.isUtilAvailable(JHOVE));
        assertFalse(ToolChecks.isChecked(manager, JHOVE));
        nextResult.put(JHOVE, ToolStatus.notFound("Cannot run program \"jhove\""));
        checks.check(JHOVE);
        runAll(workerQueue);
        assertFalse("výsledek ještě není ve vlákně UI", ToolChecks.isChecked(manager, JHOVE));
        runAll(uiQueue);
        assertTrue(ToolChecks.isChecked(manager, JHOVE));
        assertFalse(manager.isUtilAvailable(JHOVE));
        //ostatni nastroje se nekontrolovaly
        assertFalse(ToolChecks.isChecked(manager, KAKADU));
        //jiny manazer (jina inicializace validacnich dat) nic nezdedi
        ExternalUtilManager otherManager = new ExternalUtilManager(Collections.emptyMap(), Collections.emptyMap());
        assertFalse(ToolChecks.isChecked(otherManager, JHOVE));
    }

    @Test
    public void staleResultDoesNotMarkToolChecked() {
        List<Runnable> listenerCalls = new ArrayList<>();
        ToolChecks.addAvailabilityListener(manager, () -> listenerCalls.add(null));
        Map<ExternalUtil, ToolStatus> otherResult = new EnumMap<>(ExternalUtil.class);
        ToolChecks other = new ToolChecks(manager, otherResult::get, workers, ui, (util, status) -> {
        });
        nextResult.put(KAKADU, ToolStatus.notFound("Cannot run program"));
        checks.check(KAKADU);
        List<Runnable> olderWorker = new ArrayList<>(workerQueue);
        workerQueue.clear();
        otherResult.put(KAKADU, ToolStatus.found("v8.0.5"));
        other.check(KAKADU);
        //starsi kontrola dobehne driv nez novejsi: jeji vysledek se do manazeru nezapise
        for (Runnable runnable : olderWorker) {
            runnable.run();
        }
        runAll(uiQueue);
        assertFalse(ToolChecks.isChecked(manager, KAKADU));
        assertTrue(listenerCalls.isEmpty());
        runAll(workerQueue);
        runAll(uiQueue);
        assertTrue(ToolChecks.isChecked(manager, KAKADU));
        assertTrue(manager.isUtilAvailable(KAKADU));
        assertEquals(1, listenerCalls.size());
    }

    @Test
    public void availabilityListenerIsCalledInUiThreadForEachWrittenResult() {
        List<String> calls = new ArrayList<>();
        ToolChecks.addAvailabilityListener(manager, () -> calls.add("zapsáno"));
        for (ExternalUtil util : ExternalUtil.values()) {
            nextResult.put(util, ToolStatus.found("1.0"));
        }
        checks.checkAll();
        runAll(workerQueue);
        assertTrue("posluchač se volá až ve vlákně UI", calls.isEmpty());
        runAll(uiQueue);
        assertEquals(ExternalUtil.values().length, calls.size());
        for (ExternalUtil util : ExternalUtil.values()) {
            assertTrue(ToolChecks.isChecked(manager, util));
        }
        //posluchac jineho manazeru se nevola
        ExternalUtilManager otherManager = new ExternalUtilManager(Collections.emptyMap(), Collections.emptyMap());
        List<String> otherCalls = new ArrayList<>();
        ToolChecks.addAvailabilityListener(otherManager, () -> otherCalls.add("zapsáno"));
        checks.check(JHOVE);
        runAll(workerQueue);
        runAll(uiQueue);
        assertEquals(ExternalUtil.values().length + 1, calls.size());
        assertTrue(otherCalls.isEmpty());
    }
}
