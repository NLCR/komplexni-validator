package nkp.pspValidator.shared.engine;

import nkp.pspValidator.shared.XmlUtils;
import nkp.pspValidator.shared.engine.validationFunctions.ValidationProblem;
import nkp.pspValidator.shared.engine.validationFunctions.ValidationResult;
import nkp.pspValidator.shared.metadataProfile.DictionaryManager;
import nkp.pspValidator.shared.metadataProfile.MetadataProfile;
import nkp.pspValidator.shared.metadataProfile.MetadataProfileParser;
import nkp.pspValidator.shared.metadataProfile.MetadataProfileValidator;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.w3c.dom.Document;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Issues Validatoru e-publikaci prevedena do KV (label VEP), ktera se tykaji bibliografickych profilu: zaznam se
 * validuje primo proti profilu z fDMF (bez celeho balicku).
 * <ul>
 *     <li>KV #262 (VEP #14): zivotopisna data autora na urovni TITLE e-monografii 3.0 (RDA) bez vzoru RRRR-RRRR</li>
 *     <li>KV #263 (VEP #15): atribut objectPart elementu language je volitelny (DMF 3.0 kap. 3.4.1: O)</li>
 *     <li>KV #264 (VEP #16): languageOfCataloging je doporuceny (DMF 3.0 kap. 3.4.1: R)</li>
 *     <li>KV #265 (VEP #17): dc:language a dc:format na urovni TITLE e-periodik 2.5 (RDA), DMF 2.5 kap. 3.4.1</li>
 * </ul>
 */
public class EpubProfileIssuesTest {

    private static final String MODS_TITLE = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<mods:mods xmlns:mods=\"http://www.loc.gov/mods/v3\" ID=\"MODS_TITLE_0001\">\n"
            + "  <mods:titleInfo><mods:title>Souborné dílo</mods:title></mods:titleInfo>\n"
            + "  <mods:name type=\"personal\" usage=\"primary\">\n"
            + "    <mods:namePart type=\"family\">Novák</mods:namePart>\n"
            + "    <mods:namePart type=\"given\">Jan</mods:namePart>\n"
            + "    <mods:namePart type=\"date\">%s</mods:namePart>\n"
            + "    <mods:role><mods:roleTerm type=\"code\" authority=\"marcrelator\">aut</mods:roleTerm></mods:role>\n"
            + "  </mods:name>\n"
            + "  <mods:language><mods:languageTerm type=\"code\" authority=\"iso639-2b\">cze</mods:languageTerm></mods:language>\n"
            + "  <mods:originInfo eventType=\"publication\"><mods:dateIssued>2020</mods:dateIssued></mods:originInfo>\n"
            + "  <mods:genre>electronic title</mods:genre>\n"
            + "  <mods:physicalDescription><mods:form authority=\"rdamedia\">počítač</mods:form></mods:physicalDescription>\n"
            + "  <mods:identifier type=\"uuid\">5e0b0001-0000-4000-8000-000000000001</mods:identifier>\n"
            + "  <mods:recordInfo>\n"
            + "    <mods:descriptionStandard>rda</mods:descriptionStandard>\n"
            + "    <mods:recordCreationDate encoding=\"iso8601\">20200101T1200</mods:recordCreationDate>\n"
            + "  </mods:recordInfo>\n"
            + "</mods:mods>\n";

