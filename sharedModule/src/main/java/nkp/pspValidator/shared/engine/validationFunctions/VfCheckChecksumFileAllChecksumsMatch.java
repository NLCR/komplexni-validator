package nkp.pspValidator.shared.engine.validationFunctions;

import nkp.pspValidator.shared.engine.*;
import nkp.pspValidator.shared.engine.exceptions.ContractException;
import nkp.pspValidator.shared.engine.exceptions.HashComputationException;
import nkp.pspValidator.shared.engine.exceptions.InvalidPathException;

import java.io.*;


/**
 * Created by Martin Řehánek on 27.10.16.
 */
public class VfCheckChecksumFileAllChecksumsMatch extends ValidationFunction {

    public static final String PARAM_CHECKSUM_FILE = "checksum_file";

    public VfCheckChecksumFileAllChecksumsMatch(String name, Engine engine) {
        super(name, engine, new Contract()
                .withValueParam(PARAM_CHECKSUM_FILE, ValueType.FILE, 1, 1)
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

            return validate(checksumFile);
        } catch (ContractException e) {
            return invalidContractNotMet(e);
        } catch (Throwable e) {
            return invalidUnexpectedError(e);
        }
    }

    private ValidationResult validate(File checksumFile) {
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

                String line = null;
                while ((line = br.readLine()) != null) {
                    String[] parts = line.split("[ \\t]");//space or tabulator
                    if (parts.length == 1) {
                        result.addError(invalid(Level.ERROR, "chybí oddělovač (mezera/tabulátor) na řádku '%s'", line));
                    } else {
                        String hashExpected = parts[0];
                        String filepath = parts[1];
                        try {
                            File file = Utils.buildAbsoluteFile(pspRootDir, filepath);
                            String hashComputed = Utils.computeHash(file);
                            if (!hashComputed.toUpperCase().equals(hashExpected.toUpperCase())) {
                                result.addError(invalid(Level.ERROR, "uvedený kontrolní součet '%s' nesouhlasí s vypočítaným kontrolním součtem '%s' pro soubor %s", hashExpected, hashComputed, file.getAbsolutePath()));
                            }
                        } catch (InvalidPathException e) {
                            //TODO: tohle se vyskytuje vickrat, udelat pro to metodu
                            result.addError(invalid(Level.ERROR, "cesta k souboru není zapsána korektně: '%s'", e.getPath()));

                        } catch (HashComputationException e) {
                            //TODO: tohle se vyskytuje vickrat, udelat pro to metodu
                            result.addError(invalid(Level.ERROR, "chyba výpočtu kontrolního součtu souboru %s: %s", checksumFile.getAbsolutePath(), e.getMessage()));
                        }
                    }
                }
                br.close();
            } catch (IOException e) {
                result.addError(invalid(Level.ERROR, "chyba při čtení souboru %s: %s", checksumFile.getAbsolutePath(), e.getMessage()));
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
                }
            }
        }
        return result;
    }

}
