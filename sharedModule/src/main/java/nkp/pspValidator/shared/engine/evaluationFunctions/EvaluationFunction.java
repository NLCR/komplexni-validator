package nkp.pspValidator.shared.engine.evaluationFunctions;

import nkp.pspValidator.shared.engine.*;
import nkp.pspValidator.shared.engine.exceptions.ContractException;
import nkp.pspValidator.shared.engine.params.*;

import java.io.File;
import java.util.*;

/**
 * Created by Martin Řehánek on 20.10.16.
 */
public abstract class EvaluationFunction implements Function {
    private final String name;
    protected final Engine engine;
    protected final Contract contract;
    protected final ValueParams valueParams = new ValueParams();
    protected final PatternParams patternParams = new PatternParams();

    public EvaluationFunction(String name, Engine engine, Contract contract) {
        this.name = name;
        this.engine = engine;
        this.contract = contract;
    }

    public final String getName() {
        return name;
    }

    protected void checkContractCompliance() throws ContractException {
        contract.checkCompliance(this);
    }

    public ValueType getResultType() {
        return contract.getReturnType();
    }


    public abstract ValueEvaluation evaluate();

    ValueEvaluation okResult(Object data) {
        return new ValueEvaluation(data, null);
    }

    ValueEvaluation errorResult(String errorMessage) {
        return new ValueEvaluation(null, String.format("%s: %s", getName(), errorMessage));
    }

    ValueEvaluation errorResultContractNotMet(ContractException e) {
        return new ValueEvaluation(null, String.format("%s: nesplněn kontrakt vyhodnocovací funkce: %s", getName(), e.getMessage()));
    }

    ValueEvaluation errorResultUnexpectedError(Throwable e) {
        return new ValueEvaluation(null, String.format("nečekaná chyba: %s", e.getMessage()));
    }

    ValueEvaluation errorResult(Exception e) {
        return new ValueEvaluation(null, e.getMessage());
    }

    ValueEvaluation errorResultParamNull(String paramName, ValueEvaluation paramEvaluation) {
        return new ValueEvaluation(null, String.format("%s: neznámá hodnota parametru '%s': %s", getName(), paramName, paramEvaluation.getErrorMessage()));
    }

    ValueEvaluation errorResultPatternParamNull(String paramName, PatternEvaluation paramEvaluation) {
        return new ValueEvaluation(null, String.format("%s: neznámá hodnota parametru (vzor) '%s': %s", getName(), paramName, paramEvaluation.getErrorMessage()));
    }

    ValueEvaluation errorResultFileDoesNotExist(File file) {
        return new ValueEvaluation(null, String.format("%s: soubor %s neexistuje", getName(), file.getAbsolutePath()));
    }

    ValueEvaluation errorResultFileIsNotDir(File file) {
        return new ValueEvaluation(null, String.format("%s: soubor %s není adresář", getName(), file.getAbsolutePath()));
    }

    ValueEvaluation errorResultFileIsDir(File file) {
        return new ValueEvaluation(null, String.format("%s: soubor %s je adresář", getName(), file.getAbsolutePath()));
    }

    ValueEvaluation errorResultCannotReadDir(File file) {
        return new ValueEvaluation(null, String.format("%s: nelze číst adresář %s", getName(), file.getAbsolutePath()));
    }

    ValueEvaluation errorResultCannotReadFile(File file) {
        return new ValueEvaluation(null, String.format("%s: nelze číst soubor %s", getName(), file.getAbsolutePath()));
    }


    @Override
    public EvaluationFunction withValueParam(String paramName, ValueType valueType, ValueEvaluation valueEvaluation) {
        valueParams.addParam(paramName, new ValueParamConstant(valueType, valueEvaluation));
        return this;
    }

    @Override
    public EvaluationFunction withValueParamByReference(String paramName, ValueType valueType, String varName) {
        valueParams.addParam(paramName, new ValueParamReference(engine, valueType, varName));
        return this;
    }

    @Override
    public EvaluationFunction withPatternParam(String paramName, PatternEvaluation patternEvaluation) {
        patternParams.addParam(paramName, new PatternParamConstant(patternEvaluation));
        return this;
    }

    @Override
    public EvaluationFunction withPatternParamByReference(String paramName, String varName) {
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

        private ValueType returnType;
        private final Set<String> expectedPatternParams = new HashSet<>();
        private final Map<String, ValueParamSpec> valueParamsSpec = new HashMap<>();

        public Contract withReturnType(ValueType returnType) {
            this.returnType = returnType;
            return this;
        }

        public Contract withPatternParam(String paramName) {
            expectedPatternParams.add(paramName);
            return this;
        }

        public Contract withValueParam(String paramName, ValueType type, Integer minValues, Integer maxValues) {
            valueParamsSpec.put(paramName, new ValueParamSpec(type, minValues, maxValues));
            return this;
        }

        public ValueType getReturnType() {
            return returnType;
        }

        public void checkCompliance(EvaluationFunction function) throws ContractException {
            checkValueParamCompliance(function.valueParams);
            checkPatternParamCompliance(function.patternParams);
        }

        private void checkValueParamCompliance(ValueParams valueParams) throws ContractException {
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
                        throw new ContractException(String.format("parametr '%s' musí být vždy typu %s, nalezena hodnota typu %s", expectedParamName, spec.getType(), param.getType()));
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
                    throw new ContractException(String.format("nenalezen očekávaný parametr (vzor) '%s", expectedParam));
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
