package nkp.pspValidator.shared.engine.validationFunctions;

import nkp.pspValidator.shared.engine.Engine;
import nkp.pspValidator.shared.engine.ValueEvaluation;
import nkp.pspValidator.shared.engine.ValueType;
import nkp.pspValidator.shared.engine.exceptions.ContractException;

import java.io.File;


/**
 * Created by martin on 27.10.16.
 */
public class VfCheckFileIsDir extends ValidationFunction {

    public static final String PARAM_FILE = "file";


    public VfCheckFileIsDir(Engine engine) {
        super(engine, new Contract()
                .withValueParam(PARAM_FILE, ValueType.FILE, 1, 1)
        );
    }

    @Override
    public String getName() {
        return "checkFileIsDir";
    }

    @Override
    public ValidationResult validate() {
        try {
            checkContractCompliance();

            ValueEvaluation paramFile = valueParams.getParams(PARAM_FILE).get(0).getEvaluation();
            File file = (File) paramFile.getData();
            if (file == null) {
                return invalidValueParamNull(PARAM_FILE, paramFile);
            } else if (!file.exists()) {
                return invalidFileDoesNotExist(file);
            } else if (!file.isDirectory()) {
                return invalidFileIsNotDir(file);
            } else {
                return valid();
            }
        } catch (ContractException e) {
            return invalidContractNotMet(e);
        } catch (Throwable e) {
            return invalidUnexpectedError(e);
        }
    }

}
