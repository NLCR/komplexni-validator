package nkp.pspValidator.shared.engine.validationFunctions;


import nkp.pspValidator.shared.engine.Engine;
import nkp.pspValidator.shared.engine.Level;
import nkp.pspValidator.shared.engine.ValueEvaluation;
import nkp.pspValidator.shared.engine.ValueType;
import nkp.pspValidator.shared.engine.exceptions.ContractException;
import nkp.pspValidator.shared.engine.exceptions.InvalidXPathExpressionException;
import nkp.pspValidator.shared.engine.params.ValueParam;
import nkp.pspValidator.shared.engine.types.Identifier;
import nkp.pspValidator.shared.engine.utils.UrnNbnResolverChecker;

import javax.xml.xpath.XPathExpressionException;
import java.io.File;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.List;

/**
 * Created by Martin Řehánek
 */
public class VfCheckUrnNbnIdentifiersRegistered extends ValidationFunction {

    public static final String PARAM_IDENTIFIER_LIST = "identifier_list";
    /**
     * Systemova vlastnost: pri hodnote true se Resolver URN:NBN nevola a pravidlo skonci jedinou INFO hlaskou
     * "kontrola ... preskocena". Pouziva ji build.gradle pro testy (beh bez site, vysledek nezavisi na stavu cizich
     * identifikatoru v produkcnim Resolveru); jinak lze zadat -Dkv.skipUrnNbnResolver=true pri spusteni JVM.
     */
    public static final String PROP_SKIP_RESOLVER = "kv.skipUrnNbnResolver";
    private static final String PARAM_METS_FILE = "mets_file";

    public VfCheckUrnNbnIdentifiersRegistered(String name, Engine engine) {
        super(name, engine, new Contract()
                .withValueParam(PARAM_IDENTIFIER_LIST, ValueType.IDENTIFIER_LIST, 1, 1)
                .withValueParam(PARAM_METS_FILE, ValueType.FILE, 0, 1)
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
            File metsFile = null;
            List<ValueParam> paramsMetsFile = valueParams.getParams(PARAM_METS_FILE);
            if (paramsMetsFile.size() != 0) {
                ValueEvaluation paramMetsFileEval = paramsMetsFile.get(0).getEvaluation();
                metsFile = (File) paramMetsFileEval.getData();
            }
            return validate(identifiers, metsFile);
        } catch (ContractException e) {
            return invalidContractNotMet(e);
        } catch (Throwable e) {
            return invalidUnexpectedError(e);
        }
    }

    private ValidationResult validate(List<Identifier> identifiers, File metsFile) {
        if (Boolean.getBoolean(PROP_SKIP_RESOLVER)) {
            return skipped(identifiers);
        }
        try {
            UrnNbnResolverChecker checker = new UrnNbnResolverChecker(metsFile);
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

    private ValidationResult skipped(List<Identifier> identifiers) {
        ValidationResult result = new ValidationResult();
        long urnCount = identifiers.stream().filter(id -> id.getType().equals("urnnbn")).count();
        if (urnCount > 0) {
            result.addError(new ValidationProblem(Level.INFO, String.format("kontrola registrace URN:NBN v Resolveru přeskočena (systémová vlastnost %s=true), identifikátorů urn:nbn: %d", PROP_SKIP_RESOLVER, urnCount))
                    .withSimpleMessage("kontrola registrace URN:NBN v Resolveru přeskočena"));
        }
        return result;
    }

    private void checkUrn(UrnNbnResolverChecker checker, ValidationResult result, String urnNbn) {
        try {
            checker.check(urnNbn);
        } catch (IOException e) {
            result.addError(new ValidationProblem(Level.ERROR, String.format("chyba připojení při zpracování identifikátoru %s: %s", urnNbn, e.getMessage()))
                    .withSimpleMessage("chyba připojení při zpracování identifikátoru urn:nbn")
                    .withReferencedValue(urnNbn)
                    .withErrorDetails(e.getMessage())
            );
        } catch (UrnNbnResolverChecker.ResolverError e) {
            result.addError(new ValidationProblem(Level.ERROR, String.format("chyba od resolveru URN:NBN při zpracování identifikátoru %s: %s", urnNbn, e.getMessage()))
                    .withSimpleMessage("chyba od resolveru URN:NBN")
                    .withReferencedValue(urnNbn)
                    .withErrorDetails(e.getMessage())
            );
        } catch (UrnNbnResolverChecker.ResolverWarning e) {
            result.addError(new ValidationProblem(Level.WARNING, String.format("varování od resolveru URN:NBN při zpracování identifikátoru %s: %s", urnNbn, e.getMessage()))
                    .withSimpleMessage("varování od resolveru URN:NBN")
                    .withReferencedValue(urnNbn)
                    .withErrorDetails(e.getMessage())
            );
        } catch (XPathExpressionException e) {
            result.addError(new ValidationProblem(Level.ERROR, String.format("chyba při zpracování XPath výrazu při zpracování identifikátoru %s: %s", urnNbn, e.getMessage()))
                    .withSimpleMessage("chyba při zpracování XPath výrazu")
                    .withReferencedValue(urnNbn)
                    .withErrorDetails(e.getMessage())
            );
        } catch (InvalidXPathExpressionException e) {
            result.addError(new ValidationProblem(Level.ERROR, String.format("neplatný XPath výraz při zpracování identifikátoru %s: %s", urnNbn, e.getMessage()))
                    .withSimpleMessage("neplatný XPath výraz")
                    .withReferencedValue(urnNbn)
                    .withErrorDetails(e.getMessage())
            );
        }
    }

}
