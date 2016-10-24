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
    protected PatternParams patternParams;

    public EvaluationFunction(Engine engine, ValueType resultType) {
        this.engine = engine;
        this.resultType = resultType;
    }

    public void setValueParams(ValueParams valueParams) {
        this.valueParams = valueParams;
    }

    public void setPatternParams(PatternParams patternParams) {
        this.patternParams = patternParams;
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

    public static class PatternParams {
        private final Map<String, PatternParam> data = new HashMap<>();

        public void addParam(String name, PatternParam value) {
            //TODO: error if pattern already is defined
            data.put(name, value);
        }

        public PatternParam getParam(String name) {
            return data.get(name);
        }
    }


}
