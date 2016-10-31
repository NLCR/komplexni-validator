package rzehan.shared.engine.validationFunctions;

import rzehan.shared.engine.Engine;
import rzehan.shared.engine.ValueType;
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
        checkContractCompliance();
        File rootDir = (File) valueParams.getParams(PARAM_ROOT_DIR).get(0).getValue();
        if (rootDir == null) {
            return new ValidationResult(false).withMessage(String.format("hodnota parametru %s funkce %s je null", PARAM_ROOT_DIR, getName()));
        } else if (!rootDir.isDirectory()) {
            return new ValidationResult(false).withMessage(String.format("soubor %s není adresář", rootDir.getAbsoluteFile()));
        } else if (!rootDir.canRead()) {
            return new ValidationResult(false).withMessage(String.format("nelze číst soubory v adresáři %s", rootDir.getAbsoluteFile()));
        } else {
            List<File> filesInDir = listAbsoluteFiles(rootDir);
            Set<File> filesExpected = mergeAbsolutFilesFromParams();
            for (File file : filesInDir) {
                if (!filesExpected.contains(file)) {
                    return new ValidationResult(false).withMessage(String.format("nalezen nečekaný soubor %s v adresáři %s", file.getName(), rootDir.getAbsolutePath()));
                }
            }
            return new ValidationResult(true);
        }
    }

    private List<File> listAbsoluteFiles(File rootDir) {
        File[] files = rootDir.listFiles();
        List<File> result = new ArrayList<>(files.length);
        for (File file : files) {
            result.add(file.getAbsoluteFile());
        }
        return result;
    }

    private Set<File> mergeAbsolutFilesFromParams() {
        Set<File> result = new HashSet<>();
        List<ValueParam> fileParams = valueParams.getParams(PARAM_FILE);
        for (ValueParam param : fileParams) {
            File file = (File) param.getValue();
            result.add(file.getAbsoluteFile());
        }
        List<ValueParam> filesParams = valueParams.getParams(PARAM_FILES);
        for (ValueParam param : filesParams) {
            List<File> files = (List<File>) param.getValue();
            for (File file : files) {
                result.add(file.getAbsoluteFile());
            }
        }
        return result;
    }

}
