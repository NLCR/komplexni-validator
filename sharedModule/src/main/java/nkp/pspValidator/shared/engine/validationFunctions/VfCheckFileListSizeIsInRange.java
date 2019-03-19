package nkp.pspValidator.shared.engine.validationFunctions;

import nkp.pspValidator.shared.engine.Engine;
import nkp.pspValidator.shared.engine.Level;
import nkp.pspValidator.shared.engine.ValueEvaluation;
import nkp.pspValidator.shared.engine.ValueType;
import nkp.pspValidator.shared.engine.exceptions.ContractException;
import nkp.pspValidator.shared.engine.params.ValueParam;

import java.io.File;
import java.util.List;

/**
 * Created by Martin Řehánek on 27.10.16.
 */
public class VfCheckFileListSizeIsInRange extends ValidationFunction {

    public static final String PARAM_FILES = "files";
    public static final String PARAM_MIN_SIZE = "min_size";
    public static final String PARAM_MAX_SIZE = "max_size";
    public static final String PARAM_LEVEL = "level";

    public VfCheckFileListSizeIsInRange(String name, Engine engine) {
        super(name, engine, new Contract()
                .withValueParam(PARAM_FILES, ValueType.FILE_LIST, 1, 1)
                .withValueParam(PARAM_MIN_SIZE, ValueType.INTEGER, 0, 1)
                .withValueParam(PARAM_MAX_SIZE, ValueType.INTEGER, 0, 1)
                .withValueParam(PARAM_LEVEL, ValueType.LEVEL, 0, 1)
        );
    }

    @Override
    public ValidationResult validate() {
        try {
            checkContractCompliance();

            //LIST
            ValueEvaluation paramFiles = valueParams.getParams(PARAM_FILES).get(0).getEvaluation();
            List<File> fileList = (List<File>) paramFiles.getData();
            if (fileList == null) {
                return invalidValueParamNull(PARAM_FILES, paramFiles);
            }

            //LEVEL
            Level level = Level.ERROR;
            List<ValueParam> paramsLevel = valueParams.getParams(PARAM_LEVEL);
            if (!paramsLevel.isEmpty()) {
                ValueParam paramLevel = paramsLevel.get(0);
                ValueEvaluation evaluation = paramLevel.getEvaluation();
                if (evaluation.getData() == null) {
                    return invalidValueParamNull(PARAM_LEVEL, evaluation);
                } else {
                    level = (Level) evaluation.getData();
                }
            }

            //MIN, MAX
            Integer minSize = getSingleIntParamIfPresent(valueParams, PARAM_MIN_SIZE);
            Integer maxSize = getSingleIntParamIfPresent(valueParams, PARAM_MAX_SIZE);

            return validate(minSize, maxSize, fileList, level);
        } catch (ContractException e) {
            return invalidContractNotMet(e);
        } catch (Throwable e) {
            return invalidUnexpectedError(e);
        }
    }

    private Integer getSingleIntParamIfPresent(ValueParams valueParams, String paramName) {
        List<ValueParam> params = valueParams.getParams(paramName);
        if (params != null && !params.isEmpty()) {
            ValueEvaluation evaluation = params.get(0).getEvaluation();
            return (Integer) evaluation.getData();
        }
        return null;
    }

    private ValidationResult validate(Integer minSize, Integer maxSize, List<File> fileList, Level level) {
        if (minSize == null && maxSize == null) {
            return singlErrorResult(invalid(level, "je potřeba použít alespoň jeden z paramterů %s, %s", PARAM_MIN_SIZE, PARAM_MAX_SIZE));
        } else if (minSize != null && fileList.size() < minSize) {
            return singlErrorResult(invalid(level, "seznam obsahuje %d souborů, což je méně, než minimálních %d", fileList.size(), minSize));
        } else if (maxSize != null && fileList.size() > maxSize) {
            return singlErrorResult(invalid(level, "seznam obsahuje %d souborů, což je více, než maximálních %d", fileList.size(), maxSize));
        } else {
            return new ValidationResult();
        }
    }
}
