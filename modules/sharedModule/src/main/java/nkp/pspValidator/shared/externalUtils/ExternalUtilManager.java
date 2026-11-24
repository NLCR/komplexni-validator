package nkp.pspValidator.shared.externalUtils;

import nkp.pspValidator.shared.OperatingSystem;
import nkp.pspValidator.shared.Platform;
import nkp.pspValidator.shared.externalUtils.UtilHandler.CommandData;
import nkp.pspValidator.shared.externalUtils.UtilHandler.Parser;

import java.io.File;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Martin Řehánek on 14.11.16.
 */
public class ExternalUtilManager {

    private final Map<ExternalUtil, UtilHandler> utilVersionDetectionHandlers;
    private final Map<ExternalUtilExecution, UtilHandler> utilExecutionHandlers;
    private final Map<String, Boolean> utilAvailable = new HashMap<>();
    /** ověřené řady verzí pro platformu manažeru; nástroj bez záznamu seznam nemá (VersionCheck.Status.NO_LIST) */
    private final Map<ExternalUtil, VerifiedVersions> verifiedVersions;
    /** text detekce verze zapsaný CLI nebo GUI po úspěšné detekci (čte ho validace v jiném vlákně) */
    private final Map<ExternalUtil, String> detectedVersions = new ConcurrentHashMap<>();

    private final Set<ExternalUtil> utilsDisabled = new HashSet<>();

    /** Manažer bez seznamů ověřených verzí (žádná verze se neoznačuje). */
    public ExternalUtilManager(Map<ExternalUtil, UtilHandler> utilVersionDetectionHandlers, Map<ExternalUtilExecution, UtilHandler> utilExecutionHandlers) {
        this(utilVersionDetectionHandlers, utilExecutionHandlers, Collections.<ExternalUtil, VerifiedVersions>emptyMap());
    }

    /**
     * @param verifiedVersions ověřené řady verzí nástrojů pro platformu manažeru (nástroj bez záznamu seznam nemá)
     */
    public ExternalUtilManager(Map<ExternalUtil, UtilHandler> utilVersionDetectionHandlers, Map<ExternalUtilExecution, UtilHandler> utilExecutionHandlers,
                               Map<ExternalUtil, VerifiedVersions> verifiedVersions) {
        this.utilVersionDetectionHandlers = utilVersionDetectionHandlers;
        this.utilExecutionHandlers = utilExecutionHandlers;
        this.verifiedVersions = verifiedVersions.isEmpty()
                ? Collections.<ExternalUtil, VerifiedVersions>emptyMap()
                : Collections.unmodifiableMap(new EnumMap<>(verifiedVersions));
        for (ExternalUtil util : ExternalUtil.values()) {
            utilAvailable.put(util.name(), true);
        }
    }

    /** Ověřené řady verzí nástroje pro platformu manažeru, nebo null, když je konfigurace neuvádí. */
    public VerifiedVersions getVerifiedVersions(ExternalUtil util) {
        return verifiedVersions.get(util);
    }

    /**
     * Porovná text detekce verze nástroje s ověřenými řadami (nic nespouští ani nemění, lze volat z kteréhokoli
     * vlákna).
     */
    public VersionCheck checkVersion(ExternalUtil util, String detectionText) {
        return VersionCheck.evaluate(verifiedVersions.get(util), detectionText);
    }

    /**
     * Zapíše text detekce verze dostupného nástroje (CLI a GUI po úspěšné detekci, null zápis zruší). Podle něj validace
     * u pravidel nástroje ohlásí neověřenou verzi ({@link #getVersionCheck}). {@link #runUtilVersionDetection} verzi
     * sama nezapisuje: v GUI běží v pracovním vlákně a o platnosti výsledku rozhoduje až volající.
     */
    public void setUtilVersion(ExternalUtil util, String detectionText) {
        if (detectionText == null) {
            detectedVersions.remove(util);
        } else {
            detectedVersions.put(util, detectionText);
        }
    }

