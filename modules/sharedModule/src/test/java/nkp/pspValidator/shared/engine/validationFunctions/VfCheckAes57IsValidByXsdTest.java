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
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Validace AES57 zaznamu v SECONDARY-METS-AUDIO podle aes57_1.0.xsd. XSD z fDMF audio_gram_1.0 (vcetne importu), vzorovy zaznam metadata_samples/aes-sample.xml.
 */
public class VfCheckAes57IsValidByXsdTest {

    private static final String VF_NAME = "checkAes57IsValidByXsd";
    private static final File SAMPLES = new File("src/test/resources/metadata_samples");
    private static final File XSD = new File("src/main/resources/nkp/pspValidator/shared/validatorConfig/fDMF/audio_gram_1.0/xsd/aes57_1.0.xsd");

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private Engine engine;

    @Before
    public void setup() {
        engine = new Engine(null);
        engine.defineNamespace("mets", "http://www.loc.gov/METS/");
        engine.defineNamespace("aes57", "http://www.aes.org/audioObject");
    }

    private String sample() throws Exception {
        String s = new String(Files.readAllBytes(new File(SAMPLES, "aes-sample.xml").toPath()), StandardCharsets.UTF_8);
        return s.replaceFirst("^\\s*<\\?xml[^>]*\\?>", "");
    }

    private File mets(String aes57Xml) throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<mets:mets xmlns:mets=\"http://www.loc.gov/METS/\"><mets:amdSec>"
                + "<mets:techMD ID=\"AES57_0001\"><mets:mdWrap MDTYPE=\"OTHER\"><mets:xmlData>" + aes57Xml
                + "</mets:xmlData></mets:mdWrap></mets:techMD></mets:amdSec></mets:mets>";
        File f = tmp.newFile();
        Files.write(f.toPath(), xml.getBytes(StandardCharsets.UTF_8));
        return f;
    }

    private ValidationResult validate(File xsd, File... metsFiles) throws Exception {
        List<File> list = Arrays.asList(metsFiles);
        return engine.buildValidationFunction(VF_NAME)
                .withValueParam("xsd_file", ValueType.FILE, new ValueEvaluation(xsd))
                .withValueParam("mets_files", ValueType.FILE_LIST, new ValueEvaluation(list))
                .withValueParam("level", ValueType.LEVEL, new ValueEvaluation(Level.ERROR))
                .validate();
    }

    @Test
    public void validRecord() throws Exception {
        ValidationResult result = validate(XSD, mets(sample()));
        assertFalse(String.valueOf(result.getProblems().isEmpty() ? "" : result.getProblems().get(0).getFullMessage()), result.hasProblems());
    }

    @Test
    public void validRecordInSourceMd() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<mets:mets xmlns:mets=\"http://www.loc.gov/METS/\"><mets:amdSec>"
                + "<mets:sourceMD ID=\"AES57_0002\"><mets:mdWrap MDTYPE=\"OTHER\"><mets:xmlData>" + sample()
                + "</mets:xmlData></mets:mdWrap></mets:sourceMD></mets:amdSec></mets:mets>";
        File f = tmp.newFile();
        Files.write(f.toPath(), xml.getBytes(StandardCharsets.UTF_8));
        assertFalse(validate(XSD, f).hasProblems());
    }

    @Test
    public void invalidRecordBrokenElement() throws Exception {
        String broken = sample().replace("<aes:byteOrder>", "<aes:byteOrderBROKEN>").replace("</aes:byteOrder>", "</aes:byteOrderBROKEN>");
        assertNotEquals(sample(), broken);
        ValidationResult result = validate(XSD, mets(broken));
        assertTrue(result.hasProblems());
        ValidationProblem p = result.getProblems().get(0);
        assertEquals(Level.ERROR, p.getLevel());
        assertTrue(p.getFullMessage(), p.getFullMessage().contains("AES57_0001"));
    }

    @Test
    public void techMdWithoutAudioObject() throws Exception {
        ValidationResult result = validate(XSD, mets("<other/>"));
        assertEquals(1, result.getProblems().size());
        assertTrue(result.getProblems().get(0).getFullMessage().contains("nenalezen element"));
    }

    @Test
    public void techMdWithoutAes57IdPrefixIsIgnored() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<mets:mets xmlns:mets=\"http://www.loc.gov/METS/\"><mets:amdSec>"
                + "<mets:techMD ID=\"OBJ_001\"><mets:mdWrap><mets:xmlData><junk/></mets:xmlData></mets:mdWrap></mets:techMD>"
                + "</mets:amdSec></mets:mets>";
        File f = tmp.newFile();
        Files.write(f.toPath(), xml.getBytes(StandardCharsets.UTF_8));
        assertFalse(validate(XSD, f).hasProblems());
    }

    @Test
    public void missingMetsFile() throws Exception {
        File missing = new File(tmp.getRoot(), "neexistuje.xml");
        ValidationResult result = validate(XSD, missing);
        assertEquals(1, result.getProblems().size());
        assertEquals(Level.ERROR, result.getProblems().get(0).getLevel());
    }

    @Test
    public void malformedMetsFile() throws Exception {
        File f = tmp.newFile();
        Files.write(f.toPath(), "<mets:mets xmlns:mets=\"http://www.loc.gov/METS/\"><nezavreno>".getBytes(StandardCharsets.UTF_8));
        ValidationResult result = validate(XSD, f);
        assertEquals(1, result.getProblems().size());
    }

    @Test
    public void emptyMetsFileList() throws Exception {
        assertFalse(validate(XSD).hasProblems());
    }

    @Test
    public void nonexistentXsd() throws Exception {
        ValidationResult result = validate(new File(tmp.getRoot(), "neexistuje.xsd"), mets(sample()));
        assertEquals(1, result.getProblems().size());
        assertTrue(result.getProblems().get(0).getFullMessage().contains("neexistuje"));
    }

    @Test
    public void xsdIsDirectory() throws Exception {
        ValidationResult result = validate(SAMPLES, mets(sample()));
        assertEquals(1, result.getProblems().size());
    }

    @Test
    public void nullXsdGivesUnexpectedError() throws Exception {
        //znama vada: pri null xsd_file je vetev v kodu prazdna, chyba se projevi az jako NPE ("necekana chyba")
        File mets = mets(sample());
        ValidationResult result = validate(null, mets);
        assertEquals(1, result.getProblems().size());
        assertTrue(result.getProblems().get(0).getFullMessage(), result.getProblems().get(0).getFullMessage().contains("nečekaná chyba"));
    }

    @Test
    public void nullMetsFilesParamGivesUnexpectedError() throws Exception {
        ValidationResult result = engine.buildValidationFunction(VF_NAME)
                .withValueParam("xsd_file", ValueType.FILE, new ValueEvaluation(XSD))
                .withValueParam("mets_files", ValueType.FILE_LIST, new ValueEvaluation(null, "chyba"))
                .validate();
        assertEquals(1, result.getProblems().size());
        assertEquals(Level.ERROR, result.getProblems().get(0).getLevel());
    }

    @Test
    public void levelParamIsUsed() throws Exception {
        List<File> list = Collections.singletonList(mets("<other/>"));
        ValidationResult result = engine.buildValidationFunction(VF_NAME)
                .withValueParam("xsd_file", ValueType.FILE, new ValueEvaluation(XSD))
                .withValueParam("mets_files", ValueType.FILE_LIST, new ValueEvaluation(list))
                .withValueParam("level", ValueType.LEVEL, new ValueEvaluation(Level.WARNING))
                .validate();
        assertEquals(Level.WARNING, result.getProblems().get(0).getLevel());
    }
}
