package nkp.pspValidator.shared.externalUtils.validation;

import nkp.pspValidator.shared.engine.Level;
import nkp.pspValidator.shared.engine.exceptions.ExternalUtilOutputParsingException;
import nkp.pspValidator.shared.externalUtils.CliCommand;
import nkp.pspValidator.shared.externalUtils.ExternalUtil;
import nkp.pspValidator.shared.externalUtils.ExternalUtilExecution;
import nkp.pspValidator.shared.externalUtils.ExternalUtilManager;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Martin Řehánek on 17.11.16.
 */
public abstract class BinaryFileProfile {

    final ExternalUtilManager externalUtilManager;
    final ExternalUtil externalUtil;
    List<Validation> validations = new ArrayList<>();


    public BinaryFileProfile(ExternalUtilManager externalUtilManager, ExternalUtil externalUtil) {
        this.externalUtilManager = externalUtilManager;
        this.externalUtil = externalUtil;
    }

    public void addValidation(Validation validation) {
        validations.add(validation);
    }

    /**
     * Texty problémů bez úrovní, viz {@link #validateProblems(String, File)}.
     */
    public List<String> validate(String executionName, File imageFile) {
        List<String> messages = new ArrayList<>();
        for (Validation.Problem problem : validateProblems(executionName, imageFile)) {
            messages.add(problem.getMessage());
        }
        return messages;
    }

    /**
     * Spustí nástroj nad souborem a vyhodnotí všechny validace profilu.
     * <p>
     * Chyba spuštění nástroje, parsování výstupu nebo extrakce hodnoty je problém s úrovní null (úroveň z rules.xml).
     * Neočekávaná výjimka (např. NullPointerException, ClassCastException při chybné konfiguraci profilu) se nepolyká:
     * je to problém úrovně ERROR a ostatní validace se vyhodnotí dál.
     */
    public List<Validation.Problem> validateProblems(String executionName, File file) {
        List<Validation.Problem> problems = new ArrayList<>();
        Object processedOutput;
        try {
            String toolRawOutput = runExternalUtil(executionName, file);
            processedOutput = processExternalUtilOutput(toolRawOutput, externalUtil);
        } catch (CliCommand.CliCommandException | ExternalUtilOutputParsingException e) {
            problems.add(new Validation.Problem(null, e.getMessage()));
            return problems;
        } catch (RuntimeException e) {
            problems.add(new Validation.Problem(Level.ERROR,
                    String.format("neočekávaná chyba při zpracování výstupu nástroje %s: %s", externalUtil.getUserFriendlyName(), e)));
            return problems;
        }
        for (Validation validation : validations) {
            try {
                problems.addAll(validation.validate(processedOutput));
            } catch (DataExtraction.ExtractionException e) {
                e.printStackTrace();
                problems.add(new Validation.Problem(null, e.getMessage()));
            } catch (RuntimeException e) {
                problems.add(new Validation.Problem(Level.ERROR,
                        String.format("%s: neočekávaná chyba při vyhodnocení: %s", validation.getName(), e)));
            }
        }
        return problems;
    }

    String runExternalUtil(String executionName, File imageFile) throws CliCommand.CliCommandException {
        return externalUtilManager.runUtilExecution(new ExternalUtilExecution(executionName, externalUtil), imageFile);
    }

    abstract Object processExternalUtilOutput(String toolRawOutput, ExternalUtil util) throws ExternalUtilOutputParsingException;

    public enum Type {
        XML, TEXT;
    }

    @Override
    public String toString() {
        return "BinaryFileProfile{" +
                "externalUtil=" + externalUtil +
                ", validations=" + validations.size() +
                '}';
    }
}
