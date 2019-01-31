package nkp.pspValidator.shared.engine.validationFunctions;

import nkp.pspValidator.shared.engine.Engine;
import nkp.pspValidator.shared.engine.Level;
import nkp.pspValidator.shared.engine.ValueEvaluation;
import nkp.pspValidator.shared.engine.ValueType;
import nkp.pspValidator.shared.engine.exceptions.ContractException;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

    private ValidationResult validate(List<File> filesExpectedList, List<File> filesFoundList) {
        ValidationResult result = new ValidationResult();
        Set<File> filesExpectedSet = new HashSet<>(filesExpectedList.size());
        for (File file : filesExpectedList) {
            filesExpectedSet.add(file.getAbsoluteFile());
        }
        Set<File> filesFoundSet = new HashSet<>(filesFoundList.size());
        for (File file : filesFoundList) {
            filesFoundSet.add(file.getAbsoluteFile());
        }

        if (!filesExpectedSet.equals(filesFoundSet)) {//something is different
            for (File foundFile : filesFoundSet) {
                if (!filesExpectedSet.contains(foundFile)) {
                    result.addError(invalid(Level.ERROR, "nalezen neočekávaný soubor %s", foundFile.getAbsolutePath()));
                }
            }
            for (File expectedFile : filesExpectedSet) {
                if (!filesFoundSet.contains(expectedFile)) {
                    result.addError(invalid(Level.ERROR, "nenalezen očekávaný soubor %s", expectedFile.getAbsolutePath()));
                }
            }
        }
        return result;
    }
}
