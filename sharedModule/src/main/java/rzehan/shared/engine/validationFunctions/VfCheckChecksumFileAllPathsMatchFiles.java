package rzehan.shared.engine.validationFunctions;

import rzehan.shared.engine.Engine;
import rzehan.shared.engine.ValueEvaluation;
import rzehan.shared.engine.ValueType;
import rzehan.shared.engine.exceptions.ContractException;
import rzehan.shared.engine.exceptions.EmptyParamEvaluationException;
import rzehan.shared.engine.exceptions.InvalidPathException;
import rzehan.shared.engine.params.ValueParam;

import java.io.*;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


/**
 * Created by martin on 27.10.16.
 */
public class VfCheckChecksumFileAllPathsMatchFiles extends ValidationFunction {

    public static final String PARAM_CHECKSUM_FILE = "checksum_file";
    public static final String PARAM_FILE = "file";
    public static final String PARAM_FILES = "files";

    public VfCheckChecksumFileAllPathsMatchFiles(Engine engine) {
        super(engine, new Contract()
                .withValueParam(PARAM_CHECKSUM_FILE, ValueType.FILE, 1, 1)
                .withValueParam(PARAM_FILE, ValueType.FILE, 0, null)
                .withValueParam(PARAM_FILES, ValueType.FILE_LIST, 0, null)
        );
    }

    @Override
    public String getName() {
        return "checkChecksumFileAllPathsMatchFiles";
    }

    @Override
    public ValidationResult validate() {
        try {
            checkContractCompliance();
        } catch (ContractException e) {
            return invalidContractNotMet(e);
        }

        ValueEvaluation paramChecksumFile = valueParams.getParams(PARAM_CHECKSUM_FILE).get(0).getEvaluation();
        File checksumFile = (File) paramChecksumFile.getData();
        if (checksumFile == null) {
            return invalidValueParamNull(PARAM_CHECKSUM_FILE, paramChecksumFile);
        } else if (!checksumFile.exists()) {
            return invalidFileDoesNotExist(checksumFile);
        } else if (checksumFile.isDirectory()) {
            return invalidFileIsDir(checksumFile);
        }

        File pspRootDir = checksumFile.getParentFile();
        if (!pspRootDir.exists()) {
            return invalidFileDoesNotExist(pspRootDir);
        } else if (!pspRootDir.isDirectory()) {
            return invalidFileIsNotDir(pspRootDir);
        } else {
            try {
                Set<File> files = mergeAbsolutFilesFromParams();
                return validate(checksumFile, pspRootDir, files);
            } catch (EmptyParamEvaluationException e) {
                return invalidValueParamNull(e.getParamName(), e.getEvaluation());
            }
        }
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

    private ValidationResult validate(File checksumFile, File pspRootDir, Set<File> filesFromParams) {
        FileInputStream fis = null;
        BufferedReader br = null;
        try {
            fis = new FileInputStream(checksumFile);
            br = new BufferedReader(new InputStreamReader(fis));

            String line = null;
            Set<File> filesFromFile = new HashSet<>();
            while ((line = br.readLine()) != null) {
                String[] parts = line.split("[ \\t]");//space or tabulator
                if (parts.length == 1) {
                    return new ValidationResult(false).withMessage(String.format("chybí oddělovač (mezera/tabulátor) na řádku '%s'", line));
                }
                String hash = parts[0];
                String filepath = parts[1];
                try {
                    File file = toAbsoluteFile(filepath, pspRootDir);
                    filesFromFile.add(file);
                } catch (InvalidPathException e) {
                    return invalid(String.format("cesta k souboru není zapsána korektně: '%s'", e.getPath()));
                }
            }
            br.close();
            return setsAreSame(filesFromParams, filesFromFile);
            //return new ValidationResult(true);
        } catch (IOException e) {
            return invalid(String.format("chyba při čtení souboru %s: %s", checksumFile.getAbsolutePath(), e.getMessage()));
        } finally {
            try {
                if (br != null) {
                    br.close();
                }
                if (fis != null) {
                    fis.close();
                }
            } catch (IOException e) {
                //e.printStackTrace();
            }
        }
    }

    private ValidationResult setsAreSame(Set<File> filesFromParams, Set<File> filesFromFile) {
        for (File file : filesFromParams) {
            if (!filesFromFile.contains(file)) {
                return invalid(String.format("nenalezen záznam pro soubor %s", file.getAbsolutePath()));
            }
        }

        for (File file : filesFromFile) {
            if (!filesFromParams.contains(file)) {
                return invalid(String.format("nalezený soubor nebyl očekáván: %s", file.getAbsolutePath()));
            }
        }
        return valid();
    }

    private File toAbsoluteFile(String filePath, File pspRootDir) throws InvalidPathException {
        //prevod do "zakladni formy", tj. zacinajici rovnou nazvem souboru/adresare
        //tj. "neco", "\neco", "/neco", "./neco", ".\neco" -> "neco"
        if (filePath.startsWith("./") || filePath.startsWith(".\\")) {
            filePath = filePath.substring(2, filePath.length());
        } else if (filePath.startsWith("/") || filePath.startsWith("\\")) {
            filePath = filePath.substring(1, filePath.length());
        }
        // tenhle tvar nesmi ale zacinat na tecky ani lomitka
        if (filePath.matches("^[\\./\\\\]+.*")) {
            System.out.println(filePath);
            throw new InvalidPathException(filePath);
        }
        String[] segments = filePath.split("[\\\\/]");
        File file = new File(pspRootDir, buildFileFromSegments(segments));
        return file.getAbsoluteFile();
    }

    private String buildFileFromSegments(String[] segments) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < segments.length; i++) {
            if (i != 0) {//prvni soubor/adresar by nemel zacinat oddelovacem
                builder.append(File.separatorChar);
            }
            builder.append(segments[i]);
        }
        return builder.toString();
    }


}
