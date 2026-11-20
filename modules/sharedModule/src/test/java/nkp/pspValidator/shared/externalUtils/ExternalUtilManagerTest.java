package nkp.pspValidator.shared.externalUtils;

import nkp.pspValidator.shared.OperatingSystem;
import nkp.pspValidator.shared.XmlUtils;
import nkp.pspValidator.shared.externalUtils.validation.BinaryFileProfile;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import static nkp.pspValidator.shared.externalUtils.ExternalUtilsTestSupport.*;
import static org.junit.Assert.*;

/**
 * Cela cesta vystupu nastroje bez nastroje: skutecna konfigurace externalUtils.xml (ExternalUtilManagerFactory),
 * sestaveni prikazu s ${UTIL_PATH} a ${TARGET_FILE}, spusteni procesu (CliCommand), vyber proudu (stdout/stderr),
 * regularni vyraz parseru a profil fDMF. Misto nastroju jsou v docasnem adresari falesne nastroje se stejnym nazvem,
 * ktere vypisi ulozeny vystup (src/test/resources/externalUtils) a zapisou, s jakymi argumenty byly spusteny
 * ({@link FakeExternalUtils}): na Linuxu a macOS skripty /bin/sh, na Windows davky .bat. Na Windows se proto
 * v konfiguraci nazvy programu .exe a bez pripony nahradi .bat (jhove.bat a verapdf.bat zustanou), viz
 * {@link FakeExternalUtils#toBatchCommands(String)}. Testovaci pripady i ocekavane hodnoty jsou na obou systemech
 * stejne, kde se lisi, je to u aserce vysvetleno. Konfigurace pro vsechny tri OS se testuje na kazdem systemu
 * (na Unixu se skript jmenuje treba "magick.exe").
 * EPUBCheck se spousti pres "java -jar ${UTIL_PATH}epubcheck.jar", misto nej je v docasnem adresari maly jar
 * prelozeny za behu testu (vyzaduje JDK a java v PATH, jinak se testy preskoci).
 */
