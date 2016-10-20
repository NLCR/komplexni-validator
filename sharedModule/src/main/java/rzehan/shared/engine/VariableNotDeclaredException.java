package rzehan.shared.engine;

/**
 * Created by martin on 20.10.16.
 */
public class VariableNotDeclaredException extends RuntimeException {

    public VariableNotDeclaredException(String name, String regexp) {
        super(String.format("Proměnná %s použitá ve výrazu \"%s\" pro něj nebyla deklarována", name, regexp));
    }
}
