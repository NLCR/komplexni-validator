package nkp.pspValidator.gui.newvalidation;

import nkp.pspValidator.gui.ConfigurationManager;
import nkp.pspValidator.shared.Dmf;
import nkp.pspValidator.shared.FdmfRegistry;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Vynucené a preferované verze DMF uložené v {@link ConfigurationManager} (klíče {@link DmfVersionKeys}).
 * <p>
 * Platná verze se určuje stejně jako v dřívějších dialozích validace adresáře a ZIPu: když je vynucení (preferování)
 * zapnuté, použije se uložená verze, pokud pro ni existuje fDMF, jinak nejnovější dostupná verze typu (dřívější
 * dialog ji v takovém případě vybral jako první položku seznamu). Když je vypnuté, verze se nepředává (null).
 */
public class VersionSettings {

    /** Odkud se vezme verze, proti které se bude validovat (pořadí jako v DmfDetector.chooseVersion). */
    public enum Origin {
        FORCED, PACKAGE, PREFERRED, DEFAULT
    }

    /** Verze, proti které se bude validovat, a odkud se vzala. */
    public static class Resolved {
        public final String version;
        public final Origin origin;

        public Resolved(String version, Origin origin) {
            this.version = version;
            this.origin = origin;
        }
    }

    private final ConfigurationManager config;
    private final FdmfRegistry registry;
    private final Map<Dmf.Type, List<String>> versions = new EnumMap<>(Dmf.Type.class);

    public VersionSettings(ConfigurationManager config, FdmfRegistry registry) {
        this.config = config;
        this.registry = registry;
    }

    /** Dostupné verze fDMF pro typ, od nejnovější. */
    public synchronized List<String> availableVersions(Dmf.Type type) {
        List<String> result = versions.get(type);
        if (result == null) {
            result = Collections.unmodifiableList(DocumentTypes.availableVersions(registry, type));
            versions.put(type, result);
        }
        return result;
    }

    public boolean isAvailable(Dmf dmf) {
        return dmf.getVersion() != null && availableVersions(dmf.getType()).contains(dmf.getVersion());
    }

    public boolean isForced(Dmf.Type type) {
        return config.getBooleanOrDefault(DmfVersionKeys.of(type).forcedEnabled, false);
    }

    /** Vynucená verze, která se předá validaci, nebo null. */
    public String forcedVersion(Dmf.Type type) {
        DmfVersionKeys keys = DmfVersionKeys.of(type);
        return effectiveVersion(keys.forcedEnabled, keys.forcedCode, type);
    }

    /** Preferovaná verze (použije se, když balík verzi neuvádí), která se předá validaci, nebo null. */
    public String preferredVersion(Dmf.Type type) {
        DmfVersionKeys keys = DmfVersionKeys.of(type);
        return effectiveVersion(keys.preferredEnabled, keys.preferredCode, type);
    }

    /**
     * Zapne vynucení dané verze, nebo ho při null vypne. Ukládá do stejných klíčů jako dřívější dialogy; při vypnutí
     * zůstane naposledy vybraná verze uložená (stejně jako dřív při odškrtnutí).
     */
    public void setForcedVersion(Dmf.Type type, String version) {
        DmfVersionKeys keys = DmfVersionKeys.of(type);
        if (version == null) {
            config.setBoolean(keys.forcedEnabled, false);
        } else {
            config.setString(keys.forcedCode, version);
            config.setBoolean(keys.forcedEnabled, true);
        }
    }

    /** Počet typů s vynucenou verzí. */
    public int forcedTypesCount() {
        int count = 0;
        for (Dmf.Type type : Dmf.Type.values()) {
            if (forcedVersion(type) != null) {
                count++;
            }
        }
        return count;
    }

    /**
     * Verze, proti které validace balík ověří (stejné pořadí jako DmfDetector.chooseVersion: vynucená, z balíku,
     * preferovaná, výchozí).
     *
     * @param versionInPackage verze z info.xml, null když ji balík neuvádí
     */
    public Resolved resolve(Dmf.Type type, String versionInPackage) {
        String forced = forcedVersion(type);
        if (forced != null) {
            return new Resolved(forced, Origin.FORCED);
        } else if (versionInPackage != null) {
            return new Resolved(versionInPackage, Origin.PACKAGE);
        }
        String preferred = preferredVersion(type);
        if (preferred != null) {
            return new Resolved(preferred, Origin.PREFERRED);
        }
        return new Resolved(DocumentTypes.defaultVersion(type), Origin.DEFAULT);
    }

    /** Verze, která se použije, když balík verzi neuvádí (preferovaná, jinak výchozí). */
    public String versionWhenMissing(Dmf.Type type) {
        String preferred = preferredVersion(type);
        return preferred != null ? preferred : DocumentTypes.defaultVersion(type);
    }

    private String effectiveVersion(String enabledKey, String codeKey, Dmf.Type type) {
        if (!config.getBooleanOrDefault(enabledKey, false)) {
            return null;
        }
        return effectiveCode(config.getStringOrDefault(codeKey, null), availableVersions(type));
    }

    /**
     * Uložená verze, pokud je mezi dostupnými, jinak první dostupná (nejnovější), při žádné dostupné null.
     */
    static String effectiveCode(String stored, List<String> available) {
        if (stored != null && available.contains(stored)) {
            return stored;
        }
        return available.isEmpty() ? null : available.get(0);
    }
}
