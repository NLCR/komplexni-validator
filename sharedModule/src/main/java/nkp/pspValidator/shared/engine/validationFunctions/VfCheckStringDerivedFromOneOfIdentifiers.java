package nkp.pspValidator.shared.engine.validationFunctions;

import nkp.pspValidator.shared.engine.*;
import nkp.pspValidator.shared.engine.exceptions.ContractException;
import nkp.pspValidator.shared.engine.types.Identifier;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by Martin Řehánek on 27.10.16.
 */
public class VfCheckStringDerivedFromOneOfIdentifiers extends ValidationFunction {

    public static final String PARAM_STRING = "string";
    public static final String PARAM_IDENTIFIERS = "identifiers";
    public static final String PARAM_MIN_MATCH_LENGTH = "min_match_length";
    public static final String PARAM_ID_PREFIXES = "id_prefixes";

    public VfCheckStringDerivedFromOneOfIdentifiers(String name, Engine engine) {
        super(name, engine, new Contract()
                .withValueParam(PARAM_STRING, ValueType.STRING, 1, 1)
                .withValueParam(PARAM_MIN_MATCH_LENGTH, ValueType.INTEGER, 1, 1)
                .withValueParam(PARAM_IDENTIFIERS, ValueType.IDENTIFIER_LIST, 1, 1)
                .withValueParam(PARAM_ID_PREFIXES, ValueType.STRING_LIST, 1, 1)
        );
    }

    @Override
    public ValidationResult validate() {
        try {
            checkContractCompliance();
            ValueEvaluation paramStringEval = valueParams.getParams(PARAM_STRING).get(0).getEvaluation();
            String string = (String) paramStringEval.getData();
            if (string == null) {
                return invalidValueParamNull(PARAM_STRING, paramStringEval);
            }

            ValueEvaluation paramMinMatchLengthEVal = valueParams.getParams(PARAM_MIN_MATCH_LENGTH).get(0).getEvaluation();
            Integer minMatchLength = (Integer) paramMinMatchLengthEVal.getData();
            if (minMatchLength == null) {
                return invalidValueParamNull(PARAM_MIN_MATCH_LENGTH, paramMinMatchLengthEVal);
            }

            ValueEvaluation paramIdPrefixesEval = valueParams.getParams(PARAM_ID_PREFIXES).get(0).getEvaluation();
            List<String> idPrefixes = (List<String>) paramIdPrefixesEval.getData();
            if (idPrefixes == null) {
                return invalidValueParamNull(PARAM_ID_PREFIXES, paramIdPrefixesEval);
            }

            ValueEvaluation paramIdentifiersEval = valueParams.getParams(PARAM_IDENTIFIERS).get(0).getEvaluation();
            List<Identifier> identifiers = (List<Identifier>) paramIdentifiersEval.getData();
            if (idPrefixes == null) {
                return invalidValueParamNull(PARAM_IDENTIFIERS, paramIdentifiersEval);
            }

            return validate(string, minMatchLength, identifiers, idPrefixes);
        } catch (ContractException e) {
            return invalidContractNotMet(e);
        } catch (Throwable e) {
            return invalidUnexpectedError(e);
        }
    }

    protected ValidationResult validate(String string, Integer minMatchLength, List<Identifier> identifiers, List<String> idPrefixes) {
        Set<String> unprefixedIdValues = removePrefixesFromIdValues(identifiers, idPrefixes);
        for (String idValue : unprefixedIdValues) {
            int longestCommonSubstringLength = Utils.getLongestCommonSubstringLength(idValue, string);
            if (longestCommonSubstringLength >= minMatchLength) {
                //System.out.println(String.format("match: %s, %s", string, idValue));
                return new ValidationResult();
            }
        }
        return singlErrorResult(invalid(Level.WARNING,
                "nenalezena shoda pro žádný z %d nalezených identifikátorů (%s)",
                identifiers.size(), buildIdTypeList(identifiers)));
    }

    private String buildIdTypeList(List<Identifier> identifiers) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < identifiers.size(); i++) {
            if (i != 0) {
                builder.append(", ");
            }
            Identifier id = identifiers.get(i);
            builder.append(id.getType() + ":" + id.getValue());
        }
        return builder.toString();
    }

    private Set<String> removePrefixesFromIdValues(List<Identifier> identifiers, List<String> idPrefixes) {
        Set<String> result = new HashSet<>(identifiers.size());
        for (Identifier identifier : identifiers) {
            result.add(removePrefixes(identifier.getValue(), idPrefixes));
        }
        return result;
    }

    private String removePrefixes(String value, List<String> prefixes) {
        if (value == null || value.isEmpty()) {
            return value;
        } else {
            for (String prefix : prefixes) {
                if (value.startsWith(prefix)) {
                    return value.substring(prefix.length());
                }
            }
            return value;
        }
    }


}
