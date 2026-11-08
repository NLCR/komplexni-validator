package nkp.pspValidator.shared.engine;

import nkp.pspValidator.shared.Dmf;
import nkp.pspValidator.shared.DmfDetector;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertTrue;

/**
 * Smoke test fDMF data_disc_0.1: konfigurace se nacte, validator se sestavi a projde vsechna pravidla nad minimalnim
 * balickem (jen info a mets) bez padu funkce nebo nesplneneho kontraktu. Neresi, ze balicek je validni.
 * Stejny princip jako FundUnitSmokeTest.buildAndRunOverMinimalPackage.
 */
public class DataDiscSmokeTest {

    @Test
    public void buildAndRunOverMinimalPackage() throws Exception {
        File pspRootDir = new File("src/test/resources/data_disc_0.1/minimal/nk-00028d");
        Dmf dmf = new DmfDetector().resolveDmf(pspRootDir, new DmfDetector.Params());
        assertTrue("ocekavan typ DATA_DISC", dmf.getType() == Dmf.Type.DATA_DISC);
        String log = FdmfTestSupport.run(dmf, pspRootDir);
        for (String line : log.split("\n")) {
            boolean crash = (line.contains("nečekaná chyba") && !line.contains("EmptyParamEvaluationException"))
                    || line.contains("nesplněn kontrakt");
            assertTrue("konfigurace fDMF obsahuje chybu nebo funkce spadla: " + line.trim(), !crash);
        }
        //pravidla data_disc se opravdu spustila (ne jen detekce typu) a validace dobehla do souhrnu
        assertTrue("v logu chybí pravidlo NUMBER_OF_ISO_FILES_IN_DIRS_MATCH z data_disc_0.1", log.contains("NUMBER_OF_ISO_FILES_IN_DIRS_MATCH"));
        assertTrue("v logu chybí souhrn validace", log.contains("balík je:"));
    }
}
