package nkp.pspValidator.shared.externalUtils.validation.extractions;

import nkp.pspValidator.shared.NamespaceContextImpl;
import nkp.pspValidator.shared.externalUtils.ExtractionResultType;
import nkp.pspValidator.shared.externalUtils.validation.DataExtraction;
import org.junit.BeforeClass;
import org.junit.Test;
import org.w3c.dom.Document;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static nkp.pspValidator.shared.externalUtils.ExternalUtilsTestSupport.outputXml;
import static org.junit.Assert.*;

/**
 * XmlDataExtraction / FirstNonemptyByXpathDataExctraction nad ulozenymi vystupy Jpylyzer 2.2.0 a JHOVE 1.29
 * (src/test/resources/externalUtils). XPath vyrazy a jmenne prostory jsou prevzate z profilu fDMF fund_unit_0.2.
 */
public class XpathDataExtractionTest {

    private static final NamespaceContextImpl JPYLYZER_NS = new NamespaceContextImpl();
    private static final NamespaceContextImpl JHOVE_NS = new NamespaceContextImpl();

    private static Document jpylyzerMc;
    private static Document jpylyzerUc1;
    private static Document jpylyzerEmptyFile;
    private static Document jhoveMc;
    private static Document jhoveHarvardNs;
    private static Document jhoveInvalid;

    @BeforeClass
    public static void setUp() throws Exception {
        JPYLYZER_NS.setNamespace("j", "http://openpreservation.org/ns/jpylyzer/v2/");
        JHOVE_NS.setNamespace("j", "http://schema.openpreservation.org/ois/xml/ns/jhove");
        JHOVE_NS.setNamespace("j2", "http://hul.harvard.edu/ois/xml/ns/jhove");
        JHOVE_NS.setNamespace("m", "http://www.loc.gov/mix/v20");
        jpylyzerMc = outputXml("jpylyzer", "tools_mc.xml");
        jpylyzerUc1 = outputXml("jpylyzer", "ok_uc_UC1.xml");
        jpylyzerEmptyFile = outputXml("jpylyzer", "nevalidni_u5463f.xml");
        jhoveMc = outputXml("jhove", "tools_mc.xml");
        jhoveHarvardNs = outputXml("jhove", "harvard_ns_rel1.6_mc.xml");
        jhoveInvalid = outputXml("jhove", "nevalidni_u0911e.xml");
    }

    private static Object jpylyzer(ExtractionResultType type, Document doc, String... xpaths) throws DataExtraction.ExtractionException {
        return new FirstNonemptyByXpathDataExctraction(type, JPYLYZER_NS, Arrays.asList(xpaths)).extract(doc);
    }

    private static Object jhove(ExtractionResultType type, Document doc, String... xpaths) throws DataExtraction.ExtractionException {
        return new FirstNonemptyByXpathDataExctraction(type, JHOVE_NS, Arrays.asList(xpaths)).extract(doc);
    }

    // Jpylyzer, jednotlive typy vysledku

    @Test
    public void jpylyzerString() throws Exception {
        assertEquals("jpylyzer", jpylyzer(ExtractionResultType.STRING, jpylyzerMc, "//j:toolInfo/j:toolName"));
        assertEquals("2.2.0", jpylyzer(ExtractionResultType.STRING, jpylyzerMc, "//j:toolInfo/j:toolVersion"));
        assertEquals("True", jpylyzer(ExtractionResultType.STRING, jpylyzerMc, "//j:isValid"));
        assertEquals("RPCL", jpylyzer(ExtractionResultType.STRING, jpylyzerMc, "//j:properties/j:contiguousCodestreamBox/j:cod/j:order"));
        assertEquals("5-3 reversible", jpylyzer(ExtractionResultType.STRING, jpylyzerMc, "//j:properties/j:contiguousCodestreamBox/j:cod/j:transformation"));
        assertEquals("Kakadu-v8.5", jpylyzer(ExtractionResultType.STRING, jpylyzerMc, "//j:properties/j:contiguousCodestreamBox/j:com/j:comment"));
    }

    @Test
    public void jpylyzerInteger() throws Exception {
        assertEquals(4096, jpylyzer(ExtractionResultType.INTEGER, jpylyzerMc, "//j:properties/j:contiguousCodestreamBox/j:siz/j:xTsiz"));
        assertEquals(1, jpylyzer(ExtractionResultType.INTEGER, jpylyzerMc, "//j:properties/j:contiguousCodestreamBox/j:siz/j:numberOfTiles"));
        assertEquals(6, jpylyzer(ExtractionResultType.INTEGER, jpylyzerUc1, "//j:properties/j:contiguousCodestreamBox/j:siz/j:numberOfTiles"));
    }

    @Test
    public void jpylyzerFloat() throws Exception {
        assertEquals(2.67f, jpylyzer(ExtractionResultType.FLOAT, jpylyzerUc1, "//j:properties/j:compressionRatio"));
        assertEquals(11771.65f, jpylyzer(ExtractionResultType.FLOAT, jpylyzerUc1, "//j:properties/j:jp2HeaderBox/j:resolutionBox/j:captureResolutionBox/j:vRescInPixelsPerMeter"));
    }

