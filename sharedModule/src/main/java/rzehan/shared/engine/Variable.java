package rzehan.shared.engine;

/**
 * Created by martin on 20.10.16.
 */
public class Variable<T> {

    private final T value;

    public Variable(T value) {
        this.value = value;
    }

    public T getValue() {
        return value;
    }
}
