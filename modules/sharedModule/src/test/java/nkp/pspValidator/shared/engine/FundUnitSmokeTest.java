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
 * Smoke test fDMF fund_unit_0.1: konfigurace se nacte, validator se sestavi a projde vsechna pravidla
 * nad minimalnim balickem bez vyjimky. Neresi, ze balicek je validni (neni, chybi mu vetsina souboru).
 */
public class FundUnitSmokeTest {

    @Test
    public void buildAndRunOverMinimalPackage() throws Exception {
        File validatorConfigDir = new File("src/main/resources/nkp/pspValidator/shared/validatorConfig");
        File pspRootDir = new File("src/test/resources/fund_unit_0.1/clipping/nk-00027x");
        ValidatorConfigurationManager configManager = new ValidatorConfigurationManager(validatorConfigDir);
        FdmfRegistry registry = new FdmfRegistry(configManager);
        Dmf dmf = new DmfDetector().resolveDmf(pspRootDir, new DmfDetector.Params());
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
        System.out.println(log);
        // chyby konfigurace nebo pad funkce se projevi jako "necekana chyba" / kontrakt v jednotlivych pravidlech
        // (texty viz ValidationFunction.invalidUnexpectedError / invalidContractNotMet).
        // EmptyParamEvaluationException je ocekavana: minimalni balicek nema adresare s daty a stejne se chovaji
        // i ostatni fDMF (chybejici srozumitelna hlaska je samostatny problem validatoru, ne fund_unit).
        for (String line : log.split("\n")) {
            boolean crash = (line.contains("nečekaná chyba") && !line.contains("EmptyParamEvaluationException"))
                    || line.contains("nesplněn kontrakt");
            assertTrue("konfigurace fDMF obsahuje chybu nebo funkce spadla: " + line.trim(), !crash);
        }
    }
}
