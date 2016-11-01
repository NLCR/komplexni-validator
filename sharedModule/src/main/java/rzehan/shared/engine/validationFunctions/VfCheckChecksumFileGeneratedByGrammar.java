package rzehan.shared.engine.validationFunctions;

import rzehan.shared.engine.Engine;
import rzehan.shared.engine.ValueEvaluation;
import rzehan.shared.engine.ValueType;
import rzehan.shared.engine.exceptions.ContractException;

import java.io.*;


/**
 * Created by martin on 27.10.16.
 */
public class VfCheckChecksumFileGeneratedByGrammar extends ValidationFunction {

    public static final String PARAM_CHECKSUM_FILE = "checksum_file";


    public VfCheckChecksumFileGeneratedByGrammar(Engine engine) {
        super(engine, new Contract()
                .withValueParam(PARAM_CHECKSUM_FILE, ValueType.FILE, 1, 1)
        );
    }

    @Override
    public String getName() {
        return "checkChecksumFileGeneratedByGrammar";
    }

    @Override
    public ValidationResult validate() {
        try {
            checkContractCompliance();
        } catch (ContractException e) {
            return invalidContractNotMet(e);
        }

        ValueEvaluation paramChecksumFile = valueParams.getParams(PARAM_CHECKSUM_FILE).get(0).getValueEvaluation();
        File checksumFile = (File) paramChecksumFile.getData();
        if (checksumFile == null) {
            return invalidParamNull(PARAM_CHECKSUM_FILE, paramChecksumFile);
        } else if (!checksumFile.exists()) {
            return invalidFileDoesNotExist(checksumFile);
        } else if (checksumFile.isDirectory()) {
            return invalidFileIsDir(checksumFile);
        } else {
            return validate(checksumFile);
        }
    }

    private ValidationResult validate(File file) {
        FileInputStream fis = null;
        BufferedReader br = null;
        try {
            fis = new FileInputStream(file);
            br = new BufferedReader(new InputStreamReader(fis));

            String line = null;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split("[ \\t]");//space or tabulator
                if (parts.length == 1) {
                    return invalid(String.format("chybí oddělovač (mezera/tabulátor) na řádku '%s'", line));
                }
                if (!is32bHex(parts[0])) {
                    return invalid(String.format("kontrolní součet není v 32B hexadecimálním zápisu, řádek: '%s'", line));
                }
                if (!isValidPath(parts[1])) {
                    return invalid(String.format("cesta k souboru není zapsána korektně, řádek: '%s'", line));
                }
            }
            br.close();
            return valid();
        } catch (IOException e) {
            return invalid(String.format("chyba při čtení souboru %s: %s", file.getAbsolutePath(), e.getMessage()));
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
