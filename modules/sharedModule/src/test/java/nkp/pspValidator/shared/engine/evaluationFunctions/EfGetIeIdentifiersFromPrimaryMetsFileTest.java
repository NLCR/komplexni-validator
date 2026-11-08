package nkp.pspValidator.shared.engine.evaluationFunctions;

import nkp.pspValidator.shared.engine.Engine;
import nkp.pspValidator.shared.engine.ValueEvaluation;
import nkp.pspValidator.shared.engine.ValueType;
import nkp.pspValidator.shared.engine.types.Identifier;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * Funkce je @Deprecated (nahrazena selectIeIdentifiersFromPrimaryMetsFile), ale pouziva ji dosud nekolik fDMF
 * (monograph_1.3.2, periodical_1.4, data_disc_0.1 a dalsi), proto je fixovano jeji soucasne chovani.
 */
public class EfGetIeIdentifiersFromPrimaryMetsFileTest {

    private static final String FUNCTION_NAME = "getIeIdentifiersFromPrimaryMetsFile";
    private static final String PARAM_FILE = "primary_mets_file";

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private Engine engine;

    @Before
    public void setup() {
        engine = new Engine(null);
        engine.defineNamespace("mods", "http://www.loc.gov/mods/v3");
        engine.defineNamespace("mets", "http://www.loc.gov/METS/");
    }

