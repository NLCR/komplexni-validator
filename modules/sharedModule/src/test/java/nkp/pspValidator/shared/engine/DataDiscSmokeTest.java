package nkp.pspValidator.shared.engine;

import nkp.pspValidator.shared.*;
import nkp.pspValidator.shared.externalUtils.ExternalUtil;
import nkp.pspValidator.shared.externalUtils.ExternalUtilManager;
import nkp.pspValidator.shared.externalUtils.ExternalUtilManagerFactory;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;

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
        File validatorConfigDir = new File("src/main/resources/nkp/pspValidator/shared/validatorConfig");
        ValidatorConfigurationManager configManager = new ValidatorConfigurationManager(validatorConfigDir);
        FdmfRegistry registry = new FdmfRegistry(configManager);
        Dmf dmf = new DmfDetector().resolveDmf(pspRootDir, new DmfDetector.Params());
        assertTrue("ocekavan typ DATA_DISC", dmf.getType() == Dmf.Type.DATA_DISC);
        FdmfConfiguration fdmfConfig = registry.getFdmfConfig(dmf);
        ExternalUtilManager utilManager = new ExternalUtilManagerFactory(configManager.getExternalUtilsConfigFile())
                .buildExternalUtilManager(Platform.detectOs().getOperatingSystem());
        for (ExternalUtil util : ExternalUtil.values()) {
            utilManager.setUtilDisabled(util);
        }
        fdmfConfig.initBinaryFileProfiles(utilManager);
        Validator validator = ValidatorFactory.buildValidator(fdmfConfig, pspRootDir, configManager.getDictionaryManager());

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(buffer, true, "UTF-8");
        validator.run(pspRootDir, null, out, 3, null, null, null, null, dmf);
        String log = buffer.toString("UTF-8");
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
