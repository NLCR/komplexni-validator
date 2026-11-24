package nkp.pspValidator.shared.externalUtils.validation;

import nkp.pspValidator.shared.engine.Level;
import nkp.pspValidator.shared.externalUtils.ExternalUtil;
import nkp.pspValidator.shared.externalUtils.ExternalUtilsTestSupport;
import nkp.pspValidator.shared.externalUtils.ExternalUtilsTestSupport.CannedOutputExternalUtilManager;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static nkp.pspValidator.shared.externalUtils.ExternalUtilsTestSupport.loadProfile;
import static nkp.pspValidator.shared.externalUtils.ExternalUtilsTestSupport.output;
import static org.junit.Assert.*;

/**
 * Profil veraPDF všech e-born fDMF: soubor, který se k PDF/A nehlásí, je jen WARNING „Formát PDF/A“ (DMF e-monografií
 * a e-periodik, kap. 1.1: formáty .epub, PDF a PDF/A, doporučené PDF/A-1 a PDF/A-2); soubor, který se k PDF/A hlásí
 * a nevyhovuje, zůstává ERROR. Poškozené a šifrované PDF jako před změnou (dvě chyby „kontrolovaná hodnota nenalezena“).
 * <p>
 * Výstupy jsou skutečné výstupy veraPDF 1.30 (src/test/resources/externalUtils/verapdf-pdfa/README.md
 * a externalUtils/verapdf/README.md). Očekávané texty jsou určené ručně ze zadání (rozhodnutí 24. 11. 2026)
 * a z textů pravidel profilu.
 */
public class VeraPdfPdfaClaimTest {

    private static final List<String> EBORN_FDMF = Arrays.asList(
            "emonograph_2.3", "emonograph_2.4", "emonograph_3.0", "emonograph_3.1",
            "eperiodical_2.3", "eperiodical_2.4", "eperiodical_2.5", "eperiodical_2.6", "composed_perio_1.3");
    private static final String DIR = "verapdf-pdfa";
    private static final File DUMMY_FILE = new File("x.pdf");

    private static final String NOT_PDFA = "WARNING: Formát PDF/A: soubor není ve formátu PDF/A (DMF doporučuje PDF/A-1 nebo PDF/A-2)";
    private static final String NOT_COMPLIANT = "ERROR: Soubor musí být validní: hodnota \"false\" neodpovídá žádnému z omezení: [musí být přesně 'true']";
    private static final List<String> NOT_PROCESSED = Arrays.asList(
            "ERROR: Soubor musí být validní: kontrolovaná hodnota nenalezena",
            "ERROR: Soubor PDF/A by měl být v podporované verzi: kontrolovaná hodnota nenalezena");

    @Test
    public void everyEbornFdmfHasVeraPdfProfileAndErrorLevelInRules() throws Exception {
        for (String fdmf : EBORN_FDMF) {
            assertTrue(fdmf, ExternalUtilsTestSupport.profileFile(fdmf, "pdf", ExternalUtil.VERAPDF).isFile());
            assertRulesXmlLevelIsError(fdmf);
        }
    }

    // (1) PDF/A validni

    @Test
    public void compliantPdfAHasNoProblem() throws Exception {
        for (String fdmf : EBORN_FDMF) {
            assertEquals(fdmf, Collections.emptyList(), problems(fdmf, "verapdf", "compliant_PDFA-1B.xml"));
            assertEquals(fdmf, Collections.emptyList(), problems(fdmf, "verapdf", "compliant_PDFA-2B.xml"));
        }
    }

    // (2) hlasi se k PDF/A a nevyhovuje

    @Test
    public void claimedPdfAThatFailsIsError() throws Exception {
        // vzorek NK e-periodika 2.4: pdfaid:part 2, conformance B, porusene jen 6.2.4.4 (ISO 19005-2)
        for (String fdmf : EBORN_FDMF) {
            assertEquals(fdmf, Collections.singletonList(NOT_COMPLIANT),
                    problems(fdmf, DIR, "hlasi_se_pdfa_2b_nevyhovuje_eperiodika_2.4.xml"));
            // PDF/A-1b sifrovane s prazdnym heslem: identifikaci PDF/A veraPDF precte, porusene 6.7.2 test 2 (ne test 1)
            assertEquals(fdmf, Collections.singletonList(NOT_COMPLIANT),
                    problems(fdmf, DIR, "sifrovane_bez_hesla_pdfa_1b.xml"));
        }
    }

    @Test
    public void claimedPdfA2bWithBadMetadataDictionaryIsError() throws Exception {
        // synteticky: PDF/A-2b, ve slovniku metadat Type /Metadatx; profil PDF/A-2b, pravidla ISO 19005-2 -> hlasi se
        for (String fdmf : EBORN_FDMF) {
            assertEquals(fdmf, Collections.singletonList(NOT_COMPLIANT),
                    problems(fdmf, DIR, "synteticky_hlasi_se_pdfa_2b_metadata_bez_type.xml"));
        }
    }

