package nkp.pspValidator.shared.engine.exceptions;

import java.io.File;

/**
 * Created by Martin Řehánek on 8.12.16.
 */
public class XmlFileParsingException extends Exception {
    private final File file;

    public XmlFileParsingException(File file, String message) {
        super(message);
        this.file = file;
    }
}
