package rzehan.shared.engine.validationFunctions;

import rzehan.shared.engine.Engine;
import rzehan.shared.engine.ValueType;
import rzehan.shared.engine.params.PatternParam;

import java.io.File;
import java.util.List;


/**
 * Created by martin on 27.10.16.
 */
public class VfCheckAllFilenamesMatchPattern extends ValidationFunction {

    public static final String PARAM_FILES = "files";
    public static final String PARAM_PATTERN = "pattern";


    public VfCheckAllFilenamesMatchPattern(Engine engine) {
        super(engine, new Contract()
                .withValueParam(PARAM_FILES, ValueType.FILE_LIST, 1, 1)
                .withPatternParam(PARAM_PATTERN)
        );
    }

    @Override
    public ValidationResult validate() {
        checkContractCompliance();

        List<File> files = (List<File>) valueParams.getParams(PARAM_FILES).get(0).getValue();
        PatternParam paternParam = patternParams.getParam(PARAM_PATTERN);

        if (files == null) {
            return new ValidationResult(false).withMessage(String.format("hodnota parametru %s funkce %s je null", PARAM_FILES, getName()));
        } else if (paternParam == null) {
            return new ValidationResult(false).withMessage(String.format("hodnota parametru %s funkce %s je null", PARAM_PATTERN, getName()));
        } else {
            for (File file : files) {
                if (!paternParam.matches(file.getName())) {
                    return new ValidationResult(false).withMessage(String.format("Název souboru %s neodpovídá vzoru %s", file.getName(), paternParam.toString()));
                }
            }
            return new ValidationResult(true);
        }
    }

    @Override
    public String getName() {
        return "CHECK_ALL_FILENAMES_MATCH_PATTERN";
    }
}
