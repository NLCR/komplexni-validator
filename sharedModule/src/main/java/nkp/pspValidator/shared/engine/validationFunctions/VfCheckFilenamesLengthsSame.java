package nkp.pspValidator.shared.engine.validationFunctions;

import nkp.pspValidator.shared.engine.Engine;
import nkp.pspValidator.shared.engine.Level;
import nkp.pspValidator.shared.engine.ValueEvaluation;
import nkp.pspValidator.shared.engine.ValueType;
import nkp.pspValidator.shared.engine.exceptions.ContractException;

import java.io.File;
import java.util.List;


/**
 * Created by Martin Řehánek on 27.10.16.
 */
public class VfCheckFilenamesLengthsSame extends ValidationFunction {

    public static final String PARAM_FILES = "files";


    public VfCheckFilenamesLengthsSame(String name, Engine engine) {
        super(name, engine, new Contract()
                .withValueParam(PARAM_FILES, ValueType.FILE_LIST, 1, 1)
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
            return validate(files);
        } catch (ContractException e) {
            return invalidContractNotMet(e);
        } catch (Throwable e) {
            return invalidUnexpectedError(e);
        }
    }

    private ValidationResult validate(List<File> files) {
        ValidationResult result = new ValidationResult();
        Integer expectedLength = null;
        for (File file : files) {
            int length = file.getName().length();
            if (expectedLength == null) {
                expectedLength = length;
            } else if (length != expectedLength) {
                result.addError(invalid(Level.ERROR, "název souboru %s má odlišnou délku (%d) od ostatních názvů (%d)", file.getName(), length, expectedLength));
            }
        }
        return result;
    }

}
