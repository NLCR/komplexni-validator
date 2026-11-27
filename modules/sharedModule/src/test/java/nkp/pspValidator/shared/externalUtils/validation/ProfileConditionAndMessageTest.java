package nkp.pspValidator.shared.externalUtils.validation;

import nkp.pspValidator.shared.engine.exceptions.ValidatorConfigurationException;
import nkp.pspValidator.shared.externalUtils.ExternalUtil;
import nkp.pspValidator.shared.externalUtils.ExternalUtilsTestSupport;
import nkp.pspValidator.shared.externalUtils.ExternalUtilsTestSupport.CannedOutputExternalUtilManager;
import nkp.pspValidator.shared.externalUtils.ResourceType;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Rozšíření jazyka profilů binárních souborů: podmínka validace (element condition, XPath 1.0 boolean) a vlastní text
 * problému (atribut message pravidla). Profily jsou malé syntetické profily nad syntetickým výstupem.
 */
public class ProfileConditionAndMessageTest {

    private static final File XSD = new File(ExternalUtilsTestSupport.VALIDATOR_CONFIG_DIR, "xsd/binaryFileProfile.xsd");
    private static final String OUTPUT = "<report><item kind=\"a\">x</item></report>";

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    @Test
    public void validationIsSkippedWhenConditionDoesNotHold() throws Exception {
        String profile = profile(
                validation("Jen pro b", "<condition><xpath>//item/@kind = 'b'</xpath></condition>", "//missing", "<mustExist/>"),
                validation("Jen pro a", "<condition><xpath>//item/@kind = 'a'</xpath></condition>", "//missing", "<mustExist/>"));
        assertEquals(Collections.singletonList("Jen pro a: kontrolovaná hodnota nenalezena"), validate(profile, OUTPUT));
    }

    @Test
    public void conditionOnMissingNodeIsFalseAndNotOfItIsTrue() throws Exception {
        String profile = profile(
                validation("Bez uzlu", "<condition><xpath>//nothing</xpath></condition>", "//missing", "<mustExist/>"),
                validation("Negace", "<condition><xpath>not(//nothing)</xpath></condition>", "//missing", "<mustExist/>"));
        assertEquals(Collections.singletonList("Negace: kontrolovaná hodnota nenalezena"), validate(profile, OUTPUT));
    }

    @Test
    public void messageReplacesRuleTextForEachRuleType() throws Exception {
        String profile = profile(
                validation("Neexistuje", "", "//missing", "<mustExist level=\"WARNING\" message=\"vlastní text 1\"/>"),
                validation("Existuje", "", "//item", "<mustNotExist message=\"vlastní text 2\"/>"),
                validation("Hodnota", "", "//item", "<mustMatchAny message=\"vlastní text 3\"><isExactly>y</isExactly></mustMatchAny>"),
                validation("Bez textu", "", "//item", "<mustMatchAny><isExactly>y</isExactly></mustMatchAny>"));
        assertEquals(Arrays.asList(
                "WARNING: Neexistuje: vlastní text 1",
                "Existuje: vlastní text 2",
                "Hodnota: vlastní text 3",
                "Bez textu: hodnota \"x\" neodpovídá žádnému z omezení: [musí být přesně 'y']"), validateProblems(profile, OUTPUT));
    }

    @Test
    public void invalidConditionXpathIsReportedAsExtractionProblem() throws Exception {
        String profile = profile(validation("Vadná podmínka", "<condition><xpath>//item[</xpath></condition>", "//item", "<mustExist/>"));
        List<String> problems = validate(profile, OUTPUT);
        assertEquals(problems.toString(), 1, problems.size());
        assertTrue(problems.get(0), problems.get(0).startsWith("chyba v zápisu Xpath podmínky '//item['"));
    }

    @Test
    public void conditionAndMessageAreValidBySchema() throws Exception {
        String profile = profile(validation("A", "<condition><xpath>true()</xpath></condition>", "//item",
                "<mustExist level=\"WARNING\" message=\"m\"/><mustNotExist message=\"m\"/><mustMatchAny message=\"m\"><isExactly>x</isExactly></mustMatchAny>"));
        SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI).newSchema(XSD).newValidator()
                .validate(new StreamSource(write(profile)));
    }

    @Test
    public void conditionWithoutXpathIsConfigurationError() throws Exception {
        String profile = profile(validation("A", "<condition/>", "//item", "<mustExist/>"));
        try {
            validate(profile, OUTPUT);
            fail("ocekavana chyba konfigurace");
        } catch (ValidatorConfigurationException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("nemá xpath"));
        }
    }

    @Test
    public void conditionInTextProfileIsConfigurationError() throws Exception {
        String profile = "<binaryFileProfile xmlns=\"http://www.nkp.cz/pspValidator/3.0/binaryFileProfile\" dmf=\"x\">"
                + "<fromText><validation name=\"T\"><condition><xpath>true()</xpath></condition>"
                + "<textDataExtraction resultType=\"STRING\"><firstNonempty><regexp>x</regexp></firstNonempty></textDataExtraction>"
                + "<rules><mustExist/></rules></validation></fromText></binaryFileProfile>";
        try {
            new BinaryFileValidator(new CannedOutputExternalUtilManager()).registerProfile(ResourceType.ORIGINAL_COPY_PDF, ExternalUtil.VERAPDF, write(profile));
            fail("ocekavana chyba konfigurace");
        } catch (ValidatorConfigurationException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("jen v profilech fromXml"));
        }
    }

    private static String profile(String... validations) {
        return "<binaryFileProfile xmlns=\"http://www.nkp.cz/pspValidator/3.0/binaryFileProfile\" dmf=\"x\">"
                + "<fromXml>" + String.join("", validations) + "</fromXml></binaryFileProfile>";
    }

    private static String validation(String name, String condition, String xpath, String rules) {
        return "<validation name=\"" + name + "\">" + condition
                + "<xmlDataExtraction resultType=\"STRING\"><firstNonempty><xpath>" + xpath + "</xpath></firstNonempty></xmlDataExtraction>"
                + "<rules>" + rules + "</rules></validation>";
    }

    private List<String> validate(String profileXml, String toolOutput) throws Exception {
        return load(profileXml, toolOutput).validate("pdf", new File("x.pdf"));
    }

    private List<String> validateProblems(String profileXml, String toolOutput) throws Exception {
        List<String> result = new java.util.ArrayList<>();
        for (Validation.Problem problem : load(profileXml, toolOutput).validateProblems("pdf", new File("x.pdf"))) {
            result.add(problem.toString());
        }
        return result;
    }

    private BinaryFileProfile load(String profileXml, String toolOutput) throws Exception {
        CannedOutputExternalUtilManager manager = new CannedOutputExternalUtilManager().withOutput(ExternalUtil.VERAPDF, toolOutput);
        BinaryFileValidator validator = new BinaryFileValidator(manager);
        validator.registerProfile(ResourceType.ORIGINAL_COPY_PDF, ExternalUtil.VERAPDF, write(profileXml));
        return validator.getProfile(ResourceType.ORIGINAL_COPY_PDF, ExternalUtil.VERAPDF);
    }

    private File write(String content) throws Exception {
        File file = tmp.newFile();
        Files.write(file.toPath(), content.getBytes(StandardCharsets.UTF_8));
        return file;
    }
}
