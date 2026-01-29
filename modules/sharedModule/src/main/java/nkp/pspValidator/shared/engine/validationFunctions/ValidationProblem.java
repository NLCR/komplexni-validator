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
    private final String pattern;
    private final File referencedFile;
    private final String referencedValue;

    public ValidationProblem(Level level, String fullMessage) {
        this.level = level;
        this.fullMessage = fullMessage;
        this.simpleMessage = null;
        this.file = null;
        this.xsdFile = null;
        this.expectedValue = null;
        this.actualValue = null;
        this.pattern = null;
        this.referencedFile = null;
        this.referencedValue = null;
    }

    private ValidationProblem(Level level, String fullMessage, String simpleMessage, File file, File xsdFile,
                              String expectedValue, String actualValue, String pattern, File referencedFile, String referencedValue) {
        this.level = level;
        this.fullMessage = fullMessage;
        this.simpleMessage = simpleMessage;
        this.file = file;
        this.xsdFile = xsdFile;
        this.expectedValue = expectedValue;
        this.actualValue = actualValue;
        this.pattern = pattern;
        this.referencedFile = referencedFile;
        this.referencedValue = referencedValue;
    }

    public ValidationProblem withSimpleMessage(String simpleMessage) {
        return new ValidationProblem(this.level, this.fullMessage, simpleMessage, this.file, this.xsdFile,
                this.expectedValue, this.actualValue, this.pattern, this.referencedFile, this.referencedValue);
    }

    public ValidationProblem withFile(File file) {
        return new ValidationProblem(this.level, this.fullMessage, this.simpleMessage, file, this.xsdFile,
                this.expectedValue, this.actualValue, this.pattern, this.referencedFile, this.referencedValue);
    }

    public ValidationProblem withXsdFile(File xsdFile) {
        return new ValidationProblem(this.level, this.fullMessage, this.simpleMessage, this.file, xsdFile,
                this.expectedValue, this.actualValue, this.pattern, this.referencedFile, this.referencedValue);
    }

    public ValidationProblem withExpectedAndActualValues(String expectedValue, String actualValue) {
        return new ValidationProblem(this.level, this.fullMessage, this.simpleMessage, this.file, this.xsdFile,
                expectedValue, actualValue, this.pattern, this.referencedFile, this.referencedValue);
    }

    public ValidationProblem withPattern(String pattern) {
        return new ValidationProblem(this.level, this.fullMessage, this.simpleMessage, this.file, this.xsdFile,
                this.expectedValue, this.actualValue, pattern, this.referencedFile, this.referencedValue);
    }

    public ValidationProblem withReferencedFile(File file) {
        return new ValidationProblem(this.level, this.fullMessage, this.simpleMessage, this.file, this.xsdFile,
                this.expectedValue, this.actualValue, this.pattern, file, this.referencedValue);
    }

    public ValidationProblem withReferencedValue(String referencedValue) {
        return new ValidationProblem(this.level, this.fullMessage, this.simpleMessage, this.file, this.xsdFile,
                this.expectedValue, this.actualValue, this.pattern, this.referencedFile, referencedValue);
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

    public String getPattern() {
        return pattern;
    }

    public File getReferencedFile() {
        return referencedFile;
    }

    public String getReferencedValue() {
        return referencedValue;
    }
}
