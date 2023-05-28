package nkp.pspValidator.shared.engine.validationFunctions;

import nkp.pspValidator.shared.engine.Engine;
import nkp.pspValidator.shared.engine.Level;
import nkp.pspValidator.shared.engine.ValueEvaluation;
import nkp.pspValidator.shared.engine.ValueType;
import nkp.pspValidator.shared.engine.exceptions.ContractException;
import nkp.pspValidator.shared.engine.params.ValueParam;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


/**
 * Created by Martin Řehánek on 27.10.16.
 */
public class VfCheckAllFileListsHaveSameSize extends ValidationFunction {

    public static final String PARAM_FILES = "files";
    public static final String PARAM_FILES_OPTIONAL = "files_optional";


    public VfCheckAllFileListsHaveSameSize(String name, Engine engine) {
        super(name, engine, new Contract()
                .withValueParam(PARAM_FILES, ValueType.FILE_LIST, 2, null)
                .withValueParam(PARAM_FILES_OPTIONAL, ValueType.FILE_LIST, 0, null)
        );
    }

    @Override
    public ValidationResult validate() {
        try {
            checkContractCompliance();

            List<List<File>> list = new ArrayList<>();

            List<ValueParam> params = valueParams.getParams(PARAM_FILES);
            for (ValueParam param : params) {
                ValueEvaluation paramEvaluation = param.getEvaluation();
                List<File> files = (List<File>) paramEvaluation.getData();
                if (files == null) {
                    //ignore
                    //return invalidValueParamNull(PARAM_FILES, paramEvaluation);
                } else {
                    list.add(files);
                }
            }

            List<ValueParam> paramsOptional = valueParams.getParams(PARAM_FILES_OPTIONAL);
            for (ValueParam param : paramsOptional) {
                ValueEvaluation paramEvaluation = param.getEvaluation();
                List<File> files = (List<File>) paramEvaluation.getData();
                if (files != null && !files.isEmpty()) {
                    list.add(files);
                }
            }

            return validate(list);
        } catch (ContractException e) {
            return invalidContractNotMet(e);
        } catch (Throwable e) {
            return invalidUnexpectedError(e);
        }
    }

    private ValidationResult validate(List<List<File>> lists) {
        ValidationResult result = new ValidationResult();
        Integer size = null;
        for (List<File> list : lists) {
            if (size == null) {
                size = list.size();
            } else {
                if (size != list.size()) {
                    result.addError(invalid(Level.ERROR, null, "nalezeny různé velikosti seznamů souborů, např. %d a %d", size, list.size()));
                }
            }
        }
        return result;
    }


}
