package nkp.pspValidator.shared.imageUtils;

import java.io.File;

/**
 * Created by martin on 14.11.16.
 */
public class UtilHandler {
    private final Command command;
    private final Parser parser;

    public UtilHandler(Command command, Parser parser) {
        this.command = command;
        this.parser = parser;
    }

    public Command getCommand() {
        return command;
    }

    public Parser getParser() {
        return parser;
    }

    public static class Command {
        private File path;
        private final String rawCommand;

        public Command(String rawCommand) {
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
    }


}
