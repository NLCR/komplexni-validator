package nkp.pspValidator.shared;

import nkp.pspValidator.shared.externalUtils.ExternalUtil;
import org.junit.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.EnumSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static nkp.pspValidator.shared.externalUtils.ExternalUtil.*;
import static org.junit.Assert.*;

/**
 * FdmfRegistry.getExternalUtilsUsed: ktere externi nastroje pravidla fDMF pouzivaji (GUI podle toho ukazuje nastroje
 * relevantni pro rozpoznany typ dokumentu). Bez nacteni konfigurace fDMF.
 */
public class FdmfExternalUtilsUsedTest {

    private static final File CONFIG_DIR = new File("src/main/resources/nkp/pspValidator/shared/validatorConfig");
    private static final Pattern UTIL_VALUE = Pattern.compile("type=\"EXTERNAL_UTIL\">\\s*([A-Z_0-9]+)\\s*<");

    private static FdmfRegistry registry() throws Exception {
        return new FdmfRegistry(new ValidatorConfigurationManager(CONFIG_DIR));
    }

    @Test
    public void imageToolsForFundUnitMonographAndDataDisc() throws Exception {
        FdmfRegistry registry = registry();
        Set<ExternalUtil> images = EnumSet.of(IMAGE_MAGICK, JHOVE, JPYLYZER, KAKADU);
        assertEquals(images, registry.getExternalUtilsUsed(new Dmf(Dmf.Type.FUND_UNIT, "0.2")));
        assertEquals(images, registry.getExternalUtilsUsed(new Dmf(Dmf.Type.MONOGRAPH, "2.3")));
        assertEquals(images, registry.getExternalUtilsUsed(new Dmf(Dmf.Type.DATA_DISC, "0.1")));
        //konfigurace se pri tom nenacita
        assertEquals(0, registry.getLoadedFdmfConfigCount());
    }

    @Test
    public void pdfAndEpubToolsForEpublications() throws Exception {
        FdmfRegistry registry = registry();
        assertEquals(EnumSet.of(VERAPDF, EPUBCHECK), registry.getExternalUtilsUsed(new Dmf(Dmf.Type.EMONOGRAPH, "3.1")));
        assertEquals(EnumSet.of(VERAPDF, EPUBCHECK), registry.getExternalUtilsUsed(new Dmf(Dmf.Type.EPERIODICAL, "2.6")));
        assertEquals(EnumSet.of(VERAPDF, EPUBCHECK, IMAGE_MAGICK, JHOVE, JPYLYZER, KAKADU),
                registry.getExternalUtilsUsed(new Dmf(Dmf.Type.COMPERIODICAL, "1.3")));
    }

    @Test
    public void audioToolsForAudio() throws Exception {
        assertEquals(EnumSet.of(IMAGE_MAGICK, JHOVE, JPYLYZER, KAKADU, MP3VAL, SHNTOOL, CHECKMATE),
                registry().getExternalUtilsUsed(new Dmf(Dmf.Type.AUDIO_GRAM, "1.0")));
    }

    @Test
    public void unknownFdmf() throws Exception {
        try {
            registry().getExternalUtilsUsed(new Dmf(Dmf.Type.MONOGRAPH, "9.9"));
            fail("neexistujici fDMF mela vyhodit UnknownFdmfException");
        } catch (FdmfRegistry.UnknownFdmfException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("Není definována fDMF"));
        }
    }

    /**
     * Pro vsechny vydavane fDMF stejny vysledek jako prosty textovy vyskyt v rules.xml (v rules.xml nejsou
     * zakomentovana pravidla s nastroji, takze se oba zpusoby shoduji).
     */
    @Test
    public void allFdmfMatchTextOccurrences() throws Exception {
        FdmfRegistry registry = registry();
        File fdmfRoot = new File(CONFIG_DIR, "fDMF");
        int checked = 0;
        for (Dmf.Type type : Dmf.Type.values()) {
            String prefix = FdmfRegistry.fdmfDirPrefix(type);
            File[] dirs = fdmfRoot.listFiles((dir, name) -> name.matches(prefix + "_[0-9]+(\\.([0-9])+)*"));
            assertNotNull(dirs);
            for (File dir : dirs) {
                String version = dir.getName().substring(prefix.length() + 1);
                Set<ExternalUtil> expected = EnumSet.noneOf(ExternalUtil.class);
                String rules = new String(Files.readAllBytes(new File(dir, "rules.xml").toPath()), StandardCharsets.UTF_8);
                Matcher m = UTIL_VALUE.matcher(rules);
                while (m.find()) {
                    expected.add(ExternalUtil.valueOf(m.group(1)));
                }
                assertEquals(dir.getName(), expected, registry.getExternalUtilsUsed(new Dmf(type, version)));
                checked++;
            }
        }
        assertTrue("fDMF: " + checked, checked >= 35);
    }
}
