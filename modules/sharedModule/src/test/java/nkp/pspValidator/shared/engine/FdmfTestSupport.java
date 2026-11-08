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
 * Pomocny kod pro testy nad fDMF (AllFdmfSmokeTest, FdmfRegistryTest, FundUnitSmokeTest, DataDiscSmokeTest).
 */
final class FdmfTestSupport {

    static final File VALIDATOR_CONFIG_DIR = new File("src/main/resources/nkp/pspValidator/shared/validatorConfig");

    private static ValidatorConfigurationManager sharedConfigManager;
    private static FdmfRegistry sharedRegistry;

    private FdmfTestSupport() {
    }

    static ValidatorConfigurationManager configManager() throws Exception {
        return new ValidatorConfigurationManager(VALIDATOR_CONFIG_DIR);
    }

    /**
     * Jedna instance pro vsechny testy v JVM (lina inicializace). Nacteni konfigurace a stavba FdmfRegistry (XSD validace
     * vsech fDMF) trva kolem 0,4 s a smoke testy ji jinak opakovaly v kazdem testu. Sdileni je bezpecne: ValidatorConfigurationManager,
     * DictionaryManager a FdmfConfiguration po inicializaci drzi jen soubory a slovniky, validace je nemeni; Engine,
     * profily metadat i ExternalUtilManager si kazdy beh stavi znovu (viz run, ValidatorFactory.buildValidator).
     * Vyjimka: FdmfConfiguration.initBinaryFileProfiles prepise BinaryFileValidator, run ho vola pred kazdym behem
     * se vsemi nastroji vypnutymi, takze vysledek je vzdy stejny (testy bezi v jednom vlakne).
     */
    static synchronized ValidatorConfigurationManager sharedConfigManager() throws Exception {
        if (sharedConfigManager == null) {
            sharedConfigManager = configManager();
        }
        return sharedConfigManager;
    }

    static synchronized FdmfRegistry sharedRegistry() throws Exception {
        if (sharedRegistry == null) {
            sharedRegistry = new FdmfRegistry(sharedConfigManager());
        }
        return sharedRegistry;
    }

    /**
     * Jako run(registry, configManager, dmf, dir), se sdilenou konfiguraci a FdmfRegistry.
     */
    static String run(Dmf dmf, File pspRootDir) throws Exception {
        return run(sharedRegistry(), sharedConfigManager(), dmf, pspRootDir);
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
