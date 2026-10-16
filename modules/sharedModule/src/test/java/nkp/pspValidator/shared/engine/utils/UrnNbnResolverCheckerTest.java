package nkp.pspValidator.shared.engine.utils;

import com.sun.net.httpserver.HttpServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.*;

/**
 * Nastaveni URL Resolveru (https, http jen s povolenim) a dotazy na lokalni HTTP server misto skutecneho Resolveru.
 */
public class UrnNbnResolverCheckerTest {

    private static final String URN_NBN = "urn:nbn:cz:abg001-000023";

    private HttpServer server;
    private String serverUrl;

    @Before
    public void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/api/v5/urnnbn/", exchange -> {
            byte[] body = String.format("{\"urnNbn\":{\"value\":\"%s\",\"status\":\"RESERVED\"}}", URN_NBN).getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
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
}
