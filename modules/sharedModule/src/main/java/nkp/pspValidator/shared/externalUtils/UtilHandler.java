package nkp.pspValidator.shared.externalUtils;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Martin Řehánek on 14.11.16.
 */
public class UtilHandler {
    /** nejvýš tolik znaků výstupu nástroje se uvede v hlášce o selhání */
    static final int OUTPUT_EXCERPT_MAX_LENGTH = 300;

    private final CommandData commandData;
    private final Parser parser;
    /** posouzení návratového kódu (jen spuštění nad souborem), null = kód se neposuzuje (jako před verzí 3.0) */
    private final ExitCodePolicy exitCodePolicy;
    /** znak zpracování souboru ve výstupu (jen spuštění nad souborem), null = nekontroluje se */
    private final ProcessedEvidence processedEvidence;

    public UtilHandler(CommandData commandData, Parser parser) {
        this(commandData, parser, null, null);
    }

    public UtilHandler(CommandData commandData, Parser parser, ExitCodePolicy exitCodePolicy, ProcessedEvidence processedEvidence) {
        this.commandData = commandData;
        this.parser = parser;
        this.exitCodePolicy = exitCodePolicy;
        this.processedEvidence = processedEvidence;
    }

    public CommandData getCommandData() {
        return commandData;
    }

    public Parser getParser() {
        return parser;
    }

    public ExitCodePolicy getExitCodePolicy() {
        return exitCodePolicy;
    }

    public ProcessedEvidence getProcessedEvidence() {
        return processedEvidence;
    }

    /**
     * Výstup spuštění nástroje vybraný parserem (proud a regexp), bez okrajových mezer; prázdný řetězec, když regexp
     * nic nenajde.
     */
    public String parseOutput(CliCommand.Result result) {
        String rawOutput = parser.getStream() == Stream.STDERR ? result.getStderr() : result.getStdout();
        if (rawOutput == null) {
            return null;
        }
        String parsed = parser.parse(rawOutput);
        return parsed == null || parsed.isEmpty() ? "" : parsed.trim();
    }

    /**
     * Posoudí spuštění nad souborem: návratový kód podle {@link ExitCodePolicy}, pak znak zpracování souboru
     * ({@link ProcessedEvidence}). Bez nastavení (starší konfigurace) vrátí jen výstup jako dřív.
     */
    public ExecutionOutcome evaluateExecution(CliCommand.Result result) {
        if (exitCodePolicy != null) {
            String failure = exitCodePolicy.describeFailure(result.getExitValue());
            if (failure != null) {
                return ExecutionOutcome.failed(failure + ", " + outputExcerpt(result));
            }
        }
        String output = parseOutput(result);
        if (processedEvidence != null && !processedEvidence.isFoundIn(result)) {
            return ExecutionOutcome.withoutEvidence(output,
                    "ve výstupu chybí údaj o zpracování souboru (kód " + result.getExitValue() + "), " + outputExcerpt(result));
        }
        return ExecutionOutcome.completed(output);
    }

    /**
     * Začátek výstupu nástroje pro hlášku: neprázdné řádky stderr (a za nimi stdout) spojené mezerou, nejvýš
     * {@link #OUTPUT_EXCERPT_MAX_LENGTH} znaků („výstup: …“), nebo „bez výstupu“.
     */
    static String outputExcerpt(CliCommand.Result result) {
        StringBuilder text = new StringBuilder();
        for (String stream : new String[]{result.getStderr(), result.getStdout()}) {
            if (stream == null) {
                continue;
            }
            for (String line : stream.split("\\r?\\n")) {
                String trimmed = line.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }
                if (text.length() > 0) {
                    text.append(' ');
                }
                text.append(trimmed);
                if (text.length() > OUTPUT_EXCERPT_MAX_LENGTH) {
                    return "výstup: " + text.substring(0, OUTPUT_EXCERPT_MAX_LENGTH) + "…";
                }
            }
        }
        return text.length() == 0 ? "bez výstupu" : "výstup: " + text;
    }

    public static class CommandData {
        private File path;
        private final String rawCommand;

        public CommandData(String rawCommand) {
            this.rawCommand = rawCommand;
        }

        public File getPath() {
            return path;
        }

        public String getRawCommand() {
            return rawCommand;
        }

        public void setPath(File path) {
            this.path = path;
        }

        @Override
        public String toString() {
            return "Command{" +
                    "path=" + path +
                    ", rawCommand='" + rawCommand + '\'' +
                    '}';
        }
    }

    public static class Parser {
        private final Stream stream;
        private final String regexp;

        public Parser(Stream stream, String regexp) {
            this.stream = stream;
            this.regexp = regexp;
        }

        public Stream getStream() {
            return stream;
        }

        public String getRegexp() {
            return regexp;
        }

        /** Celý výstup (bez regexp), první výskyt regexp, nebo null, když se nenajde. */
        public String parse(String rawOutput) {
            if (regexp == null) {
                return rawOutput;
            }
            Matcher m = Pattern.compile(regexp).matcher(rawOutput);
            return m.find() ? m.group(0) : null;
        }

        @Override
        public String toString() {
            return "Parser{" +
                    "stream=" + stream +
                    ", regexp='" + regexp + '\'' +
                    '}';
        }
    }

    /**
     * Znak, že nástroj soubor opravdu zpracoval (element processedFileEvidence): regexp, který se musí najít
     * v daném proudu výstupu (např. řádek „Consumed … tile-part(s)“ na stdout u Kakadu).
     */
    public static class ProcessedEvidence {
        private final Stream stream;
        private final Pattern pattern;

        public ProcessedEvidence(Stream stream, String regexp) {
            this.stream = stream;
            this.pattern = Pattern.compile(regexp);
        }

        public Stream getStream() {
            return stream;
        }

        public String getRegexp() {
            return pattern.pattern();
        }

        public boolean isFoundIn(CliCommand.Result result) {
            String text = stream == Stream.STDERR ? result.getStderr() : result.getStdout();
            return text != null && pattern.matcher(text).find();
        }

        @Override
        public String toString() {
            return "ProcessedEvidence{" +
                    "stream=" + stream +
                    ", regexp='" + pattern.pattern() + '\'' +
                    '}';
        }
    }
}
