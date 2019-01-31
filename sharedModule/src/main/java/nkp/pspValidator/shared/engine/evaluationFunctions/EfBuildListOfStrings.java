package nkp.pspValidator.shared.engine.evaluationFunctions;

import nkp.pspValidator.shared.engine.Engine;
import nkp.pspValidator.shared.engine.ValueEvaluation;
import nkp.pspValidator.shared.engine.ValueType;
import nkp.pspValidator.shared.engine.exceptions.ContractException;
import nkp.pspValidator.shared.engine.params.ValueParam;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Martin Řehánek on 21.10.16.
 */
public class EfBuildListOfStrings extends EvaluationFunction {

    private static final String PARAM_STRING = "string";

    public EfBuildListOfStrings(String name, Engine engine) {
        super(name, engine, new Contract()
                .withReturnType(ValueType.STRING_LIST)
                .withValueParam(PARAM_STRING, ValueType.STRING, 0, null));
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
