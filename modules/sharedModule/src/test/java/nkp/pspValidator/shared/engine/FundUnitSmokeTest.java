package nkp.pspValidator.shared.engine;

import nkp.pspValidator.shared.*;
import nkp.pspValidator.shared.externalUtils.ExternalUtil;
import nkp.pspValidator.shared.externalUtils.ExternalUtilManager;
import nkp.pspValidator.shared.externalUtils.ExternalUtilManagerFactory;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Smoke test fDMF fund_unit_0.1 a fund_unit_0.2: konfigurace se nacte, validator se sestavi a projde vsechna pravidla
 * nad minimalnim balickem bez vyjimky. Neresi, ze balicek je validni (neni, chybi mu vetsina souboru).
 */
public class FundUnitSmokeTest {

    @Test
    public void buildAndRunOverMinimalPackage() throws Exception {
        runAndCheckNoCrash(new File("src/test/resources/fund_unit_0.1/clipping/nk-00027x"));
    }

    /**
     * Synteticky balicek vygenerovany src/test/tools/gen_fund_unit_package.py (vystrizek, RDA, 2 strany).
     * Musi byt validni (zadny ERROR); externi nastroje jsou vypnute, obrazy se nekontroluji.
     */
    @Test
    public void generatedClippingIsValid() throws Exception {
        String log = runAndCheckNoCrash(new File("src/test/resources/fund_unit_0.1/valid_clipping/nk-00027x"));
        assertValid(log);
    }

    /**
     * Soubor kartotecnich listku s urovni DIRECTORY (RDA, 3 strany), pokryva profily directory_*.
     */
    @Test
    public void generatedCardIndexWithDirectoryIsValid() throws Exception {
        String log = runAndCheckNoCrash(new File("src/test/resources/fund_unit_0.1/valid_card_index_directory/nk-00027z"));
        assertValid(log);
    }

    /**
     * ALTO 2.0 (namespace ns-v2#): predpis OCR (ALTO XML a TXT OCR) 1.0 pripousti ALTO 2.0 a novejsi, fDMF vybira XSD
     * podle namespace (K8). Balicek musi byt validni.
     */
    @Test
    public void generatedClippingWithAlto2IsValid() throws Exception {
        String log = runAndCheckNoCrash(new File("src/test/resources/fund_unit_0.1/valid_clipping_alto2/nk-00027v"));
        assertValid(log);
    }

    /**
     * ALTO 3.0 (namespace ns-v3#), tj. verze, kterou zapisuje tesseract; validuje se proti alto_3.1.xsd.
     */
    @Test
    public void generatedClippingWithAlto3IsValid() throws Exception {
        String log = runAndCheckNoCrash(new File("src/test/resources/fund_unit_0.1/valid_clipping_alto3/nk-00027u"));
        assertValid(log);
    }

    /**
     * Smisene verze ALTO v jednom balicku (prvni strana 2.0, druha 4.4): kazdy soubor je validni podle sveho XSD,
     * ale pravidlo OCR-ALTO_FILES_SAME_VERSION hlasi WARNING. Balicek zustava validni (zadny ERROR).
     */
    @Test
    public void generatedClippingWithMixedAltoVersionsWarns() throws Exception {
        String log = runAndCheckNoCrash(new File("src/test/resources/fund_unit_0.1/alto_mixed/nk-00027t"));
        assertValid(log);
        assertTrue("ocekavan WARNING o nejednotne verzi ALTO", log.contains("nemají jednotnou verzi formátu"));
    }

    /**
     * ALTO v neznamem namespace (ns-v9#): zadne XSD neodpovida, pravidlo OCR-ALTO_FILES_VALID_BY_XSD hlasi ERROR
     * "nepodporovana verze", balicek je nevalidni.
     */
    @Test
    public void generatedClippingWithUnknownAltoVersionIsInvalid() throws Exception {
        String log = runAndCheckNoCrash(new File("src/test/resources/fund_unit_0.1/alto_unknown/nk-00027s"));
        assertTrue("ocekavan ERROR o nepodporovane verzi ALTO", log.contains("nepodporovanou verzi formátu"));
        assertTrue("balik ma byt nevalidni", log.contains("balík je: nevalidní"));
    }

    // ---- fund_unit_0.2: balicky z gen_fund_unit_package.py --standard-version 0.2 (verze se bere z INFO metadataversion)

