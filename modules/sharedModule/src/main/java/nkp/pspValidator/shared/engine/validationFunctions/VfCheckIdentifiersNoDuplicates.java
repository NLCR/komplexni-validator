package nkp.pspValidator.shared.engine.validationFunctions;

import nkp.pspValidator.shared.engine.Engine;
import nkp.pspValidator.shared.engine.Level;
import nkp.pspValidator.shared.engine.ValueEvaluation;
import nkp.pspValidator.shared.engine.ValueType;
import nkp.pspValidator.shared.engine.exceptions.ContractException;
import nkp.pspValidator.shared.engine.params.ValueParam;
import nkp.pspValidator.shared.engine.types.Identifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Martin Řehánek on 27.10.16.
 */
public class VfCheckIdentifiersNoDuplicates extends ValidationFunction {

    public static final String PARAM_IDENTIFIER_LIST = "identifier_list";
    public static final String PARAM_IDENTIFIER_LIST_LIST = "identifier_list_list";
    public static final String PARAM_IGNORED_TYPE_LIST = "ignored_type_list";

    public VfCheckIdentifiersNoDuplicates(String name, Engine engine) {
        super(name, engine, new Contract()
                .withValueParam(PARAM_IDENTIFIER_LIST, ValueType.IDENTIFIER_LIST, 0, null)
                .withValueParam(PARAM_IDENTIFIER_LIST_LIST, ValueType.IDENTIFIER_LIST_LIST, 0, null)
                .withValueParam(PARAM_IGNORED_TYPE_LIST, ValueType.STRING_LIST, 0, 1)
        );
    }

    @Override
    public ValidationResult validate() {
        try {
            checkContractCompliance();

            List<List<Identifier>> idListList = new ArrayList<>();
            //just list of id  - but possibly multiple param  occurence
            List<ValueParam> idListParams = valueParams.getParams(PARAM_IDENTIFIER_LIST);
            for (ValueParam param : idListParams) {
                ValueEvaluation eval = param.getEvaluation();
                List<Identifier> list = (List<Identifier>) eval.getData();
                if (list == null) {
                    return invalidValueParamNull(PARAM_IDENTIFIER_LIST, eval);
                } else {
                    idListList.add(list);
                }
            }
            //list of lists of id
            List<ValueParam> idListListParams = valueParams.getParams(PARAM_IDENTIFIER_LIST_LIST);
            for (ValueParam param : idListListParams) {
                ValueEvaluation eval = param.getEvaluation();
                List<List<Identifier>> listList = (List<List<Identifier>>) eval.getData();
                if (listList == null) {
                    return invalidValueParamNull(PARAM_IDENTIFIER_LIST_LIST, eval);
                } else {
                    idListList.addAll(listList);
                }
            }

            //ignored types
            List<String> ignoredTypes = new ArrayList<>();
            List<ValueParam> ignoredTypesParam = valueParams.getParams(PARAM_IGNORED_TYPE_LIST);
            if (!ignoredTypesParam.isEmpty()) {
                ValueEvaluation eval = ignoredTypesParam.get(0).getEvaluation();
                ignoredTypes = (List<String>) eval.getData();
                if (ignoredTypes == null) {
                    return invalidValueParamNull(PARAM_IGNORED_TYPE_LIST, eval);
                }
            }

            return validate(idListList, ignoredTypes);
        } catch (ContractException e) {
            return invalidContractNotMet(e);
        } catch (Throwable e) {
            return invalidUnexpectedError(e);
        }
    }

    private ValidationResult validate(List<List<Identifier>> idListList, List<String> ignoredTypes) {
        ValidationResult result = new ValidationResult();
        for (List<Identifier> idList : idListList) {
            //System.out.println(Utils.listToString(idList));
            Map<String, String> map = new HashMap<>();
            for (Identifier id : idList) {
                if (!ignoredTypes.contains(id.getType())) {
                    String valueFound = map.get(id.getType());
                    if (valueFound == null) {
                        map.put(id.getType(), id.getValue());
                    } else if (valueFound.equals(id.getValue())) {
                        result.addError(invalid(Level.WARNING, null, "seznam obsahuje duplikovaný identifikátor '%s'", id.toString()));
                    }
                }
            }
        }
        return result;
    }
}
