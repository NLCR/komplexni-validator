package nkp.pspValidator.shared;

import nkp.pspValidator.shared.engine.Level;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;

import static org.junit.Assert.assertEquals;

public class ValidatorProtocolTextBuilderTest {

    private String log(String message, String referencedFile) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(bytes, true, "UTF-8");
        new ValidatorProtocolTextBuilder(ps).logRuleError(Level.ERROR, message, referencedFile);
        return bytes.toString("UTF-8").trim();
    }

    @Test
    public void bezReferencedFile() throws Exception {
        assertEquals("ERROR: hláška", log("hláška", null));
    }

    @Test
    public void sReferencedFile() throws Exception {
        assertEquals("ERROR: hláška (soubor: pdf/a.pdf)", log("hláška", "pdf/a.pdf"));
    }

    @Test
    public void cestaVHlasceSeNeduplikuje() throws Exception {
        assertEquals("ERROR: soubor pdf/a.pdf je vadný", log("soubor pdf/a.pdf je vadný", "pdf/a.pdf"));
    }

    @Test
    public void cestaUvnitrBalickuVHlasceSeNeduplikuje() throws Exception {
        assertEquals("ERROR: soubor pdf/a.pdf je vadný", log("soubor pdf/a.pdf je vadný", "balik/pdf/a.pdf"));
        assertEquals("ERROR: hláška (soubor: balik/pdf/a.pdf)", log("hláška", "balik/pdf/a.pdf"));
    }

    @Test
    public void relativniCesta() {
        File root = new File("/tmp/balik");
        // od adresare balicku vcetne, jako v XML protokolu; mimo balicek absolutni cesta
        assertEquals("balik/pdf/a.pdf", ValidatorProtocolTextBuilder.toPackageRelativePath(root, new File("/tmp/balik/pdf/a.pdf")));
        assertEquals(new File("/jinde/a.pdf").getAbsolutePath(), ValidatorProtocolTextBuilder.toPackageRelativePath(root, new File("/jinde/a.pdf")));
        assertEquals(null, ValidatorProtocolTextBuilder.toPackageRelativePath(root, null));
    }
}
