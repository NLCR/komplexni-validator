package rzehan.shared.engine.validationFunctions;

import rzehan.shared.engine.Engine;
import rzehan.shared.engine.ValueType;
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
    public ValidationResult validate() {
        checkContractCompliance();

        File file = (File) valueParams.getParams(PARAM_FILE).get(0).getValue();
        PatternParam paternParam = patternParams.getParam(PARAM_PATTERN);

        if (file == null) {
            return new ValidationResult(false).withMessage(String.format("hodnota parametru %s funkce %s je null", PARAM_FILE, getName()));
        } else if (paternParam == null) {
            return new ValidationResult(false).withMessage(String.format("hodnota parametru %s funkce %s je null", PARAM_PATTERN, getName()));
        } else if (!paternParam.matches(file.getName())) {
            return new ValidationResult(false).withMessage(String.format("Název souboru %s neodpovídá vzoru %s", file.getName(), paternParam.toString()));
        } else {
            return new ValidationResult(true);
        }
    }

    @Override
    public String getName() {
        return "CHECK_FILENAME_MATCHES_PATTERN";
    }
}
