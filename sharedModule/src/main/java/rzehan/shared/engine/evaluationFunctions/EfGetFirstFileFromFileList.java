package rzehan.shared.engine.evaluationFunctions;

import rzehan.shared.engine.Engine;
import rzehan.shared.engine.ValueEvaluation;
import rzehan.shared.engine.ValueType;
import rzehan.shared.engine.exceptions.ContractException;

import java.io.File;
import java.util.List;

/**
 * Created by martin on 21.10.16.
 */
public class EfGetFirstFileFromFileList extends EvaluationFunction {

    private static final String PARAM_FILE_LIST = "files";

    public EfGetFirstFileFromFileList(Engine engine) {
        super(engine, new Contract()
                .withReturnType(ValueType.FILE)
                .withValueParam(PARAM_FILE_LIST, ValueType.FILE_LIST, 1, 1)
        );
    }

    @Override
    public String getName() {
        return "getFirstFileFromFileList";
    }

    @Override
    public ValueEvaluation evaluate() {
        try {
            checkContractCompliance();

            ValueEvaluation paramFiles = valueParams.getParams(PARAM_FILE_LIST).get(0).getEvaluation();
            List<File> files = (List<File>) paramFiles.getData();
            if (files == null) {
                return errorResultParamNull(PARAM_FILE_LIST, paramFiles);
            } else if (files.isEmpty()) {
                return errorResult("seznam souborů je prázdný");
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
