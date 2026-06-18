package nkp.pspValidator.shared;

import nkp.pspValidator.shared.engine.Level;
import nkp.pspValidator.shared.engine.Rule;
import nkp.pspValidator.shared.engine.RulesSection;
import nkp.pspValidator.shared.engine.validationFunctions.ValidationProblem;
import nkp.pspValidator.shared.engine.validationFunctions.ValidationResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.util.Date;
import java.util.Map;

/**
 * Created by Martin Řehánek on 15.12.16.
 */
public class ValidatorProtocolXmlBuilder {

    final static String NS = "http://www.nkp.cz/pspValidator/2.7/validationProtocol";
    final static String XSI_NS = XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI;
    final static String XSD_URL = "https://raw.githubusercontent.com/NLCR/komplexni-validator/refs/tags/v2.7/modules/sharedModule/src/main/resources/nkp/pspValidator/shared/validatorConfig/xsd/validationProtocol.xsd";
    //TODO: change XSD_URL to one of:
    //final static String XSD_URL = "https://raw.githubusercontent.com/NLCR/komplexni-validator/v2.6.1/modules/sharedModule/src/main/resources/nkp/pspValidator/shared/validatorConfig/xsd/validationProtocol.xsd";
    //final static String XSD_URL = "https://docs.validator.nkp.cz/xsd/2.6.1/validationProtocol.xsd";

