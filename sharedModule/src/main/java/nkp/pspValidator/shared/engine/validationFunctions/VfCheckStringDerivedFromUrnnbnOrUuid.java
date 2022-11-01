package nkp.pspValidator.shared.engine.validationFunctions;

import nkp.pspValidator.shared.engine.Engine;
import nkp.pspValidator.shared.engine.Level;
import nkp.pspValidator.shared.engine.ValueEvaluation;
import nkp.pspValidator.shared.engine.ValueType;
import nkp.pspValidator.shared.engine.exceptions.ContractException;
import nkp.pspValidator.shared.engine.types.Identifier;

import java.util.List;

public class VfCheckStringDerivedFromUrnnbnOrUuid extends ValidationFunction {

    public static final String PARAM_STRING = "string";

    public static final String PARAM_IDENTIFIERS = "identifiers";


    public VfCheckStringDerivedFromUrnnbnOrUuid(String name, Engine engine) {
        super(name, engine, new Contract()
                .withValueParam(PARAM_STRING, ValueType.STRING, 1, 1)
                .withValueParam(PARAM_IDENTIFIERS, ValueType.IDENTIFIER_LIST, 1, 1)
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

            ValueEvaluation paramIdentifiersEval = valueParams.getParams(PARAM_IDENTIFIERS).get(0).getEvaluation();
            List<Identifier> identifiers = (List<Identifier>) paramIdentifiersEval.getData();
            String uuid = null;
            String urnnbn = null;
            for (Identifier id : identifiers) {
                if ("uuid".equals(id.getType())) {
                    uuid = id.getValue();
                }
                if ("urnnbn".equals(id.getType())) {
                    urnnbn = id.getValue();
                }
            }
            return validate(string, uuid, urnnbn);
        } catch (ContractException e) {
            return invalidContractNotMet(e);
        } catch (Throwable e) {
            return invalidUnexpectedError(e);
        }
    }

    ValidationResult validate(String string, String uuid, String urnnbn) {
        if (uuid == null && urnnbn == null) {
            return singlErrorResult(invalid(Level.ERROR, "nenalezen žádný z identifikátorů UUID ani URN:NBN"));
        }
        if (uuid != null && !uuid.matches("uuid:[a-f0-9]{8}(-[a-f0-9]{4}){3}-[a-f0-9]{12}")) {
            return singlErrorResult(invalid(Level.ERROR, "hodnota '%s' není ve formátu UUID", uuid));
        }
        if (urnnbn != null && !urnnbn.matches("urn:nbn:cz:[A-Za-z0-9]{2,6}-[A-Za-z0-9]{6}")) {
            return singlErrorResult(invalid(Level.ERROR, "hodnota '%s' není ve formátu URN:NBN", urnnbn));
        }
        if (uuid != null && stringIsDerivedFromUUid(string, uuid)) {
            return ValidationResult.ok();
        }
        if (urnnbn != null && stringIsDerivedFromUrnNbn(string, urnnbn)) {
            return ValidationResult.ok();
        }
        return singlErrorResult(invalid(Level.ERROR, "řetězec '%s' není odvozen od UUID ani URN:NBN", string));
    }

    private boolean stringIsDerivedFromUrnNbn(String string, String urnnbn) {
        return string.equals(urnnbn.substring("urn:nbn:cz:".length()));
    }

    private boolean stringIsDerivedFromUUid(String string, String uuid) {
        return string.equals(uuid.substring("uuid:".length()));
    }
}
