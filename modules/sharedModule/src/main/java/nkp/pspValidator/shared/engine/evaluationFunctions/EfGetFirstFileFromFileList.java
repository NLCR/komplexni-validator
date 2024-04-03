package nkp.pspValidator.shared.engine.evaluationFunctions;

import nkp.pspValidator.shared.engine.Engine;
import nkp.pspValidator.shared.engine.ValueEvaluation;
import nkp.pspValidator.shared.engine.ValueType;
import nkp.pspValidator.shared.engine.exceptions.ContractException;
import nkp.pspValidator.shared.engine.params.ValueParam;

import java.io.File;
import java.util.List;

/**
 * Created by Martin Řehánek on 21.10.16.
 */
public class EfGetFirstFileFromFileList extends EvaluationFunction {

    private static final String PARAM_FILE_LIST = "files";
    private static final String PARAM_RETURN_NULL_IF_EMPTY = "returnNullIfEmpty";

    public EfGetFirstFileFromFileList(String name, Engine engine) {
        super(name, engine, new Contract()
                .withReturnType(ValueType.FILE)
                .withValueParam(PARAM_FILE_LIST, ValueType.FILE_LIST, 1, 1)
                .withValueParam(PARAM_RETURN_NULL_IF_EMPTY, ValueType.BOOLEAN, 0, 1)
        );
    }

    @Override
    public ValueEvaluation evaluate() {
        try {
            checkContractCompliance();

            boolean returnNullIfEmpty = false;
            List<ValueParam> returnNullIfEmptyParams = valueParams.getParams(PARAM_RETURN_NULL_IF_EMPTY);
            if (returnNullIfEmptyParams.size() == 1) {
                ValueEvaluation paramNsAware = returnNullIfEmptyParams.get(0).getEvaluation();
                returnNullIfEmpty = (boolean) paramNsAware.getData();
            }

            ValueEvaluation paramFiles = valueParams.getParams(PARAM_FILE_LIST).get(0).getEvaluation();
            List<File> files = (List<File>) paramFiles.getData();
            if (files == null) {
                return errorResultParamNull(PARAM_FILE_LIST, paramFiles);
            } else if (files.isEmpty()) {
                if (returnNullIfEmpty) {
                    return okResult(null);
                } else {
                    return errorResult("seznam souborů je prázdný");
                }
            } else {
                return okResult(files.get(0));
            }
        } catch (ContractException e) {
            return errorResultContractNotMet(e);
        } catch (Throwable e) {
            return errorResultUnexpectedError(e);
        }
    }
}
