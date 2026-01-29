package nkp.pspValidator.shared.engine.exceptions;

import java.io.File;

/**
 * Created by Martin Řehánek on 2.11.16.
 */
public class HashComputationException extends Exception {

    private final File file;

    public HashComputationException(File file, String message) {
        super(message);
        this.file = file;
    }

    public File getFile() {
        return file;
    }
}
