package rzehan.shared.engine.validationFunctions;

import rzehan.shared.engine.Engine;
import rzehan.shared.engine.ValueEvaluation;
import rzehan.shared.engine.ValueType;
import rzehan.shared.engine.exceptions.ContractException;

import java.io.File;


/**
 * Created by martin on 27.10.16.
 */
public class VfCheckFileIsNotDir extends ValidationFunction {

    public static final String PARAM_FILE = "file";


    public VfCheckFileIsNotDir(Engine engine) {
        super(engine, new Contract()
                .withValueParam(PARAM_FILE, ValueType.FILE, 1, 1)
        );
    }

    @Override
    public String getName() {
        return "checkFileIsNotDir";
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
        } else if (!file.exists()) {
            return invalidFileDoesNotExist(file);
        } else if (file.isDirectory()) {
            return invalidFileIsDir(file);
        } else {
            return valid();
        }
    }


}
