package nkp.pspValidator.shared.engine.utils;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Nastaveni URL Resolveru (https, http jen s povolenim) a dotazy na lokalni HTTP server misto skutecneho Resolveru.
 * Odpovedi atrapy maji jen tvar, ktery kod cte (urnNbn.status, error, digitalDocument.*.titleInfo); netvrdi nic dalsiho
 * o chovani skutecneho Resolveru.
 */
public class UrnNbnResolverCheckerTest {

    private static final String URN_NBN = "urn:nbn:cz:abg001-000023";

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private HttpServer server;
    private String serverUrl;
    /**
     * Odpovedi atrapy podle URN:NBN (posledni segment cesty); kdyz chybi, /api/v5/urnnbn/ vraci RESERVED.
     */
    private final Map<String, Response> statusResponses = new HashMap<>();
    private final Map<String, Response> resolverResponses = new HashMap<>();

    private static class Response {
        final int code;
        final String contentType;
        final String body;

        Response(int code, String contentType, String body) {
            this.code = code;
            this.contentType = contentType;
            this.body = body;
        }

        static Response json(String body) {
            return new Response(200, "application/json; charset=UTF-8", body);
        }
    }

    private static String urnFromPath(HttpExchange exchange) {
        String path = exchange.getRequestURI().getPath();
        return path.substring(path.lastIndexOf('/') + 1);
    }

    private static void send(HttpExchange exchange, Response response) throws IOException {
        byte[] body = response.body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", response.contentType);
        exchange.sendResponseHeaders(response.code, body.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(body);
        }
    }

