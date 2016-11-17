package nkp.pspValidator.shared.imageUtils.validation;

import nkp.pspValidator.shared.NamespaceContextImpl;
import nkp.pspValidator.shared.engine.exceptions.XmlParsingException;
import nkp.pspValidator.shared.imageUtils.ImageUtil;
import nkp.pspValidator.shared.imageUtils.ImageUtilManager;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by martin on 17.11.16.
 */
public class J2kProfile {

    private final ImageUtilManager imageUtilManager;
    private final Type type;
    private final ImageUtil imageUtil;
    private List<Validation> validations = new ArrayList<>();
    private final NamespaceContextImpl namespaceContext = new NamespaceContextImpl();

    public J2kProfile(ImageUtilManager imageUtilManager, Type type, ImageUtil imageUtil) {
        this.imageUtilManager = imageUtilManager;
        this.type = type;
        this.imageUtil = imageUtil;
    }

    public void addNamespace(String prefix, String url) {
        namespaceContext.setNamespace(prefix, url);
    }

    public void addValidation(Validation validation) {
        validations.add(validation);
    }

    public List<String> validate(File imageFile) throws XmlParsingException, IOException, InterruptedException, DataExtraction.ExtractionException {
        List<String> errors = new ArrayList<>();
        String toolRawOutput = getToolOutput(imageFile);
        Object processedOutput = processOutput(toolRawOutput, imageUtil);
        for (Validation validation : validations) {
            String result = validation.validate(processedOutput);
            if (result != null) {
                errors.add(result);
            }
        }
        return errors;
    }

    private String getToolOutput(File imageFile) throws IOException, InterruptedException {
        try {
            return imageUtilManager.runUtilExecution(imageUtil, imageFile);
        } catch (IOException e) {
            throw new IOException(String.format("chyba běhu nástroje %s: ", imageUtil, e.getMessage()), e);
        } catch (InterruptedException e) {
            throw new IOException(String.format("běh nástroje %s přerušen: ", imageUtil, e.getMessage()), e);
        }
    }

    private Object processOutput(String toolRawOutput, ImageUtil util) throws XmlParsingException {
        switch (type) {
            case XML:
                return toXml(toolRawOutput, util);
            case TEXT:
                return toolRawOutput;
            default:
                throw new IllegalStateException();
        }
    }

    private Object toXml(String toolRawOutput, ImageUtil util) throws XmlParsingException {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            InputSource is = new InputSource(new StringReader(toolRawOutput));
            return builder.parse(is);
        } catch (SAXException e) {
            throw new XmlParsingException(String.format("chyba parsování xml (výstup nástroje %s): %s", util, e.getMessage()));
        } catch (IOException e) {
            throw new XmlParsingException(String.format("chyba čtení výstupu nástroje %s: %s", util, e.getMessage()));
        } catch (ParserConfigurationException e) {
            throw new XmlParsingException(String.format("chyba konfigurace parseru při čtení výstupu nástroje %s: %s", util, e.getMessage()));
        }
    }

    public NamespaceContextImpl getNamespaceContext() {
        return namespaceContext;
    }

    public static enum Type {
        XML, TEXT;
    }
}
