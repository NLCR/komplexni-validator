package rzehan.shared.engine.evaluationFunctions;

import rzehan.shared.engine.Engine;
import rzehan.shared.engine.ValueEvaluation;
import rzehan.shared.engine.ValueType;
import rzehan.shared.engine.exceptions.ContractException;

/**
 * Created by martin on 20.10.16.
 */
public class EfGetProvidedString extends EvaluationFunction {

    private static final String PARAM_STRING_ID = "string_id";


    public EfGetProvidedString(Engine engine) {
        super(engine, new Contract()
                .withReturnType(ValueType.STRING)
                .withValueParam(PARAM_STRING_ID, ValueType.STRING, 1, 1));
    }

    @Override
    public String getName() {
        return "getProvidedString";
    }

    @Override
    public ValueEvaluation evaluate() {
        try {
            checkContractCompliance();

            ValueEvaluation paramStringId = valueParams.getParams(PARAM_STRING_ID).get(0).getEvaluation();
            String stringId = (String) paramStringId.getData();
            if (stringId == null) {
                return errorResultParamNull(PARAM_STRING_ID, paramStringId);
            } else if (stringId.isEmpty()) {
                return errorResult(String.format("hodnota parametru %s je prázdná", PARAM_STRING_ID));
            }

            String string = engine.getProvidedVarsManager().getProvidedString(stringId);
            if (string == null) {
                return errorResult(String.format("řetězec s id %s není poskytován", stringId));
            } else {
                return okResult(string);
            }
        } catch (ContractException e) {
            return errorResultContractNotMet(e);
        } catch (Throwable e) {
            return errorResultUnexpectedError(e);
        }
    }

}
