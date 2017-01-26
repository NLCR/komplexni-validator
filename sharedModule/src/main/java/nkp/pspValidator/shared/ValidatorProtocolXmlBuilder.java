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

            Long duration = protocol.getGlobalFinishTime() - protocol.getGlobalStartTime();
            Date startDate = new Date(protocol.getGlobalStartTime());
            Date finishDAte = new Date(protocol.getGlobalFinishTime());
            String verdict = protocol.isValid() ? "VALID" : "INVALID";
            Element summaryEl = buildSummaryEl(doc, duration, startDate, finishDAte, protocol.getGlobalProblemsTotal(), protocol.getGlobalProblemsByLevel(), verdict);
            protocolEl.appendChild(summaryEl);

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
                                appendErrorEl(doc, problemsEl, error);
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

    private void appendErrorEl(Document doc, Element problemsEl, ValidationProblem error) {
        Element problemEl = doc.createElement("problem");
        problemsEl.appendChild(problemEl);
        problemEl.setAttribute("level", error.getLevel().name());
        problemEl.setTextContent(error.getMessage());
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
