package nkp.pspValidator.shared.engine;

import nkp.pspValidator.shared.Dmf;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.*;

/**
 * Golden testy textoveho a XML protokolu validace balicku (Validator.run, ValidatorProtocolTextBuilder, ValidatorProtocolXmlBuilder).
 * <p>
 * Kazdy balicek se validuje jednou (vynucena fDMF, externi nastroje vypnute, Resolver offline, verbosity 3, XML protokol do
 * TemporaryFolder). Textovy log i XML protokol se po normalizaci porovnaji se soubory v src/test/resources/golden/
 * (&lt;nazev&gt;.txt a &lt;nazev&gt;.xml). XML se navic overuje proti validationProtocol.xsd a strukturne proti textu
 * (namespace, pocet sekci a pravidel, verdikt, celkovy pocet problemu).
 * <p>
 * Normalizace (viz normalizeText, normalizeXml): absolutni cesty (docasny adresar XML protokolu, kmenovy adresar balicku,
 * adresar modulu, koren repa) se nahradi zastupnymi retezci [TMP], [BALIK], [MODUL], [REPO]; verze
 * validatoru a datum buildu (paticka textu, element validator) se nahradi [VERZE] a [BUILD_DATE]; v XML casy
 * startTime/finishTime ([CAS]) a delky durationMs ([MS]); ve Windows oddelovace cest za zastupnym retezcem na /; konce radku na \n.
 * Zastupne retezce jsou v hranatych zavorkach, aby golden XML zustalo dobre formovane. Namespace a schemaLocation XML protokolu obsahuji verzi
 * (3.0) a normalizovane nejsou: zmena verze protokolu je zamerna a golden soubory se pri ni pregeneruji.
 * <p>
 * Pregenerovani golden souboru (po zamerne zmene hlasek, pravidel nebo formatu protokolu):
 * <pre>
 * ./gradlew :modules:sharedModule:test --tests '*ProtocolGoldenTest' -Dkv.updateGolden=true
 * </pre>
 * Vlastnost kv.updateGolden predava Gradle do testovaci JVM (sharedModule/build.gradle); misto ni lze nastavit promennou
 * prostredi KV_UPDATE_GOLDEN=true (napr. pri spusteni z IDE). V rezimu pregenerovani testy soubory zapisou a projdou;
 * zmeny je pak treba projit v git diff.
 * <p>
 * Balicek muze byt i odvozeny za behu (sloupec derive): audio-gram-0.5-no-ocr vznikne z audio_gram_0.3/1234567890 kopii do
 * TemporaryFolder, prejmenovanim adresaru na nazvy podle DMF 0.4+ (amdsec, mastercopy, usercopy, usercopy_audio) a smazanim
 * adresaru alto a txt; hlida, ze protokol gramodesek 0.4/0.5 bez OCR nehlasi chybejici OCR (18 MB balicek se nekopiruje do repa).
 */
@RunWith(Parameterized.class)
public class ProtocolGoldenTest {

    static final String PROTOCOL_NS = "http://www.nkp.cz/pspValidator/3.0/validationProtocol";
    static final File GOLDEN_DIR = new File("src/test/resources/golden");
    static final File PROTOCOL_XSD = new File(FdmfTestSupport.VALIDATOR_CONFIG_DIR, "xsd/validationProtocol.xsd");

    @ClassRule
    public static final TemporaryFolder TMP = new TemporaryFolder();

    /**
     * Vysledky behu podle nazvu (kazdy balicek se validuje jen jednou pro vsechny testy teto tridy).
     */
    private static final Map<String, Result> RESULTS = new HashMap<>();

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> packages() {
        return Arrays.asList(new Object[][]{
                {"fund-unit-0.2-valid-clipping", "fund_unit_0.2/valid_clipping/nk-00027x", new Dmf(Dmf.Type.FUND_UNIT, "0.2"), null},
                {"fund-unit-0.2-page-no-genre", "fund_unit_0.2/page_no_genre/nk-00027r", new Dmf(Dmf.Type.FUND_UNIT, "0.2"), null},
                {"monograph-1.3.2", "monograph_1.3.2/b50eb6b0-f0a4-11e3-b72e-005056827e52", new Dmf(Dmf.Type.MONOGRAPH, "1.3.2"), null},
                {"audio-gram-0.5-no-ocr", "audio_gram_0.3/1234567890", new Dmf(Dmf.Type.AUDIO_GRAM, "0.5"), DERIVE_AUDIO_NO_OCR},
        });
    }