public class ExternalUtilManagerTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private File fakeUtilsDir;

    @Before
    public void setUp() throws Exception {
        fakeUtilsDir = tmp.newFolder("fakeUtils");
    }

    /**
     * Spravce nastroju pro dany OS z konfigurace externalUtils.xml (na Windows s prikazy prevedenymi na davky).
     */
    private ExternalUtilManager manager(OperatingSystem os) throws Exception {
        File config = FakeExternalUtils.hostConfig(EXTERNAL_UTILS_CONFIG, tmp.getRoot());
        return new ExternalUtilManagerFactory(config).buildExternalUtilManager(os);
    }

    /**
     * Falesny nastroj programName (nazev programu z externalUtils.xml) v adresari dir, ktery vypise obsah souboru
     * na stdout (nebo stderr) a skonci s exitCode.
     */
    private static File fakeUtil(File dir, String programName, File stdout, File stderr, int exitCode) throws Exception {
        return FakeExternalUtils.create(dir, programName, stdout, stderr, exitCode);
    }

    /**
     * Spustitelny jar name v adresari dir: na stdout vypise obsah souboru stdout, argumenty zapise do args.txt vedle jaru
     * (ve tvaru [arg1][arg2]...). Prekladac a java v PATH musi byt k dispozici, jinak se test preskoci.
     */
    private static void fakeJar(File dir, String name, File stdout) throws Exception {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        Assume.assumeNotNull("chybí překladač Javy (JRE místo JDK)", compiler);
        Assume.assumeTrue("java není v PATH", javaInPath());
        File srcDir = Files.createTempDirectory(dir.toPath().getParent(), "fakejar").toFile();
        File src = new File(srcDir, "FakeUtil.java");
        String code = "import java.nio.file.*;\n"
                + "public class FakeUtil {\n"
                + "  public static void main(String[] args) throws Exception {\n"
                + "    Path dir = Paths.get(FakeUtil.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getParent();\n"
                + "    StringBuilder sb = new StringBuilder();\n"
                + "    for (String a : args) { sb.append('[').append(a).append(']'); }\n"
                + "    Files.write(dir.resolve(\"args.txt\"), sb.toString().getBytes(\"UTF-8\"));\n"
                + "    System.out.write(Files.readAllBytes(Paths.get(\"" + stdout.getAbsolutePath().replace("\\", "\\\\") + "\")));\n"
                + "    System.out.flush();\n"
                + "    System.exit(1);\n"
                + "  }\n"
                + "}\n";
        Files.write(src.toPath(), code.getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream compilerOut = new ByteArrayOutputStream();
        assertEquals(compilerOut.toString(), 0, compiler.run(null, compilerOut, compilerOut, "--release", "8", "-d", srcDir.getAbsolutePath(), src.getAbsolutePath()));
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        manifest.getMainAttributes().put(Attributes.Name.MAIN_CLASS, "FakeUtil");
        try (JarOutputStream jar = new JarOutputStream(new FileOutputStream(new File(dir, name)), manifest)) {
            jar.putNextEntry(new JarEntry("FakeUtil.class"));
            jar.write(Files.readAllBytes(new File(srcDir, "FakeUtil.class").toPath()));
            jar.closeEntry();
        }
    }

    private static boolean javaInPath() {
        try {
            return new CliCommand(new String[]{"java", "-version"}).execute().getExitValue() == 0;
        } catch (CliCommand.CliCommandException e) {
            return false;
        }
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
    public void versionDetectionWindows() throws Exception {
        // regularni vyrazy pro Windows: JHOVE jen "Rel. x.y.z", Kakadu jen "vX.Y", ImageMagick bez Q16-HDRI;
        // ImageMagick 7 na Windows je jeden program magick.exe s podprikazem convert
        fakeUtil(fakeUtilsDir, "jhove.bat", outputFile("jhove", "version.txt"), null, 0);
        fakeUtil(fakeUtilsDir, "kdu_expand.exe", outputFile("kakadu", "version.txt"), null, 0);
        fakeUtil(fakeUtilsDir, "magick.exe", outputFile("imageMagick", "version.txt"), null, 0);
        ExternalUtilManager manager = manager(OperatingSystem.WINDOWS);
        manager.setPath(ExternalUtil.JHOVE, fakeUtilsDir);
        manager.setPath(ExternalUtil.KAKADU, fakeUtilsDir);
        manager.setPath(ExternalUtil.IMAGE_MAGICK, fakeUtilsDir);
        assertEquals("Rel. 1.29.0", manager.runUtilVersionDetection(ExternalUtil.JHOVE));
        assertEquals("[--version]", FakeExternalUtils.recordedArgs(fakeUtilsDir, "jhove.bat"));
        assertEquals("v8.5", manager.runUtilVersionDetection(ExternalUtil.KAKADU));
        assertEquals("[-version]", FakeExternalUtils.recordedArgs(fakeUtilsDir, "kdu_expand.exe"));
        assertEquals("ImageMagick 7.1.2-24", manager.runUtilVersionDetection(ExternalUtil.IMAGE_MAGICK));
        assertEquals("[convert][-version]", FakeExternalUtils.recordedArgs(fakeUtilsDir, "magick.exe"));
    }

    @Test
    public void versionDetectionAudioUtilsOnEveryOs() throws Exception {
        // vystupy MP3val 0.1.8, shntool 3.0.10 a Checkmate 0.22 z Homebrew (macOS, 20. 11. 2026); shntool -v pise
        // na stderr, text na stdout se nesmi vzit
        fakeUtil(fakeUtilsDir, "mp3val", textFile("MP3val 0.1.8\n"), null, 0);
        fakeUtil(fakeUtilsDir, "shntool", textFile("shntool 9.9.9 na stdout se ignoruje\n"), outputFile("shntool", "version.txt"), 0);
        fakeUtil(fakeUtilsDir, "shntool.exe", textFile("shntool 9.9.9 na stdout se ignoruje\n"), outputFile("shntool", "version.txt"), 0);
        fakeUtil(fakeUtilsDir, "mpck", textFile("mpck 0.22\nCopyright (C) 2003-2026 Sjoerd Langkemper\n"), null, 0);
        fakeUtil(fakeUtilsDir, "mpck.exe", textFile("mpck 0.22\nCopyright (C) 2003-2026 Sjoerd Langkemper\n"), null, 0);
        for (OperatingSystem os : OperatingSystem.values()) {
            ExternalUtilManager manager = manager(os);
            manager.setPath(ExternalUtil.MP3VAL, fakeUtilsDir);
            manager.setPath(ExternalUtil.SHNTOOL, fakeUtilsDir);
            manager.setPath(ExternalUtil.CHECKMATE, fakeUtilsDir);
            assertEquals(os.name(), "MP3val 0.1.8", manager.runUtilVersionDetection(ExternalUtil.MP3VAL));
            assertEquals(os.name(), "shntool 3.0.10", manager.runUtilVersionDetection(ExternalUtil.SHNTOOL));
            assertEquals(os.name(), "mpck 0.22", manager.runUtilVersionDetection(ExternalUtil.CHECKMATE));
        }
    }

    @Test
    public void jpylyzerPathIsUsedOnAllOs() throws Exception {
        // dřív (do 3.0) chybel v externalUtils.xml u Jpylyzeru pro MAC (detekce verze i spusteni) a LINUX (detekce
        // verze) ${UTIL_PATH} a nastavena cesta se ignorovala (nalez 24), viz everyCommandContainsUtilPath
        fakeUtil(fakeUtilsDir, "jpylyzer", textFile("9.9.9-podvrh\n"), null, 0);
        fakeUtil(fakeUtilsDir, "jpylyzer.exe", textFile("9.9.9-podvrh\n"), null, 0);
        for (OperatingSystem os : OperatingSystem.values()) {
            ExternalUtilManager manager = manager(os);
            manager.setPath(ExternalUtil.JPYLYZER, fakeUtilsDir);
            assertEquals(os.name(), "9.9.9-podvrh", manager.runUtilVersionDetection(ExternalUtil.JPYLYZER));
            assertEquals(os.name(), "9.9.9-podvrh", manager.runUtilExecution(new ExternalUtilExecution("jp2k", ExternalUtil.JPYLYZER), new File("x.jp2")));
        }
    }

    @Test
    public void everyCommandContainsUtilPath() throws Exception {
        // kazda (nezakomentovana) polozka externalUtils.xml musi obsahovat ${UTIL_PATH}, jinak se cesta z GUI
        // nebo z --<nastroj>-path ignoruje (nalez 24)
        Document doc = XmlUtils.buildDocumentFromFile(EXTERNAL_UTILS_CONFIG, false);
        NodeList commands = doc.getElementsByTagName("command");
        int count = 0;
        for (int i = 0; i < commands.getLength(); i++) {
            Element command = (Element) commands.item(i);
            Element os = (Element) command.getParentNode();
            Element kind = (Element) os.getParentNode();
            Element util = (Element) kind.getParentNode();
            String where = util.getAttribute("name") + "/" + kind.getTagName() + kind.getAttribute("name") + "/" + os.getAttribute("name");
            assertTrue(where + ": " + command.getTextContent(), command.getTextContent().contains("${UTIL_PATH}"));
            count++;
        }
        // 9 nastroju, kazdy detekce verze a jedno spusteni na 3 OS, JHOVE dve spusteni (jp2k, wav)
        assertEquals(9 * 2 * 3 + 3, count);
    }

    @Test
    public void everyUtilHasVersionDetectionAndExecutionOnEveryOs() throws Exception {
        for (OperatingSystem os : OperatingSystem.values()) {
            ExternalUtilManager manager = manager(os);
            for (ExternalUtil util : ExternalUtil.values()) {
                assertTrue(os + "/" + util, manager.isVersionDetectionDefined(util));
            }
            assertTrue(os.name(), manager.isUtilExecutionDefined(new ExternalUtilExecution("pdf", ExternalUtil.VERAPDF)));
            assertTrue(os.name(), manager.isUtilExecutionDefined(new ExternalUtilExecution("epub", ExternalUtil.EPUBCHECK)));
        }
    }

    // veraPDF a EPUBCheck

    @Test
    public void veraPdfVersionDetection() throws Exception {
        // veraPDF 1.30 z Homebrew (formule 1.30.2) vypisuje "veraPDF 1.30.0", viz README.md v externalUtils
        fakeUtil(fakeUtilsDir, "verapdf", outputFile("verapdf", "version.txt"), null, 0);
        fakeUtil(fakeUtilsDir, "verapdf.bat", outputFile("verapdf", "version.txt"), null, 0);
        for (OperatingSystem os : OperatingSystem.values()) {
            ExternalUtilManager manager = manager(os);
            manager.setPath(ExternalUtil.VERAPDF, fakeUtilsDir);
            assertEquals(os.name(), "veraPDF 1.30.0", manager.runUtilVersionDetection(ExternalUtil.VERAPDF));
        }
    }

    @Test
    public void veraPdfReadsStdoutIgnoresExitCodeAndIsValidatedByProfile() throws Exception {
        // veraPDF konci u nevalidniho PDF kodem 1, u neparsovatelneho 7; varovani jde na stderr
        fakeUtil(fakeUtilsDir, "verapdf", outputFile("verapdf", "noncompliant_dummy.xml"), textFile("WARNING: stderr se ignoruje\n"), 1);
        ExternalUtilManager manager = manager(OperatingSystem.LINUX);
        manager.setPath(ExternalUtil.VERAPDF, fakeUtilsDir);
        String parsed = manager.runUtilExecution(new ExternalUtilExecution("pdf", ExternalUtil.VERAPDF), new File("dummy.pdf"));
        assertTrue(parsed.startsWith("<?xml"));
        assertTrue(parsed.endsWith("</report>"));
        List<String> errors = loadProfile(manager, "emonograph_3.0", "pdf", ExternalUtil.VERAPDF).validate("pdf", new File("dummy.pdf"));
        assertTrue(errors.toString(), errors.contains("Soubor musí být validní: hodnota \"false\" neodpovídá žádnému z omezení: [musí být přesně 'true']"));
    }

    @Test
    public void epubCheckRunsThroughJavaJar() throws Exception {
        File epubcheckDir = tmp.newFolder("epubcheck dir");
        fakeJar(epubcheckDir, "epubcheck.jar", outputFile("epubcheck", "version.txt"));
        ExternalUtilManager manager = manager(OperatingSystem.MAC);
        manager.setPath(ExternalUtil.EPUBCHECK, epubcheckDir);
        assertEquals("EPUBCheck v5.4.0", manager.runUtilVersionDetection(ExternalUtil.EPUBCHECK));

        // spusteni: argumenty --out - -q <soubor>, XML se cte ze stdout, exit kod (1 u chyb v EPUB) se ignoruje
        fakeJar(epubcheckDir, "epubcheck.jar", outputFile("epubcheck", "notwellformed_epub2_sample1.xml"));
        File target = new File(tmp.getRoot(), "a b.epub");
        String parsed = manager.runUtilExecution(new ExternalUtilExecution("epub", ExternalUtil.EPUBCHECK), target);
        assertTrue(parsed, parsed.startsWith("<?xml"));
        List<String> errors = loadProfile(manager, "emonograph_3.0", "epub", ExternalUtil.EPUBCHECK).validate("epub", target);
        assertEquals(Arrays.asList(
                "Soubor musí být validní: hodnota \"Not well-formed\" neodpovídá žádnému z omezení: [musí být přesně 'Well-formed']",
                "Detekce chybových hlášek: zakázaná hodnota nalezena: RSC-012, ERROR, [Fragment identifier is not defined.], toc.ncx (18-75)"
        ), errors);
        assertEquals("[--out][-][-q][" + target.getAbsolutePath() + "]", new String(Files.readAllBytes(new File(epubcheckDir, "args.txt").toPath()), StandardCharsets.UTF_8));
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
        // prazdny stdout, chyba na stderr a nenulovy navratovy kod (napr. java -jar s chybejicim jarem): hlaska uvede
        // prvni radek stderr a kod, aby uzivatel videl pricinu
        fakeUtil(fakeUtilsDir, "mp3val", textFile(""), textFile("Error: Unable to access jarfile x.jar\ndalsi radek\n"), 1);
        manager.setPath(ExternalUtil.MP3VAL, fakeUtilsDir);
        try {
            manager.runUtilVersionDetection(ExternalUtil.MP3VAL);
            fail();
        } catch (CliCommand.CliCommandException e) {
            assertEquals("prázdný výstup (stderr: Error: Unable to access jarfile x.jar, návratový kód 1)", e.getMessage());
        }
    }

    @Test
    public void missingUtilThrowsCliCommandException() throws Exception {
        ExternalUtilManager manager = manager(OperatingSystem.LINUX);
        manager.setPath(ExternalUtil.JHOVE, new File(fakeUtilsDir, "neexistuje"));
        String program = new File(fakeUtilsDir, "neexistuje").getAbsolutePath() + File.separator + FakeExternalUtils.hostFileName("jhove");
        try {
            manager.runUtilVersionDetection(ExternalUtil.JHOVE);
            fail();
        } catch (CliCommand.CliCommandException e) {
            assertMissingProgramMessage(program, e.getMessage());
        }
        try {
            manager.runUtilExecution(new ExternalUtilExecution("jp2k", ExternalUtil.JHOVE), new File("x.jp2"));
            fail();
        } catch (CliCommand.CliCommandException e) {
            assertMissingProgramMessage(program, e.getMessage());
        }
    }

    /**
     * Text vyjimky Runtime.exec, ktery CliCommand preda beze zmeny. Zacatek je na vsech systemech stejny, pricinu
     * dodava JDK podle systemu a lisi se: na Windows "CreateProcess error=2, " a text chyby ERROR_FILE_NOT_FOUND
     * v jazyce systemu (cesky "Systém nemůže nalézt uvedený soubor"), na Unixu podle zpusobu spousteni procesu
     * v JDK "Exec failed, error: 2 (No such file or directory)" (macOS, JDK 21, posix_spawn), jinak
     * "error=2, No such file or directory". Kontroluje se proto jen kod 2 (soubor nenalezen) v zapisu daneho systemu.
     */
    private static void assertMissingProgramMessage(String program, String message) {
        assertTrue(message, message.startsWith("Cannot run program \"" + program + "\": "));
        if (FakeExternalUtils.WINDOWS_HOST) {
            assertTrue(message, message.contains("\": CreateProcess error=2, "));
        } else {
            assertTrue(message, message.contains("\": error=2, ") || message.contains("\": Exec failed, error: 2 "));
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
        // ImageMagick identify pise na stderr, stdout se ignoruje
        fakeUtil(fakeUtilsDir, "identify", textFile("stdout se ignoruje\n"), textFile("identify: chyba na stderr\n"), 1);
        ExternalUtilManager manager = manager(OperatingSystem.LINUX);
        manager.setPath(ExternalUtil.IMAGE_MAGICK, fakeUtilsDir);
        File target = new File("src/test/tools/jp2/mc.jp2");
        assertEquals("identify: chyba na stderr",
                manager.runUtilExecution(new ExternalUtilExecution("jp2k", ExternalUtil.IMAGE_MAGICK), target));
        assertEquals("[-verbose][" + target.getAbsolutePath() + "]", FakeExternalUtils.recordedArgs(fakeUtilsDir, "identify"));
    }

    @Test
    public void targetFileWithSpaceStaysOneArgument() throws Exception {
        // sablona prikazu se rozdeli podle mezer a teprve potom se dosadi cesty, takze cesta s mezerou
        // zustane jednim argumentem (stejne pro ${UTIL_PATH}, viz utilPathWithSpaceWorks)
        fakeUtil(fakeUtilsDir, "identify", null, textFile("ok\n"), 0);
        ExternalUtilManager manager = manager(OperatingSystem.LINUX);
        manager.setPath(ExternalUtil.IMAGE_MAGICK, fakeUtilsDir);
        File target = new File(tmp.getRoot(), "a b.jp2");
        assertEquals("ok", manager.runUtilExecution(new ExternalUtilExecution("jp2k", ExternalUtil.IMAGE_MAGICK), target));
        assertEquals("[-verbose][" + target.getAbsolutePath() + "]", FakeExternalUtils.recordedArgs(fakeUtilsDir, "identify"));
    }

    @Test
    public void utilPathWithSpaceWorks() throws Exception {
        assertJhoveInDirWorksOnEveryOs(tmp.newFolder("Program Files", "JHOVE 1.34"));
    }

    @Test
    public void utilPathWithCzechDiacriticsWorks() throws Exception {
        // ceske znaky jsou v kodove strance Windows-1250 i v UTF-8, na zadnem systemu se nesmi rozbit
        assertJhoveInDirWorksOnEveryOs(tmp.newFolder("nástroje ěščřžýáíéůú", "Příliš žluťoučký kůň"));
    }

    /**
     * JHOVE (jhove, na Windows jhove.bat) v adresari dir: detekce verze a spusteni jp2k s cilovym souborem v adresari
     * s mezerou a diakritikou, pro konfiguraci vsech tri OS.
     */
    private void assertJhoveInDirWorksOnEveryOs(File dir) throws Exception {
        File target = new File(tmp.newFolder("Balíky test", "PSP ř"), "mc 0001 ěš.jp2");
        Map<OperatingSystem, String> expectedVersion = new EnumMap<>(OperatingSystem.class);
        expectedVersion.put(OperatingSystem.LINUX, "Rel. 1.29.0");
        expectedVersion.put(OperatingSystem.WINDOWS, "Rel. 1.29.0");
        expectedVersion.put(OperatingSystem.MAC, "Jhove (Rel. 1.29.0-SNAPSHOT, 2024-02-18)");
        for (OperatingSystem os : OperatingSystem.values()) {
            String program = os == OperatingSystem.WINDOWS ? "jhove.bat" : "jhove";
            fakeUtil(dir, program, outputFile("jhove", "version.txt"), null, 0);
            ExternalUtilManager manager = manager(os);
            manager.setPath(ExternalUtil.JHOVE, dir);
            assertEquals(os.name(), expectedVersion.get(os), manager.runUtilVersionDetection(ExternalUtil.JHOVE));
            assertEquals(os.name(), "[--version]", FakeExternalUtils.recordedArgs(dir, program));

            fakeUtil(dir, program, outputFile("jhove", "tools_mc.xml"), null, 0);
            String parsed = manager.runUtilExecution(new ExternalUtilExecution("jp2k", ExternalUtil.JHOVE), target);
            assertTrue(os.name(), parsed.startsWith("<?xml"));
            assertTrue(os.name(), parsed.endsWith("</jhove>"));
            assertEquals(os.name(), "[-h][XML][-m][jpeg2000-hul][-k][" + target.getAbsolutePath() + "]", FakeExternalUtils.recordedArgs(dir, program));
        }
    }

    // davky .bat na Windows (jhove.bat, verapdf.bat), na Unixu stejne pripady se skripty pojmenovanymi .bat

    @Test
    public void batchUtilsWithSpacedArgumentsFromSpacedPath() throws Exception {
        // davka v ceste s mezerou a s diakritikou, argument (cesta k souboru) s mezerami a diakritikou: cmd.exe
        // dostane prikazovy radek s uvozovkami kolem obou, argument musi do davky dojit jako jeden (issue #32
        // u JHOVE 1.14 na Windows)
        File jhoveDir = tmp.newFolder("Program Files", "JHOVE ř");
        File veraPdfDir = tmp.newFolder("Program Files", "veraPDF 1.30");
        File pspDir = tmp.newFolder("Balíky test", "e-monografie ěščř");
        File wav = new File(pspDir, "mca 1234567890 0001.wav");
        File pdf = new File(pspDir, "oc 1234567890 0001.pdf");
        ExternalUtilManager manager = manager(OperatingSystem.WINDOWS);
        manager.setPath(ExternalUtil.JHOVE, jhoveDir);
        manager.setPath(ExternalUtil.VERAPDF, veraPdfDir);

        fakeUtil(jhoveDir, "jhove.bat", outputFile("jhove", "wav_ok_24bit_96kHz.xml"), null, 0);
        String jhove = manager.runUtilExecution(new ExternalUtilExecution("wav", ExternalUtil.JHOVE), wav);
        assertEquals(output("jhove", "wav_ok_24bit_96kHz.xml").trim(), jhove);
        assertEquals("[-h][XML][-m][WAVE-hul][" + wav.getAbsolutePath() + "]", FakeExternalUtils.recordedArgs(jhoveDir, "jhove.bat"));

        // veraPDF konci u nevalidniho PDF kodem 1, stdout se presto vezme
        fakeUtil(veraPdfDir, "verapdf.bat", outputFile("verapdf", "noncompliant_dummy.xml"), textFile("WARNING: stderr se ignoruje\n"), 1);
        String veraPdf = manager.runUtilExecution(new ExternalUtilExecution("pdf", ExternalUtil.VERAPDF), pdf);
        assertEquals(output("verapdf", "noncompliant_dummy.xml").trim(), veraPdf);
        assertEquals("[" + pdf.getAbsolutePath() + "]", FakeExternalUtils.recordedArgs(veraPdfDir, "verapdf.bat"));
    }

    @Test
    public void batchGetsArgumentOutsideAnsiCodePageIntact() throws Exception {
        // recke lambda neni ve Windows-1250: nastroje, ktere ctou argumenty v kodove strance ANSI (launcher Javy,
        // shntool, MP3val), ho dostanou jako "?" (overeni na Windows 20. 11. 2026, kap. 6.1). Validator sam
        // (CreateProcess s Unicode) i cmd.exe ho predaji beze zmeny.
        File pdf = new File(tmp.newFolder("λ λ"), "λ.pdf");
        fakeUtil(fakeUtilsDir, "verapdf.bat", outputFile("verapdf", "noncompliant_dummy.xml"), null, 1);
        ExternalUtilManager manager = manager(OperatingSystem.WINDOWS);
        manager.setPath(ExternalUtil.VERAPDF, fakeUtilsDir);
        assertTrue(manager.runUtilExecution(new ExternalUtilExecution("pdf", ExternalUtil.VERAPDF), pdf).startsWith("<?xml"));
        assertEquals("[" + pdf.getAbsolutePath() + "]", FakeExternalUtils.recordedArgs(fakeUtilsDir, "verapdf.bat"));
    }

    @Test
    public void batchCommandsConversion() {
        // prevod prikazu pro falesne nastroje na Windows (FakeExternalUtils): meni se jen nazev programu za ${UTIL_PATH}
        assertEquals("<command>${UTIL_PATH}magick.bat convert -version</command>",
                FakeExternalUtils.toBatchCommands("<command>${UTIL_PATH}magick.exe convert -version</command>"));
        assertEquals("<command>${UTIL_PATH}jhove.bat -h XML -m jpeg2000-hul -k ${TARGET_FILE}</command>",
                FakeExternalUtils.toBatchCommands("<command>${UTIL_PATH}jhove.bat -h XML -m jpeg2000-hul -k ${TARGET_FILE}</command>"));
        assertEquals("<command>${UTIL_PATH}jhove.bat --version</command>",
                FakeExternalUtils.toBatchCommands("<command>${UTIL_PATH}jhove --version</command>"));
        assertEquals("<command>${UTIL_PATH}mp3val.bat ${TARGET_FILE}</command>",
                FakeExternalUtils.toBatchCommands("<command>${UTIL_PATH}mp3val ${TARGET_FILE}</command>"));
        assertEquals("<command>${UTIL_PATH}kdu_expand.bat -i ${TARGET_FILE}</command>",
                FakeExternalUtils.toBatchCommands("<command>${UTIL_PATH}kdu_expand.exe -i ${TARGET_FILE}</command>"));
        assertEquals("<command>java -jar ${UTIL_PATH}epubcheck.jar --version</command>",
                FakeExternalUtils.toBatchCommands("<command>java -jar ${UTIL_PATH}epubcheck.jar --version</command>"));
        assertEquals("<command>python ${UTIL_PATH}jpylyzer.py --version</command>",
                FakeExternalUtils.toBatchCommands("<command>python ${UTIL_PATH}jpylyzer.py --version</command>"));
    }

    @Test
    public void definitionsAndAvailability() throws Exception {
        ExternalUtilManager manager = manager(OperatingSystem.LINUX);
        for (ExternalUtil util : ExternalUtil.values()) {
            assertTrue(util.name(), manager.isVersionDetectionDefined(util));
            // výchozí stav: vsechny nastroje jsou "dostupne", dokud je nekdo neoznaci jinak; CLI (Main.detectImageTools)
            // i GUI (ExternalUtilsCheckDialogController) je po neuspesne detekci verze oznaci jako nedostupne
            // (setUtilAvailable(false)) a validace pak hlasi jednu INFO hlasku "nástroj ... není dostupný" (nalez 27)
            assertTrue(util.name(), manager.isUtilAvailable(util));
            assertFalse(util.name(), manager.isUtilDisabled(util));
        }
        assertTrue(manager.isUtilExecutionDefined(new ExternalUtilExecution("jp2k", ExternalUtil.JPYLYZER)));
        assertTrue(manager.isUtilExecutionDefined(new ExternalUtilExecution("wav", ExternalUtil.JHOVE)));
        assertTrue(manager.isUtilExecutionDefined(new ExternalUtilExecution("pdf", ExternalUtil.VERAPDF)));
        assertTrue(manager.isUtilExecutionDefined(new ExternalUtilExecution("epub", ExternalUtil.EPUBCHECK)));
        assertFalse(manager.isUtilExecutionDefined(new ExternalUtilExecution("wav", ExternalUtil.JPYLYZER)));
        assertFalse(manager.isUtilExecutionDefined(new ExternalUtilExecution("jp2k", ExternalUtil.VERAPDF)));
        manager.setUtilAvailable(ExternalUtil.KAKADU, false);
        assertFalse(manager.isUtilAvailable(ExternalUtil.KAKADU));
        manager.setUtilDisabled(ExternalUtil.KAKADU);
        assertTrue(manager.isUtilDisabled(ExternalUtil.KAKADU));
    }

    // CliCommand

    @Test
    public void cliCommandCapturesStreamsAndExitCode() throws Exception {
        File script = fakeUtil(fakeUtilsDir, "s", textFile("out1\nout2\n"), textFile("err\n"), 3);
        CliCommand command = new CliCommand(new String[]{script.getAbsolutePath()});
        CliCommand.Result result = command.execute();
        assertEquals(3, result.getExitValue());
        assertEquals("out1\nout2\n", result.getStdout());
        assertEquals("err\n", result.getStderr());
        assertTrue(command.toString().contains(script.getAbsolutePath()));
    }

    @Test
    public void cliCommandExitCodeZeroAndEmptyOutput() throws Exception {
        File script = fakeUtil(fakeUtilsDir, "s", null, null, 0);
        CliCommand.Result result = new CliCommand(new String[]{script.getAbsolutePath()}).execute();
        assertEquals(0, result.getExitValue());
        assertEquals("", result.getStdout());
        assertEquals("", result.getStderr());
    }

    @Test
    public void cliCommandWithoutTrailingNewlineAddsOne() throws Exception {
        File script = fakeUtil(fakeUtilsDir, "s", textFile("bez konce"), null, 0);
        assertEquals("bez konce\n", new CliCommand(new String[]{script.getAbsolutePath()}).execute().getStdout());
    }

    @Test
    public void cliCommandConvertsCrlfToLf() throws Exception {
        // nastroje na Windows pisi konce radku CRLF; CliCommand cte po radcich a kazdy ukonci LF, takze parsery
        // a profily vidi na vsech systemech stejny text
        File script = fakeUtil(fakeUtilsDir, "s", textFile("řádek 1\r\nřádek 2\r\n"), textFile("chyba\r\n"), 0);
        CliCommand.Result result = new CliCommand(new String[]{script.getAbsolutePath()}).execute();
        assertEquals("řádek 1\nřádek 2\n", result.getStdout());
        assertEquals("chyba\n", result.getStderr());
    }

    @Test(expected = CliCommand.CliCommandException.class)
    public void cliCommandMissingProgram() throws Exception {
        new CliCommand(new String[]{new File(fakeUtilsDir, "neexistuje").getAbsolutePath()}).execute();
    }
}
