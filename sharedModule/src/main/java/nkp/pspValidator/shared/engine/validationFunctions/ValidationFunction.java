package nkp.pspValidator.shared.engine.validationFunctions;

import nkp.pspValidator.shared.engine.*;
import nkp.pspValidator.shared.engine.exceptions.ContractException;
import nkp.pspValidator.shared.engine.params.*;

import java.io.File;
import java.util.*;

/**
 * Created by Martin Řehánek on 20.10.16.
 */
public abstract class ValidationFunction implements Function {
    private final String name;
    protected final Engine engine;
    protected final Contract contract;
    protected final ValueParams valueParams = new ValueParams();
    protected final PatternParams patternParams = new PatternParams();

    public ValidationFunction(String name, Engine engine, Contract contract) {
        this.name = name;
        this.engine = engine;
        this.contract = contract;
    }

    public final String getName() {
        return name;
    }

    protected void checkContractCompliance() throws ContractException {
        if (contract != null) {
            contract.checkCompliance(this);
        } else {
            //TODO: logging
            System.err.println("No contract for " + getName());
        }
    }

    public abstract ValidationResult validate();


    ValidationResult singlErrorResult(ValidationProblem error) {
        ValidationResult result = new ValidationResult();
        result.addError(error);
        return result;
    }

    //TODO: prejmenovat metody

    ValidationResult invalidContractNotMet(ContractException e) {
        ValidationResult result = new ValidationResult();
        result.addError(Level.ERROR, String.format("%s: nesplněn kontrakt validační funkce: %s", getName(), e.getMessage()));
        return result;
    }

    ValidationResult invalidUnexpectedError(Throwable e) {
        ValidationResult result = new ValidationResult();
        String message = e.getMessage() != null ? e.getMessage() : e.toString();
        result.addError(Level.ERROR, String.format("nečekaná chyba: %s", message));
        return result;
    }

    ValidationResult invalidValueParamNull(String paramName, ValueEvaluation paramEvaluation) {
        ValidationResult result = new ValidationResult();

        String message = paramEvaluation != null ? paramEvaluation.getErrorMessage() : null;
        result.addError(Level.ERROR, String.format("%s: neznámá hodnota povinného parametru '%s': %s", getName(), paramName, message));
        return result;
    }

    ValidationResult invalidPatternParamNull(String paramName, PatternEvaluation paramEvaluation) {
        ValidationResult result = new ValidationResult();
        result.addError(Level.ERROR, String.format("%s: neznámá hodnota poviného parametru (vzor) '%s': %s", getName(), paramName, paramEvaluation.getErrorMessage()));
        return result;
    }

    ValidationProblem invalidFileDoesNotExist(File file) {
        return new ValidationProblem(Level.ERROR, String.format("%s: soubor %s neexistuje", getName(), file.getAbsoluteFile()));
    }

    ValidationProblem invalidFileIsDir(File file) {
        return new ValidationProblem(Level.ERROR, String.format("%s: soubor %s je adresář", getName(), file.getAbsoluteFile()));
    }

    ValidationProblem invalidFileIsNotDir(File file) {
        return new ValidationProblem(Level.ERROR, String.format("%s: soubor %s není adresář", getName(), file.getAbsoluteFile()));
    }

    ValidationProblem invalidCannotReadFile(File file) {
        return new ValidationProblem(Level.ERROR, String.format("nelze číst soubor %s", file.getAbsoluteFile()));
    }

    ValidationProblem invalidCannotReadDir(File dir) {
        return new ValidationProblem(Level.ERROR, String.format("%s: nelze číst adresář %s", getName(), dir.getAbsoluteFile()));
    }

    ValidationProblem invalid(Exception e) {
        return new ValidationProblem(Level.ERROR, e.getMessage());
    }

    ValidationProblem invalid(Level level, String errorMessage) {
        return new ValidationProblem(level, String.format("%s: %s", getName(), errorMessage));
    }

    ValidationProblem invalid(Level level, String errorMessage, Object... errorMsgParams) {
        return new ValidationProblem(level, String.format("%s: %s", getName(), String.format(errorMessage, errorMsgParams)));
    }


    @Override
    public ValidationFunction withValueParam(String paramName, ValueType valueType, ValueEvaluation valueEvaluation) {
        valueParams.addParam(paramName, new ValueParamConstant(valueType, valueEvaluation));
        return this;
    }

    @Override
    public ValidationFunction withValueParamByReference(String paramName, ValueType valueType, String varName) {
        valueParams.addParam(paramName, new ValueParamReference(engine, valueType, varName));
        return this;
    }

    @Override
    public ValidationFunction withPatternParam(String paramName, PatternEvaluation patternEvaluation) {
        patternParams.addParam(paramName, new PatternParamConstant(patternEvaluation));
        return this;
    }

    @Override
    public ValidationFunction withPatternParamByReference(String paramName, String varName) {
        patternParams.addParam(paramName, new PatternParamReference(engine, varName));
        return this;
    }

    public static class ValueParams {
        private final Map<String, List<ValueParam>> data = new HashMap<>();

        public void addParam(String name, ValueParam value) {
            List<ValueParam> values = data.get(name);
            if (values == null) {
                values = new ArrayList<>();
                data.put(name, values);
            }
            values.add(value);
        }

        public void addAll(ValueParams valueParams) {
            for (String paramName : valueParams.keySet()) {
                List<ValueParam> params = valueParams.getParams(paramName);
                addParams(paramName, params);
            }
        }

