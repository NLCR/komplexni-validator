package nkp.pspValidator.shared.externalUtils.validation;

import nkp.pspValidator.shared.NamespaceContextImpl;
import nkp.pspValidator.shared.XmlUtils;
import nkp.pspValidator.shared.engine.exceptions.ExternalUtilOutputParsingException;
import nkp.pspValidator.shared.externalUtils.ExternalUtil;
import nkp.pspValidator.shared.externalUtils.ExternalUtilManager;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;

/**
 * Created by Martin Řehánek on 18.11.16.
 */
public class BinaryFileXmlProfile extends BinaryFileProfile {


    private final NamespaceContextImpl namespaceContext = new NamespaceContextImpl();

    public BinaryFileXmlProfile(ExternalUtilManager externalUtilManager, ExternalUtil externalUtil) {
        super(externalUtilManager, externalUtil);
    }

    public void addNamespace(String prefix, String url) {
        namespaceContext.setNamespace(prefix, url);
    }

    public NamespaceContextImpl getNamespaceContext() {
        return namespaceContext;
    }

    Object processExternalUtilOutput(String toolRawOutput, ExternalUtil util) throws ExternalUtilOutputParsingException {
        try {
            Document doc = XmlUtils.buildDocumentFromString(toolRawOutput, true);
            /*String docStr = XmlUtils.toString(doc);
            System.out.println(docStr);*/
            return doc;
        } catch (SAXException e) {
            throw new ExternalUtilOutputParsingException(String.format("chyba parsování xml (výstup nástroje %s): %s", util, e.getMessage()));
        } catch (IOException e) {
            throw new ExternalUtilOutputParsingException(String.format("chyba čtení výstupu nástroje %s: %s", util, e.getMessage()));
        } catch (ParserConfigurationException e) {
            throw new ExternalUtilOutputParsingException(String.format("chyba konfigurace parseru při čtení výstupu nástroje %s: %s", util, e.getMessage()));
        }
    }

}
