package nkp.pspValidator.shared.externalUtils;

import nkp.pspValidator.shared.OperatingSystem;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.*;

/**
 * Porovnání verze s ověřenými řadami ({@link VersionCheck}) a texty hlášek do protokolu a do výpisu CLI.
 */
public class VersionCheckTest {

    private static final VerifiedVersions JHOVE_MAC = new VerifiedVersions(Arrays.asList(
            new VerifiedVersions.Series("1.29", "1.29.0-SNAPSHOT"),
            new VerifiedVersions.Series("1.34", "1.34.0")), null);
    private static final VerifiedVersions JHOVE_LINUX = JHOVE_MAC.inheritedFrom(OperatingSystem.MAC);
    private static final VerifiedVersions IM_WINDOWS = new VerifiedVersions(
            Collections.singletonList(new VerifiedVersions.Series("7.1", "7.1.2-35")), null);
    private static final VerifiedVersions EMPTY = new VerifiedVersions(Collections.<VerifiedVersions.Series>emptyList(), null);

    @Test
    public void verified() {
        VersionCheck check = VersionCheck.evaluate(JHOVE_MAC, "Jhove (Rel. 1.34.0, 2025-07-02)");
        assertEquals(VersionCheck.Status.VERIFIED, check.getStatus());
        assertEquals("1.34.0", check.getVersion());
        assertEquals("1.34", check.getMatchedSeries().getSeries());
        assertEquals("1.34.0", check.getMatchedSeries().getVerifiedWith());
        assertEquals("Jhove (Rel. 1.34.0, 2025-07-02)", check.getDetectionText());
        assertFalse(check.isWarning());
        assertNull(check.protocolMessage("JHOVE"));
        assertNull(check.protocolSimpleMessage());
        assertNull(check.protocolActualValue());
        assertNull(check.cliNote());
    }

    @Test
    public void verifiedSnapshotSeries() {
        VersionCheck check = VersionCheck.evaluate(JHOVE_MAC, "Jhove (Rel. 1.29.0-SNAPSHOT, 2024-02-18)");
        assertEquals(VersionCheck.Status.VERIFIED, check.getStatus());
        assertEquals("1.29", check.getMatchedSeries().getSeries());
        assertEquals("1.29.0-SNAPSHOT", check.getMatchedSeries().getVerifiedWith());
    }

    @Test
    public void newerPatchInSeriesIsVerified() {
        // rada 7.1 plati pro 7.1.2-35 i pro 7.1.3-1
        assertEquals(VersionCheck.Status.VERIFIED, VersionCheck.evaluate(IM_WINDOWS, "ImageMagick 7.1.2-35").getStatus());
        assertEquals(VersionCheck.Status.VERIFIED, VersionCheck.evaluate(IM_WINDOWS, "ImageMagick 7.1.3-1").getStatus());
        assertEquals(VersionCheck.Status.UNVERIFIED, VersionCheck.evaluate(IM_WINDOWS, "ImageMagick 7.2.0-1").getStatus());
        assertEquals(VersionCheck.Status.UNVERIFIED, VersionCheck.evaluate(IM_WINDOWS, "ImageMagick 7.10.0-1").getStatus());
    }

    @Test
    public void verifiedOnOtherPlatform() {
        VersionCheck check = VersionCheck.evaluate(JHOVE_LINUX, "Rel. 1.34.0");
        assertEquals(VersionCheck.Status.VERIFIED_ON_OTHER_PLATFORM, check.getStatus());
        assertEquals("1.34", check.getMatchedSeries().getSeries());
        assertFalse(check.isWarning());
        assertNull(check.protocolMessage("JHOVE"));
        assertEquals("ověřeno na macOS (řada 1.34)", check.cliNote());
    }

    @Test
    public void unverified() {
        VersionCheck check = VersionCheck.evaluate(JHOVE_MAC, "Jhove (Rel. 1.36.0, 2026-01-15)");
        assertEquals(VersionCheck.Status.UNVERIFIED, check.getStatus());
        assertEquals("1.36.0", check.getVersion());
        assertNull(check.getMatchedSeries());
        assertTrue(check.isWarning());
        assertEquals("nástroj JHOVE je ve verzi 1.36.0, která nebyla s validátorem ověřena (ověřené řady: 1.29, 1.34); "
                + "výsledky kontroly se mohou lišit", check.protocolMessage("JHOVE"));
        assertEquals("neověřená verze nástroje", check.protocolSimpleMessage());
        assertEquals("1.36.0", check.protocolActualValue());
        assertEquals("ověřené řady: 1.29, 1.34", check.seriesPhrase());
        assertEquals("neověřená verze, ověřené řady: 1.29, 1.34", check.cliNote());
    }

