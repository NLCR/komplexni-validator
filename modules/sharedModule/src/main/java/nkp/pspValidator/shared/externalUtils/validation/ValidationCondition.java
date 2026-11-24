package nkp.pspValidator.shared.externalUtils.validation;

/**
 * Podmínka validace v profilu binárního souboru (element condition): validace se vyhodnotí, jen když podmínka nad
 * zpracovaným výstupem nástroje platí, jinak nehlásí nic.
 */
public interface ValidationCondition {

    boolean holds(Object processedOutput) throws DataExtraction.ExtractionException;
}
