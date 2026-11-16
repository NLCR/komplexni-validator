package nkp.pspValidator.shared.engine;

import nkp.pspValidator.shared.Dmf;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.Assert.*;

/**
 * Issue #258 (VEP #3): tvar UUID u e-publikaci. Vsech devet DMF pro e-publikace odkazuje u UUID na RFC 4122, proto
 * se tvar (8-4-4-4-12 hexadecimalnich znaku, velka i mala pismena, bez prefixu "uuid:") kontroluje v MODS profilech
 * vsech deviti fDMF (ERROR) a u hodnot dc:identifier typu uuid (WARNING, DMF tvar hodnoty v DC nepopisuji).
 * <p>
 * Balicek je kopie src/test/resources/epublikace/eperiodical_2.6/valid_pdf_originaldata s upravenym hlavnim METS;
 * kontroluji se jen hlasky tykajici se UUID (zmena METS rozbije jeho kontrolni soucet, to testy neresi).
 */
public class EpubUuidFormatTest {

    private static final File SOURCE = new File("src/test/resources/epublikace/eperiodical_2.6/valid_pdf_originaldata/5e0b0003-0000-4000-8000-000000000261");
    private static final String TITLE_UUID = "5e0b0002-0000-4000-8000-000000000002";
    private static final Dmf EPE_2_6 = new Dmf(Dmf.Type.EPERIODICAL, "2.6");
    private static final String UUID_REGEXP = "^[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}$";
    private static final String[] E_FDMF = {"emonograph_2.3", "emonograph_2.4", "emonograph_3.0", "emonograph_3.1",
            "eperiodical_2.3", "eperiodical_2.4", "eperiodical_2.5", "eperiodical_2.6", "composed_perio_1.3"};

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    /**
     * Kopie balicku, v hlavnim METS nahradi hodnotu MODS a DC identifikatoru titulu.
     */
    private File packageWith(String modsUuid, String dcIdentifier) throws IOException {
        File target = new File(tmp.newFolder(), SOURCE.getName());
        Path src = SOURCE.toPath();
        try (Stream<Path> paths = Files.walk(src)) {
            for (Path p : (Iterable<Path>) paths::iterator) {
                Path t = target.toPath().resolve(src.relativize(p).toString());
                if (Files.isDirectory(p)) {
                    Files.createDirectories(t);
                } else {
                    Files.copy(p, t);
                }
            }
        }
        File mets = new File(target, "mets_" + SOURCE.getName() + ".xml");
        String content = new String(Files.readAllBytes(mets.toPath()), StandardCharsets.UTF_8);
        String modsOld = "<mods:identifier type=\"uuid\">" + TITLE_UUID + "</mods:identifier>";
        String dcOld = "<dc:identifier>uuid:" + TITLE_UUID + "</dc:identifier>";
        assertTrue(content.contains(modsOld) && content.contains(dcOld));
        content = content.replace(modsOld, "<mods:identifier type=\"uuid\">" + modsUuid + "</mods:identifier>")
                .replace(dcOld, "<dc:identifier>" + dcIdentifier + "</dc:identifier>");
        Files.write(mets.toPath(), content.getBytes(StandardCharsets.UTF_8));
        return target;
    }

    private static List<String> uuidProblems(String log) {
        List<String> found = new ArrayList<>();
        for (String line : log.split("\n")) {
            String trimmed = line.trim();
            boolean problem = trimmed.startsWith("ERROR:") || trimmed.startsWith("WARNING:");
            if (problem && (trimmed.contains("ve formátu UUID") || (trimmed.contains("uuid") && trimmed.contains("neodpovídá regulárním")))) {
                found.add(trimmed);
            }
            assertFalse(trimmed, trimmed.contains("nečekaná chyba") || trimmed.contains("nesplněn kontrakt"));
        }
        return found;
    }

    @Test
    public void validUuidHasNoProblems() throws Exception {
        String log = FdmfTestSupport.run(EPE_2_6, packageWith(TITLE_UUID, "uuid:" + TITLE_UUID));
        assertEquals(new ArrayList<String>(), uuidProblems(log));
    }

