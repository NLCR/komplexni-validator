package nkp.pspValidator.shared;

import nkp.pspValidator.shared.engine.Level;
import nkp.pspValidator.shared.engine.Rule;
import nkp.pspValidator.shared.engine.RulesSection;
import nkp.pspValidator.shared.engine.validationFunctions.ValidationProblem;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.util.Date;
import java.util.Map;

/**
 * Created by Martin Řehánek on 15.12.16.
 */
public class ValidatorProtocolXmlBuilder {

    public void buildXmlOutput(File xmlOutputFile, ValidationState protocol) {
        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.newDocument();

            Element protocolEl = doc.createElement("protocol");
            doc.appendChild(protocolEl);

            Element validatorEl = doc.createElement("validator");
            protocolEl.appendChild(validatorEl);
            validatorEl.setAttribute("version", Version.VERSION_CODE);
            validatorEl.setAttribute("buildDate", Version.BUILD_DATE);

            Element fDmfEl = doc.createElement("fdmf");
            protocolEl.appendChild(fDmfEl);
            fDmfEl.setAttribute("type", protocol.getDmfUsed().getType().toString());
            fDmfEl.setAttribute("version", protocol.getDmfUsed().getVersion());

            //package
            Element packageEl = doc.createElement("package");
            protocolEl.appendChild(packageEl);
            //package file
            Element fileEl = doc.createElement("file");
            packageEl.appendChild(fileEl);
            fileEl.setAttribute("fileName", protocol.getPackageFile().getName());
            fileEl.setAttribute("parentDir", protocol.getPackageFile().getParentFile().getAbsolutePath());
            //package INFO
            InfoExtractor.InfoData infoData = protocol.getInfoData();
            if (infoData != null) {
                Element infoEl = doc.createElement("info");
                packageEl.appendChild(infoEl);
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
                    infoEl.setAttribute("size", infoData.size().toString());
                }
            }

            //validation summary
            Long duration = protocol.getGlobalFinishTime() - protocol.getGlobalStartTime();
            Date startDate = new Date(protocol.getGlobalStartTime());
            Date finishDAte = new Date(protocol.getGlobalFinishTime());
            String verdict = protocol.isValid() ? "VALID" : "INVALID";
            Element summaryEl = buildSummaryEl(doc, duration, startDate, finishDAte, protocol.getGlobalProblemsTotal(), protocol.getGlobalProblemsByLevel(), verdict);
            protocolEl.appendChild(summaryEl);

            String packageParentPath = protocol.getPackageFile().getParentFile().getAbsolutePath();

            Element sectionsEl = doc.createElement("sections");
            protocolEl.appendChild(sectionsEl);
            for (RulesSection section : protocol.getSections()) {
                Element sectionEl = doc.createElement("section");
                sectionsEl.appendChild(sectionEl);
                sectionEl.setAttribute("name", section.getName());
                if (section.getDescription() != null) {
                    sectionEl.setAttribute("description", section.getDescription());
                }
                if (protocol.sectionWasExecuted(section)) {
                    Element sectionSummaryEl = buildSummaryEl(doc, protocol.getSectionProcessingDuration(section), null, null,
                            protocol.getSectionProblemsTotal(section), protocol.getSectionProblemsByLevel(section), null);
                    sectionEl.appendChild(sectionSummaryEl);
                    for (Rule rule : protocol.getRules(section)) {
                        Element ruleEl = doc.createElement("rule");
                        sectionEl.appendChild(ruleEl);
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
            DOMSource source = new DOMSource(doc);
            StreamResult consoleResult = new StreamResult(xmlOutputFile);
            transformer.transform(source, consoleResult);
        } catch (TransformerConfigurationException e) {
            e.printStackTrace();
        } catch (TransformerException e) {
            e.printStackTrace();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }
    }

    private void appendErrorEl(String packageParentPath, Document doc, Element problemsEl, ValidationProblem error) {
        Element problemEl = doc.createElement("problem");
        problemsEl.appendChild(problemEl);
        problemEl.setAttribute("_level", error.getLevel().name());
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
    }

    private String toPackageRelativePath(String packagePath, File file) {
        String path = file.getAbsolutePath();
        if (path.startsWith(packagePath)) {
            return path.substring(packagePath.length() + 1);
        } else {
            return path;
        }
    }

    private Element buildSummaryEl(Document doc, Long duration, Date startDate, Date finishDate, Integer problemsTotal, Map<Level, Integer> problemsByLevel, String vertict) {
        Element summaryEl = doc.createElement("summary");
        if (duration != null) {
            summaryEl.setAttribute("duration", String.format("%d ms", duration));
        }
        if (startDate != null) {
            summaryEl.setAttribute("startTime", startDate.toString());
        }
        if (finishDate != null) {
            summaryEl.setAttribute("finishTime", finishDate.toString());
        }

        if (vertict != null) {
            summaryEl.setAttribute("verdict", vertict);
        }

        Element problemsEl = doc.createElement("problems");
        summaryEl.appendChild(problemsEl);
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