    /**
     * Odvozeni balicku za behu: ze zdrojoveho adresare v src/test/resources vytvori balicek v docasnem adresari.
     */
    interface Derivation {
        File derive(File source, File tmpRoot) throws IOException;
    }

    static final Derivation DERIVE_AUDIO_NO_OCR = (source, tmpRoot) -> {
        File target = new File(tmpRoot, source.getName());
        copyDir(source.toPath(), target.toPath());
        for (String[] rename : new String[][]{{"amd_sec", "amdsec"}, {"master_copy", "mastercopy"}, {"user_copy", "usercopy"}, {"user_copy_audio", "usercopy_audio"}}) {
            Files.move(new File(target, rename[0]).toPath(), new File(target, rename[1]).toPath());
        }
        deleteDir(new File(target, "alto").toPath());
        deleteDir(new File(target, "txt").toPath());
        // md5 a itemlist v INFO: cesty s / (puvodni balicek ma \\, coz by v hlaskach dalo cesty zavisle na OS),
        // prejmenovane adresare, bez OCR souboru (jinak by hlasky obsahovaly text chyby OS "No such file or directory")
        for (String name : new String[]{"md5_1234567890.md5", "info_1234567890.xml"}) {
            File f = new File(target, name);
            String content = new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
            StringBuilder out = new StringBuilder();
            for (String line : content.split("(?<=\\n)")) {
                if (line.contains("\\alto\\") || line.contains("\\txt\\")) {
                    continue;
                }
                line = line.replace('\\', '/')
                        .replace("/amd_sec/", "/amdsec/").replace("/master_copy/", "/mastercopy/")
                        .replace("/user_copy_audio/", "/usercopy_audio/").replace("/user_copy/", "/usercopy/")
                        .replace("itemtotal=\"19\"", "itemtotal=\"15\"");
                out.append(line);
            }
            Files.write(f.toPath(), out.toString().getBytes(StandardCharsets.UTF_8));
        }
        return target;
    };

    private static void copyDir(java.nio.file.Path src, java.nio.file.Path dst) throws IOException {
        try (java.util.stream.Stream<java.nio.file.Path> paths = Files.walk(src)) {
            for (java.nio.file.Path p : (Iterable<java.nio.file.Path>) paths::iterator) {
                java.nio.file.Path t = dst.resolve(src.relativize(p).toString());
                if (Files.isDirectory(p)) {
                    Files.createDirectories(t);
                } else {
                    Files.copy(p, t);
                }
            }
        }
    }

    private static void deleteDir(java.nio.file.Path dir) throws IOException {
        try (java.util.stream.Stream<java.nio.file.Path> paths = Files.walk(dir)) {
            for (java.nio.file.Path p : (Iterable<java.nio.file.Path>) paths.sorted(Comparator.reverseOrder())::iterator) {
                Files.delete(p);
            }
        }
    }

    private final String name;
    private final String packagePath;
    private final Dmf dmf;
    private final Derivation derivation;

    public ProtocolGoldenTest(String name, String packagePath, Dmf dmf, Derivation derivation) {
        this.name = name;
        this.packagePath = packagePath;
        this.dmf = dmf;
        this.derivation = derivation;
    }

    private static final class Result {
        final String text;
        final String xml;
        final File xmlFile;

        Result(String text, String xml, File xmlFile) {
            this.text = text;
            this.xml = xml;
            this.xmlFile = xmlFile;
        }
    }

    private Result result() throws Exception {
        synchronized (RESULTS) {
            Result result = RESULTS.get(name);
            if (result == null) {
                File packageDir = new File("src/test/resources", packagePath).getAbsoluteFile();
                assertTrue("chybi testovaci balicek " + packageDir, packageDir.isDirectory());
                if (derivation != null) {
                    packageDir = derivation.derive(packageDir, TMP.newFolder(name)).getAbsoluteFile();
                }
                File xmlFile = new File(TMP.getRoot(), name + ".xml");
                String text = FdmfTestSupport.run(dmf, packageDir, 3, xmlFile);
                assertTrue("XML protokol nevznikl: " + xmlFile, xmlFile.isFile());
                String xml = new String(Files.readAllBytes(xmlFile.toPath()), StandardCharsets.UTF_8);
                List<String[]> paths = pathReplacements(packageDir);
                result = new Result(normalizeText(text, paths), normalizeXml(xml, paths), xmlFile);
                RESULTS.put(name, result);
            }
            return result;
        }
    }

    @Test
    public void textProtocolMatchesGolden() throws Exception {
        assertMatchesGolden(new File(GOLDEN_DIR, name + ".txt"), result().text);
    }

