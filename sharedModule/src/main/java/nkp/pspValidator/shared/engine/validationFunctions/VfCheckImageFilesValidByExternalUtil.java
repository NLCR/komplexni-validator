package nkp.pspValidator.shared.engine.validationFunctions;

import nkp.pspValidator.shared.engine.Engine;
import nkp.pspValidator.shared.engine.Level;
import nkp.pspValidator.shared.engine.ValueEvaluation;
import nkp.pspValidator.shared.engine.ValueType;
import nkp.pspValidator.shared.engine.exceptions.ContractException;
import nkp.pspValidator.shared.imageUtils.ImageCopy;
import nkp.pspValidator.shared.imageUtils.ImageUtil;
import nkp.pspValidator.shared.imageUtils.validation.ImageValidator;
import nkp.pspValidator.shared.imageUtils.validation.J2kProfile;

import java.io.File;
import java.util.List;


/**
 * Created by Martin Řehánek on 27.10.16.
 */
public class VfCheckImageFilesValidByExternalUtil extends ValidationFunction {

    public static final String PARAM_FILES = "files";
    public static final String PARAM_LEVEL = "level";
    public static final String PARAM_COPY = "copy";
    public static final String PARAM_UTIL = "util";


    public VfCheckImageFilesValidByExternalUtil(Engine engine) {
        super(engine, new Contract()
                .withValueParam(PARAM_FILES, ValueType.FILE_LIST, 1, 1)
                .withValueParam(PARAM_LEVEL, ValueType.LEVEL, 1, 1)
                .withValueParam(PARAM_COPY, ValueType.IMAGE_COPY, 1, 1)
                .withValueParam(PARAM_UTIL, ValueType.IMAGE_UTIL, 1, 1)

        );
    }

    @Override
    public String getName() {
        return "checkImageFilesValidByExternalUtil";
    }

    @Override
    public ValidationResult validate() {
        try {
            checkContractCompliance();

            ValueEvaluation paramFiles = valueParams.getParams(PARAM_FILES).get(0).getEvaluation();
            List<File> files = (List<File>) paramFiles.getData();
            if (files == null) {
                return invalidValueParamNull(PARAM_FILES, paramFiles);
            }

            ValueEvaluation paramLevel = valueParams.getParams(PARAM_LEVEL).get(0).getEvaluation();
            Level level = (Level) paramLevel.getData();
            if (level == null) {
                return invalidValueParamNull(PARAM_LEVEL, paramLevel);
            }

            ValueEvaluation paramCopy = valueParams.getParams(PARAM_COPY).get(0).getEvaluation();
            ImageCopy copy = (ImageCopy) paramCopy.getData();
            if (copy == null) {
                return invalidValueParamNull(PARAM_COPY, paramCopy);
            }

            ValueEvaluation paramUtil = valueParams.getParams(PARAM_UTIL).get(0).getEvaluation();
            ImageUtil util = (ImageUtil) paramUtil.getData();
            if (util == null) {
                return invalidValueParamNull(PARAM_UTIL, paramUtil);
            }

            return validate(level, files, copy, util);
        } catch (ContractException e) {
            return invalidContractNotMet(e);
        } catch (Exception e) {
            e.printStackTrace();
            return invalidUnexpectedError(e);
        }
    }

    private ValidationResult validate(Level level, List<File> files, ImageCopy copy, ImageUtil util) {
        if (!engine.getImageValidator().isUtilAvailable(util)) {
            return singlErrorResult(invalid(Level.INFO, "nástroj %s není dostupný", util.getUserFriendlyName()));
        } else {
            ValidationResult result = new ValidationResult();
            ImageValidator imageValidator = engine.getImageValidator();
            J2kProfile profile = imageValidator.getProfile(copy, util);
            if (profile == null) {
                return singlErrorResult(invalid(Level.ERROR, "nenalezen J2K profil pro kopii %s a nástroj %s", copy, util));
            }
            for (File file : files) {
                //System.out.println(String.format("validating (%s): %s", profile, file.getAbsolutePath()));
                try {
                    List<String> problems = profile.validate(file);
                    for (String problem : problems) {
                        result.addError(invalid(level, "%s (soubor %s)", problem, file.getCanonicalPath()));
                    }
                } catch (Exception e) {
                    result.addError(invalid(Level.ERROR, "%s: (soubor %s)", e.getMessage(), file.getName()));
                    e.printStackTrace();
                }
                //break;
            }
            return result;
        }
    }

}
