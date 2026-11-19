package nkp.pspValidator.gui.newvalidation;

import nkp.pspValidator.gui.ConfigurationManager;
import nkp.pspValidator.shared.Dmf;

/**
 * Klíče v config.properties pro vynucenou a preferovanou verzi DMF daného typu. Jsou to tytéž klíče, které používaly
 * dialogy validace adresáře a ZIPu do verze 3.0, aby stará i nová verze aplikace sdílely nastavení.
 */
public final class DmfVersionKeys {

    public final Dmf.Type type;
    public final String forcedEnabled;
    public final String forcedCode;
    public final String preferredEnabled;
    public final String preferredCode;

    private DmfVersionKeys(Dmf.Type type, String forcedEnabled, String forcedCode, String preferredEnabled, String preferredCode) {
        this.type = type;
        this.forcedEnabled = forcedEnabled;
        this.forcedCode = forcedCode;
        this.preferredEnabled = preferredEnabled;
        this.preferredCode = preferredCode;
    }

    public static DmfVersionKeys of(Dmf.Type type) {
        switch (type) {
            case MONOGRAPH:
                return new DmfVersionKeys(type,
                        ConfigurationManager.PROP_FORCE_MON_VERSION_ENABLED, ConfigurationManager.PROP_FORCE_MON_VERSION_CODE,
                        ConfigurationManager.PROP_PREFER_MON_VERSION_ENABLED, ConfigurationManager.PROP_PREFER_MON_VERSION_CODE);
            case PERIODICAL:
                return new DmfVersionKeys(type,
                        ConfigurationManager.PROP_FORCE_PER_VERSION_ENABLED, ConfigurationManager.PROP_FORCE_PER_VERSION_CODE,
                        ConfigurationManager.PROP_PREFER_PER_VERSION_ENABLED, ConfigurationManager.PROP_PREFER_PER_VERSION_CODE);
            case AUDIO_GRAM:
                return new DmfVersionKeys(type,
                        ConfigurationManager.PROP_FORCE_AUDIO_GRAM_VERSION_ENABLED, ConfigurationManager.PROP_FORCE_AUDIO_GRAM_VERSION_CODE,
                        ConfigurationManager.PROP_PREFER_AUDIO_GRAM_VERSION_ENABLED, ConfigurationManager.PROP_PREFER_AUDIO_GRAM_VERSION_CODE);
            case AUDIO_FONO:
                return new DmfVersionKeys(type,
                        ConfigurationManager.PROP_FORCE_AUDIO_FONO_VERSION_ENABLED, ConfigurationManager.PROP_FORCE_AUDIO_FONO_VERSION_CODE,
                        ConfigurationManager.PROP_PREFER_AUDIO_FONO_VERSION_ENABLED, ConfigurationManager.PROP_PREFER_AUDIO_FONO_VERSION_CODE);
            case AUDIO_DISC:
                return new DmfVersionKeys(type,
                        ConfigurationManager.PROP_FORCE_AUDIO_DISC_VERSION_ENABLED, ConfigurationManager.PROP_FORCE_AUDIO_DISC_VERSION_CODE,
                        ConfigurationManager.PROP_PREFER_AUDIO_DISC_VERSION_ENABLED, ConfigurationManager.PROP_PREFER_AUDIO_DISC_VERSION_CODE);
            case AUDIO_NO_CARRIER:
                return new DmfVersionKeys(type,
                        ConfigurationManager.PROP_FORCE_AUDIO_NO_CARRIER_VERSION_ENABLED, ConfigurationManager.PROP_FORCE_AUDIO_NO_CARRIER_VERSION_CODE,
                        ConfigurationManager.PROP_PREFER_AUDIO_NO_CARRIER_VERSION_ENABLED, ConfigurationManager.PROP_PREFER_AUDIO_NO_CARRIER_VERSION_CODE);
            case DATA_DISC:
                return new DmfVersionKeys(type,
                        ConfigurationManager.PROP_FORCE_DIGITAL_DISC_VERSION_ENABLED, ConfigurationManager.PROP_FORCE_DIGITAL_DISC_VERSION_CODE,
                        ConfigurationManager.PROP_PREFER_DIGITAL_DISC_VERSION_ENABLED, ConfigurationManager.PROP_PREFER_DIGITAL_DISC_VERSION_CODE);
            case FUND_UNIT:
                return new DmfVersionKeys(type,
                        ConfigurationManager.PROP_FORCE_FUND_UNIT_VERSION_ENABLED, ConfigurationManager.PROP_FORCE_FUND_UNIT_VERSION_CODE,
                        ConfigurationManager.PROP_PREFER_FUND_UNIT_VERSION_ENABLED, ConfigurationManager.PROP_PREFER_FUND_UNIT_VERSION_CODE);
            case EMONOGRAPH:
                return new DmfVersionKeys(type,
                        ConfigurationManager.PROP_FORCE_EMONOGRAPH_VERSION_ENABLED, ConfigurationManager.PROP_FORCE_EMONOGRAPH_VERSION_CODE,
                        ConfigurationManager.PROP_PREFER_EMONOGRAPH_VERSION_ENABLED, ConfigurationManager.PROP_PREFER_EMONOGRAPH_VERSION_CODE);
            case EPERIODICAL:
                return new DmfVersionKeys(type,
                        ConfigurationManager.PROP_FORCE_EPERIODICAL_VERSION_ENABLED, ConfigurationManager.PROP_FORCE_EPERIODICAL_VERSION_CODE,
                        ConfigurationManager.PROP_PREFER_EPERIODICAL_VERSION_ENABLED, ConfigurationManager.PROP_PREFER_EPERIODICAL_VERSION_CODE);
            case COMPERIODICAL:
                return new DmfVersionKeys(type,
                        ConfigurationManager.PROP_FORCE_COMPERIODICAL_VERSION_ENABLED, ConfigurationManager.PROP_FORCE_COMPERIODICAL_VERSION_CODE,
                        ConfigurationManager.PROP_PREFER_COMPERIODICAL_VERSION_ENABLED, ConfigurationManager.PROP_PREFER_COMPERIODICAL_VERSION_CODE);
            default:
                throw new IllegalStateException("neznámý typ " + type);
        }
    }
}
