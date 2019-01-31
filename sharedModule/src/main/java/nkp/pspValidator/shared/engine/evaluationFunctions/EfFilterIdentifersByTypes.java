package nkp.pspValidator.shared.engine.evaluationFunctions;

import nkp.pspValidator.shared.engine.Engine;
import nkp.pspValidator.shared.engine.ValueEvaluation;
import nkp.pspValidator.shared.engine.ValueType;
import nkp.pspValidator.shared.engine.exceptions.ContractException;
import nkp.pspValidator.shared.engine.types.Identifier;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Martin Řehánek on 21.10.16.
 */
public class EfFilterIdentifersByTypes extends EvaluationFunction {

    private static final String PARAM_IDENTIFIERS = "identifiers";
    private static final String PARAM_TYPES = "types";

    public EfFilterIdentifersByTypes(String name, Engine engine) {
        super(name, engine, new Contract()
                .withReturnType(ValueType.IDENTIFIER_LIST)
                .withValueParam(PARAM_IDENTIFIERS, ValueType.IDENTIFIER_LIST, 1, 1)
                .withValueParam(PARAM_TYPES, ValueType.STRING_LIST, 1, 1)
        );
    }

    @Override
    public ValueEvaluation evaluate() {
        try {
            checkContractCompliance();

            ValueEvaluation paramIdsEval = valueParams.getParams(PARAM_IDENTIFIERS).get(0).getEvaluation();
            List<Identifier> identifiers = (List<Identifier>) paramIdsEval.getData();
            if (identifiers == null) {
                return errorResultParamNull(PARAM_IDENTIFIERS, paramIdsEval);
            }

            ValueEvaluation paramTypesEval = valueParams.getParams(PARAM_TYPES).get(0).getEvaluation();
            List<String> types = (List<String>) paramTypesEval.getData();
            if (types == null) {
                return errorResultParamNull(PARAM_TYPES, paramTypesEval);
            }

            return evaluate(identifiers, types);
        } catch (ContractException e) {
            return errorResultContractNotMet(e);
        } catch (Throwable e) {
            return errorResultUnexpectedError(e);
        }
    }

    protected ValueEvaluation evaluate(List<Identifier> identifiers, List<String> types) {
        List<Identifier> result = new ArrayList<>();
        for (Identifier id : identifiers) {
            if (types.contains(id.getType())) {
                result.add(id);
            }
        }
        return okResult(result);
    }

}