    @Test
    public void xmlProtocolMatchesGolden() throws Exception {
        assertMatchesGolden(new File(GOLDEN_DIR, name + ".xml"), result().xml);
    }

    @Test
    public void xmlProtocolIsValidByXsd() throws Exception {
        Schema schema = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI).newSchema(PROTOCOL_XSD);
        // pri chybe vyhodi SAXParseException s radkem a popisem
        schema.newValidator().validate(new StreamSource(result().xmlFile));
    }

    /**
     * XML odpovida textu: namespace, typ protokolu, fDMF, pocet sekci a pravidel (verbosity 3 vypisuje vsechny),
     * celkovy pocet problemu a verdikt.
     */
    @Test
    public void xmlStructureMatchesText() throws Exception {
        Result result = result();
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        Document doc = factory.newDocumentBuilder().parse(result.xmlFile);
        Element root = doc.getDocumentElement();
        assertEquals(PROTOCOL_NS, root.getNamespaceURI());
        assertEquals("protocol", root.getLocalName());
        assertEquals("package", root.getAttribute("type"));

        Element fdmf = single(root, "fdmf");
        assertEquals(dmf.getType().toString(), fdmf.getAttribute("type"));
        assertEquals(dmf.getVersion(), fdmf.getAttribute("version"));

        List<String> lines = Arrays.asList(result.text.split("\n"));
        long textSections = lines.stream().filter(l -> l.startsWith("Sekce ")).count();
        long textRules = lines.stream().filter(l -> l.startsWith("Pravidlo ")).count();
        assertTrue("text bez sekci", textSections > 0);
        assertEquals("pocet sekci", textSections, doc.getElementsByTagNameNS(PROTOCOL_NS, "section").getLength());
        assertEquals("pocet pravidel", textRules, doc.getElementsByTagNameNS(PROTOCOL_NS, "rule").getLength());

        String totalLine = lines.stream().filter(l -> l.startsWith("Celkem: ")).findFirst().orElse(null);
        assertNotNull("text bez souhrnu", totalLine);
        boolean textValid = totalLine.endsWith("balík je: validní");
        Element summary = single(root, "summary");
        assertEquals(textValid ? "VALID" : "INVALID", summary.getAttribute("verdict"));

        Element problems = single(summary, "problems");
        Matcher count = Pattern.compile("^Celkem: (\\d+) ").matcher(totalLine);
        int textTotal = count.find() ? Integer.parseInt(count.group(1)) : 0;
        assertEquals("celkovy pocet problemu", String.valueOf(textTotal), problems.getAttribute("total"));
        // kazdy problem pravidla je v XML jako element problem
        assertEquals("pocet elementu problem", textTotal, doc.getElementsByTagNameNS(PROTOCOL_NS, "problem").getLength());
    }

    private static Element single(Element parent, String localName) {
        List<Element> found = new ArrayList<>();
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            if (children.item(i) instanceof Element && localName.equals(children.item(i).getLocalName())) {
                found.add((Element) children.item(i));
            }
        }
        assertEquals("pocet elementu " + localName + " v " + parent.getLocalName(), 1, found.size());
        return found.get(0);
    }

    // ---- normalizace

    /**
     * Dvojice {cesta, zastupny retezec}, od nejdelsi (nejkonkretnejsi) cesty. Kazda cesta i v kanonicke podobe
     * (na macOS je docasny adresar /var/... a kanonicky /private/var/...).
     */
    static List<String[]> pathReplacements(File packageDir) throws IOException {
        File moduleDir = new File("").getAbsoluteFile();
        File repoDir = moduleDir.getParentFile().getParentFile();
        List<String[]> result = new ArrayList<>();
        addPath(result, TMP.getRoot(), "[TMP]");
        addPath(result, packageDir, "[BALIK]");
        addPath(result, moduleDir, "[MODUL]");
        addPath(result, repoDir, "[REPO]");
        result.sort((a, b) -> Integer.compare(b[0].length(), a[0].length()));
        return result;
    }

    private static void addPath(List<String[]> result, File file, String placeholder) throws IOException {
        result.add(new String[]{file.getAbsolutePath(), placeholder});
        String canonical = file.getCanonicalPath();
        if (!canonical.equals(file.getAbsolutePath())) {
            result.add(new String[]{canonical, placeholder});
        }
    }

    private static String replacePaths(String content, List<String[]> paths) {
        for (String[] path : paths) {
            content = content.replace(path[0], path[1]);
        }
        if (File.separatorChar == '\\') {
            // ve Windows: cesty s / jako na ostatnich systemech; tyka se i relativnich cest bez zastupneho retezce
            // (problem/@file, @referencedFile), proto se nahrazuji vsechna zpetna lomitka (v hlaskach protokolu se
            // zpetne lomitko jinak nevyskytuje)
            content = content.replace('\\', '/');
        }
        return content;
    }

    static String normalizeText(String text, List<String[]> paths) {
        text = text.replace("\r\n", "\n");
        text = replacePaths(text, paths);
        text = text.replaceAll("(?m)^Validováno Komplexním Validátorem ve verzi .*, build z .*$",
                "Validováno Komplexním Validátorem ve verzi [VERZE], build z [BUILD_DATE]");
        return text;
    }

    static String normalizeXml(String xml, List<String[]> paths) {
        xml = xml.replace("\r\n", "\n");
        xml = replacePaths(xml, paths);
        xml = xml.replaceAll("durationMs=\"\\d+\"", "durationMs=\"[MS]\"");
        xml = xml.replaceAll("startTime=\"[^\"]*\"", "startTime=\"[CAS]\"");
        xml = xml.replaceAll("finishTime=\"[^\"]*\"", "finishTime=\"[CAS]\"");
        xml = xml.replaceAll("(<validator\\b[^>]*?\\bversion=\")[^\"]*\"", "$1[VERZE]\"");
        xml = xml.replaceAll("(<validator\\b[^>]*?\\bbuildDate=\")[^\"]*\"", "$1[BUILD_DATE]\"");
        return xml;
    }

    // ---- porovnani s golden souborem

    static boolean updateGolden() {
        return Boolean.parseBoolean(System.getProperty("kv.updateGolden"))
                || Boolean.parseBoolean(System.getenv("KV_UPDATE_GOLDEN"));
    }

    static void assertMatchesGolden(File goldenFile, String actual) throws IOException {
        if (updateGolden()) {
            Files.write(goldenFile.toPath(), actual.getBytes(StandardCharsets.UTF_8));
            System.out.println("golden soubor zapsan: " + goldenFile);
            return;
        }
        assertTrue("chybi golden soubor " + goldenFile + ", vytvori se spustenim s -Dkv.updateGolden=true", goldenFile.isFile());
        String expected = new String(Files.readAllBytes(goldenFile.toPath()), StandardCharsets.UTF_8).replace("\r\n", "\n");
        if (!expected.equals(actual)) {
            fail("vystup se lisi od " + goldenFile + " (pokud je zmena zamerna, pregeneruj s -Dkv.updateGolden=true):\n"
                    + lineDiff(expected, actual, 30));
        }
    }

    /**
     * Rozdil po radcich (LCS po odriznuti spolecneho zacatku a konce): "-  12: ..." chybi oproti golden (cislo radku v golden),
     * "+  12: ..." navic ve vystupu (cislo radku ve vystupu). Nejvys maxLines radku rozdilu.
     */
    static String lineDiff(String expected, String actual, int maxLines) {
        String[] a = expected.split("\n", -1);
        String[] b = actual.split("\n", -1);
        int start = 0;
        while (start < a.length && start < b.length && a[start].equals(b[start])) {
            start++;
        }
        int endA = a.length;
        int endB = b.length;
        while (endA > start && endB > start && a[endA - 1].equals(b[endB - 1])) {
            endA--;
            endB--;
        }
        int n = endA - start;
        int m = endB - start;
        int[][] lcs = new int[n + 1][m + 1];
        for (int i = n - 1; i >= 0; i--) {
            for (int j = m - 1; j >= 0; j--) {
                lcs[i][j] = a[start + i].equals(b[start + j]) ? lcs[i + 1][j + 1] + 1 : Math.max(lcs[i + 1][j], lcs[i][j + 1]);
            }
        }
        StringBuilder out = new StringBuilder();
        out.append(String.format("golden %d radku, vystup %d radku, prvni rozdil na radku %d%n", a.length, b.length, start + 1));
        int i = 0;
        int j = 0;
        int printed = 0;
        while ((i < n || j < m) && printed < maxLines) {
            if (i < n && j < m && a[start + i].equals(b[start + j])) {
                i++;
                j++;
            } else if (i < n && (j == m || lcs[i + 1][j] >= lcs[i][j + 1])) {
                out.append(String.format("-%5d: %s%n", start + i + 1, a[start + i]));
                i++;
                printed++;
            } else {
                out.append(String.format("+%5d: %s%n", start + j + 1, b[start + j]));
                j++;
                printed++;
            }
        }
        if (i < n || j < m) {
            out.append("... (dalsi rozdily vynechany)\n");
        }
        return out.toString();
    }
}
