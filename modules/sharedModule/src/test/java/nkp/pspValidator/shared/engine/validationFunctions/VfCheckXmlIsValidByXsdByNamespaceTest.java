package nkp.pspValidator.shared.engine.validationFunctions;

import nkp.pspValidator.shared.engine.Engine;
import nkp.pspValidator.shared.engine.Level;
import nkp.pspValidator.shared.engine.ValueEvaluation;
import nkp.pspValidator.shared.engine.ValueType;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Vyber XSD podle namespace korenoveho elementu, vcetne vice alternativnich XSD pro jeden namespace (ALTO 2.0 a 2.1
 * sdili ns-v2#; 2.1 ma povinny MeasurementUnit, 2.0 ne, 2.0 zase nezna Tags). Schemata z fDMF fund_unit_0.2.
 */
public class VfCheckXmlIsValidByXsdByNamespaceTest {

    private static final String VF_NAME = "checkXmlIsValidByXsdByNamespace";
    private static final File XSD_DIR = new File("src/main/resources/nkp/pspValidator/shared/validatorConfig/fDMF/fund_unit_0.2/xsd");
    private static final File ALTO_2_0 = new File(XSD_DIR, "alto_2.0.xsd");
    private static final File ALTO_2_1 = new File(XSD_DIR, "alto_2.1.xsd");
    private static final File ALTO_3_1 = new File(XSD_DIR, "alto_3.1.xsd");

    private static final String NS_V2 = "http://www.loc.gov/standards/alto/ns-v2#";
    private static final String LAYOUT = "<Layout><Page ID=\"P1\" PHYSICAL_IMG_NR=\"1\" HEIGHT=\"10\" WIDTH=\"10\">"
            + "<PrintSpace HEIGHT=\"10\" WIDTH=\"10\" HPOS=\"0\" VPOS=\"0\"/></Page></Layout>";
    private static final String DESCRIPTION_WITHOUT_UNIT = "<Description><sourceImageInformation><fileName>a.jp2</fileName>"
            + "</sourceImageInformation></Description>";
    private static final String DESCRIPTION_WITH_UNIT = "<Description><MeasurementUnit>pixel</MeasurementUnit></Description>";
    private static final String TAGS = "<Tags><OtherTag ID=\"T1\" LABEL=\"x\"/></Tags>";

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private Engine engine;

    @Before
    public void setup() {
        engine = new Engine(null);
    }

    private File alto(String namespace, String content) throws Exception {
        File f = tmp.newFile();
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<alto xmlns=\"" + namespace + "\">" + content + "</alto>";
        Files.write(f.toPath(), xml.getBytes(StandardCharsets.UTF_8));
        return f;
    }

    /**
     * ALTO 2.0 bez MeasurementUnit: validni jen proti 2.0.
     */
    private File alto20WithoutUnit() throws Exception {
        return alto(NS_V2, DESCRIPTION_WITHOUT_UNIT + LAYOUT);
    }

    /**
     * ALTO 2.1 s Tags: validni jen proti 2.1.
     */
    private File alto21WithTags() throws Exception {
        return alto(NS_V2, DESCRIPTION_WITH_UNIT + TAGS + LAYOUT);
    }

    /**
     * Bez MeasurementUnit a s Tags: nevalidni proti obema.
     */
    private File alto2InvalidForBoth() throws Exception {
        return alto(NS_V2, DESCRIPTION_WITHOUT_UNIT + TAGS + LAYOUT);
    }

    private ValidationResult validate(List<File> xsdFiles, File... xmlFiles) throws Exception {
        return engine.buildValidationFunction(VF_NAME)
                .withValueParam("xsd_files", ValueType.FILE_LIST, new ValueEvaluation(xsdFiles))
                .withValueParam("xml_files", ValueType.FILE_LIST, new ValueEvaluation(Arrays.asList(xmlFiles)))
                .withValueParam("level", ValueType.LEVEL, new ValueEvaluation(Level.ERROR))
                .validate();
    }

    private static String messages(ValidationResult result) {
        StringBuilder sb = new StringBuilder();
        for (ValidationProblem p : result.getProblems()) {
            sb.append(p.getLevel()).append(": ").append(p.getFullMessage()).append('\n');
        }
        return sb.toString();
    }

    @Test
    public void twoXsdsForNamespaceValidAgainstFirst() throws Exception {
        ValidationResult result = validate(Arrays.asList(ALTO_2_0, ALTO_2_1, ALTO_3_1), alto20WithoutUnit());
        assertFalse(messages(result), result.hasProblems());
    }

    @Test
    public void twoXsdsForNamespaceValidAgainstSecond() throws Exception {
        ValidationResult result = validate(Arrays.asList(ALTO_2_0, ALTO_2_1, ALTO_3_1), alto21WithTags());
        assertFalse(messages(result), result.hasProblems());
    }

    @Test
    public void twoXsdsForNamespaceOrderDoesNotMatter() throws Exception {
        ValidationResult result = validate(Arrays.asList(ALTO_2_1, ALTO_2_0), alto20WithoutUnit(), alto21WithTags());
        assertFalse(messages(result), result.hasProblems());
    }

    @Test
    public void twoXsdsForNamespaceValidAgainstNone() throws Exception {
        File xml = alto2InvalidForBoth();
        ValidationResult result = validate(Arrays.asList(ALTO_2_0, ALTO_2_1, ALTO_3_1), xml);
        assertEquals(messages(result), 1, result.getProblems().size());
        ValidationProblem p = result.getProblems().get(0);
        assertEquals(Level.ERROR, p.getLevel());
        String msg = p.getFullMessage();
        assertTrue(msg, msg.contains("není validní podle žádného z Xml schemat pro namespace '" + NS_V2 + "'"));
        //chyby proti obema alternativam
        assertTrue(msg, msg.contains(ALTO_2_0.getAbsolutePath() + ": "));
        assertTrue(msg, msg.contains(ALTO_2_1.getAbsolutePath() + ": "));
        assertTrue(msg, msg.contains("Tags"));
        assertTrue(msg, msg.contains("MeasurementUnit"));
        assertTrue(p.getSimpleMessage(), p.getSimpleMessage().contains("2.0: "));
        assertTrue(p.getSimpleMessage(), p.getSimpleMessage().contains("2.1: "));
        assertEquals(xml, p.getFile());
    }

    /**
     * Jedno XSD pro namespace: chovani i text hlasky jako pred zavedenim alternativ.
     */
    @Test
    public void singleXsdForNamespaceKeepsOriginalMessage() throws Exception {
        ValidationResult result = validate(Arrays.asList(ALTO_2_1, ALTO_3_1), alto20WithoutUnit());
        assertEquals(messages(result), 1, result.getProblems().size());
        ValidationProblem p = result.getProblems().get(0);
        assertTrue(p.getFullMessage(), p.getFullMessage().contains("není validní podle Xml schema ze souboru " + ALTO_2_1.getAbsolutePath()));
        assertTrue(p.getSimpleMessage(), p.getSimpleMessage().startsWith("obsah souboru není validní podle XSD schématu: "));
        assertEquals(ALTO_2_1, p.getXsdFile());
    }

    @Test
    public void singleXsdForNamespaceValid() throws Exception {
        ValidationResult result = validate(Arrays.asList(ALTO_2_0), alto20WithoutUnit());
        assertFalse(messages(result), result.hasProblems());
    }

    @Test
    public void unknownNamespaceListsAlternatives() throws Exception {
        File xml = alto("http://www.loc.gov/standards/alto/ns-v9#", DESCRIPTION_WITH_UNIT + LAYOUT);
        ValidationResult result = validate(Arrays.asList(ALTO_2_0, ALTO_2_1, ALTO_3_1), xml);
        assertEquals(messages(result), 1, result.getProblems().size());
        String msg = result.getProblems().get(0).getFullMessage();
        assertTrue(msg, msg.contains("nepodporovanou verzi formátu"));
        assertTrue(msg, msg.contains("2.0, 2.1 (" + NS_V2 + "), 3.1 (http://www.loc.gov/standards/alto/ns-v3#)"));
    }

    @Test
    public void eachFileReportedSeparately() throws Exception {
        ValidationResult result = validate(Arrays.asList(ALTO_2_0, ALTO_2_1), alto20WithoutUnit(), alto2InvalidForBoth(), alto2InvalidForBoth());
        assertEquals(messages(result), 2, result.getProblems().size());
    }
}