    @Test
    public void fundUnit02GeneratedClippingIsValid() throws Exception {
        File dir = new File("src/test/resources/fund_unit_0.2/valid_clipping/nk-00027x");
        assertEquals("0.2", new DmfDetector().resolveDmf(dir, new DmfDetector.Params()).getVersion());
        String log = runAndCheckNoCrash(dir);
        assertValid(log);
    }

    @Test
    public void fundUnit02GeneratedCardIndexWithDirectoryIsValid() throws Exception {
        String log = runAndCheckNoCrash(new File("src/test/resources/fund_unit_0.2/valid_card_index_directory/nk-00027z"));
        assertValid(log);
    }

    /**
     * DMF 0.2: strana musi mit mods:genre (M); balicek podle 0.1 bez nej je v 0.2 nevalidni.
     */
    @Test
    public void fundUnit02PageWithoutGenreIsInvalid() throws Exception {
        String log = runAndCheckNoCrash(new File("src/test/resources/fund_unit_0.2/page_no_genre/nk-00027r"));
        assertHasError(log, "MODSMD_PAGE_0001", "mods:genre");
    }

    /**
     * Duplicitni uuid u jine nez prvni strany (A, B, B): puvodni VfCheckIdentifiersNoDuplicates to neodhalila.
     */
    @Test
    public void fundUnit02DuplicatePageUuidIsInvalid() throws Exception {
        String log = runAndCheckNoCrash(new File("src/test/resources/fund_unit_0.2/duplicate_page_uuid/nk-00027q"));
        assertHasError(log, "duplikovaný identifikátor", "uuid:");
    }

    /**
     * DMF 0.2 opravila dc:type slozky na model:directory; hodnota model:collection z 0.1 uz neprochazi.
     */
    @Test
    public void fundUnit02DirectoryDcCollectionIsInvalid() throws Exception {
        String log = runAndCheckNoCrash(new File("src/test/resources/fund_unit_0.2/directory_dc_collection/nk-00027p"));
        assertHasError(log, "DCMD_DIRECTORY_0001", "model:directory");
    }

    /**
     * DMF 0.2: v RDA ma originInfo zdrojoveho dokumentu povinny atribut eventType.
     */
    @Test
    public void fundUnit02SourceOriginInfoWithoutEventTypeIsInvalid() throws Exception {
        String log = runAndCheckNoCrash(new File("src/test/resources/fund_unit_0.2/origininfo_no_eventtype/nk-00027n"));
        assertHasError(log, "MODSMD_UNIT_0001", "eventType");
    }

    /**
     * DMF 0.2, kap. 4: typ identifikatoru mimo vycet je jen WARNING (ma se konzultovat s odd. standardu).
     */
    @Test
    public void fundUnit02UnknownIdentifierTypeWarns() throws Exception {
        String log = runAndCheckNoCrash(new File("src/test/resources/fund_unit_0.2/unknown_identifier_type/nk-00027m"));
        assertValid(log);
        assertHasLine(log, "WARNING:", "typ 'inventory'");
    }

    /**
     * include_invalid: neznamy typ identifikatoru se hlasi i u neplatneho (invalid="yes") identifikatoru v MODS.
     */
    @Test
    public void fundUnit02UnknownTypeOfInvalidIdentifierWarns() throws Exception {
        String log = runAndCheckNoCrash(new File("src/test/resources/fund_unit_0.2/unknown_identifier_type_invalid/nk-00027l"));
        assertValid(log);
        assertHasLine(log, "WARNING:", "typ 'inventory'");
    }

    /**
     * DMF 0.2, kap. 7.1: titleid = vsechny platne identifikatory kolekce; chybejici je jen WARNING.
     */
    @Test
    public void fundUnit02InfoMissingCollectionIdWarns() throws Exception {
        String log = runAndCheckNoCrash(new File("src/test/resources/fund_unit_0.2/info_missing_collection_id/nk-00027k"));
        assertValid(log);
        assertHasLine(log, "WARNING:", "sysno:000123456");
    }

    /**
     * TYPE rodicovskeho divu fyzicke mapy musi byt UNIT (E3); "Unit" z DMF 0.1 je chyba.
     */
    @Test
    public void fundUnit02TopDivTypeUnitLowercaseIsInvalid() throws Exception {
        String log = runAndCheckNoCrash(new File("src/test/resources/fund_unit_0.2/top_div_type_unit_lowercase/nk-00027j"));
        assertHasError(log, "UNIT");
    }

