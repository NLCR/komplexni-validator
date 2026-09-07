package nkp.pspValidator.shared.engine.evaluationFunctions;

import nkp.pspValidator.shared.engine.Engine;
import nkp.pspValidator.shared.engine.ValueEvaluation;
import nkp.pspValidator.shared.engine.ValueType;
import nkp.pspValidator.shared.engine.exceptions.ContractException;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Seznam souboru poskytnuty volajicim programem pod jednim id (napr. ALTO_XSD_FILES: vsechna XSD pro ruzne verze ALTO
 * v adresari xsd/ dane fDMF). Obdoba getProvidedFile pro typ FILE_LIST.
 */
public class EfGetProvidedFiles extends EvaluationFunction {

    private static final String PARAM_FILES_ID = "files_id";

    public EfGetProvidedFiles(String name, Engine engine) {
        super(name, engine, new Contract()
                .withReturnType(ValueType.FILE_LIST)
                .withValueParam(PARAM_FILES_ID, ValueType.STRING, 1, 1));
    }

    @Override
    public ValueEvaluation evaluate() {
        try {
            checkContractCompliance();

            ValueEvaluation paramFilesId = valueParams.getParams(PARAM_FILES_ID).get(0).getEvaluation();
            String filesId = (String) paramFilesId.getData();
            if (filesId == null) {
                return errorResultParamNull(PARAM_FILES_ID, paramFilesId);
            } else if (filesId.isEmpty()) {
                return errorResult(String.format("hodnota parametru %s je prázdná", PARAM_FILES_ID));
            }

            List<File> files = engine.getProvidedVarsManager().getProvidedFileList(filesId);
            if (files == null) {
                return errorResult(String.format("seznam souborů s id %s není poskytován", filesId));
            } else {
                return okResult(new ArrayList<>(files));
            }
        } catch (ContractException e) {
            return errorResultContractNotMet(e);
        } catch (Throwable e) {
            return errorResultUnexpectedError(e);
        }
    }

}
