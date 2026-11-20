package nkp.pspValidator.shared.engine;

import nkp.pspValidator.shared.Dmf;
import nkp.pspValidator.shared.DmfDetector;
import nkp.pspValidator.shared.engine.exceptions.InvalidXPathExpressionException;
import nkp.pspValidator.shared.engine.exceptions.PspDataException;
import nkp.pspValidator.shared.engine.exceptions.XmlFileParsingException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static junit.framework.TestCase.fail;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Created by Martin Řehánek on 2.11.16.
 */
public class DmfDetectorTest {

    private final DmfDetector dmfDetector = new DmfDetector();

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    @Test
    public void detectDmfTypeMonograph() {
        File pspRootDir = new File("src/test/resources/monograph_1.2/b50eb6b0-f0a4-11e3-b72e-005056827e52");
        try {
            Dmf.Type dmfType = dmfDetector.detectDmfType(pspRootDir);
            assertEquals(Dmf.Type.MONOGRAPH, dmfType);
        } catch (PspDataException e) {
            fail(e.getMessage());
        } catch (XmlFileParsingException e) {
            fail(e.getMessage());
        } catch (InvalidXPathExpressionException e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void detectDmfTypePeriodical() {
        File pspRootDir = new File("src/test/resources/periodical_1.6/7033d800-0935-11e4-beed-5ef3fc9ae867");
        try {
            Dmf.Type dmfType = dmfDetector.detectDmfType(pspRootDir);
            assertEquals(Dmf.Type.PERIODICAL, dmfType);
        } catch (PspDataException e) {
            fail(e.getMessage());
        } catch (XmlFileParsingException e) {
            fail(e.getMessage());
        } catch (InvalidXPathExpressionException e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void detectDmfTypeAudioGram() {
        File pspRootDir = new File("src/test/resources/audio_gram_0.3/1234567890");
        try {
            Dmf.Type dmfType = dmfDetector.detectDmfType(pspRootDir);
            assertEquals(Dmf.Type.AUDIO_GRAM, dmfType);
        } catch (PspDataException e) {
            fail(e.getMessage());
        } catch (XmlFileParsingException e) {
            fail(e.getMessage());
        } catch (InvalidXPathExpressionException e) {
            fail(e.getMessage());
        }
    }


    @Test
    public void detectDmfTypeFundUnit() throws PspDataException, XmlFileParsingException, InvalidXPathExpressionException {
        //DMF Jednotky fondu: tri hodnoty METS/@TYPE pro jeden typ DMF, porovnani presne vcetne velikosti pismen
        assertEquals(Dmf.Type.FUND_UNIT, dmfDetector.detectDmfType(new File("src/test/resources/fund_unit_0.1/clipping/nk-00027x")));
        assertEquals(Dmf.Type.FUND_UNIT, dmfDetector.detectDmfType(new File("src/test/resources/fund_unit_0.1/clipping_index/nk-00027y")));
        assertEquals(Dmf.Type.FUND_UNIT, dmfDetector.detectDmfType(new File("src/test/resources/fund_unit_0.1/card_index/nk-00027z")));
    }

    @Test
    public void detectDmfTypeFundUnitWrongCase() throws XmlFileParsingException, InvalidXPathExpressionException {
        //"card index" malymi pismeny neni platna hodnota (spec s. 17), balik se nesmi rozpoznat
        try {
            dmfDetector.detectDmfType(new File("src/test/resources/fund_unit_0.1/card_index_lowercase/nk-00028a"));
            fail();
        } catch (PspDataException e) {
            //ok
        }
    }

    @Test
    public void detectDmfVersionFundUnit() throws PspDataException, XmlFileParsingException, InvalidXPathExpressionException {
        File pspRootDir = new File("src/test/resources/fund_unit_0.1/clipping/nk-00027x");
        assertEquals("0.1", dmfDetector.detectDmfVersionFromInfoFile(Dmf.Type.FUND_UNIT, pspRootDir));
        assertEquals("0.2", dmfDetector.detectDmfVersionFromInfoFile(Dmf.Type.FUND_UNIT, new File("src/test/resources/fund_unit_0.2/valid_clipping/nk-00027x")));
    }

    @Test
    public void versionsFundUnit() throws PspDataException, XmlFileParsingException, InvalidXPathExpressionException {
        File fduDir = new File("src/test/resources/fund_unit_0.1/clipping/nk-00027x");
        //forced > info > preferred > default
        assertEquals("123", resolverFundUnit(fduDir, null, "123").getVersion());
        assertEquals("123", resolverFundUnit(fduDir, "1", "123").getVersion());
        assertEquals("0.1", resolverFundUnit(fduDir, "1", null).getVersion());
        assertEquals("0.1", resolverFundUnit(fduDir, null, null).getVersion());
        assertEquals(Dmf.Type.FUND_UNIT, resolverFundUnit(fduDir, null, null).getType());
    }

    private Dmf resolverFundUnit(File pspDir, String preferred, String forced) throws PspDataException, XmlFileParsingException, InvalidXPathExpressionException {
        DmfDetector.Params params = new DmfDetector.Params();
        params.forcedDmfFduVersion = forced;
        params.preferredDmfFduVersion = preferred;
        return dmfDetector.resolveDmf(pspDir, params);
    }

    @Test
    public void detectDmfTypeInvalid() {
        File pspRootDir = new File("src/test/resources/monograph_wrongType/b50eb6b0-f0a4-11e3-b72e-005056827e52");
        try {
            Dmf.Type dmfType = dmfDetector.detectDmfType(pspRootDir);
            System.out.println(dmfType);
            fail();
        } catch (PspDataException e) {
            //ok
        } catch (XmlFileParsingException e) {
            fail(e.getMessage());
        } catch (InvalidXPathExpressionException e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void detectDmfVersionMonograph() {
        File pspRootDir = new File("src/test/resources/monograph_1.2/b50eb6b0-f0a4-11e3-b72e-005056827e52");
        try {
            String version = dmfDetector.detectDmfVersionFromInfoFile(Dmf.Type.MONOGRAPH, pspRootDir);
            assertEquals("1.2", version);
        } catch (PspDataException e) {
            fail(e.getMessage());
        } catch (XmlFileParsingException e) {
            fail(e.getMessage());
        } catch (InvalidXPathExpressionException e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void detectDmfVersionPeriodical() {
        File pspRootDir = new File("src/test/resources/periodical_1.6/7033d800-0935-11e4-beed-5ef3fc9ae867");
        try {
            String version = dmfDetector.detectDmfVersionFromInfoFile(Dmf.Type.PERIODICAL, pspRootDir);
            assertEquals("1.6", version);
        } catch (PspDataException e) {
            fail(e.getMessage());
        } catch (XmlFileParsingException e) {
            fail(e.getMessage());
        } catch (InvalidXPathExpressionException e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void detectDmfVersionAudioGram() {
        File pspRootDir = new File("src/test/resources/audio_gram_0.3/1234567890");
        try {
            String version = dmfDetector.detectDmfVersionFromInfoFile(Dmf.Type.AUDIO_GRAM, pspRootDir);
            assertEquals("0.3", version);
        } catch (PspDataException e) {
            fail(e.getMessage());
        } catch (XmlFileParsingException e) {
            fail(e.getMessage());
        } catch (InvalidXPathExpressionException e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void versions() throws PspDataException, XmlFileParsingException, InvalidXPathExpressionException {
        //monograph
        File mon12Dir = new File("src/test/resources/monograph_1.2/b50eb6b0-f0a4-11e3-b72e-005056827e52");
        assertEquals(resolverMon(mon12Dir, null, "123").getVersion(), "123");
        assertEquals(resolverMon(mon12Dir, "1", "123").getVersion(), "123");
        assertEquals(resolverMon(mon12Dir, "1", null).getVersion(), "1.2");

        //periodical
        File per14Dir = new File("src/test/resources/periodical_1.4/ope301-00000v");
        assertEquals(resolverPer(per14Dir, null, "123").getVersion(), "123");
        assertEquals(resolverPer(per14Dir, "1", "123").getVersion(), "123");
        assertEquals(resolverPer(per14Dir, "1", null).getVersion(), "1.4");

        //audio document gramophone
        File adg03Dir = new File("src/test/resources/audio_gram_0.3/1234567890");
        assertEquals(resolverAudioGram(adg03Dir, null, "123").getVersion(), "123");
        assertEquals(resolverAudioGram(adg03Dir, "1", "123").getVersion(), "123");
        assertEquals(resolverAudioGram(adg03Dir, null, null).getVersion(), "0.3");
        assertEquals(resolverAudioGram(adg03Dir, "1", null).getVersion(), "0.3");
    }


    private Dmf resolverMon(File pspDir, String preferred, String forced) throws PspDataException, XmlFileParsingException, InvalidXPathExpressionException {
        DmfDetector.Params params = new DmfDetector.Params();
        params.forcedDmfMonVersion = forced;
        params.forcedDmfPerVersion = null;
        params.forcedDmfAdgVersion = null;
        params.forcedDmfAdfVersion = null;
        params.forcedDmfDadVersion = null;

        params.preferredDmfMonVersion = preferred;
        params.preferredDmfPerVersion = null;
        params.preferredDmfAdgVersion = null;
        params.preferredDmfAdfVersion = null;
        params.preferredDmfDadVersion = null;

        return dmfDetector.resolveDmf(pspDir, params);
    }

    private Dmf resolverPer(File pspDir, String preferred, String forced) throws PspDataException, XmlFileParsingException, InvalidXPathExpressionException {
        DmfDetector.Params params = new DmfDetector.Params();
        params.forcedDmfMonVersion = null;
        params.forcedDmfPerVersion = forced;
        params.forcedDmfAdgVersion = null;
        params.forcedDmfAdfVersion = null;
        params.forcedDmfDadVersion = null;

        params.preferredDmfMonVersion = null;
        params.preferredDmfPerVersion = preferred;
        params.preferredDmfAdgVersion = null;
        params.preferredDmfAdfVersion = null;
        params.preferredDmfDadVersion = null;

        return dmfDetector.resolveDmf(pspDir, params);
    }

    private Dmf resolverAudioGram(File pspDir, String preferred, String forced) throws PspDataException, XmlFileParsingException, InvalidXPathExpressionException {
        DmfDetector.Params params = new DmfDetector.Params();
        params.forcedDmfMonVersion = null;
        params.forcedDmfPerVersion = null;
        params.forcedDmfAdgVersion = forced;
        params.forcedDmfAdfVersion = null;
        params.forcedDmfDadVersion = null;

        params.preferredDmfMonVersion = null;
        params.preferredDmfPerVersion = null;
        params.preferredDmfAdgVersion = preferred;
        params.preferredDmfAdfVersion = null;
        params.preferredDmfDadVersion = null;

        return dmfDetector.resolveDmf(pspDir, params);
    }

    private Dmf resolverAudioFono(File pspDir, String preferred, String forced) throws PspDataException, XmlFileParsingException, InvalidXPathExpressionException {
        DmfDetector.Params params = new DmfDetector.Params();
        params.forcedDmfMonVersion = null;
        params.forcedDmfPerVersion = null;
        params.forcedDmfAdgVersion = null;
        params.forcedDmfAdfVersion = forced;
        params.forcedDmfDadVersion = null;

        params.preferredDmfMonVersion = null;
        params.preferredDmfPerVersion = null;
        params.preferredDmfAdgVersion = null;
        params.preferredDmfAdfVersion = preferred;
        params.preferredDmfDadVersion = null;

        return dmfDetector.resolveDmf(pspDir, params);
    }


    // ---- typy z minimalnich METS vyrobenych v docasnem adresari ----

    private File psp(String metsType, String infoVersion) throws Exception {
        File dir = tmp.newFolder();
        String typeAttr = metsType == null ? "" : " TYPE=\"" + metsType + "\"";
        write(new File(dir, "mets_x.xml"), "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<mets:mets xmlns:mets=\"http://www.loc.gov/METS/\" LABEL=\"x\"" + typeAttr + "/>");
        if (infoVersion != null) {
            write(new File(dir, "info_x.xml"), "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<info><metadataversion>" + infoVersion + "</metadataversion></info>");
        }
        return dir;
    }

    private static void write(File file, String content) throws Exception {
        Files.write(file.toPath(), content.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    public void detectDmfTypeAudioFono() throws Exception {
        assertEquals(Dmf.Type.AUDIO_FONO, dmfDetector.detectDmfType(psp("audio cylinder", null)));
    }

    @Test
    public void detectDmfTypeAudioDisc() throws Exception {
        assertEquals(Dmf.Type.AUDIO_DISC, dmfDetector.detectDmfType(psp("audio disc", null)));
    }

    @Test
    public void detectDmfTypeAudioNoCarrier() throws Exception {
        assertEquals(Dmf.Type.AUDIO_NO_CARRIER, dmfDetector.detectDmfType(psp("digital audio", null)));
    }

    @Test
    public void detectDmfTypeDataDiscFromSample() throws Exception {
        assertEquals(Dmf.Type.DATA_DISC, dmfDetector.detectDmfType(new File("src/test/resources/data_disc_0.1/minimal/nk-00028d")));
    }

    @Test
    public void detectDmfTypeTrimsWhitespace() throws Exception {
        assertEquals(Dmf.Type.MONOGRAPH, dmfDetector.detectDmfType(psp("  Monograph \n", null)));
    }

    @Test
    public void detectDmfTypeIsCaseSensitive() throws Exception {
        for (String wrong : new String[]{"monograph", "AUDIO DISC", "Digital audio", "Data_disc", "Sound Recording"}) {
            try {
                dmfDetector.detectDmfType(psp(wrong, null));
                fail("typ '" + wrong + "' nema byt rozpoznan");
            } catch (PspDataException e) {
                assertTrue(e.getMessage(), e.getMessage().contains("'" + wrong + "'"));
            }
        }
    }

    @Test
    public void detectDmfTypeUnknownValueMessage() throws Exception {
        try {
            dmfDetector.detectDmfType(psp("Neznamy typ", null));
            fail();
        } catch (PspDataException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("'Neznamy typ'"));
            assertTrue(e.getMessage(), e.getMessage().contains("atribut TYPE"));
        }
    }

    @Test
    public void detectDmfTypeMissingTypeAttribute() throws Exception {
        try {
            dmfDetector.detectDmfType(psp(null, null));
            fail();
        } catch (PspDataException e) {
            //chybejici atribut se chova jako prazdna hodnota
            assertTrue(e.getMessage(), e.getMessage().contains("hodnotu ''"));
        }
    }

    @Test
    public void detectDmfTypeNoMets() throws Exception {
        File dir = tmp.newFolder();
        write(new File(dir, "info_x.xml"), "<info/>");
        try {
            dmfDetector.detectDmfType(dir);
            fail();
        } catch (PspDataException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("nebyl nalezen soubor PRIMARY-METS"));
            //hlaska nevyjmenovava typy dokumentu (drive zastaraly vycet tri typu)
            assertFalse(e.getMessage(), e.getMessage().contains("gramodeska"));
        }
    }

    @Test
    public void detectDmfTypeTwoMetsListsCandidates() throws Exception {
        File dir = psp("Monograph", null);
        write(new File(dir, "METS_second.XML"), "<mets/>");
        try {
            dmfDetector.detectDmfType(dir);
            fail();
        } catch (PspDataException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("nalezeno více možných souborů PRIMARY-METS"));
            assertTrue(e.getMessage(), e.getMessage().contains("mets_x.xml"));
            assertTrue(e.getMessage(), e.getMessage().contains("METS_second.XML"));
        }
    }

    @Test
    public void detectDmfTypeMalformedXml() throws Exception {
        File dir = tmp.newFolder();
        write(new File(dir, "mets_x.xml"), "<mets:mets xmlns:mets=\"http://www.loc.gov/METS/\" TYPE=\"Monograph\"><nezavreno>");
        try {
            dmfDetector.detectDmfType(dir);
            fail();
        } catch (XmlFileParsingException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("chyba parsování xml"));
            assertTrue(e.getMessage(), e.getMessage().contains("mets_x.xml"));
        }
    }

    @Test
    public void detectDmfTypeEmptyMetsFile() throws Exception {
        File dir = tmp.newFolder();
        write(new File(dir, "mets_x.xml"), "");
        try {
            dmfDetector.detectDmfType(dir);
            fail();
        } catch (XmlFileParsingException e) {
            //ok
        }
    }

    @Test
    public void detectDmfVersionNoInfo() throws Exception {
        try {
            dmfDetector.detectDmfVersionFromInfoFile(Dmf.Type.MONOGRAPH, psp("Monograph", null));
            fail();
        } catch (PspDataException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("nenalezen soubor INFO"));
        }
    }

    @Test
    public void detectDmfVersionTwoInfos() throws Exception {
        File dir = psp("Monograph", "1.0");
        write(new File(dir, "info_second.xml"), "<info/>");
        try {
            dmfDetector.detectDmfVersionFromInfoFile(Dmf.Type.MONOGRAPH, dir);
            fail();
        } catch (PspDataException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("více možných souborů INFO"));
        }
    }

    @Test
    public void detectDmfVersionEmptyAndCamelCaseElement() throws Exception {
        assertEquals(null, dmfDetector.detectDmfVersionFromInfoFile(Dmf.Type.MONOGRAPH, psp("Monograph", "  ")));
        File dir = tmp.newFolder();
        write(new File(dir, "info_x.xml"), "<info><metadataVersion> 1.7 </metadataVersion></info>");
        assertEquals("1.7", dmfDetector.detectDmfVersionFromInfoFile(Dmf.Type.MONOGRAPH, dir));
    }

    // ---- poradi forced > info > preferred > default pro dalsi typy ----

    private Dmf resolve(String metsType, String infoVersion, DmfDetector.Params params) throws Exception {
        //INFO bez verze (prazdny element) = verze neni v INFO uvedena
        return dmfDetector.resolveDmf(psp(metsType, infoVersion == null ? "" : infoVersion), params);
    }

    @Test
    public void versionsAudioFono() throws Exception {
        DmfDetector.Params p = new DmfDetector.Params();
        p.forcedDmfAdfVersion = "F";
        p.preferredDmfAdfVersion = "P";
        assertEquals("F", resolve("audio cylinder", "I", p).getVersion());
        p.forcedDmfAdfVersion = null;
        assertEquals("I", resolve("audio cylinder", "I", p).getVersion());
        assertEquals("P", resolve("audio cylinder", null, p).getVersion());
        p.preferredDmfAdfVersion = null;
        assertEquals(DmfDetector.DEFAULT_AUDIO_FONO_VERSION, resolve("audio cylinder", null, p).getVersion());
        assertEquals(Dmf.Type.AUDIO_FONO, resolve("audio cylinder", null, p).getType());
    }

    @Test
    public void versionsAudioDisc() throws Exception {
        DmfDetector.Params p = new DmfDetector.Params();
        p.forcedDmfAdiVersion = "F";
        p.preferredDmfAdiVersion = "P";
        assertEquals("F", resolve("audio disc", "I", p).getVersion());
        p.forcedDmfAdiVersion = null;
        assertEquals("I", resolve("audio disc", "I", p).getVersion());
        assertEquals("P", resolve("audio disc", null, p).getVersion());
        p.preferredDmfAdiVersion = null;
        assertEquals(DmfDetector.DEFAULT_AUDIO_DISC_VERSION, resolve("audio disc", null, p).getVersion());
        assertEquals(Dmf.Type.AUDIO_DISC, resolve("audio disc", null, p).getType());
    }

    @Test
    public void versionsAudioNoCarrier() throws Exception {
        DmfDetector.Params p = new DmfDetector.Params();
        p.forcedDmfAdnVersion = "F";
        p.preferredDmfAdnVersion = "P";
        assertEquals("F", resolve("digital audio", "I", p).getVersion());
        p.forcedDmfAdnVersion = null;
        assertEquals("I", resolve("digital audio", "I", p).getVersion());
        assertEquals("P", resolve("digital audio", null, p).getVersion());
        p.preferredDmfAdnVersion = null;
        assertEquals(DmfDetector.DEFAULT_AUDIO_NO_CARRIER_VERSION, resolve("digital audio", null, p).getVersion());
        assertEquals(Dmf.Type.AUDIO_NO_CARRIER, resolve("digital audio", null, p).getType());
    }

    @Test
    public void versionsDataDisc() throws Exception {
        File dir = new File("src/test/resources/data_disc_0.1/minimal/nk-00028d");
        DmfDetector.Params p = new DmfDetector.Params();
        p.forcedDmfDadVersion = "F";
        p.preferredDmfDadVersion = "P";
        assertEquals("F", dmfDetector.resolveDmf(dir, p).getVersion());
        p.forcedDmfDadVersion = null;
        //verze z INFO vzorku
        String fromInfo = dmfDetector.detectDmfVersionFromInfoFile(Dmf.Type.DATA_DISC, dir);
        assertEquals(fromInfo, dmfDetector.resolveDmf(dir, p).getVersion());
        //bez verze v INFO: preferred, pak default
        assertEquals("P", resolve("data_disc", null, p).getVersion());
        p.preferredDmfDadVersion = null;
        assertEquals(DmfDetector.DEFAULT_DATA_DISC_VERSION, resolve("data_disc", null, p).getVersion());
        assertEquals(Dmf.Type.DATA_DISC, resolve("data_disc", null, p).getType());
    }

    @Test
    public void versionsDefaultsForMonographPeriodicalAudioGram() throws Exception {
        DmfDetector.Params p = new DmfDetector.Params();
        assertEquals(DmfDetector.DEFAULT_MONOGRAPH_VERSION, resolve("Monograph", "  ", p).getVersion());
        assertEquals(DmfDetector.DEFAULT_PERIODICAL_VERSION, resolve("Periodical", "  ", p).getVersion());
        assertEquals(DmfDetector.DEFAULT_AUDIO_GRAM_VERSION, resolve("sound recording", "  ", p).getVersion());
        assertEquals(DmfDetector.DEFAULT_FUND_UNIT_VERSION, resolve("Clipping", "  ", p).getVersion());
    }

    @Test
    public void detectDmfTypeEpublications() throws Exception {
        //hodnoty METS/@TYPE podle metsProfiles fDMF validatoru e-publikaci (VEP 2.4), presna shoda
        assertEquals(Dmf.Type.EMONOGRAPH, dmfDetector.detectDmfType(psp("electronic_monograph", null)));
        assertEquals(Dmf.Type.EPERIODICAL, dmfDetector.detectDmfType(psp("electronic_periodical", null)));
        assertEquals(Dmf.Type.COMPERIODICAL, dmfDetector.detectDmfType(psp("electronic_coll_journal", null)));
    }

    @Test
    public void detectDmfTypeEpublicationWrongCase() throws Exception {
        try {
            dmfDetector.detectDmfType(psp("Electronic_monograph", null));
            fail();
        } catch (PspDataException e) {
            //hlaska vyjmenovava i typy e-publikaci
            assertTrue(e.getMessage(), e.getMessage().contains("electronic_monograph/electronic_periodical/electronic_coll_journal"));
        }
    }

    @Test
    public void detectDmfVersionEpublications() throws Exception {
        assertEquals("3.0", dmfDetector.detectDmfVersionFromInfoFile(Dmf.Type.EMONOGRAPH, psp("electronic_monograph", "3.0")));
        assertEquals("2.5", dmfDetector.detectDmfVersionFromInfoFile(Dmf.Type.EPERIODICAL, psp("electronic_periodical", "2.5")));
        assertEquals("1.3", dmfDetector.detectDmfVersionFromInfoFile(Dmf.Type.COMPERIODICAL, psp("electronic_coll_journal", "1.3")));
    }

    @Test
    public void versionsEmonograph() throws Exception {
        DmfDetector.Params p = new DmfDetector.Params();
        p.forcedDmfEmoVersion = "F";
        p.preferredDmfEmoVersion = "P";
        assertEquals("F", resolve("electronic_monograph", "I", p).getVersion());
        p.forcedDmfEmoVersion = null;
        assertEquals("I", resolve("electronic_monograph", "I", p).getVersion());
        assertEquals("P", resolve("electronic_monograph", null, p).getVersion());
        p.preferredDmfEmoVersion = null;
        assertEquals(DmfDetector.DEFAULT_EMONOGRAPH_VERSION, resolve("electronic_monograph", null, p).getVersion());
        assertEquals(Dmf.Type.EMONOGRAPH, resolve("electronic_monograph", null, p).getType());
    }

    @Test
    public void versionsEperiodical() throws Exception {
        DmfDetector.Params p = new DmfDetector.Params();
        p.forcedDmfEpeVersion = "F";
        p.preferredDmfEpeVersion = "P";
        assertEquals("F", resolve("electronic_periodical", "I", p).getVersion());
        p.forcedDmfEpeVersion = null;
        assertEquals("I", resolve("electronic_periodical", "I", p).getVersion());
        assertEquals("P", resolve("electronic_periodical", null, p).getVersion());
        p.preferredDmfEpeVersion = null;
        assertEquals(DmfDetector.DEFAULT_EPERIODICAL_VERSION, resolve("electronic_periodical", null, p).getVersion());
        assertEquals(Dmf.Type.EPERIODICAL, resolve("electronic_periodical", null, p).getType());
    }

    @Test
    public void versionsComposedPeriodical() throws Exception {
        DmfDetector.Params p = new DmfDetector.Params();
        p.forcedDmfSepVersion = "F";
        p.preferredDmfSepVersion = "P";
        assertEquals("F", resolve("electronic_coll_journal", "I", p).getVersion());
        p.forcedDmfSepVersion = null;
        assertEquals("I", resolve("electronic_coll_journal", "I", p).getVersion());
        assertEquals("P", resolve("electronic_coll_journal", null, p).getVersion());
        p.preferredDmfSepVersion = null;
        assertEquals(DmfDetector.DEFAULT_COMPERIODICAL_VERSION, resolve("electronic_coll_journal", null, p).getVersion());
        assertEquals(Dmf.Type.COMPERIODICAL, resolve("electronic_coll_journal", null, p).getType());
    }

    @Test
    public void versionsEpublicationParamsDoNotLeakToOtherTypes() throws Exception {
        //vynucena verze e-monografie se nesmi pouzit pro monografii ani e-periodikum
        DmfDetector.Params p = new DmfDetector.Params();
        p.forcedDmfEmoVersion = "F";
        assertEquals("I", resolve("Monograph", "I", p).getVersion());
        assertEquals("I", resolve("electronic_periodical", "I", p).getVersion());
        assertEquals("I", resolve("electronic_coll_journal", "I", p).getVersion());
    }

    @Test
    public void versionsForcedDoesNotNeedInfoFile() throws Exception {
        DmfDetector.Params p = new DmfDetector.Params();
        p.forcedDmfAdiVersion = "F";
        assertEquals("F", dmfDetector.resolveDmf(psp("audio disc", null), p).getVersion());
    }

    @Test
    public void versionsWithoutForcedRequireInfoFile() throws Exception {
        try {
            dmfDetector.resolveDmf(psp("audio disc", null), new DmfDetector.Params());
            fail();
        } catch (PspDataException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("INFO"));
        }
    }

    @Test
    public void resolveDmfUnknownTypeFails() throws Exception {
        try {
            resolve("neznamy", "1.0", new DmfDetector.Params());
            fail();
        } catch (PspDataException e) {
            //ok
        }
    }

}