    /**
     * dc:format jednotky je v 0.2 povinny.
     */
    @Test
    public void fundUnit02UnitDcWithoutFormatIsInvalid() throws Exception {
        String log = runAndCheckNoCrash(new File("src/test/resources/fund_unit_0.2/unit_dc_no_format/nk-00027i"));
        assertHasError(log, "DCMD_UNIT_0001", "dc:format");
    }

    /**
     * formatName v PREMIS Object i MIX musi byt MIME typ s predponou image/ nebo text/ (DMF Periodika 2.3, kap. 7.5,
     * na kterou DMF Jednotky fondu 0.2 odkazuje).
     */
    @Test
    public void fundUnit02FormatNameNotMimeIsInvalid() throws Exception {
        String log = runAndCheckNoCrash(new File("src/test/resources/fund_unit_0.2/premis_format_not_mime/nk-00027h"));
        assertHasError(log, "premis:formatName", "JPEG 2000");
        assertHasError(log, "mix:formatName", "JPEG 2000");
    }

    /**
     * relatedItem/part zdroje bez detail je v 0.2 chyba: detail je M, pri pouziti part aspon jeden (NK 23. 10. 2026).
     */
    @Test
    public void fundUnit02SourcePartWithoutDetailIsInvalid() throws Exception {
        String log = runAndCheckNoCrash(new File("src/test/resources/fund_unit_0.2/part_no_detail/nk-00027g"));
        assertHasError(log, "MODSMD_UNIT_0001", "mods:detail");
    }

    /**
     * Staci jeden detail (napr. jen cislo bez rocniku), obe hodnoty nejsou nutne (NK 23. 10. 2026).
     */
    @Test
    public void fundUnit02SourcePartWithIssueDetailOnlyIsValid() throws Exception {
        String log = runAndCheckNoCrash(new File("src/test/resources/fund_unit_0.2/part_issue_only/nk-00027f"));
        assertValid(log);
    }

    // ---- fund_unit_0.2: mutace generatoru (krok K6), kazda ma ocekavane pravidlo a fragment hlasky

    /**
     * Spatny MD5 jen u prvni archivni kopie v souboru .md5; druha strana (stejny JP2) chybu hlasit nesmi.
     */
    @Test
    public void fundUnit02BadChecksumIsInvalid() throws Exception {
        String log = runAndCheckNoCrash(new File("src/test/resources/fund_unit_0.2/bad_checksum/nk-00027e"));
        assertRuleProblem(log, "CHECKSUM_FILE_ALL_CHECKSUMS_MATCH", "ERROR:", "nesouhlasí", "mc_nk-00027e_0001.jp2");
        assertNoLine(log, "ERROR:", "mc_nk-00027e_0002.jp2");
    }

    @Test
    public void fundUnit02ItemtotalMismatchIsInvalid() throws Exception {
        String log = runAndCheckNoCrash(new File("src/test/resources/fund_unit_0.2/itemtotal_mismatch/nk-00027d"));
        assertRuleProblem(log, "INFO_ITEMTOTAL_MATCHES_ITEMS_COUNT", "ERROR:", "item (14)", "itemtotal/ITEMTOTAL (15)");
    }

    @Test
    public void fundUnit02StructLinkGapIsInvalid() throws Exception {
        String log = runAndCheckNoCrash(new File("src/test/resources/fund_unit_0.2/structlink_gap/nk-00027c"));
        assertRuleProblem(log, "PRIMARY-METS_STRUCT_LINKS_CORRECT", "ERROR:", "structLink neobsahuje záznam pro stránku DIV_PAGE_0002");
    }

    /**
     * Jednotka bez URN:NBN (MODS i DC): chybi povinny identifikator a PSP-ID neni odvozeno od identifikatoru entity.
     */
    @Test
    public void fundUnit02MissingUrnNbnIsInvalid() throws Exception {
        String log = runAndCheckNoCrash(new File("src/test/resources/fund_unit_0.2/missing_urnnbn/nk-00027b"));
        assertRuleProblem(log, "MODS_MANDATORY_IDENTIFIERS_PRESENT_LEVEL_UNIT", "ERROR:", "'urnnbn' pro úroveň UNIT");
        assertRuleProblem(log, "DC_MANDATORY_IDENTIFIERS_PRESENT_LEVEL_UNIT", "ERROR:", "'urnnbn' pro úroveň UNIT");
        assertRuleProblem(log, "PSP_ID_DERIVED_FROM_IE_ID", "ERROR:", "'nk-00027b' není odvozen");
        assertRuleProblem(log, "BIBLIOGRAPHIC_METADATA_MATCH_PROFILE_UNIT", "ERROR:", "MODSMD_UNIT_0001", "mods:identifier[@type='urnnbn']");
    }

