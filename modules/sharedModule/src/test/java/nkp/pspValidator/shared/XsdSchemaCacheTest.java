package nkp.pspValidator.shared;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.xml.sax.SAXException;

import javax.xml.validation.Schema;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.Assert.*;

/**
 * Cache zkompilovanych XSD: stejne XSD dava stejny objekt Schema, kazda varianta (bez resolveru, s resolverem importu)
 * ma vlastni zaznam, neuspesna kompilace se necachuje.
 */
public class XsdSchemaCacheTest {

    private static final File FDMF_XSD_DIR = new File("src/main/resources/nkp/pspValidator/shared/validatorConfig/fDMF/monograph_2.0/xsd");
    private static final File CONFIG_XSD = new File("src/main/resources/nkp/pspValidator/shared/validatorConfig/xsd/fdmfConfig.xsd");

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    @Test
    public void sameSchemaObjectForSameXsd() throws Exception {
        XsdSchemaCache cache = new XsdSchemaCache();
        Schema first = cache.getSchema(CONFIG_XSD);
        //jiny objekt File se stejnou cestou
        Schema second = cache.getSchema(new File(CONFIG_XSD.getAbsolutePath()));
        assertSame(first, second);
        assertEquals(1, cache.getCompilationCount());
    }

    @Test
    public void importsResolverVariantIsCachedSeparately() throws Exception {
        XsdSchemaCache cache = new XsdSchemaCache();
        File premis = premisXsd();
        Schema withResolver = cache.getSchemaWithImportsResolver(premis);
        assertSame(withResolver, cache.getSchemaWithImportsResolver(premis));
        assertEquals(1, cache.getCompilationCount());
        Schema plain = cache.getSchema(CONFIG_XSD);
        assertNotSame(withResolver, plain);
        assertEquals(2, cache.getCompilationCount());
    }

    @Test
    public void differentXsdGivesDifferentSchema() throws Exception {
        XsdSchemaCache cache = new XsdSchemaCache();
        File metadataProfileXsd = new File(CONFIG_XSD.getParentFile(), "metadataProfile.xsd");
        assertNotSame(cache.getSchema(CONFIG_XSD), cache.getSchema(metadataProfileXsd));
        assertEquals(2, cache.getCompilationCount());
    }

    @Test
    public void failedCompilationIsNotCached() throws Exception {
        XsdSchemaCache cache = new XsdSchemaCache();
        File xsd = tmp.newFile("broken.xsd");
        Files.write(xsd.toPath(), "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"><xs:element/>".getBytes(StandardCharsets.UTF_8));
        String firstMessage = null;
        for (int i = 0; i < 2; i++) {
            try {
                cache.getSchema(xsd);
                fail("neplatne XSD melo vyhodit SAXException");
            } catch (SAXException e) {
                if (firstMessage == null) {
                    firstMessage = e.getMessage();
                } else {
                    assertEquals(firstMessage, e.getMessage());
                }
            }
        }
        assertEquals(0, cache.getCompilationCount());
        //po oprave souboru se schema zkompiluje
        Files.write(xsd.toPath(), ("<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">"
                + "<xs:element name=\"a\" type=\"xs:string\"/></xs:schema>").getBytes(StandardCharsets.UTF_8));
        assertNotNull(cache.getSchema(xsd));
        assertEquals(1, cache.getCompilationCount());
    }

    @Test
    public void concurrentAccessReturnsOneSharedSchema() throws Exception {
        XsdSchemaCache cache = new XsdSchemaCache();
        File premis = premisXsd();
        ExecutorService executor = Executors.newFixedThreadPool(6);
        try {
            List<Future<Schema>> futures = new ArrayList<>();
            for (int i = 0; i < 12; i++) {
                futures.add(executor.submit((Callable<Schema>) () -> cache.getSchemaWithImportsResolver(premis)));
            }
            Schema first = futures.get(0).get();
            for (Future<Schema> future : futures) {
                assertSame(first, future.get());
            }
        } finally {
            executor.shutdownNow();
        }
        assertSame(cache.getSchemaWithImportsResolver(premis), cache.getSchemaWithImportsResolver(premis));
    }

    private static File premisXsd() {
        File[] files = FDMF_XSD_DIR.listFiles((dir, name) -> name.matches("premis_[0-9.]+\\.xsd"));
        assertNotNull(files);
        assertEquals(1, files.length);
        return files[0];
    }

    /**
     * Zmena XSD souboru za behu (GUI drzi cache mezi validacemi) vede k nove kompilaci, ne k zastaralemu schematu.
     */
    @Test
    public void changedXsdFileIsRecompiled() throws Exception {
        File xsd = tmp.newFile("a.xsd");
        String schema = "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"><xs:element name=\"%s\" type=\"xs:string\"/></xs:schema>";
        java.nio.file.Files.write(xsd.toPath(), String.format(schema, "a").getBytes(java.nio.charset.StandardCharsets.UTF_8));
        XsdSchemaCache cache = new XsdSchemaCache();
        Schema first = cache.getSchema(xsd);
        org.junit.Assert.assertSame(first, cache.getSchema(xsd));
        java.nio.file.Files.write(xsd.toPath(), String.format(schema, "jine").getBytes(java.nio.charset.StandardCharsets.UTF_8));
        org.junit.Assert.assertTrue(xsd.setLastModified(xsd.lastModified() + 5000));
        Schema second = cache.getSchema(xsd);
        org.junit.Assert.assertNotSame(first, second);
        org.junit.Assert.assertEquals(2, cache.getCompilationCount());
    }
}