    @Before
    public void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/api/v5/urnnbn/", exchange -> {
            String urn = urnFromPath(exchange);
            Response response = statusResponses.get(urn);
            if (response == null) {
                response = Response.json(String.format("{\"urnNbn\":{\"value\":\"%s\",\"status\":\"RESERVED\"}}", urn));
            }
            send(exchange, response);
        });
        server.createContext("/api/v5/resolver/", exchange -> {
            Response response = resolverResponses.get(urnFromPath(exchange));
            send(exchange, response != null ? response : new Response(404, "text/plain", "not found"));
        });
        server.createContext("/redirect/", exchange -> {
            exchange.getResponseHeaders().add("Location", "https://resolver-test.nkp.cz" + exchange.getRequestURI().getPath().substring("/redirect".length()));
            exchange.sendResponseHeaders(301, -1);
            exchange.close();
        });
        server.start();
        serverUrl = "http://" + server.getAddress().getHostString() + ":" + server.getAddress().getPort();
    }

    @After
    public void tearDown() {
        server.stop(0);
        UrnNbnResolverChecker.setResolverBaseUrl(null);
    }

    @Test
    public void defaultIsProduction() {
        UrnNbnResolverChecker.setResolverBaseUrl(null);
        assertEquals(UrnNbnResolverChecker.DEFAULT_RESOLVER_BASE_URL, UrnNbnResolverChecker.getResolverBaseUrl());
        UrnNbnResolverChecker.setResolverBaseUrl("  ");
        assertEquals(UrnNbnResolverChecker.DEFAULT_RESOLVER_BASE_URL, UrnNbnResolverChecker.getResolverBaseUrl());
    }

    @Test
    public void httpsAcceptedTrailingSlashRemoved() {
        UrnNbnResolverChecker.setResolverBaseUrl("https://resolver-test.nkp.cz/");
        assertEquals("https://resolver-test.nkp.cz", UrnNbnResolverChecker.getResolverBaseUrl());
    }

    @Test
    public void httpRejectedWithoutAllowHttp() {
        try {
            UrnNbnResolverChecker.setResolverBaseUrl(serverUrl);
            fail("http bez povolení musí selhat");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("--urnnbn-resolver-allow-http"));
        }
        assertEquals(UrnNbnResolverChecker.DEFAULT_RESOLVER_BASE_URL, UrnNbnResolverChecker.getResolverBaseUrl());
    }

    @Test
    public void invalidUrlsRejected() {
        for (String url : new String[]{"resolver-test.nkp.cz", "ftp://resolver-test.nkp.cz", "https://"}) {
            try {
                UrnNbnResolverChecker.setResolverBaseUrl(url, true);
                fail("neplatná URL musí selhat: " + url);
            } catch (IllegalArgumentException e) {
                //ok
            }
        }
    }

    @Test
    public void httpAllowedReadsStatus() throws Exception {
        UrnNbnResolverChecker.setResolverBaseUrl(serverUrl, true);
        UrnNbnResolverChecker checker = new UrnNbnResolverChecker(null);
        try {
            checker.check(URN_NBN);
            fail("stav RESERVED musí skončit varováním");
        } catch (UrnNbnResolverChecker.ResolverWarning e) {
            assertTrue(e.getMessage(), e.getMessage().contains("rezervován"));
        }
    }

    @Test
    public void redirectBetweenProtocolsReported() throws Exception {
        UrnNbnResolverChecker.setResolverBaseUrl(serverUrl + "/redirect", true);
        UrnNbnResolverChecker checker = new UrnNbnResolverChecker(null);
        try {
            checker.check(URN_NBN);
            fail("přesměrování na https musí skončit chybou");
        } catch (IOException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("přesměroval"));
            assertTrue(e.getMessage(), e.getMessage().contains("https://resolver-test.nkp.cz/api/v5/urnnbn/"));
        }
    }

    // ---------- pomocne metody pro testy porovnani metadat ----------

    /**
     * URN:NBN je v Resolveru aktivni, detail digitalniho dokumentu vraci danou entitu (napr. monograph) s danym titleInfo.
     */
    private void activeWithEntity(String urnNbn, String entityKey, String titleInfoJson) {
        statusResponses.put(urnNbn, Response.json(String.format("{\"urnNbn\":{\"value\":\"%s\",\"status\":\"ACTIVE\"}}", urnNbn)));
        String entity = titleInfoJson == null ? "{}" : "{\"titleInfo\":" + titleInfoJson + "}";
        resolverResponses.put(urnNbn, Response.json(String.format("{\"digitalDocument\":{\"%s\":%s}}", entityKey, entity)));
    }

    /**
     * Minimalni METS s jednim zaznamem MODS (ID MODS_{type}_0001) s nazvem, cislem casti a URN:NBN.
     */
    private File metsWithMods(String modsType, String title, String partNumber, String urnNbn) throws IOException {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<mets:mets xmlns:mets=\"http://www.loc.gov/METS/\" xmlns:mods=\"http://www.loc.gov/mods/v3\">"
                + "<mets:dmdSec ID=\"MODSMD_" + modsType + "_0001\"><mets:mdWrap><mets:xmlData>"
                + "<mods:mods ID=\"MODS_" + modsType + "_0001\"><mods:titleInfo>"
                + (title == null ? "" : "<mods:title>" + title + "</mods:title>")
                + (partNumber == null ? "" : "<mods:partNumber>" + partNumber + "</mods:partNumber>")
                + "</mods:titleInfo><mods:identifier type=\"urnnbn\">" + urnNbn + "</mods:identifier></mods:mods>"
                + "</mets:xmlData></mets:mdWrap></mets:dmdSec></mets:mets>";
        File file = tmp.newFile();
        Files.write(file.toPath(), xml.getBytes(StandardCharsets.UTF_8));
        return file;
    }

    private UrnNbnResolverChecker checker(File metsFile) throws Exception {
        UrnNbnResolverChecker.setResolverBaseUrl(serverUrl, true);
        return new UrnNbnResolverChecker(metsFile);
    }

    // ---------- B2: '%' ve zprave ----------

    @Test
    public void percentInTitleReportedAsWarning() throws Exception {
        activeWithEntity(URN_NBN, "monograph", "{\"title\":\"Sleva 100 % na vše\"}");
        UrnNbnResolverChecker checker = checker(metsWithMods("VOLUME", "Sleva 100 % na nic", null, URN_NBN));
        try {
            checker.check(URN_NBN);
            fail("rozdílný název musí skončit varováním");
        } catch (UrnNbnResolverChecker.ResolverWarning e) {
            assertTrue(e.getMessage(), e.getMessage().contains("Sleva 100 % na vše"));
            assertTrue(e.getMessage(), e.getMessage().contains("Sleva 100 % na nic"));
        }
    }

    // ---------- B3: neplatna odpoved ----------

    @Test
    public void htmlInsteadOfJsonIsErrorForThisUrn() throws Exception {
        statusResponses.put(URN_NBN, new Response(502, "text/html", "<html><body><h1>502 Bad Gateway</h1></body></html>"));
        try {
            checker(null).check(URN_NBN);
            fail("HTML místo JSON musí skončit chybou");
        } catch (UrnNbnResolverChecker.ResolverError e) {
            assertTrue(e.getMessage(), e.getMessage().startsWith("neplatná odpověď Resolveru: "));
            assertTrue(e.getMessage(), e.getMessage().contains("HTTP 502"));
        }
    }

    @Test
    public void missingStatusIsErrorForThisUrn() throws Exception {
        statusResponses.put(URN_NBN, Response.json(String.format("{\"urnNbn\":{\"value\":\"%s\"}}", URN_NBN)));
        try {
            checker(null).check(URN_NBN);
            fail("odpověď bez status musí skončit chybou");
        } catch (UrnNbnResolverChecker.ResolverError e) {
            assertTrue(e.getMessage(), e.getMessage().startsWith("neplatná odpověď Resolveru: "));
            assertTrue(e.getMessage(), e.getMessage().contains("status"));
        }
    }

    @Test
    public void missingTitleInfoMeansNothingToCompare() throws Exception {
        activeWithEntity(URN_NBN, "monograph", null);
        checker(metsWithMods("VOLUME", "Název", null, URN_NBN)).check(URN_NBN);
    }

    @Test
    public void missingDigitalDocumentIsWarningForThisUrn() throws Exception {
        //registrace je potvrzena (ACTIVE), jen porovnani metadat nejde: WARNING jako u neshody metadat, ne ERROR
        activeWithEntity(URN_NBN, "monograph", "{}");
        resolverResponses.put(URN_NBN, Response.json("{}"));
        try {
            checker(metsWithMods("VOLUME", "Název", null, URN_NBN)).check(URN_NBN);
            fail("odpověď bez digitalDocument musí skončit varováním");
        } catch (UrnNbnResolverChecker.ResolverWarning e) {
            assertTrue(e.getMessage(), e.getMessage().contains("nelze porovnat s Resolverem"));
        }
    }

    // ---------- B4: typ entity Resolveru vs. typ zaznamu MODS ----------

    @Test
    public void otherEntityMatchesSoundCollection() throws Exception {
        activeWithEntity(URN_NBN, "otherEntity", "{\"title\":\"Gramodeska\"}");
        checker(metsWithMods("SOUNDCOLLECTION", "Gramodeska", null, URN_NBN)).check(URN_NBN);
    }

    @Test
    public void otherEntityMatchesDataCollection() throws Exception {
        activeWithEntity(URN_NBN, "otherEntity", "{\"title\":\"Disketa\"}");
        UrnNbnResolverChecker checker = checker(metsWithMods("DATACOLLECTION", "Jiná disketa", null, URN_NBN));
        try {
            checker.check(URN_NBN);
            fail("rozdílný název musí skončit varováním");
        } catch (UrnNbnResolverChecker.ResolverWarning e) {
            //zaznam MODS nalezen a porovnan, ne hlaska o nenalezenem zaznamu
            assertTrue(e.getMessage(), e.getMessage().contains("MODS.type:DATACOLLECTION"));
        }
    }

    @Test
    public void otherEntityMatchesUnit() throws Exception {
        activeWithEntity(URN_NBN, "otherEntity", "{\"title\":\"Jednotka\"}");
        checker(metsWithMods("UNIT", "Jednotka", null, URN_NBN)).check(URN_NBN);
    }

    @Test
    public void monographMatchesSupplement() throws Exception {
        activeWithEntity(URN_NBN, "monograph", "{\"title\":\"Příloha\"}");
        checker(metsWithMods("SUPPL", "Příloha", null, URN_NBN)).check(URN_NBN);
    }

    @Test
    public void monographVolumeMatchesSupplement() throws Exception {
        activeWithEntity(URN_NBN, "monographVolume", "{\"monographTitle\":\"Příloha\",\"volumeTitle\":\"2\"}");
        checker(metsWithMods("SUPPL", "Příloha", "2", URN_NBN)).check(URN_NBN);
    }

    @Test
    public void entityTypeWithoutMatchingModsIsWarning() throws Exception {
        activeWithEntity(URN_NBN, "otherEntity", "{\"title\":\"Svazek\"}");
        UrnNbnResolverChecker checker = checker(metsWithMods("VOLUME", "Svazek", null, URN_NBN));
        try {
            checker.check(URN_NBN);
            fail("typ entity bez odpovídajícího záznamu MODS musí skončit varováním");
        } catch (UrnNbnResolverChecker.ResolverWarning e) {
            assertTrue(e.getMessage(), e.getMessage().contains("typ entity v Resolveru (OTHER) neodpovídá žádnému záznamu MODS s tímto URN:NBN"));
            assertTrue(e.getMessage(), e.getMessage().contains("VOLUME"));
        }
    }

    // ---------- B7: chybejici klic v titleInfo se nekontroluje ----------

    @Test
    public void missingTitleInfoKeyIsNotCompared() throws Exception {
        //Resolver nevratil volumeTitle, v MODS je partNumber 1: neporovnava se
        activeWithEntity(URN_NBN, "monographVolume", "{\"monographTitle\":\"Sebrané spisy\"}");
        checker(metsWithMods("VOLUME", "Sebrané spisy", "1", URN_NBN)).check(URN_NBN);
    }

    @Test
    public void presentTitleInfoKeyIsStillCompared() throws Exception {
        activeWithEntity(URN_NBN, "monographVolume", "{\"monographTitle\":\"Sebrané spisy\",\"volumeTitle\":\"2\"}");
        UrnNbnResolverChecker checker = checker(metsWithMods("VOLUME", "Sebrané spisy", "1", URN_NBN));
        try {
            checker.check(URN_NBN);
            fail("rozdílné číslo svazku musí skončit varováním");
        } catch (UrnNbnResolverChecker.ResolverWarning e) {
            assertTrue(e.getMessage(), e.getMessage().contains("'1' != '2'"));
        }
    }

    // ---------- UTF-8 v odpovedi ----------

    @Test
    public void utf8InResponseMatchesCzechTitle() throws Exception {
        String title = "Příliš žluťoučký kůň úpěl ďábelské ódy";
        activeWithEntity(URN_NBN, "monograph", "{\"title\":\"" + title + "\"}");
        checker(metsWithMods("VOLUME", title, null, URN_NBN)).check(URN_NBN);
    }
}
