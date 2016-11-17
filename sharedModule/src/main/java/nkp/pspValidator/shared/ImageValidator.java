package nkp.pspValidator.shared;

import com.mycila.xmltool.XMLDoc;
import com.mycila.xmltool.XMLTag;
import nkp.pspValidator.shared.engine.exceptions.ValidatorConfigurationException;
import nkp.pspValidator.shared.engine.exceptions.XmlParsingException;
import nkp.pspValidator.shared.imageUtils.ExtractionType;
import nkp.pspValidator.shared.imageUtils.ImageCopy;
import nkp.pspValidator.shared.imageUtils.ImageUtil;
import nkp.pspValidator.shared.imageUtils.ImageUtilManager;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.*;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by martin on 16.11.16.
 */
public class ImageValidator {

    private final ImageUtilManager imageUtilManager;
    private final Map<ImageUtil, J2kProfile> masterCopyProfiles = new HashMap<>();
    private final Map<ImageUtil, J2kProfile> userCopyProfiles = new HashMap<>();


    public ImageValidator(ImageUtilManager imageUtilManager) {
        this.imageUtilManager = imageUtilManager;
    }

    public void processProfile(ImageUtil util, ImageCopy copy, File profileDefinitionFile) throws ValidatorConfigurationException {
        switch (copy) {
            case MASTER:
                processProfile(util, profileDefinitionFile, masterCopyProfiles);
                break;
            case USER:
                processProfile(util, profileDefinitionFile, userCopyProfiles);
                break;
        }
    }

    public J2kProfile getProfile(ImageCopy copy, ImageUtil util) {
        switch (copy) {
            case MASTER:
                return masterCopyProfiles.get(util);
            case USER:
                return userCopyProfiles.get(util);
            default:
                throw new IllegalStateException();
        }
    }

    private void processProfile(ImageUtil util, File profileDefinitionFile, Map<ImageUtil, J2kProfile> profiles) throws ValidatorConfigurationException {
        XMLTag doc = XMLDoc.from(profileDefinitionFile, true);
        J2kProfile profile = buildProfile(util, doc.getCurrentTag());
        profiles.put(util, profile);
    }

    private J2kProfile buildProfile(ImageUtil util, Element rootEl) throws ValidatorConfigurationException {
        Element profileTypeEl = XmlUtils.getChilrenElements(rootEl).get(0);
        switch (profileTypeEl.getTagName()) {
            case "xmlValidations":
                return buildXmlProfile(rootEl, util);
            /*TODO: jeste pro parsovani samotneho textu, treba neco jako "textValidation"*/
            default:
                throw new ValidatorConfigurationException("neznámý element " + profileTypeEl.getTagName());
        }
    }

    private J2kProfile buildXmlProfile(Element rootEl, ImageUtil util) throws ValidatorConfigurationException {
        J2kProfile profile = new J2kProfile(imageUtilManager, J2kProfile.Type.XML, util);
        //extractions data
        Element namespacesEl = XmlUtils.getFirstChildElementsByName(rootEl, "namespaces");
        if (namespacesEl != null) {
            List<Element> nsEls = XmlUtils.getChildrenElementsByName(namespacesEl, "namespace");
            for (Element nsEl : nsEls) {
                String prefix = nsEl.getAttribute("prefix");
                String url = nsEl.getTextContent().trim();
                profile.addNamespace(prefix, url);
            }
            Element defaultNamespaceEl = XmlUtils.getFirstChildElementsByName(namespacesEl, "defaultNamespace");
            if (defaultNamespaceEl != null) {
                profile.addNamespace(null, defaultNamespaceEl.getTextContent().trim());
            }
        }
        List<Element> validationEls = XmlUtils.getChildrenElementsByName(rootEl, "validation");
        for (Element attributeEl : validationEls) {
            String name = attributeEl.getAttribute("name");
            System.out.println("name: " + name);
            //extraction
            Element extractionEl = XmlUtils.getFirstChildElementsByName(attributeEl, "extraction");
            DataExtraction dataExtraction = buildExtraction(profile.getNamespaceContext(), extractionEl);
            //validations
            Element validationEl = XmlUtils.getFirstChildElementsByName(attributeEl, "validation");
            List<DataRule> dataRules = new ArrayList<>();
            List<Element> ruleEls = XmlUtils.getChilrenElements(validationEl);
            for (Element ruleEl : ruleEls) {
                dataRules.add(buildRule(name, ruleEl));
            }
            Validation validation = new Validation(name, dataExtraction, dataRules);
            profile.addValidation(validation);
        }
        return profile;
    }

