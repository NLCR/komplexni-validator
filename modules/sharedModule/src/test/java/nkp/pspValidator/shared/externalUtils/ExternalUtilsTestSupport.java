package nkp.pspValidator.shared.externalUtils;

import nkp.pspValidator.shared.XmlUtils;
import nkp.pspValidator.shared.externalUtils.validation.BinaryFileProfile;
import nkp.pspValidator.shared.externalUtils.validation.BinaryFileValidator;
import org.w3c.dom.Document;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/**
 * Pomocny kod pro testy parsovani vystupu externich nastroju (Jpylyzer, JHOVE, Kakadu, ImageMagick, veraPDF, EPUBCheck)
 * bez samotnych nastroju.
 * <p>
 * Ulozene vystupy jsou v src/test/resources/externalUtils/&lt;nastroj&gt;/, popis (verze, prikaz, datum, synteticke vystupy)
 * v README.md tamtez.
 * <p>
 * Misto pro podstrceni hotoveho vystupu: ExternalUtilManager.runUtilExecution(ExternalUtilExecution, File) je verejna
 * a neni final, BinaryFileProfile.validate ji vola pres runExternalUtil. Podtrida {@link CannedOutputExternalUtilManager}
 * vraci ulozeny vystup misto spusteni procesu, takze profil z fDMF (BinaryFileValidator.registerProfile) i extrakce
 * a pravidla bezi nad realnym textem bez zmeny produkcniho kodu.
 */
public final class ExternalUtilsTestSupport {

    public static final File OUTPUTS_DIR = new File("src/test/resources/externalUtils");
    public static final File VALIDATOR_CONFIG_DIR = new File("src/main/resources/nkp/pspValidator/shared/validatorConfig");
    public static final File EXTERNAL_UTILS_CONFIG = new File(VALIDATOR_CONFIG_DIR, "externalUtils.xml");
    public static final File FDMF_DIR = new File(VALIDATOR_CONFIG_DIR, "fDMF");

    private ExternalUtilsTestSupport() {
    }

    /**
     * Ulozeny vystup nastroje, napr. output("jpylyzer", "tools_mc.xml").
     */
    public static File outputFile(String util, String name) {
        File file = new File(new File(OUTPUTS_DIR, util), name);
        if (!file.isFile()) {
            throw new IllegalStateException("chybí uložený výstup " + file.getPath());
        }
        return file;
    }

    public static String output(String util, String name) throws IOException {
        // konce radku vzdy LF (checkout s core.autocrlf by jinak rozbil viceradkove aserty); CliCommand dela totez
        return new String(Files.readAllBytes(outputFile(util, name).toPath()), StandardCharsets.UTF_8).replace("\r\n", "\n");
    }

    public static Document outputXml(String util, String name) throws Exception {
        return XmlUtils.buildDocumentFromString(output(util, name), true);
    }

    /**
     * Profil binarnich souboru pro dane fDMF z konfigurace aplikace a typ kopie, napr.
     * profileFile("fund_unit_0.2", "mc", ExternalUtil.JPYLYZER) nebo profileFile("emonograph_3.0", "pdf", ExternalUtil.VERAPDF).
     */
    public static File profileFile(String fdmf, String copy, ExternalUtil util) {
        File fdmfDir = new File(FDMF_DIR, fdmf);
        return new File(new File(new File(fdmfDir, "binaryFileProfiles"), copy), util.getProfileFileName());
    }

    /**
     * Typ zdroje podle adresare profilu (stejne jako FdmfConfiguration.buildSourceTypeDir).
     */
    public static ResourceType resourceType(String copy) {
        switch (copy) {
            case "mc":
                return ResourceType.IMAGE_MASTER_COPY;
            case "uc":
                return ResourceType.IMAGE_USER_COPY;
            case "sa":
                return ResourceType.AUDIO_SOURCE;
            case "mca":
                return ResourceType.AUDIO_MASTER_COPY;
            case "uca":
                return ResourceType.AUDIO_USER_COPY;
            case "pdf":
                return ResourceType.ORIGINAL_COPY_PDF;
            case "epub":
                return ResourceType.ORIGINAL_COPY_EPUB;
            default:
                throw new IllegalArgumentException("neznámý adresář profilů " + copy);
        }
    }

    /**
     * Postavi profil z konfigurace fDMF stejnym parserem jako aplikace (BinaryFileValidator.registerProfile).
     */
    public static BinaryFileProfile loadProfile(ExternalUtilManager manager, String fdmf, String copy, ExternalUtil util) throws Exception {
        BinaryFileValidator validator = new BinaryFileValidator(manager);
        ResourceType type = resourceType(copy);
        validator.registerProfile(type, util, profileFile(fdmf, copy, util));
        return validator.getProfile(type, util);
    }

    /**
     * ExternalUtilManager, ktery nespousti zadny proces a misto vystupu nastroje vrati predem nastaveny text
     * (nebo vyhodi CliCommandException, pokud je nastavena chyba). Vystup se nastavuje pro nastroj, ne pro soubor.
     */
    public static class CannedOutputExternalUtilManager extends ExternalUtilManager {
        private final Map<ExternalUtil, String> outputs = new EnumMap<>(ExternalUtil.class);
        private final Map<ExternalUtil, String> failures = new EnumMap<>(ExternalUtil.class);
        private ExternalUtilExecution lastExecution;
        private File lastTargetFile;

        public CannedOutputExternalUtilManager() {
            super(new HashMap<>(), new HashMap<>());
        }

        /** S ověřenými řadami verzí (jako manažer z konfigurace pro jednu platformu). */
        public CannedOutputExternalUtilManager(Map<ExternalUtil, VerifiedVersions> verifiedVersions) {
            super(new HashMap<>(), new HashMap<>(), verifiedVersions);
        }

        public CannedOutputExternalUtilManager withOutput(ExternalUtil util, String output) {
            outputs.put(util, output);
            failures.remove(util);
            return this;
        }

        public CannedOutputExternalUtilManager withFailure(ExternalUtil util, String message) {
            failures.put(util, message);
            outputs.remove(util);
            return this;
        }

        @Override
        public boolean isUtilExecutionDefined(ExternalUtilExecution execution) {
            return outputs.containsKey(execution.getUtil()) || failures.containsKey(execution.getUtil());
        }

        @Override
        public String runUtilExecution(ExternalUtilExecution utilExecution, File targetFile) throws CliCommand.CliCommandException {
            lastExecution = utilExecution;
            lastTargetFile = targetFile;
            ExternalUtil util = utilExecution.getUtil();
            if (failures.containsKey(util)) {
                throw new CliCommand.CliCommandException(failures.get(util));
            }
            if (!outputs.containsKey(util)) {
                throw new IllegalStateException("pro nástroj " + util + " není nastaven výstup");
            }
            return outputs.get(util);
        }

        public ExternalUtilExecution getLastExecution() {
            return lastExecution;
        }

        public File getLastTargetFile() {
            return lastTargetFile;
        }
    }
}
