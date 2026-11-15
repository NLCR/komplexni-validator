package nkp.pspValidator.shared.engine.evaluationFunctions;

import nkp.pspValidator.shared.engine.Engine;
import nkp.pspValidator.shared.engine.ValueEvaluation;
import nkp.pspValidator.shared.engine.ValueType;
import nkp.pspValidator.shared.engine.exceptions.ContractException;
import nkp.pspValidator.shared.engine.params.ValueParam;

import java.io.File;
import java.util.List;

/**
 * Vrati true, kdyz aspon jeden ze seznamu souboru neni prazdny; seznam null (napr. nevyhodnotitelny) se bere jako
 * prazdny. Pouziti: podminena povinnost u nepovinnych adresaru, napr. PREMIS Object pro ALTO je povinny jen tehdy,
 * kdyz balicek ALTO obsahuje (OCR u zvuku je nepovinne).
 */
public class EfIsFileListNonEmpty extends EvaluationFunction {

    private static final String PARAM_FILE_LIST = "files";

    public EfIsFileListNonEmpty(String name, Engine engine) {
        super(name, engine, new Contract()
                .withReturnType(ValueType.BOOLEAN)
                .withValueParam(PARAM_FILE_LIST, ValueType.FILE_LIST, 1, null)
        );
    }

    @Override
    public ValueEvaluation evaluate() {
        try {
            checkContractCompliance();
            for (ValueParam param : valueParams.getParams(PARAM_FILE_LIST)) {
                List<File> files = (List<File>) param.getEvaluation().getData();
                if (files != null && !files.isEmpty()) {
                    return okResult(true);
                }
            }
            return okResult(false);
        } catch (ContractException e) {
            return errorResultContractNotMet(e);
        } catch (Throwable e) {
            return errorResultUnexpectedError(e);
        }
    }
}
