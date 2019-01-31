package nkp.pspValidator.shared.engine.validationFunctions;

import nkp.pspValidator.shared.engine.Engine;
import nkp.pspValidator.shared.engine.Level;
import nkp.pspValidator.shared.engine.ValueEvaluation;
import nkp.pspValidator.shared.engine.ValueType;
import nkp.pspValidator.shared.engine.exceptions.ContractException;

import java.io.*;


/**
 * Created by Martin Řehánek on 27.10.16.
 */
public class VfCheckChecksumFileGeneratedByGrammar extends ValidationFunction {

    public static final String PARAM_CHECKSUM_FILE = "checksum_file";


    public VfCheckChecksumFileGeneratedByGrammar(String name, Engine engine) {
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

    private ValidationResult validate(File file) {
        ValidationResult result = new ValidationResult();
        FileInputStream fis = null;
        BufferedReader br = null;
        try {
            fis = new FileInputStream(file);
            br = new BufferedReader(new InputStreamReader(fis));

            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split("[ \\t]");//space or tabulator
                if (parts.length == 1) {
                    result.addError(invalid(Level.ERROR, "chybí oddělovač (mezera/tabulátor) na řádku '%s'", line));
                } else if (!is32bHex(parts[0])) {
                    result.addError(invalid(Level.INFO, "kontrolní součet není v 32B hexadecimálním zápisu, řádek: '%s'", line));
                } else if (!isValidPath(parts[1])) {
                    result.addError(invalid(Level.WARNING, "cesta k souboru není zapsána korektně, řádek: '%s'", line));
                }
            }
            br.close();
        } catch (IOException e) {
            result.addError(invalid(Level.ERROR, "chyba při čtení souboru %s: %s", file.getAbsolutePath(), e.getMessage()));
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

    private boolean is32bHex(String hash) {
        if (hash == null || hash.isEmpty() || hash.length() != 32) {
            return false;
        } else {
            return hash.matches("[0-9a-fA-F]*");
        }
    }

    private boolean isValidPath(String path) {
        if (!(path.startsWith("\\") || path.startsWith("/"))) {
            return false;
        }
        String[] segments = path.split("[\\\\/]");// backslash or slash
        for (int i = 0; i < segments.length; i++) {
            String segment = segments[i];
            if (i != 0) {//first segment ignored, should be empty
                if (!segment.matches("[0-9a-zA-Z\\._\\-]+")) { //apha, digit, '.', '_', '-'
                    return false;
                }
            }
        }
        return true;
    }
}