    private DataExtraction buildExtraction(NamespaceContextImpl nsContext, Element extractionEl) throws ValidatorConfigurationException {
        String typeStr = extractionEl.getAttribute("type");
        ExtractionType type = ExtractionType.valueOf(typeStr);

        Element firstPathAvailableEl = XmlUtils.getFirstChildElementsByName(extractionEl, "firstNonempty");
        if (firstPathAvailableEl != null) {
            List<String> xpaths = new ArrayList<>();
            List<Element> xpathEls = XmlUtils.getChildrenElementsByName(firstPathAvailableEl, "xpath");
            for (Element xpathEl : xpathEls) {
                xpaths.add(xpathEl.getTextContent().trim());
            }
            return new FirstNonemptyDataExctraction(type, nsContext, xpaths);
        }
        throw new ValidatorConfigurationException("neznámá extrakce hodnoty");
    }

    private DataRule buildRule(String validationName, Element ruleEl) throws ValidatorConfigurationException {
        //mustExist
        //mustNotExist
        //mustMatch

        switch (ruleEl.getTagName()) {
            case "mustExist": {
                return new MustExist(validationName);
            }
            case "mustNotExist": {
                return new MustNotExist(validationName);
            }
            case "mustMatch": {
                Element childEl = XmlUtils.getChilrenElements(ruleEl).get(0);
                Constraint constraint = buildConstraint(childEl);
                return new MustMatch(validationName, constraint);
            }
            case "mustMatchOne": {
                List<Constraint> constraints = new ArrayList<>();
                List<Element> constraintEls = XmlUtils.getChilrenElements(ruleEl);
                for (Element constraintEl : constraintEls) {
                    constraints.add(buildConstraint(constraintEl));
                }
                return new MustMatchOne(validationName, constraints);
            }
            default:
                throw new ValidatorConfigurationException("nečekaný element %s", ruleEl.getTagName());
        }
    }

    private Constraint buildConstraint(Element constraintEl) {
        switch (constraintEl.getTagName()) {
            case "constant": {
                String constant = constraintEl.getTextContent().trim();
                return new ConstantConstraint(constant);
            }
            case "floatRange": {
                Element minEl = XmlUtils.getFirstChildElementsByName(constraintEl, "min");
                Float min = minEl == null ? null : Float.valueOf(minEl.getTextContent().trim());
                Element maxEl = XmlUtils.getFirstChildElementsByName(constraintEl, "max");
                Float max = maxEl == null ? null : Float.valueOf(maxEl.getTextContent().trim());
                return new FlowRangeConstraint(min, max);
            }
            case "intRange": {
                Element minEl = XmlUtils.getFirstChildElementsByName(constraintEl, "min");
                Integer min = minEl == null ? null : Integer.valueOf(minEl.getTextContent().trim());
                Element maxEl = XmlUtils.getFirstChildElementsByName(constraintEl, "max");
                Integer max = maxEl == null ? null : Integer.valueOf(maxEl.getTextContent().trim());
                return new IntRangeConstraint(min, max);
            }
            case "nTimesXRemainingY": {
                Integer n = Integer.valueOf(XmlUtils.getFirstChildElementsByName(constraintEl, "n").getTextContent().trim());
                String x = XmlUtils.getFirstChildElementsByName(constraintEl, "x").getTextContent().trim();
                String y = XmlUtils.getFirstChildElementsByName(constraintEl, "y").getTextContent().trim();
                return new NTimesXRemainingYElConstraint(n, x, y);
            }
            default:
                throw new IllegalStateException("unknow constraint");
        }
    }


    public static class J2kProfile {

        private final ImageUtilManager imageUtilManager;
        private final Type type;
        private final ImageUtil imageUtil;
        // private Map<String, String> namespacesByPrefix = new HashMap<>();
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


    private static class Validation {
        private final String name;
        private final DataExtraction dataExtraction;
        private final List<DataRule> dataRules;

        public Validation(String name, DataExtraction dataExtraction, List<DataRule> dataRules) {
            this.name = name;
            this.dataExtraction = dataExtraction;
            this.dataRules = dataRules;
        }

        public String validate(Object processedOutput) throws DataExtraction.ExtractionException {
            //System.out.println("validating " + name);
            Object data = dataExtraction.extract(processedOutput);
            for (DataRule rule : dataRules) {
                String result = rule.validate(data);
                if (result != null) {
                    return result;
                }
            }
            return null;
        }
    }

    //TODO: refactoring, moc zanoreni uz
    public static interface DataExtraction {
        Object extract(Object processedOutput) throws ExtractionException;

