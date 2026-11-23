package nkp.pspValidator.shared.externalUtils;

import nkp.pspValidator.shared.OperatingSystem;
import nkp.pspValidator.shared.engine.exceptions.ValidatorConfigurationException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.EnumMap;
import java.util.Map;

import static nkp.pspValidator.shared.externalUtils.ExternalUtil.*;
import static org.junit.Assert.*;

/**
 * Načtení ověřených řad verzí z externalUtils.xml (element verifiedVersions, {@link ExternalUtilManagerFactory}):
 * vlastní seznam, Linux přebírající macOS, prázdný seznam, chybějící seznam (starší konfigurace) a chyby konfigurace;
 * obsah skutečné konfigurace z repozitáře proti ručně vypsanému seznamu.
 */
public class VerifiedVersionsConfigTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private static final String DETECTION = "<versionDetection><operatingSystem name=\"MAC\"><command>${UTIL_PATH}jhove --version</command>"
            + "<parser><stream>STDOUT</stream><regexp>Jhove \\(.*\\)</regexp></parser></operatingSystem></versionDetection>";

    private File config(String jhoveInner) throws Exception {
        File file = tmp.newFile();
        String xml = "<externalUtils><util name=\"JHOVE\">" + jhoveInner + DETECTION + "</util></externalUtils>";
        Files.write(file.toPath(), xml.getBytes(StandardCharsets.UTF_8));
        return file;
    }

    private static ExternalUtilManager manager(File config, OperatingSystem os) throws Exception {
        return new ExternalUtilManagerFactory(config).buildExternalUtilManager(os);
    }

    /** Řady a přesné verze, např. „1.29 (1.29.0-SNAPSHOT), 1.34 (1.34.0)“, s „ z MAC“ u převzatého seznamu. */
    private static String describe(VerifiedVersions versions) {
        if (versions == null) {
            return null;
        }
        StringBuilder text = new StringBuilder();
        for (VerifiedVersions.Series series : versions.getSeries()) {
            text.append(text.length() == 0 ? "" : ", ").append(series.getSeries());
            if (series.getVerifiedWith() != null) {
                text.append(" (").append(series.getVerifiedWith()).append(')');
            }
        }
        if (versions.getInheritedFrom() != null) {
            text.append(" z ").append(versions.getInheritedFrom());
        }
        return text.toString();
    }

    @Test
    public void ownListAndLinuxInheritingMac() throws Exception {
        File config = config("<verifiedVersions>"
                + "<operatingSystem name=\"MAC\"><series verifiedWith=\"1.29.0-SNAPSHOT\">1.29</series><series verifiedWith=\"1.34.0\">1.34</series></operatingSystem>"
                + "<operatingSystem name=\"WINDOWS\"><series verifiedWith=\"1.34.0\">1.34</series></operatingSystem>"
                + "<operatingSystem name=\"LINUX\" inheritsFrom=\"MAC\"/>"
                + "</verifiedVersions>");
        assertEquals("1.29 (1.29.0-SNAPSHOT), 1.34 (1.34.0)", describe(manager(config, OperatingSystem.MAC).getVerifiedVersions(JHOVE)));
        assertEquals("1.34 (1.34.0)", describe(manager(config, OperatingSystem.WINDOWS).getVerifiedVersions(JHOVE)));
        assertEquals("1.29 (1.29.0-SNAPSHOT), 1.34 (1.34.0) z MAC", describe(manager(config, OperatingSystem.LINUX).getVerifiedVersions(JHOVE)));
        // ostatni nastroje v konfiguraci nejsou: bez seznamu
        assertNull(manager(config, OperatingSystem.MAC).getVerifiedVersions(KAKADU));

        ExternalUtilManager linux = manager(config, OperatingSystem.LINUX);
        assertEquals(VersionCheck.Status.VERIFIED_ON_OTHER_PLATFORM, linux.checkVersion(JHOVE, "Rel. 1.29.0").getStatus());
        assertEquals(VersionCheck.Status.UNVERIFIED, linux.checkVersion(JHOVE, "Rel. 1.36.0").getStatus());
        ExternalUtilManager windows = manager(config, OperatingSystem.WINDOWS);
        assertEquals(VersionCheck.Status.VERIFIED, windows.checkVersion(JHOVE, "Rel. 1.34.0").getStatus());
        assertEquals(VersionCheck.Status.UNVERIFIED, windows.checkVersion(JHOVE, "Rel. 1.29.0").getStatus());
    }

    @Test
    public void inheritingPlatformBeforeSourceInDocument() throws Exception {
        File config = config("<verifiedVersions>"
                + "<operatingSystem name=\"LINUX\" inheritsFrom=\"MAC\"/>"
                + "<operatingSystem name=\"MAC\"><series>8.5</series></operatingSystem>"
                + "</verifiedVersions>");
        assertEquals("8.5 z MAC", describe(manager(config, OperatingSystem.LINUX).getVerifiedVersions(JHOVE)));
        assertEquals("8.5", describe(manager(config, OperatingSystem.MAC).getVerifiedVersions(JHOVE)));
        // verifiedWith neni povinny
        assertNull(manager(config, OperatingSystem.MAC).getVerifiedVersions(JHOVE).getSeries().get(0).getVerifiedWith());
    }

    @Test
    public void emptyOperatingSystemMeansNothingVerified() throws Exception {
        File config = config("<verifiedVersions><operatingSystem name=\"WINDOWS\"/></verifiedVersions>");
        ExternalUtilManager windows = manager(config, OperatingSystem.WINDOWS);
        assertEquals("", describe(windows.getVerifiedVersions(JHOVE)));
        assertEquals(VersionCheck.Status.UNVERIFIED, windows.checkVersion(JHOVE, "Rel. 1.34.0").getStatus());
        // platforma, ktera v seznamu chybi, seznam nema
        assertNull(manager(config, OperatingSystem.MAC).getVerifiedVersions(JHOVE));
        assertEquals(VersionCheck.Status.NO_LIST, manager(config, OperatingSystem.MAC).checkVersion(JHOVE, "Rel. 1.34.0").getStatus());
    }

    @Test
    public void configWithoutListMarksNothing() throws Exception {
        // starsi nebo uzivatelem upravena externalUtils.xml bez verifiedVersions
        File config = config("");
        for (OperatingSystem os : OperatingSystem.values()) {
            ExternalUtilManager manager = manager(config, os);
            for (ExternalUtil util : ExternalUtil.values()) {
                assertNull(os + "/" + util, manager.getVerifiedVersions(util));
            }
            assertEquals(os.name(), VersionCheck.Status.NO_LIST, manager.checkVersion(JHOVE, "Rel. 1.36.0").getStatus());
            assertEquals(os.name(), VersionCheck.Status.NO_LIST, manager.checkVersion(JHOVE, "neznámý výstup").getStatus());
        }
    }

    @Test
    public void storedVersionIsCheckedAndCanBeCleared() throws Exception {
        File config = config("<verifiedVersions><operatingSystem name=\"MAC\"><series>1.34</series></operatingSystem></verifiedVersions>");
        ExternalUtilManager manager = manager(config, OperatingSystem.MAC);
        assertNull(manager.getVersionCheck(JHOVE));
        assertNull(manager.getUtilVersion(JHOVE));
        manager.setUtilVersion(JHOVE, "Jhove (Rel. 1.36.0, 2026-01-15)");
        assertEquals("Jhove (Rel. 1.36.0, 2026-01-15)", manager.getUtilVersion(JHOVE));
        assertEquals(VersionCheck.Status.UNVERIFIED, manager.getVersionCheck(JHOVE).getStatus());
        manager.setUtilVersion(JHOVE, "Jhove (Rel. 1.34.0, 2025-07-02)");
        assertEquals(VersionCheck.Status.VERIFIED, manager.getVersionCheck(JHOVE).getStatus());
        manager.setUtilVersion(JHOVE, null);
        assertNull(manager.getVersionCheck(JHOVE));
        assertNull(manager.getVersionCheck(KAKADU));
    }

    // ----- chyby konfigurace -----

    private void assertConfigError(String inner, String expectedMessage) throws Exception {
        try {
            new ExternalUtilManagerFactory(config(inner));
            fail("čekána chyba konfigurace: " + inner);
        } catch (ValidatorConfigurationException e) {
            assertEquals(expectedMessage, e.getMessage());
        }
    }

    @Test
    public void invalidSeries() throws Exception {
        assertConfigError("<verifiedVersions><operatingSystem name=\"MAC\"><series>1.x</series></operatingSystem></verifiedVersions>",
                "JHOVE: neplatná řada verzí pro MAC: '1.x'");
        assertConfigError("<verifiedVersions><operatingSystem name=\"MAC\"><series> </series></operatingSystem></verifiedVersions>",
                "JHOVE: neplatná řada verzí pro MAC: ''");
        assertConfigError("<verifiedVersions><operatingSystem name=\"MAC\"><series>v1.34</series></operatingSystem></verifiedVersions>",
                "JHOVE: neplatná řada verzí pro MAC: 'v1.34'");
    }

    @Test
    public void unknownOperatingSystem() throws Exception {
        assertConfigError("<verifiedVersions><operatingSystem name=\"BSD\"/></verifiedVersions>",
                "JHOVE: neznámý operační systém v ověřených verzích: 'BSD'");
        assertConfigError("<verifiedVersions><operatingSystem name=\"LINUX\" inheritsFrom=\"OSX\"/></verifiedVersions>",
                "JHOVE: neznámý operační systém v ověřených verzích: 'OSX'");
    }

    @Test
    public void inheritanceErrors() throws Exception {
        assertConfigError("<verifiedVersions><operatingSystem name=\"LINUX\" inheritsFrom=\"MAC\"/></verifiedVersions>",
                "JHOVE: platforma LINUX přebírá ověřené verze z MAC, ta ale vlastní seznam nemá");
        // retezeni prevzeti neni dovoleno
        assertConfigError("<verifiedVersions><operatingSystem name=\"MAC\"><series>1.34</series></operatingSystem>"
                        + "<operatingSystem name=\"WINDOWS\" inheritsFrom=\"MAC\"/><operatingSystem name=\"LINUX\" inheritsFrom=\"WINDOWS\"/></verifiedVersions>",
                "JHOVE: platforma LINUX přebírá ověřené verze z WINDOWS, ta ale vlastní seznam nemá");
        assertConfigError("<verifiedVersions><operatingSystem name=\"MAC\"><series>1.34</series></operatingSystem>"
                        + "<operatingSystem name=\"LINUX\" inheritsFrom=\"MAC\"><series>1.34</series></operatingSystem></verifiedVersions>",
                "JHOVE: platforma LINUX přebírá ověřené verze z MAC, nesmí mít vlastní řady");
    }

    @Test
    public void duplicateOperatingSystem() throws Exception {
        assertConfigError("<verifiedVersions><operatingSystem name=\"MAC\"/><operatingSystem name=\"MAC\"/></verifiedVersions>",
                "JHOVE: ověřené verze pro MAC jsou uvedeny dvakrát");
        assertConfigError("<verifiedVersions><operatingSystem name=\"MAC\"/><operatingSystem name=\"MAC\" inheritsFrom=\"WINDOWS\"/></verifiedVersions>",
                "JHOVE: ověřené verze pro MAC jsou uvedeny dvakrát");
    }

    // ----- skutecna konfigurace -----

    /**
     * Ověřené řady ve skutečném externalUtils.xml: seznam z ověření 20. 11. 2026 na macOS a Windows a z ověření
     * 23. 11. 2026 na Linuxu a Kakadu na všech platformách (rozhodnutí uživatele 23. 11. 2026); Linux má vlastní
     * seznamy, nic se nepřebírá. Vypsáno ručně, ne odvozeno z kódu.
     */
    @Test
    public void repositoryConfigContainsVerifiedList() throws Exception {
        Map<ExternalUtil, String> mac = new EnumMap<>(ExternalUtil.class);
        mac.put(IMAGE_MAGICK, "7.1 (7.1.2-35)");
        mac.put(JHOVE, "1.29 (1.29.0-SNAPSHOT), 1.34 (1.34.0)");
        mac.put(JPYLYZER, "2.2 (2.2.1)");
        mac.put(KAKADU, "8.5 (8.5), 8.7 (8.7)");
        mac.put(MP3VAL, "0.1 (0.1.8)");
        mac.put(SHNTOOL, "3.0 (3.0.10)");
        mac.put(CHECKMATE, "0.22 (0.22)");
        mac.put(VERAPDF, "1.30 (1.30.2)");
        mac.put(EPUBCHECK, "5.4 (5.4.0)");
        Map<ExternalUtil, String> windows = new EnumMap<>(ExternalUtil.class);
        windows.put(IMAGE_MAGICK, "7.1 (7.1.2-35)");
        windows.put(JHOVE, "1.34 (1.34.0)");
        windows.put(JPYLYZER, "2.2 (2.2.1)");
        windows.put(KAKADU, "7.9 (7.9.1), 8.5 (8.5), 8.7 (8.7)");
        windows.put(MP3VAL, "0.1 (0.1.8)");
        windows.put(SHNTOOL, "3.0 (3.0.10)");
        windows.put(CHECKMATE, "0.22 (0.22)");
        windows.put(VERAPDF, "1.30 (1.30.4)");
        windows.put(EPUBCHECK, "5.4 (5.4.0)");
        Map<ExternalUtil, String> linux = new EnumMap<>(ExternalUtil.class);
        linux.put(IMAGE_MAGICK, "7.1 (7.1.1-43)");
        linux.put(JHOVE, "1.28 (1.28.0), 1.34 (1.34.0)");
        linux.put(JPYLYZER, "2.2 (2.2.1)");
        linux.put(KAKADU, "8.1 (8.1), 8.5 (8.5), 8.6 (8.6.2), 8.7 (8.7)");
        linux.put(MP3VAL, "0.1 (0.1.8)");
        linux.put(SHNTOOL, "3.0 (3.0.10)");
        linux.put(CHECKMATE, "0.22 (0.22)");
        linux.put(VERAPDF, "1.30 (1.30.4)");
        linux.put(EPUBCHECK, "5.4 (5.4.0)");

        ExternalUtilManagerFactory factory = new ExternalUtilManagerFactory(ExternalUtilsTestSupport.EXTERNAL_UTILS_CONFIG);
        assertList(mac, factory.buildExternalUtilManager(OperatingSystem.MAC));
        assertList(windows, factory.buildExternalUtilManager(OperatingSystem.WINDOWS));
        assertList(linux, factory.buildExternalUtilManager(OperatingSystem.LINUX));
    }

    private static void assertList(Map<ExternalUtil, String> expected, ExternalUtilManager manager) {
        assertEquals(ExternalUtil.values().length, expected.size());
        for (ExternalUtil util : ExternalUtil.values()) {
            assertEquals(util.name(), expected.get(util), describe(manager.getVerifiedVersions(util)));
        }
    }
}