    /**
     * Chybi dmdSec jednotky (MODS i DC), zustava UNITCOLLECTION. Chybu hlasi obe strukturalni mapy (DMDID na neexistujici
     * zaznam); pravidla pro identifikatory UNIT nesmi vzit UNITCOLLECTION za UNIT (XPath "MODSMD_UNIT_" s podtrzitkem).
     */
    @Test
    public void fundUnit02WithoutUnitDmdSecIsInvalid() throws Exception {
        String log = runAndCheckNoCrash(new File("src/test/resources/fund_unit_0.2/no_unit_dmdsec/nk-00027a"));
        assertRuleProblem(log, "PRIMARY-METS_LOGICAL_STRUCTURAL_MAP_CORRECT", "ERROR:", "odkaz na neexistující metadatový záznam", "MODSMD_UNIT_0001");
        assertRuleProblem(log, "PRIMARY-METS_PHYSICAL_STRUCTURAL_MAP_CORRECT", "ERROR:", "neexistující záznam", "MODSMD_UNIT_0001");
        assertNoLine(log, "ERROR:", "pro úroveň UNIT");
        assertTrue("balik ma byt nevalidni", log.contains("balík je: nevalidní"));
    }

    @Test
    public void fundUnit02WrongUnitGenreIsInvalid() throws Exception {
        String log = runAndCheckNoCrash(new File("src/test/resources/fund_unit_0.2/wrong_genre/nk-00026z"));
        assertRuleProblem(log, "BIBLIOGRAPHIC_METADATA_MATCH_PROFILE_UNIT", "ERROR:", "MODSMD_UNIT_0001", "mods:genre[text()='clipping'");
    }

    @Test
    public void fundUnit02WrongUnitDcTypeIsInvalid() throws Exception {
        String log = runAndCheckNoCrash(new File("src/test/resources/fund_unit_0.2/wrong_dc_type/nk-00026y"));
        assertRuleProblem(log, "BIBLIOGRAPHIC_METADATA_MATCH_PROFILE_UNIT", "ERROR:", "DCMD_UNIT_0001", "model:unit");
    }

    /**
     * URN:NBN na urovni UNITCOLLECTION je zakazany jen vykladem DMF 0.2, proto WARNING a balik zustava validni.
     */
    @Test
    public void fundUnit02UrnNbnOnCollectionWarns() throws Exception {
        String log = runAndCheckNoCrash(new File("src/test/resources/fund_unit_0.2/urnnbn_prohibited_on_collection/nk-00026x"));
        assertValid(log);
        assertRuleProblem(log, "MODS_PROHIBITED_IDENTIFIERS_NOT_PRESENT_LEVEL_UNITCOLLECTION", "WARNING:", "zakázaný identifikátor typu 'urnnbn'");
    }

    /**
     * Neznamy TYPE strany ve fyzicke mape je jen WARNING.
     */
    @Test
    public void fundUnit02UnknownPageTypeWarns() throws Exception {
        String log = runAndCheckNoCrash(new File("src/test/resources/fund_unit_0.2/page_type_unknown/nk-00026w"));
        assertValid(log);
        assertRuleProblem(log, "PRIMARY-METS_PHYSICAL_STRUCTURAL_MAP_CORRECT", "WARNING:", "DIV_PAGE_0001", "'weirdPage'");
    }

    /**
     * mets/@TYPE "Monograph": DmfDetector balik rozpozna jako monografii, validuje se tedy jinou fDMF nez fund_unit.
     */
    @Test
    public void fundUnit02WrongMetsTypeIsDetectedAsMonograph() throws Exception {
        Dmf dmf = new DmfDetector().resolveDmf(new File("src/test/resources/fund_unit_0.2/wrong_mets_type/nk-00026v"), new DmfDetector.Params());
        assertEquals(Dmf.Type.MONOGRAPH, dmf.getType());
    }

    // ---- fund_unit_0.2: finalni text DMF Jednotky fondu 0.2 (NK 2. 11. 2026)

    /**
     * detail volume i issue zdroje s vnorenym mods:title (finalni 0.2 doplnila title R), spolu s part/extent unit="pages".
     */
    @Test
    public void fundUnit02SourceDetailWithTitleAndExtentIsValid() throws Exception {
        String log = runAndCheckNoCrash(new File("src/test/resources/fund_unit_0.2/detail_with_title_and_extent/nk-00026u"));
        assertValid(log);
    }

