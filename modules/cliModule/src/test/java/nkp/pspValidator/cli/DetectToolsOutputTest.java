package nkp.pspValidator.cli;

import nkp.pspValidator.shared.OperatingSystem;
import nkp.pspValidator.shared.externalUtils.CliCommand;
import nkp.pspValidator.shared.externalUtils.ExternalUtil;
import nkp.pspValidator.shared.externalUtils.ExternalUtilManager;
import nkp.pspValidator.shared.externalUtils.VerifiedVersions;
import nkp.pspValidator.shared.externalUtils.VersionCheck;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Výpis nástrojů na začátku validace (Main.detectImageTools) s ověřenými řadami verzí: řádky „Kontroluji dostupnost
 * nástroje X: …“ zůstávají beze změny, u neověřené a nerozpoznané verze a u verze ověřené na jiné platformě následuje
 * řádek s poznámkou odsazený čtyřmi mezerami. Nástroje se nespouštějí (manažer vrací pevné texty detekce), test
 * běží stejně na každém systému.
 */
public class DetectToolsOutputTest {

    /** Manažer s pevnými výsledky detekce verze: text, nebo výjimka (hodnota null). */
    private static class StubManager extends ExternalUtilManager {
        private final Map<ExternalUtil, String> detected;

        StubManager(Map<ExternalUtil, String> detected, Map<ExternalUtil, VerifiedVersions> verified) {
            super(new HashMap<>(), new HashMap<>(), verified);
            this.detected = detected;
        }

        @Override
        public boolean isVersionDetectionDefined(ExternalUtil util) {
            return true;
        }

        @Override
        public String runUtilVersionDetection(ExternalUtil util) throws CliCommand.CliCommandException {
            String text = detected.get(util);
            if (text == null) {
                throw new CliCommand.CliCommandException("Cannot run program \"x\"");
            }
            return text;
        }
    }

    private static VerifiedVersions list(OperatingSystem inheritedFrom, String... seriesAndVersion) {
        VerifiedVersions.Series[] series = new VerifiedVersions.Series[seriesAndVersion.length / 2];
        for (int i = 0; i < series.length; i++) {
            series[i] = new VerifiedVersions.Series(seriesAndVersion[2 * i], seriesAndVersion[2 * i + 1]);
        }
        return new VerifiedVersions(Arrays.asList(series), inheritedFrom);
    }

