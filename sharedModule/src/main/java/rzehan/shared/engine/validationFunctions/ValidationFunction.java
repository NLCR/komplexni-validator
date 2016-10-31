package rzehan.shared.engine.validationFunctions;

import rzehan.shared.engine.Engine;
import rzehan.shared.engine.Function;
import rzehan.shared.engine.Pattern;
import rzehan.shared.engine.ValueType;
import rzehan.shared.engine.exceptions.ValidatorException;
import rzehan.shared.engine.params.*;

import java.util.*;

/**
 * Created by martin on 20.10.16.
 */
public abstract class ValidationFunction implements Function {
    protected final Engine engine;
    protected final Contract contract;
    protected final ValueParams valueParams = new ValueParams();
    protected final PatternParams patternParams = new PatternParams();

    public ValidationFunction(Engine engine, Contract contract) {
        this.engine = engine;
        this.contract = contract;
    }

    protected void checkContractCompliance() {
        contract.checkCompliance(this);
    }

    public abstract ValidationResult validate();

    public abstract String getName();

    @Override
    public ValidationFunction withValueParam(String paramName, ValueType valueType, Object value) {
        valueParams.addParam(paramName, new ValueParamConstant(valueType, value));
        return this;
    }

    @Override
    public ValidationFunction withValueParamByReference(String paramName, ValueType valueType, String varName) {
        valueParams.addParam(paramName, new ValueParamReference(engine, valueType, varName));
        return this;
    }

    @Override
    public ValidationFunction withPatternParam(String paramName, Pattern pattern) {
        patternParams.addParam(paramName, new PatternParamConstant(pattern));
        return this;
    }

    @Override
    public ValidationFunction withPatternParamByReference(String paramName, String varName) {
        patternParams.addParam(paramName, new PatternParamReference(engine, varName));
        return this;
    }

    /*
    public void addValueParams(ValueParams valueParams) {
        this.valueParams.addAll(valueParams);

    }

    public void addPatternParams(PatternParams patternParams) {
        this.patternParams.addAll(patternParams);
    }

*/


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

        public void checkCompliance(ValidationFunction function) {
            checkValueParamComplience(function.valueParams, function.getName());
            checkPatternParamCompliance(function.patternParams, function.getName());
        }

        private void checkValueParamComplience(ValueParams valueParams, String functionName) {
            //all actual params are expected
            for (String actualParam : valueParams.keySet()) {
                if (!valueParamsSpec.keySet().contains(actualParam)) {
                    throw new ValidatorException(String.format("%s: nalezen neočekávaný parametr (hodnota) %s", functionName, actualParam));
                }
            }
            //all expected params found and comply to spec
            for (String expectedParamName : valueParamsSpec.keySet()) {
                List<ValueParam> paramValues = valueParams.getParams(expectedParamName);
                ValueParamSpec spec = valueParamsSpec.get(expectedParamName);
                if (spec.getMinOccurs() != null && paramValues.size() < spec.getMinOccurs()) {
                    throw new ValidatorException(String.format("%s: parametr %s musí mít alespoň %d hodnot, nalezeno %d", functionName, expectedParamName, spec.getMinOccurs(), paramValues.size()));
                }
                if (spec.getMaxOccurs() != null && paramValues.size() > spec.getMaxOccurs()) {
                    throw new ValidatorException(String.format("%s: parametr %s musí mít nejvýše %d hodnot, nalezeno %d", functionName, expectedParamName, spec.getMaxOccurs(), paramValues.size()));
                }
                for (ValueParam param : paramValues) {
                    if (param.getType() != spec.getType()) {
                        throw new ValidatorException(String.format("%s: parametr %s musí být vždy typu %s, nalezen typ %s", functionName, expectedParamName, spec.getType(), param.getType()));
                    }
                }
            }

        }

        private void checkPatternParamCompliance(PatternParams patternParams, String functionName) {
            //all actual params are expected
            for (String actualParam : patternParams.keySet()) {
                if (!expectedPatternParams.contains(actualParam)) {
                    throw new ValidatorException(String.format("%s: nalezen neočekávaný parametr (vzor) %s", functionName, actualParam));
                }
            }
            //all expected params found
            for (String expectedParam : expectedPatternParams) {
                if (!patternParams.keySet().contains(expectedParam)) {
                    throw new ValidatorException(String.format("%s: nenalezen očekávaný parametr (vzor) %s", functionName, expectedParam));
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
