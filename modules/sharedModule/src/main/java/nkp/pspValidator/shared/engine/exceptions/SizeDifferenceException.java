package nkp.pspValidator.shared.engine.exceptions;

import java.io.File;

/**
 * Created by Martin Řehánek on 11.11.16.
 */
public class SizeDifferenceException extends Exception {

    private final File file;
    private final long sizeExpected;
    private final long sizeActual;

    public SizeDifferenceException(File file, long sizeExpected, long sizeActual) {
        super(String.format("uvedená velikost (%d B) se liší od zjištěné velikosti (%d B) souboru %s", sizeExpected, sizeActual, file.getName()));
        this.file = file;
        this.sizeExpected = sizeExpected;
        this.sizeActual = sizeActual;
    }

    public File getFile() {
        return file;
    }

    public long getSizeExpected() {
        return sizeExpected;
    }

    public long getSizeActual() {
        return sizeActual;
    }

    public String getSimpleMessage() {
        return "uvedná velikost se liší od zjištěné velikosti souboru (v bytech)";
    }
}