    private static String run(ExternalUtilManager manager, EnumSet<ExternalUtil> disabled) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(bytes, true, "UTF-8");
        Main.detectImageTools(out, manager, disabled);
        return bytes.toString("UTF-8").replace("\r\n", "\n");
    }

    @Test
    public void notesForUnverifiedUnrecognizedAndEmptyList() throws Exception {
        Map<ExternalUtil, String> detected = new EnumMap<>(ExternalUtil.class);
        detected.put(ExternalUtil.JPYLYZER, "2.2.1");
        detected.put(ExternalUtil.IMAGE_MAGICK, "ImageMagick 7.1.3-1 Q16-HDRI");
        detected.put(ExternalUtil.JHOVE, "Jhove (Rel. 1.36.0, 2026-01-15)");
        detected.put(ExternalUtil.KAKADU, "v8.5");
        detected.put(ExternalUtil.MP3VAL, "MP3val");
        detected.put(ExternalUtil.SHNTOOL, "shntool 3.0.10");
        detected.put(ExternalUtil.CHECKMATE, "mpck 0.22");
        Map<ExternalUtil, VerifiedVersions> verified = new EnumMap<>(ExternalUtil.class);
        verified.put(ExternalUtil.JPYLYZER, list(null, "2.2", "2.2.1"));
        verified.put(ExternalUtil.IMAGE_MAGICK, list(null, "7.1", "7.1.2-35"));
        verified.put(ExternalUtil.JHOVE, list(null, "1.29", "1.29.0-SNAPSHOT", "1.34", "1.34.0"));
        verified.put(ExternalUtil.KAKADU, list(null));
        verified.put(ExternalUtil.MP3VAL, list(null, "0.1", "0.1.8"));
        // shntool a Checkmate bez seznamu: verze se neoznacuje
        StubManager manager = new StubManager(detected, verified);
        String out = run(manager, EnumSet.of(ExternalUtil.VERAPDF));
        assertEquals("Kontroluji dostupnost nástroje ImageMagick: nalezen, verze: ImageMagick 7.1.3-1 Q16-HDRI\n"
                + "Kontroluji dostupnost nástroje JHOVE: nalezen, verze: Jhove (Rel. 1.36.0, 2026-01-15)\n"
                + "    neověřená verze, ověřené řady: 1.29, 1.34\n"
                + "Kontroluji dostupnost nástroje Jpylyzer: nalezen, verze: 2.2.1\n"
                + "Kontroluji dostupnost nástroje Kakadu: nalezen, verze: v8.5\n"
                + "    neověřená verze, na této platformě nebyla ověřena žádná verze\n"
                + "Kontroluji dostupnost nástroje MP3val: nalezen, verze: MP3val\n"
                + "    verzi nelze rozpoznat, ověřená řada: 0.1\n"
                + "Kontroluji dostupnost nástroje shntool: nalezen, verze: shntool 3.0.10\n"
                + "Kontroluji dostupnost nástroje Checkmate: nalezen, verze: mpck 0.22\n"
                + "Vypnuto použití nástroje veraPDF.\n"
                + "Kontroluji dostupnost nástroje EPUBCheck: nenalezen (Cannot run program \"x\")\n", out);
        // verze dostupnych nastroju je zapsana pro validaci, nedostupny nastroj verzi nema
        assertEquals("Jhove (Rel. 1.36.0, 2026-01-15)", manager.getUtilVersion(ExternalUtil.JHOVE));
        assertEquals(VersionCheck.Status.UNVERIFIED, manager.getVersionCheck(ExternalUtil.JHOVE).getStatus());
        assertEquals(VersionCheck.Status.VERIFIED, manager.getVersionCheck(ExternalUtil.IMAGE_MAGICK).getStatus());
        assertNull(manager.getUtilVersion(ExternalUtil.EPUBCHECK));
        assertNull(manager.getUtilVersion(ExternalUtil.VERAPDF));
        assertFalse(manager.isUtilAvailable(ExternalUtil.EPUBCHECK));
    }

    @Test
    public void linuxInheritingMacNotes() throws Exception {
        Map<ExternalUtil, String> detected = new EnumMap<>(ExternalUtil.class);
        detected.put(ExternalUtil.JHOVE, "Rel. 1.34.0");
        detected.put(ExternalUtil.VERAPDF, "veraPDF 1.28.2");
        Map<ExternalUtil, VerifiedVersions> verified = new EnumMap<>(ExternalUtil.class);
        verified.put(ExternalUtil.JHOVE, list(OperatingSystem.MAC, "1.29", "1.29.0-SNAPSHOT", "1.34", "1.34.0"));
        verified.put(ExternalUtil.VERAPDF, list(OperatingSystem.MAC, "1.30", "1.30.2"));
        EnumSet<ExternalUtil> disabled = EnumSet.allOf(ExternalUtil.class);
        disabled.remove(ExternalUtil.JHOVE);
        disabled.remove(ExternalUtil.VERAPDF);
        String out = run(new StubManager(detected, verified), disabled);
        assertEquals("Vypnuto použití nástroje ImageMagick.\n"
                + "Kontroluji dostupnost nástroje JHOVE: nalezen, verze: Rel. 1.34.0\n"
                + "    ověřeno na macOS (řada 1.34)\n"
                + "Vypnuto použití nástroje Jpylyzer.\n"
                + "Vypnuto použití nástroje Kakadu.\n"
                + "Vypnuto použití nástroje MP3val.\n"
                + "Vypnuto použití nástroje shntool.\n"
                + "Vypnuto použití nástroje Checkmate.\n"
                + "Kontroluji dostupnost nástroje veraPDF: nalezen, verze: veraPDF 1.28.2\n"
                + "    neověřená verze, ověřená řada na macOS: 1.30\n"
                + "Vypnuto použití nástroje EPUBCheck.\n", out);
    }

    @Test
    public void withoutListsOutputIsUnchanged() throws Exception {
        // starsi externalUtils.xml bez verifiedVersions: vypis jako pred zavedenim overenych rad
        Map<ExternalUtil, String> detected = new EnumMap<>(ExternalUtil.class);
        detected.put(ExternalUtil.JHOVE, "Rel. 1.36.0");
        EnumSet<ExternalUtil> disabled = EnumSet.allOf(ExternalUtil.class);
        disabled.remove(ExternalUtil.JHOVE);
        String out = run(new StubManager(detected, Collections.<ExternalUtil, VerifiedVersions>emptyMap()), disabled);
        assertTrue(out, out.contains("Kontroluji dostupnost nástroje JHOVE: nalezen, verze: Rel. 1.36.0\nVypnuto použití nástroje Jpylyzer.\n"));
        assertFalse(out, out.contains("    "));
    }
}
