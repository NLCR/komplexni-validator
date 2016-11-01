package rzehan.shared.engine.validationFunctions;

import rzehan.shared.engine.Engine;
import rzehan.shared.engine.PatternEvaluation;
import rzehan.shared.engine.ValueEvaluation;
import rzehan.shared.engine.ValueType;
import rzehan.shared.engine.exceptions.ContractException;

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
        } catch (ContractException e) {
            return invalidContractNotMet(e);
        }

        ValueEvaluation paramFile = valueParams.getParams(PARAM_FILE).get(0).getEvaluation();
        File file = (File) paramFile.getData();
        if (file == null) {
            return invalidValueParamNull(PARAM_FILE, paramFile);
        }

        PatternEvaluation paramPattern = patternParams.getParam(PARAM_PATTERN).getEvaluation();
        if (!paramPattern.isOk()) {
            return invalidPatternParamNull(PARAM_PATTERN, paramPattern);
        }

        if (!paramPattern.matches(file.getName())) {
            return invalid(String.format("název souboru %s neodpovídá vzoru %s", file.getName(), paramPattern));
        } else {
            return valid();
        }
    }


}
