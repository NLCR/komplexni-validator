package nkp.pspValidator.shared.engine.validationFunctions;

import nkp.pspValidator.shared.engine.*;
import nkp.pspValidator.shared.engine.exceptions.ContractException;
import nkp.pspValidator.shared.engine.exceptions.EmptyParamEvaluationException;
import nkp.pspValidator.shared.engine.exceptions.InvalidPathException;
import nkp.pspValidator.shared.engine.params.ValueParam;

import java.io.*;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


/**
 * Created by Martin Řehánek on 27.10.16.
 */
public class VfCheckChecksumFileAllPathsMatchFiles extends ValidationFunction {

    public static final String PARAM_CHECKSUM_FILE = "checksum_file";
    public static final String PARAM_FILE = "file";
    public static final String PARAM_FILES = "files";

    public VfCheckChecksumFileAllPathsMatchFiles(String name, Engine engine) {
        super(name, engine, new Contract()
                .withValueParam(PARAM_CHECKSUM_FILE, ValueType.FILE, 1, 1)
                .withValueParam(PARAM_FILE, ValueType.FILE, 0, null)
                .withValueParam(PARAM_FILES, ValueType.FILE_LIST, 0, null)
        );
    }

    @Override
    public ValidationResult validate() {
        try {
            checkContractCompliance();

            ValueEvaluation paramChecksumFile = valueParams.getParams(PARAM_CHECKSUM_FILE).get(0).getEvaluation();
            File checksumFile = (File) paramChecksumFile.getData();
            if (checksumFile == null) {
                return invalidValueParamNull(PARAM_CHECKSUM_FILE, paramChecksumFile);
            } else if (!checksumFile.exists()) {
                return singlErrorResult(invalidFileDoesNotExist(checksumFile));
            } else if (checksumFile.isDirectory()) {
                return singlErrorResult(invalidFileIsDir(checksumFile));
            }

            try {
                Set<File> files = mergeAbsolutFilesFromParams();
                return validate(checksumFile, files);
            } catch (EmptyParamEvaluationException e) {
                return invalidValueParamNull(e.getParamName(), e.getEvaluation());
            }
        } catch (ContractException e) {
            return invalidContractNotMet(e);
        } catch (Throwable e) {
            return invalidUnexpectedError(e);
        }
    }

    private Set<File> mergeAbsolutFilesFromParams() throws EmptyParamEvaluationException {
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

    private ValidationResult validate(File checksumFile, Set<File> filesFromParams) {
        ValidationResult result = new ValidationResult();

        File pspRootDir = checksumFile.getParentFile();
        if (!pspRootDir.exists()) {
            result.addError(invalidFileDoesNotExist(pspRootDir));
        } else if (!pspRootDir.isDirectory()) {
            result.addError(invalidFileIsNotDir(pspRootDir));
        } else {
            FileInputStream fis = null;
            BufferedReader br = null;
            try {
                fis = new FileInputStream(checksumFile);
                br = new BufferedReader(new InputStreamReader(fis));

                String line;
                Set<File> filesFromFile = new HashSet<>();
                while ((line = br.readLine()) != null) {
                    String[] parts = line.split("[ \\t]");//space or tabulator
                    if (parts.length == 1) {
                        result.addError(invalid(Level.ERROR, "chybí oddělovač (mezera/tabulátor) na řádku '%s'", line));
                    } else {
                        String hash = parts[0];
                        String filepath = parts[1];
                        try {
                            File file = Utils.buildAbsoluteFile(pspRootDir, filepath);
                            filesFromFile.add(file);
                        } catch (InvalidPathException e) {
                            result.addError(invalid(Level.ERROR, "cesta k souboru není zapsána korektně: '%s'", e.getPath()));
                        }
                    }
                }
                br.close();
                checkSetsAreSame(result, filesFromParams, filesFromFile);
            } catch (IOException e) {
                result.addError(invalid(Level.ERROR, "chyba při čtení souboru %s: %s", checksumFile.getAbsolutePath(), e.getMessage()));
                return result;
            } finally {
                try {
                    if (br != null) {
                        br.close();
                    }
                    if (fis != null) {
                        fis.close();
                    }
                } catch (IOException e) {
                    result.addError(invalid(e));
                } finally {
                    return result;
                }
            }
        }
        return result;
    }

    private void checkSetsAreSame(ValidationResult result, Set<File> filesFromParams, Set<File> filesFromFile) {
        for (File file : filesFromParams) {
            if (!filesFromFile.contains(file)) {
                result.addError(invalid(Level.ERROR, "nenalezen záznam pro soubor %s", file.getAbsolutePath()));
            }
        }

        for (File file : filesFromFile) {
            if (!filesFromParams.contains(file)) {
                result.addError(invalid(Level.ERROR, "nalezený soubor nebyl očekáván: %s", file.getAbsolutePath()));
            }
        }
    }


}
