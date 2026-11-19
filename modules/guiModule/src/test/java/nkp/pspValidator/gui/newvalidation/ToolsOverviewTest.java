package nkp.pspValidator.gui.newvalidation;

import nkp.pspValidator.shared.externalUtils.ExternalUtil;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.util.EnumMap;
import java.util.Map;

import static nkp.pspValidator.shared.externalUtils.ExternalUtil.*;
import static org.junit.Assert.*;

/**
 * Texty a rozhodnutí dialogu Externí nástroje: celkový stav, souhrn v patičce, zkrácení textu stavu, tooltip,
 * třídy vzhledu a podrobnosti chyby.
 */
public class ToolsOverviewTest {

    private static final String IMAGE_MAGICK_VERSION = "ImageMagick 7.1.2-24 Q16-HDRI";

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private static Map<ExternalUtil, ToolStatus> allFound() {
        Map<ExternalUtil, ToolStatus> statuses = new EnumMap<>(ExternalUtil.class);
        for (ExternalUtil util : ExternalUtil.values()) {
            statuses.put(util, ToolStatus.found("1.0"));
        }
        return statuses;
    }

    // ----- celkovy stav -----

    @Test
    public void stateWithoutStatusesIsChecking() {
        assertEquals(ToolsOverview.State.CHECKING, ToolsOverview.state(new EnumMap<>(ExternalUtil.class)));
    }

    @Test
    public void stateAllFound() {
        assertEquals(ToolsOverview.State.ALL_AVAILABLE, ToolsOverview.state(allFound()));
    }

    @Test
    public void stateMissingOneStatusIsChecking() {
        Map<ExternalUtil, ToolStatus> statuses = allFound();
        statuses.remove(EPUBCHECK);
        assertEquals(ToolsOverview.State.CHECKING, ToolsOverview.state(statuses));
    }

    @Test
    public void stateCheckingWinsOverEarlierUnavailable() {
        Map<ExternalUtil, ToolStatus> statuses = allFound();
        statuses.put(IMAGE_MAGICK, ToolStatus.notFound("x"));
        statuses.put(EPUBCHECK, ToolStatus.checking());
        assertEquals(ToolsOverview.State.CHECKING, ToolsOverview.state(statuses));
    }

    @Test
    public void stateNotFoundOrErrorIsSomeUnavailable() {
        Map<ExternalUtil, ToolStatus> statuses = allFound();
        statuses.put(MP3VAL, ToolStatus.notFound("x"));
        assertEquals(ToolsOverview.State.SOME_UNAVAILABLE, ToolsOverview.state(statuses));

        statuses = allFound();
        statuses.put(KAKADU, ToolStatus.error("x"));
        assertEquals(ToolsOverview.State.SOME_UNAVAILABLE, ToolsOverview.state(statuses));
    }

    // ----- souhrn -----

    @Test
    public void summaryAfterCheck() {
        Map<ExternalUtil, ToolStatus> statuses = allFound();
        statuses.put(JHOVE, ToolStatus.notFound("x"));
        statuses.put(JPYLYZER, ToolStatus.notFound("x"));
        statuses.put(KAKADU, ToolStatus.error("x"));
        statuses.put(EPUBCHECK, ToolStatus.error("x"));
        assertEquals("5 z 9 nástrojů dostupných", ToolsOverview.summary(statuses));
        assertEquals("9 z 9 nástrojů dostupných", ToolsOverview.summary(allFound()));

        Map<ExternalUtil, ToolStatus> none = new EnumMap<>(ExternalUtil.class);
        for (ExternalUtil util : ExternalUtil.values()) {
            none.put(util, ToolStatus.notFound("x"));
        }
        assertEquals("0 z 9 nástrojů dostupných", ToolsOverview.summary(none));
    }

    @Test
    public void summaryWhileChecking() {
        assertEquals("Kontroluji nástroje, hotovo 0 z 9…", ToolsOverview.summary(new EnumMap<>(ExternalUtil.class)));
        Map<ExternalUtil, ToolStatus> statuses = new EnumMap<>(ExternalUtil.class);
        for (ExternalUtil util : ExternalUtil.values()) {
            statuses.put(util, ToolStatus.checking());
        }
        statuses.put(JHOVE, ToolStatus.found("1.29"));
        statuses.put(KAKADU, ToolStatus.error("x"));
        statuses.put(MP3VAL, ToolStatus.notFound("x"));
        statuses.put(VERAPDF, ToolStatus.found("1.30.0"));
        assertEquals("Kontroluji nástroje, hotovo 4 z 9…", ToolsOverview.summary(statuses));
        //posledni nastroj jeste bezi
        Map<ExternalUtil, ToolStatus> almost = allFound();
        almost.put(CHECKMATE, ToolStatus.checking());
        assertEquals("Kontroluji nástroje, hotovo 8 z 9…", ToolsOverview.summary(almost));
    }

    // ----- text stavu -----

    @Test
    public void statusTexts() {
        assertEquals("kontroluji…", ToolsOverview.statusText(ToolStatus.checking()));
        assertEquals("nalezen, 1.29", ToolsOverview.statusText(ToolStatus.found("1.29")));
        assertEquals("nalezen", ToolsOverview.statusText(ToolStatus.found(null)));
        assertEquals("nalezen", ToolsOverview.statusText(ToolStatus.found("")));
        assertEquals("nenalezen", ToolsOverview.statusText(ToolStatus.notFound("Cannot run program \"jhove\"")));
        assertEquals("chyba při spuštění", ToolsOverview.statusText(ToolStatus.error("prázdný výstup")));
    }

