package nkp.pspValidator.shared.externalUtils.validation;

import nkp.pspValidator.shared.engine.Level;

import java.util.List;

/**
 * Created by Martin Řehánek on 17.11.16.
 */
public interface DataRule {

    List<String> validate(Object data);

    /**
     * Úroveň problémů pravidla (atribut level v profilu binárního souboru), null = úroveň z rules.xml
     * (parametr validation_problem_level funkce checkBinaryFilesValidByExternalUtil).
     */
    Level getLevel();
}
