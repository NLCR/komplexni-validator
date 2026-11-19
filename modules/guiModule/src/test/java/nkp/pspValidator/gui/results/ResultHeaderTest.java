package nkp.pspValidator.gui.results;

import nkp.pspValidator.shared.Dmf;
import nkp.pspValidator.shared.engine.Level;
import org.junit.Test;

import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Horní lišta: verdikt a počty se přebírají z jádra, texty podle fáze validace.
 */
public class ResultHeaderTest {

    private static final Dmf FUND_UNIT = new Dmf(Dmf.Type.FUND_UNIT, "0.2");
    private static final LocalDateTime AT = LocalDateTime.of(2026, 11, 19, 14, 2);

    private static Map<Level, Integer> counts(int errors, int warnings, int infos) {
        Map<Level, Integer> map = new EnumMap<>(Level.class);
        map.put(Level.ERROR, errors);
        map.put(Level.WARNING, warnings);
        map.put(Level.INFO, infos);
        return map;
    }

    @Test
    public void finishedInvalid() {
        ResultHeader header = ResultHeader.finished("nk-00027x", FUND_UNIT, counts(30, 0, 132), false, 3400, AT);
        assertEquals(ResultHeader.Phase.FINISHED, header.getPhase());
        assertEquals("nk-00027x", header.getTitle());
        assertEquals("Jednotky fondu 0.2, validováno 19. 11. 2026 14:02, trvalo 3,4 s", header.getSubtitle());
        assertEquals("Nevalidní", header.getVerdictText());
        assertEquals("verdict-invalid", header.getVerdictStyleClass());
        assertTrue(header.isCountsVisible());
        assertFalse(header.isRunning());
        assertEquals(30, header.getErrors());
        assertEquals(0, header.getWarnings());
        assertEquals(132, header.getInfos());
        assertEquals("30 chyb", header.getErrorsText());
        assertEquals("0 varování", header.getWarningsText());
        assertEquals("132 informací", header.getInfosText());
    }

    @Test
    public void verdictComesFromCoreNotFromCounts() {
        //jadro rozhoduje o validite (napr. varovani validitu nemeni); lista ji jen prebira
        ResultHeader valid = ResultHeader.finished("b", FUND_UNIT, counts(0, 1, 137), true, 500, AT);
        assertEquals("Validní", valid.getVerdictText());
        assertEquals("verdict-valid", valid.getVerdictStyleClass());
        assertEquals("1 varování", valid.getWarningsText());
        ResultHeader invalidWithoutErrors = ResultHeader.finished("b", FUND_UNIT, counts(0, 0, 0), false, 500, AT);
        assertEquals("Nevalidní", invalidWithoutErrors.getVerdictText());
    }

    @Test
    public void missingLevelsCountAsZero() {
        Map<Level, Integer> onlyErrors = new EnumMap<>(Level.class);
        onlyErrors.put(Level.ERROR, 2);
        ResultHeader header = ResultHeader.finished("b", FUND_UNIT, onlyErrors, false, 0, AT);
        assertEquals(2, header.getErrors());
        assertEquals(0, header.getWarnings());
        assertEquals(0, header.getInfos());
        ResultHeader none = ResultHeader.finished("b", FUND_UNIT, null, true, 0, AT);
        assertEquals("0 chyb", none.getErrorsText());
        assertEquals("0 informací", none.getInfosText());
    }

    @Test
    public void running() {
        ResultHeader beforeDetection = ResultHeader.running("nk-00027x", null);
        assertEquals(ResultHeader.Phase.RUNNING, beforeDetection.getPhase());
        assertEquals("nk-00027x", beforeDetection.getTitle());
        assertEquals("Rozpoznávám typ balíku, validace běží", beforeDetection.getSubtitle());
        assertNull(beforeDetection.getVerdictText());
        assertNull(beforeDetection.getVerdictStyleClass());
        assertFalse(beforeDetection.isCountsVisible());
        assertTrue(beforeDetection.isRunning());
        assertEquals("Jednotky fondu 0.2, validace běží", ResultHeader.running("nk-00027x", FUND_UNIT).getSubtitle());
    }

    @Test
    public void failedAndStopped() {
        ResultHeader failed = ResultHeader.failed("b", null);
        assertEquals(ResultHeader.Phase.FAILED, failed.getPhase());
        assertEquals("Validace skončila chybou", failed.getSubtitle());
        assertEquals("Chyba", failed.getVerdictText());
        assertEquals("verdict-invalid", failed.getVerdictStyleClass());
        assertFalse(failed.isCountsVisible());
        assertFalse(failed.isRunning());
        assertEquals("Jednotky fondu 0.2, validace skončila chybou", ResultHeader.failed("b", FUND_UNIT).getSubtitle());

        ResultHeader stopped = ResultHeader.stopped("b", FUND_UNIT);
        assertEquals(ResultHeader.Phase.STOPPED, stopped.getPhase());
        assertEquals("Jednotky fondu 0.2, validace zrušena", stopped.getSubtitle());
        assertEquals("Zrušeno", stopped.getVerdictText());
        assertEquals("verdict-none", stopped.getVerdictStyleClass());
        assertFalse(stopped.isCountsVisible());
        assertFalse(stopped.isRunning());
        assertEquals("Validace zrušena", ResultHeader.stopped("b", null).getSubtitle());
    }
}
