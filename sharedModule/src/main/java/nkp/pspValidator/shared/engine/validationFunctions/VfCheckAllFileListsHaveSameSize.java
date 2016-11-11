package nkp.pspValidator.shared.engine.validationFunctions;

import nkp.pspValidator.shared.engine.Engine;
import nkp.pspValidator.shared.engine.ValueEvaluation;
import nkp.pspValidator.shared.engine.ValueType;
import nkp.pspValidator.shared.engine.exceptions.ContractException;
import nkp.pspValidator.shared.engine.params.ValueParam;

import java.io.File;
import java.util.List;


/**
 * Created by martin on 27.10.16.
 */
public class VfCheckAllFileListsHaveSameSize extends ValidationFunction {

    public static final String PARAM_FILES = "files";


    public VfCheckAllFileListsHaveSameSize(Engine engine) {
        super(engine, new Contract()
                .withValueParam(PARAM_FILES, ValueType.FILE_LIST, 2, null)
        );
    }

    @Override
    public String getName() {
        return "checkAllFileListsHaveSameSize";
    }

    @Override
    public ValidationResult validate() {
        try {
            checkContractCompliance();

            Integer size = null;

            List<ValueParam> params = valueParams.getParams(PARAM_FILES);
            for (ValueParam param : params) {
                ValueEvaluation paramEvaluation = param.getEvaluation();
                List<File> files = (List<File>) paramEvaluation.getData();
                if (files == null) {
                    return invalidValueParamNull(PARAM_FILES, paramEvaluation);
                }
                if (size == null) {
                    size = files.size();
                } else {
                    if (size != files.size()) {
                        return invalid(String.format("nalezeny různé velikosti seznamů soborů, např. %d a %d", size, files.size()));
                    }
                }
            }
            return valid();
        } catch (ContractException e) {
            return invalidContractNotMet(e);
        } catch (Throwable e) {
            return invalidUnexpectedError(e);
        }
    }


}
