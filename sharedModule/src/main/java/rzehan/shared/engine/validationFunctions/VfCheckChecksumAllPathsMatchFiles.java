package rzehan.shared.engine.validationFunctions;

import com.sun.xml.internal.bind.v2.TODO;
import rzehan.shared.engine.Engine;
import rzehan.shared.engine.ValueType;
import rzehan.shared.engine.params.ValueParam;

import java.io.*;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


/**
 * Created by martin on 27.10.16.
 */
public class VfCheckChecksumAllPathsMatchFiles extends ValidationFunction {

    public static final String PARAM_CHECKSUM_FILE = "checksum_file";
    public static final String PARAM_PSP_ROOT_DIR = "psp_root_dir";
    public static final String PARAM_FILE = "file";
    public static final String PARAM_FILES = "files";


    /*TODO: vlastne PARAM_PSP_ROOT_DIR nepotrebuju, zjistim to  PARAM_CHECKSUM_FILE - jeho adresa*/
    public VfCheckChecksumAllPathsMatchFiles(Engine engine) {
        super(engine, new Contract()
                .withValueParam(PARAM_CHECKSUM_FILE, ValueType.FILE, 1, 1)
                .withValueParam(PARAM_PSP_ROOT_DIR, ValueType.FILE, 1, 1)
                .withValueParam(PARAM_FILE, ValueType.FILE, 0, null)
                .withValueParam(PARAM_FILES, ValueType.FILE_LIST, 0, null)
        );
    }

    @Override
    public String getName() {
        return "CHECK_CHECKSUM_ALL_PATHS_MATCH_FILES";
    }

    @Override
    public ValidationResult validate() {
        checkContractCompliance();

        File checksumFile = (File) valueParams.getParams(PARAM_CHECKSUM_FILE).get(0).getValue();
        File pspRootDir = (File) valueParams.getParams(PARAM_PSP_ROOT_DIR).get(0).getValue();

        if (checksumFile == null) {
            return new ValidationResult(false).withMessage(String.format("hodnota parametru %s funkce %s je null", PARAM_CHECKSUM_FILE, getName()));
        } else if (!checksumFile.exists()) {
            return new ValidationResult(false).withMessage(String.format("soubor %s neexistuje", checksumFile.getAbsoluteFile()));
        } else if (checksumFile.isDirectory()) {
            return new ValidationResult(false).withMessage(String.format("soubor %s je adresář", checksumFile.getAbsoluteFile()));
        } else if (pspRootDir == null) {
            return new ValidationResult(false).withMessage(String.format("hodnota parametru %s funkce %s je null", PARAM_PSP_ROOT_DIR, getName()));
        } else if (!pspRootDir.exists()) {
            return new ValidationResult(false).withMessage(String.format("soubor %s neexistuje", pspRootDir.getAbsoluteFile()));
        } else if (!pspRootDir.isDirectory()) {
            return new ValidationResult(false).withMessage(String.format("soubor %s není adresář", pspRootDir.getAbsoluteFile()));
        } else {
            Set<File> files = mergeAbsolutFilesFromParams();
            return validate(checksumFile, pspRootDir, files);
        }
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
                } catch (PathInvalidException e) {
                    return new ValidationResult(false).withMessage(String.format("cesta k souboru není zapsána korektně: '%s'", filepath));
                }
            }
            br.close();
            return same(filesFromParams, filesFromFile);
            //return new ValidationResult(true);
        } catch (IOException e) {
            return new ValidationResult(false).withMessage(String.format("chyba při čtení souboru %s: %s", checksumFile.getAbsolutePath(), e.getMessage()));
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

    private ValidationResult same(Set<File> filesFromParams, Set<File> filesFromFile) {
        for (File file : filesFromParams) {
            if (!filesFromFile.contains(file)) {
                return new ValidationResult(false).withMessage(String.format("nenalezen záznam pro soubor %s", file.getAbsolutePath()));
            }
        }

        for (File file : filesFromFile) {
            if (!filesFromParams.contains(file)) {
                return new ValidationResult(false).withMessage(String.format("nalezený soubor nebyl očekáván: %s", file.getAbsolutePath()));
            }
        }
        return new ValidationResult(true);
    }

    private File toAbsoluteFile(String filePath, File pspRootDir) throws PathInvalidException {
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
            throw new PathInvalidException();
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

    private static class PathInvalidException extends Exception {
    }


}
