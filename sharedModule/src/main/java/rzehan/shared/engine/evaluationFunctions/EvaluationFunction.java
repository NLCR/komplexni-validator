package rzehan.shared.engine.evaluationFunctions;

import rzehan.shared.engine.Engine;
import rzehan.shared.engine.ValueType;

import java.util.*;

/**
 * Created by martin on 20.10.16.
 */
public abstract class EvaluationFunction {
    protected final Engine engine;
    protected final Contract contract;
    protected ValueParams valueParams;
    protected PatternParams patternParams;

    public EvaluationFunction(Engine engine, Contract contract) {
        this.engine = engine;
        this.contract = contract;
    }

    public void setValueParams(ValueParams valueParams) {
        this.valueParams = valueParams;
    }

    public void setPatternParams(PatternParams patternParams) {
        this.patternParams = patternParams;
    }

    public ValueType getResultType() {
        return contract.getReturnType();
    }

    public abstract Object evaluate();

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

        public List<ValueParam> getParams(String name) {
            return data.get(name);
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

        public void checkComplience(ValueParams valueParams, PatternParams patternParams) {
            if (valueParams != null) {
                checkValueParamComplience(valueParams);
            }
            if (patternParams != null) {
                checkPatternParamComplience(patternParams);
            }
        }

        private void checkValueParamComplience(ValueParams valueParams) {
            //all actual params are expected
            for (String actualParam : valueParams.keySet()) {
                if (!valueParamsSpec.keySet().contains(actualParam)) {
                    //TODO: other exception
                    throw new RuntimeException(String.format("nalezen neočekávaný parametr (hodnota) %s", actualParam));
                }
            }
            //all expected params found and comply to spec
            for (String expectedParamName : valueParamsSpec.keySet()) {
                List<ValueParam> paramValues = valueParams.getParams(expectedParamName);
                if (paramValues == null) {
                    throw new RuntimeException(String.format("nelezen očekávaný parametr (vzor) %s", expectedParamName));
                }
                ValueParamSpec spec = valueParamsSpec.get(expectedParamName);
                if (spec.getMinValues() != null && paramValues.size() < spec.getMinValues()) {
                    throw new RuntimeException(String.format("parametr %s musí mít alespoň %d hodnot, nalezeno %d", expectedParamName, spec.getMinValues(), paramValues.size()));
                }
                if (spec.getMaxValues() != null && paramValues.size() > spec.getMaxValues()) {
                    throw new RuntimeException(String.format("parametr %s musí mít nejvýše %d hodnot, nalezeno %d", expectedParamName, spec.getMaxValues(), paramValues.size()));
                }
                for (ValueParam param : paramValues) {
                    if (param.getType() != spec.getType()) {
                        throw new RuntimeException(String.format("parametr %s musí být vždy typu %s, nalezen typ %s", expectedParamName, spec.getType(), param.getType()));
                    }
                }
            }

        }

        private void checkPatternParamComplience(PatternParams patternParams) {
            //all actual params are expected
            for (String actualParam : patternParams.keySet()) {
                if (!expectedPatternParams.contains(actualParam)) {
                    //TODO: other exception
                    throw new RuntimeException(String.format("nalezen neočekávaný parametr (vzor) %s", actualParam));
                }
            }
            //all expected params found
            for (String expectedParam : expectedPatternParams) {
                if (!patternParams.keySet().contains(expectedParam)) {
                    //TODO: other exception
                    throw new RuntimeException(String.format("nelezen očekávaný parametr (vzor) %s", expectedParam));
                }
            }
        }

        public static class ValueParamSpec {
            private final ValueType type;
            private final Integer minValues;
            private final Integer maxValues;

            public ValueParamSpec(ValueType type, Integer minValues, Integer maxValues) {
                this.type = type;
                if (type == null) {
                    throw new IllegalArgumentException("musí být uvedent typ parametru");
                }
                this.minValues = minValues;
                this.maxValues = maxValues;
                if (minValues != null && maxValues != null && minValues > maxValues) {
                    throw new IllegalArgumentException(String.format("minimální počet hodnot (%d) je větší, než maximální počet hodnot (%d)", minValues, maxValues));
                }
            }

            public ValueType getType() {
                return type;
            }

            public Integer getMinValues() {
                return minValues;
            }

            public Integer getMaxValues() {
                return maxValues;
            }
        }

    }


}
