package rzehan.shared.engine.evaluationFunctions;

import rzehan.shared.engine.Engine;
import rzehan.shared.engine.Pattern;
import rzehan.shared.engine.ValueType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by martin on 20.10.16.
 */
public abstract class EvaluationFunction {
    protected final Engine engine;
    private final ValueType resultType;
    protected ValueParams valueParams;


    public EvaluationFunction(Engine engine, ValueType resultType) {
        this.engine = engine;
        this.resultType = resultType;
    }

    public void setValueParams(ValueParams valueParams) {
        this.valueParams = valueParams;
    }


    public ValueType getResultType() {
        return resultType;
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
    }


    public static abstract class ValueParam {

        private final ValueType type;

        protected ValueParam(ValueType type) {
            this.type = type;
        }

        public ValueType getType() {
            return type;
        }

        public abstract Object getValue();

    }

    public static class ValueParamReference extends ValueParam {
        protected final Engine engine;
        //nazev promenne
        private final String varName;

        public ValueParamReference(Engine engine, ValueType type, String varName) {
            super(type);
            this.engine = engine;
            this.varName = varName;
        }

        @Override
        public Object getValue() {
            return engine.evaluateVariable(varName);
        }
    }

    public static final class ValueParamConstant extends ValueParam {
        //hodnota, treba i xpath
        private final Object value;

        public ValueParamConstant(ValueType type, Object value) {
            super(type);
            this.value = value;
        }

        @Override
        public Object getValue() {
            return value;
        }
    }


    public static abstract class PatternParam {

        public abstract boolean matches(String value);

    }


    public static class PatternParamConstant extends PatternParam {

        private final Pattern pattern;

        public PatternParamConstant(Pattern pattern) {
            this.pattern = pattern;
        }

        @Override
        public boolean matches(String value) {
            return pattern.matches(value);
        }
    }

    public static class PatternParamReference extends PatternParam {
        private final String patternName;

        public PatternParamReference(String patternName) {
            this.patternName = patternName;
        }

        @Override
        public boolean matches(String value) {
            //todo: engine vrati pattern podle jmena;
            return false;
        }
    }


}
