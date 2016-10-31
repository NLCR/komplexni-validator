package rzehan.shared.engine.validationFunctions;

import rzehan.shared.engine.Engine;
import rzehan.shared.engine.ValueType;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Stream;


/**
 * Created by martin on 27.10.16.
 */
public class VfCheckChecksumGeneratedByGrammar extends ValidationFunction {

    public static final String PARAM_CHECKSUM_FILE = "checksum_file";


    public VfCheckChecksumGeneratedByGrammar(Engine engine) {
        super(engine, new Contract()
                .withValueParam(PARAM_CHECKSUM_FILE, ValueType.FILE, 1, 1)
        );
    }

    @Override
    public ValidationResult validate() {
        checkContractCompliance();

        File file = (File) valueParams.getParams(PARAM_CHECKSUM_FILE).get(0).getValue();

        if (file == null) {
            return new ValidationResult(false).withMessage(String.format("hodnota parametru %s funkce %s je null", PARAM_CHECKSUM_FILE, getName()));
        } else if (!file.exists()) {
            return new ValidationResult(false).withMessage(String.format("soubor %s neexistuje", file.getAbsoluteFile()));
        } else if (file.isDirectory()) {
            return new ValidationResult(false).withMessage(String.format("soubor %s je adresář", file.getAbsoluteFile()));
        } else {
            return validate(file);
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
                    return new ValidationResult(false).withMessage(String.format("chybí oddělovač (mezera/tabulátor) na řádku '%s'", line));
                }
                if (!is32bHex(parts[0])) {
                    return new ValidationResult(false).withMessage(String.format("kontrolní součet není v 32B hexadecimálním zápisu, řádek: '%s'", line));
                }
                if (!isValidPath(parts[1])) {
                    return new ValidationResult(false).withMessage(String.format("cesta k souboru není zapsána korektně, řádek: '%s'", line));
                }
            }
            br.close();
            return new ValidationResult(true);
        } catch (IOException e) {
            return new ValidationResult(false).withMessage(String.format("chyba při čtení souboru %s: %s", file.getAbsolutePath(), e.getMessage()));
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

    @Override
    public String getName() {
        return "CHECK_CHECKSUM_GENERATED_BY_GRAMMAR";
    }
}
