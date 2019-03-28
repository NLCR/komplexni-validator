package nkp.pspValidator.shared.externalUtils;

import java.io.File;

/**
 * Created by Martin Řehánek on 14.11.16.
 */
public class UtilHandler {
    private final CommandData commandData;
    private final Parser parser;

    public UtilHandler(CommandData commandData, Parser parser) {
        this.commandData = commandData;
        this.parser = parser;
    }

    public CommandData getCommandData() {
        return commandData;
    }

    public Parser getParser() {
        return parser;
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

        @Override
        public String toString() {
            return "Parser{" +
                    "stream=" + stream +
                    ", regexp='" + regexp + '\'' +
                    '}';
        }
    }


}
