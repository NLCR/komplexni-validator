package nkp.pspValidator.shared.externalUtils.validation;

import nkp.pspValidator.shared.engine.Level;
import nkp.pspValidator.shared.engine.exceptions.ExternalUtilOutputParsingException;
import nkp.pspValidator.shared.externalUtils.CliCommand;
import nkp.pspValidator.shared.externalUtils.ExecutionOutcome;
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
     * Když nástroj soubor prokazatelně nezkontroloval (nejde spustit, spadl, skončil neočekávaným kódem podle
     * externalUtils.xml), je to jediný problém úrovně ERROR „nástroj … soubor nezkontroloval: …“ a validace profilu
     * se nevyhodnocují. Když nástroj skončil běžně, ale ve výstupu chybí znak zpracování souboru, vyhodnotí se
     * validace profilu a hláška o nezkontrolovaném souboru (ERROR) se přidá, jen když profil žádný problém nenašel
     * (u poškozeného souboru tak nevznikne druhá, matoucí chyba).
     * <p>
     * Chyba parsování výstupu nebo extrakce hodnoty je problém s úrovní null (úroveň z rules.xml).
     * Neočekávaná výjimka (např. NullPointerException, ClassCastException při chybné konfiguraci profilu) se nepolyká:
     * je to problém úrovně ERROR a ostatní validace se vyhodnotí dál.
     */
    public List<Validation.Problem> validateProblems(String executionName, File file) {
        List<Validation.Problem> problems = new ArrayList<>();
        ExecutionOutcome outcome;
        Object processedOutput;
        try {
            outcome = runExternalUtil(executionName, file);
            if (outcome.getFailure() != null) {
                problems.add(new Validation.Problem(Level.ERROR, notCheckedMessage(outcome.getFailure())));
                return problems;
            }
            processedOutput = processExternalUtilOutput(outcome.getOutput(), externalUtil);
        } catch (CliCommand.CliCommandException e) {
            problems.add(new Validation.Problem(Level.ERROR, notCheckedMessage("nejde spustit: " + e.getMessage())));
            return problems;
        } catch (ExternalUtilOutputParsingException e) {
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
        if (problems.isEmpty() && outcome.getMissingEvidence() != null) {
            problems.add(new Validation.Problem(Level.ERROR, notCheckedMessage(outcome.getMissingEvidence())));
        }
        return problems;
    }

    private String notCheckedMessage(String reason) {
        return String.format("nástroj %s soubor nezkontroloval: %s", externalUtil.getUserFriendlyName(), reason);
    }

    ExecutionOutcome runExternalUtil(String executionName, File imageFile) throws CliCommand.CliCommandException {
        return externalUtilManager.runUtilExecutionChecked(new ExternalUtilExecution(executionName, externalUtil), imageFile);
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