    // (3) nehlasi se k PDF/A

    @Test
    public void pdfWithXmpWithoutPdfaIdentificationIsOnlyWarning() throws Exception {
        // vzorky NK e-monografie a e-periodika 2.3: XMP bez pdfaid, veraPDF zvoli PDF/A-1b, porusene 6.7.11 test 1
        // a tisice dalsich pravidel; zadna hlaska o validite ani o nepodporovane verzi
        for (String fdmf : EBORN_FDMF) {
            assertEquals(fdmf, Collections.singletonList(NOT_PDFA), problems(fdmf, DIR, "nehlasi_se_xmp_bez_pdfaid_eperiodika_2.3.xml"));
            assertEquals(fdmf, Collections.singletonList(NOT_PDFA), problems(fdmf, DIR, "nehlasi_se_xmp_bez_pdfaid_emonografie_2.3.xml"));
        }
    }

    @Test
    public void pdfWithoutXmpIsOnlyWarning() throws Exception {
        // synteticky_bez_pdfa.pdf z test resources a dummy.pdf z VEP: katalog bez Metadata, porusene 6.7.2 test 1
        for (String fdmf : EBORN_FDMF) {
            assertEquals(fdmf, Collections.singletonList(NOT_PDFA), problems(fdmf, DIR, "nehlasi_se_bez_xmp.xml"));
            assertEquals(fdmf, Collections.singletonList(NOT_PDFA), problems(fdmf, "verapdf", "noncompliant_dummy.xml"));
        }
    }

    @Test
    public void knownLimitPdfA1WithBadMetadataDictionaryLooksNotClaimed() throws Exception {
        // mez detekce (komentar v profilu): PDF/A-1b se spatnym Type slovniku metadat da totez 6.7.2 test 1 jako soubor
        // bez XMP, profil ho proto bere jako nehlasici se (WARNING); synteticky vystup
        for (String fdmf : EBORN_FDMF) {
            assertEquals(fdmf, Collections.singletonList(NOT_PDFA),
                    problems(fdmf, DIR, "synteticky_hlasi_se_pdfa_1b_metadata_bez_type.xml"));
        }
    }

    // (4) poskozene nebo necitelne PDF, (5) sifrovane PDF: beze zmeny

    @Test
    public void unreadablePdfIsErrorAsBefore() throws Exception {
        for (String fdmf : EBORN_FDMF) {
            assertEquals(fdmf, NOT_PROCESSED, problems(fdmf, DIR, "poskozene_useknute.xml"));
            assertEquals(fdmf, NOT_PROCESSED, problems(fdmf, "verapdf", "empty.xml"));
            assertEquals(fdmf, NOT_PROCESSED, problems(fdmf, "verapdf", "not_pdf.xml"));
        }
    }

    @Test
    public void encryptedPdfIsErrorAsBefore() throws Exception {
        // veraPDF kod 8, taskException "The PDF stream appears to be encrypted", bez validationReport
        for (String fdmf : EBORN_FDMF) {
            assertEquals(fdmf, NOT_PROCESSED, problems(fdmf, DIR, "sifrovane_s_heslem.xml"));
        }
    }

    private static List<String> problems(String fdmf, String dir, String outputName) throws Exception {
        CannedOutputExternalUtilManager manager = new CannedOutputExternalUtilManager()
                .withOutput(ExternalUtil.VERAPDF, output(dir, outputName));
        BinaryFileProfile profile = loadProfile(manager, fdmf, "pdf", ExternalUtil.VERAPDF);
        List<String> result = new ArrayList<>();
        for (Validation.Problem problem : profile.validateProblems("pdf", DUMMY_FILE)) {
            // uroven null = validation_problem_level z rules.xml, ktery pravidlo veraPDF nema (ERROR)
            Level level = problem.getLevel() == null ? Level.ERROR : problem.getLevel();
            result.add(level + ": " + problem.getMessage());
        }
        return result;
    }

    private static void assertRulesXmlLevelIsError(String fdmf) throws Exception {
        File rulesFile = new File(ExternalUtilsTestSupport.FDMF_DIR, fdmf + "/rules.xml");
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        Document doc = factory.newDocumentBuilder().parse(rulesFile);
        NodeList values = doc.getElementsByTagNameNS("*", "value");
        int veraPdfRules = 0;
        for (int i = 0; i < values.getLength(); i++) {
            Element value = (Element) values.item(i);
            if ("util".equals(value.getAttribute("name")) && "VERAPDF".equals(value.getTextContent().trim())) {
                veraPdfRules++;
                Element params = (Element) value.getParentNode();
                NodeList paramValues = params.getElementsByTagNameNS("*", "value");
                for (int j = 0; j < paramValues.getLength(); j++) {
                    assertNotEquals(fdmf, "validation_problem_level", ((Element) paramValues.item(j)).getAttribute("name"));
                }
            }
        }
        assertTrue(fdmf + ": žádné pravidlo veraPDF", veraPdfRules > 0);
    }
}
