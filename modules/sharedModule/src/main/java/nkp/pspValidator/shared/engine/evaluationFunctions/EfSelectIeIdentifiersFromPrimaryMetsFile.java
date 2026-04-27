package nkp.pspValidator.shared.engine.evaluationFunctions;

import nkp.pspValidator.shared.engine.Engine;
import nkp.pspValidator.shared.engine.ValueEvaluation;
import nkp.pspValidator.shared.engine.ValueType;
import nkp.pspValidator.shared.engine.exceptions.ContractException;
import nkp.pspValidator.shared.engine.params.ValueParam;
import nkp.pspValidator.shared.engine.types.Identifier;

import java.util.ArrayList;
import java.util.List;

public class EfSelectIeIdentifiersFromPrimaryMetsFile extends EvaluationFunction {

    public static final String PARAM_IDENTIFIER_LIST_LIST = "mets_identifier_list_list";
    public static final String PARAM_IDENTIFIER_LIST_LIST_2 = "mets_identifier_list_list_2";

    public EfSelectIeIdentifiersFromPrimaryMetsFile(String name, Engine engine) {
        super(name, engine, new Contract()
                .withReturnType(ValueType.IDENTIFIER_LIST)
                .withValueParam(PARAM_IDENTIFIER_LIST_LIST, ValueType.IDENTIFIER_LIST_LIST, 1, 1)
                .withValueParam(PARAM_IDENTIFIER_LIST_LIST_2, ValueType.IDENTIFIER_LIST_LIST, 0, 1)
        );
    }

    @Override
    public ValueEvaluation evaluate() {
        try {
            checkContractCompliance();

            List<List<Identifier>> modsIdListList1 = new ArrayList<>();
            //list of lists of id
            List<ValueParam> modsIdListList1Params = valueParams.getParams(PARAM_IDENTIFIER_LIST_LIST);
            for (ValueParam param : modsIdListList1Params) {
                ValueEvaluation eval = param.getEvaluation();
                List<List<Identifier>> listList = (List<List<Identifier>>) eval.getData();
                if (listList == null) {
                    return errorResultParamNull(PARAM_IDENTIFIER_LIST_LIST, eval);
                } else {
                    modsIdListList1.addAll(listList);
                }
            }

            List<List<Identifier>> modsIidListList2 = new ArrayList<>();
            //list of lists of id (second)
            List<ValueParam> modsIdListListParams2 = valueParams.getParams(PARAM_IDENTIFIER_LIST_LIST_2);
            for (ValueParam param : modsIdListListParams2) {
                ValueEvaluation eval = param.getEvaluation();
                List<List<Identifier>> listList = (List<List<Identifier>>) eval.getData();
                if (listList == null) {
                    return errorResultParamNull(PARAM_IDENTIFIER_LIST_LIST_2, eval);
                } else {
                    modsIidListList2.addAll(listList);
                }
            }

            return evaluate(modsIdListList1, modsIidListList2);
        } catch (ContractException e) {
            return errorResultContractNotMet(e);
        } catch (Throwable e) {
            return errorResultUnexpectedError(e);
        }
    }

    private ValueEvaluation evaluate(List<List<Identifier>> idListList, List<List<Identifier>> idListList2) {
        if (idListList != null && !idListList.isEmpty()) {
            return okResult(idListList.get(0));
        } else if (idListList2 != null && !idListList2.isEmpty()) {
            return okResult(idListList2.get(0));
        } else {
            return errorResult("nebyly nalezeny žádné identifikátory intelektuální entity z MODS");
        }
    }
}
