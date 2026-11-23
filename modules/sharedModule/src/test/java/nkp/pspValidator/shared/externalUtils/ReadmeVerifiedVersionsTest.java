package nkp.pspValidator.shared.externalUtils;

import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Tabulka ověřených řad verzí v README.md (oddíl „Podporované verze nástrojů“, mezi značkami
 * {@value #START_MARK} a {@value #END_MARK}) musí odpovídat elementům verifiedVersions v externalUtils.xml.
 * README je jediné místo, kde se podporované verze uvádějí pro lidi; zdrojem pravdy je konfigurace.
 * <p>
 * Test čte obě strany sám a nezávisle na produkčním kódu: XML přes DOM a XPath, Markdown po řádcích. Pravidla, jak
 * se seznam platformy zapisuje do buňky, jsou popsaná jen tady:
 * <ul>
 * <li>vlastní seznam: řady oddělené „, “, za řadou v závorce přesná verze z atributu verifiedWith (bez atributu jen řada),
 * např. „1.29 (1.29.0-SNAPSHOT), 1.34 (1.34.0)“;</li>
 * <li>platforma bez řad: „žádná“;</li>
 * <li>platforma s inheritsFrom: „jako macOS“ (podle zdrojové platformy, „jako Windows“, „jako Linux“);</li>
 * <li>platforma, která v konfiguraci chybí: „bez seznamu“.</li>
 * </ul>
 */
public class ReadmeVerifiedVersionsTest {

    static final String START_MARK = "<!-- overene-verze:zacatek";
    static final String END_MARK = "<!-- overene-verze:konec";

    /** Hlavička tabulky: sloupce platforem v tomto pořadí, hodnoty atributu name v externalUtils.xml. */
    private static final List<String> HEADER = Arrays.asList("nástroj", "macOS", "Windows", "Linux");
    private static final List<String> OS_NAMES = Arrays.asList("MAC", "WINDOWS", "LINUX");

    /** Název nástroje v README podle atributu util/@name v externalUtils.xml, vypsáno ručně. */
    private static final Map<String, String> README_NAMES = new LinkedHashMap<>();

    static {
        README_NAMES.put("IMAGE_MAGICK", "ImageMagick");
        README_NAMES.put("JHOVE", "JHOVE");
        README_NAMES.put("JPYLYZER", "Jpylyzer");
        README_NAMES.put("KAKADU", "Kakadu");
        README_NAMES.put("MP3VAL", "MP3val");
        README_NAMES.put("SHNTOOL", "shntool");
        README_NAMES.put("CHECKMATE", "Checkmate");
        README_NAMES.put("VERAPDF", "veraPDF");
        README_NAMES.put("EPUBCHECK", "EPUBCheck");
    }

    // ----- skutečné soubory -----

    @Test
    public void readmeTableMatchesExternalUtilsConfig() throws Exception {
        File readme = findReadme();
        //konfigurace se hleda od korene repozitare jako README, aby test nezavisel na pracovnim adresari
        File config = new File(readme.getParentFile(),
                "modules/sharedModule/src/main/resources/nkp/pspValidator/shared/validatorConfig/externalUtils.xml");
        assertTrue("konfigurace nástrojů nenalezena: " + config, config.isFile());
        assertTrue("nenalezen " + config.getAbsolutePath(), config.isFile());
        Document xml = parseXml(Files.readAllBytes(config.toPath()));
        String markdown = new String(Files.readAllBytes(readme.toPath()), StandardCharsets.UTF_8);
        List<String> differences = differences(xml, markdown);
        if (!differences.isEmpty()) {
            fail("Tabulka ověřených verzí v " + readme.getPath() + " neodpovídá " + config.getAbsolutePath()
                    + " (zdrojem je konfigurace, README oprav):\n- " + String.join("\n- ", differences));
        }
    }

    /**
     * Kořen repozitáře: první nadřazený adresář pracovního adresáře (Gradle spouští testy v adresáři modulu), který
     * obsahuje README.md i settings.gradle. Když se nenajde, test selže (nepřeskočí se).
     */
    static File findReadme() {
        File start = new File("").getAbsoluteFile();
        for (File dir = start; dir != null; dir = dir.getParentFile()) {
            File readme = new File(dir, "README.md");
            if (readme.isFile() && new File(dir, "settings.gradle").isFile()) {
                return readme;
            }
        }
        fail("README.md kořene repozitáře (adresář s README.md a settings.gradle) nenalezen nad " + start);
        return null;
    }

    // ----- porovnání -----

    /** Rozdíly mezi tabulkou v README a konfigurací, srozumitelně popsané; prázdný seznam, když se shodují. */
    static List<String> differences(Document xml, String markdown) throws Exception {
        List<String> result = new ArrayList<>();
        Map<String, List<String>> readmeRows = readTable(markdown, result);
        if (readmeRows == null) {
            return result;
        }
        Map<String, List<String>> expectedRows = expectedRows(xml, result);
        for (Map.Entry<String, List<String>> expected : expectedRows.entrySet()) {
            String tool = expected.getKey();
            List<String> actual = readmeRows.get(tool);
            if (actual == null) {
                result.add("v README chybí řádek „" + tool + "“, má být: | " + tool + " | "
                        + String.join(" | ", expected.getValue()) + " |");
                continue;
            }
            for (int i = 0; i < OS_NAMES.size(); i++) {
                String want = expected.getValue().get(i);
                String have = actual.get(i);
                if (!want.equals(have)) {
                    result.add("řádek „" + tool + "“, sloupec „" + HEADER.get(i + 1) + "“: v README je „" + have
                            + "“, má být „" + want + "“");
                }
            }
        }
        for (String tool : readmeRows.keySet()) {
            if (!expectedRows.containsKey(tool)) {
                result.add("řádek „" + tool + "“ v README neodpovídá žádnému nástroji s verifiedVersions v externalUtils.xml");
            }
        }
        return result;
    }

    /**
     * Řádky tabulky mezi značkami: název nástroje → buňky platforem. Null (a popis do {@code problems}), když tabulku
     * nejde spolehlivě přečíst.
     */
    static Map<String, List<String>> readTable(String markdown, List<String> problems) {
        String[] lines = markdown.replace("\r\n", "\n").split("\n", -1);
        int start = -1;
        int end = -1;
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.startsWith(START_MARK)) {
                if (start >= 0) {
                    problems.add("značka " + START_MARK + " je v README víckrát (řádek " + (i + 1) + ")");
                    return null;
                }
                start = i;
            } else if (line.startsWith(END_MARK)) {
                if (end >= 0) {
                    problems.add("značka " + END_MARK + " je v README víckrát (řádek " + (i + 1) + ")");
                    return null;
                }
                end = i;
            }
        }
        if (start < 0 || end < 0 || end < start) {
            problems.add("v README chybí značky tabulky ověřených verzí (" + START_MARK + " … --> a " + END_MARK
                    + " … -->, v tomto pořadí)");
            return null;
        }
        List<String> tableLines = new ArrayList<>();
        for (int i = start + 1; i < end; i++) {
            if (!lines[i].trim().isEmpty()) {
                tableLines.add(lines[i].trim());
            }
        }
        if (tableLines.size() < 2) {
            problems.add("mezi značkami v README není tabulka (hlavička a oddělovač)");
            return null;
        }
        List<String> header = cells(tableLines.get(0));
        if (!HEADER.equals(header)) {
            problems.add("hlavička tabulky v README je " + header + ", má být " + HEADER);
            return null;
        }
        if (!tableLines.get(1).matches("\\|(\\s*:?-+:?\\s*\\|)+")) {
            problems.add("druhý řádek tabulky v README není oddělovač |---|: „" + tableLines.get(1) + "“");
            return null;
        }
        Map<String, List<String>> rows = new LinkedHashMap<>();
        for (String line : tableLines.subList(2, tableLines.size())) {
            List<String> row = cells(line);
            if (!line.startsWith("|") || row.size() != HEADER.size()) {
                problems.add("řádek tabulky v README nemá " + HEADER.size() + " buňky: „" + line + "“");
                return null;
            }
            if (rows.put(row.get(0), row.subList(1, row.size())) != null) {
                problems.add("nástroj „" + row.get(0) + "“ je v tabulce v README víckrát");
                return null;
            }
        }
        return rows;
    }

    private static List<String> cells(String line) {
        String inner = line.trim();
        if (inner.startsWith("|")) {
            inner = inner.substring(1);
        }
        if (inner.endsWith("|")) {
            inner = inner.substring(0, inner.length() - 1);
        }
        List<String> result = new ArrayList<>();
        for (String cell : inner.split("\\|", -1)) {
            result.add(cell.trim());
        }
        return result;
    }

    /** Očekávané buňky podle konfigurace: název nástroje v README → buňky macOS, Windows, Linux. */
    static Map<String, List<String>> expectedRows(Document xml, List<String> problems) throws Exception {
        XPath xpath = XPathFactory.newInstance().newXPath();
        NodeList utils = (NodeList) xpath.evaluate("/externalUtils/util[verifiedVersions]", xml, XPathConstants.NODESET);
        Map<String, List<String>> rows = new LinkedHashMap<>();
        for (int u = 0; u < utils.getLength(); u++) {
            Element util = (Element) utils.item(u);
            String utilName = util.getAttribute("name");
            String readmeName = README_NAMES.get(utilName);
            if (readmeName == null) {
                problems.add("nástroj " + utilName + " z externalUtils.xml nemá v testu název pro README (README_NAMES)");
                continue;
            }
            List<String> row = new ArrayList<>();
            for (String os : OS_NAMES) {
                Element osEl = (Element) xpath.evaluate("verifiedVersions/operatingSystem[@name='" + os + "']", util, XPathConstants.NODE);
                row.add(cell(xpath, osEl));
            }
            rows.put(readmeName, row);
        }
        return rows;
    }

    private static String cell(XPath xpath, Element osEl) throws Exception {
        if (osEl == null) {
            return "bez seznamu";
        }
        String inheritsFrom = osEl.getAttribute("inheritsFrom").trim();
        if (!inheritsFrom.isEmpty()) {
            switch (inheritsFrom) {
                case "MAC":
                    return "jako macOS";
                case "WINDOWS":
                    return "jako Windows";
                case "LINUX":
                    return "jako Linux";
                default:
                    return "jako " + inheritsFrom;
            }
        }
        NodeList series = (NodeList) xpath.evaluate("series", osEl, XPathConstants.NODESET);
        if (series.getLength() == 0) {
            return "žádná";
        }
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < series.getLength(); i++) {
            Element seriesEl = (Element) series.item(i);
            text.append(i == 0 ? "" : ", ").append(seriesEl.getTextContent().trim());
            String verifiedWith = seriesEl.getAttribute("verifiedWith").trim();
            if (!verifiedWith.isEmpty()) {
                text.append(" (").append(verifiedWith).append(')');
            }
        }
        return text.toString();
    }

    static Document parseXml(byte[] bytes) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(false);
        return factory.newDocumentBuilder().parse(new ByteArrayInputStream(bytes));
    }

    // ----- samotné porovnání na umělých vstupech (musí umět selhat) -----

    private static Document xml(String utils) throws Exception {
        return parseXml(("<externalUtils>" + utils + "</externalUtils>").getBytes(StandardCharsets.UTF_8));
    }

    private static final String JHOVE_XML = "<util name=\"JHOVE\"><verifiedVersions>"
            + "<operatingSystem name=\"MAC\"><series verifiedWith=\"1.29.0-SNAPSHOT\">1.29</series><series verifiedWith=\"1.34.0\">1.34</series></operatingSystem>"
            + "<operatingSystem name=\"WINDOWS\"><series verifiedWith=\"1.34.0\">1.34</series></operatingSystem>"
            + "<operatingSystem name=\"LINUX\" inheritsFrom=\"MAC\"/>"
            + "</verifiedVersions></util>";

    private static String readme(String... rows) {
        StringBuilder text = new StringBuilder("# README\n\ntext\n\n<!-- overene-verze:zacatek (poznámka) -->\n\n"
                + "| nástroj | macOS | Windows | Linux |\n|---|---|---|---|\n");
        for (String row : rows) {
            text.append(row).append('\n');
        }
        return text.append("\n<!-- overene-verze:konec -->\n\ndalší text\n").toString();
    }

    private static final String JHOVE_ROW = "| JHOVE | 1.29 (1.29.0-SNAPSHOT), 1.34 (1.34.0) | 1.34 (1.34.0) | jako macOS |";

    @Test
    public void matchingTableHasNoDifferences() throws Exception {
        assertEquals(new ArrayList<String>(), differences(xml(JHOVE_XML), readme(JHOVE_ROW)));
    }

    @Test
    public void differentCellIsReportedWithRowColumnAndExpectedValue() throws Exception {
        List<String> differences = differences(xml(JHOVE_XML),
                readme("| JHOVE | 1.29 (1.29.0-SNAPSHOT), 1.34 (1.34.0) | 1.34 (1.34.1) | jako macOS |"));
        assertEquals(Arrays.asList("řádek „JHOVE“, sloupec „Windows“: v README je „1.34 (1.34.1)“, má být „1.34 (1.34.0)“"),
                differences);
    }

    @Test
    public void everyKindOfCellIsCompared() throws Exception {
        String xml = "<util name=\"KAKADU\"><verifiedVersions>"
                + "<operatingSystem name=\"MAC\"><series>8.5</series></operatingSystem>"
                + "<operatingSystem name=\"WINDOWS\"/>"
                + "</verifiedVersions></util>";
        assertEquals(new ArrayList<String>(), differences(xml(xml), readme("| Kakadu | 8.5 | žádná | bez seznamu |")));
        assertEquals(Arrays.asList(
                        "řádek „Kakadu“, sloupec „macOS“: v README je „8.5 (8.5)“, má být „8.5“",
                        "řádek „Kakadu“, sloupec „Windows“: v README je „8.5“, má být „žádná“",
                        "řádek „Kakadu“, sloupec „Linux“: v README je „jako macOS“, má být „bez seznamu“"),
                differences(xml(xml), readme("| Kakadu | 8.5 (8.5) | 8.5 | jako macOS |")));
        // prevzeti z jine platformy
        assertEquals(Arrays.asList("řádek „JHOVE“, sloupec „Linux“: v README je „1.29 (1.29.0-SNAPSHOT), 1.34 (1.34.0)“, má být „jako macOS“"),
                differences(xml(JHOVE_XML), readme("| JHOVE | 1.29 (1.29.0-SNAPSHOT), 1.34 (1.34.0) | 1.34 (1.34.0) | 1.29 (1.29.0-SNAPSHOT), 1.34 (1.34.0) |")));
        // poradi rad je soucast zapisu
        assertEquals(Arrays.asList("řádek „JHOVE“, sloupec „macOS“: v README je „1.34 (1.34.0), 1.29 (1.29.0-SNAPSHOT)“, má být „1.29 (1.29.0-SNAPSHOT), 1.34 (1.34.0)“"),
                differences(xml(JHOVE_XML), readme("| JHOVE | 1.34 (1.34.0), 1.29 (1.29.0-SNAPSHOT) | 1.34 (1.34.0) | jako macOS |")));
    }

    @Test
    public void missingAndExtraRows() throws Exception {
        assertEquals(Arrays.asList("v README chybí řádek „JHOVE“, má být: | JHOVE | 1.29 (1.29.0-SNAPSHOT), 1.34 (1.34.0) | 1.34 (1.34.0) | jako macOS |"),
                differences(xml(JHOVE_XML), readme()));
        assertEquals(Arrays.asList("řádek „Jpylyzer“ v README neodpovídá žádnému nástroji s verifiedVersions v externalUtils.xml"),
                differences(xml(JHOVE_XML), readme(JHOVE_ROW, "| Jpylyzer | 2.2 (2.2.1) | 2.2 (2.2.1) | jako macOS |")));
        // nastroj bez verifiedVersions v README byt nema
        assertEquals(Arrays.asList("řádek „Jpylyzer“ v README neodpovídá žádnému nástroji s verifiedVersions v externalUtils.xml"),
                differences(xml(JHOVE_XML + "<util name=\"JPYLYZER\"/>"), readme(JHOVE_ROW, "| Jpylyzer | 2.2 (2.2.1) | 2.2 (2.2.1) | jako macOS |")));
        assertEquals(Arrays.asList("nástroj NOVY z externalUtils.xml nemá v testu název pro README (README_NAMES)"),
                differences(xml(JHOVE_XML + "<util name=\"NOVY\"><verifiedVersions/></util>"), readme(JHOVE_ROW)));
    }

    @Test
    public void unreadableTableFails() throws Exception {
        assertEquals(Arrays.asList("v README chybí značky tabulky ověřených verzí (<!-- overene-verze:zacatek … --> a <!-- overene-verze:konec … -->, v tomto pořadí)"),
                differences(xml(JHOVE_XML), "| nástroj | macOS | Windows | Linux |\n|---|---|---|---|\n" + JHOVE_ROW + "\n"));
        assertEquals(Arrays.asList("v README chybí značky tabulky ověřených verzí (<!-- overene-verze:zacatek … --> a <!-- overene-verze:konec … -->, v tomto pořadí)"),
                differences(xml(JHOVE_XML), "<!-- overene-verze:konec -->\n" + JHOVE_ROW + "\n<!-- overene-verze:zacatek -->\n"));
        assertEquals(Arrays.asList("značka <!-- overene-verze:zacatek je v README víckrát (řádek 14)"),
                differences(xml(JHOVE_XML), readme(JHOVE_ROW) + "<!-- overene-verze:zacatek -->\n"));
        assertEquals(Arrays.asList("hlavička tabulky v README je [nástroj, macOS, Linux, Windows], má být [nástroj, macOS, Windows, Linux]"),
                differences(xml(JHOVE_XML), readme(JHOVE_ROW).replace("| macOS | Windows | Linux |", "| macOS | Linux | Windows |")));
        assertEquals(Arrays.asList("druhý řádek tabulky v README není oddělovač |---|: „| JHOVE | 1.29 (1.29.0-SNAPSHOT), 1.34 (1.34.0) | 1.34 (1.34.0) | jako macOS |“"),
                differences(xml(JHOVE_XML), readme(JHOVE_ROW).replace("|---|---|---|---|\n", "")));
        assertEquals(Arrays.asList("řádek tabulky v README nemá 4 buňky: „| JHOVE | 1.34 (1.34.0) | jako macOS |“"),
                differences(xml(JHOVE_XML), readme("| JHOVE | 1.34 (1.34.0) | jako macOS |")));
        assertEquals(Arrays.asList("nástroj „JHOVE“ je v tabulce v README víckrát"),
                differences(xml(JHOVE_XML), readme(JHOVE_ROW, JHOVE_ROW)));
        assertEquals(Arrays.asList("mezi značkami v README není tabulka (hlavička a oddělovač)"),
                differences(xml(JHOVE_XML), "<!-- overene-verze:zacatek -->\n\n<!-- overene-verze:konec -->\n"));
    }
}