    @Test
    public void upperCaseUuidIsAccepted() throws Exception {
        String upper = TITLE_UUID.toUpperCase();
        String log = FdmfTestSupport.run(EPE_2_6, packageWith(upper, "uuid:" + upper));
        assertEquals(new ArrayList<String>(), uuidProblems(log));
    }

    @Test
    public void invalidUuidInModsIsError() throws Exception {
        // prilis kratke a s ne-hexa znakem (pripady z issue: spatny pocet znaku, ne-hexa znaky)
        for (String invalid : new String[]{"5e0b0002-0000-4000-8000-00000000000", "5e0b0002-0000-4000-8000-00000000000g"}) {
            List<String> problems = uuidProblems(FdmfTestSupport.run(EPE_2_6, packageWith(invalid, "uuid:" + TITLE_UUID)));
            assertEquals(problems.toString(), 1, problems.size());
            assertTrue(problems.get(0), problems.get(0).startsWith("ERROR:") && problems.get(0).contains("hodnota '" + invalid + "' neodpovídá regulárním"));
        }
    }

    @Test
    public void uuidPrefixInModsIsError() throws Exception {
        List<String> problems = uuidProblems(FdmfTestSupport.run(EPE_2_6, packageWith("uuid:" + TITLE_UUID, "uuid:" + TITLE_UUID)));
        assertEquals(problems.toString(), 1, problems.size());
        assertTrue(problems.get(0), problems.get(0).startsWith("ERROR:") && problems.get(0).contains("hodnota 'uuid:" + TITLE_UUID + "' neodpovídá regulárním"));
    }

    @Test
    public void invalidUuidInDcIsWarning() throws Exception {
        String invalid = "uuid:5e0b0002-0000-4000-8000-00000000000g";
        String log = FdmfTestSupport.run(EPE_2_6, packageWith(TITLE_UUID, invalid));
        List<String> problems = uuidProblems(log);
        assertEquals(problems.toString(), 1, problems.size());
        assertEquals("WARNING: identifikátor \"" + invalid + "\" typu uuid nemá hodnotu ve formátu UUID", problems.get(0));
        assertEquals(problems, RepoPackagesSupport.problemsOfRule(log, "DC_IDENTIFIERS_CONTAIN_TYPE_AND_VALUE"));
    }

    /**
     * Vsechny MODS profily vsech deviti e-fDMF kontroluji tvar UUID stejnym vzorem a pravidlo DC ma zapnutou kontrolu.
     */
    @Test
    public void allEPublicationFdmfCheckUuidFormat() throws Exception {
        Pattern uuidElement = Pattern.compile("<element name=\"mods:identifier\"\\s+specification=\"@type\\s*=\\s*'uuid'\"[^>]*>(.*?)</element>", Pattern.DOTALL);
        int profiles = 0;
        for (String fdmf : E_FDMF) {
            File dir = new File(FdmfTestSupport.VALIDATOR_CONFIG_DIR, "fDMF/" + fdmf);
            File[] mods = new File(dir, "biblioProfiles/mods").listFiles((d, n) -> n.endsWith(".xml"));
            assertNotNull(fdmf, mods);
            assertTrue(fdmf, mods.length >= 10);
            for (File profile : mods) {
                String content = new String(Files.readAllBytes(profile.toPath()), StandardCharsets.UTF_8);
                Matcher m = uuidElement.matcher(content);
                assertTrue(profile.getPath(), m.find());
                assertTrue(profile.getPath(), m.group(1).contains("<regexp>" + UUID_REGEXP + "</regexp>"));
                profiles++;
            }
            String rules = new String(Files.readAllBytes(new File(dir, "rules.xml").toPath()), StandardCharsets.UTF_8);
            assertTrue(fdmf, Pattern.compile("functionName=\"checkDcIdentifiersHaveTypeAndValue\">.*?<value name=\"check_uuid_format\" type=\"BOOLEAN\">true</value>", Pattern.DOTALL).matcher(rules).find());
        }
        assertEquals(92, profiles);
    }
}