        static class ExtractionException extends Exception {
            ExtractionException(String message) {
                super(message);
            }
        }
    }

    private static abstract class XmlDataExtraction implements DataExtraction {

        final ExtractionType extractionType;
        private final NamespaceContext namespaceContext;

        protected XmlDataExtraction(ExtractionType extractionType, NamespaceContext namespaceContext) {
            this.extractionType = extractionType;
            this.namespaceContext = namespaceContext;
        }

        XPathExpression buildXpath(String xpathExpression) throws ExtractionException {
            try {
                XPathFactory xPathfactory = XPathFactory.newInstance();
                XPath xpath = xPathfactory.newXPath();
                xpath.setNamespaceContext(namespaceContext);
                return xpath.compile(xpathExpression);
            } catch (XPathExpressionException e) {
                throw new ExtractionException(String.format("chyba v zápisu Xpath '%s': %s", xpathExpression, e.getMessage()));
            }
        }

        Object extractData(XPathExpression xPath, Object processedOutput) throws XPathExpressionException {
            try {
                switch (extractionType) {
                    case STRING: {
                        return xPath.evaluate(processedOutput, XPathConstants.STRING);
                    }
                    case INTEGER: {
                        String dataStr = (String) xPath.evaluate(processedOutput, XPathConstants.STRING);
                        if (dataStr == null) {
                            return null;
                        } else {
                            return Integer.valueOf(dataStr);
                        }
                    }
                    case FLOAT: {
                        String dataStr = (String) xPath.evaluate(processedOutput, XPathConstants.STRING);
                        if (dataStr == null) {
                            return null;
                        } else {
                            return Float.valueOf(dataStr);
                        }
                    }
                    case STRING_LIST: {
                        NodeList nodes = (NodeList) xPath.evaluate(processedOutput, XPathConstants.NODESET);
                        if (nodes == null) {
                            return null;
                        } else {
                            List<String> result = new ArrayList<>(nodes.getLength());
                            for (int i = 0; i < nodes.getLength(); i++) {
                                Node node = nodes.item(i);
                                result.add(node.getTextContent());
                            }
                            return result;
                        }
                    }
                    case INTEGER_LIST: {
                        NodeList nodes = (NodeList) xPath.evaluate(processedOutput, XPathConstants.NODESET);
                        if (nodes == null) {
                            return null;
                        } else {
                            List<Integer> result = new ArrayList<>(nodes.getLength());
                            for (int i = 0; i < nodes.getLength(); i++) {
                                Node node = nodes.item(i);
                                result.add(Integer.valueOf(node.getTextContent()));
                            }
                            return result;
                        }
                    }
                    case FLOAT_LIST: {
                        NodeList nodes = (NodeList) xPath.evaluate(processedOutput, XPathConstants.NODESET);
                        if (nodes == null) {
                            return null;
                        } else {
                            List<Float> result = new ArrayList<>(nodes.getLength());
                            for (int i = 0; i < nodes.getLength(); i++) {
                                Node node = nodes.item(i);
                                result.add(Float.valueOf(node.getTextContent()));
                            }
                            return result;
                        }
                    }
                    default:
                        return null;
                }
            } catch (NumberFormatException e) {
                return null;
            }
        }
    }


    private static class FirstNonemptyDataExctraction extends XmlDataExtraction {
        private final List<String> paths;

        private FirstNonemptyDataExctraction(ExtractionType extractionType, NamespaceContext namespaceContext, List<String> paths) {
            super(extractionType, namespaceContext);
            this.paths = paths;
        }

        @Override
        public Object extract(Object processedOutput) throws ExtractionException {
            String pathForError = null;
            try {
                for (String path : paths) {
                    pathForError = path;
                    XPathExpression xPath = buildXpath(path);
                    Object extractedData = extractData(xPath, processedOutput);
                    if (extractedData != null) {
                        return extractedData;
                    }
                }
                return null;
            } catch (XPathExpressionException e) {
                throw new ExtractionException(String.format("chyba v zápisu Xpath '%s': %s", pathForError, e.getMessage()));
            }
        }
    }

    private static abstract class DataRule {
        private final String validationName;

        public DataRule(String validationName) {
            this.validationName = validationName;
        }

        String error(String message) {
            return String.format("%s: %s", validationName, message);
        }

        abstract String validate(Object data);

        public String toString(Object data) {
            if (data instanceof List) {
                List list = (List) data;
                StringBuilder builder = new StringBuilder();
                builder.append('[');
                for (int i = 0; i < list.size(); i++) {
                    if (i != 0) {
                        builder.append(", ");
                    }
                    builder.append(list.get(i));
                }
                builder.append(']');
                return builder.toString();
            } else {
                return data.toString();
            }
        }
    }

