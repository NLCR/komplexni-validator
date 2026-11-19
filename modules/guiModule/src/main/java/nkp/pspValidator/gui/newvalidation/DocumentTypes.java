package nkp.pspValidator.gui.newvalidation;

import nkp.pspValidator.gui.VersionComparator;
import nkp.pspValidator.shared.Dmf;
import nkp.pspValidator.shared.DmfDetector;
import nkp.pspValidator.shared.FdmfRegistry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Typy dokumentů pro GUI: české názvy (z {@link Dmf.Type#toString()}, jediné místo s názvy v jádře), skupiny pro tabulku
 * všech typů, dostupné verze fDMF a výchozí verze z {@link DmfDetector}.
 */
public final class DocumentTypes {

    /** Skupiny typů dokumentů v tabulce verzí; pořadí výčtu je pořadí v tabulce. */
    public enum Group {
        PRINTED("Tištěné dokumenty"),
        ELECTRONIC("E-publikace"),
        AUDIO("Zvuk"),
        OTHER("Ostatní");

        private final String label;

        Group(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    private DocumentTypes() {
    }

    /** Český název typu dokumentu, např. „Jednotky fondu“. */
    public static String name(Dmf.Type type) {
        return type.toString();
    }

    public static Group groupOf(Dmf.Type type) {
        switch (type) {
            case MONOGRAPH:
            case PERIODICAL:
            case FUND_UNIT:
                return Group.PRINTED;
            case EMONOGRAPH:
            case EPERIODICAL:
            case COMPERIODICAL:
                return Group.ELECTRONIC;
            case AUDIO_GRAM:
            case AUDIO_FONO:
            case AUDIO_DISC:
            case AUDIO_NO_CARRIER:
                return Group.AUDIO;
            case DATA_DISC:
                return Group.OTHER;
            default:
                throw new IllegalStateException("neznámý typ " + type);
        }
    }

    /** Typy dané skupiny v pořadí výčtu {@link Dmf.Type}. */
    public static List<Dmf.Type> typesOf(Group group) {
        List<Dmf.Type> result = new ArrayList<>();
        for (Dmf.Type type : Dmf.Type.values()) {
            if (groupOf(type) == group) {
                result.add(type);
            }
        }
        return result;
    }

    /** Verze, se kterou validace počítá, když ji balík neuvádí a není nastavena preferovaná verze. */
    public static String defaultVersion(Dmf.Type type) {
        switch (type) {
            case MONOGRAPH:
                return DmfDetector.DEFAULT_MONOGRAPH_VERSION;
            case PERIODICAL:
                return DmfDetector.DEFAULT_PERIODICAL_VERSION;
            case AUDIO_GRAM:
                return DmfDetector.DEFAULT_AUDIO_GRAM_VERSION;
            case AUDIO_FONO:
                return DmfDetector.DEFAULT_AUDIO_FONO_VERSION;
            case AUDIO_DISC:
                return DmfDetector.DEFAULT_AUDIO_DISC_VERSION;
            case AUDIO_NO_CARRIER:
                return DmfDetector.DEFAULT_AUDIO_NO_CARRIER_VERSION;
            case DATA_DISC:
                return DmfDetector.DEFAULT_DATA_DISC_VERSION;
            case FUND_UNIT:
                return DmfDetector.DEFAULT_FUND_UNIT_VERSION;
            case EMONOGRAPH:
                return DmfDetector.DEFAULT_EMONOGRAPH_VERSION;
            case EPERIODICAL:
                return DmfDetector.DEFAULT_EPERIODICAL_VERSION;
            case COMPERIODICAL:
                return DmfDetector.DEFAULT_COMPERIODICAL_VERSION;
            default:
                throw new IllegalStateException("neznámý typ " + type);
        }
    }

    /**
     * Verze fDMF dostupné pro typ, od nejnovější (stejné pořadí jako dřívější dialog pro adresář; první verze je
     * zároveň náhradní volba, když uložená verze není k dispozici).
     */
    public static List<String> availableVersions(FdmfRegistry registry, Dmf.Type type) {
        List<String> result = new ArrayList<>(registryVersions(registry, type));
        Collections.sort(result, new VersionComparator(true));
        return result;
    }

    private static Set<String> registryVersions(FdmfRegistry registry, Dmf.Type type) {
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
            case EMONOGRAPH:
                return registry.getEmonographFdmfVersions();
            case EPERIODICAL:
                return registry.getEperiodicalFdmfVersions();
            case COMPERIODICAL:
                return registry.getComposedPeriodicalFdmfVersions();
            default:
                throw new IllegalStateException("neznámý typ " + type);
        }
    }
}
