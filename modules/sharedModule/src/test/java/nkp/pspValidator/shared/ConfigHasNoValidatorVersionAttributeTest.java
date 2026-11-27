package nkp.pspValidator.shared;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Verzi konfigurace nese jen manifest.properties (viz {@link ConfigManifest}). Atribut validatorVersion, ktery ji
 * dřív nesl v každém souboru, byl odstraněn a nesmí se do konfigurace vrátit (ani do XSD, protože by se pak
 * znovu stal povinným).
 */
public class ConfigHasNoValidatorVersionAttributeTest {

    private static final File CONFIG_DIR = new File("src/main/resources/nkp/pspValidator/shared/validatorConfig");

    @Test
    public void noFileOfSourceConfigurationMentionsValidatorVersion() throws IOException {
        assertTrue("neexistuje " + CONFIG_DIR.getAbsolutePath(), CONFIG_DIR.isDirectory());
        List<String> offending = new ArrayList<>();
        try (Stream<Path> paths = Files.walk(CONFIG_DIR.toPath())) {
            for (Path path : (Iterable<Path>) paths.filter(Files::isRegularFile)::iterator) {
                String content = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
                if (content.contains("validatorVersion")) {
                    offending.add(CONFIG_DIR.toPath().relativize(path).toString());
                }
            }
        }
        assertEquals("soubory konfigurace obsahují validatorVersion (verzi nese jen " + ConfigManifest.FILE_NAME + "): " + offending,
                0, offending.size());
    }
}
