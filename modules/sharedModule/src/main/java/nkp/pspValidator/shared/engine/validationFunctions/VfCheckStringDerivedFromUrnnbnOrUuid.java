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
        ValidationResult result = new ValidationResult();
        if (uuid == null && urnnbn == null) {
            return singlErrorResult(invalid(Level.ERROR, "nenalezen žádný z identifikátorů UUID ani URN:NBN"));
        }
        if (uuid != null && uuid.startsWith("uuid:")) {
            result.addError(new ValidationProblem(Level.WARNING,
                    String.format("identifikátor UUID by měl být bez prefixu 'uuid:' (nalezeno '%s')", uuid))
                    .withSimpleMessage("identifikátor UUID by měl být bez prefixu 'uuid:'")
                    .withReferencedValue(uuid));
            uuid = uuid.substring("uuid:".length());
        }
        if (uuid != null && !uuid.matches("[a-f0-9]{8}(-[a-f0-9]{4}){3}-[a-f0-9]{12}")) {
            result.addError(new ValidationProblem(Level.ERROR, String.format("hodnota '%s' není ve formátu UUID", uuid))
                    .withSimpleMessage("hodnota není ve formátu UUID")
                    .withReferencedValue(uuid));
            uuid = null; //not using this incorrect uuid for further validation
        }
        if (urnnbn != null && !urnnbn.matches("urn:nbn:cz:[A-Za-z0-9]{2,6}-[A-Za-z0-9]{6}")) {
            result.addError(new ValidationProblem(Level.ERROR, String.format("hodnota '%s' není ve formátu URN:NBN", urnnbn))
                    .withSimpleMessage("hodnota není ve formátu URN:NBN")
                    .withReferencedValue(urnnbn));
            urnnbn = null; //not using this incorrect urnnbn for further validation
        }
        if (!stringIsDerivedFromUUid(string, uuid) && !stringIsDerivedFromUrnNbn(string, urnnbn)) {
            result.addError(new ValidationProblem(Level.ERROR, String.format("řetězec '%s' není odvozen od UUID ani URN:NBN intelektuální entity", string))
                    .withSimpleMessage("řetězec není odvozen od UUID ani URN:NBN intelektuální entity")
                    .withReferencedValue(string));
        }
        return result;
    }

    private boolean stringIsDerivedFromUrnNbn(String string, String urnnbn) {
        if (urnnbn == null) {
            return false;
        }
        return string.equals(urnnbn.substring("urn:nbn:cz:".length()));
    }

    private boolean stringIsDerivedFromUUid(String string, String uuidWithoutPrefix) {
        if (uuidWithoutPrefix == null) {
            return false;
        }
        //aktuálně odvození znamená přesný match na obsah UUID bez prefixu "uuid:"
        return string.equals(uuidWithoutPrefix);
    }
}