    @Test
    public void jpylyzerStringList() throws Exception {
        assertEquals(Arrays.asList("128", "128", "128", "128", "256", "256"),
                jpylyzer(ExtractionResultType.STRING_LIST, jpylyzerMc, "//j:properties/j:contiguousCodestreamBox/j:cod/j:precinctSizeX"));
    }

    @Test
    public void jpylyzerIntegerAndFloatList() throws Exception {
        assertEquals(Arrays.asList(128, 128, 128, 128, 256, 256),
                jpylyzer(ExtractionResultType.INTEGER_LIST, jpylyzerMc, "//j:properties/j:contiguousCodestreamBox/j:cod/j:precinctSizeY"));
        assertEquals(Arrays.asList(128.0f, 128.0f, 128.0f, 128.0f, 256.0f, 256.0f),
                jpylyzer(ExtractionResultType.FLOAT_LIST, jpylyzerMc, "//j:properties/j:contiguousCodestreamBox/j:cod/j:precinctSizeY"));
    }

    @Test
    public void listWithUnparsableItemIsNull() throws Exception {
        // NumberFormatException u kterekoli polozky se spolkne a cely vysledek je null
        assertNull(jpylyzer(ExtractionResultType.INTEGER_LIST, jpylyzerMc, "//j:properties/j:contiguousCodestreamBox/j:com/j:comment"));
        assertNull(jpylyzer(ExtractionResultType.FLOAT_LIST, jpylyzerMc, "//j:toolInfo/j:toolName"));
    }

    // chybejici hodnoty a firstNonempty

    @Test
    public void missingValueIsNull() throws Exception {
        // tools_mc.jp2 nema resolution box (realny vystup), hodnota nenalezena
        String resolution = "//j:properties/j:jp2HeaderBox/j:resolutionBox/j:captureResolutionBox/j:vRescInPixelsPerMeter";
        assertNull(jpylyzer(ExtractionResultType.STRING, jpylyzerMc, resolution));
        assertNull(jpylyzer(ExtractionResultType.FLOAT, jpylyzerMc, resolution));
        assertNull(jpylyzer(ExtractionResultType.INTEGER, jpylyzerMc, resolution));
        // prazdny soubor: Jpylyzer vypise jen isValid False a prazdne properties
        assertEquals("False", jpylyzer(ExtractionResultType.STRING, jpylyzerEmptyFile, "//j:isValid"));
        assertNull(jpylyzer(ExtractionResultType.STRING, jpylyzerEmptyFile, "//j:properties/j:jp2HeaderBox/j:imageHeaderBox/j:height"));
    }

    @Test
    public void unparsableNumberIsNull() throws Exception {
        assertNull(jpylyzer(ExtractionResultType.INTEGER, jpylyzerMc, "//j:toolInfo/j:toolVersion"));
        assertNull(jpylyzer(ExtractionResultType.FLOAT, jpylyzerMc, "//j:toolInfo/j:toolName"));
    }

    @Test
    public void firstNonemptyFallsBackToNextXpath() throws Exception {
        // Rozlišení: capture resolution chybi, pouzije se display resolution, pripadne dalsi
        assertEquals("jpylyzer", jpylyzer(ExtractionResultType.STRING, jpylyzerMc, "//j:neexistuje", "//j:toolInfo/j:toolName"));
        assertEquals(1, jpylyzer(ExtractionResultType.INTEGER, jpylyzerMc, "//j:toolInfo/j:toolName", "//j:properties/j:contiguousCodestreamBox/j:siz/j:numberOfTiles"));
        assertNull(jpylyzer(ExtractionResultType.STRING, jpylyzerMc, "//j:a", "//j:b"));
        assertNull(jpylyzer(ExtractionResultType.STRING, jpylyzerMc));
    }

    @Test
    public void firstNonemptyDoesNotFallBackForEmptyList() throws Exception {
        // známá vada: u typu *_LIST je "nenalezeno" prazdny seznam, jehoz toString() je "[]" (neprazdny retezec);
        // firstNonempty proto vrati prazdny seznam z prvniho XPath a dalsi XPath nezkusi
        Object result = jpylyzer(ExtractionResultType.STRING_LIST, jpylyzerMc, "//j:neexistuje", "//j:properties/j:contiguousCodestreamBox/j:cod/j:precinctSizeX");
        assertEquals(Collections.emptyList(), result);
    }

    @Test
    public void jpylyzerXpathWithoutNamespaceFindsNothing() throws Exception {
        // známá vada: profil Jpylyzer ma validaci "regions_of_interest" s XPath "rgn" (relativni, bez prefixu),
        // ktera nad dokumentem nenajde nic ani kdyby vystup element rgn obsahoval; mustNotExist tak vzdy projde
        assertNull(jpylyzer(ExtractionResultType.STRING, jpylyzerMc, "rgn"));
        assertNull(jpylyzer(ExtractionResultType.STRING, jpylyzerMc, "//rgn"));
    }

