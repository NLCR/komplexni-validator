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
 * <p>
 * Neměnná hodnota: parametry pro {@link DmfDetector} se při vytvoření, při čtení i při každém spuštění kopírují.
 * Pro „Validovat znovu“ vznikne z požadavku nový přes {@link #forRerun(VersionSettings)}: volby dialogu (zdroj, vynucené
 * verze, výstupy, podrobnost logu, vypnuté nástroje) zůstanou, preferované verze se vezmou z aktuálního Nastavení.
 */
public class ValidationRequest {

    /** Kdo validaci spustí (v aplikaci Main a ExternalUtilManager, v testech náhrada). */
    public interface Launcher {

        /** Nastaví nástroje vypnuté pro spouštěnou validaci (předchozí sadu nahradí). */
        void setUtilsDisabled(Set<ExternalUtil> utils);

        void runPspDirValidation(File pspDir, DmfDetector.Params params, boolean createTxtLog, boolean createXmlLog, int verbosity);

        void unzipAndRunPspZipValidation(File pspZip, DmfDetector.Params params, boolean createTxtLog, boolean createXmlLog, int verbosity);

        /** Validace skupiny balíků v adresáři (balíky se hledají až při spuštění, „Validovat znovu“ čte adresář znovu). */
        void runGroupValidation(File groupDir, DmfDetector.Params params, boolean createTxtLog, boolean createXmlLog, int verbosity);
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
        this.params = DmfParamsBuilder.copy(params);
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

    /**
     * Požadavek pro „Validovat znovu“: stejný zdroj, vynucené verze, výstupy, podrobnost logu a vypnuté nástroje,
     * preferované (výchozí) verze z aktuálního nastavení. Přeskakované sekce a adresář logů se čtou z nastavení až při
     * běhu, takže se také projeví aktuální Nastavení.
     */
    public ValidationRequest forRerun(VersionSettings currentSettings) {
        return new ValidationRequest(source, file, DmfParamsBuilder.withCurrentPreferred(params, currentSettings),
                createTxtLog, createXmlLog, verbosity, disabledUtils);
    }

    /** Spustí validaci stejnými metodami jako dřívější dialogy adresáře a ZIPu, skupinu balíků jako skupinu. */
    public void launch(Launcher launcher) {
        launcher.setUtilsDisabled(disabledUtils);
        switch (source) {
            case DIRECTORY:
                launcher.runPspDirValidation(file, DmfParamsBuilder.copy(params), createTxtLog, createXmlLog, verbosity);
                break;
            case ZIP:
                launcher.unzipAndRunPspZipValidation(file, DmfParamsBuilder.copy(params), createTxtLog, createXmlLog, verbosity);
                break;
            case GROUP:
                launcher.runGroupValidation(file, DmfParamsBuilder.copy(params), createTxtLog, createXmlLog, verbosity);
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

    /** Kopie parametrů (změna vrácené hodnoty požadavek nezmění). */
    public DmfDetector.Params getParams() {
        return DmfParamsBuilder.copy(params);
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
