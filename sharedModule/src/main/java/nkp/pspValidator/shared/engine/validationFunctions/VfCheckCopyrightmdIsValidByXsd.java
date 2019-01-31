package nkp.pspValidator.shared.engine.validationFunctions;

import nkp.pspValidator.shared.XmlUtils;
import nkp.pspValidator.shared.XsdImportsResourceResolver;
import nkp.pspValidator.shared.engine.Engine;
import nkp.pspValidator.shared.engine.Level;
import nkp.pspValidator.shared.engine.ValueEvaluation;
import nkp.pspValidator.shared.engine.ValueType;
import nkp.pspValidator.shared.engine.exceptions.ContractException;
import nkp.pspValidator.shared.engine.exceptions.EmptyParamEvaluationException;
import nkp.pspValidator.shared.engine.exceptions.InvalidXPathExpressionException;
import nkp.pspValidator.shared.engine.exceptions.XmlFileParsingException;
import nkp.pspValidator.shared.engine.params.ValueParam;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


/**
 * Created by Martin Řehánek on 27.10.16.
 */
public class VfCheckCopyrightmdIsValidByXsd extends ValidationFunction {

    public static final String PARAM_XSD_FILE = "xsd_file";
    public static final String PARAM_METS_FILES = "mets_files";
    public static final String PARAM_METS_FILE = "mets_file";
    public static final String PARAM_LEVEL = "level";

    public VfCheckCopyrightmdIsValidByXsd(String name, Engine engine) {
        super(name, engine, new Contract()
                .withValueParam(PARAM_XSD_FILE, ValueType.FILE, 1, 1)
                .withValueParam(PARAM_LEVEL, ValueType.LEVEL, 0, 1)
                .withValueParam(PARAM_METS_FILES, ValueType.FILE_LIST, 0, null)
                .withValueParam(PARAM_METS_FILE, ValueType.FILE, 0, null)
        );
    }

    @Override
    public ValidationResult validate() {
        try {
            checkContractCompliance();

            ValueEvaluation paramXsdFile = valueParams.getParams(PARAM_XSD_FILE).get(0).getEvaluation();
            File xsdFile = (File) paramXsdFile.getData();
            if (xsdFile == null) {
            } else if (!xsdFile.exists()) {
                return singlErrorResult(invalidFileDoesNotExist(xsdFile));
            } else if (xsdFile.isDirectory()) {
                return singlErrorResult(invalidFileIsDir(xsdFile));
            } else if (!xsdFile.canRead()) {
                return singlErrorResult(invalidCannotReadFile(xsdFile));
            }

            Set<File> metsFiles = mergeAbsolutFilesFromParams();

            Level level = Level.ERROR;
            List<ValueParam> paramsLevel = valueParams.getParams(PARAM_LEVEL);
            if (!paramsLevel.isEmpty()) {
                ValueParam paramLevel = paramsLevel.get(0);
                ValueEvaluation evaluation = paramLevel.getEvaluation();
                if (evaluation.getData() == null) {
                    return invalidValueParamNull(PARAM_LEVEL, evaluation);
                } else {
                    level = (Level) evaluation.getData();
                }
            }

            return validate(metsFiles, xsdFile, level);
        } catch (ContractException e) {
            return invalidContractNotMet(e);
        } catch (Throwable e) {
            return invalidUnexpectedError(e);
        }
    }

    private Set<File> mergeAbsolutFilesFromParams() throws EmptyParamEvaluationException {
        Set<File> result = new HashSet<>();
        List<ValueParam> fileParams = valueParams.getParams(PARAM_METS_FILE);
        for (ValueParam param : fileParams) {
            ValueEvaluation evaluation = param.getEvaluation();
            File file = (File) evaluation.getData();
            if (file == null) {
                throw new EmptyParamEvaluationException(PARAM_METS_FILE, evaluation);
            }
            result.add(file.getAbsoluteFile());
        }
        List<ValueParam> filesParams = valueParams.getParams(PARAM_METS_FILES);
        for (ValueParam param : filesParams) {
            ValueEvaluation evaluation = param.getEvaluation();
            List<File> files = (List<File>) evaluation.getData();
            if (files == null) {
                throw new EmptyParamEvaluationException(PARAM_METS_FILES, evaluation);
            }
            for (File file : files) {
                result.add(file.getAbsoluteFile());
            }
        }
        return result;
    }

    private ValidationResult validate(Set<File> metsFiles, File xsdFile, Level level) {
        ValidationResult result = new ValidationResult();
        for (File metsFile : metsFiles) {
            validate(metsFile, xsdFile, level, result);
        }
        return result;
    }

    private void validate(File metsFile, File xsdFile, Level level, ValidationResult result) {
        try {
            Document metsDoc = engine.getXmlDocument(metsFile, true);
            NodeList rightsMdEls = (NodeList) engine.buildXpath("//mets:rightsMD").evaluate(metsDoc, XPathConstants.NODESET);
            for (int i = 0; i < rightsMdEls.getLength(); i++) {
                Element rightsMdEl = (Element) rightsMdEls.item(i);
                validate(rightsMdEl, metsFile, xsdFile, level, result);
            }
        } catch (XmlFileParsingException e) {
            result.addError(invalid(level, "%s: %s", metsFile.getName(), e.getMessage()));
        } catch (InvalidXPathExpressionException e) {
            result.addError(invalid(level, "%s: %s", metsFile.getName(), e.getMessage()));
        } catch (XPathExpressionException e) {
            result.addError(invalid(level, "%s: %s", metsFile.getName(), e.getMessage()));
        }
    }

    private void validate(Element rightsMdEl, File metsFile, File xsdFile, Level level, ValidationResult result) {
        String id = rightsMdEl.getAttribute("ID");
        try {
            String copyrightXpath = "mets:mdWrap/mets:xmlData/cmd:copyright";
            Element copyrightEl = (Element) engine.buildXpath(copyrightXpath).evaluate(rightsMdEl, XPathConstants.NODE);
            if (copyrightEl == null) {
                result.addError(invalid(Level.WARNING, "%s: %s: nenalezen element %s", metsFile.getName(), id, copyrightXpath));
            } else {
                Document doc = XmlUtils.elementToNewDocument(copyrightEl, true);
                DOMSource source = new DOMSource(doc);
                SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
                schemaFactory.setResourceResolver(new XsdImportsResourceResolver(xsdFile.getParentFile()));
                Schema schema = schemaFactory.newSchema(xsdFile);
                Validator validator = schema.newValidator();
                validator.validate(source);
            }
        } catch (InvalidXPathExpressionException e) {
            result.addError(invalid(level, "%s: %s: %s", metsFile.getName(), id, e.getMessage()));
        } catch (XPathExpressionException e) {
            result.addError(invalid(level, "%s: %s: %s", metsFile.getName(), id, e.getMessage()));
        } catch (ParserConfigurationException e) {
            result.addError(invalid(level, "%s: %s: %s", metsFile.getName(), id, e.getMessage()));
        } catch (SAXException e) {
            result.addError(invalid(level, "%s: %s: %s", metsFile.getName(), id, e.getMessage()));
        } catch (IOException e) {
            result.addError(invalid(level, "%s: %s: %s", metsFile.getName(), id, e.getMessage()));
        }
    }

}
