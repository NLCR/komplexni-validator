package nkp.pspValidator.shared.engine.validationFunctions;

import nkp.pspValidator.shared.engine.Level;

import java.io.File;

/**
 * Created by Martin Řehánek on 11.11.16.
 */
public class ValidationProblem {

    private final Level level;

    private final File file;
    private final String description;

    public ValidationProblem(Level level, File file, String description) {
        this.level = level;
        this.file = file;
        this.description = description;
    }

    public Level getLevel() {
        return level;
    }

    public File getFile() {
        return file;
    }

    //TODO: change to getMessage a getDescription
    public String getMessage(boolean full) {
        if (full) {
            StringBuilder builder = new StringBuilder();
            if (file != null) {
                builder.append(file.getName()).append(": ");
            }
            //TODO: dalsi veci
            builder.append(description);
            return builder.toString();
        } else {
            return description;
        }
    }
}
