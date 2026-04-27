package nkp.pspValidator.shared.engine.validationFunctions;

import nkp.pspValidator.shared.engine.Engine;
import nkp.pspValidator.shared.engine.Level;
import nkp.pspValidator.shared.engine.ValueEvaluation;
import nkp.pspValidator.shared.engine.ValueType;
import nkp.pspValidator.shared.engine.exceptions.ContractException;
import nkp.pspValidator.shared.engine.params.ValueParam;
import nkp.pspValidator.shared.engine.types.Identifier;

import java.util.ArrayList;
import java.util.List;

public class VfCheckInfoFileIdentifiersMatchTopLevelModsIdentifiers extends ValidationFunction {

    public static final String PARAM_INFO_IDENTIFIER_LIST = "info_file_identifier_list";
    public static final String PARAM_IDENTIFIER_LIST_LIST = "mets_identifier_list_list";
    public static final String PARAM_IDENTIFIER_LIST_LIST_2 = "mets_identifier_list_list_2";

    public VfCheckInfoFileIdentifiersMatchTopLevelModsIdentifiers(String name, Engine engine) {
        super(name, engine, new Contract()
                .withValueParam(PARAM_INFO_IDENTIFIER_LIST, ValueType.IDENTIFIER_LIST, 1, 1)
                .withValueParam(PARAM_IDENTIFIER_LIST_LIST, ValueType.IDENTIFIER_LIST_LIST, 1, 1)
                .withValueParam(PARAM_IDENTIFIER_LIST_LIST_2, ValueType.IDENTIFIER_LIST_LIST, 0, 1)
        );
    }

    @Override
    public ValidationResult validate() {
        try {
            checkContractCompliance();

            List<Identifier> infoFileIdList = new ArrayList<>();
            List<ValueParam> idListParams = valueParams.getParams(PARAM_INFO_IDENTIFIER_LIST);
            for (ValueParam param : idListParams) {
                ValueEvaluation eval = param.getEvaluation();
                List<Identifier> list = (List<Identifier>) eval.getData();
                if (list == null) {
                    return invalidValueParamNull(PARAM_INFO_IDENTIFIER_LIST, eval);
                } else {
                    infoFileIdList.addAll(list);
                }
            }

            List<List<Identifier>> modsIdListList1 = new ArrayList<>();
            //list of lists of id
            List<ValueParam> modsIdListList1Params = valueParams.getParams(PARAM_IDENTIFIER_LIST_LIST);
            for (ValueParam param : modsIdListList1Params) {
                ValueEvaluation eval = param.getEvaluation();
                List<List<Identifier>> listList = (List<List<Identifier>>) eval.getData();
                if (listList == null) {
                    return invalidValueParamNull(PARAM_IDENTIFIER_LIST_LIST, eval);
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
                    return invalidValueParamNull(PARAM_IDENTIFIER_LIST_LIST_2, eval);
                } else {
                    modsIidListList2.addAll(listList);
                }
            }

            return validate(infoFileIdList, modsIdListList1, modsIidListList2);
        } catch (ContractException e) {
            return invalidContractNotMet(e);
        } catch (Throwable e) {
            return invalidUnexpectedError(e);
        }
    }

    private ValidationResult validate(List<Identifier> infoFileIdList, List<List<Identifier>> idListList, List<List<Identifier>> idListList2) {
        //identifiers from MODS
        //prefer idListList, then idListList2 and use first list available
        List<Identifier> metsTopLevelIdList = null;
        if (idListList != null && !idListList.isEmpty()) {
            metsTopLevelIdList = idListList.get(0);
        } else if (idListList2 != null && !idListList2.isEmpty()) {
            metsTopLevelIdList = idListList2.get(0);
        } else {
            return singlErrorResult(
                    new ValidationProblem(Level.ERROR, String.format("nebyly nalezeny žádné identifikátory z MODS pro porovnání s identifikátory z info souboru"))
                            .withSimpleMessage("nenalezeny identifikátory z MODS pro porovnání s identifikátory z info souboru")
            );
        }
        return matchInfoIdsAndTopLevelModsIds(infoFileIdList, metsTopLevelIdList);
    }

    private ValidationResult matchInfoIdsAndTopLevelModsIds(List<Identifier> infoIds, List<Identifier> topLevelModsIds) {
        ValidationResult result = new ValidationResult();

        //check if everything in info_ids is present in mets-top-level_ids
        for (int i = 0; i < infoIds.size(); i++) {
            Identifier idFromInfo = infoIds.get(i);
            if (!listContainsItem(topLevelModsIds, idFromInfo)) {
                result.addError(new ValidationProblem(Level.ERROR, String.format("identifikátor z INFO souboru '%s' není přítomen mezi identifikátory z top-level MODS záznamu ", idFromInfo))
                        .withSimpleMessage("identifikátor z INFO souboru není přítomen mezi identifikátory z top-level MODS záznamu")
                        .withReferencedValue(idFromInfo.toString())
                );
            }
        }

        //check if everything in mets-top-level_ids is present in info_ids
        for (int i = 0; i < topLevelModsIds.size(); i++) {
            Identifier idFromMods = topLevelModsIds.get(i);
            if (!listContainsItem(infoIds, idFromMods)) {
                result.addError(new ValidationProblem(Level.ERROR, String.format("identifikátor z top-level MODS záznamu '%s' není přítomen mezi identifikátory z INFO souboru", idFromMods))
                        .withSimpleMessage("identifikátor z top-level MODS záznamu není přítomen mezi identifikátory z INFO souboru")
                        .withReferencedValue(idFromMods.toString())
                );
            }
        }

        return result;
    }

    private boolean listContainsItem(List<Identifier> list, Identifier item) {
        for (Identifier idInList : list) {
            if (idInList.equals(item)) {
                return true;
            }
        }
        return false;
    }

}
