package nkp.pspValidator.shared.imageUtils.validation;

import nkp.pspValidator.shared.imageUtils.ImageUtil;
import nkp.pspValidator.shared.imageUtils.ImageUtilManager;

import java.io.File;
import java.io.IOException;
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

    public List<String> validate(File imageFile) throws Exception {
        List<String> totalErrors = new ArrayList<>();
        String toolRawOutput = runImageUtil(imageFile);
        Object processedOutput = processImageUtilOutput(toolRawOutput, imageUtil);
        for (Validation validation : validations) {
            List<String> validationErrors = validation.validate(processedOutput);
            totalErrors.addAll(validationErrors);
        }
        return totalErrors;
    }

    String runImageUtil(File imageFile) throws IOException, InterruptedException {
        try {
            return imageUtilManager.runUtilExecution(imageUtil, imageFile);
        } catch (IOException e) {
            throw new IOException(String.format("chyba běhu nástroje %s: ", imageUtil, e.getMessage()), e);
        } catch (InterruptedException e) {
            throw new IOException(String.format("běh nástroje %s přerušen: ", imageUtil, e.getMessage()), e);
        }
    }

    abstract Object processImageUtilOutput(String toolRawOutput, ImageUtil util) throws Exception;

    public enum Type {
        XML, TEXT;
    }
}
