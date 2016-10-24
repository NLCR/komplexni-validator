package rzehan.shared.engine.evaluationFunctions;

/**
 * Created by martin on 24.10.16.
 */
public class PatternParamReference extends PatternParam {
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
