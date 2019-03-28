package nkp.pspValidator.shared.externalUtils.validation;

import nkp.pspValidator.shared.engine.exceptions.ExternalUtilOutputParsingException;
import nkp.pspValidator.shared.externalUtils.CliCommand;
import nkp.pspValidator.shared.externalUtils.ExternalUtil;
import nkp.pspValidator.shared.externalUtils.ExternalUtilManager;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Martin Řehánek on 17.11.16.
 */
public abstract class J2kProfile {

    final ExternalUtilManager externalUtilManager;
    final ExternalUtil externalUtil;
    List<Validation> validations = new ArrayList<>();


    public J2kProfile(ExternalUtilManager externalUtilManager, ExternalUtil externalUtil) {
        this.externalUtilManager = externalUtilManager;
        this.externalUtil = externalUtil;
    }

    public void addValidation(Validation validation) {
        validations.add(validation);
    }

    public List<String> validate(File imageFile) {
        List<String> totalErrors = new ArrayList<>();
        String toolRawOutput = null;
        try {
            toolRawOutput = runExternalUtil(imageFile);
            Object processedOutput = processExternalUtilOutput(toolRawOutput, externalUtil);
            for (Validation validation : validations) {
                try {
                    List<String> validationErrors = validation.validate(processedOutput);
                    totalErrors.addAll(validationErrors);
                } catch (DataExtraction.ExtractionException e) {
                    totalErrors.add(e.getMessage());
                }
            }
        } catch (CliCommand.CliCommandException e) {
            totalErrors.add(e.getMessage());
        } catch (ExternalUtilOutputParsingException e) {
            totalErrors.add(e.getMessage());
        } finally {
            return totalErrors;
        }
    }

    String runExternalUtil(File imageFile) throws CliCommand.CliCommandException {
        return externalUtilManager.runUtilExecution(externalUtil, imageFile);
    }

    abstract Object processExternalUtilOutput(String toolRawOutput, ExternalUtil util) throws ExternalUtilOutputParsingException;

    public enum Type {
        XML, TEXT;
    }

    @Override
    public String toString() {
        return "J2kProfile{" +
                "externalUtil=" + externalUtil +
                ", validations=" + validations.size() +
                '}';
    }
}
