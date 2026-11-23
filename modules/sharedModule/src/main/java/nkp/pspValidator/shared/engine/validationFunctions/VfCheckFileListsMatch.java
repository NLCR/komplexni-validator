package nkp.pspValidator.shared.engine.validationFunctions;

import nkp.pspValidator.shared.engine.Engine;
import nkp.pspValidator.shared.engine.Level;
import nkp.pspValidator.shared.engine.ValueEvaluation;
import nkp.pspValidator.shared.engine.ValueType;
import nkp.pspValidator.shared.engine.exceptions.ContractException;

import nkp.pspValidator.shared.engine.utils.PackageFileLookup;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Created by Martin Řehánek on 27.10.16.
 */
public class VfCheckFileListsMatch extends ValidationFunction {

    public static final String PARAM_FILES_EXPECTED = "expected_files";
    public static final String PARAM_FILES_FOUND = "found_files";


    public VfCheckFileListsMatch(String name, Engine engine) {
        super(name, engine, new Contract()
                .withValueParam(PARAM_FILES_EXPECTED, ValueType.FILE_LIST, 1, 1)
                .withValueParam(PARAM_FILES_FOUND, ValueType.FILE_LIST, 1, 1)
        );
    }

    @Override
    public ValidationResult validate() {
        try {
            checkContractCompliance();

            ValueEvaluation paramFilesExpected = valueParams.getParams(PARAM_FILES_EXPECTED).get(0).getEvaluation();
            List<File> filesExpected = (List<File>) paramFilesExpected.getData();
            if (filesExpected == null) {
                return invalidValueParamNull(PARAM_FILES_EXPECTED, paramFilesExpected);
            }

            ValueEvaluation paramFilesFound = valueParams.getParams(PARAM_FILES_FOUND).get(0).getEvaluation();
            List<File> filesFound = (List<File>) paramFilesFound.getData();
            if (filesFound == null) {
                return invalidValueParamNull(PARAM_FILES_FOUND, paramFilesFound);
            }

            return validate(filesExpected, filesFound);
        } catch (ContractException e) {
            return invalidContractNotMet(e);
        } catch (Throwable e) {
            return invalidUnexpectedError(e);
        }
    }

    /**
     * Porovnani podle absolutni cesty jako retezce (PackageFileLookup.pathKey): velikost pismen se rozlisuje na vsech
     * systemech. Drive pres TreeSet&lt;File&gt;, ktery ji na Windows nerozlisoval (File.compareTo), takze odkaz z metadat
     * s jinou velikosti pismen tam prosel a na macOS a Linuxu ne.
     */
    private ValidationResult validate(List<File> filesExpectedList, List<File> filesFoundList) {
        ValidationResult result = new ValidationResult();
        Map<String, File> filesExpected = PackageFileLookup.byPathKey(filesExpectedList);
        Map<String, File> filesFound = PackageFileLookup.byPathKey(filesFoundList);

        if (!filesExpected.keySet().equals(filesFound.keySet())) {//something is different
            List<File> unexpected = new ArrayList<>();
            for (Map.Entry<String, File> found : filesFound.entrySet()) {
                if (!filesExpected.containsKey(found.getKey())) {
                    unexpected.add(found.getValue());
                }
            }
            List<File> missing = new ArrayList<>();
            for (Map.Entry<String, File> expected : filesExpected.entrySet()) {
                if (!filesFound.containsKey(expected.getKey())) {
                    missing.add(expected.getValue());
                }
            }
            Map<String, TreeMap<String, File>> unexpectedIndex = PackageFileLookup.caseVariantIndex(unexpected);
            Map<String, TreeMap<String, File>> missingIndex = PackageFileLookup.caseVariantIndex(missing);
            for (File foundFile : unexpected) {
                File variant = PackageFileLookup.findCaseVariant(foundFile, missingIndex);
                String hint = variant == null ? "" : PackageFileLookup.actualFileHint(variant.getAbsolutePath());
                result.addError(invalid(Level.ERROR, "nalezen neočekávaný soubor %s%s", foundFile.getAbsolutePath(), hint)
                        .withFile(foundFile).withSimpleMessage("nalezen neočekávaný soubor"));
            }
            for (File expectedFile : missing) {
                File variant = PackageFileLookup.findCaseVariant(expectedFile, unexpectedIndex);
                String hint = variant == null ? "" : PackageFileLookup.referenceHint(variant.getAbsolutePath());
                result.addError(invalid(Level.ERROR, "nenalezen očekávaný soubor %s%s", expectedFile.getAbsolutePath(), hint)
                        .withFile(expectedFile).withSimpleMessage("nenalezen očekávaný soubor"));
            }
        }
        return result;
    }
}
