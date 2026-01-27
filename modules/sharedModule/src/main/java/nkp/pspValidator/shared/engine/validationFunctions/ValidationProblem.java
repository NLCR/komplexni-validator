package nkp.pspValidator.shared.engine.validationFunctions;

import nkp.pspValidator.shared.engine.Level;

import java.io.File;

/**
 * Created by Martin Řehánek on 11.11.16.
 */
public class ValidationProblem {

    private final Level level;

    private final String fullMessage;

    private final String simpleMessage;

    //custom attributres
    private final File file;

    public ValidationProblem(Level level, String fullMessage) {
        this.level = level;
        this.fullMessage = fullMessage;
        this.simpleMessage = null;
        this.file = null;
    }

    private ValidationProblem(Level level, String fullMessage, String simpleMessage, File file) {
        this.level = level;
        this.fullMessage = fullMessage;
        this.simpleMessage = simpleMessage;
        this.file = file;
    }

    public ValidationProblem withSimpleMessage(String simpleMessage) {
        return new ValidationProblem(this.level, this.fullMessage, simpleMessage, this.file);
    }

    public ValidationProblem withFile(File file) {
        return new ValidationProblem(this.level, this.fullMessage, this.simpleMessage, file);
    }

    public Level getLevel() {
        return level;
    }

    public String getFullMessage() {
        if (fullMessage == null) { //fallbacke to simple message or empty string
            return simpleMessage != null ? simpleMessage : "";
        }
        return fullMessage;
    }

    public String getSimpleMessage() {
        return simpleMessage;
    }

    public File getFile() {
        return file;
    }

}
