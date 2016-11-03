package rzehan.shared.engine.validationFunctions;

import rzehan.shared.engine.Engine;
import rzehan.shared.engine.ValueEvaluation;
import rzehan.shared.engine.ValueType;
import rzehan.shared.engine.exceptions.ContractException;
import rzehan.shared.engine.types.Identifier;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by martin on 27.10.16.
 */
public class VfCheckNoDuplicateIdentifiers extends ValidationFunction {

    public static final String PARAM_IDENTIFIERS = "identifiers";


    public VfCheckNoDuplicateIdentifiers(Engine engine) {
        super(engine, new Contract()
                .withValueParam(PARAM_IDENTIFIERS, ValueType.IDENTIFIER_LIST, 1, 1)
        );
    }

    @Override
    public String getName() {
        return "checkNoDuplicateIdentifiers";
    }

    @Override
    public ValidationResult validate() {
        try {
            checkContractCompliance();
        } catch (ContractException e) {
            return invalidContractNotMet(e);
        }

        ValueEvaluation param = valueParams.getParams(PARAM_IDENTIFIERS).get(0).getEvaluation();
        List<Identifier> identifiers = (List<Identifier>) param.getData();
        if (identifiers == null) {
            return invalidValueParamNull(PARAM_IDENTIFIERS, param);
        }
        Map<String, String> map = new HashMap<>();
        for (Identifier id : identifiers) {
            if (!map.containsKey(id.getType())) {
                map.put(id.getType(), id.getValue());
            } else {
                return invalid(String.format("seznam obsahuje více identifikátorů typu '%s'", id.getType()));
            }
        }
        return valid();
    }
}
