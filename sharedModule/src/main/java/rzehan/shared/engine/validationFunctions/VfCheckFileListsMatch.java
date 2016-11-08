package rzehan.shared.engine.validationFunctions;

import rzehan.shared.engine.Engine;
import rzehan.shared.engine.ValueEvaluation;
import rzehan.shared.engine.ValueType;
import rzehan.shared.engine.exceptions.ContractException;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by martin on 27.10.16.
 */
public class VfCheckFileListsMatch extends ValidationFunction {

    public static final String PARAM_FILES_EXPECTED = "expected_files";
    public static final String PARAM_FILES_FOUND = "found_files";


    public VfCheckFileListsMatch(Engine engine) {
        super(engine, new Contract()
                .withValueParam(PARAM_FILES_EXPECTED, ValueType.FILE_LIST, 1, 1)
                .withValueParam(PARAM_FILES_FOUND, ValueType.FILE_LIST, 1, 1)
        );
    }

    @Override
    public String getName() {
        return "checkFileListsMatch";
    }

    @Override
    public ValidationResult validate() {
        try {
            checkContractCompliance();
        } catch (ContractException e) {
            return invalidContractNotMet(e);
        }

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


    }

    private ValidationResult validate(List<File> filesExpectedList, List<File> filesFoundList) {
        try {
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
                        return invalid(String.format("nalezen neočekávaný soubor %s", foundFile.getAbsolutePath()));
                    }
                }
                for (File expectedFile : filesExpectedSet) {
                    if (!filesFoundSet.contains(expectedFile)) {
                        return invalid(String.format("nenalezen očekávaný soubor %s", expectedFile.getAbsolutePath()));
                    }
                }
            }
            return valid();
        } catch (Throwable e) {
            return invalid("neočekávaná chyba: " + e.getMessage());
        }
    }
}
