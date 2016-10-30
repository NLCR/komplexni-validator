package rzehan.shared.engine.validationFunctions;

import rzehan.shared.engine.Engine;
import rzehan.shared.engine.ValueType;

import java.io.File;


/**
 * Created by martin on 27.10.16.
 */
public class VfCheckChecksumGeneratedByGrammar extends ValidationFunction {

    public static final String PARAM_CHECKSUM_FILE = "checksum_file";


    public VfCheckChecksumGeneratedByGrammar(Engine engine) {
        super(engine, new Contract()
                .withValueParam(PARAM_CHECKSUM_FILE, ValueType.FILE, 1, 1)
        );
    }

    @Override
    public ValidationResult validate() {
        checkContractCompliance();

        File file = (File) valueParams.getParams(PARAM_CHECKSUM_FILE).get(0).getValue();

        if (file == null) {
            return new ValidationResult(false).withMessage(String.format("hodnota parametru %s funkce %s je null", PARAM_CHECKSUM_FILE, getName()));
        } else if (!file.exists()) {
            return new ValidationResult(false).withMessage(String.format("soubor %s neexistuje", file.getAbsoluteFile()));
        } else if (file.isDirectory()) {
            return new ValidationResult(false).withMessage(String.format("soubor %s je adresář", file.getAbsoluteFile()));
        } else {
            /*TODO: implement properly*/
            return new ValidationResult(false).withMessage("TODO: neni implementovano");
        }
    }

    @Override
    public String getName() {
        return "CHECK_CHECKSUM_GENERATED_BY_GRAMMAR";
    }
}
