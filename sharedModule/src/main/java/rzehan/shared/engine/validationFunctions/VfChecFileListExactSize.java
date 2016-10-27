package rzehan.shared.engine.validationFunctions;

import rzehan.shared.engine.Engine;
import rzehan.shared.engine.ValueType;

import java.io.File;
import java.util.List;

/**
 * Created by martin on 27.10.16.
 */
public class VfChecFileListExactSize extends ValidationFunction {

    public static final String PARAM_LIST = "list";
    public static final String PARAM_SIZE = "size";


    public VfChecFileListExactSize(Engine engine) {
        super(engine, new Contract()
                .withValueParam(PARAM_LIST, ValueType.LIST_OF_FILES, 1, 1)
                .withValueParam(PARAM_SIZE, ValueType.INTEGER, 1, 1)
        );
    }

    @Override
    public ValidationResult validate() {
        checkContractCompliance();

        List<File> fileList = (List<File>) valueParams.getParams(PARAM_LIST).get(0).getValue();
        Integer expectedSize = (Integer) valueParams.getParams(PARAM_SIZE).get(0).getValue();

        if (fileList == null) {
            return new ValidationResult(false).withMessage("seznam souborů je null");
        } else if (expectedSize == null) {
            return new ValidationResult(false).withMessage("očekávaná velikost seznamu je null");
        } else if (fileList.size() != expectedSize) {
            return new ValidationResult(false).withMessage(String.format("seznam obsahuje %d souborů namísto očekávaných %d", fileList.size(), expectedSize));
        } else {
            return new ValidationResult(true).withMessage(String.format("seznam obsahuje %d souborů", expectedSize));
        }
    }

    @Override
    public String getName() {
        return "CHECK_FILE_LIST_EXACT_SIZE";
    }
}
