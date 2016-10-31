package rzehan.shared.engine.validationFunctions;

import rzehan.shared.engine.Engine;
import rzehan.shared.engine.ValueType;

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
        checkContractCompliance();

        List<File> files = (List<File>) valueParams.getParams(PARAM_FILES).get(0).getValue();

        if (files == null) {
            return new ValidationResult(false).withMessage(String.format("hodnota parametru %s funkce %s je null", PARAM_FILES, getName()));
        } else {
            Integer expectedLength = null;
            for (File file : files) {
                if (!file.exists()) {
                    return new ValidationResult(true).withMessage(String.format("soubor %d neexistuje", file.getAbsolutePath()));
                } else {
                    int length = file.getName().length();
                    if (expectedLength == null) {
                        expectedLength = length;
                    } else if (length != expectedLength) {
                        return new ValidationResult(true).withMessage(
                                String.format("název souboru %s má odlišnou délku (%d) od ostatních názvů (%d)", file.getName(), length, expectedLength)
                        );
                    }
                }
            }
            return new ValidationResult(true);
        }
    }

}
