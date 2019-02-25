package nkp.pspValidator.shared.engine.validationFunctions;

import nkp.pspValidator.shared.engine.*;
import nkp.pspValidator.shared.engine.exceptions.ContractException;

import java.io.File;
import java.util.List;


/**
 * Created by Martin Řehánek on 27.10.16.
 */
public class VfCheckAllFilenamesMatchPattern extends ValidationFunction {

    public static final String PARAM_FILES = "files";
    public static final String PARAM_PATTERN = "pattern";


    public VfCheckAllFilenamesMatchPattern(String name, Engine engine) {
        super(name, engine, new Contract()
                .withValueParam(PARAM_FILES, ValueType.FILE_LIST, 1, 1)
                .withPatternParam(PARAM_PATTERN)
        );
    }

    @Override
    public ValidationResult validate() {
        try {
            checkContractCompliance();

            ValueEvaluation paramFiles = valueParams.getParams(PARAM_FILES).get(0).getEvaluation();
            List<File> files = (List<File>) paramFiles.getData();
            if (files == null) {
                //ignore
                //return invalidValueParamNull(PARAM_FILES, paramFiles);
                return new ValidationResult();
            }

            PatternEvaluation patternParam = patternParams.getParam(PARAM_PATTERN).getEvaluation();
            if (!patternParam.isOk()) {
                return invalidPatternParamNull(PARAM_PATTERN, patternParam);
            }

            return validate(files, patternParam);
        } catch (ContractException e) {
            return invalidContractNotMet(e);
        } catch (Throwable e) {
            return invalidUnexpectedError(e);
        }

    }

    private ValidationResult validate(List<File> files, PatternEvaluation patternParam) {
        ValidationResult result = new ValidationResult();
        for (File file : files) {
            if (!patternParam.matches(file.getName())) {
                result.addError(invalid(Level.ERROR, "název souboru/adresáře '%s' neodpovídá vzoru %s", file.getName(), patternParam));
            }
        }
        return result;
    }

}
