package rzehan.shared.engine.validationFunctions;

import rzehan.shared.engine.Engine;
import rzehan.shared.engine.ValueEvaluation;
import rzehan.shared.engine.ValueType;
import rzehan.shared.engine.exceptions.ContractException;
import rzehan.shared.engine.params.PatternParam;

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

        ValueEvaluation paramFile = valueParams.getParams(PARAM_FILE).get(0).getValueEvaluation();
        File file = (File) paramFile.getData();
        if (file == null) {
            return invalidParamNull(PARAM_FILE, paramFile);
        }

        PatternParam paternParam = patternParams.getParam(PARAM_PATTERN);
        if (paternParam == null) {
            return invalidParamNull(PARAM_PATTERN, null);
        } else if (!paternParam.matches(file.getName())) {
            return invalid(String.format("Název souboru %s neodpovídá vzoru %s", file.getName(), paternParam.toString()));
        } else {
            return valid();
        }
    }


}
