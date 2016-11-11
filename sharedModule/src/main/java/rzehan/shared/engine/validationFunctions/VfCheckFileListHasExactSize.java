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
public class VfCheckFileListHasExactSize extends ValidationFunction {

    public static final String PARAM_FILES = "files";
    public static final String PARAM_SIZE = "size";


    public VfCheckFileListHasExactSize(Engine engine) {
        super(engine, new Contract()
                .withValueParam(PARAM_FILES, ValueType.FILE_LIST, 1, 1)
                .withValueParam(PARAM_SIZE, ValueType.INTEGER, 1, 1)
        );
    }

    @Override
    public String getName() {
        return "checkFilelistHasExactSize";
    }

    @Override
    public ValidationResult validate() {
        try {
            checkContractCompliance();

            ValueEvaluation paramFiles = valueParams.getParams(PARAM_FILES).get(0).getEvaluation();
            List<File> fileList = (List<File>) paramFiles.getData();
            if (fileList == null) {
                return invalidValueParamNull(PARAM_FILES, paramFiles);
            }

            ValueEvaluation paramSize = valueParams.getParams(PARAM_SIZE).get(0).getEvaluation();
            Integer expectedSize = (Integer) paramSize.getData();
            if (expectedSize == null) {
                return invalidValueParamNull(PARAM_SIZE, paramSize);
            }

            if (fileList.size() != expectedSize) {
                return invalid(String.format("seznam obsahuje %d souborů namísto očekávaných %d", fileList.size(), expectedSize));
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
