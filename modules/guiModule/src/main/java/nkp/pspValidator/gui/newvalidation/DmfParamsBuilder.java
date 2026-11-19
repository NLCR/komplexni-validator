package nkp.pspValidator.gui.newvalidation;

import nkp.pspValidator.shared.Dmf;
import nkp.pspValidator.shared.DmfDetector;

/**
 * Sestaví {@link DmfDetector.Params} (vynucené a preferované verze pro všech 11 typů) z uloženého nastavení, stejně
 * jako je dřív plnily dialogy validace adresáře a ZIPu.
 */
public final class DmfParamsBuilder {

    private DmfParamsBuilder() {
    }

    public static DmfDetector.Params build(VersionSettings settings) {
        DmfDetector.Params params = new DmfDetector.Params();
        for (Dmf.Type type : Dmf.Type.values()) {
            set(params, type, settings.forcedVersion(type), settings.preferredVersion(type));
        }
        return params;
    }

    private static void set(DmfDetector.Params params, Dmf.Type type, String forced, String preferred) {
        switch (type) {
            case MONOGRAPH:
                params.forcedDmfMonVersion = forced;
                params.preferredDmfMonVersion = preferred;
                break;
            case PERIODICAL:
                params.forcedDmfPerVersion = forced;
                params.preferredDmfPerVersion = preferred;
                break;
            case AUDIO_GRAM:
                params.forcedDmfAdgVersion = forced;
                params.preferredDmfAdgVersion = preferred;
                break;
            case AUDIO_FONO:
                params.forcedDmfAdfVersion = forced;
                params.preferredDmfAdfVersion = preferred;
                break;
            case AUDIO_DISC:
                params.forcedDmfAdiVersion = forced;
                params.preferredDmfAdiVersion = preferred;
                break;
            case AUDIO_NO_CARRIER:
                params.forcedDmfAdnVersion = forced;
                params.preferredDmfAdnVersion = preferred;
                break;
            case DATA_DISC:
                params.forcedDmfDadVersion = forced;
                params.preferredDmfDadVersion = preferred;
                break;
            case FUND_UNIT:
                params.forcedDmfFduVersion = forced;
                params.preferredDmfFduVersion = preferred;
                break;
            case EMONOGRAPH:
                params.forcedDmfEmoVersion = forced;
                params.preferredDmfEmoVersion = preferred;
                break;
            case EPERIODICAL:
                params.forcedDmfEpeVersion = forced;
                params.preferredDmfEpeVersion = preferred;
                break;
            case COMPERIODICAL:
                params.forcedDmfSepVersion = forced;
                params.preferredDmfSepVersion = preferred;
                break;
            default:
                throw new IllegalStateException("neznámý typ " + type);
        }
    }
}
