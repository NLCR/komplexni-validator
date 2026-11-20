package nkp.pspValidator.gui.newvalidation;

import nkp.pspValidator.shared.Dmf;
import nkp.pspValidator.shared.DmfDetector;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 * Sestaví {@link DmfDetector.Params} (vynucené a preferované verze pro všech 11 typů) z uloženého nastavení, stejně
 * jako je dřív plnily dialogy validace adresáře a ZIPu.
 */
public final class DmfParamsBuilder {

    /** Předpona polí {@link DmfDetector.Params} s preferovanou verzí (ostatní pole jsou vynucené verze). */
    private static final String PREFERRED_PREFIX = "preferred";

    private DmfParamsBuilder() {
    }

    public static DmfDetector.Params build(VersionSettings settings) {
        DmfDetector.Params params = new DmfDetector.Params();
        for (Dmf.Type type : Dmf.Type.values()) {
            set(params, type, settings.forcedVersion(type), settings.preferredVersion(type));
        }
        return params;
    }

    /**
     * Nezávislá kopie parametrů (všechna veřejná pole). {@link ValidationRequest} si drží kopii, aby šel požadavek
     * spustit znovu se stejnými hodnotami, i kdyby někdo předaný objekt mezitím změnil.
     */
    public static DmfDetector.Params copy(DmfDetector.Params original) {
        if (original == null) {
            return null;
        }
        DmfDetector.Params copy = new DmfDetector.Params();
        try {
            for (Field field : DmfDetector.Params.class.getFields()) {
                if (!Modifier.isStatic(field.getModifiers()) && !Modifier.isFinal(field.getModifiers())) {
                    field.set(copy, field.get(original));
                }
            }
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
        return copy;
    }

    /**
     * Parametry pro opakovanou validaci („Validovat znovu“): vynucené verze z původních parametrů (volba dialogu Nová
     * validace pro tuto validaci), preferované (výchozí) verze znovu z aktuálního nastavení, aby se projevila změna
     * v Nastavení mezi během a opakováním.
     */
    public static DmfDetector.Params withCurrentPreferred(DmfDetector.Params original, VersionSettings settings) {
        DmfDetector.Params result = build(settings);
        if (original == null) {
            return result;
        }
        try {
            for (Field field : DmfDetector.Params.class.getFields()) {
                if (!Modifier.isStatic(field.getModifiers()) && !Modifier.isFinal(field.getModifiers())
                        && !field.getName().startsWith(PREFERRED_PREFIX)) {
                    field.set(result, field.get(original));
                }
            }
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
        return result;
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
