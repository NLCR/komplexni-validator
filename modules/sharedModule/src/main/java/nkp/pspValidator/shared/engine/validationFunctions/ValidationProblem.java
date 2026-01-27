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
    private final File xsdFile;
    private final String expectedValue;
    private final String actualValue;

    public ValidationProblem(Level level, String fullMessage) {
        this.level = level;
        this.fullMessage = fullMessage;
        this.simpleMessage = null;
        this.file = null;
        this.xsdFile = null;
        this.expectedValue = null;
        this.actualValue = null;
    }

    private ValidationProblem(Level level, String fullMessage, String simpleMessage, File file, File xsdFile, String expectedValue, String actualValue) {
        this.level = level;
        this.fullMessage = fullMessage;
        this.simpleMessage = simpleMessage;
        this.file = file;
        this.xsdFile = xsdFile;
        this.expectedValue = expectedValue;
        this.actualValue = actualValue;
    }

    public ValidationProblem withSimpleMessage(String simpleMessage) {
        return new ValidationProblem(this.level, this.fullMessage, simpleMessage, this.file, this.xsdFile, this.expectedValue, this.actualValue);
    }

    public ValidationProblem withFile(File file) {
        return new ValidationProblem(this.level, this.fullMessage, this.simpleMessage, file, this.xsdFile, this.expectedValue, this.actualValue);
    }

    public ValidationProblem withXsdFile(File xsdFile) {
        return new ValidationProblem(this.level, this.fullMessage, this.simpleMessage, this.file, xsdFile, this.expectedValue, this.actualValue);
    }

    public ValidationProblem withExpectedAndActualValues(String expectedValue, String actualValue) {
        return new ValidationProblem(this.level, this.fullMessage, this.simpleMessage, this.file, this.xsdFile, expectedValue, actualValue);
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

    public File getXsdFile() {
        return xsdFile;
    }

    public String getExpectedValue() {
        return expectedValue;
    }

    public String getActualValue() {
        return actualValue;
    }
}