        private void addParams(String paramName, List<ValueParam> params) {
            List<ValueParam> values = data.get(paramName);
            if (values == null) {
                values = new ArrayList<>();
                data.put(paramName, values);
            }
            values.addAll(params);
        }

        /**
         * @param name
         * @return never null, possibly empty list
         */
        public List<ValueParam> getParams(String name) {
            List<ValueParam> params = data.get(name);
            return params != null ? params : Collections.emptyList();
        }

        public Set<String> keySet() {
            return data.keySet();
        }


    }

    public static class PatternParams {
        private final Map<String, PatternParam> data = new HashMap<>();

        public void addParam(String name, PatternParam value) {
            //TODO: error if pattern already is defined
            data.put(name, value);
        }

        public PatternParam getParam(String name) {
            return data.get(name);
        }


        public Set<String> keySet() {
            return data.keySet();
        }

        public void addAll(PatternParams patternParams) {
            for (String paramName : patternParams.keySet()) {
                addParam(paramName, patternParams.getParam(paramName));
            }
        }
    }

    public static class Contract {

        private final Set<String> expectedPatternParams = new HashSet<>();
        private final Map<String, ValueParamSpec> valueParamsSpec = new HashMap<>();

        public Contract withPatternParam(String paramName) {
            expectedPatternParams.add(paramName);
            return this;
        }

        public Contract withValueParam(String paramName, ValueType type, Integer minValues, Integer maxValues) {
            valueParamsSpec.put(paramName, new ValueParamSpec(type, minValues, maxValues));
            return this;
        }

        public void checkCompliance(ValidationFunction function) throws ContractException {
            checkValueParamComplience(function.valueParams);
            checkPatternParamCompliance(function.patternParams);
        }

        private void checkValueParamComplience(ValueParams valueParams) throws ContractException {
            //all actual params are expected
            for (String actualParam : valueParams.keySet()) {
                if (!valueParamsSpec.keySet().contains(actualParam)) {
                    throw new ContractException(String.format("nalezen neočekávaný parametr (hodnota) '%s'", actualParam));
                }
            }
            //all expected params found and comply to spec
            for (String expectedParamName : valueParamsSpec.keySet()) {
                List<ValueParam> paramValues = valueParams.getParams(expectedParamName);
                ValueParamSpec spec = valueParamsSpec.get(expectedParamName);
                if (spec.getMinOccurs() != null && paramValues.size() < spec.getMinOccurs()) {
                    throw new ContractException(String.format("parametr '%s' musí mít alespoň %d hodnot, nalezeno %d", expectedParamName, spec.getMinOccurs(), paramValues.size()));
                }
                if (spec.getMaxOccurs() != null && paramValues.size() > spec.getMaxOccurs()) {
                    throw new ContractException(String.format("parametr '%s' musí mít nejvýše %d hodnot, nalezeno %d", expectedParamName, spec.getMaxOccurs(), paramValues.size()));
                }
                for (ValueParam param : paramValues) {
                    if (param.getType() != spec.getType()) {
                        throw new ContractException(String.format("parametr '%s' musí být vždy typu %s, nalezen typ %s", expectedParamName, spec.getType(), param.getType()));
                    }
                }
            }

        }

        private void checkPatternParamCompliance(PatternParams patternParams) throws ContractException {
            //all actual params are expected
            for (String actualParam : patternParams.keySet()) {
                if (!expectedPatternParams.contains(actualParam)) {
                    throw new ContractException(String.format("nalezen neočekávaný parametr (vzor) '%s'", actualParam));
                }
            }
            //all expected params found
            for (String expectedParam : expectedPatternParams) {
                if (!patternParams.keySet().contains(expectedParam)) {
                    throw new ContractException(String.format("nenalezen očekávaný parametr (vzor) '%s'", expectedParam));
                }
            }
        }

        public static class ValueParamSpec {
            private final ValueType type;
            private final Integer minOccurs;
            private final Integer maxOccurs;

            /**
             * @param type
             * @param minOccurs null value means 0
             * @param maxOccurs null value means "unbound"
             */
            public ValueParamSpec(ValueType type, Integer minOccurs, Integer maxOccurs) {
                this.type = type;
                if (type == null) {
                    //TODO: nahradit za hlidane ValidatorConfigurationException
                    throw new IllegalArgumentException("musí být uveden typ parametru");
                }
                if (minOccurs == null) {
                    this.minOccurs = 0;
                } else if (minOccurs < 0) {
                    throw new IllegalArgumentException("minimální počet výskytů musí být kladné číslo");

                } else {
                    this.minOccurs = minOccurs;
                }
                if (maxOccurs == null) {
                    this.maxOccurs = null;
                } else if (maxOccurs < 1) {
                    throw new IllegalArgumentException("maximální počet výskytů musí být alespoň 1");
                } else {
                    this.maxOccurs = maxOccurs;
                }
                if (minOccurs != null && maxOccurs != null && minOccurs > maxOccurs) {
                    throw new IllegalArgumentException(String.format("minimální počet výskytů (%d) je větší, než maximální počet výskytů (%d)", minOccurs, maxOccurs));
                }
            }

            public ValueType getType() {
                return type;
            }

            public Integer getMinOccurs() {
                return minOccurs;
            }

            public Integer getMaxOccurs() {
                return maxOccurs;
            }
        }

    }


}
