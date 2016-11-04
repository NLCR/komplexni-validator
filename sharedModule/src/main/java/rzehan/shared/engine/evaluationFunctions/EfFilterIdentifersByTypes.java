package rzehan.shared.engine.evaluationFunctions;

import rzehan.shared.engine.Engine;
import rzehan.shared.engine.ValueEvaluation;
import rzehan.shared.engine.ValueType;
import rzehan.shared.engine.exceptions.ContractException;
import rzehan.shared.engine.types.Identifier;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by martin on 21.10.16.
 */
public class EfFilterIdentifersByTypes extends EvaluationFunction {

    private static final String PARAM_IDENTIFIERS = "identifiers";
    private static final String PARAM_TYPES = "types";

    public EfFilterIdentifersByTypes(Engine engine) {
        super(engine, new Contract()
                .withReturnType(ValueType.IDENTIFIER_LIST)
                .withValueParam(PARAM_IDENTIFIERS, ValueType.IDENTIFIER_LIST, 1, 1)
                .withValueParam(PARAM_TYPES, ValueType.STRING_LIST, 1, 1)
        );
    }

    @Override
    public String getName() {
        return "filterIdentifersByTypes";
    }

    @Override
    public ValueEvaluation evaluate() {
        try {
            checkContractCompliance();
        } catch (ContractException e) {
            return errorResultContractNotMet(e);
        }

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
