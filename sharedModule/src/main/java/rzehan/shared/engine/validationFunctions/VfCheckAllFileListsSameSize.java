package rzehan.shared.engine.validationFunctions;

import rzehan.shared.engine.Engine;
import rzehan.shared.engine.ValueType;
import rzehan.shared.engine.params.ValueParam;

import java.io.File;
import java.util.List;


/**
 * Created by martin on 27.10.16.
 */
public class VfCheckAllFileListsSameSize extends ValidationFunction {

    public static final String PARAM_FILES = "files";


    public VfCheckAllFileListsSameSize(Engine engine) {
        super(engine, new Contract()
                .withValueParam(PARAM_FILES, ValueType.FILE, 2, null)
        );
    }

    @Override
    public ValidationResult validate() {
        checkContractCompliance();
        Integer size = null;

        List<ValueParam> params = valueParams.getParams(PARAM_FILES);
        for (ValueParam param : params) {
            List<File> files = (List<File>) param.getValue();
            if (files == null) {
                return new ValidationResult(false).withMessage(String.format("hodnota parametru %s funkce %s je null", PARAM_FILES, getName()));
            }
            if (size == null) {
                size = files.size();
            } else {
                if (size != files.size()) {
                    return new ValidationResult(false).withMessage(String.format("nalezeny různé velikosti seznamů soborů, např. %d a %d", size, files.size()));
                }
            }
        }
        return new ValidationResult(true);
    }

    @Override
    public String getName() {
        return "CHECK_ALL_FILE_LISTS_SAME_SIZE";
    }
}
