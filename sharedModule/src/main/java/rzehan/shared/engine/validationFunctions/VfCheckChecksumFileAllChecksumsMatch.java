package rzehan.shared.engine.validationFunctions;

import rzehan.shared.engine.Engine;
import rzehan.shared.engine.ValueType;

import java.io.*;
import java.util.HashSet;
import java.util.Set;


/**
 * Created by martin on 27.10.16.
 */
public class VfCheckChecksumFileAllChecksumsMatch extends ValidationFunction {

    public static final String PARAM_CHECKSUM_FILE = "checksum_file";
    public static final String PARAM_PSP_ROOT_DIR = "psp_root_dir";

    /*TODO: vlastne PARAM_PSP_ROOT_DIR nepotrebuju, zjistim to  PARAM_CHECKSUM_FILE - jeho adresar*/
    public VfCheckChecksumFileAllChecksumsMatch(Engine engine) {
        super(engine, new Contract()
                .withValueParam(PARAM_CHECKSUM_FILE, ValueType.FILE, 1, 1)
                .withValueParam(PARAM_PSP_ROOT_DIR, ValueType.FILE, 1, 1)
        );
    }

    @Override
    public String getName() {
        return "checkChecksumFileAllChecksumsMatch";
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
            return validate(checksumFile, pspRootDir);
        }
    }

    private ValidationResult validate(File checksumFile, File pspRootDir) {
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
                String hashExpected = parts[0];
                String filepath = parts[1];
                try {
                    File file = toAbsoluteFile(filepath, pspRootDir);
                    String hashComputed = computeHash(file);
                    if (!hashComputed.equals(hashExpected)) {
                        return new ValidationResult(false).withMessage(
                                String.format("uvedený kontrolní součet '%s' nesouhlasí s vypočítaným kontrolním součtem '%s' pro soubor '%s",
                                        hashExpected, hashComputed, file.getAbsolutePath()));
                    }
                    filesFromFile.add(file);
                } catch (PathInvalidException e) {
                    return new ValidationResult(false).withMessage(String.format("cesta k souboru není zapsána korektně: '%s'", filepath));
                } catch (HashComputationException e) {
                    return new ValidationResult(false).withMessage(String.format("chyba výpočtu kontrolního součtu souboru %s: %s", checksumFile.getAbsolutePath(), e.getMessage()));
                }
            }
            br.close();
            return new ValidationResult(true);
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

    private String computeHash(File file) throws HashComputationException {
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
            String md5 = org.apache.commons.codec.digest.DigestUtils.md5Hex(fis);
            fis.close();
            return md5.toUpperCase();
        } catch (Exception e) {
            throw new HashComputationException(e.getMessage());
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    //e.printStackTrace();
                }
            }
        }
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

    private static class HashComputationException extends Exception {
        public HashComputationException(String message) {
            super(message);
        }
    }


}
