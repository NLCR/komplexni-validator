package nkp.pspValidator.shared.biblio.biblioValidator;

/**
 * Created by Martin Řehánek on 10.1.17.
 */
public interface ExpectedContentDefinition {
    public CheckingResult checkAgainst(String valueFound);
}
