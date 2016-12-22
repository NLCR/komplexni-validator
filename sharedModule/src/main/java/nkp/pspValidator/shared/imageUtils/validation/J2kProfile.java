package nkp.pspValidator.shared.imageUtils.validation;

import nkp.pspValidator.shared.engine.exceptions.ImageUtilOutputParsingException;
import nkp.pspValidator.shared.imageUtils.CliCommand;
import nkp.pspValidator.shared.imageUtils.ImageUtil;
import nkp.pspValidator.shared.imageUtils.ImageUtilManager;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by martin on 17.11.16.
 */
public abstract class J2kProfile {

    final ImageUtilManager imageUtilManager;
    final ImageUtil imageUtil;
    List<Validation> validations = new ArrayList<>();


    public J2kProfile(ImageUtilManager imageUtilManager, ImageUtil imageUtil) {
        this.imageUtilManager = imageUtilManager;
        this.imageUtil = imageUtil;
    }

    public void addValidation(Validation validation) {
        validations.add(validation);
    }

    public List<String> validate(File imageFile) {
        List<String> totalErrors = new ArrayList<>();
        String toolRawOutput = null;
        try {
            toolRawOutput = runImageUtil(imageFile);
            Object processedOutput = processImageUtilOutput(toolRawOutput, imageUtil);
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
        } catch (ImageUtilOutputParsingException e) {
            totalErrors.add(e.getMessage());
        } finally {
            return totalErrors;
        }
    }

    String runImageUtil(File imageFile) throws CliCommand.CliCommandException {
        return imageUtilManager.runUtilExecution(imageUtil, imageFile);
    }

    abstract Object processImageUtilOutput(String toolRawOutput, ImageUtil util) throws ImageUtilOutputParsingException;

    public enum Type {
        XML, TEXT;
    }

    @Override
    public String toString() {
        return "J2kProfile{" +
                "imageUtil=" + imageUtil +
                ", validations=" + validations.size() +
                '}';
    }
}