    /** Zapsaný text detekce verze nástroje, nebo null. */
    public String getUtilVersion(ExternalUtil util) {
        return detectedVersions.get(util);
    }

    /**
     * Porovnání zapsané verze nástroje ({@link #setUtilVersion}) s ověřenými řadami, nebo null, když verze zapsaná
     * není.
     */
    public VersionCheck getVersionCheck(ExternalUtil util) {
        String detectionText = detectedVersions.get(util);
        return detectionText == null ? null : checkVersion(util, detectionText);
    }

    public synchronized void setUtilDisabled(ExternalUtil util) {
        utilsDisabled.add(util);
    }

    /**
     * Nahradi celou sadu vypnutych nastroju (GUI: vypnuti nastroje jen pro spoustenou validaci, kazde spusteni sadu
     * nastavi znovu). Vypnuty nastroj validace hlasi stejne jako CLI s --disable-...: INFO "nástroj ... je vypnutý".
     */
    public synchronized void setUtilsDisabled(Set<ExternalUtil> utils) {
        utilsDisabled.clear();
        if (utils != null) {
            utilsDisabled.addAll(utils);
        }
    }

    public synchronized boolean isUtilDisabled(ExternalUtil util) {
        return utilsDisabled.contains(util);
    }

    public boolean isVersionDetectionDefined(ExternalUtil type) {
        return utilVersionDetectionHandlers.containsKey(type);
    }

    public void setUtilAvailable(ExternalUtil util, boolean available) {
        utilAvailable.put(util.name(), available);
    }

    public boolean isUtilAvailable(ExternalUtil util) {
        return utilAvailable.get(util.name());
    }

    public boolean isUtilExecutionDefined(ExternalUtilExecution execution) {
        return utilExecutionHandlers.containsKey(execution);
    }

    public void setPaths(Map<ExternalUtil, File> paths) {
        for (ExternalUtil util : paths.keySet()) {
            File path = paths.get(util);
            setPath(util, path);
        }
    }

    public void setPath(ExternalUtil util, File path) {
        UtilHandler versionDetection = utilVersionDetectionHandlers.get(util);
        if (versionDetection != null) {
            versionDetection.getCommandData().setPath(path);
        }
        for (ExternalUtilExecution execution : utilExecutionHandlers.keySet()) {
            if (execution.getUtil() == util) {
                utilExecutionHandlers.get(execution).getCommandData().setPath(path);
            }
        }
    }

    /**
     * Cesta ke knihovnám nástroje pro spouštěný proces: nástroj se zadaným adresářem dostane v proměnné prostředí
     * svůj adresář a obvyklá umístění knihoven vedle něj (viz {@link LibraryPathEnvironment}).
     */
    private static Map<String, String> libraryEnvironment(UtilHandler.CommandData commandData) {
        OperatingSystem os = Platform.detectOs().getOperatingSystem();
        String variable = LibraryPathEnvironment.variableName(os);
        return LibraryPathEnvironment.forUtil(os, commandData.getPath(), variable == null ? null : System.getenv(variable));
    }

    public String runUtilVersionDetection(ExternalUtil util) throws CliCommand.CliCommandException {
        UtilHandler versionDetection = utilVersionDetectionHandlers.get(util);
        String[] command = buildCommand(versionDetection.getCommandData(), null);
        CliCommand.Result result = new CliCommand(command, libraryEnvironment(versionDetection.getCommandData())).execute();
        String rawOutput = null;
        Parser parser = versionDetection.getParser();
        Stream stream = parser.getStream();
        switch (stream) {
            case STDERR:
                rawOutput = result.getStderr();
                break;
            case STDOUT:
                rawOutput = result.getStdout();
                break;
            default:
                throw new IllegalStateException("unexpected stream " + stream);
        }
        if (rawOutput != null && !rawOutput.isEmpty()) {
            String parsed = parseData(rawOutput, parser);
            if (parsed == null || parsed.isEmpty()) {
                throw new CliCommand.CliCommandException("verze nenalezena ve výstupu: " + rawOutput);
            } else {
                return parsed.trim();
            }
        } else {
            throw new CliCommand.CliCommandException(emptyOutputMessage(result, stream));
        }
    }

