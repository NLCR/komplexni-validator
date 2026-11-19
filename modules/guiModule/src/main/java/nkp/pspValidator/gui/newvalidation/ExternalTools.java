package nkp.pspValidator.gui.newvalidation;

import nkp.pspValidator.shared.Dmf;
import nkp.pspValidator.shared.FdmfRegistry;
import nkp.pspValidator.shared.engine.exceptions.ValidatorConfigurationException;
import nkp.pspValidator.shared.externalUtils.CliCommand;
import nkp.pspValidator.shared.externalUtils.ExternalUtil;
import nkp.pspValidator.shared.externalUtils.ExternalUtilManager;

import java.util.Collection;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Externí nástroje v dialogu Nová validace: popis, zjištění stavu (detekce verze stejně jako v dialogu externích
 * nástrojů), nástroje relevantní pro fDMF a sada nástrojů vypnutých pro jednu validaci.
 */
public final class ExternalTools {

    private ExternalTools() {
    }

    /** Krátký popis, k čemu nástroj validace používá. */
    public static String description(ExternalUtil util) {
        switch (util) {
            case JHOVE:
                return "obrazy JPEG 2000, zvuk";
            case JPYLYZER:
            case IMAGE_MAGICK:
            case KAKADU:
                return "obrazy JPEG 2000";
            case MP3VAL:
            case CHECKMATE:
                return "zvuk MP3";
            case SHNTOOL:
                return "zvuk WAV";
            case VERAPDF:
                return "PDF/A";
            case EPUBCHECK:
                return "EPUB";
            default:
                return "";
        }
    }

    /**
     * Zjistí stav nástroje detekcí verze. Dostupnost v manažeru nemění: tu podle výsledku nastaví volající, a to jen
     * pro výsledek, který ještě platí (jinak by zastaralá kontrola přepsala výsledek novější). Spouští proces, volat
     * z pracovního vlákna.
     */
    public static ToolStatus check(ExternalUtilManager manager, ExternalUtil util) {
        if (!manager.isVersionDetectionDefined(util)) {
            return ToolStatus.error("detekce verze není definována");
        }
        try {
            return ToolStatus.found(manager.runUtilVersionDetection(util));
        } catch (CliCommand.CliCommandException e) {
            return classifyFailure(e.getMessage());
        } catch (RuntimeException e) {
            return ToolStatus.error(e.getMessage());
        }
    }

    /** Proces nejde spustit (program neexistuje) je „nenalezen“, ostatní selhání „chyba při spuštění“. */
    static ToolStatus classifyFailure(String message) {
        if (message != null && message.contains("Cannot run program")) {
            return ToolStatus.notFound(message);
        }
        return ToolStatus.error(message);
    }

    /**
     * Nástroje, které pravidla dané fDMF používají; null, když fDMF neexistuje nebo ji nelze přečíst (pak se
     * relevance nezobrazuje).
     */
    public static Set<ExternalUtil> relevantFor(FdmfRegistry registry, Dmf dmf) {
        if (dmf == null) {
            return null;
        }
        try {
            return registry.getExternalUtilsUsed(dmf);
        } catch (FdmfRegistry.UnknownFdmfException | ValidatorConfigurationException e) {
            return null;
        }
    }

    /**
     * Nástroje, které se mají pro spouštěnou validaci vypnout: dostupné nástroje, u kterých uživatel zrušil
     * zaškrtnutí „použít“. Nedostupné nástroje se nevypínají (validace je hlásí jako nedostupné).
     */
    public static Set<ExternalUtil> toDisable(Collection<ExternalUtil> unchecked, Map<ExternalUtil, ToolStatus> statuses) {
        Set<ExternalUtil> result = EnumSet.noneOf(ExternalUtil.class);
        for (ExternalUtil util : unchecked) {
            ToolStatus status = statuses.get(util);
            if (status == null || status.getState() != ToolStatus.State.NOT_FOUND && status.getState() != ToolStatus.State.ERROR) {
                result.add(util);
            }
        }
        return result;
    }
}
