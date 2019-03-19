package nkp.pspValidator.shared.engine.validationFunctions;

import nkp.pspValidator.shared.engine.Engine;
import nkp.pspValidator.shared.engine.Level;
import nkp.pspValidator.shared.engine.ValueEvaluation;
import nkp.pspValidator.shared.engine.ValueType;
import nkp.pspValidator.shared.engine.exceptions.ContractException;
import nkp.pspValidator.shared.engine.exceptions.EmptyParamEvaluationException;
import nkp.pspValidator.shared.engine.params.ValueParam;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


/**
 * Created by Martin Řehánek on 27.10.16.
 */
public class VfCheckNoOtherFilesInDir extends ValidationFunction {

    public static final String PARAM_ROOT_DIR = "root_dir";
    public static final String PARAM_FILE = "file";
    public static final String PARAM_FILES = "files";


    public VfCheckNoOtherFilesInDir(String name, Engine engine) {
        super(name, engine, new Contract()
                .withValueParam(PARAM_ROOT_DIR, ValueType.FILE, 1, 1)
                .withValueParam(PARAM_FILE, ValueType.FILE, 0, null)
                .withValueParam(PARAM_FILES, ValueType.FILE_LIST, 0, null)
        );
    }

    @Override
    public ValidationResult validate() {
        try {
            checkContractCompliance();

            ValueEvaluation paramRootDir = valueParams.getParams(PARAM_ROOT_DIR).get(0).getEvaluation();
            File rootDir = (File) paramRootDir.getData();
            if (rootDir == null) {
                //ignore
                //return invalidValueParamNull(PARAM_ROOT_DIR, paramRootDir);
                return new ValidationResult();
            } else if (!rootDir.isDirectory()) {
                return singlErrorResult(invalidFileIsNotDir(rootDir));
            } else if (!rootDir.canRead()) {
                return singlErrorResult(invalidCannotReadDir(rootDir));
            }

            try {
                Set<File> filesExpected = mergeAbsoluteFilesFromParamsIgnoreNulls();
                return validate(rootDir, filesExpected);
            } catch (EmptyParamEvaluationException e) {
                return invalidValueParamNull(e.getParamName(), e.getEvaluation());
            }
        } catch (ContractException e) {
            return invalidContractNotMet(e);
        } catch (Throwable e) {
            return invalidUnexpectedError(e);
        }
    }


    private Set<File> mergeAbsoluteFilesFromParamsIgnoreNulls() throws EmptyParamEvaluationException {
        Set<File> result = new HashSet<>();
        List<ValueParam> fileParams = valueParams.getParams(PARAM_FILE);
        for (ValueParam param : fileParams) {
            ValueEvaluation evaluation = param.getEvaluation();
            File file = (File) evaluation.getData();
            if (file == null) {
                //ignore
                //throw new EmptyParamEvaluationException(PARAM_FILE, evaluation);
            } else {
                result.add(file.getAbsoluteFile());
            }
        }
        List<ValueParam> filesParams = valueParams.getParams(PARAM_FILES);
        for (ValueParam param : filesParams) {
            ValueEvaluation evaluation = param.getEvaluation();
            List<File> files = (List<File>) evaluation.getData();
            if (files == null) {
                //ignore
                //throw new EmptyParamEvaluationException(PARAM_FILES, evaluation);
            } else {
                for (File file : files) {
                    result.add(file.getAbsoluteFile());
                }
            }
        }
        return result;
    }

    private ValidationResult validate(File rootDir, Set<File> filesExpected) {
        ValidationResult result = new ValidationResult();
        List<File> filesInDir = listAbsoluteFiles(rootDir);
        for (File file : filesInDir) {
            if (!filesExpected.contains(file)) {
                result.addError(invalid(Level.ERROR, "nalezen nečekaný soubor %s v adresáři %s", file.getName(), rootDir.getAbsolutePath()));
            }
        }
        return result;
    }

    private List<File> listAbsoluteFiles(File rootDir) {
        File[] files = rootDir.listFiles();
        List<File> result = new ArrayList<>(files.length);
        for (File file : files) {
            result.add(file.getAbsoluteFile());
        }
        return result;
    }

}