    private File mets(String... modsSections) throws Exception {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<mets:mets xmlns:mets=\"http://www.loc.gov/METS/\" xmlns:mods=\"http://www.loc.gov/mods/v3\">\n");
        for (String section : modsSections) {
            sb.append("<mets:dmdSec><mets:mdWrap><mets:xmlData>").append(section).append("</mets:xmlData></mets:mdWrap></mets:dmdSec>\n");
        }
        sb.append("</mets:mets>\n");
        File file = tmp.newFile();
        Files.write(file.toPath(), sb.toString().getBytes(StandardCharsets.UTF_8));
        return file;
    }

    private String mods(String id, String... identifiers) {
        StringBuilder sb = new StringBuilder("<mods:mods ID=\"" + id + "\">");
        for (String identifier : identifiers) {
            sb.append(identifier);
        }
        return sb.append("</mods:mods>").toString();
    }

    private String ident(String type, String value) {
        return "<mods:identifier type=\"" + type + "\">" + value + "</mods:identifier>";
    }

    @SuppressWarnings("unchecked")
    private Set<Identifier> evaluate(File file) {
        ValueEvaluation evaluation = engine.buildEvaluationFunction(FUNCTION_NAME)
                .withValueParam(PARAM_FILE, ValueType.FILE, new ValueEvaluation(file))
                .evaluate();
        assertNull(evaluation.getErrorMessage(), evaluation.getErrorMessage());
        assertNotNull(evaluation.getData());
        return new HashSet<>((List<Identifier>) evaluation.getData());
    }

    @Test
    public void issue() throws Exception {
        File file = mets(mods("MODS_ISSUE_0001", ident("urnnbn", "urn:nbn:cz:abc001-000001"), ident("uuid", "47647030-3fb3-11e7-ad33-5ef3fc9ae867")));
        Set<Identifier> ids = evaluate(file);
        assertEquals(2, ids.size());
        assertTrue(ids.contains(new Identifier("urnnbn", "urn:nbn:cz:abc001-000001")));
        // uuid se vraci s predponou "uuid:"
        assertTrue(ids.contains(new Identifier("uuid", "uuid:47647030-3fb3-11e7-ad33-5ef3fc9ae867")));
    }

    @Test
    public void volume() throws Exception {
        Set<Identifier> ids = evaluate(mets(mods("MODS_VOLUME_0001", ident("ccnb", "cnb001"))));
        assertEquals(1, ids.size());
        assertTrue(ids.contains(new Identifier("ccnb", "cnb001")));
    }

    @Test
    public void suppl() throws Exception {
        Set<Identifier> ids = evaluate(mets(mods("MODS_SUPPL_0001", ident("isbn", " 978-80-1 "))));
        // hodnota se orezava
        assertTrue(ids.contains(new Identifier("isbn", "978-80-1")));
    }

    @Test
    public void onlyIntellectualEntitiesAreUsedAndDuplicatesMerged() throws Exception {
        File file = mets(
                mods("MODS_ISSUE_0001", ident("urnnbn", "urn:nbn:cz:abc001-000001")),
                mods("MODS_VOLUME_0001", ident("urnnbn", "urn:nbn:cz:abc001-000001"), ident("sysno", "1")),
                mods("MODS_TITLE_0001", ident("urnnbn", "urn:nbn:cz:zzz-ignored")),
                mods("MODS_PAGE_0001", ident("uuid", "11111111-1111-1111-1111-111111111111")));
        Set<Identifier> ids = evaluate(file);
        assertEquals(2, ids.size());
        assertTrue(ids.contains(new Identifier("urnnbn", "urn:nbn:cz:abc001-000001")));
        assertTrue(ids.contains(new Identifier("sysno", "1")));
    }

    /**
     * Znama vada: funkce zna jen ID MODS_ISSUE, MODS_SUPPL a MODS_VOLUME. Pro typy jine (MODS_DATACOLLECTION v
     * data_disc_0.1, MODS_UNIT u jednotek fondu) vraci prazdny seznam, ne chybu, takze navazujici pravidla
     * (napr. PSP_ID_DERIVED_FROM_IE_ID) nemaji z ceho kontrolovat. Test fixuje dnesni chovani; az se opravi
     * (napr. prechodem na selectIeIdentifiersFromPrimaryMetsFile), je treba ho zmenit.
     */
    @Test
    public void unknownIntellectualEntityTypesYieldEmptyList_knownDefect() throws Exception {
        assertTrue(evaluate(mets(mods("MODS_DATACOLLECTION_0001", ident("urnnbn", "urn:nbn:cz:abc001-000001")))).isEmpty());
        assertTrue(evaluate(mets(mods("MODS_UNIT_0001", ident("urnnbn", "urn:nbn:cz:abc001-000001")))).isEmpty());
    }

    @Test
    public void noModsGivesEmptyList() throws Exception {
        assertTrue(evaluate(mets()).isEmpty());
    }

    @Test
    public void fileDoesNotExist() {
        ValueEvaluation evaluation = engine.buildEvaluationFunction(FUNCTION_NAME)
                .withValueParam(PARAM_FILE, ValueType.FILE, new ValueEvaluation(new File(tmp.getRoot(), "neexistuje.xml")))
                .evaluate();
        assertNull(evaluation.getData());
        assertNotNull(evaluation.getErrorMessage());
    }

    @Test
    public void fileIsDirectory() {
        ValueEvaluation evaluation = engine.buildEvaluationFunction(FUNCTION_NAME)
                .withValueParam(PARAM_FILE, ValueType.FILE, new ValueEvaluation(tmp.getRoot()))
                .evaluate();
        assertNull(evaluation.getData());
        assertNotNull(evaluation.getErrorMessage());
    }

    @Test
    public void notXml() throws Exception {
        File file = tmp.newFile();
        Files.write(file.toPath(), "neni xml".getBytes(StandardCharsets.UTF_8));
        ValueEvaluation evaluation = engine.buildEvaluationFunction(FUNCTION_NAME)
                .withValueParam(PARAM_FILE, ValueType.FILE, new ValueEvaluation(file))
                .evaluate();
        assertNull(evaluation.getData());
        assertNotNull(evaluation.getErrorMessage());
    }

    @Test
    public void paramMissing() {
        ValueEvaluation evaluation = engine.buildEvaluationFunction(FUNCTION_NAME).evaluate();
        assertNull(evaluation.getData());
    }
}