    // JHOVE

    @Test
    public void jhoveCurrentNamespace() throws Exception {
        assertEquals("Jhove", jhove(ExtractionResultType.STRING, jhoveMc, "/j:jhove/@name", "/j2:jhove/@name"));
        assertEquals("1.29.0-SNAPSHOT", jhove(ExtractionResultType.STRING, jhoveMc, "/j:jhove/@release", "/j2:jhove/@release"));
        assertEquals("Well-Formed and valid", jhove(ExtractionResultType.STRING, jhoveMc, "//j:repInfo/j:status", "//j2:repInfo/j2:status"));
        assertEquals("JPEG 2000", jhove(ExtractionResultType.STRING, jhoveMc, "//j:repInfo/j:format", "//j2:repInfo/j2:format"));
        assertEquals(4096, jhove(ExtractionResultType.INTEGER, jhoveMc,
                "//j:property[j:name='Codestream']/j:values/j:property[j:name='ImageAndTileSize']/j:values/j:property[j:name='XTSize']/j:values/j:value"));
        assertEquals("2", jhove(ExtractionResultType.STRING, jhoveMc, "//j:property[j:name='ProgressionOrder']/j:values/j:value"));
    }

    @Test
    public void jhoveInvalidFile() throws Exception {
        assertEquals("Not well-formed", jhove(ExtractionResultType.STRING, jhoveInvalid, "//j:repInfo/j:status", "//j2:repInfo/j2:status"));
        assertNull(jhove(ExtractionResultType.STRING, jhoveInvalid, "//j:property[j:name='YSize']/j:values/j:value"));
    }

    @Test
    public void jhoveOldHarvardNamespaceFallsBack() throws Exception {
        // starsi JHOVE (zde 1.6) pouziva jmenny prostor hul.harvard.edu, profil ma pro to druhy XPath s prefixem j2
        assertEquals("1.6", jhove(ExtractionResultType.STRING, jhoveHarvardNs, "/j:jhove/@release", "/j2:jhove/@release"));
        assertEquals("Well-Formed and valid", jhove(ExtractionResultType.STRING, jhoveHarvardNs, "//j:repInfo/j:status", "//j2:repInfo/j2:status"));
    }

    @Test
    public void jhoveTileCountWithCount() throws Exception {
        String current = "count(.//j:name[text()='TilePart'])";
        String old = "count(.//j2:name[text()='TilePart'])";
        assertEquals(1, jhove(ExtractionResultType.INTEGER, jhoveMc, current, old));
        assertEquals(6, jhove(ExtractionResultType.INTEGER, jhoveHarvardNs, old));
        // známá vada: count() vraci 0 i kdyz nic nenajde, 0 neni "prazdna" hodnota, takze firstNonempty
        // u stareho jmenneho prostoru skonci u prvniho XPath s 0 a profil JHOVE pak hlasi "Počet dlaždic" (min 1)
        assertEquals(0, jhove(ExtractionResultType.INTEGER, jhoveHarvardNs, current, old));
    }

    // chyby

    @Test
    public void invalidXpathThrowsExtractionException() {
        try {
            jpylyzer(ExtractionResultType.STRING, jpylyzerMc, "//j:[");
            fail();
        } catch (DataExtraction.ExtractionException e) {
            // chyba z buildXpath se v extract() zachyti vetvi Throwable a zabali podruhe, zprava proto zacina
            // nazvem tridy vyjimky: "nkp...DataExtraction$ExtractionException: chyba v zápisu Xpath '//j:[': ..."
            assertTrue(e.getMessage(), e.getMessage().startsWith(DataExtraction.ExtractionException.class.getName() + ": chyba v zápisu Xpath '//j:['"));
        }
    }

    @Test
    public void unknownPrefixThrowsExtractionException() {
        try {
            jpylyzer(ExtractionResultType.STRING, jpylyzerMc, "//x:toolName");
            fail();
        } catch (DataExtraction.ExtractionException e) {
            assertNotNull(e.getMessage());
        }
    }

    @Test
    public void nodesetResultTypeOnStringXpathThrowsExtractionException() {
        // STRING_LIST nad vyrazem, ktery nevraci uzly (count), skonci chybou vyhodnoceni XPath
        try {
            jpylyzer(ExtractionResultType.STRING_LIST, jpylyzerMc, "count(//j:precinctSizeX)");
            fail();
        } catch (DataExtraction.ExtractionException e) {
            assertTrue(e.getMessage(), e.getMessage().startsWith("chyba v zápisu Xpath 'count(//j:precinctSizeX)'"));
        }
    }

    @Test
    public void resultTypeIsKept() {
        List<String> paths = Collections.singletonList("/");
        assertEquals(ExtractionResultType.FLOAT, new FirstNonemptyByXpathDataExctraction(ExtractionResultType.FLOAT, JPYLYZER_NS, paths).extractionResultType);
    }
}
