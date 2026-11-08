package nkp.pspValidator.shared.engine;

import nkp.pspValidator.shared.*;
import nkp.pspValidator.shared.engine.exceptions.PspDataException;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * Regresni smoke test nad realnymi balicky ulozenymi v src/test/resources (ukol C z .local/doc/testy/plan-a-postup.md).
 * <p>
 * Pro kazdy balicek: autodetekce DMF (DmfDetector), validace s vypnutymi externimi nastroji a kontrola, ze
 * <ol>
 *     <li>v logu neni "nečekaná chyba" (krome EmptyParamEvaluationException) ani "nesplněn kontrakt",</li>
 *     <li>detekovany typ a verze DMF odpovida,</li>
 *     <li>pocty ERROR a WARNING a verdikt se nezmenily (stav 2.9, 8. 11. 2026),</li>
 *     <li>v logu jsou 2 az 4 konkretni ocekavane hlasky (pravidlo + fragment), aby test nepresel, kdyz jedna chyba
 *     nahradi jinou a pocty zustanou stejne.</li>
 * </ol>
 * Cilem je hlidat regresi spolecneho kodu (strukturalni mapy METS, checksum, INFO, MODS/DC, fileSec, PREMIS),
 * NE tvrdit, ze balicky jsou spravne. Jde o realna data, casto zmensena (obrazky nahrazene par bajty, upravene METS
 * bez prepocitani kontrolnich souctu), proto jsou vsechny nevalidni.
 * <p>
 * Pravidlo MODS_IDENTIFIERS_URN_NBN_REGISTERED se pta produkcniho URN:NBN Resolveru, jeho vysledek zavisi na siti
 * (bez site "Connect timed out" / "Network is unreachable" po 5 s, se siti podle stavu registrace). Proto se jeho
 * hlasky do poctu nezapocitavaji: pocty se scitaji ze zahlavi pravidel ("Pravidlo X: n problémů (a x ERROR, b x WARNING)")
 * a zahlavi tohoto pravidla se preskoci. Verdikt na nem nezavisi, protoze vsechny balicky maji i jine chyby.
 * <p>
 * Beh validace a registr fDMF jsou sdilene pres FdmfTestSupport. Resolver se v testech preskakuje systemovou vlastnosti
 * kv.skipUrnNbnResolver (build.gradle), pravidlo pak hlasi jen INFO; filtr podle nazvu pravidla zustava jako pojistka.
 */
@RunWith(Parameterized.class)
public class RepoPackagesSmokeTest {

    private static final String RESOURCES = "src/test/resources/";
    private static final Set<String> EXCLUDED_RULES = Collections.singleton("MODS_IDENTIFIERS_URN_NBN_REGISTERED");
    private static final String VERDICT_INVALID = "balík je: nevalidní";

    private static ValidatorConfigurationManager configManager;
    private static FdmfRegistry registry;

    /**
     * Ocekavana hlaska: pravidlo a fragment textu jednoho z jeho problemu (vcetne urovne, napr. "ERROR: ...").
     */
    private static final class Msg {
        final String rule;
        final String fragment;

        Msg(String rule, String fragment) {
            this.rule = rule;
            this.fragment = fragment;
        }
    }

    private static Msg msg(String rule, String fragment) {
        return new Msg(rule, fragment);
    }

