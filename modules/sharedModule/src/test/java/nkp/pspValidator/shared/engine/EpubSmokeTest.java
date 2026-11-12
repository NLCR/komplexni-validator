package nkp.pspValidator.shared.engine;

import nkp.pspValidator.shared.Dmf;
import nkp.pspValidator.shared.DmfDetector;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Smoke testy fDMF elektronickych publikaci (e-monografie, e-periodika, skladana e-periodika) nad syntetickymi balicky
 * ze src/test/resources/epublikace, vygenerovanymi src/test/tools/gen_epub_package.py (prikazy v README.txt).
 * Pozitivni balicky musi byt validni (zadny ERROR), mutace maji ocekavane pravidlo a fragment hlasky.
 * Externi nastroje (veraPDF, EPUBCheck) jsou vypnute a Resolver URN:NBN se nevola (FdmfTestSupport.run).
 */
public class EpubSmokeTest {

    private static final String RES = "src/test/resources/epublikace/";

    // ---- pozitivni balicky

    @Test
    public void emo30PdfIsValid() throws Exception {
        assertValid(run("emonograph_3.0/valid_pdf/aba007-p5m301", Dmf.Type.EMONOGRAPH, "3.0"));
    }

    @Test
    public void emo30EpubIsValid() throws Exception {
        assertValid(run("emonograph_3.0/valid_epub/aba007-p5m302", Dmf.Type.EMONOGRAPH, "3.0"));
    }

    /**
     * DMF e-monografie 3.1: nepovinna slozka originaldata (od_*.pdf, conv_*.xml) v md5 i itemlistu (VEP #19).
     */
    @Test
    public void emo31EpubWithOriginaldataIsValid() throws Exception {
        assertValid(run("emonograph_3.1/valid_epub_originaldata/aba007-p5m311", Dmf.Type.EMONOGRAPH, "3.1"));
    }

    /**
     * Profily emonograph_2.3 a 2.4 volume_*_rda mely z VEP u mods:titleInfo atribut type povinny, DMF e-monografie 2.3
     * (kap. 5.4.3, RDA) rika "hlavni nazev bez type"; opraveno 12. 11. 2026, balicek podle DMF je validni.
     */
    @Test
    public void emo23RdaPdfIsValid() throws Exception {
        assertValid(run("emonograph_2.3/valid_pdf_rda/aba007-p5m231", Dmf.Type.EMONOGRAPH, "2.3"));
    }

    @Test
    public void epe25EpubIsValid() throws Exception {
        assertValid(run("eperiodical_2.5/valid_epub/5e0b0003-0000-4000-8000-000000000251", Dmf.Type.EPERIODICAL, "2.5"));
    }

    @Test
    public void epe26PdfWithOriginaldataIsValid() throws Exception {
        assertValid(run("eperiodical_2.6/valid_pdf_originaldata/5e0b0003-0000-4000-8000-000000000261", Dmf.Type.EPERIODICAL, "2.6"));
    }

    /**
     * Skladane e-periodikum 1.3, varianta A: dva clanky v PDF, amdsec s vedlejsimi METS (PREMIS objekt, udalost,
     * agent), originaldata k prvnimu clanku, dve strukturalni mapy a structLink.
     */
    @Test
    public void sep13VariantAWithOriginaldataIsValid() throws Exception {
        assertValid(run("composed_perio_1.3/valid_a_pdf_originaldata/5e0b0003-0000-4000-8000-000000000131", Dmf.Type.COMPERIODICAL, "1.3"));
    }

    // ---- mutace: opravy z portu VEP (P4, P4b, P4c), ktere maji zustat validni

    /**
     * VEP #20: dmdSec prilohy (MODSMD_SUPPL, DCMD_SUPPL) hlavicka METS pripousti, priloha se kontroluje profilem
     * supplement bez chyb i bez varovani.
     */
    @Test
    public void emo31SupplementDmdSecIsValid() throws Exception {
        String log = run("emonograph_3.1/supplement_dmdsec/aba007-p5m313", Dmf.Type.EMONOGRAPH, "3.1");
        assertValid(log);
        assertNoLine(log, "WARNING:", "SUPPL");
    }

    /**
     * VEP #18: v RDA je dc:creator svazku nepovinny.
     */
    @Test
    public void emo31VolumeWithoutDcCreatorIsValid() throws Exception {
        String log = run("emonograph_3.1/volume_no_dc_creator/aba007-p5m314", Dmf.Type.EMONOGRAPH, "3.1");
        assertValid(log);
        assertNoLine(log, "WARNING:", "dc:creator");
    }

