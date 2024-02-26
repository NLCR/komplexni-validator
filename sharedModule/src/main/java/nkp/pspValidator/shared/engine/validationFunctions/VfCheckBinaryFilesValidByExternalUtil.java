package nkp.pspValidator.shared.engine.validationFunctions;

import nkp.pspValidator.shared.engine.Engine;
import nkp.pspValidator.shared.engine.Level;
import nkp.pspValidator.shared.engine.ValueEvaluation;
import nkp.pspValidator.shared.engine.ValueType;
import nkp.pspValidator.shared.engine.exceptions.ContractException;
import nkp.pspValidator.shared.engine.params.ValueParam;
import nkp.pspValidator.shared.externalUtils.ExternalUtil;
import nkp.pspValidator.shared.externalUtils.ExternalUtilExecution;
import nkp.pspValidator.shared.externalUtils.ResourceType;
import nkp.pspValidator.shared.externalUtils.validation.BinaryFileProfile;
import nkp.pspValidator.shared.externalUtils.validation.BinaryFileValidator;

import java.io.File;
import java.util.List;


/**
 * Created by Martin Řehánek on 27.10.16.
 */
public class VfCheckBinaryFilesValidByExternalUtil extends ValidationFunction {

    public static final String PARAM_NO_FILES_PROBLEM_LEVEL = "no_files_problem_level";
    public static final String PARAM_FILES = "files";
    public static final String PARAM_TYPE = "type";
    public static final String PARAM_UTIL = "util";
    public static final String PARAM_EXECUTION = "execution";
    public static final String PARAM_VALIDATION_PROBLEM_LEVEL = "validation_problem_level";

    public VfCheckBinaryFilesValidByExternalUtil(String name, Engine engine) {
        super(name, engine, new Contract()
                .withValueParam(PARAM_NO_FILES_PROBLEM_LEVEL, ValueType.LEVEL, 0, 1)
                .withValueParam(PARAM_FILES, ValueType.FILE_LIST, 1, 1)
                .withValueParam(PARAM_TYPE, ValueType.RESOURCE_TYPE, 1, 1)
                .withValueParam(PARAM_UTIL, ValueType.EXTERNAL_UTIL, 1, 1)
                .withValueParam(PARAM_EXECUTION, ValueType.STRING, 1, 1)
                .withValueParam(PARAM_VALIDATION_PROBLEM_LEVEL, ValueType.LEVEL, 0, 1)
        );
    }

    @Override
    public ValidationResult validate() {
        try {
            checkContractCompliance();

            Level noFilesProblemLevel = Level.ERROR;
            List<ValueParam> noFilesProblemLevelParams = valueParams.getParams(PARAM_NO_FILES_PROBLEM_LEVEL);
            if (noFilesProblemLevelParams.size() == 1) {
                ValueEvaluation eval = noFilesProblemLevelParams.get(0).getEvaluation();
                noFilesProblemLevel = (Level) eval.getData();
            }

            ValueEvaluation paramFiles = valueParams.getParams(PARAM_FILES).get(0).getEvaluation();
            List<File> files = (List<File>) paramFiles.getData();
            if (files == null) {
                return invalidValueParamNull(PARAM_FILES, paramFiles);
            } else if (files.isEmpty()) {
                return singlErrorResult(invalid(noFilesProblemLevel, null, "prázdný seznam souborů"));
            }

            Level validationProblemLevel = Level.ERROR;
            List<ValueParam> validationProblemLevelParams = valueParams.getParams(PARAM_VALIDATION_PROBLEM_LEVEL);
            if (!validationProblemLevelParams.isEmpty()) {
                ValueEvaluation eval = validationProblemLevelParams.get(0).getEvaluation();
                validationProblemLevel = (Level) eval.getData();
            }

            ValueEvaluation paramType = valueParams.getParams(PARAM_TYPE).get(0).getEvaluation();
            ResourceType type = (ResourceType) paramType.getData();
            if (type == null) {
                return invalidValueParamNull(PARAM_TYPE, paramType);
            }

            ValueEvaluation paramUtil = valueParams.getParams(PARAM_UTIL).get(0).getEvaluation();
            ExternalUtil util = (ExternalUtil) paramUtil.getData();
            if (util == null) {
                return invalidValueParamNull(PARAM_UTIL, paramUtil);
            }

            ValueEvaluation paramExecution = valueParams.getParams(PARAM_EXECUTION).get(0).getEvaluation();
            String executionName = (String) paramExecution.getData();
            if (executionName == null) {
                return invalidValueParamNull(PARAM_EXECUTION, paramExecution);
            }

            ExternalUtilExecution execution = new ExternalUtilExecution(executionName, util);
            return validate(validationProblemLevel, files, type, execution);
        } catch (ContractException e) {
            return invalidContractNotMet(e);
        } catch (Exception e) {
            e.printStackTrace();
            return invalidUnexpectedError(e);
        }
    }

    private ValidationResult validate(Level level, List<File> files, ResourceType type, ExternalUtilExecution execution) {
        BinaryFileValidator validator = engine.getBinaryFileValidator();
        if (validator.isUtilDisabled(execution.getUtil())) {
            return singlErrorResult(invalid(Level.INFO, null, "nástroj %s je vypnutý", execution.getUtil().getUserFriendlyName()));
        } else if (!validator.isUtilAvailable(execution.getUtil())) {
            return singlErrorResult(invalid(Level.INFO, null, "nástroj %s není dostupný", execution.getUtil().getUserFriendlyName()));
        } else if (!validator.isUtilExecutionDefined(execution)) {
            return singlErrorResult(invalid(Level.INFO, null, "pro nástroj %s není definováno spuštění '%s'", execution.getUtil().getUserFriendlyName(), execution.getName()));
        } else {
            ValidationResult result = new ValidationResult();
            BinaryFileProfile profile = validator.getProfile(type, execution.getUtil());
            if (profile == null) {
                return singlErrorResult(invalid(Level.ERROR, null, "nenalezen profil binárního souboru pro typ %s a nástroj %s", type, execution));
            }
            for (File file : files) {
                //System.out.println(String.format("validating (%s): %s", profile, file.getAbsolutePath()));
                try {
                    List<String> problems = profile.validate(execution.getName(), file);
                    for (String problem : problems) {
                        result.addError(invalid(level, file, "%s", problem));
                    }
                } catch (Exception e) {
                    result.addError(invalid(Level.ERROR, file, "%s", e.getMessage()));
                    e.printStackTrace();
                }
                //break;
            }
            return result;
        }
    }

}