    private static Object[] pkg(String name, String dir, Dmf.Type type, String version, int errors, int warnings, Msg... msgs) {
        return new Object[]{name, dir, type, version, errors, warnings, Arrays.asList(msgs)};
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Ocekavane pocty ERROR a WARNING bez pravidla MODS_IDENTIFIERS_URN_NBN_REGISTERED: stav 2.9, 8. 11. 2026.
    // ---------------------------------------------------------------------------------------------------------------

    /**
     * monograph_1.2, b50eb6b0: zmenseny balicek (jp2 nahrazene 25B soubory, navic *.jp2.bak), METS upravene bez
     * prepoctu souctu. Hlavni chyby: PREMIS agentNote prazdne (32x), nesouhlas checksumu ve fileSec (10x) a v md5 (3x),
     * extra soubory .jp2.bak, logicka mapa s TYPE 'Monograph', INFO s titleid typu uuid (XSD 1.2 ho nezna).
     */
    private static final int MON_1_2_ERRORS = 50;
    private static final int MON_1_2_WARNINGS = 24;

    /**
     * monograph_1.2/map, 6e9a7000 (kartograficky dokument): navic LTP_MD.xml v koreni, DC bez dc:type model:monograph,
     * PREMIS agentNote prazdne (12x), PREMIS udalosti odkazuji na neexistujici objekty, logicka mapa s TYPE 'Monograph'.
     */
    private static final int MON_1_2_MAP_ERRORS = 19;
    private static final int MON_1_2_MAP_WARNINGS = 16;

    /**
     * monograph_1.3.1, b50eb6b0: INFO s checksum type 'MD5' (XSD chce 'md5'), DC zaznamy stran nevalidni podle
     * dc_1.1.xsd (element dc:extent, 9x).
     */
    private static final int MON_1_3_1_ERRORS = 10;
    private static final int MON_1_3_1_WARNINGS = 3;

    /**
     * monograph_1.3.2, b50eb6b0: jen INFO s checksum type 'MD5'; varovani k atributu mods/@version a dc:type u strany.
     */
    private static final int MON_1_3_2_ERRORS = 1;
    private static final int MON_1_3_2_WARNINGS = 4;

    /**
     * periodical_1.4, ope301-00000v: stary balicek (velka pismena v nazvech, info.xml bez namespace podle XSD,
     * md5 soubor bez prefixu md5_, cesty s '.\'). Hlavni chyby: PREMIS odkazy na Windows cesty (220x),
     * nesouhlas checksumu ve fileSec (40x), DC/MODS titulu a cisla neodpovida profilu.
     */
    private static final int PER_1_4_ERRORS = 271;
    private static final int PER_1_4_WARNINGS = 228;

    /**
     * periodical_1.6, 7033d800: PREMIS agentNote prazdne (60x), duplicitni identifikatory agentu (PREMIS_LINKING 20x),
     * smLink na neexistujici ISSUE_1 (10x), nesouhlas checksumu ve fileSec (10x), duplicitni URN:NBN v DC i MODS.
     */
    private static final int PER_1_6_ERRORS = 107;
    private static final int PER_1_6_WARNINGS = 29;

    /**
     * periodical_1.7.1, df53c210: zmenseny balicek (ITEMTOTAL 203, ale jen 18 polozek), INFO s checksum type 'MD5',
     * DC zaznamy stran nevalidni podle dc_1.1.xsd (dc:extent, 4x).
     */
    private static final int PER_1_7_1_ERRORS = 6;
    private static final int PER_1_7_1_WARNINGS = 3;

    /**
     * audio_gram_0.3, 1234567890: ukazkovy balicek s vymyslenymi identifikatory (neplatna UUID), prazdne soubory
     * (md5 d41d8cd9...), nesoulad nazvu amdSec mezi INFO, md5 a adresarem, duplicitni local identifikatory,
     * typeOfResource 'sound recording-musical'.
     */
    private static final int ADG_0_3_ERRORS = 32;
    private static final int ADG_0_3_WARNINGS = 25;

    /**
     * audio_gram_0.3, aba007-0001w2: torzo balicku (jen main_mets, md5 a dve mp3, chybi INFO). Autodetekce typu selze
     * na METS/@TYPE 'Sound recording' (velke S), proto se validuje s vynucenou DMF audio_gram 0.3. Bez INFO neni
     * znamo PSP_ID a vetsina pravidel konci kaskadou "neznámá hodnota parametru".
     */
    private static final int ADG_0_3_ABA007_ERRORS = 92;
    private static final int ADG_0_3_ABA007_WARNINGS = 0;

    /**
     * data_disc_0.1/minimal, nk-00028d: umele minimalni balicek (jen info a mets, viz DataDiscSmokeTest), chybi vsechny
     * adresare a md5, METS bez fileSec a strukturalnich map.
     */
    private static final int DAD_0_1_MINIMAL_ERRORS = 75;
    private static final int DAD_0_1_MINIMAL_WARNINGS = 5;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> packages() {
        return Arrays.asList(
                pkg("monograph_1.2", "monograph_1.2/b50eb6b0-f0a4-11e3-b72e-005056827e52",
                        Dmf.Type.MONOGRAPH, "1.2", MON_1_2_ERRORS, MON_1_2_WARNINGS,
                        msg("NO_EXTRA_FILES_IN_MASTER-COPY-DIR", "ERROR: nalezen nečekaný soubor: mc_b50eb6b0-f0a4-11e3-b72e-005056827e52_0004.jp2.bak"),
                        msg("PRIMARY-METS_FILESEC_SIZES_MATCH", "WARNING: uvedená velikost (6039375 B) se liší od zjištěné velikosti (25 B) souboru mc_b50eb6b0-f0a4-11e3-b72e-005056827e52_0004.jp2"),
                        msg("PRIMARY-METS_LOGICAL_STRUCTURAL_MAP_CORRECT", "ERROR: /mets:mets/mets:structMap[@TYPE='LOGICAL']/mets:div[ID='MONOGRAPH_0001']: nepovolená hodnota atributu TYPE: 'Monograph'"),
                        msg("INFO_XML_VALID_BY_XSD", "Value 'uuid' is not facet-valid")),
                pkg("monograph_1.2_map", "monograph_1.2/map/6e9a7000-65c0-11e6-85af-005056827e52",
                        Dmf.Type.MONOGRAPH, "1.2", MON_1_2_MAP_ERRORS, MON_1_2_MAP_WARNINGS,
                        msg("NO_TOLERATED_TL_FILES_FOUND", "WARNING: nalezen neočekávaný soubor: "),
                        msg("BIBLIOGRAPHIC_METADATA_MATCH_PROFILE_VOLUME", "ERROR: mets_6e9a7000-65c0-11e6-85af-005056827e52.xml: DCMD_VOLUME_0001: /oai_dc:dc: nenalezen očekávaný povinný element 'dc:type[text() = 'model:monograph']'"),
                        msg("PREMIS_LINKING_CORRECT", "se odkazuje se na neexistující Objekt identifikátorem typu 'file' a hodnoty 'PS_1_a0001'")),
                pkg("monograph_1.3.1", "monograph_1.3.1/b50eb6b0-f0a4-11e3-b72e-005056827e52",
                        Dmf.Type.MONOGRAPH, "1.3.1", MON_1_3_1_ERRORS, MON_1_3_1_WARNINGS,
                        msg("INFO_XML_VALID_BY_XSD", "Value 'MD5' is not facet-valid with respect to pattern 'md5'"),
                        msg("BIBLIOGRAPHIC_METADATA_MATCH_PROFILE_PAGE", "ERROR: záznam DCMD_PAGE_0006 není validní podle"),
                        msg("BIBLIOGRAPHIC_METADATA_MATCH_PROFILE_VOLUME", "WARNING: mets_b50eb6b0-f0a4-11e3-b72e-005056827e52.xml: MODSMD_VOLUME_0001: /mods:mods: nalezen neočekávaný atribut 'version'")),
                pkg("monograph_1.3.2", "monograph_1.3.2/b50eb6b0-f0a4-11e3-b72e-005056827e52",
                        Dmf.Type.MONOGRAPH, "1.3.2", MON_1_3_2_ERRORS, MON_1_3_2_WARNINGS,
                        msg("INFO_XML_VALID_BY_XSD", "Value 'MD5' is not facet-valid with respect to pattern 'md5'"),
                        msg("BIBLIOGRAPHIC_METADATA_MATCH_PROFILE_PAGE", "WARNING: mets_b50eb6b0-f0a4-11e3-b72e-005056827e52.xml: DCMD_PAGE_0000: /oai_dc:dc: nalezen neočekávaný element 'dc:type'"),
                        msg("BIBLIOGRAPHIC_METADATA_MATCH_PROFILE_VOLUME", "nalezen neočekávaný atribut 'version'")),
                pkg("periodical_1.4", "periodical_1.4/ope301-00000v",
                        Dmf.Type.PERIODICAL, "1.4", PER_1_4_ERRORS, PER_1_4_WARNINGS,
                        msg("CHECKSUM_FILE_HAS_PREFIX_MD5", "WARNING: název souboru/adresáře neodpovídá vzoru [md5_.*]"),
                        msg("INFO_ITEMTOTAL_MATCHES_ITEMS_COUNT", "ERROR: neplatný formát čísla v atributu itemtotal"),
                        msg("PRIMARY-METS_PHYSICAL_STRUCTURAL_MAP_CORRECT", "WARNING: záznam stránky PageID_0057 ve fyzické strukturální mapě: nepovolená hodnota atributu TYPE ('TitlePage')"),
                        msg("PREMIS_LINKING_CORRECT", "se odkazuje se na neexistující Objekt identifikátorem typu 'temporary filepath'")),
                pkg("periodical_1.6", "periodical_1.6/7033d800-0935-11e4-beed-5ef3fc9ae867",
                        Dmf.Type.PERIODICAL, "1.6", PER_1_6_ERRORS, PER_1_6_WARNINGS,
                        msg("PRIMARY-METS_STRUCT_LINKS_CORRECT", "ERROR: nalezen element smLink s hodnotou atributu xlink:from (ISSUE_1), která neodpovídá žádnému atributu mets:div/@ID"),
                        msg("DC_IDENTIFIERS_NO_DUPLICATES_ACROSS_DMDSECS", "ERROR: seznam obsahuje duplikovaný identifikátor 'urnnbn:urn:nbn:cz:nk-0016ke'"),
                        msg("PREMIS_LINKING_CORRECT", "Agenti 'AGENT_004' a 'AGENT_002' mají stejný identifikátor")),
                pkg("periodical_1.7.1", "periodical_1.7.1/df53c210-d3f7-11e6-a1de-001018b3eb4c",
                        Dmf.Type.PERIODICAL, "1.7.1", PER_1_7_1_ERRORS, PER_1_7_1_WARNINGS,
                        msg("INFO_ITEMTOTAL_MATCHES_ITEMS_COUNT", "ERROR: počet elementů item (18) nesouhlasí s obsahem atributu itemtotal/ITEMTOTAL (203)"),
                        msg("BIBLIOGRAPHIC_METADATA_MATCH_PROFILE_PAGE", "ERROR: záznam DCMD_PAGE_0002 není validní podle"),
                        msg("BIBLIOGRAPHIC_METADATA_MATCH_PROFILE_TITLE", "/mods:mods/mods:titleInfo[1]: nalezen neočekávaný atribut 'displayLabel'")),
                pkg("audio_gram_0.3", "audio_gram_0.3/1234567890",
                        Dmf.Type.AUDIO_GRAM, "0.3", ADG_0_3_ERRORS, ADG_0_3_WARNINGS,
                        msg("NO_EXTRA_FILES_IN_SECONDARY-METS-DIR", "ERROR: nalezen nečekaný soubor: amd_mets_1234567890__0002.xml"),
                        msg("CHECKSUM_FILE_ALL_CHECKSUMS_MATCH", "s vypočítaným kontrolním součtem 'd41d8cd98f00b204e9800998ecf8427e'"),
                        msg("BIBLIOGRAPHIC_METADATA_MATCH_PROFILE_SOUNDRECORDING", "/mods:mods/mods:typeOfResource: hodnota 'sound recording-musical' nesplňuje žádnou ze specifikací")),
                pkg("audio_gram_0.3_aba007", "audio_gram_0.3/aba007-0001w2",
                        Dmf.Type.AUDIO_GRAM, "0.3", ADG_0_3_ABA007_ERRORS, ADG_0_3_ABA007_WARNINGS,
                        msg("PSP_DIR_MATCHES_PSP_ID", "neznámá hodnota promměnné PSP_ID"),
                        msg("NO_OTHER_TL_FILES_FOUND", "ERROR: nalezen nečekaný soubor: md5_aba007-0001w2.md5"),
                        msg("NO_EXTRA_FILES_IN_USER-COPY-AUDIO-DIR", "ERROR: nalezen nečekaný soubor: uca_aba007-0001w2__0001.mp3")),
                pkg("data_disc_0.1_minimal", "data_disc_0.1/minimal/nk-00028d",
                        Dmf.Type.DATA_DISC, "0.1", DAD_0_1_MINIMAL_ERRORS, DAD_0_1_MINIMAL_WARNINGS,
                        msg("SINGLE_ISOIMAGE-DIR_FILE_FOUND", "ERROR: seznam obsahuje 0 souborů namísto očekávaných 1"),
                        msg("PRIMARY-METS_FILESEC_ALL_FILEGROUPS_PRESENT", "ERROR: nenalezen element mets:fileGrp s atributem ID=\"MC_IMGGRP\""),
                        msg("PRIMARY-METS_PHYSICAL_STRUCTURAL_MAP_CORRECT", "ERROR: chybí fyzická strukturální mapa"))
        );
    }

    @Parameterized.Parameter(0)
    public String name;
    @Parameterized.Parameter(1)
    public String dir;
    @Parameterized.Parameter(2)
    public Dmf.Type expectedType;
    @Parameterized.Parameter(3)
    public String expectedVersion;
    @Parameterized.Parameter(4)
    public int expectedErrors;
    @Parameterized.Parameter(5)
    public int expectedWarnings;
    @Parameterized.Parameter(6)
    public List<Msg> expectedMessages;

    @BeforeClass
    public static void initRegistry() throws Exception {
        configManager = FdmfTestSupport.sharedConfigManager();
        registry = FdmfTestSupport.sharedRegistry();
    }

    @Test
    public void validate() throws Exception {
        File pspRootDir = new File(RESOURCES + dir);
        assertTrue("balicek neexistuje: " + pspRootDir, pspRootDir.isDirectory());
        long start = System.currentTimeMillis();

        Dmf dmf = detectDmf(pspRootDir);
        assertEquals("typ DMF", expectedType, dmf.getType());
        assertEquals("verze DMF", expectedVersion, dmf.getVersion());

        String log = FdmfTestSupport.run(registry, configManager, dmf, pspRootDir);
        writeLogForDebugging(log);

        for (String line : log.split("\n")) {
            boolean crash = (line.contains("nečekaná chyba") && !line.contains("EmptyParamEvaluationException"))
                    || line.contains("nesplněn kontrakt");
            assertFalse("funkce spadla nebo konfigurace obsahuje chybu: " + line.trim(), crash);
        }
        assertTrue("v logu chybi verdikt '" + VERDICT_INVALID + "'", log.contains(VERDICT_INVALID));

        int[] counts = RepoPackagesSupport.countErrorsAndWarnings(log, EXCLUDED_RULES);
        assertEquals("pocet ERROR (bez " + EXCLUDED_RULES + ")", expectedErrors, counts[0]);
        assertEquals("pocet WARNING (bez " + EXCLUDED_RULES + ")", expectedWarnings, counts[1]);

        for (Msg expected : expectedMessages) {
            List<String> problems = RepoPackagesSupport.problemsOfRule(log, expected.rule);
            boolean found = false;
            for (String problem : problems) {
                if (problem.contains(expected.fragment)) {
                    found = true;
                    break;
                }
            }
            assertTrue("pravidlo " + expected.rule + " nehlasi '" + expected.fragment + "', hlasi: " + problems, found);
        }
        System.out.println(String.format("RepoPackagesSmokeTest %s: %d ms", name, System.currentTimeMillis() - start));
    }

    /**
     * Autodetekce DMF. Vyjimka: torzo aba007-0001w2 nema INFO a METS/@TYPE 'Sound recording' (velke S) DmfDetector
     * nerozpozna; to je zafixovane chovani, validuje se pak s vynucenou verzi.
     */
    private Dmf detectDmf(File pspRootDir) throws Exception {
        if (dir.endsWith("aba007-0001w2")) {
            try {
                new DmfDetector().resolveDmf(pspRootDir, new DmfDetector.Params());
                fail("ocekavano, ze typ DMF nebude rozpoznan (METS/@TYPE 'Sound recording')");
            } catch (PspDataException e) {
                assertTrue(e.getMessage(), e.getMessage().contains("ale hodnotu 'Sound recording'"));
            }
            return new Dmf(Dmf.Type.AUDIO_GRAM, "0.3");
        }
        return new DmfDetector().resolveDmf(pspRootDir, new DmfDetector.Params());
    }

    private void writeLogForDebugging(String log) {
        try {
            File outDir = new File("build/repo-packages-smoke");
            outDir.mkdirs();
            Files.write(new File(outDir, name + ".log").toPath(), log.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            //jen pomucka pro ladeni, test na ni nezavisi
        }
    }
}
