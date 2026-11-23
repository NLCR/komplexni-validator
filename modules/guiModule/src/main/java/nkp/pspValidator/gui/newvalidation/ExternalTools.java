package nkp.pspValidator.gui.newvalidation;

import nkp.pspValidator.gui.ConfigurationManager;
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
 * Externí nástroje v dialozích Nová validace a Externí nástroje: popis, klíč nastavení s adresářem nástroje, návod
 * k instalaci, zjištění stavu detekcí verze, nástroje relevantní pro fDMF a sada nástrojů vypnutých pro jednu validaci.
 */
public final class ExternalTools {

    /**
     * Návod k instalaci nástrojů na wiki projektu: oddíl „Instalace nástrojů pro validaci obrazových/zvukových
     * souborů“ (GitHub v kotvě lomítko vypouští).
     */
    public static final String INSTALLATION_HELP_URL = "https://github.com/NLCR/komplexni-validator/wiki/Instalace#instalace-n%C3%A1stroj%C5%AF-pro-validaci-obrazov%C3%BDchzvukov%C3%BDch-soubor%C5%AF";

    private static final String INSTALLATION_URL_PREFIX = "https://github.com/NLCR/komplexni-validator/wiki/Instalace#";

    /**
     * Podporované (ověřené) verze nástrojů: oddíl „Podporované verze nástrojů“ v README na větvi master, jediné místo,
     * kde se vedou (aplikace vlastní popis verzí nemá, bublina u nástroje jen čte konfiguraci). Kotva je id nadpisu
     * podle pravidel GitHubu, zakódované pro URL.
     */
    public static final String SUPPORTED_VERSIONS_URL = "https://github.com/NLCR/komplexni-validator/blob/master/README.md#podporovan%C3%A9-verze-n%C3%A1stroj%C5%AF";

    /** Text odkazu na {@link #SUPPORTED_VERSIONS_URL}. */
    public static final String SUPPORTED_VERSIONS_LINK_TEXT = "Podporované verze nástrojů";

    private ExternalTools() {
    }

    /** Klíč v {@link ConfigurationManager}, pod kterým je uložený adresář nástroje. */
    public static String configKey(ExternalUtil util) {
        switch (util) {
            case JHOVE:
                return ConfigurationManager.PROP_JHOVE_DIR;
            case JPYLYZER:
                return ConfigurationManager.PROP_JPYLYZER_DIR;
            case IMAGE_MAGICK:
                return ConfigurationManager.PROP_IMAGE_MAGICK_DIR;
            case KAKADU:
                return ConfigurationManager.PROP_KAKADU_DIR;
            case MP3VAL:
                return ConfigurationManager.PROP_MP3VAL_DIR;
            case SHNTOOL:
                return ConfigurationManager.PROP_SHNTOOL_DIR;
            case CHECKMATE:
                return ConfigurationManager.PROP_CHECKMATE_DIR;
            case VERAPDF:
                return ConfigurationManager.PROP_VERAPDF_DIR;
            case EPUBCHECK:
                return ConfigurationManager.PROP_EPUBCHECK_DIR;
            default:
                throw new IllegalArgumentException("unknown util " + util);
        }
    }

    /** Návod k instalaci nástroje (oddíl stránky Instalace na wiki projektu). */
    public static String installationUrl(ExternalUtil util) {
        switch (util) {
            case JHOVE:
                return INSTALLATION_URL_PREFIX + "jhove";
            case JPYLYZER:
                return INSTALLATION_URL_PREFIX + "jpylyzer";
            case IMAGE_MAGICK:
                return INSTALLATION_URL_PREFIX + "imagemagick";
            case KAKADU:
                return INSTALLATION_URL_PREFIX + "kakadu";
            case MP3VAL:
                return INSTALLATION_URL_PREFIX + "mp3val";
            case SHNTOOL:
                return INSTALLATION_URL_PREFIX + "shntool";
            case CHECKMATE:
                return INSTALLATION_URL_PREFIX + "checkmate";
            case VERAPDF:
                return INSTALLATION_URL_PREFIX + "verapdf";
            case EPUBCHECK:
                return INSTALLATION_URL_PREFIX + "epubcheck";
            default:
                throw new IllegalArgumentException("unknown util " + util);
        }
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
            String version = manager.runUtilVersionDetection(util);
            return ToolStatus.found(version, manager.checkVersion(util, version));
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
