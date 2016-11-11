package rzehan.shared.engine.evaluationFunctions;

import rzehan.shared.engine.Engine;
import rzehan.shared.engine.ValueEvaluation;
import rzehan.shared.engine.ValueType;
import rzehan.shared.engine.exceptions.ContractException;
import rzehan.shared.engine.params.ValueParam;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by martin on 21.10.16.
 */
public class EfBuildListOfStrings extends EvaluationFunction {

    private static final String PARAM_STRING = "string";

    public EfBuildListOfStrings(Engine engine) {
        super(engine, new Contract()
                .withReturnType(ValueType.STRING_LIST)
                .withValueParam(PARAM_STRING, ValueType.STRING, 0, null));
    }

    @Override
    public String getName() {
        return "buildListOfStrings";
    }

    @Override
    public ValueEvaluation evaluate() {
        try {
            checkContractCompliance();

            List<ValueParam> params = valueParams.getParams(PARAM_STRING);
            List<String> result = new ArrayList<>(params.size());
            for (ValueParam param : params) {
                ValueEvaluation paramEval = param.getEvaluation();
                String string = (String) paramEval.getData();
                if (string == null) {
                    return errorResultParamNull(PARAM_STRING, paramEval);
                } else {
                    result.add(string);
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
