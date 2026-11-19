package nkp.pspValidator.gui.newvalidation;

import nkp.pspValidator.shared.DmfDetector;
import nkp.pspValidator.shared.externalUtils.ExternalUtil;

import java.io.File;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * Požadavek na validaci z dialogu Nová validace: zdroj a cesta, parametry pro {@link DmfDetector} (vynucené
 * a preferované verze všech typů), výstupy a nástroje vypnuté jen pro tuto validaci.
 */
public class ValidationRequest {

    /** Kdo validaci spustí (v aplikaci Main a ExternalUtilManager, v testech náhrada). */
    public interface Launcher {

        /** Nastaví nástroje vypnuté pro spouštěnou validaci (předchozí sadu nahradí). */
        void setUtilsDisabled(Set<ExternalUtil> utils);

        void runPspDirValidation(File pspDir, DmfDetector.Params params, boolean createTxtLog, boolean createXmlLog, int verbosity);

        void unzipAndRunPspZipValidation(File pspZip, DmfDetector.Params params, boolean createTxtLog, boolean createXmlLog, int verbosity);
    }

    private final ValidationSource source;
    private final File file;
    private final DmfDetector.Params params;
    private final boolean createTxtLog;
    private final boolean createXmlLog;
    private final int verbosity;
    private final Set<ExternalUtil> disabledUtils;

    public ValidationRequest(ValidationSource source, File file, DmfDetector.Params params,
                             boolean createTxtLog, boolean createXmlLog, int verbosity, Set<ExternalUtil> disabledUtils) {
        this.source = source;
        this.file = file;
        this.params = params;
        this.createTxtLog = createTxtLog;
        this.createXmlLog = createXmlLog;
        this.verbosity = verbosity;
        this.disabledUtils = disabledUtils == null || disabledUtils.isEmpty()
                ? Collections.<ExternalUtil>emptySet()
                : Collections.unmodifiableSet(EnumSet.copyOf(disabledUtils));
    }

    /**
     * Sestaví požadavek z uloženého nastavení.
     *
     * @throws IllegalArgumentException když cesta neprošla kontrolou zdroje (tlačítko Validovat je pak zakázané)
     */
    public static ValidationRequest create(ValidationSource source, String pathText, VersionSettings versions,
                                           OutputSettings outputs, Set<ExternalUtil> disabledUtils) {
        String problem = source.checkPath(pathText);
        if (problem != null) {
            throw new IllegalArgumentException(problem);
        }
        return new ValidationRequest(source, new File(pathText.trim()), DmfParamsBuilder.build(versions),
                outputs.isCreateTxtLog(), outputs.isCreateXmlLog(), outputs.getVerbosity().getLevel(), disabledUtils);
    }

    /** Spustí validaci stejnými metodami jako dřívější dialogy adresáře a ZIPu. */
    public void launch(Launcher launcher) {
        launcher.setUtilsDisabled(disabledUtils);
        switch (source) {
            case DIRECTORY:
                launcher.runPspDirValidation(file, params, createTxtLog, createXmlLog, verbosity);
                break;
            case ZIP:
                launcher.unzipAndRunPspZipValidation(file, params, createTxtLog, createXmlLog, verbosity);
                break;
            default:
                throw new IllegalStateException();
        }
    }

    public ValidationSource getSource() {
        return source;
    }

    public File getFile() {
        return file;
    }

    public DmfDetector.Params getParams() {
        return params;
    }

    public boolean isCreateTxtLog() {
        return createTxtLog;
    }

    public boolean isCreateXmlLog() {
        return createXmlLog;
    }

    public int getVerbosity() {
        return verbosity;
    }

    public Set<ExternalUtil> getDisabledUtils() {
        return disabledUtils;
    }
}
