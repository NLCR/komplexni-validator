package nkp.pspValidator.shared.engine.validationFunctions;

import nkp.pspValidator.shared.engine.Engine;
import nkp.pspValidator.shared.engine.ValueEvaluation;
import nkp.pspValidator.shared.engine.ValueType;
import nkp.pspValidator.shared.engine.exceptions.ContractException;
import nkp.pspValidator.shared.engine.params.ValueParam;

import java.io.File;
import java.util.List;


/**
 * Created by Martin Řehánek on 27.10.16.
 */
public class VfCheckFileIsDir extends ValidationFunction {

    public static final String PARAM_FILE = "file";
    public static final String PARAM_IGNORE_IF_FILE_IS_NULL = "ignoreIfFileIsNull";


    public VfCheckFileIsDir(String name, Engine engine) {
        super(name, engine, new Contract()
                .withValueParam(PARAM_FILE, ValueType.FILE, 1, 1)
                .withValueParam(PARAM_IGNORE_IF_FILE_IS_NULL, ValueType.BOOLEAN, 0, 1)
        );
    }

    @Override
    public ValidationResult validate() {
        try {
            checkContractCompliance();

            boolean ignoreIfFileIsNull = false;
            List<ValueParam> params = valueParams.getParams(PARAM_IGNORE_IF_FILE_IS_NULL);
            if (params.size() == 1) {
                ValueEvaluation eval = params.get(0).getEvaluation();
                ignoreIfFileIsNull = (boolean) eval.getData();
            }

            ValueEvaluation paramFile = valueParams.getParams(PARAM_FILE).get(0).getEvaluation();
            File file = (File) paramFile.getData();
            if (file == null) {
                if (ignoreIfFileIsNull) {
                    return new ValidationResult();
                } else {
                    return invalidValueParamNull(PARAM_FILE, paramFile);
                }
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
        } else if (!file.isDirectory()) {
            return singlErrorResult(invalidFileIsNotDir(file));
        } else {
            return new ValidationResult();
        }
    }

}
