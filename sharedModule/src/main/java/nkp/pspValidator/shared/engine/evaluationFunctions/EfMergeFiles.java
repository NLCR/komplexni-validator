package nkp.pspValidator.shared.engine.evaluationFunctions;

import nkp.pspValidator.shared.engine.Engine;
import nkp.pspValidator.shared.engine.ValueEvaluation;
import nkp.pspValidator.shared.engine.ValueType;
import nkp.pspValidator.shared.engine.exceptions.ContractException;
import nkp.pspValidator.shared.engine.params.ValueParam;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Martin Řehánek on 21.10.16.
 */
public class EfMergeFiles extends EvaluationFunction {

    private static final String PARAM_FILE = "file";
    private static final String PARAM_FILE_LIST = "file_list";
    private static final String PARAM_FILE_LIST_LIST = "file_list_list";

    public EfMergeFiles(String name, Engine engine) {
        super(name, engine, new Contract()
                .withReturnType(ValueType.FILE_LIST)
                .withValueParam(PARAM_FILE, ValueType.FILE, 0, null)
                .withValueParam(PARAM_FILE_LIST, ValueType.FILE_LIST, 0, null)
                .withValueParam(PARAM_FILE_LIST_LIST, ValueType.FILE_LIST_LIST, 0, null)
        );
    }

    @Override
    public ValueEvaluation evaluate() {
        try {
            checkContractCompliance();

            List<File> result = new ArrayList<>();

            //just file
            List<ValueParam> params = valueParams.getParams(PARAM_FILE);
            for (ValueParam param : params) {
                ValueEvaluation paramEval = param.getEvaluation();
                File file = (File) paramEval.getData();
                if (file == null) {
                    return errorResultParamNull(PARAM_FILE, paramEval);
                } else {
                    result.add(file);
                }
            }

            //list of files
            List<ValueParam> fileListParams = valueParams.getParams(PARAM_FILE_LIST);
            for (ValueParam param : fileListParams) {
                ValueEvaluation eval = param.getEvaluation();
                List<File> list = (List<File>) eval.getData();
                if (list == null) {
                    return errorResultParamNull(PARAM_FILE_LIST, eval);
                } else {
                    result.addAll(list);
                }
            }

            //list of lists of files
            List<ValueParam> fileListListParams = valueParams.getParams(PARAM_FILE_LIST_LIST);
            for (ValueParam param : fileListListParams) {
                ValueEvaluation eval = param.getEvaluation();
                List<List<File>> listList = (List<List<File>>) eval.getData();
                if (listList == null) {
                    return errorResultParamNull(PARAM_FILE_LIST_LIST, eval);
                } else {
                    for (List<File> list : listList) {
                        result.addAll(list);
                    }
                }
            }

            return okResult(result);
        } catch (ContractException e) {
            return errorResultContractNotMet(e);
        } catch (Throwable e) {
            return errorResultUnexpectedError(e);
        }
    }

}
