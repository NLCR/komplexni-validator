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
import javax.xml.transform.dom.DOMSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


/**
 * Created by Martin Řehánek on 27.10.16.
 */
public class VfCheckPremisIsValidByXsd extends ValidationFunction {

    public static final String PARAM_XSD_FILE = "xsd_file";
    public static final String PARAM_METS_FILES = "mets_files";
    public static final String PARAM_LEVEL = "level";

    public VfCheckPremisIsValidByXsd(String name, Engine engine) {
        super(name, engine, new Contract()
                .withValueParam(PARAM_XSD_FILE, ValueType.FILE, 1, 1)
                .withValueParam(PARAM_LEVEL, ValueType.LEVEL, 0, 1)
                .withValueParam(PARAM_METS_FILES, ValueType.FILE_LIST, 0, null)
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

            List<File> metsFiles = new ArrayList<>();
            List<ValueParam> metsFilesParams = valueParams.getParams(PARAM_METS_FILES);
            for (ValueParam param : metsFilesParams) {
                ValueEvaluation evaluation = param.getEvaluation();
                List<File> files = (List<File>) evaluation.getData();
                if (files == null) {
                    throw new EmptyParamEvaluationException(PARAM_METS_FILES, evaluation);
                }
                for (File file : files) {
                    metsFiles.add(file.getAbsoluteFile());
                }
            }

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
            e.printStackTrace();
            return invalidUnexpectedError(e);

        }
    }

    private ValidationResult validate(List<File> metsFiles, File xsdFile, Level level) {
        ValidationResult result = new ValidationResult();
        for (File metsFile : metsFiles) {
            validate(metsFile, "mets:techMD", "OBJ_", xsdFile, level, result);
            validate(metsFile, "mets:digiprovMD", "EVT_", xsdFile, level, result);
            validate(metsFile, "mets:digiprovMD", "AGENT_", xsdFile, level, result);
        }
        return result;
    }

    private void validate(File metsFile, String amdSecElement, String idPrefix, File xsdFile, Level level, ValidationResult result) {
        try {
            Document metsDoc = engine.getXmlDocument(metsFile, true);
            String xpathStr = String.format("/mets:mets/mets:amdSec/%s[starts-with(@ID,'%s')]", amdSecElement, idPrefix);
            NodeList techMdEls = (NodeList) engine.buildXpath(xpathStr).evaluate(metsDoc, XPathConstants.NODESET);
            for (int i = 0; i < techMdEls.getLength(); i++) {
                Element techMdEl = (Element) techMdEls.item(i);
                validate(techMdEl, metsFile, xsdFile, level, result);
            }
        } catch (XmlFileParsingException | InvalidXPathExpressionException | XPathExpressionException e) {
            result.addError(new ValidationProblem(level, e.getMessage())
                    .withFile(metsFile)
                    .withXsdFile(xsdFile)
                    .withLabel(amdSecElement)
                    .withSimpleMessage(e.getMessage())
            );
        }
    }

    private void validate(Element techMdEl, File metsFile, File xsdFile, Level level, ValidationResult result) {
        String id = techMdEl.getAttribute("ID");
        try {
            String xpathStr = "mets:mdWrap/mets:xmlData/*[1]";
            XPathExpression xPath = engine.buildXpath(xpathStr);
            Element premisEl = (Element) xPath.evaluate(techMdEl, XPathConstants.NODE);

            if (premisEl == null) {
                result.addError(new ValidationProblem(level, String.format("%s: nenalezen element %s", id, xpathStr))
                        .withFile(metsFile)
                        .withXsdFile(xsdFile)
                        .withLabel(id)
                        .withSimpleMessage("element nenalezen")
                        .withReferencedValue(xpathStr)
                );
                return;
            }

            // vytvoření nového dokumentu
            Document mixDoc = XmlUtils.newDocument(true);

            // hluboký import elementu do nového dokumentu
            Element importedPremisEl = (Element) mixDoc.importNode(premisEl, true);
            mixDoc.appendChild(importedPremisEl);

            // explicitní doplnění namespace deklarací potřebných pro validaci QName v xsi:type
            ensureNamespaceDeclaration(importedPremisEl, "premis", "info:lc/xmlns/premis-v2");
            ensureNamespaceDeclaration(importedPremisEl, "xsi", XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI);

            // volitelně: pokud by PREMIS data používala i další prefixy, lze je doplnit stejně
            // ensureNamespaceDeclaration(importedPremisEl, "mix", "http://www.loc.gov/mix/v20");

            DOMSource source = new DOMSource(mixDoc);

            SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            schemaFactory.setResourceResolver(new XsdImportsResourceResolver(xsdFile.getParentFile()));
            Schema schema = schemaFactory.newSchema(xsdFile);

            Validator validator = schema.newValidator();
            validator.validate(source);

        } catch (InvalidXPathExpressionException
                 | XPathExpressionException
                 | SAXException
                 | IOException e) {
            result.addError(new ValidationProblem(level, String.format("%s: %s", id, e.getMessage()))
                    .withFile(metsFile)
                    .withXsdFile(xsdFile)
                    .withLabel(id)
                    .withSimpleMessage(e.getMessage())
            );
        }
    }

    private void ensureNamespaceDeclaration(Element element, String prefix, String namespaceUri) {
        String attrName = prefix == null || prefix.isEmpty() ? "xmlns" : "xmlns:" + prefix;

        if (prefix == null || prefix.isEmpty()) {
            String current = element.getAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, "xmlns");
            if (current == null || current.isEmpty()) {
                element.setAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, attrName, namespaceUri);
            }
        } else {
            String current = element.getAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, prefix);
            if (current == null || current.isEmpty()) {
                element.setAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, attrName, namespaceUri);
            }
        }
    }

}
