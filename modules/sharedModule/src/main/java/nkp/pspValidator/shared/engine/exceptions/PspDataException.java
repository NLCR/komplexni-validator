package nkp.pspValidator.shared.engine.exceptions;

import java.io.File;

/**
 * Created by Martin Řehánek on 2.11.16.
 */
public class PspDataException extends Exception {

    private final File pspRootDir;

    public PspDataException(File pspRootDir, String message) {
        super(message);
        this.pspRootDir = pspRootDir;
    }
}
