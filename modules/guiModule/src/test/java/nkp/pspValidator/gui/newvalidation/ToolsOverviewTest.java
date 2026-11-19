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
        assertEquals("Zjišťuji dostupnost nástrojů, hotovo 0 z 9…", ToolsOverview.summary(new EnumMap<>(ExternalUtil.class)));
        Map<ExternalUtil, ToolStatus> statuses = new EnumMap<>(ExternalUtil.class);
        for (ExternalUtil util : ExternalUtil.values()) {
            statuses.put(util, ToolStatus.checking());
        }
        statuses.put(JHOVE, ToolStatus.found("1.29"));
        statuses.put(KAKADU, ToolStatus.error("x"));
        statuses.put(MP3VAL, ToolStatus.notFound("x"));
        statuses.put(VERAPDF, ToolStatus.found("1.30.0"));
        assertEquals("Zjišťuji dostupnost nástrojů, hotovo 4 z 9…", ToolsOverview.summary(statuses));
        //posledni nastroj jeste bezi
        Map<ExternalUtil, ToolStatus> almost = allFound();
        almost.put(CHECKMATE, ToolStatus.checking());
        assertEquals("Zjišťuji dostupnost nástrojů, hotovo 8 z 9…", ToolsOverview.summary(almost));
    }

    // ----- text stavu -----

    @Test
    public void statusTexts() {
        //behem kontroly stejny text jako v dialogu Nova validace (ToolStatus)
        assertEquals("zjišťuji…", ToolsOverview.statusText(JHOVE, ToolStatus.checking()));
        assertEquals("nalezen, JHOVE 1.29", ToolsOverview.statusText(JHOVE, ToolStatus.found("1.29")));
        assertEquals("nalezen", ToolsOverview.statusText(JHOVE, ToolStatus.found(null)));
        assertEquals("nalezen", ToolsOverview.statusText(JHOVE, ToolStatus.found("")));
        assertEquals("nenalezen", ToolsOverview.statusText(JHOVE, ToolStatus.notFound("Cannot run program \"jhove\"")));
        assertEquals("chyba při spuštění", ToolsOverview.statusText(KAKADU, ToolStatus.error("prázdný výstup")));
    }

    @Test
    public void longVersionIsShortenedToNameAndNumber() {
        //dřív „nalezen, ImageMagick 7.1.2-24…“ (oříznuto na 30 znaků), teď název a číslo verze
        assertEquals("nalezen, ImageMagick 7.1.2-24", ToolsOverview.statusText(IMAGE_MAGICK, ToolStatus.found(IMAGE_MAGICK_VERSION)));
        assertEquals("nalezen, ImageMagick 7.1.1-21",
                ToolsOverview.statusText(IMAGE_MAGICK, ToolStatus.found("ImageMagick 7.1.1-21 Q16-HDRI x64 2023-10-15")));
    }

    @Test
    public void shortVersionOfRealOutputs() {
        //vystupy podle regularnich vyrazu detekce verze v externalUtils.xml (macOS, Windows, Linux)
        assertEquals("ImageMagick 7.1.2-24", ToolsOverview.shortVersion(IMAGE_MAGICK, "ImageMagick 7.1.2-24"));
        assertEquals("ImageMagick 6.9.11-60", ToolsOverview.shortVersion(IMAGE_MAGICK, "ImageMagick 6.9.11-60 Q16"));
        assertEquals("JHOVE 1.20.1", ToolsOverview.shortVersion(JHOVE, "Rel. 1.20.1"));
        assertEquals("JHOVE 1.28.0", ToolsOverview.shortVersion(JHOVE, "Jhove (Rel. 1.28.0, 2023-05-18)"));
        assertEquals("Kakadu 8.0.5", ToolsOverview.shortVersion(KAKADU, "v8.0.5"));
        assertEquals("Kakadu 7.10.2", ToolsOverview.shortVersion(KAKADU, "Current core system version is v7.10.2"));
        assertEquals("Jpylyzer 2.1.0", ToolsOverview.shortVersion(JPYLYZER, "2.1.0"));
        //cely vystup (Linux): rozhoduje prvni radek
        assertEquals("Jpylyzer 2.2.1", ToolsOverview.shortVersion(JPYLYZER, "2.2.1\nother line 3.4\n"));
        assertEquals("MP3val 0.1.8", ToolsOverview.shortVersion(MP3VAL, "MP3val 0.1.8"));
        assertEquals("shntool 3.0.10", ToolsOverview.shortVersion(SHNTOOL, "shntool 3.0.10"));
        assertEquals("Checkmate 0.21", ToolsOverview.shortVersion(CHECKMATE, "mpck 0.21"));
        assertEquals("veraPDF 1.30.0", ToolsOverview.shortVersion(VERAPDF, "veraPDF 1.30.0"));
        assertEquals("EPUBCheck 5.1.0", ToolsOverview.shortVersion(EPUBCHECK, "EPUBCheck v5.1.0"));
    }

    @Test
    public void shortVersionWithoutVersionNumberKeepsFirstLine() {
        //cislo bez tecky neni cislo verze
        assertEquals("mpck 2", ToolsOverview.shortVersion(CHECKMATE, "mpck 2"));
        assertEquals("nějaký výstup", ToolsOverview.shortVersion(JHOVE, "  nějaký výstup  \ndruhý řádek 1.2"));
        //cislo navazujici na pismeno nebo tecku neni samostatne cislo verze
        assertEquals("x86_64abc1.2", ToolsOverview.shortVersion(JHOVE, "x86_64abc1.2"));
        assertEquals("build.1.2", ToolsOverview.shortVersion(JHOVE, "build.1.2"));
        //v hned za pismenem neni zacatek verze
        assertEquals("Rev8.1", ToolsOverview.shortVersion(KAKADU, "Rev8.1"));
    }

    @Test
    public void shortVersionTakesFirstNumberAndIsTruncated() {
        assertEquals("JHOVE 1.2", ToolsOverview.shortVersion(JHOVE, "Rel. 1.2 (core 3.4.5)"));
        //nazev a cislo delsi nez sirka sloupce se zkrati jako ostatni texty (cele v tooltipu)
        String text = ToolsOverview.statusText(IMAGE_MAGICK, ToolStatus.found("ImageMagick 7.1.2-2400000"));
        assertEquals("nalezen, ImageMagick 7.1.2-24…", text);
        assertEquals(30, text.length());
        String exact = ToolsOverview.statusText(IMAGE_MAGICK, ToolStatus.found("ImageMagick 7.1.2-240"));
        assertEquals("nalezen, ImageMagick 7.1.2-240", exact);
        assertEquals(30, exact.length());
    }

    @Test
    public void compactStatusTextHasOnlyVersionNumber() {
        //v uzke tabulce (Nastaveni) je nazev nastroje vlevo ve stejnem radku
        assertEquals("nalezen, 7.1.2-24", ToolsOverview.compactStatusText(IMAGE_MAGICK, ToolStatus.found(IMAGE_MAGICK_VERSION)));
        assertEquals("nalezen, 8.0.5", ToolsOverview.compactStatusText(KAKADU, ToolStatus.found("v8.0.5")));
        //bez cisla verze jako siroka tabulka
        assertEquals("nalezen, mpck 2", ToolsOverview.compactStatusText(CHECKMATE, ToolStatus.found("mpck 2")));
        assertEquals("nalezen", ToolsOverview.compactStatusText(JHOVE, ToolStatus.found(null)));
        assertEquals("nalezen", ToolsOverview.compactStatusText(JHOVE, ToolStatus.found("")));
        assertEquals("zjišťuji…", ToolsOverview.compactStatusText(JHOVE, ToolStatus.checking()));
        assertEquals("nenalezen", ToolsOverview.compactStatusText(JHOVE, ToolStatus.notFound("Cannot run program")));
        assertEquals("chyba při spuštění", ToolsOverview.compactStatusText(KAKADU, ToolStatus.error("x")));
    }

    @Test
    public void versionNumber() {
        assertEquals("7.1.2-24", ToolsOverview.versionNumber("ImageMagick 7.1.2-24 Q16-HDRI"));
        assertEquals("5.1.0", ToolsOverview.versionNumber("EPUBCheck v5.1.0"));
        assertNull(ToolsOverview.versionNumber("mpck 2"));
        assertNull(ToolsOverview.versionNumber(""));
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
        assertEquals("zjišťuji…", ToolsOverview.statusTooltip(ToolStatus.checking()));
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