    /**
     * Zdroj jako vicesvazkova monografie: relatedItem/titleInfo s partNumber a partName (finalni 0.2, MA), bez mods:part.
     */
    @Test
    public void fundUnit02SourceMonographWithPartNumberIsValid() throws Exception {
        String log = runAndCheckNoCrash(new File("src/test/resources/fund_unit_0.2/source_monograph_partnumber/nk-00026t"));
        assertValid(log);
    }

    /**
     * Typ strany generalView (Pravidla pro popis periodik 8.8) v genre i ve fyzicke mape je povoleny, bez WARNING.
     */
    @Test
    public void fundUnit02GeneralViewPageIsValid() throws Exception {
        String log = runAndCheckNoCrash(new File("src/test/resources/fund_unit_0.2/page_generalview/nk-00026s"));
        assertValid(log);
        assertNoLine(log, "WARNING:", "generalView");
    }

    /**
     * Div PAGE v logicke mape pod UNIT (finalni 0.2: PAGE je O).
     */
    @Test
    public void fundUnit02LogicalMapWithPagesIsValid() throws Exception {
        String log = runAndCheckNoCrash(new File("src/test/resources/fund_unit_0.2/logical_map_pages/nk-00026r"));
        assertValid(log);
    }

    /**
     * Popis podle AACR2 (descriptionStandard "aacr"), pokryva profily *_aacr2.
     */
    @Test
    public void fundUnit02GeneratedClippingAacrIsValid() throws Exception {
        String log = runAndCheckNoCrash(new File("src/test/resources/fund_unit_0.2/valid_clipping_aacr/nk-00026q"));
        assertValid(log);
    }

    @Test
    public void fundUnit02GeneratedCardIndexIsValid() throws Exception {
        String log = runAndCheckNoCrash(new File("src/test/resources/fund_unit_0.2/valid_card_index/nk-00026p"));
        assertValid(log);
    }

    @Test
    public void fundUnit02GeneratedClippingIndexIsValid() throws Exception {
        String log = runAndCheckNoCrash(new File("src/test/resources/fund_unit_0.2/valid_clipping_index/nk-00026n"));
        assertValid(log);
    }

    @Test
    public void fundUnit02GeneratedClippingWithAlto2IsValid() throws Exception {
        String log = runAndCheckNoCrash(new File("src/test/resources/fund_unit_0.2/valid_clipping_alto2/nk-00026m"));
        assertValid(log);
    }

    @Test
    public void fundUnit02GeneratedClippingWithAlto3IsValid() throws Exception {
        String log = runAndCheckNoCrash(new File("src/test/resources/fund_unit_0.2/valid_clipping_alto3/nk-00026l"));
        assertValid(log);
    }

    /**
     * detail type="pageNumber" pod relatedItem/part zdroje: profil povoluje jen volume a issue.
     */
    @Test
    public void fundUnit02SourceDetailWrongTypeIsInvalid() throws Exception {
        String log = runAndCheckNoCrash(new File("src/test/resources/fund_unit_0.2/detail_wrong_type/nk-00026k"));
        assertRuleProblem(log, "BIBLIOGRAPHIC_METADATA_MATCH_PROFILE_UNIT", "ERROR:", "mods:relatedItem/mods:part/mods:detail", "'pageNumber'");
    }

    /**
     * genre strany bez atributu type (M, kap. 7.4.4).
     */
    @Test
    public void fundUnit02PageGenreWithoutTypeIsInvalid() throws Exception {
        String log = runAndCheckNoCrash(new File("src/test/resources/fund_unit_0.2/page_genre_no_type/nk-00026j"));
        assertRuleProblem(log, "BIBLIOGRAPHIC_METADATA_MATCH_PROFILE_PAGE", "ERROR:", "MODSMD_PAGE_0001", "mods:genre", "'type'");
    }

    @Test
    public void fundUnit02PageDcWithoutTypeIsInvalid() throws Exception {
        String log = runAndCheckNoCrash(new File("src/test/resources/fund_unit_0.2/page_dc_no_type/nk-00026i"));
        assertRuleProblem(log, "BIBLIOGRAPHIC_METADATA_MATCH_PROFILE_PAGE", "ERROR:", "DCMD_PAGE_0001", "model:page");
    }

