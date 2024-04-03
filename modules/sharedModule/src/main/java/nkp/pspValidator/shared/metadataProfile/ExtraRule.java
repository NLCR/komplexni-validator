package nkp.pspValidator.shared.metadataProfile;

import nkp.pspValidator.shared.engine.XmlManager;
import org.w3c.dom.Element;

/**
 * Created by Martin Řehánek on 27.1.17.
 */
public interface ExtraRule {
    public CheckingResult checkAgainst(XmlManager manager, Element currentElement);
}
