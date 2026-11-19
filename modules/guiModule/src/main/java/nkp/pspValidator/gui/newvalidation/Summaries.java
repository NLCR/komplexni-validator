package nkp.pspValidator.gui.newvalidation;

import nkp.pspValidator.shared.Dmf;
import nkp.pspValidator.shared.externalUtils.ExternalUtil;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Texty dialogu Nová validace: karta rozpoznání a souhrny sbalených sekcí. Bez JavaFX.
 */
public final class Summaries {

    private Summaries() {
    }

    // ===== karta „Rozpoznáno v balíku“ =====

    public static String recognitionTitle(Recognition recognition) {
        switch (recognition.getStatus()) {
            case RECOGNIZED:
                return "Rozpoznáno v balíku";
            case NOT_RECOGNIZED:
                return "Balík nelze rozpoznat";
            case INVALID_PATH:
            default:
                return "Balík není vybrán";
        }
    }

    /** Štítek s typem a verzí z balíku, null když typ není rozpoznán. */
    public static String recognitionBadge(Recognition recognition) {
        if (!recognition.isRecognized()) {
            return null;
        }
        String name = DocumentTypes.name(recognition.getType());
        return recognition.getVersionInPackage() == null ? name + ", verze neuvedena" : name + " " + recognition.getVersionInPackage();
    }

    /** Věta pod nadpisem karty: odkud se typ a verze vzaly a proti které verzi se bude validovat. */
    public static String recognitionDetail(Recognition recognition, VersionSettings versions) {
        if (!recognition.isRecognized()) {
            return recognition.getMessage();
        }
        Dmf.Type type = recognition.getType();
        String inPackage = recognition.getVersionInPackage();
        VersionSettings.Resolved resolved = versions.resolve(type, inPackage);
        StringBuilder text = new StringBuilder();
        if (inPackage != null) {
            text.append("Typ podle METS, verze podle info.xml. ");
        } else if (recognition.getVersionProblem() != null) {
            text.append("Typ podle METS. Verzi nelze zjistit: ").append(recognition.getVersionProblem()).append(". ");
        } else {
            text.append("Typ podle METS, balík verzi neuvádí. ");
        }
        switch (resolved.origin) {
            case FORCED:
                text.append("Vynucena verze ").append(resolved.version).append(", validovat se bude proti ní.");
                break;
            case PACKAGE:
                if (versions.isAvailable(new Dmf(type, inPackage))) {
                    text.append("Validovat se bude proti této verzi.");
                } else {
                    text.append("Pro verzi ").append(inPackage).append(" není k dispozici fDMF, validace skončí chybou; vynuťte verzi v sekci Verze DMF.");
                }
                break;
            case PREFERRED:
            case DEFAULT:
                if (recognition.getVersionProblem() != null) {
                    text.append("Bez vynucené verze validace skončí chybou.");
                } else {
                    text.append("Použije se ").append(resolved.origin == VersionSettings.Origin.PREFERRED ? "preferovaná" : "výchozí")
                            .append(" verze ").append(resolved.version).append('.');
                }
                break;
            default:
                throw new IllegalStateException();
        }
        return text.toString();
    }

    /**
     * fDMF, proti které se bude validovat; null, když typ není rozpoznán nebo verzi nelze zjistit a není vynucena
     * (validace by skončila chybou).
     */
    public static Dmf effectiveDmf(Recognition recognition, VersionSettings versions) {
        if (recognition == null || !recognition.isRecognized()) {
            return null;
        }
        VersionSettings.Resolved resolved = versions.resolve(recognition.getType(), recognition.getVersionInPackage());
        if (resolved.origin != VersionSettings.Origin.FORCED && recognition.getVersionProblem() != null) {
            return null;
        }
        return new Dmf(recognition.getType(), resolved.version);
    }

    // ===== souhrny sbalených sekcí =====

    public static String versionsSummary(Recognition recognition, VersionSettings versions) {
        if (recognition != null && recognition.isRecognized()) {
            String forced = versions.forcedVersion(recognition.getType());
            return forced == null ? "podle balíku, nic není vynuceno" : "vynucena verze " + forced;
        }
        int count = versions.forcedTypesCount();
        if (count == 0) {
            return "nic není vynuceno";
        }
        return "vynuceno u " + count + " " + plural(count, "typu", "typů", "typů");
    }

    /**
     * @param typeName  název rozpoznaného typu, nebo null
     * @param relevant  nástroje používané fDMF, nebo null, když nejsou známé
     * @param statuses  stav všech nástrojů (chybějící = ještě se nezjišťoval)
     * @param unchecked nástroje, u kterých uživatel zrušil „použít“
     */
    public static String toolsSummary(String typeName, Set<ExternalUtil> relevant, Map<ExternalUtil, ToolStatus> statuses,
                                      Collection<ExternalUtil> unchecked) {
        for (ExternalUtil util : ExternalUtil.values()) {
            ToolStatus status = statuses.get(util);
            if (status == null || status.getState() == ToolStatus.State.CHECKING) {
                return "zjišťuji dostupnost nástrojů…";
            }
        }
        if (typeName != null && relevant != null) {
            if (relevant.isEmpty()) {
                return "pro " + typeName + " se žádný nepoužije";
            }
            int used = 0;
            for (ExternalUtil util : relevant) {
                if (statuses.get(util).isAvailable() && !unchecked.contains(util)) {
                    used++;
                }
            }
            int all = relevant.size();
            return "pro " + typeName + " se " + plural(used, "použije", "použijí", "použije") + " " + used + " " + fromPreposition(all) + " " + all;
        }
        int available = 0;
        for (ExternalUtil util : ExternalUtil.values()) {
            if (statuses.get(util).isAvailable()) {
                available++;
            }
        }
        int missing = ExternalUtil.values().length - available;
        StringBuilder text = new StringBuilder();
        text.append(available).append(' ').append(plural(available, "dostupný", "dostupné", "dostupných"));
        if (missing > 0) {
            text.append(", ").append(missing).append(' ').append(plural(missing, "nedostupný", "nedostupné", "nedostupných"));
        }
        return text.toString();
    }

    /**
     * @param dmf     fDMF, proti které se bude validovat, nebo null
     * @param skipped přeskakované sekce, nebo null, když fDMF neexistuje
     */
    public static String skippedSummary(Dmf dmf, List<String> skipped) {
        if (dmf == null) {
            return "určí se podle typu a verze balíku";
        } else if (skipped == null) {
            return "pro " + DocumentTypes.name(dmf.getType()) + " " + dmf.getVersion() + " není fDMF";
        } else if (skipped.isEmpty()) {
            return "žádná";
        }
        int n = skipped.size();
        return n + " " + plural(n, "celá sekce", "celé sekce", "celých sekcí");
    }

    // ===== čeština =====

    /** Tvar podle počtu: 1, 2 až 4, ostatní (včetně 0). */
    public static String plural(int count, String one, String few, String many) {
        if (count == 1) {
            return one;
        } else if (count >= 2 && count <= 4) {
            return few;
        }
        return many;
    }

    /** „z“ nebo „ze“ před číslovkou (ze dvou, tří, čtyř, šesti, sedmi; z pěti, osmi, devíti). */
    static String fromPreposition(int count) {
        switch (count) {
            case 2:
            case 3:
            case 4:
            case 6:
            case 7:
                return "ze";
            default:
                return "z";
        }
    }
}