    /**
     * amd_mets prvni strany bez fileGrp UC_IMGGRP: amd_mets odkazuje na MC, UC, ALTO a TXT (kap. 5.6, 7.7.2).
     */
    @Test
    public void fundUnit02AmdMetsWithoutUcFileGrpIsInvalid() throws Exception {
        String log = runAndCheckNoCrash(new File("src/test/resources/fund_unit_0.2/amdmets_no_uc_filegrp/nk-00026h"));
        assertRuleProblem(log, "SECONDARY-METS_FILESEC_ALL_FILEGROUPS_PRESENT", "ERROR:", "ID=\"UC_IMGGRP\"");
        assertRuleProblem(log, "SECONDARY-METS_FILESEC_USER_COPY_FILE_REFERENCED", "ERROR:", "UC_IMGGRP", "amd_mets_nk-00026h_0001.xml");
    }

    @Test
    public void fundUnit02ExtraFileInCatalogEntryIsInvalid() throws Exception {
        String log = runAndCheckNoCrash(new File("src/test/resources/fund_unit_0.2/extra_file_in_catalog_entry/nk-00026g"));
        assertRuleProblem(log, "NO_EXTRA_FILES_IN_CATALOG-ENTRY-DIR", "ERROR:", "poznamka.txt");
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

    private void assertNoLine(String log, String prefix, String fragment) {
        for (String line : log.split("\n")) {
            String l = line.trim();
            assertTrue("neocekavany radek: " + l, !(l.startsWith(prefix) && l.contains(fragment)));
        }
    }

    private void assertHasLine(String log, String prefix, String fragment) {
        for (String line : log.split("\n")) {
            String l = line.trim();
            if (l.startsWith(prefix) && l.contains(fragment)) {
                return;
            }
        }
        assertTrue("ocekavan radek " + prefix + " obsahujici " + fragment, false);
    }

    private void assertHasError(String log, String... fragments) {
        for (String line : log.split("\n")) {
            String l = line.trim();
            if (l.startsWith("ERROR:")) {
                boolean all = true;
                for (String f : fragments) {
                    all &= l.contains(f);
                }
                if (all) {
                    return;
                }
            }
        }
        assertTrue("ocekavan ERROR obsahujici " + String.join(" + ", fragments), false);
    }

    private void assertValid(String log) {
        for (String line : log.split("\n")) {
            assertTrue("neocekavany ERROR: " + line.trim(), !line.trim().startsWith("ERROR:"));
        }
        assertTrue("balik ma byt validni", log.contains("balík je: validní"));
    }

    private String runAndCheckNoCrash(File pspRootDir) throws Exception {
        File validatorConfigDir = new File("src/main/resources/nkp/pspValidator/shared/validatorConfig");
        ValidatorConfigurationManager configManager = new ValidatorConfigurationManager(validatorConfigDir);
        FdmfRegistry registry = new FdmfRegistry(configManager);
        Dmf dmf = new DmfDetector().resolveDmf(pspRootDir, new DmfDetector.Params());
        FdmfConfiguration fdmfConfig = registry.getFdmfConfig(dmf);
        ExternalUtilManager utilManager = new ExternalUtilManagerFactory(configManager.getExternalUtilsConfigFile())
                .buildExternalUtilManager(Platform.detectOs().getOperatingSystem());
        for (ExternalUtil util : ExternalUtil.values()) {
            utilManager.setUtilDisabled(util);
        }
        fdmfConfig.initBinaryFileProfiles(utilManager);
        Validator validator = ValidatorFactory.buildValidator(fdmfConfig, pspRootDir, configManager.getDictionaryManager());

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(buffer, true, "UTF-8");
        validator.run(pspRootDir, null, out, 3, null, null, null, null, dmf);
        String log = buffer.toString("UTF-8");
        System.out.println(log);
        // chyby konfigurace nebo pad funkce se projevi jako "necekana chyba" / kontrakt v jednotlivych pravidlech
        // (texty viz ValidationFunction.invalidUnexpectedError / invalidContractNotMet).
        // EmptyParamEvaluationException je ocekavana: minimalni balicek nema adresare s daty a stejne se chovaji
        // i ostatni fDMF (chybejici srozumitelna hlaska je samostatny problem validatoru, ne fund_unit).
        for (String line : log.split("\n")) {
            boolean crash = (line.contains("nečekaná chyba") && !line.contains("EmptyParamEvaluationException"))
                    || line.contains("nesplněn kontrakt");
            assertTrue("konfigurace fDMF obsahuje chybu nebo funkce spadla: " + line.trim(), !crash);
        }
        return log;
    }
}
