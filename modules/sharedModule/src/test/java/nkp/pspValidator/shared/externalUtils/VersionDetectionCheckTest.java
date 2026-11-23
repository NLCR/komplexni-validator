package nkp.pspValidator.shared.externalUtils;

import nkp.pspValidator.shared.OperatingSystem;
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
 * Celá cesta od výstupu nástroje k ověření verze: skutečná externalUtils.xml (příkaz, proud a regulární výraz
 * detekce verze pro každý OS, ověřené řady), falešné nástroje ({@link FakeExternalUtils}, na Windows dávky .bat)
 * s výstupy verzí, které nástroje vypsaly při ověření 20. 11. 2026 na macOS a na Windows. EPUBCheck (java -jar)
 * je v {@link ExternalUtilManagerTest#epubCheckRunsThroughJavaJar}.
 */
public class VersionDetectionCheckTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private static final String IM_MAC = "Version: ImageMagick 7.1.2-35 Q16-HDRI aarch64 acaf7e418:20261031 https://imagemagick.org\n"
            + "Copyright: (C) 1999 ImageMagick Studio LLC\n";
    private static final String IM_WINDOWS = "Version: ImageMagick 7.1.2-35 Q16-HDRI x64 5d1bcc5:20261031 https://imagemagick.org\n"
            + "Copyright: (C) 1999 ImageMagick Studio LLC\n";
    private static final String JHOVE_134 = "Jhove (Rel. 1.34.0, 2025-07-02)\n Date: 2026-11-23 14:42:26 SEČ\n";
    private static final String VERAPDF_MAC = "veraPDF 1.30.0\nBuilt: Wed Jun 03 13:47:00 CEST 2026\n";
    private static final String VERAPDF_WINDOWS = "veraPDF 1.30.4\nBuilt: Mon Nov 09 13:40:00 CET 2026\n";

    private File text(String content) throws Exception {
        File file = tmp.newFile();
        Files.write(file.toPath(), content.getBytes(StandardCharsets.UTF_8));
        return file;
    }

    /** Falešné nástroje (bez EPUBChecku) s názvy programů z konfigurace daného OS v adresáři dir. */
    private void fakeTools(File dir, OperatingSystem os) throws Exception {
        boolean win = os == OperatingSystem.WINDOWS;
        FakeExternalUtils.create(dir, win ? "jpylyzer.exe" : "jpylyzer", text("2.2.1\n"), null, 0);
        FakeExternalUtils.create(dir, win ? "magick.exe" : "convert", text(win ? IM_WINDOWS : IM_MAC), null, 0);
        FakeExternalUtils.create(dir, win ? "jhove.bat" : "jhove", text(JHOVE_134), null, 0);
        FakeExternalUtils.create(dir, win ? "kdu_expand.exe" : "kdu_expand", ExternalUtilsTestSupport.outputFile("kakadu", "version.txt"), null, 0);
        FakeExternalUtils.create(dir, "mp3val", text("MP3val 0.1.8\n"), null, 0);
        FakeExternalUtils.create(dir, win ? "shntool.exe" : "shntool", null, text("shntool 3.0.10\n"), 0);
        FakeExternalUtils.create(dir, win ? "mpck.exe" : "mpck", text("mpck 0.22\n"), null, 0);
        FakeExternalUtils.create(dir, win ? "verapdf.bat" : "verapdf", text(win ? VERAPDF_WINDOWS : VERAPDF_MAC), null, 0);
    }

    private ExternalUtilManager managerWithFakes(OperatingSystem os) throws Exception {
        File dir = tmp.newFolder("nastroje-" + os.name());
        fakeTools(dir, os);
        File config = FakeExternalUtils.hostConfig(ExternalUtilsTestSupport.EXTERNAL_UTILS_CONFIG, tmp.getRoot());
        ExternalUtilManager manager = new ExternalUtilManagerFactory(config).buildExternalUtilManager(os);
        for (ExternalUtil util : ExternalUtil.values()) {
            manager.setPath(util, dir);
        }
        return manager;
    }

    private static void assertDetection(ExternalUtilManager manager, ExternalUtil util, String expectedText, String expectedVersion,
                                        VersionCheck.Status expectedStatus) throws Exception {
        String detected = manager.runUtilVersionDetection(util);
        assertEquals(util.name(), expectedText, detected);
        VersionCheck check = manager.checkVersion(util, detected);
        assertEquals(util.name(), expectedVersion, check.getVersion());
        assertEquals(util.name(), expectedStatus, check.getStatus());
    }

    @Test
    public void macVersionsAreVerified() throws Exception {
        ExternalUtilManager manager = managerWithFakes(OperatingSystem.MAC);
        VersionCheck.Status ok = VersionCheck.Status.VERIFIED;
        assertDetection(manager, JPYLYZER, "2.2.1", "2.2.1", ok);
        assertDetection(manager, IMAGE_MAGICK, "ImageMagick 7.1.2-35 Q16-HDRI", "7.1.2", ok);
        assertDetection(manager, JHOVE, "Jhove (Rel. 1.34.0, 2025-07-02)", "1.34.0", ok);
        assertDetection(manager, KAKADU, "Current core system version is v8.5", "8.5", ok);
        assertDetection(manager, MP3VAL, "MP3val 0.1.8", "0.1.8", ok);
        assertDetection(manager, SHNTOOL, "shntool 3.0.10", "3.0.10", ok);
        assertDetection(manager, CHECKMATE, "mpck 0.22", "0.22", ok);
        // Homebrew veraPDF 1.30.2 vypise 1.30.0, rada 1.30 plati
        assertDetection(manager, VERAPDF, "veraPDF 1.30.0", "1.30.0", ok);
    }

    @Test
    public void windowsVersionsAreVerifiedExceptKakadu() throws Exception {
        ExternalUtilManager manager = managerWithFakes(OperatingSystem.WINDOWS);
        VersionCheck.Status ok = VersionCheck.Status.VERIFIED;
        assertDetection(manager, JPYLYZER, "2.2.1", "2.2.1", ok);
        assertDetection(manager, IMAGE_MAGICK, "ImageMagick 7.1.2-35", "7.1.2", ok);
        assertDetection(manager, JHOVE, "Rel. 1.34.0", "1.34.0", ok);
        // Kakadu na Windows nikdo neoveril: kazda verze je neoverena
        assertDetection(manager, KAKADU, "v8.5", "8.5", VersionCheck.Status.UNVERIFIED);
        assertDetection(manager, MP3VAL, "MP3val 0.1.8", "0.1.8", ok);
        assertDetection(manager, SHNTOOL, "shntool 3.0.10", "3.0.10", ok);
        assertDetection(manager, CHECKMATE, "mpck 0.22", "0.22", ok);
        assertDetection(manager, VERAPDF, "veraPDF 1.30.4", "1.30.4", ok);
    }

    @Test
    public void linuxVersionsAreVerifiedOnMac() throws Exception {
        ExternalUtilManager manager = managerWithFakes(OperatingSystem.LINUX);
        VersionCheck.Status mac = VersionCheck.Status.VERIFIED_ON_OTHER_PLATFORM;
        assertDetection(manager, JPYLYZER, "2.2.1", "2.2.1", mac);
        assertDetection(manager, IMAGE_MAGICK, "ImageMagick 7.1.2-35", "7.1.2", mac);
        assertDetection(manager, JHOVE, "Rel. 1.34.0", "1.34.0", mac);
        assertDetection(manager, KAKADU, "v8.5", "8.5", mac);
        assertDetection(manager, MP3VAL, "MP3val 0.1.8", "0.1.8", mac);
        assertDetection(manager, SHNTOOL, "shntool 3.0.10", "3.0.10", mac);
        assertDetection(manager, CHECKMATE, "mpck 0.22", "0.22", mac);
        assertDetection(manager, VERAPDF, "veraPDF 1.30.0", "1.30.0", mac);
    }

    @Test
    public void jhoveSnapshotOnEveryOs() throws Exception {
        // 1.29.0-SNAPSHOT: macOS rada 1.29 overena; Windows overil jen 1.34; Linux prebira macOS. Regexp pro Linux
        // a Windows -SNAPSHOT odrizne, na verzi to nic nemeni.
        Map<OperatingSystem, String> text = new EnumMap<>(OperatingSystem.class);
        text.put(OperatingSystem.MAC, "Jhove (Rel. 1.29.0-SNAPSHOT, 2024-02-18)");
        text.put(OperatingSystem.WINDOWS, "Rel. 1.29.0");
        text.put(OperatingSystem.LINUX, "Rel. 1.29.0");
        Map<OperatingSystem, VersionCheck.Status> status = new EnumMap<>(OperatingSystem.class);
        status.put(OperatingSystem.MAC, VersionCheck.Status.VERIFIED);
        status.put(OperatingSystem.WINDOWS, VersionCheck.Status.UNVERIFIED);
        status.put(OperatingSystem.LINUX, VersionCheck.Status.VERIFIED_ON_OTHER_PLATFORM);
        for (OperatingSystem os : OperatingSystem.values()) {
            ExternalUtilManager manager = managerWithFakes(os);
            File dir = tmp.newFolder("jhove129-" + os.name());
            FakeExternalUtils.create(dir, os == OperatingSystem.WINDOWS ? "jhove.bat" : "jhove",
                    ExternalUtilsTestSupport.outputFile("jhove", "version.txt"), null, 0);
            manager.setPath(JHOVE, dir);
            assertDetection(manager, JHOVE, text.get(os), "1.29.0", status.get(os));
        }
    }

    @Test
    public void newerJhoveIsUnverifiedWithMessage() throws Exception {
        ExternalUtilManager manager = managerWithFakes(OperatingSystem.MAC);
        File dir = tmp.newFolder("jhove136");
        FakeExternalUtils.create(dir, "jhove", text("Jhove (Rel. 1.36.0, 2026-01-15)\n"), null, 0);
        manager.setPath(JHOVE, dir);
        String detected = manager.runUtilVersionDetection(JHOVE);
        manager.setUtilVersion(JHOVE, detected);
        VersionCheck check = manager.getVersionCheck(JHOVE);
        assertEquals(VersionCheck.Status.UNVERIFIED, check.getStatus());
        assertEquals("nástroj JHOVE je ve verzi 1.36.0, která nebyla s validátorem ověřena (ověřené řady: 1.29, 1.34); "
                + "výsledky kontroly se mohou lišit", check.protocolMessage(JHOVE.getUserFriendlyName()));
    }

    @Test
    public void detectionDoesNotStoreVersion() throws Exception {
        // verzi zapisuje az volajici (CLI, GUI ve vlakne UI), detekce sama ne
        ExternalUtilManager manager = managerWithFakes(OperatingSystem.MAC);
        manager.runUtilVersionDetection(JHOVE);
        assertNull(manager.getUtilVersion(JHOVE));
        assertNull(manager.getVersionCheck(JHOVE));
    }
}
