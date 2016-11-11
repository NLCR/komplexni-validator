package nkp.pspValidator.shared.engine.validationFunctions;

import nkp.pspValidator.shared.engine.*;
import nkp.pspValidator.shared.engine.exceptions.ContractException;

import java.io.File;


/**
 * Created by martin on 27.10.16.
 */
public class VfCheckFilenameMatchesPattern extends ValidationFunction {

    public static final String PARAM_FILE = "file";
    public static final String PARAM_PATTERN = "pattern";


    public VfCheckFilenameMatchesPattern(Engine engine) {
        super(engine, new Contract()
                .withValueParam(PARAM_FILE, ValueType.FILE, 1, 1)
                .withPatternParam(PARAM_PATTERN)
        );
    }

    @Override
    public String getName() {
        return "checkFilenameMatchesPattern";
    }

    @Override
    public ValidationResult validate() {
        try {
            checkContractCompliance();

            ValueEvaluation paramFile = valueParams.getParams(PARAM_FILE).get(0).getEvaluation();
            File file = (File) paramFile.getData();
            if (file == null) {
                return invalidValueParamNull(PARAM_FILE, paramFile);
            }

            PatternEvaluation paramPattern = patternParams.getParam(PARAM_PATTERN).getEvaluation();
            if (!paramPattern.isOk()) {
                return invalidPatternParamNull(PARAM_PATTERN, paramPattern);
            }

            return validate(file, paramPattern);
        } catch (ContractException e) {
            return invalidContractNotMet(e);
        } catch (Throwable e) {
            return invalidUnexpectedError(e);
        }
    }

    private ValidationResult validate(File file, PatternEvaluation paramPattern) {
        if (!paramPattern.matches(file.getName())) {
            return singlErrorResult(invalid(Level.ERROR, "název souboru %s neodpovídá vzoru %s", file.getName(), paramPattern));
        } else {
            return new ValidationResult();
        }
    }

}
