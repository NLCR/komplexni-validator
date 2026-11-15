package nkp.pspValidator.shared.engine.utils;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Chovani {@link UrnNbnResolverChecker} nad skutecnymi odpovedmi Resolveru (API v5) ulozenymi v
 * src/test/resources/resolver (viz README.md tamtez: URL, datum stazeni, stav). Lokalni HTTP server servíruje soubory
 * beze zmeny, jen HTTP kod odpovida skutecnemu serveru (viz {@link #HTTP_CODES}). Testy nejdou do site.
 * Soubory jsou snimek ke dni stazeni (2026-11-15), nic nevymyslene.
 */
public class UrnNbnResolverCheckerFixturesTest {

    /**
     * HTTP kody skutecnych odpovedi, ktere nejsou 200 (klic: kind + "/" + URN bez prefixu urn:nbn:cz:).
     */
    private static final Map<String, Integer> HTTP_CODES = new HashMap<>();

    static {
        HTTP_CODES.put("resolver/nk-00027d", 403);
        HTTP_CODES.put("resolver/nk-00026w", 403);
        HTTP_CODES.put("resolver/nk-zzzzzz", 404);
        HTTP_CODES.put("urnnbn/xx", 400);
    }

    private static final String MZK = "urn:nbn:cz:mzk-006obk";               //monograph, "Babicka vypravuje"
    private static final String VOLUME = "urn:nbn:cz:nk-003do9";             //monographVolume
    private static final String ISSUE_LT = "urn:nbn:cz:abg001-0005lt";       //periodicalIssue
    private static final String ISSUE_QV = "urn:nbn:cz:abg001-0005qv";       //periodicalIssue (MLP), bez volumeTitle
    private static final String ARTICLE = "urn:nbn:cz:pna001-00cxz5";        //analytical
    private static final String OTHER = "urn:nbn:cz:abg001-0003ig";          //otherEntity (sheetmusic)
    private static final String DEACTIVATED = "urn:nbn:cz:nk-00027d";
    private static final String FREE = "urn:nbn:cz:nk-zzzzzz";
    private static final String RESERVED = "urn:nbn:cz:nk-00000e";
    private static final String INVALID = "urn:nbn:cz:xx";

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private HttpServer server;
    private String serverUrl;
    /**
     * Predpona souboru podle instalace: prod (resolver.nkp.cz) nebo test (resolver-test.nkp.cz).
     */
    private String instance = "prod";

    @Before
    public void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/api/v5/urnnbn/", exchange -> serve(exchange, "urnnbn"));
        server.createContext("/api/v5/resolver/", exchange -> serve(exchange, "resolver"));
        server.start();
        serverUrl = "http://" + server.getAddress().getHostString() + ":" + server.getAddress().getPort();
    }

    @After
    public void tearDown() {
        server.stop(0);
        UrnNbnResolverChecker.setResolverBaseUrl(null);
    }

    private void serve(HttpExchange exchange, String kind) throws IOException {
        String urn = exchange.getRequestURI().getPath();
        urn = urn.substring(urn.lastIndexOf('/') + 1);
        String id = urn.replaceFirst("^urn:nbn:cz:", "");
        String resource = "/resolver/" + instance + "-" + id + "-" + kind + ".json";
        byte[] body;
        int code;
        try (InputStream in = getClass().getResourceAsStream(resource)) {
            if (in == null) {
                //chybi fixture: test musi spadnout, ne dostat vymyslenou odpoved
                body = ("missing fixture " + resource).getBytes(StandardCharsets.UTF_8);
                code = 500;
            } else {
                body = readAll(in);
                Integer c = HTTP_CODES.get(kind + "/" + id);
                code = c != null && "prod".equals(instance) ? c : 200;
            }
        }
        exchange.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(code, body.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(body);
        }
    }

    private static byte[] readAll(InputStream in) throws IOException {
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int n;
        while ((n = in.read(buf)) > 0) {
            out.write(buf, 0, n);
        }
        return out.toByteArray();
    }

    /**
     * Zaznam MODS do minimalniho METS: ID (napr. MODS_ISSUE_0001), title, partNumber a URN:NBN (kazde muze byt null).
     */
    private static String[] mods(String id, String title, String partNumber, String urnNbn) {
        return new String[]{id, title, partNumber, urnNbn};
    }

    private File mets(String[]... records) throws IOException {
        StringBuilder xml = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<mets:mets xmlns:mets=\"http://www.loc.gov/METS/\" xmlns:mods=\"http://www.loc.gov/mods/v3\">");
        for (String[] r : records) {
            xml.append("<mets:dmdSec ID=\"MODSMD_").append(r[0].substring("MODS_".length())).append("\"><mets:mdWrap><mets:xmlData>")
                    .append("<mods:mods ID=\"").append(r[0]).append("\"><mods:titleInfo>")
                    .append(r[1] == null ? "" : "<mods:title>" + r[1] + "</mods:title>")
                    .append(r[2] == null ? "" : "<mods:partNumber>" + r[2] + "</mods:partNumber>")
                    .append("</mods:titleInfo>")
                    .append(r[3] == null ? "" : "<mods:identifier type=\"urnnbn\">" + r[3] + "</mods:identifier>")
                    .append("</mods:mods></mets:xmlData></mets:mdWrap></mets:dmdSec>");
        }
        xml.append("</mets:mets>");
        File file = tmp.newFile();
        Files.write(file.toPath(), xml.toString().getBytes(StandardCharsets.UTF_8));
        return file;
    }

    private UrnNbnResolverChecker checker(File metsFile) throws Exception {
        UrnNbnResolverChecker.setResolverBaseUrl(serverUrl, true);
        return new UrnNbnResolverChecker(metsFile);
    }

    private UrnNbnResolverChecker.ResolverWarning expectWarning(UrnNbnResolverChecker checker, String urn) throws Exception {
        try {
            checker.check(urn);
        } catch (UrnNbnResolverChecker.ResolverWarning e) {
            return e;
        }
        fail("očekáváno varování pro " + urn);
        return null;
    }

    // ---------- monografie ----------

    @Test
    public void activeMonographWithSameTitlePasses() throws Exception {
        checker(mets(mods("MODS_VOLUME_0001", "Babička vypravuje", null, MZK))).check(MZK);
    }

    @Test
    public void activeMonographWithDifferentTitleIsWarning() throws Exception {
        UrnNbnResolverChecker.ResolverWarning e = expectWarning(checker(mets(mods("MODS_VOLUME_0001", "Dědeček vypravuje", null, MZK))), MZK);
        assertTrue(e.getMessage(), e.getMessage().contains("'Dědeček vypravuje' != 'Babička vypravuje'"));
        assertTrue(e.getMessage(), e.getMessage().contains("CZIDLO.type:MONOGRAPH"));
    }

    @Test
    public void activeWithoutMetsOnlyChecksStatus() throws Exception {
        checker(null).check(MZK);
    }

    @Test
    public void monographResolverEntityWithIssueModsIsWarningB4() throws Exception {
        UrnNbnResolverChecker.ResolverWarning e = expectWarning(checker(mets(mods("MODS_ISSUE_0001", "Babička vypravuje", null, MZK))), MZK);
        assertTrue(e.getMessage(), e.getMessage().contains("typ entity v Resolveru (MONOGRAPH) neodpovídá žádnému záznamu MODS"));
    }

    @Test
    public void monographVolumePasses() throws Exception {
        checker(mets(mods("MODS_VOLUME_0001", "Národní Báchorky a Powěsti", "1", VOLUME))).check(VOLUME);
    }

    @Test
    public void monographVolumeWithDifferentVolumeNumberIsWarning() throws Exception {
        UrnNbnResolverChecker.ResolverWarning e = expectWarning(checker(mets(mods("MODS_VOLUME_0001", "Národní Báchorky a Powěsti", "2", VOLUME))), VOLUME);
        assertTrue(e.getMessage(), e.getMessage().contains("'2' != '1'"));
    }

    // ---------- periodikum ----------

    @Test
    public void periodicalIssueWithoutVolumeTitleSkipsVolumeB7() throws Exception {
        //reálná odpověď (MLP) nemá titleInfo.volumeTitle; MODS_VOLUME_0001 s partNumber se proto nesrovnává
        checker(mets(
                mods("MODS_TITLE_0001", "Ohněm proti ohni", null, null),
                mods("MODS_VOLUME_0001", "1984", "1984", null),
                mods("MODS_ISSUE_0001", null, "10.-19.09.1984", ISSUE_QV))).check(ISSUE_QV);
    }

    @Test
    public void periodicalIssueWithDifferentIssueTitleIsWarning() throws Exception {
        UrnNbnResolverChecker.ResolverWarning e = expectWarning(checker(mets(
                mods("MODS_TITLE_0001", "Ohněm proti ohni", null, null),
                mods("MODS_ISSUE_0001", null, "1.-9.09.1984", ISSUE_QV))), ISSUE_QV);
        assertTrue(e.getMessage(), e.getMessage().contains("'1.-9.09.1984' != '10.-19.09.1984'"));
        assertTrue(e.getMessage(), e.getMessage().contains("CZIDLO.type:PERIODICAL_ISSUE"));
    }

    @Test
    public void periodicalIssueWithDifferentPeriodicalTitleIsWarning() throws Exception {
        UrnNbnResolverChecker.ResolverWarning e = expectWarning(checker(mets(
                mods("MODS_TITLE_0001", "Jiný název", null, null),
                mods("MODS_ISSUE_0001", null, "1982", ISSUE_LT))), ISSUE_LT);
        assertTrue(e.getMessage(), e.getMessage().contains("'Jiný název' != 'Babička se zbláznila'"));
    }

    @Test
    public void periodicalIssuePasses() throws Exception {
        checker(mets(
                mods("MODS_TITLE_0001", "Babička se zbláznila", null, null),
                mods("MODS_ISSUE_0001", null, "1982", ISSUE_LT))).check(ISSUE_LT);
    }

    // ---------- clanek ----------

    @Test
    public void analyticalArticlePasses() throws Exception {
        checker(mets(mods("MODS_ART_0001", "»Babička«", null, ARTICLE))).check(ARTICLE);
    }

    @Test
    public void analyticalArticleWithDifferentTitleIsWarning() throws Exception {
        UrnNbnResolverChecker.ResolverWarning e = expectWarning(checker(mets(mods("MODS_ART_0001", "Babička", null, ARTICLE))), ARTICLE);
        assertTrue(e.getMessage(), e.getMessage().contains("'Babička' != '»Babička«'"));
    }

    @Test
    public void analyticalChapterModsMatchesToo() throws Exception {
        checker(mets(mods("MODS_CHAP_0001", "»Babička«", null, ARTICLE))).check(ARTICLE);
    }

    // ---------- otherEntity (B4) ----------

    @Test
    public void otherEntityMatchesUnitAndSoundCollection() throws Exception {
        String title = "První dechový kvintet (G dur), op. 23";
        checker(mets(mods("MODS_UNIT_0001", title, null, OTHER))).check(OTHER);
        checker(mets(mods("MODS_SOUNDCOLLECTION_0001", title, null, OTHER))).check(OTHER);
    }

    @Test
    public void otherEntityWithDifferentTitleIsWarning() throws Exception {
        UrnNbnResolverChecker.ResolverWarning e = expectWarning(checker(mets(mods("MODS_UNIT_0001", "Jiný titul", null, OTHER))), OTHER);
        assertTrue(e.getMessage(), e.getMessage().contains("CZIDLO.type:OTHER, MODS.type:UNIT"));
    }

    @Test
    public void otherEntityWithVolumeModsIsWarningB4() throws Exception {
        UrnNbnResolverChecker.ResolverWarning e = expectWarning(checker(mets(mods("MODS_VOLUME_0001", "První dechový kvintet (G dur), op. 23", null, OTHER))), OTHER);
        assertTrue(e.getMessage(), e.getMessage().contains("typ entity v Resolveru (OTHER)"));
    }

    // ---------- stavy a chyby ----------

    @Test
    public void deactivatedIsWarning() throws Exception {
        UrnNbnResolverChecker.ResolverWarning e = expectWarning(checker(null), DEACTIVATED);
        assertTrue(e.getMessage(), e.getMessage().contains("deaktivován"));
    }

    @Test
    public void deactivatedWithMetsIsWarningWithoutCallingDetail() throws Exception {
        //detail /resolver/ deaktivovaneho vraci HTTP 403, kontrola se zastavi uz u stavu
        UrnNbnResolverChecker.ResolverWarning e = expectWarning(checker(mets(mods("MODS_ISSUE_0001", null, "1", DEACTIVATED))), DEACTIVATED);
        assertTrue(e.getMessage(), e.getMessage().contains("deaktivován"));
    }

    @Test
    public void freeIsError() throws Exception {
        try {
            checker(null).check(FREE);
            fail("FREE musí skončit chybou");
        } catch (UrnNbnResolverChecker.ResolverError e) {
            assertTrue(e.getMessage(), e.getMessage().contains("není registrován"));
        }
    }

    @Test
    public void reservedIsWarning() throws Exception {
        UrnNbnResolverChecker.ResolverWarning e = expectWarning(checker(null), RESERVED);
        assertTrue(e.getMessage(), e.getMessage().contains("rezervován"));
    }

    @Test
    public void invalidUrnFormatIsResolverError() throws Exception {
        try {
            checker(null).check(INVALID);
            fail("chybný formát musí skončit chybou");
        } catch (UrnNbnResolverChecker.ResolverError e) {
            assertTrue(e.getMessage(), e.getMessage().startsWith("INVALID_URN_NBN: "));
            assertTrue(e.getMessage(), e.getMessage().contains("incorrect syntax"));
        }
    }

    // ---------- testovaci instance ----------

    @Test
    public void testInstanceActivePeriodicalIssueWithVolumeTitle() throws Exception {
        instance = "test";
        File ok = mets(
                mods("MODS_TITLE_0001", "Archivní zprávy ČSAV", null, null),
                mods("MODS_VOLUME_0001", null, "2000", null),
                mods("MODS_ISSUE_0001", null, "1", DEACTIVATED));
        checker(ok).check(DEACTIVATED);
        File bad = mets(
                mods("MODS_TITLE_0001", "Archivní zprávy ČSAV", null, null),
                mods("MODS_VOLUME_0001", null, "1999", null),
                mods("MODS_ISSUE_0001", null, "1", DEACTIVATED));
        UrnNbnResolverChecker.ResolverWarning e = expectWarning(checker(bad), DEACTIVATED);
        assertTrue(e.getMessage(), e.getMessage().contains("'1999' != '2000'"));
    }

    @Test
    public void testInstanceDiffersFromProduction() throws Exception {
        //stejne URN je v produkci ACTIVE, v testovaci instanci FREE
        instance = "test";
        try {
            checker(null).check(MZK);
            fail("FREE musí skončit chybou");
        } catch (UrnNbnResolverChecker.ResolverError e) {
            assertTrue(e.getMessage(), e.getMessage().contains("není registrován"));
        }
    }
}