    /**
     * Hlaska pro prazdny sledovany vystup detekce verze: kdyz nastroj (nebo java -jar s chybejicim jarem) vypsal chybu
     * na druhy stream, uvede se jeji prvni radek a navratovy kod, aby uzivatel videl pricinu a ne jen "prazdny vystup".
     */
    private static String emptyOutputMessage(CliCommand.Result result, Stream watched) {
        String other = watched == Stream.STDOUT ? result.getStderr() : result.getStdout();
        StringBuilder message = new StringBuilder("prázdný výstup");
        if (other != null && !other.trim().isEmpty()) {
            String firstLine = other.trim().split("\\r?\\n", 2)[0].trim();
            message.append(" (").append(watched == Stream.STDOUT ? "stderr" : "stdout").append(": ").append(firstLine);
            if (result.getExitValue() != 0) {
                message.append(", návratový kód ").append(result.getExitValue());
            }
            message.append(')');
        } else if (result.getExitValue() != 0) {
            message.append(" (návratový kód ").append(result.getExitValue()).append(')');
        }
        return message.toString();
    }

    /**
     * Spustí nástroj nad souborem a vrátí výstup vybraný parserem; návratový kód ani znak zpracování souboru
     * neposuzuje (viz {@link #runUtilExecutionChecked}).
     */
    public String runUtilExecution(ExternalUtilExecution utilExecution, File targetFile) throws CliCommand.CliCommandException {
        UtilHandler utilHandler = utilExecutionHandlers.get(utilExecution);
        CliCommand.Result result = execute(utilHandler, targetFile);
        String parsed = utilHandler.parseOutput(result);
        if (parsed == null) {
            throw new CliCommand.CliCommandException("chybí výstup");
        }
        return parsed;
    }

    /**
     * Spustí nástroj nad souborem pro profil binárního souboru a posoudí, zda soubor opravdu zkontroloval (návratový
     * kód a znak zpracování souboru podle externalUtils.xml, viz {@link UtilHandler#evaluateExecution}).
     *
     * @throws CliCommand.CliCommandException když nástroj nejde spustit
     */
    public ExecutionOutcome runUtilExecutionChecked(ExternalUtilExecution utilExecution, File targetFile) throws CliCommand.CliCommandException {
        UtilHandler utilHandler = utilExecutionHandlers.get(utilExecution);
        CliCommand.Result result = execute(utilHandler, targetFile);
        return utilHandler.evaluateExecution(result);
    }

    /** Nastavení spuštění nástroje (příkaz, parser, posouzení výsledku), nebo null, když spuštění není definováno. */
    public UtilHandler getExecutionHandler(ExternalUtilExecution utilExecution) {
        return utilExecutionHandlers.get(utilExecution);
    }

    private CliCommand.Result execute(UtilHandler utilHandler, File targetFile) throws CliCommand.CliCommandException {
        String[] command = buildCommand(utilHandler.getCommandData(), targetFile);
        return new CliCommand(command, libraryEnvironment(utilHandler.getCommandData())).execute();
    }

    private String[] buildCommand(CommandData commandData, File targetFile) {
        String utilPath = commandData.getPath() == null ? "" : commandData.getPath().getAbsolutePath();
        if (!utilPath.isEmpty() && !utilPath.endsWith(File.separator)) {
            utilPath = utilPath + File.separatorChar;
        }

        String[] result = commandData.getRawCommand().split("\\s");
        for (int i = 0; i < result.length; i++) {
            result[i] = result[i].replace("${UTIL_PATH}", utilPath);
            if (targetFile != null) {
                result[i] = result[i].replace("${TARGET_FILE}", targetFile.getAbsolutePath());
            }
        }
        return result;
    }

    private String parseData(String rawOutput, Parser parser) {
        return parser.parse(rawOutput);
    }

}