    private static final String DC_TITLE = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<oai_dc:dc xmlns:oai_dc=\"http://www.openarchives.org/OAI/2.0/oai_dc/\" xmlns:dc=\"http://purl.org/dc/elements/1.1/\">\n"
            + "  <dc:title>A / Věda a výzkum</dc:title>\n"
            + "  <dc:type>model:electronicperiodical</dc:type>\n"
            + "  <dc:date>2020</dc:date>\n"
            + "  <dc:identifier>uuid:5e0b0002-0000-4000-8000-000000000002</dc:identifier>\n"
            + "  <dc:language>cze</dc:language>\n"
            + "  <dc:format>počítač</dc:format>\n"
            + "</oai_dc:dc>\n";

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    /**
     * @return hlasky urovne WARNING a ERROR (INFO o doporucenych elementech se nepocitaji)
     */
    private List<String> problems(String fdmf, String format, String profile, String xml) throws Exception {
        File configDir = FdmfTestSupport.VALIDATOR_CONFIG_DIR;
        File profileFile = new File(configDir, "fDMF/" + fdmf + "/biblioProfiles/" + format + "/" + profile + ".xml");
        assertTrue(profileFile.getPath(), profileFile.isFile());
        MetadataProfileParser parser = new MetadataProfileParser(new DictionaryManager(new File(configDir, "dictionaries")));
        MetadataProfile metadataProfile = parser.parseProfile(profileFile);
        File file = tmp.newFile();
        Files.write(file.toPath(), xml.getBytes(StandardCharsets.UTF_8));
        Document doc = XmlUtils.buildDocumentFromFile(file, true);
        ValidationResult result = new ValidationResult();
        MetadataProfileValidator.validate(metadataProfile, file, doc, result, null);
        List<String> found = new ArrayList<>();
        for (ValidationProblem problem : result.getProblems()) {
            if (problem.getLevel() != Level.INFO) {
                found.add(problem.getLevel() + ": " + problem.getFullMessage());
            }
        }
        return found;
    }

    @Test
    public void emo30TitleRdaLifeDatesAsInCatalogue() throws Exception {
        for (String dates : new String[]{"1976-", "-1989", "1920-1989", "asi 1850-1900", "činný 1650-1670"}) {
            List<String> problems = problems("emonograph_3.0", "mods", "title_rda", String.format(MODS_TITLE, dates));
            assertEquals(dates + ": " + problems, 0, problems.size());
        }
    }

    @Test
    public void emo30TitleRdaObjectPartAndLanguageOfCatalogingOptional() throws Exception {
        // zaznam nema atribut objectPart u language ani element languageOfCataloging
        String without = String.format(MODS_TITLE, "1920-1989");
        assertFalse(without.contains("objectPart") || without.contains("languageOfCataloging"));
        List<String> problems = problems("emonograph_3.0", "mods", "title_rda", without);
        assertEquals(problems.toString(), 0, problems.size());
        // a s nimi (objectPart="translation", languageOfCataloging s kodem jazyka) projde take
        String with = without
                .replace("<mods:language>", "<mods:language objectPart=\"translation\">")
                .replace("  </mods:recordInfo>", "    <mods:languageOfCataloging><mods:languageTerm authority=\"iso639-2b\">cze</mods:languageTerm></mods:languageOfCataloging>\n  </mods:recordInfo>");
        assertTrue(with.contains("objectPart=\"translation\"") && with.contains("languageOfCataloging"));
        problems = problems("emonograph_3.0", "mods", "title_rda", with);
        assertEquals(problems.toString(), 0, problems.size());
    }

    @Test
    public void emo30TitleRdaStillRequiresMandatoryElements() throws Exception {
        // kontrola, ze profil zaznam opravdu validuje: bez povinneho titleInfo je chyba
        String withoutTitle = String.format(MODS_TITLE, "1920-1989")
                .replace("  <mods:titleInfo><mods:title>Souborné dílo</mods:title></mods:titleInfo>\n", "");
        List<String> problems = problems("emonograph_3.0", "mods", "title_rda", withoutTitle);
        assertEquals(problems.toString(), 1, problems.size());
        assertTrue(problems.get(0), problems.get(0).startsWith("ERROR: ") && problems.get(0).contains("mods:titleInfo"));
    }

    @Test
    public void epe25TitleRdaDcLanguageAndFormat() throws Exception {
        List<String> problems = problems("eperiodical_2.5", "dc", "title_rda", DC_TITLE);
        assertEquals(problems.toString(), 0, problems.size());
    }

    @Test
    public void epe25TitleRdaDcUnexpectedElementStillReported() throws Exception {
        String withRights = DC_TITLE.replace("  <dc:format>počítač</dc:format>\n", "  <dc:format>počítač</dc:format>\n  <dc:rights>volné</dc:rights>\n");
        List<String> problems = problems("eperiodical_2.5", "dc", "title_rda", withRights);
        assertEquals(problems.toString(), 1, problems.size());
        assertTrue(problems.get(0), problems.get(0).contains("neočekávaný element 'dc:rights'"));
    }
}
