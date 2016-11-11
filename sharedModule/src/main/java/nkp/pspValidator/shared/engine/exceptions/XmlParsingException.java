package nkp.pspValidator.shared.engine.exceptions;

import java.io.File;

/**
 * Created by martin on 1.11.16.
 */
public class XmlParsingException extends Exception {

    private final File file;

    public XmlParsingException(File file, String message) {
        super(message);
        this.file = file;
    }

    public File getFile() {
        return file;
    }
}
