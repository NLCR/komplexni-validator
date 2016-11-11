package rzehan.shared.engine.validationFunctions;

import rzehan.shared.engine.Engine;
import rzehan.shared.engine.Utils;
import rzehan.shared.engine.ValueEvaluation;
import rzehan.shared.engine.ValueType;
import rzehan.shared.engine.exceptions.ContractException;
import rzehan.shared.engine.exceptions.HashComputationException;
import rzehan.shared.engine.exceptions.InvalidPathException;

import java.io.*;
import java.util.HashSet;
import java.util.Set;


/**
 * Created by martin on 27.10.16.
 */
public class VfCheckChecksumFileAllChecksumsMatch extends ValidationFunction {

    public static final String PARAM_CHECKSUM_FILE = "checksum_file";

    public VfCheckChecksumFileAllChecksumsMatch(Engine engine) {
        super(engine, new Contract()
                .withValueParam(PARAM_CHECKSUM_FILE, ValueType.FILE, 1, 1)
        );
    }

    @Override
    public String getName() {
        return "checkChecksumFileAllChecksumsMatch";
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
                return validate(checksumFile, pspRootDir);
            }
        } catch (ContractException e) {
            return invalidContractNotMet(e);
        } catch (Throwable e) {
            return invalidUnexpectedError(e);
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
                    return invalid(String.format("chybí oddělovač (mezera/tabulátor) na řádku '%s'", line));
                }
                String hashExpected = parts[0];
                String filepath = parts[1];
                try {
                    File file = Utils.buildAbsoluteFile(pspRootDir, filepath);
                    String hashComputed = Utils.computeHash(file);
                    if (!hashComputed.toUpperCase().equals(hashExpected.toUpperCase())) {
                        return invalid(String.format("uvedený kontrolní součet '%s' nesouhlasí s vypočítaným kontrolním součtem '%s' pro soubor %s",
                                hashExpected, hashComputed, file.getAbsolutePath()));
                    }
                    filesFromFile.add(file);
                } catch (InvalidPathException e) {
                    return invalid(String.format("cesta k souboru není zapsána korektně: '%s'", e.getPath()));
                } catch (HashComputationException e) {
                    return invalid(String.format("chyba výpočtu kontrolního součtu souboru %s: %s", checksumFile.getAbsolutePath(), e.getMessage()));
                }
            }
            br.close();
            return valid();
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

}
