package nkp.pspValidator.shared.engine.validationFunctions;

import nkp.pspValidator.shared.engine.*;
import nkp.pspValidator.shared.engine.exceptions.ContractException;
import nkp.pspValidator.shared.engine.params.ValueParam;

import java.io.File;
import java.util.List;


/**
 * Created by Martin Řehánek on 27.10.16.
 */
public class VfCheckFilenameMatchesPattern extends ValidationFunction {

    public static final String PARAM_FILE = "file";
    public static final String PARAM_PATTERN = "pattern";
    public static final String PARAM_LEVEL = "level";


    public VfCheckFilenameMatchesPattern(String name, Engine engine) {
        super(name, engine, new Contract()
                .withValueParam(PARAM_FILE, ValueType.FILE, 1, 1)
                .withPatternParam(PARAM_PATTERN)
                .withValueParam(PARAM_LEVEL, ValueType.LEVEL, 0, 1)
        );
    }

    @Override
    public ValidationResult validate() {
        try {
            checkContractCompliance();

            ValueEvaluation paramFile = valueParams.getParams(PARAM_FILE).get(0).getEvaluation();
            File file = (File) paramFile.getData();
            /*if (file == null) {
                return invalidValueParamNull(PARAM_FILE, paramFile);
            }*/

            PatternEvaluation paramPattern = patternParams.getParam(PARAM_PATTERN).getEvaluation();
            if (!paramPattern.isOk()) {
                return invalidPatternParamNull(PARAM_PATTERN, paramPattern);
            }

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

            return validate(file, paramPattern, level);
        } catch (ContractException e) {
            return invalidContractNotMet(e);
        } catch (Throwable e) {
            return invalidUnexpectedError(e);
        }
    }

    private ValidationResult validate(File file, PatternEvaluation paramPattern, Level level) {
        if (file == null) {
            return new ValidationResult();

        } else if (!paramPattern.matches(file.getName())) {
            return singlErrorResult(invalid(level, "název souboru %s neodpovídá vzoru %s", file.getName(), paramPattern));
        } else {
            return new ValidationResult();
        }
    }

}