    @Test
    public void unverifiedSingleSeries() {
        VersionCheck check = VersionCheck.evaluate(IM_WINDOWS, "ImageMagick 7.0.10-1");
        assertEquals("nástroj ImageMagick je ve verzi 7.0.10, která nebyla s validátorem ověřena (ověřená řada: 7.1); "
                + "výsledky kontroly se mohou lišit", check.protocolMessage("ImageMagick"));
        assertEquals("neověřená verze, ověřená řada: 7.1", check.cliNote());
    }

    @Test
    public void unverifiedOnInheritingPlatform() {
        VersionCheck check = VersionCheck.evaluate(JHOVE_LINUX, "Rel. 1.36.0");
        assertEquals(VersionCheck.Status.UNVERIFIED, check.getStatus());
        assertEquals("nástroj JHOVE je ve verzi 1.36.0, která nebyla s validátorem ověřena (ověřené řady na macOS: 1.29, 1.34); "
                + "výsledky kontroly se mohou lišit", check.protocolMessage("JHOVE"));
        assertEquals("neověřená verze, ověřené řady na macOS: 1.29, 1.34", check.cliNote());
    }

    @Test
    public void emptyListMakesEveryVersionUnverified() {
        VersionCheck check = VersionCheck.evaluate(EMPTY, "v8.5");
        assertEquals(VersionCheck.Status.UNVERIFIED, check.getStatus());
        assertEquals("nástroj Kakadu je ve verzi 8.5, která nebyla s validátorem ověřena (na této platformě nebyla ověřena "
                + "žádná verze); výsledky kontroly se mohou lišit", check.protocolMessage("Kakadu"));
        assertEquals("neověřená verze, na této platformě nebyla ověřena žádná verze", check.cliNote());
        assertEquals("na macOS nebyla ověřena žádná verze", VersionCheck.evaluate(EMPTY.inheritedFrom(OperatingSystem.MAC), "v8.5").seriesPhrase());
    }

    @Test
    public void unrecognized() {
        VersionCheck check = VersionCheck.evaluate(JHOVE_MAC, "Jhove (Rel. unknown, 2025-07-02)");
        assertEquals(VersionCheck.Status.UNRECOGNIZED, check.getStatus());
        assertNull(check.getVersion());
        assertTrue(check.isWarning());
        assertEquals("verzi nástroje JHOVE nelze rozpoznat z výstupu „Jhove (Rel. unknown, 2025-07-02)“ (ověřené řady: 1.29, 1.34); "
                + "výsledky kontroly se mohou lišit", check.protocolMessage("JHOVE"));
        assertEquals("verzi nástroje nelze rozpoznat", check.protocolSimpleMessage());
        assertEquals("Jhove (Rel. unknown, 2025-07-02)", check.protocolActualValue());
        assertEquals("verzi nelze rozpoznat, ověřené řady: 1.29, 1.34", check.cliNote());
    }

    @Test
    public void unrecognizedDetectionTextIsFirstLineShortened() {
        String longLine = "x" + new String(new char[100]).replace('\0', 'y');
        VersionCheck check = VersionCheck.evaluate(JHOVE_MAC, longLine + "\ndruhý řádek");
        String shortText = check.shortDetectionText();
        assertEquals(VersionCheck.MAX_DETECTION_TEXT, shortText.length());
        assertTrue(shortText, shortText.endsWith("…"));
        assertTrue(shortText, shortText.startsWith("xyyy"));
        assertFalse(check.protocolMessage("JHOVE"), check.protocolMessage("JHOVE").contains("druhý"));
        // presne na hranici se nezkracuje
        String exact = new String(new char[VersionCheck.MAX_DETECTION_TEXT]).replace('\0', 'a');
        assertEquals(exact, VersionCheck.evaluate(JHOVE_MAC, exact).shortDetectionText());
        String oneMore = exact + "a";
        assertEquals(exact.substring(0, VersionCheck.MAX_DETECTION_TEXT - 1) + "…", VersionCheck.evaluate(JHOVE_MAC, oneMore).shortDetectionText());
    }

    @Test
    public void withoutListNothingIsMarked() {
        for (String text : new String[]{"Rel. 1.36.0", "Rel. 1.34.0", "neznámý výstup"}) {
            VersionCheck check = VersionCheck.evaluate(null, text);
            assertEquals(text, VersionCheck.Status.NO_LIST, check.getStatus());
            assertFalse(text, check.isWarning());
            assertNull(text, check.protocolMessage("JHOVE"));
            assertNull(text, check.cliNote());
            assertNull(text, check.protocolActualValue());
            assertNull(text, check.seriesPhrase());
            assertNull(text, check.getVerifiedVersions());
        }
        assertEquals("1.36.0", VersionCheck.evaluate(null, "Rel. 1.36.0").getVersion());
    }

    @Test
    public void platformNames() {
        assertEquals("macOS", VersionCheck.platformName(OperatingSystem.MAC));
        assertEquals("Windows", VersionCheck.platformName(OperatingSystem.WINDOWS));
        assertEquals("Linux", VersionCheck.platformName(OperatingSystem.LINUX));
    }
}
