package nkp.pspValidator.gui.results;

import nkp.pspValidator.shared.ConfigManifest;
import org.junit.Test;

import java.time.LocalDateTime;

import static org.junit.Assert.assertEquals;

/**
 * Texty horní lišty: tvary slov podle počtu (1, 2 až 4, ostatní), doba validace a datum.
 */
public class ResultTextsTest {

    @Test
    public void errorsPlural() {
        assertEquals("0 chyb", ResultTexts.errors(0));
        assertEquals("1 chyba", ResultTexts.errors(1));
        assertEquals("2 chyby", ResultTexts.errors(2));
        assertEquals("4 chyby", ResultTexts.errors(4));
        assertEquals("5 chyb", ResultTexts.errors(5));
        assertEquals("21 chyb", ResultTexts.errors(21));
        assertEquals("30 chyb", ResultTexts.errors(30));
    }

    @Test
    public void warningsDoNotChange() {
        assertEquals("0 varování", ResultTexts.warnings(0));
        assertEquals("1 varování", ResultTexts.warnings(1));
        assertEquals("3 varování", ResultTexts.warnings(3));
        assertEquals("7 varování", ResultTexts.warnings(7));
    }

    @Test
    public void infosPlural() {
        assertEquals("0 informací", ResultTexts.infos(0));
        assertEquals("1 informace", ResultTexts.infos(1));
        assertEquals("2 informace", ResultTexts.infos(2));
        assertEquals("4 informace", ResultTexts.infos(4));
        assertEquals("5 informací", ResultTexts.infos(5));
        assertEquals("132 informací", ResultTexts.infos(132));
    }

    @Test
    public void durationUnderMinuteInTenthsOfSecond() {
        assertEquals("0,0 s", ResultTexts.duration(0));
        assertEquals("0,0 s", ResultTexts.duration(49));
        assertEquals("0,1 s", ResultTexts.duration(50));
        assertEquals("3,4 s", ResultTexts.duration(3400));
        assertEquals("59,9 s", ResultTexts.duration(59_949));
        assertEquals("0,0 s", ResultTexts.duration(-5));
    }

    @Test
    public void durationMinutesAndHours() {
        //59,95 s se zaokrouhli na celou minutu
        assertEquals("1 min 0 s", ResultTexts.duration(59_950));
        assertEquals("2 min 5 s", ResultTexts.duration(125_000));
        assertEquals("59 min 59 s", ResultTexts.duration(3_599_499));
        assertEquals("1 h 0 min", ResultTexts.duration(3_599_500));
        assertEquals("1 h 2 min", ResultTexts.duration(3_723_000));
        assertEquals("25 h 0 min", ResultTexts.duration(90_000_000));
    }

    @Test
    public void dateTimeWithoutLeadingZeros() {
        assertEquals("19. 11. 2026 14:02", ResultTexts.dateTime(LocalDateTime.of(2026, 11, 19, 14, 2)));
        assertEquals("5. 1. 2027 9:07", ResultTexts.dateTime(LocalDateTime.of(2027, 1, 5, 9, 7, 59)));
    }

    @Test
    public void emptyStateInfo() {
        assertEquals("Nástroje: 5 z 9 dostupných", ResultTexts.toolsAvailable(5, 9));
        assertEquals("Nástroje: 0 z 9 dostupných", ResultTexts.toolsAvailable(0, 9));
        assertEquals("Nástroje: 1 z 1 dostupného", ResultTexts.toolsAvailable(1, 1));
        assertEquals("Konfigurace: 39 fDMF", ResultTexts.fdmfCount(39));
    }

    @Test
    public void fdmfCountWithConfigVersion() {
        ConfigManifest.Status ok = ConfigManifest.Status.OK;
        assertEquals("Konfigurace: 39 fDMF", ResultTexts.fdmfCount(39, ok, "3.1", "3.1"));
        assertEquals("Konfigurace: 39 fDMF", ResultTexts.fdmfCount(39, ok, null, null));
        assertEquals("Konfigurace: 39 fDMF (verze 3.0, aplikace 3.1)",
                ResultTexts.fdmfCount(39, ConfigManifest.Status.OLDER_MINOR, "3.0", "3.1"));
        assertEquals("Konfigurace: 39 fDMF (verze 3.2, aplikace 3.1)",
                ResultTexts.fdmfCount(39, ConfigManifest.Status.NEWER_MINOR, "3.2", "3.1"));
        //chybí-li údaj o verzi, dovětek se nepřidá (nevypisovat „verze null“)
        assertEquals("Konfigurace: 39 fDMF", ResultTexts.fdmfCount(39, ConfigManifest.Status.NEWER_MINOR, null, "3.1"));
        assertEquals("Konfigurace: 39 fDMF", ResultTexts.fdmfCount(39, ConfigManifest.Status.OLDER_MINOR, "3.0", null));
    }

    @Test
    public void elapsedTicksByWholeSeconds() {
        assertEquals("0:00", ResultTexts.elapsed(0));
        assertEquals("0:00", ResultTexts.elapsed(999));
        assertEquals("0:01", ResultTexts.elapsed(1000));
        assertEquals("0:41", ResultTexts.elapsed(41_500));
        assertEquals("0:59", ResultTexts.elapsed(59_999));
        assertEquals("1:00", ResultTexts.elapsed(60_000));
        assertEquals("12:05", ResultTexts.elapsed(725_000));
        assertEquals("59:59", ResultTexts.elapsed(3_599_999));
        assertEquals("1:00:00", ResultTexts.elapsed(3_600_000));
        assertEquals("1:02:03", ResultTexts.elapsed(3_723_000));
        //cas pred spustenim (posun hodin) se zobrazi jako nula
        assertEquals("0:00", ResultTexts.elapsed(-5000));
    }

    @Test
    public void toolsInfoWhileUnknown() {
        assertEquals("Nástroje: zjišťuji…", ResultTexts.toolsInfo(null, 9));
        assertEquals("Nástroje: 0 z 9 dostupných", ResultTexts.toolsInfo(0, 9));
        assertEquals("Nástroje: 9 z 9 dostupných", ResultTexts.toolsInfo(9, 9));
    }

    @Test
    public void zeroCountBadgeIsNeutral() {
        assertEquals("badge-muted", ResultTexts.countBadgeClass(0, "badge-error"));
        assertEquals("badge-error", ResultTexts.countBadgeClass(1, "badge-error"));
        assertEquals("badge-warning", ResultTexts.countBadgeClass(2, "badge-warning"));
        assertEquals("badge-ok", ResultTexts.countBadgeClass(137, "badge-ok"));
        //zaporny pocet nenastava, ale nesmi dostat barvu urovne
        assertEquals("badge-muted", ResultTexts.countBadgeClass(-1, "badge-error"));
    }
}
