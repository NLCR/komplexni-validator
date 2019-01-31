package nkp.pspValidator.shared.engine.evaluationFunctions;

import nkp.pspValidator.shared.engine.Engine;
import nkp.pspValidator.shared.engine.ValueEvaluation;
import nkp.pspValidator.shared.engine.ValueType;
import nkp.pspValidator.shared.engine.exceptions.ContractException;

/**
 * Created by Martin Řehánek on 20.10.16.
 */
public class EfGetProvidedInteger extends EvaluationFunction {

    private static final String PARAM_INT_ID = "int_id";


    public EfGetProvidedInteger(String name, Engine engine) {
        super(name, engine, new Contract()
                .withReturnType(ValueType.INTEGER)
                .withValueParam(PARAM_INT_ID, ValueType.STRING, 1, 1));
    }

    @Override
    public ValueEvaluation evaluate() {
        try {
            checkContractCompliance();

            ValueEvaluation paramIntId = valueParams.getParams(PARAM_INT_ID).get(0).getEvaluation();
            String intId = (String) paramIntId.getData();
            if (intId == null) {
                return errorResultParamNull(PARAM_INT_ID, paramIntId);
            } else if (intId.isEmpty()) {
                return errorResult(String.format("hodnota parametru %s je prázdná", PARAM_INT_ID));
            }

            Integer value = engine.getProvidedVarsManager().getProvidedInteger(intId);
            if (value == null) {
                return errorResult(String.format("číslo s id %s není poskytováno", intId));
            } else {
                return okResult(value);
            }
        } catch (ContractException e) {
            return errorResultContractNotMet(e);
        } catch (Throwable e) {
            return errorResultUnexpectedError(e);
        }
    }


}
