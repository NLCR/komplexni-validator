package nkp.pspValidator.shared.engine;

import nkp.pspValidator.shared.*;
import nkp.pspValidator.shared.externalUtils.ExternalUtil;
import nkp.pspValidator.shared.externalUtils.ExternalUtilManager;
import nkp.pspValidator.shared.externalUtils.ExternalUtilManagerFactory;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.util.Set;

/**
 * Pomocny kod pro testy nad vsemi fDMF (AllFdmfSmokeTest, FdmfRegistryTest).
 */
final class FdmfTestSupport {

    static final File VALIDATOR_CONFIG_DIR = new File("src/main/resources/nkp/pspValidator/shared/validatorConfig");

    private FdmfTestSupport() {
    }

    static ValidatorConfigurationManager configManager() throws Exception {
        return new ValidatorConfigurationManager(VALIDATOR_CONFIG_DIR);
    }

    /**
     * Adresar fDMF v konfiguraci, napr. "monograph_2.3".
     */
    static File[] fdmfDirs() {
        File[] dirs = new File(VALIDATOR_CONFIG_DIR, "fDMF").listFiles(File::isDirectory);
        java.util.Arrays.sort(dirs);
        return dirs;
    }

    /**
     * Prefix nazvu adresare fDMF pro dany typ (viz FdmfRegistry.init).
     */
    static String dirPrefix(Dmf.Type type) {
        switch (type) {
            case MONOGRAPH:
                return "monograph";
            case PERIODICAL:
                return "periodical";
            case AUDIO_GRAM:
                return "audio_gram";
            case AUDIO_FONO:
                return "audio_fono";
            case AUDIO_DISC:
                return "audio_disc";
            case AUDIO_NO_CARRIER:
                return "audio_no_carrier";
            case DATA_DISC:
                return "data_disc";
            case FUND_UNIT:
                return "fund_unit";
            default:
                throw new IllegalStateException("neznamy typ " + type);
        }
    }

    /**
     * Dmf z nazvu adresare fDMF; nejdelsi shodny prefix (audio_gram_0.3 nesmi byt chybne rozpoznano jinak).
     */
    static Dmf dmfFromDirName(String dirName) {
        for (Dmf.Type type : Dmf.Type.values()) {
            String prefix = dirPrefix(type) + "_";
            if (dirName.startsWith(prefix)) {
                return new Dmf(type, dirName.substring(prefix.length()));
            }
        }
        throw new IllegalStateException("adresar fDMF neodpovida zadnemu typu: " + dirName);
    }

    static Set<String> versions(FdmfRegistry registry, Dmf.Type type) {
        switch (type) {
            case MONOGRAPH:
                return registry.getMonographFdmfVersions();
            case PERIODICAL:
                return registry.getPeriodicalFdmfVersions();
            case AUDIO_GRAM:
                return registry.getAudioGramFdmfVersions();
            case AUDIO_FONO:
                return registry.getAudioFonoFdmfVersions();
            case AUDIO_DISC:
                return registry.getAudioDiscFdmfVersions();
            case AUDIO_NO_CARRIER:
                return registry.getAudioNoCarrierFdmfVersions();
            case DATA_DISC:
                return registry.getDataDiscFdmfVersions();
            case FUND_UNIT:
                return registry.getFundUnitFdmfVersions();
            default:
                throw new IllegalStateException("neznamy typ " + type);
        }
    }

    /**
     * Spusti validator s vynucenou fDMF nad danym adresarem, vsechny externi nastroje jsou vypnute.
     *
     * @return log validace (verbosity 3)
     */
    static String run(FdmfRegistry registry, ValidatorConfigurationManager configManager, Dmf dmf, File pspRootDir) throws Exception {
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
        return buffer.toString("UTF-8");
    }
}
