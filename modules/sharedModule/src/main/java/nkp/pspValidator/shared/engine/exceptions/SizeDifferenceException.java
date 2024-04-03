package nkp.pspValidator.shared.engine.exceptions;

import java.io.File;

/**
 * Created by Martin Řehánek on 11.11.16.
 */
public class SizeDifferenceException extends Exception {

    private final File file;
    public SizeDifferenceException(String message, File file) {
        super(message);
        this.file = file;
    }

    public File getFile() {
        return file;
    }
}
