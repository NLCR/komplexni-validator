package nkp.pspValidator.shared.externalUtils;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Číslo verze z textu detekce verze a porovnání s ověřenou řadou ({@link UtilVersion}). Texty jsou to, co vrátí
 * detekce verze s regulárními výrazy z externalUtils.xml (ověření na macOS a Windows 20. 11. 2026), a celé výstupy
 * nástrojů.
 */
public class UtilVersionTest {

    // ----- texty detekce verze podle nastroje a OS -----

    @Test
    public void imageMagick() {
        // LINUX, WINDOWS: regexp "ImageMagick [0-9\.\-]+"; MAC: "ImageMagick\s\S*\s\S*"
        assertEquals("7.1.2", UtilVersion.parse("ImageMagick 7.1.2-35"));
        assertEquals("7.1.2", UtilVersion.parse("ImageMagick 7.1.2-35 Q16-HDRI"));
        // cely radek vystupu na Windows a macOS
        assertEquals("7.1.2", UtilVersion.parse("Version: ImageMagick 7.1.2-35 Q16-HDRI x64 5d1bcc5:20261031"));
        assertEquals("7.1.2", UtilVersion.parse("Version: ImageMagick 7.1.2-35 Q16-HDRI aarch64 acaf7e418:20261031 https://imagemagick.org"));
        assertEquals("6.9.11", UtilVersion.parse("ImageMagick 6.9.11-60 Q16 x86_64"));
    }

    @Test
    public void jhove() {
        // LINUX, WINDOWS: regexp "Rel\. [0-9\.]+" (u snapshotu odrizne -SNAPSHOT); MAC: "Jhove \(.*\)"
        assertEquals("1.34.0", UtilVersion.parse("Rel. 1.34.0"));
        assertEquals("1.29.0", UtilVersion.parse("Rel. 1.29.0"));
        assertEquals("1.34.0", UtilVersion.parse("Jhove (Rel. 1.34.0, 2025-07-02)"));
        assertEquals("1.29.0", UtilVersion.parse("Jhove (Rel. 1.29.0-SNAPSHOT, 2024-02-18)"));
    }

    @Test
    public void jpylyzer() {
        // vsechny OS: cely stdout
        assertEquals("2.2.1", UtilVersion.parse("2.2.1"));
        assertEquals("2.2.1", UtilVersion.parse("2.2.1\n"));
    }

    @Test
    public void kakadu() {
        // LINUX, WINDOWS: regexp "v[\.0-9]+"; MAC: "Current core system version is \S*"
        assertEquals("8.5", UtilVersion.parse("v8.5"));
        assertEquals("8.5", UtilVersion.parse("Current core system version is v8.5"));
        // cely vystup: "version" pred cislem neni "v" s cislem
        assertEquals("8.5", UtilVersion.parse("This is Kakadu's \"kdu_expand\" application.\n    Compiled against the Kakadu core system, version v8.5\n"));
    }

    @Test
    public void audioTools() {
        assertEquals("0.1.8", UtilVersion.parse("MP3val 0.1.8"));
        assertEquals("3.0.10", UtilVersion.parse("shntool 3.0.10"));
        assertEquals("0.22", UtilVersion.parse("mpck 0.22"));
    }

    @Test
    public void veraPdfAndEpubCheck() {
        assertEquals("1.30.0", UtilVersion.parse("veraPDF 1.30.0"));
        assertEquals("1.30.4", UtilVersion.parse("veraPDF 1.30.4"));
        assertEquals("5.4.0", UtilVersion.parse("EPUBCheck v5.4.0"));
    }

    // ----- kraje parsovani -----

    @Test
    public void nullEmptyAndTextWithoutNumber() {
        assertNull(UtilVersion.parse(null));
        assertNull(UtilVersion.parse(""));
        assertNull(UtilVersion.parse("   \n"));
        assertNull(UtilVersion.parse("neznámý výstup"));
    }

    @Test
    public void dateIsNotVersion() {
        assertNull(UtilVersion.parse("(2025-07-02)"));
        assertNull(UtilVersion.parse("Jhove (Rel. unknown, 2025-07-02)"));
        // datum za verzi nevadi, bere se prvni vyskyt
        assertEquals("1.34.0", UtilVersion.parse("Jhove (Rel. 1.34.0, 2025-07-02)"));
    }

    @Test
    public void singleNumberIsNotVersion() {
        assertNull(UtilVersion.parse("v8"));
        assertNull(UtilVersion.parse("build 2024"));
        // nejmensi rozpoznana verze: dve cisla s teckou
        assertEquals("8.0", UtilVersion.parse("v8.0"));
    }

    @Test
    public void firstOfMoreNumbersWins() {
        assertEquals("1.2", UtilVersion.parse("tool 1.2 (library 3.4.5)"));
        assertEquals("3.4.5", UtilVersion.parse("tool x (library 3.4.5) 1.2"));
    }

