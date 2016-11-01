package rzehan.shared.engine.validationFunctions;

import rzehan.shared.engine.Engine;
import rzehan.shared.engine.ValueEvaluation;
import rzehan.shared.engine.ValueType;
import rzehan.shared.engine.exceptions.ContractException;
import rzehan.shared.engine.exceptions.EmptyParamEvaluationException;
import rzehan.shared.engine.params.ValueParam;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


/**
 * Created by martin on 27.10.16.
 */
public class VfCheckNoOtherFilesInDir extends ValidationFunction {

    public static final String PARAM_ROOT_DIR = "root_dir";
    public static final String PARAM_FILE = "file";
    public static final String PARAM_FILES = "files";


    public VfCheckNoOtherFilesInDir(Engine engine) {
        super(engine, new Contract()
                .withValueParam(PARAM_ROOT_DIR, ValueType.FILE, 1, 1)
                .withValueParam(PARAM_FILE, ValueType.FILE, 0, null)
                .withValueParam(PARAM_FILES, ValueType.FILE_LIST, 0, null)
        );
    }

    @Override
    public String getName() {
        return "checkNoOtherFilesInDir";
    }

    @Override
    public ValidationResult validate() {
        try {
            checkContractCompliance();
        } catch (ContractException e) {
            return invalidContractNotMet(e);
        }

        ValueEvaluation paramRootDir = valueParams.getParams(PARAM_ROOT_DIR).get(0).getEvaluation();
        File rootDir = (File) paramRootDir.getData();
        if (rootDir == null) {
            return invalidValueParamNull(PARAM_ROOT_DIR, paramRootDir);
        } else if (!rootDir.isDirectory()) {
            return invalidFileIsNotDir(rootDir);
        } else if (!rootDir.canRead()) {
            return invalidCannotReadDir(rootDir);
        }

        List<File> filesInDir = listAbsoluteFiles(rootDir);
        try {
            Set<File> filesExpected = mergeAbsolutFilesFromParams();
            for (File file : filesInDir) {
                if (!filesExpected.contains(file)) {
                    return invalid(String.format("nalezen nečekaný soubor %s v adresáři %s", file.getName(), rootDir.getAbsolutePath()));
                }
            }
        } catch (EmptyParamEvaluationException e) {
            return invalidValueParamNull(e.getParamName(), e.getEvaluation());
        }

        return valid();
    }

    private List<File> listAbsoluteFiles(File rootDir) {
        File[] files = rootDir.listFiles();
        List<File> result = new ArrayList<>(files.length);
        for (File file : files) {
            result.add(file.getAbsoluteFile());
        }
        return result;
    }

    private Set<File> mergeAbsolutFilesFromParams() throws EmptyParamEvaluationException {
        Set<File> result = new HashSet<>();
        List<ValueParam> fileParams = valueParams.getParams(PARAM_FILE);
        for (ValueParam param : fileParams) {
            ValueEvaluation evaluation = param.getEvaluation();
            File file = (File) evaluation.getData();
            if (file == null) {
                throw new EmptyParamEvaluationException(PARAM_FILE, evaluation);
            }
            result.add(file.getAbsoluteFile());
        }
        List<ValueParam> filesParams = valueParams.getParams(PARAM_FILES);
        for (ValueParam param : filesParams) {
            ValueEvaluation evaluation = param.getEvaluation();
            List<File> files = (List<File>) evaluation.getData();
            if (files == null) {
                throw new EmptyParamEvaluationException(PARAM_FILES, evaluation);
            }
            for (File file : files) {
                result.add(file.getAbsoluteFile());
            }
        }
        return result;
    }

}