    /**
     * VEP #18 i v emonograph_2.3: svazek bez dc:creator (RDA) je validni a zadne pravidlo dc:creator nehlasi.
     */
    @Test
    public void emo23RdaVolumeWithoutDcCreatorIsValid() throws Exception {
        String log = run("emonograph_2.3/volume_no_dc_creator_rda/aba007-p5m232", Dmf.Type.EMONOGRAPH, "2.3");
        assertValid(log);
        assertNoLine(log, "WARNING:", "dc:creator");
    }

    /**
     * DMF 3.1 kap. 3.1.2 (archivni kopie "napr. ve formatu EPUB nebo PDF/A") kombinaci PDF a EPUB nezakazuje.
     */
    @Test
    public void emo31PdfAndEpubTogetherIsValid() throws Exception {
        assertValid(run("emonograph_3.1/pdf_and_epub_both/aba007-p5m315", Dmf.Type.EMONOGRAPH, "3.1"));
    }

    /**
     * DMF e-periodika 2.6 (zmenovy list): URN:NBN na urovni VOLUME je povoleny.
     */
    @Test
    public void epe26VolumeUrnNbnIsValid() throws Exception {
        String log = run("eperiodical_2.6/volume_urnnbn/5e0b0003-0000-4000-8000-000000000264", Dmf.Type.EPERIODICAL, "2.6");
        assertValid(log);
        assertNoLine(log, "WARNING:", "urnnbn");
    }

    /**
     * DMF skladana e-periodika 1.3 kap. 5.4.2: URN:NBN na urovni VOLUME je povoleny.
     */
    @Test
    public void sep13VolumeUrnNbnIsValid() throws Exception {
        String log = run("composed_perio_1.3/volume_urnnbn/5e0b0003-0000-4000-8000-000000000133", Dmf.Type.COMPERIODICAL, "1.3");
        assertValid(log);
        assertNoLine(log, "WARNING:", "urnnbn");
    }

    /**
     * fileGrp OC_EBGRP misto OC_EBSFL: DMF 1.3 je nejednoznacna (N2), profil hlasi jen varovani.
     */
    @Test
    public void sep13OcFileGrpEbgrpWarns() throws Exception {
        String log = run("composed_perio_1.3/oc_filegrp_ebgrp/5e0b0003-0000-4000-8000-000000000134", Dmf.Type.COMPERIODICAL, "1.3");
        assertValid(log);
        assertRuleProblem(log, "PRIMARY_METS_HEADER_MATCHES_PROFILE", "WARNING:", "'OC_EBGRP'", "'OC_EBSFL'");
    }

    // ---- mutace: chyby

    /**
     * DMF 1.3 kap. 3.1: slozka amdsec je povinna v obou variantach.
     */
    @Test
    public void sep13WithoutAmdsecIsInvalid() throws Exception {
        String log = run("composed_perio_1.3/missing_amdsec/5e0b0003-0000-4000-8000-000000000132", Dmf.Type.COMPERIODICAL, "1.3");
        assertRuleProblem(log, "SINGLE_SECONDARY-METS-DIR_FILE_FOUND", "ERROR:", "0 souborů");
        assertRuleProblem(log, "PRIMARY_METS_HEADER_MATCHES_PROFILE", "ERROR:", "mets:fileGrp[@ID='TECHMDGRP']");
        assertInvalid(log);
    }

    /**
     * DMF 3.1 kap. 3.1.3: soubory originaldata musi byt v md5 (i v itemlistu).
     */
    @Test
    public void emo31OriginaldataNotInMd5IsInvalid() throws Exception {
        String log = run("emonograph_3.1/originaldata_not_in_md5/aba007-p5m312", Dmf.Type.EMONOGRAPH, "3.1");
        assertRuleProblem(log, "CHECKSUM_FILE_ALL_PATHS_MATCH_FILES", "ERROR:", "nenalezen záznam pro soubor", "od_aba007-p5m312.pdf");
        assertRuleProblem(log, "CHECKSUM_FILE_ALL_PATHS_MATCH_FILES", "ERROR:", "nenalezen záznam pro soubor", "conv_aba007-p5m312.xml");
        assertEquals(2, countRuleProblems(log, "CHECKSUM_FILE_ALL_PATHS_MATCH_FILES", "ERROR:"));
    }

    /**
     * Soubor bez predpony od_ v originaldata (v md5 i itemlistu): pravidlo cizich souboru slozky.
     */
    @Test
    public void epe26OriginaldataBadNameIsInvalid() throws Exception {
        String log = run("eperiodical_2.6/originaldata_bad_name/5e0b0003-0000-4000-8000-000000000262", Dmf.Type.EPERIODICAL, "2.6");
        assertRuleProblem(log, "NO_EXTRA_FILES_IN_ORIGINALDATA-DIR", "ERROR:", "puvodni.pdf");
    }

