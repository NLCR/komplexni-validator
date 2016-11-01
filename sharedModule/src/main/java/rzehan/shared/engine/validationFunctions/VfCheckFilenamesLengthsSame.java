package rzehan.shared.engine.validationFunctions;

import rzehan.shared.engine.Engine;
import rzehan.shared.engine.ValueEvaluation;
import rzehan.shared.engine.ValueType;
import rzehan.shared.engine.exceptions.ContractException;

import java.io.File;
import java.util.List;


/**
 * Created by martin on 27.10.16.
 */
public class VfCheckFilenamesLengthsSame extends ValidationFunction {

    public static final String PARAM_FILES = "files";


    public VfCheckFilenamesLengthsSame(Engine engine) {
        super(engine, new Contract()
                .withValueParam(PARAM_FILES, ValueType.FILE_LIST, 1, 1)
        );
    }

    @Override
    public String getName() {
        return "checkFilenamesLengthsSame";
    }

    @Override
    public ValidationResult validate() {
        try {
            checkContractCompliance();
        } catch (ContractException e) {
            return invalidContractNotMet(e);
        }

        ValueEvaluation paramFiles = valueParams.getParams(PARAM_FILES).get(0).getEvaluation();
        List<File> files = (List<File>) paramFiles.getData();
        if (files == null) {
            return invalidValueParamNull(PARAM_FILES, paramFiles);
        }

        Integer expectedLength = null;
        for (File file : files) {
            if (!file.exists()) {
                return invalidFileDoesNotExist(file);
            } else {
                int length = file.getName().length();
                if (expectedLength == null) {
                    expectedLength = length;
                } else if (length != expectedLength) {
                    return invalid(String.format("název souboru %s má odlišnou délku (%d) od ostatních názvů (%d)",
                            file.getName(), length, expectedLength)
                    );
                }
            }
        }
        return valid();
    }

}
