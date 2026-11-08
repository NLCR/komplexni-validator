package nkp.pspValidator.shared.externalUtils;

import nkp.pspValidator.shared.OperatingSystem;
import nkp.pspValidator.shared.externalUtils.validation.BinaryFileProfile;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;

import static nkp.pspValidator.shared.externalUtils.ExternalUtilsTestSupport.*;
import static org.junit.Assert.*;

/**
 * Cela cesta vystupu nastroje bez nastroje: skutecna konfigurace externalUtils.xml (ExternalUtilManagerFactory),
 * sestaveni prikazu s ${UTIL_PATH} a ${TARGET_FILE}, spusteni procesu (CliCommand), vyber proudu (stdout/stderr),
 * regularni vyraz parseru a profil fDMF. Misto nastroju jsou v docasnem adresari shellove skripty se stejnym nazvem,
 * ktere vypisi ulozeny vystup (src/test/resources/externalUtils). Jen pro Linux a macOS (skripty /bin/sh).
 */
public class ExternalUtilManagerTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private File fakeUtilsDir;

    @Before
    public void setUp() throws Exception {
        Assume.assumeFalse("skripty /bin/sh, na Windows se nespouští", isWindows());
        fakeUtilsDir = tmp.newFolder("fakeUtils");
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().startsWith("windows");
    }

    private static ExternalUtilManager manager(OperatingSystem os) throws Exception {
        return new ExternalUtilManagerFactory(EXTERNAL_UTILS_CONFIG).buildExternalUtilManager(os);
    }

    /**
     * Skript name v adresari dir, ktery vypise obsah souboru na stdout (nebo stderr) a skonci s exitCode.
     */
    private static File fakeUtil(File dir, String name, File stdout, File stderr, int exitCode) throws Exception {
        StringBuilder script = new StringBuilder("#!/bin/sh\n");
        if (stdout != null) {
            script.append("cat '").append(stdout.getAbsolutePath()).append("'\n");
        }
        if (stderr != null) {
            script.append("cat '").append(stderr.getAbsolutePath()).append("' >&2\n");
        }
        script.append("exit ").append(exitCode).append('\n');
        return writeScript(dir, name, script.toString());
    }

    private static File writeScript(File dir, String name, String content) throws Exception {
        File file = new File(dir, name);
        Files.write(file.toPath(), content.getBytes(StandardCharsets.UTF_8));
        assertTrue(file.setExecutable(true));
        return file;
    }

    private File textFile(String content) throws Exception {
        File file = tmp.newFile();
        Files.write(file.toPath(), content.getBytes(StandardCharsets.UTF_8));
        return file;
    }

    // detekce verze, regularni vyrazy z externalUtils.xml nad realnymi vystupy --version

    @Test
    public void versionDetectionLinux() throws Exception {
        fakeUtil(fakeUtilsDir, "jhove", outputFile("jhove", "version.txt"), null, 0);
        fakeUtil(fakeUtilsDir, "kdu_expand", outputFile("kakadu", "version.txt"), null, 0);
        fakeUtil(fakeUtilsDir, "convert", outputFile("imageMagick", "version.txt"), null, 0);
        ExternalUtilManager manager = manager(OperatingSystem.LINUX);
        manager.setPath(ExternalUtil.JHOVE, fakeUtilsDir);
        manager.setPath(ExternalUtil.KAKADU, fakeUtilsDir);
        manager.setPath(ExternalUtil.IMAGE_MAGICK, fakeUtilsDir);
        assertEquals("Rel. 1.29.0", manager.runUtilVersionDetection(ExternalUtil.JHOVE));
        assertEquals("v8.5", manager.runUtilVersionDetection(ExternalUtil.KAKADU));
        assertEquals("ImageMagick 7.1.2-24", manager.runUtilVersionDetection(ExternalUtil.IMAGE_MAGICK));
    }

    @Test
    public void versionDetectionMac() throws Exception {
        fakeUtil(fakeUtilsDir, "jhove", outputFile("jhove", "version.txt"), null, 0);
        fakeUtil(fakeUtilsDir, "kdu_expand", outputFile("kakadu", "version.txt"), null, 0);
        fakeUtil(fakeUtilsDir, "convert", outputFile("imageMagick", "version.txt"), null, 0);
        ExternalUtilManager manager = manager(OperatingSystem.MAC);
        manager.setPath(ExternalUtil.JHOVE, fakeUtilsDir);
        manager.setPath(ExternalUtil.KAKADU, fakeUtilsDir);
        manager.setPath(ExternalUtil.IMAGE_MAGICK, fakeUtilsDir);
        assertEquals("Jhove (Rel. 1.29.0-SNAPSHOT, 2024-02-18)", manager.runUtilVersionDetection(ExternalUtil.JHOVE));
        assertEquals("Current core system version is v8.5", manager.runUtilVersionDetection(ExternalUtil.KAKADU));
        assertEquals("ImageMagick 7.1.2-24 Q16-HDRI", manager.runUtilVersionDetection(ExternalUtil.IMAGE_MAGICK));
    }

    @Test
    public void jpylyzerPathIsIgnoredOnMac() throws Exception {
        // známá vada: v externalUtils.xml pro MAC (detekce verze i spusteni) a pro LINUX (detekce verze) chybi
        // ${UTIL_PATH} u Jpylyzeru; nastavena cesta se ignoruje a spusti se "jpylyzer" z PATH (nebo nic)
        fakeUtil(fakeUtilsDir, "jpylyzer", textFile("9.9.9-podvrh\n"), null, 0);
        for (OperatingSystem os : new OperatingSystem[]{OperatingSystem.MAC, OperatingSystem.LINUX}) {
            ExternalUtilManager manager = manager(os);
            manager.setPath(ExternalUtil.JPYLYZER, fakeUtilsDir);
            try {
                assertNotEquals(os.name(), "9.9.9-podvrh", manager.runUtilVersionDetection(ExternalUtil.JPYLYZER));
            } catch (CliCommand.CliCommandException e) {
                // jpylyzer neni v PATH
            }
        }
        // spusteni na Linuxu cestu pouziva
        ExternalUtilManager linux = manager(OperatingSystem.LINUX);
        linux.setPath(ExternalUtil.JPYLYZER, fakeUtilsDir);
        assertEquals("9.9.9-podvrh", linux.runUtilExecution(new ExternalUtilExecution("jp2k", ExternalUtil.JPYLYZER), new File("x.jp2")));
    }

    @Test
    public void versionDetectionEmptyOrUnknownOutput() throws Exception {
        fakeUtil(fakeUtilsDir, "jhove", textFile(""), null, 0);
        fakeUtil(fakeUtilsDir, "kdu_expand", textFile("neznámý výstup\n"), null, 0);
        ExternalUtilManager manager = manager(OperatingSystem.LINUX);
        manager.setPath(ExternalUtil.JHOVE, fakeUtilsDir);
        manager.setPath(ExternalUtil.KAKADU, fakeUtilsDir);
        try {
            manager.runUtilVersionDetection(ExternalUtil.JHOVE);
            fail();
        } catch (CliCommand.CliCommandException e) {
            assertEquals("prázdný výstup", e.getMessage());
        }
        try {
            manager.runUtilVersionDetection(ExternalUtil.KAKADU);
            fail();
        } catch (CliCommand.CliCommandException e) {
            assertEquals("verze nenalezena ve výstupu: neznámý výstup\n", e.getMessage());
        }
    }

    @Test
    public void missingUtilThrowsCliCommandException() throws Exception {
        ExternalUtilManager manager = manager(OperatingSystem.LINUX);
        manager.setPath(ExternalUtil.JHOVE, new File(fakeUtilsDir, "neexistuje"));
        try {
            manager.runUtilVersionDetection(ExternalUtil.JHOVE);
            fail();
        } catch (CliCommand.CliCommandException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("neexistuje/jhove"));
        }
        try {
            manager.runUtilExecution(new ExternalUtilExecution("jp2k", ExternalUtil.JHOVE), new File("x.jp2"));
            fail();
        } catch (CliCommand.CliCommandException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("neexistuje/jhove"));
        }
    }

    // spusteni a parser vystupu

    @Test
    public void jhoveXmlIsCutFromStdoutAndValidatedByProfile() throws Exception {
        // JHOVE muze pred XML vypsat jine radky (napr. varovani JVM); parser (?s)<\?xml.*$ je odrizne
        File stdout = textFile("WARNING: An illegal reflective access operation has occurred\n" + output("jhove", "tools_mc.xml"));
        fakeUtil(fakeUtilsDir, "jhove", stdout, textFile("stderr se ignoruje\n"), 0);
        ExternalUtilManager manager = manager(OperatingSystem.LINUX);
        manager.setPath(ExternalUtil.JHOVE, fakeUtilsDir);
        String parsed = manager.runUtilExecution(new ExternalUtilExecution("jp2k", ExternalUtil.JHOVE), new File("src/test/tools/jp2/mc.jp2"));
        assertTrue(parsed.startsWith("<?xml"));
        assertTrue(parsed.endsWith("</jhove>"));

        BinaryFileProfile profile = loadProfile(manager, "fund_unit_0.2", "mc", ExternalUtil.JHOVE);
        assertEquals(Collections.emptyList(), profile.validate("jp2k", new File("src/test/tools/jp2/mc.jp2")));
    }

    @Test
    public void jhoveWithoutXmlGivesEmptyOutput() throws Exception {
        fakeUtil(fakeUtilsDir, "jhove", textFile("Exception in thread \"main\"\n"), null, 1);
        ExternalUtilManager manager = manager(OperatingSystem.MAC);
        manager.setPath(ExternalUtil.JHOVE, fakeUtilsDir);
        assertEquals("", manager.runUtilExecution(new ExternalUtilExecution("jp2k", ExternalUtil.JHOVE), new File("x.jp2")));
        // prazdny vystup pak profil hlasi jako chybu parsovani XML
        List<String> errors = loadProfile(manager, "fund_unit_0.2", "mc", ExternalUtil.JHOVE).validate("jp2k", new File("x.jp2"));
        assertEquals(1, errors.size());
        assertTrue(errors.get(0), errors.get(0).startsWith("chyba parsování xml (výstup nástroje JHOVE)"));
    }

    @Test
    public void kakaduReadsStderrAndIgnoresExitCode() throws Exception {
        // kdu_expand pri chybe konci nenulovym kodem (realne 255), rozhoduje jen text na stderr
        fakeUtil(fakeUtilsDir, "kdu_expand", textFile("stdout se ignoruje\n"), outputFile("kakadu", "detekce_poskozeni_w0352gUC.txt"), 255);
        ExternalUtilManager manager = manager(OperatingSystem.MAC);
        manager.setPath(ExternalUtil.KAKADU, fakeUtilsDir);
        String parsed = manager.runUtilExecution(new ExternalUtilExecution("jp2k", ExternalUtil.KAKADU), new File("x.jp2"));
        assertEquals("Kakadu Core Error:\nExpected to find EPH marker following packet header.  Found 0x0 instead.", parsed);
        assertEquals(1, loadProfile(manager, "fund_unit_0.2", "uc", ExternalUtil.KAKADU).validate("jp2k", new File("x.jp2")).size());
    }

    @Test
    public void jpylyzerOnLinuxThroughProfile() throws Exception {
        fakeUtil(fakeUtilsDir, "jpylyzer", outputFile("jpylyzer", "ok_uc_UC1.xml"), null, 0);
        ExternalUtilManager manager = manager(OperatingSystem.LINUX);
        manager.setPath(ExternalUtil.JPYLYZER, fakeUtilsDir);
        List<String> errors = loadProfile(manager, "fund_unit_0.2", "uc", ExternalUtil.JPYLYZER).validate("jp2k", new File("x.jp2"));
        assertEquals(1, errors.size());
        assertTrue(errors.get(0), errors.get(0).startsWith("Kompresní poměr: hodnota \"2.67\""));
    }

    @Test
    public void targetFileIsPassedAsAbsolutePath() throws Exception {
        writeScript(fakeUtilsDir, "identify", "#!/bin/sh\nfor a in \"$@\"; do printf '[%s]' \"$a\" >&2; done\n");
        ExternalUtilManager manager = manager(OperatingSystem.LINUX);
        manager.setPath(ExternalUtil.IMAGE_MAGICK, fakeUtilsDir);
        File target = new File("src/test/tools/jp2/mc.jp2");
        assertEquals("[-verbose][" + target.getAbsolutePath() + "]",
                manager.runUtilExecution(new ExternalUtilExecution("jp2k", ExternalUtil.IMAGE_MAGICK), target));
    }

    @Test
    public void targetFileWithSpaceStaysOneArgument() throws Exception {
        // sablona prikazu se rozdeli podle mezer a teprve potom se dosadi cesty, takze cesta s mezerou
        // zustane jednim argumentem (stejne pro ${UTIL_PATH}, viz utilPathWithSpaceWorks)
        writeScript(fakeUtilsDir, "identify", "#!/bin/sh\nfor a in \"$@\"; do printf '[%s]' \"$a\" >&2; done\n");
        ExternalUtilManager manager = manager(OperatingSystem.LINUX);
        manager.setPath(ExternalUtil.IMAGE_MAGICK, fakeUtilsDir);
        File target = new File(tmp.getRoot(), "a b.jp2");
        assertEquals("[-verbose][" + target.getAbsolutePath() + "]",
                manager.runUtilExecution(new ExternalUtilExecution("jp2k", ExternalUtil.IMAGE_MAGICK), target));
    }

    @Test
    public void utilPathWithSpaceWorks() throws Exception {
        File dirWithSpace = tmp.newFolder("Program Files");
        fakeUtil(dirWithSpace, "jhove", outputFile("jhove", "version.txt"), null, 0);
        ExternalUtilManager manager = manager(OperatingSystem.LINUX);
        manager.setPath(ExternalUtil.JHOVE, dirWithSpace);
        assertEquals("Rel. 1.29.0", manager.runUtilVersionDetection(ExternalUtil.JHOVE));
    }

    @Test
    public void definitionsAndAvailability() throws Exception {
        ExternalUtilManager manager = manager(OperatingSystem.LINUX);
        for (ExternalUtil util : ExternalUtil.values()) {
            assertTrue(util.name(), manager.isVersionDetectionDefined(util));
            // výchozí stav: vsechny nastroje jsou "dostupne", dokud je nekdo neoznaci jinak
            assertTrue(util.name(), manager.isUtilAvailable(util));
            assertFalse(util.name(), manager.isUtilDisabled(util));
        }
        assertTrue(manager.isUtilExecutionDefined(new ExternalUtilExecution("jp2k", ExternalUtil.JPYLYZER)));
        assertTrue(manager.isUtilExecutionDefined(new ExternalUtilExecution("wav", ExternalUtil.JHOVE)));
        assertFalse(manager.isUtilExecutionDefined(new ExternalUtilExecution("wav", ExternalUtil.JPYLYZER)));
        manager.setUtilAvailable(ExternalUtil.KAKADU, false);
        assertFalse(manager.isUtilAvailable(ExternalUtil.KAKADU));
        manager.setUtilDisabled(ExternalUtil.KAKADU);
        assertTrue(manager.isUtilDisabled(ExternalUtil.KAKADU));
    }

    // CliCommand

    @Test
    public void cliCommandCapturesStreamsAndExitCode() throws Exception {
        File script = writeScript(fakeUtilsDir, "s", "#!/bin/sh\necho out1\necho out2\necho err >&2\nexit 3\n");
        CliCommand command = new CliCommand(new String[]{script.getAbsolutePath()});
        CliCommand.Result result = command.execute();
        assertEquals(3, result.getExitValue());
        assertEquals("out1\nout2\n", result.getStdout());
        assertEquals("err\n", result.getStderr());
        assertTrue(command.toString().contains(script.getAbsolutePath()));
    }

    @Test
    public void cliCommandWithoutTrailingNewlineAddsOne() throws Exception {
        File script = writeScript(fakeUtilsDir, "s", "#!/bin/sh\nprintf 'bez konce'\n");
        assertEquals("bez konce\n", new CliCommand(new String[]{script.getAbsolutePath()}).execute().getStdout());
    }

    @Test(expected = CliCommand.CliCommandException.class)
    public void cliCommandMissingProgram() throws Exception {
        new CliCommand(new String[]{new File(fakeUtilsDir, "neexistuje").getAbsolutePath()}).execute();
    }
}