    /**
     * Skladana 1.3 ma prisnejsi nazvovou konvenci (od_PSP-ID_XXXX); hlaska neuvadi nazev souboru, jen vzor.
     */
    @Test
    public void sep13OriginaldataBadNameIsInvalid() throws Exception {
        String log = run("composed_perio_1.3/originaldata_bad_name/5e0b0003-0000-4000-8000-000000000135", Dmf.Type.COMPERIODICAL, "1.3");
        assertRuleProblem(log, "ORIGINALDATA_FILENAMES_VALID", "ERROR:", "neodpovídá vzoru", "od_5e0b0003");
        assertInvalid(log);
    }

    /**
     * VEP #21: PSP-ID musi byt odvozen od identifikatoru intelektualni entity (cislo), ne titulu.
     */
    @Test
    public void epe26PspIdFromTitleUuidIsInvalid() throws Exception {
        String log = run("eperiodical_2.6/psp_id_from_title_id/5e0b0002-0000-4000-8000-000000000263", Dmf.Type.EPERIODICAL, "2.6");
        assertRuleProblem(log, "PSP_ID_DERIVED_FROM_IE_ID", "ERROR:", "'5e0b0002-0000-4000-8000-000000000263' není odvozen");
        assertEquals(1, countErrors(log));
    }

    /**
     * DMF e-periodika 2.6: URN:NBN cisla je povinny (MODS i DC).
     */
    @Test
    public void epe26IssueWithoutUrnNbnIsInvalid() throws Exception {
        String log = run("eperiodical_2.6/issue_no_urnnbn/5e0b0003-0000-4000-8000-000000000265", Dmf.Type.EPERIODICAL, "2.6");
        assertRuleProblem(log, "MODS_MANDATORY_IDENTIFIERS_PRESENT_LEVEL_ISSUE", "ERROR:", "'urnnbn' pro úroveň ISSUE");
        assertRuleProblem(log, "DC_MANDATORY_IDENTIFIERS_PRESENT_LEVEL_ISSUE", "ERROR:", "'urnnbn' pro úroveň ISSUE");
        assertRuleProblem(log, "BIBLIOGRAPHIC_METADATA_MATCH_PROFILE_ISSUE", "ERROR:", "MODSMD_ISSUE_0001", "mods:identifier[@type='urnnbn']");
    }

    @Test
    public void epe26ItemtotalMismatchIsInvalid() throws Exception {
        String log = run("eperiodical_2.6/itemtotal_mismatch/5e0b0003-0000-4000-8000-000000000266", Dmf.Type.EPERIODICAL, "2.6");
        assertRuleProblem(log, "INFO_ITEMTOTAL_MATCHES_ITEMS_COUNT", "ERROR:", "item (4)", "itemtotal/ITEMTOTAL (5)");
    }

    /**
     * mets/@TYPE "Monograph": DmfDetector balik rozpozna jako (tistenou) monografii; s vynucenou fDMF e-monografie 3.0
     * hlasi chybu profil hlavicky.
     */
    @Test
    public void emo30WrongMetsTypeIsInvalid() throws Exception {
        File dir = new File(RES + "emonograph_3.0/wrong_mets_type/aba007-p5m303");
        assertEquals(Dmf.Type.MONOGRAPH, new DmfDetector().resolveDmf(dir, new DmfDetector.Params()).getType());
        String log = runForced(dir, new Dmf(Dmf.Type.EMONOGRAPH, "3.0"));
        assertRuleProblem(log, "PRIMARY_METS_HEADER_MATCHES_PROFILE", "ERROR:", "@TYPE", "'Monograph'");
    }

    @Test
    public void emo30ExtraFileInOriginalIsInvalid() throws Exception {
        String log = run("emonograph_3.0/extra_file_in_original/aba007-p5m304", Dmf.Type.EMONOGRAPH, "3.0");
        assertRuleProblem(log, "NO_EXTRA_FILES_IN_ORIGINAL-DIR", "ERROR:", "poznamka.txt");
    }

    /**
     * metadataversion 3.0 u balicku podle 3.1 s originaldata: verze se bere z info.xml, fDMF 3.0 originaldata nezna.
     */
    @Test
    public void emo31InfoMetadataversionOldSelectsOlderFdmf() throws Exception {
        String log = run("emonograph_3.1/info_metadataversion_old/aba007-p5m316", Dmf.Type.EMONOGRAPH, "3.0");
        assertRuleProblem(log, "NO_OTHER_TL_FILES_FOUND", "ERROR:", "originaldata");
        assertInvalid(log);
    }

