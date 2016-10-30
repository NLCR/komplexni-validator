package rzehan.shared.engine.validationFunctions;

import rzehan.shared.engine.Engine;
import rzehan.shared.engine.ValueType;

import java.io.File;
import java.util.List;


/**
 * Created by martin on 27.10.16.
 */
public class VfCheckNoFileIsDir extends ValidationFunction {

    public static final String PARAM_FILES = "files";


    public VfCheckNoFileIsDir(Engine engine) {
        super(engine, new Contract()
                .withValueParam(PARAM_FILES, ValueType.FILE_LIST, 1, 1)
        );
    }

    @Override
    public ValidationResult validate() {
        checkContractCompliance();

        List<File> files = (List<File>) valueParams.getParams(PARAM_FILES).get(0).getValue();


        if (files == null) {
            return new ValidationResult(false).withMessage(String.format("hodnota parametru %s funkce %s je null", PARAM_FILES, getName()));
        } else {
            for (File file : files) {
                if (!file.exists()) {
                    return new ValidationResult(false).withMessage(String.format("soubor %s neexistuje", file.getAbsoluteFile()));
                } else if (file.isDirectory()) {
                    return new ValidationResult(false).withMessage(String.format("soubor %s je adresář", file.getAbsoluteFile()));
                }
            }
        }
        return new ValidationResult(true);
    }

    @Override
    public String getName() {
        return "CHECK_NO_FILE_IS_DIR";
    }
}