    @Test
    public void longVersionIsTruncated() {
        assertEquals("nalezen, ImageMagick 7.1.2-24…", ToolsOverview.statusText(ToolStatus.found(IMAGE_MAGICK_VERSION)));
        assertEquals(30, ToolsOverview.statusText(ToolStatus.found(IMAGE_MAGICK_VERSION)).length());
    }

    @Test
    public void truncateBoundaries() {
        String thirty = "nalezen, 123456789012345678901";
        assertEquals(30, thirty.length());
        assertEquals(thirty, ToolsOverview.truncate(thirty, 30));
        assertEquals("nalezen, 12345678901234567890…", ToolsOverview.truncate(thirty + "2", 30));
        assertEquals("nalezen, 1234567890123456789…", ToolsOverview.truncate(thirty, 29));
        assertEquals("", ToolsOverview.truncate("", 5));
        assertNull(ToolsOverview.truncate(null, 5));
        //mezera pred vypustkou se vynecha
        assertEquals("abc…", ToolsOverview.truncate("abc def", 5));
        assertEquals("…", ToolsOverview.truncate("ab", 1));
        assertEquals("a", ToolsOverview.truncate("a", 1));
    }

    @Test(expected = IllegalArgumentException.class)
    public void truncateRejectsZeroLength() {
        ToolsOverview.truncate("abc", 0);
    }

    @Test
    public void tooltipHasFullText() {
        assertEquals("nalezen, " + IMAGE_MAGICK_VERSION, ToolsOverview.statusTooltip(ToolStatus.found(IMAGE_MAGICK_VERSION)));
        assertEquals("nenalezen: Cannot run program \"jhove\": error=2, No such file or directory",
                ToolsOverview.statusTooltip(ToolStatus.notFound("Cannot run program \"jhove\": error=2, No such file or directory")));
        assertEquals("chyba při spuštění: prázdný výstup (stderr: Error: Unable to access jarfile epubcheck.jar, návratový kód 1)",
                ToolsOverview.statusTooltip(ToolStatus.error("prázdný výstup (stderr: Error: Unable to access jarfile epubcheck.jar, návratový kód 1)")));
        assertEquals("chyba při spuštění", ToolsOverview.statusTooltip(ToolStatus.error(null)));
        assertEquals("chyba při spuštění", ToolsOverview.statusTooltip(ToolStatus.error("")));
        assertEquals("kontroluji…", ToolsOverview.statusTooltip(ToolStatus.checking()));
    }

    // ----- vzhled -----

    @Test
    public void styleClasses() {
        assertEquals("status-dot-ok", ToolsOverview.dotStyleClass(ToolStatus.found("1")));
        assertEquals("status-dot-error", ToolsOverview.dotStyleClass(ToolStatus.error("x")));
        //volitelny nastroj, ktery chybi, neni chyba
        assertEquals("status-dot-none", ToolsOverview.dotStyleClass(ToolStatus.notFound("x")));
        assertEquals("status-dot-none", ToolsOverview.dotStyleClass(ToolStatus.checking()));

        assertNull(ToolsOverview.textStyleClass(ToolStatus.found("1")));
        assertEquals("text-error", ToolsOverview.textStyleClass(ToolStatus.error("x")));
        assertEquals("text-muted", ToolsOverview.textStyleClass(ToolStatus.notFound("x")));
        assertEquals("text-muted", ToolsOverview.textStyleClass(ToolStatus.checking()));
    }

    // ----- podrobnosti -----

    @Test
    public void detailsOnlyForUnavailable() {
        assertTrue(ToolsOverview.hasDetails(ToolStatus.notFound("x")));
        assertTrue(ToolsOverview.hasDetails(ToolStatus.error(null)));
        assertFalse(ToolsOverview.hasDetails(ToolStatus.found("1")));
        assertFalse(ToolsOverview.hasDetails(ToolStatus.checking()));
    }

    @Test
    public void detailsTextKeepsWholeOutput() throws Exception {
        String emptyOutput = "prázdný výstup (stderr: dyld[23332]: Library not loaded: ../../lib/Mac-arm-64-gcc/libkdu_v85R.so, návratový kód 134)";
        assertEquals("Kakadu: chyba při spuštění", ToolsOverview.detailsTitle(KAKADU, ToolStatus.error(emptyOutput)));
        assertEquals("Adresář nástroje: nenastaven\n\nVýstup kontroly:\n" + emptyOutput,
                ToolsOverview.detailsText(ToolStatus.error(emptyOutput), null));

        File dir = tmp.newFolder("kakadu");
        assertTrue(dir.isAbsolute());
        assertEquals("Adresář nástroje: " + dir.getPath() + "\n\nVýstup kontroly:\nCannot run program \"kdu_expand\"",
                ToolsOverview.detailsText(ToolStatus.notFound("Cannot run program \"kdu_expand\""), dir));
        assertEquals("Adresář nástroje: nenastaven\n\nVýstup kontroly:\n(bez podrobností)",
                ToolsOverview.detailsText(ToolStatus.error(null), null));
        assertEquals("Adresář nástroje: nenastaven\n\nVýstup kontroly:\n(bez podrobností)",
                ToolsOverview.detailsText(ToolStatus.error(""), null));
        assertEquals("JHOVE: nenalezen", ToolsOverview.detailsTitle(JHOVE, ToolStatus.notFound("x")));
    }

    @Test
    public void pathTooltip() throws Exception {
        assertEquals("Adresář nástroje není nastaven.", ToolsOverview.pathTooltip(null));
        File dir = tmp.newFolder("jhove");
        assertEquals("Nastavený adresář: " + dir.getPath(), ToolsOverview.pathTooltip(dir));
    }
}
