package nkp.pspValidator.shared;

import org.junit.Assume;
import org.junit.Test;

import java.io.File;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Properties;

import static org.junit.Assert.*;

/**
 * Manifest konfigurace ve zdrojích musí odpovídat verzi aplikace (MAJOR.MINOR z appVersion v gradle.properties),
 * aby se při změně verze nezapomnělo upravit configVersion.
 */
public class ConfigManifestVersionTest {

    private static final File MANIFEST = new File("src/main/resources/nkp/pspValidator/shared/validatorConfig/" + ConfigManifest.FILE_NAME);

    @Test
    public void configVersionMatchesAppVersion() throws Exception {
        Assume.assumeFalse("záložní verze aplikace (běh bez version.properties), není s čím porovnat",
                Version.FALLBACK_VERSION_CODE.equals(Version.VERSION_CODE));
        assertTrue("chybí " + MANIFEST.getPath(), MANIFEST.isFile());
        Properties props = new Properties();
        try (Reader in = new InputStreamReader(Files.newInputStream(MANIFEST.toPath()), StandardCharsets.UTF_8)) {
            props.load(in);
        }
        int[] app = ConfigManifest.parseMajorMinor(Version.VERSION_CODE);
        String expected = app[0] + "." + app[1];
        assertEquals("Verze aplikace je " + Version.VERSION_CODE + ". Při změně verze na " + expected + " uprav configVersion v "
                + MANIFEST.getPath() + " na " + expected + ".", expected, String.valueOf(props.getProperty(ConfigManifest.KEY_CONFIG_VERSION)).trim());
    }
}