    @Test
    public void numberGluedToLetterOrDigitIsNotVersion() {
        // "x64", "Q16" ani cislo za pismenem (krome "v") se nebere
        assertNull(UtilVersion.parse("abc1.2"));
        assertEquals("2.0", UtilVersion.parse("abc1.2 2.0"));
        // "v" pred cislem smi byt, ale ne jako konec slova
        assertNull(UtilVersion.parse("rev1.2"));
    }

    @Test
    public void suffixesAreIgnored() {
        assertEquals("7.1.2", UtilVersion.parse("7.1.2-35"));
        assertEquals("1.29.0", UtilVersion.parse("1.29.0-SNAPSHOT"));
        assertEquals("5.4.0", UtilVersion.parse("5.4.0rc2"));
        assertEquals("5.4", UtilVersion.parse("5.4-beta"));
    }

    // ----- porovnani s radou -----

    @Test
    public void seriesPrefixByComponents() {
        assertTrue(UtilVersion.isInSeries("7.1", "7.1"));
        assertTrue(UtilVersion.isInSeries("7.1.2", "7.1"));
        assertTrue(UtilVersion.isInSeries("7.1.3", "7.1"));
        assertTrue(UtilVersion.isInSeries("1.34.0", "1.34"));
        assertTrue(UtilVersion.isInSeries("0.22", "0.22"));
        assertTrue(UtilVersion.isInSeries("8.5", "8.5"));
    }

    @Test
    public void seriesIsNotStringPrefix() {
        assertFalse(UtilVersion.isInSeries("1.34", "1.3"));
        assertFalse(UtilVersion.isInSeries("1.34.0", "1.3"));
        assertFalse(UtilVersion.isInSeries("7.10", "7.1"));
        assertFalse(UtilVersion.isInSeries("7.10.1", "7.1"));
        assertFalse(UtilVersion.isInSeries("7.1", "7.10"));
        assertFalse(UtilVersion.isInSeries("0.21", "0.22"));
        assertFalse(UtilVersion.isInSeries("0.23", "0.22"));
        assertFalse(UtilVersion.isInSeries("1.35.0", "1.34"));
        assertFalse(UtilVersion.isInSeries("1.33.9", "1.34"));
        assertFalse(UtilVersion.isInSeries("17.1", "7.1"));
    }

    @Test
    public void versionShorterThanSeries() {
        assertFalse(UtilVersion.isInSeries("1.34", "1.34.0"));
        assertFalse(UtilVersion.isInSeries("7.1", "7.1.2"));
        assertTrue(UtilVersion.isInSeries("7.1.2", "7.1.2"));
    }

    @Test
    public void leadingZerosCompareAsNumbers() {
        assertTrue(UtilVersion.isInSeries("7.01.2", "7.1"));
        assertTrue(UtilVersion.isInSeries("7.1", "07.1"));
        assertTrue(UtilVersion.isInSeries("0.0.1", "0.0"));
        assertFalse(UtilVersion.isInSeries("7.0", "7.00.1"));
        // velka cisla bez preteceni
        assertTrue(UtilVersion.isInSeries("99999999999999999999.1", "99999999999999999999"));
    }

    @Test
    public void suffixesMustBeParsedAwayBeforeComparison() {
        // isInSeries bere jen cislo verze (UtilVersion.parse), text s priponou do rady nepatri
        assertFalse(UtilVersion.isInSeries("7.1.2-35", "7.1"));
        assertFalse(UtilVersion.isInSeries("1.29.0-SNAPSHOT", "1.29"));
        assertTrue(UtilVersion.isInSeries(UtilVersion.parse("ImageMagick 7.1.2-35"), "7.1"));
        assertTrue(UtilVersion.isInSeries(UtilVersion.parse("Jhove (Rel. 1.29.0-SNAPSHOT, 2024-02-18)"), "1.29"));
        assertTrue(UtilVersion.isInSeries(UtilVersion.parse("5.4.0rc2"), "5.4"));
    }

    @Test
    public void invalidInputsAreNotInSeries() {
        assertFalse(UtilVersion.isInSeries(null, "1.34"));
        assertFalse(UtilVersion.isInSeries("1.34", null));
        assertFalse(UtilVersion.isInSeries("1.34", ""));
        assertFalse(UtilVersion.isInSeries("", "1"));
        assertFalse(UtilVersion.isInSeries("1.34", "1.x"));
        assertFalse(UtilVersion.isInSeries("1..34", "1"));
    }

    @Test
    public void validSeries() {
        assertTrue(UtilVersion.isValidSeries("7.1"));
        assertTrue(UtilVersion.isValidSeries("8"));
        assertTrue(UtilVersion.isValidSeries("1.29.0"));
        assertFalse(UtilVersion.isValidSeries(null));
        assertFalse(UtilVersion.isValidSeries(""));
        assertFalse(UtilVersion.isValidSeries("7.1."));
        assertFalse(UtilVersion.isValidSeries(".7"));
        assertFalse(UtilVersion.isValidSeries("7.x"));
        assertFalse(UtilVersion.isValidSeries("v7.1"));
        assertFalse(UtilVersion.isValidSeries("7.1 "));
    }
}
