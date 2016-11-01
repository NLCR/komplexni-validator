package rzehan.shared.engine.exceptions;

/**
 * Created by martin on 1.11.16.
 */
public class InvalidPathException extends Exception {

    private final String path;

    public InvalidPathException(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }
}
