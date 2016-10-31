package rzehan.shared.engine.validationFunctions;

import rzehan.shared.engine.Engine;
import rzehan.shared.engine.ValueType;

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
        checkContractCompliance();

        List<File> fileList = (List<File>) valueParams.getParams(PARAM_FILES).get(0).getValue();
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
}
