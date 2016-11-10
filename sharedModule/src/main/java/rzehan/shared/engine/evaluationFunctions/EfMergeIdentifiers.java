package rzehan.shared.engine.evaluationFunctions;

import rzehan.shared.engine.Engine;
import rzehan.shared.engine.ValueEvaluation;
import rzehan.shared.engine.ValueType;
import rzehan.shared.engine.exceptions.ContractException;
import rzehan.shared.engine.params.ValueParam;
import rzehan.shared.engine.types.Identifier;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by martin on 21.10.16.
 */
public class EfMergeIdentifiers extends EvaluationFunction {

    private static final String PARAM_ID = "identifier";
    private static final String PARAM_ID_LIST = "identifier_list";
    private static final String PARAM_ID_LIST_LIST = "identifier_list_list";

    public EfMergeIdentifiers(Engine engine) {
        super(engine, new Contract()
                .withReturnType(ValueType.IDENTIFIER_LIST)
                .withValueParam(PARAM_ID, ValueType.IDENTIFIER, 0, null)
                .withValueParam(PARAM_ID_LIST, ValueType.IDENTIFIER_LIST, 0, null)
                .withValueParam(PARAM_ID_LIST_LIST, ValueType.IDENTIFIER_LIST_LIST, 0, null)
        );
    }

    @Override
    public String getName() {
        return "mergeIdentifiers";
    }

    @Override
    public ValueEvaluation evaluate() {
        try {
            checkContractCompliance();

            List<Identifier> result = new ArrayList<>();

            //just id
            List<ValueParam> params = valueParams.getParams(PARAM_ID);
            for (ValueParam param : params) {
                ValueEvaluation paramEval = param.getEvaluation();
                Identifier identifier = (Identifier) paramEval.getData();
                if (identifier == null) {
                    return errorResultParamNull(PARAM_ID, paramEval);
                } else {
                    result.add(identifier);
                }
            }

            //list of id
            List<ValueParam> idListParams = valueParams.getParams(PARAM_ID_LIST);
            for (ValueParam param : idListParams) {
                ValueEvaluation eval = param.getEvaluation();
                List<Identifier> list = (List<Identifier>) eval.getData();
                if (list == null) {
                    return errorResultParamNull(PARAM_ID_LIST, eval);
                } else {
                    result.addAll(list);
                }
            }

            //list of lists of id
            List<ValueParam> idListListParams = valueParams.getParams(PARAM_ID_LIST_LIST);
            for (ValueParam param : idListListParams) {
                ValueEvaluation eval = param.getEvaluation();
                List<List<Identifier>> listList = (List<List<Identifier>>) eval.getData();
                if (listList == null) {
                    return errorResultParamNull(PARAM_ID_LIST_LIST, eval);
                } else {
                    for (List<Identifier> list : listList) {
                        result.addAll(list);
                    }
                }
            }

            return okResult(result);
        } catch (ContractException e) {
            return errorResultContractNotMet(e);
        } catch (Throwable e) {
            return errorResult(String.format("Nečekaná chyba: %s", e.getMessage()));
        }
    }

}