    private void setNamespaceAndSchemaLocation(Element element) {
        element.setAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, "xmlns", NS);
        element.setAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, "xmlns:xsi", XSI_NS);
        element.setAttributeNS(
                XSI_NS,
                "xsi:schemaLocation",
                NS + " " + XSD_URL
        );
    }

    public void buildProfileValidationXmlOutput(File xmlOutputFile, File metadataFile, String profileId, ValidationResult result, long startTime, long finishTime) {
        try {
            if (result == null) {
                return; //no xml output for empty result
            }
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            dbFactory.setNamespaceAware(true);
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.newDocument();

            Element protocolEl = createRootElement(doc, "protocol");
            protocolEl.setAttribute("type", "profile");

            Element validatorEl = createChildElement(protocolEl, "validator");
            validatorEl.setAttribute("version", Version.VERSION_CODE);
            validatorEl.setAttribute("buildDate", convertBuilDate(Version.BUILD_DATE));

            Element profileEl = createChildElement(protocolEl, "profile");
            profileEl.setAttribute("id", profileId);

            Element metadataFileEl = createChildElement(protocolEl, "metadataFile");
            metadataFileEl.setAttribute("parentDir", metadataFile.getParentFile().getAbsolutePath());
            metadataFileEl.setAttribute("fileName", metadataFile.getName());

            Element summaryEl = createChildElement(protocolEl, "summary");
            Long duration = finishTime - startTime;
            Date startDate = new Date(startTime);
            Date finishDate = new Date(finishTime);
            summaryEl.setAttribute("durationMs", duration.toString());
            summaryEl.setAttribute("startTime", String.format("%tFT%<tT", startDate));
            summaryEl.setAttribute("finishTime", String.format("%tFT%<tT", finishDate));

            if (result.hasProblems()) {
                Element problemsEl = createChildElement(protocolEl, "problems");
                problemsEl.setAttribute("total", result.getProblems().size() + "");
                int infoCount = 0;
                int warningCount = 0;
                int errorCount = 0;
                for (ValidationProblem problem : result.getProblems()) {
                    switch (problem.getLevel()) {
                        case INFO:
                            infoCount++;
                            break;
                        case WARNING:
                            warningCount++;
                            break;
                        case ERROR:
                            errorCount++;
                            break;
                    }
                    appendErrorEl("", doc, problemsEl, problem);
                }
                problemsEl.setAttribute("INFO", infoCount + "");
                problemsEl.setAttribute("WARNING", warningCount + "");
                problemsEl.setAttribute("ERROR", errorCount + "");
                summaryEl.setAttribute("verdict", errorCount > 0 ? "INVALID" : "VALID");
            }

            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");

            DOMSource source = new DOMSource(doc);
            StreamResult consoleResult = new StreamResult(xmlOutputFile);
            transformer.transform(source, consoleResult);
        } catch (TransformerException | ParserConfigurationException e) {
            e.printStackTrace();
        }
    }

    private Element createChildElement(Element parent, String elName) {
        Element childEl = parent.getOwnerDocument().createElementNS(NS, elName);
        parent.appendChild(childEl);
        return childEl;
    }

    private Element createRootElement(Document doc, String elName) {
        Element rootEl = doc.createElementNS(NS, elName);
        setNamespaceAndSchemaLocation(rootEl);
        doc.appendChild(rootEl);
        return rootEl;
    }

    public void buildPackageValidationXmlOutput(File xmlOutputFile, ValidationState protocol) {
        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            dbFactory.setNamespaceAware(true);
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.newDocument();

            Element protocolEl = createRootElement(doc, "protocol");
            protocolEl.setAttribute("type", "package");

            Element validatorEl = createChildElement(protocolEl, "validator");
            validatorEl.setAttribute("version", Version.VERSION_CODE);
            validatorEl.setAttribute("buildDate", convertBuilDate(Version.BUILD_DATE));

            Element fDmfEl = createChildElement(protocolEl, "fdmf");
            fDmfEl.setAttribute("type", protocol.getDmfUsed().getType().toString());
            fDmfEl.setAttribute("version", protocol.getDmfUsed().getVersion());

            //package
            Element packageEl = createChildElement(protocolEl, "package");
            //package file
            Element fileEl = createChildElement(packageEl, "file");
            fileEl.setAttribute("fileName", protocol.getPackageFile().getName());
            fileEl.setAttribute("parentDir", protocol.getPackageFile().getParentFile().getAbsolutePath());
            //package INFO
            InfoExtractor.InfoData infoData = protocol.getInfoData();
            if (infoData != null) {
                Element infoEl = createChildElement(packageEl, "info");
                if (infoData.created() != null) {
                    infoEl.setAttribute("created", infoData.created());
                }
                if (infoData.metadataversion() != null) {
                    infoEl.setAttribute("metadataVersion", infoData.metadataversion());
                }
                if (infoData.packageid() != null) {
                    infoEl.setAttribute("packageId", infoData.packageid());
                }
                if (infoData.titleIds() != null) {
                    for (InfoExtractor.TitleId titleId : infoData.titleIds()) {
                        infoEl.setAttribute("titleId_" + titleId.type(), titleId.value());
                    }
                }
                if (infoData.collection() != null) {
                    infoEl.setAttribute("collection", infoData.collection());
                }
                if (infoData.institution() != null) {
                    infoEl.setAttribute("institution", infoData.institution());
                }
                if (infoData.creator() != null) {
                    infoEl.setAttribute("creator", infoData.creator());
                }
                if (infoData.size() != null) {
                    infoEl.setAttribute("sizeKB", infoData.size().toString());
                }
            }

            //validation summary
            Long duration = protocol.getGlobalFinishTime() - protocol.getGlobalStartTime();
            Date startDate = new Date(protocol.getGlobalStartTime());
            Date finishDate = new Date(protocol.getGlobalFinishTime());
            String verdict = protocol.isValid() ? "VALID" : "INVALID";
            Element summaryEl = buildSummaryEl(doc, duration, startDate, finishDate, protocol.getGlobalProblemsTotal(), protocol.getGlobalProblemsByLevel(), verdict);
            protocolEl.appendChild(summaryEl);

            String packageParentPath = protocol.getPackageFile().getParentFile().getAbsolutePath();

            Element sectionsEl = createChildElement(protocolEl, "sections");
            for (RulesSection section : protocol.getSections()) {
                Element sectionEl = createChildElement(sectionsEl, "section");
                sectionEl.setAttribute("name", section.getName());
                if (section.getDescription() != null) {
                    sectionEl.setAttribute("description", section.getDescription());
                }
                if (protocol.sectionWasExecuted(section)) {
                    Element sectionSummaryEl = buildSummaryEl(doc, protocol.getSectionProcessingDuration(section), null, null,
                            protocol.getSectionProblemsTotal(section), protocol.getSectionProblemsByLevel(section), null);
                    sectionEl.appendChild(sectionSummaryEl);
                    for (Rule rule : protocol.getRules(section)) {
                        Element ruleEl = createChildElement(sectionEl, "rule");
                        ruleEl.setAttribute("name", rule.getName());
                        if (rule.getDescription() != null) {
                            ruleEl.setAttribute("description", rule.getDescription());
                        }
                        Element ruleSummaryEl = buildSummaryEl(doc, protocol.getRuleProcessingDuration(rule),
                                null, null, protocol.getRuleProblemsTotal(rule), protocol.getRuleProblemsByLevel(rule), null);
                        ruleEl.appendChild(ruleSummaryEl);
                        if (rule.getResult().hasProblems()) {
                            Element problemsEl = (Element) ruleSummaryEl.getElementsByTagName("problems").item(0);
                            for (ValidationProblem error : rule.getResult().getProblems()) {
                                appendErrorEl(packageParentPath, doc, problemsEl, error);
                            }
                        }
                    }
                }
            }
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");

            DOMSource source = new DOMSource(doc);
            StreamResult consoleResult = new StreamResult(xmlOutputFile);
            transformer.transform(source, consoleResult);
        } catch (TransformerException | ParserConfigurationException e) {
            e.printStackTrace();
        }
    }

    private String convertBuilDate(String buildDate) {
        //convert 7. 11. 2025 to 2025-11-07
        try {
            String[] tokens = buildDate.split("\\.\\s*");
            if (tokens.length == 3) {
                int day = Integer.parseInt(tokens[0]);
                int month = Integer.parseInt(tokens[1]);
                int year = Integer.parseInt(tokens[2]);
                return String.format("%04d-%02d-%02d", year, month, day);
            } else {
                return buildDate;
            }
        } catch (NumberFormatException e) {
            e.printStackTrace();
            return buildDate;
        }
    }

    private void appendErrorEl(String packageParentPath, Document doc, Element problemsEl, ValidationProblem error) {
        //Element problemEl = doc.createElementNS(NS, "problem");
        //problemsEl.appendChild(problemEl);
        Element problemEl = createChildElement(problemsEl, "problem");
        problemEl.setAttribute("level", error.getLevel().name());
        problemEl.setTextContent(error.getSimpleMessage() == null ? error.getFullMessage() : error.getSimpleMessage());
        if (error.getFile() != null) {
            problemEl.setAttribute("file", toPackageRelativePath(packageParentPath, error.getFile()));
        }
        if (error.getXsdFile() != null) {
            problemEl.setAttribute("xsdFile", error.getXsdFile().getAbsolutePath());
        }
        if (error.getExpectedValue() != null) {
            problemEl.setAttribute("expectedValue", error.getExpectedValue());
        }
        if (error.getActualValue() != null) {
            problemEl.setAttribute("actualValue", error.getActualValue());
        }
        if (error.getValueSpec() != null) {
            problemEl.setAttribute("valueSpec", error.getValueSpec());
        }
        if (error.getPattern() != null) {
            problemEl.setAttribute("pattern", error.getPattern());
        }
        if (error.getReferencedFile() != null) {
            problemEl.setAttribute("referencedFile", toPackageRelativePath(packageParentPath, error.getReferencedFile()));
        }
        if (error.getReferencedValue() != null) {
            problemEl.setAttribute("referencedValue", error.getReferencedValue());
        }
        if (error.getParentElementSpec() != null) {
            problemEl.setAttribute("parentElementSpec", error.getParentElementSpec());
        }
        if (error.getAttributeSpec() != null) {
            problemEl.setAttribute("attributeSpec", error.getAttributeSpec());
        }
        if (error.getLabel() != null) {
            problemEl.setAttribute("label", error.getLabel());
        }
        if (error.getElementSpec() != null) {
            problemEl.setAttribute("elementSpec", error.getElementSpec());
        }
        if (error.getToolName() != null) {
            problemEl.setAttribute("toolName", error.getToolName());
        }
        if (error.getErrorDetails() != null) {
            problemEl.setAttribute("errorDetails", error.getErrorDetails());
        }
    }

    private String toPackageRelativePath(String packagePath, File file) {
        String path = file.getAbsolutePath();
        if (path.startsWith(packagePath)) {
            return path.substring(packagePath.length() + 1);
        } else {
            return path;
        }
    }

    private Element buildSummaryEl(Document doc, Long duration, Date startDate, Date finishDate, Integer problemsTotal, Map<Level, Integer> problemsByLevel, String verdict) {
        Element summaryEl = doc.createElementNS(NS, "summary");
        if (duration != null) {
            summaryEl.setAttribute("durationMs", duration.toString());
        }
        if (startDate != null) {
            summaryEl.setAttribute("startTime", String.format("%tFT%<tT", startDate));
        }
        if (finishDate != null) {
            summaryEl.setAttribute("finishTime", String.format("%tFT%<tT", finishDate));
        }

        if (verdict != null) {
            summaryEl.setAttribute("verdict", verdict);
        }

        Element problemsEl = createChildElement(summaryEl, "problems");
        problemsEl.setAttribute("total", problemsTotal.toString());
        for (Level level : problemsByLevel.keySet()) {
            Integer problems = problemsByLevel.get(level);
            if (problems != 0) {
                problemsEl.setAttribute(level.name(), problems.toString());
            }
        }
        return summaryEl;
    }
}
