package nkp.pspValidator.shared.engine.validationFunctions;


import nkp.pspValidator.shared.engine.Engine;
import nkp.pspValidator.shared.engine.Level;
import nkp.pspValidator.shared.engine.ValueEvaluation;
import nkp.pspValidator.shared.engine.ValueType;
import nkp.pspValidator.shared.engine.exceptions.ContractException;
import nkp.pspValidator.shared.engine.types.Identifier;
import nkp.pspValidator.shared.engine.utils.UrnNbnResolverChecker;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.List;

/**
 * Created by Martin Řehánek
 */
public class VfCheckUrnNbnIdentifiersRegistered extends ValidationFunction {

    public static final String PARAM_IDENTIFIER_LIST = "identifier_list";

    public VfCheckUrnNbnIdentifiersRegistered(String name, Engine engine) {
        super(name, engine, new Contract()
                .withValueParam(PARAM_IDENTIFIER_LIST, ValueType.IDENTIFIER_LIST, 1, 1)
        );
    }

    @Override
    public ValidationResult validate() {
        try {
            checkContractCompliance();

            ValueEvaluation paramEvaluation = valueParams.getParams(PARAM_IDENTIFIER_LIST).get(0).getEvaluation();
            List<Identifier> identifiers = (List<Identifier>) paramEvaluation.getData();
            if (identifiers == null) {
                return invalidValueParamNull(PARAM_IDENTIFIER_LIST, paramEvaluation);
            }
            return validate(identifiers);
        } catch (ContractException e) {
            return invalidContractNotMet(e);
        } catch (Throwable e) {
            return invalidUnexpectedError(e);
        }
    }

    private ValidationResult validate(List<Identifier> identifiers) {
        try {
            UrnNbnResolverChecker checker = new UrnNbnResolverChecker();
            ValidationResult result = new ValidationResult();
            for (Identifier identifier : identifiers) {
                if (identifier.getType().equals("urnnbn")) {
                    checkUrn(checker, result, identifier.getValue());
                }
            }
            return result;
        } catch (NoSuchAlgorithmException e) {
            return invalidUnexpectedError(e);
        } catch (KeyManagementException e) {
            return invalidUnexpectedError(e);
        }
    }

    private void checkUrn(UrnNbnResolverChecker checker, ValidationResult result, String urnNbn) {
        try {
            checker.check(urnNbn);
        } catch (IOException e) {
            result.addError(Level.ERROR, "chyba připojení při zpracování identifikátoru %s: %s", urnNbn, e.getMessage());
            e.printStackTrace();
        } catch (UrnNbnResolverChecker.ResolverError e) {
            result.addError(Level.ERROR, e.getMessage());
        } catch (UrnNbnResolverChecker.ResolverWarning e) {
            result.addError(Level.WARNING, e.getMessage());
        }
    }

}