    @Test
    public void epe25BadChecksumIsInvalid() throws Exception {
        String log = run("eperiodical_2.5/bad_checksum/5e0b0003-0000-4000-8000-000000000252", Dmf.Type.EPERIODICAL, "2.5");
        assertRuleProblem(log, "CHECKSUM_FILE_ALL_CHECKSUMS_MATCH", "ERROR:", "nesouhlasí", "oc_5e0b0003-0000-4000-8000-000000000252.pdf");
        assertEquals(1, countErrors(log));
    }

    @Test
    public void epe25MissingOriginalFileIsInvalid() throws Exception {
        String log = run("eperiodical_2.5/missing_original_file/5e0b0003-0000-4000-8000-000000000253", Dmf.Type.EPERIODICAL, "2.5");
        String oc = "oc_5e0b0003-0000-4000-8000-000000000253.pdf";
        assertRuleProblem(log, "CHECKSUM_FILE_ALL_PATHS_MATCH_FILES", "ERROR:", oc);
        assertRuleProblem(log, "CHECKSUM_FILE_ALL_CHECKSUMS_MATCH", "ERROR:", "chyba výpočtu kontrolního součtu", oc);
        assertRuleProblem(log, "INFO_ITEMLIST_REFERENCES_ALL_FILES", "ERROR:", oc);
    }

    // ---- pomocne metody

    /**
     * Rozpozna typ a verzi DMF (DmfDetector, verze z info.xml), overi je a spusti validaci.
     */
    private String run(String relPath, Dmf.Type expectedType, String expectedVersion) throws Exception {
        File dir = new File(RES + relPath);
        assertTrue("balicek neexistuje: " + dir, dir.isDirectory());
        Dmf dmf = new DmfDetector().resolveDmf(dir, new DmfDetector.Params());
        assertEquals(expectedType, dmf.getType());
        assertEquals(expectedVersion, dmf.getVersion());
        return runForced(dir, dmf);
    }

    private String runForced(File dir, Dmf dmf) throws Exception {
        String log = FdmfTestSupport.run(dmf, dir);
        System.out.println(log);
        for (String line : log.split("\n")) {
            boolean crash = line.contains("nečekaná chyba") || line.contains("nesplněn kontrakt");
            assertTrue("konfigurace fDMF obsahuje chybu nebo funkce spadla: " + line.trim(), !crash);
        }
        return log;
    }

    /**
     * Radek s prefixem (ERROR:, WARNING:) a vsemi fragmenty musi byt v sekci daneho pravidla ("Pravidlo NAZEV: ...").
     */
    private void assertRuleProblem(String log, String rule, String prefix, String... fragments) {
        String currentRule = null;
        for (String line : log.split("\n")) {
            if (line.startsWith("Pravidlo ")) {
                int colon = line.indexOf(':');
                currentRule = colon < 0 ? line.substring("Pravidlo ".length()).trim() : line.substring("Pravidlo ".length(), colon);
                continue;
            }
            String l = line.trim();
            if (rule.equals(currentRule) && l.startsWith(prefix)) {
                boolean all = true;
                for (String f : fragments) {
                    all &= l.contains(f);
                }
                if (all) {
                    return;
                }
            }
        }
        assertTrue("ocekavan " + prefix + " pravidla " + rule + " obsahujici " + String.join(" + ", fragments), false);
    }

    private int countRuleProblems(String log, String rule, String prefix) {
        String currentRule = null;
        int count = 0;
        for (String line : log.split("\n")) {
            if (line.startsWith("Pravidlo ")) {
                int colon = line.indexOf(':');
                currentRule = colon < 0 ? line.substring("Pravidlo ".length()).trim() : line.substring("Pravidlo ".length(), colon);
                continue;
            }
            if (rule.equals(currentRule) && line.trim().startsWith(prefix)) {
                count++;
            }
        }
        return count;
    }

    private int countErrors(String log) {
        int count = 0;
        for (String line : log.split("\n")) {
            if (line.trim().startsWith("ERROR:")) {
                count++;
            }
        }
        return count;
    }

    private void assertNoLine(String log, String prefix, String fragment) {
        for (String line : log.split("\n")) {
            String l = line.trim();
            assertTrue("neocekavany radek: " + l, !(l.startsWith(prefix) && l.contains(fragment)));
        }
    }

    private void assertValid(String log) {
        for (String line : log.split("\n")) {
            assertTrue("neocekavany ERROR: " + line.trim(), !line.trim().startsWith("ERROR:"));
        }
        assertTrue("balik ma byt validni", log.contains("balík je: validní"));
    }

    private void assertInvalid(String log) {
        assertTrue("balik ma byt nevalidni", log.contains("balík je: nevalidní"));
    }
}
