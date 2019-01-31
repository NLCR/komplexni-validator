package nkp.pspValidator.shared.engine.validationFunctions;

import nkp.pspValidator.shared.engine.Engine;
import nkp.pspValidator.shared.engine.ValueEvaluation;
import nkp.pspValidator.shared.engine.ValueType;
import nkp.pspValidator.shared.engine.exceptions.ContractException;

import java.io.File;


/**
 * Created by Martin Řehánek on 27.10.16.
 */
public class VfCheckFileIsNotDir extends ValidationFunction {

    public static final String PARAM_FILE = "file";


    public VfCheckFileIsNotDir(String name, Engine engine) {
        super(name, engine, new Contract()
                .withValueParam(PARAM_FILE, ValueType.FILE, 1, 1)
        );
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

            return validate(file);
        } catch (ContractException e) {
            return invalidContractNotMet(e);
        } catch (Throwable e) {
            return invalidUnexpectedError(e);
        }
    }

    private ValidationResult validate(File file) {
        if (!file.exists()) {
            return singlErrorResult(invalidFileDoesNotExist(file));
        } else if (file.isDirectory()) {
            return singlErrorResult(invalidFileIsDir(file));
        } else {
            return new ValidationResult();
        }
    }


}