    private static class MustExist extends DataRule {

        public MustExist(String validationName) {
            super(validationName);
        }

        @Override
        public String validate(Object data) {
            if (data == null || data.toString().isEmpty()) {
                return error("požadovaná hodnota nenalezena");
            } else {
                return null;
            }
        }

    }

    private static class MustNotExist extends DataRule {
        public MustNotExist(String validationName) {
            super(validationName);
        }

        @Override
        public String validate(Object data) {
            if (data != null && !data.toString().isEmpty()) {
                return error("zakázaná hodnota nalezena");
            } else {
                return null;
            }
        }
    }

    private static class MustMatch extends DataRule {
        private final Constraint constraint;

        public MustMatch(String validationName, Constraint constraint) {
            super(validationName);
            this.constraint = constraint;
        }

        @Override
        public String validate(Object data) {
            if (!constraint.matches(data)) {
                return error(String.format("hodnota \"%s\" neodpovídá omezení: \"%s\"", toString(data), constraint.toString()));
            } else {
                return null;
            }
        }
    }

    private class MustMatchOne extends DataRule {
        private final List<Constraint> constraints;

        public MustMatchOne(String validationName, List<Constraint> constraints) {
            super(validationName);
            this.constraints = constraints;
        }

        @Override
        public String validate(Object data) {
            for (Constraint constraint : constraints) {
                if (constraint.matches(data)) {
                    return null;
                }
            }
            return error(String.format("hodnota \"%s\" neodpovídá žádnému z omezení: %s", toString(data), toString(constraints)));
        }
    }


    private static interface Constraint {
        boolean matches(Object data);
    }

    private static class ConstantConstraint implements Constraint {
        private final String constant;

        public ConstantConstraint(String constant) {
            this.constant = constant;
        }

        @Override
        public boolean matches(Object data) {
            String dataStr = (String) data;
            return constant.equals(dataStr);
        }

        public String toString() {
            return String.format("musí být přesně '%s'", constant);
        }
    }


    private class FlowRangeConstraint implements Constraint {
        private final Float min;
        private final Float max;


        public FlowRangeConstraint(Float min, Float max) {
            this.min = min;
            this.max = max;
        }

        @Override
        public boolean matches(Object data) {
            if (data == null) {
                return false;
            } else {
                Float value = (Float) data;
                if (min != null && value < min) {
                    return false;
                } else if (max != null && value > max) {
                    return false;
                } else {
                    return true;
                }
            }
        }

        public String toString() {
            if (min != null && max != null) {
                return String.format("musí být v intervalu <%f, %f>", min, max);
            } else if (min != null) {
                return String.format("musí být alespoň %f", min);
            } else if (max != null) {
                return String.format("musí být nejvýše %f", max);
            } else {
                return "musí být číslo (float)";
            }
        }

    }

    private class IntRangeConstraint implements Constraint {
        private final Integer min;
        private final Integer max;

        public IntRangeConstraint(Integer min, Integer max) {
            this.min = min;
            this.max = max;
        }

        @Override
        public boolean matches(Object data) {
            if (data == null) {
                return false;
            } else {
                Integer value = (Integer) data;
                if (min != null && value < min) {
                    return false;
                } else if (max != null && value > max) {
                    return false;
                } else {
                    return true;
                }
            }
        }

        public String toString() {
            if (min != null && max != null) {
                return String.format("musí být v intervalu <%d, %d>", min, max);
            } else if (min != null) {
                return String.format("musí být alespoň %d", min);
            } else if (max != null) {
                return String.format("musí být nejvýše %d", max);
            } else {
                return "musí být číslo (integer)";
            }
        }
    }

    private class NTimesXRemainingYElConstraint implements Constraint {
        private final Integer n;
        private final String x;
        private final String y;

        public NTimesXRemainingYElConstraint(Integer n, String x, String y) {
            this.n = n;
            this.x = x;
            this.y = y;
        }

        @Override
        public boolean matches(Object data) {
            if (data == null) {
                return false;
            } else {
                List<String> list = (List<String>) data;
                int xCounter = 0;
                for (String string : list) {
                    if (string.equals(x)) {
                        if (xCounter == n) {
                            return false;
                        } else {
                            xCounter++;
                        }
                    } else if (string.equals(y)) {
                        //ok
                    } else {
                        //unknown value
                        return false;
                    }
                }
                return true;
            }
        }
    }


}
