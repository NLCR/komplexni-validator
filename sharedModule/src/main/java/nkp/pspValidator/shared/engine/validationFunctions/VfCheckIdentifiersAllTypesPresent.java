package nkp.pspValidator.shared.engine.validationFunctions;

import nkp.pspValidator.shared.engine.Engine;
import nkp.pspValidator.shared.engine.Level;
import nkp.pspValidator.shared.engine.ValueEvaluation;
import nkp.pspValidator.shared.engine.ValueType;
import nkp.pspValidator.shared.engine.exceptions.ContractException;
import nkp.pspValidator.shared.engine.params.ValueParam;
import nkp.pspValidator.shared.engine.types.Identifier;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by Martin Řehánek on 27.10.16.
 */
public class VfCheckIdentifiersAllTypesPresent extends ValidationFunction {

    public static final String PARAM_IDENTIFIER_LIST = "identifier_list";
    public static final String PARAM_IDENTIFIER_LIST_LIST = "identifier_list_list";
    public static final String PARAM_ID_TYPES = "id_types";
    public static final String PARAM_ID_LEVEL_NAME = "level_name";

    public VfCheckIdentifiersAllTypesPresent(String name, Engine engine) {
        super(name, engine, new Contract()
                .withValueParam(PARAM_IDENTIFIER_LIST, ValueType.IDENTIFIER_LIST, 0, null)
                .withValueParam(PARAM_IDENTIFIER_LIST_LIST, ValueType.IDENTIFIER_LIST_LIST, 0, null)
                .withValueParam(PARAM_ID_TYPES, ValueType.STRING_LIST, 1, 1)
                .withValueParam(PARAM_ID_LEVEL_NAME, ValueType.STRING, 1, 1)
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

            //id types
            ValueEvaluation idTypesParamEval = valueParams.getParams(PARAM_ID_TYPES).get(0).getEvaluation();
            List<String> types = (List<String>) idTypesParamEval.getData();
            if (types == null) {
                return invalidValueParamNull(PARAM_ID_TYPES, idTypesParamEval);
            }

            //level name
            ValueEvaluation paramLevelNameEval = valueParams.getParams(PARAM_ID_LEVEL_NAME).get(0).getEvaluation();
            String levelName = (String) paramLevelNameEval.getData();
            if (levelName == null) {
                return invalidValueParamNull(PARAM_ID_LEVEL_NAME, paramLevelNameEval);
            }


            return validate(levelName, types, idListList);
        } catch (ContractException e) {
            return invalidContractNotMet(e);
        } catch (Throwable e) {
            return invalidUnexpectedError(e);
        }
    }

    private ValidationResult validate(String levelName, List<String> typesExpected, List<List<Identifier>> idListList) {
        ValidationResult result = new ValidationResult();
        for (List<Identifier> idList : idListList) {
            //System.out.println(Utils.listToString(idList));
            Set<String> typesFound = new HashSet<>();
            for (Identifier id : idList) {
                typesFound.add(id.getType());
            }
            for (String typeExpected : typesExpected) {
                if (!typesFound.contains(typeExpected)) {
                    result.addError(invalid(Level.ERROR, "nenalezen identifikátor typu '%s' pro úroveň %s", typeExpected, levelName));
                }
            }
        }
        return result;
    }
}
