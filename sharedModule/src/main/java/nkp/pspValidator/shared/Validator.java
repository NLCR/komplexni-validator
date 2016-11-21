package nkp.pspValidator.shared;

import nkp.pspValidator.shared.engine.Engine;
import nkp.pspValidator.shared.engine.Level;
import nkp.pspValidator.shared.engine.Rule;
import nkp.pspValidator.shared.engine.RulesSection;
import nkp.pspValidator.shared.engine.validationFunctions.ValidationError;
import nkp.pspValidator.shared.engine.validationFunctions.ValidationResult;
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
import java.util.*;

/**
 * Created by martin on 2.11.16.
 */
public class Validator {

    private final Engine engine;

    public Validator(Engine engine) {
        this.engine = engine;
    }

    /**
     * only for tests
     *
     * @return
     */
    public Engine getEngine() {
        return engine;
    }

    public void run(File xmlOutputFile,
                    boolean printSectionsWithProblems, boolean printSectionsWithoutProblems,
                    boolean printRulesWithProblems, boolean printRulesWithoutProblems) {
        Protocol protocol = new Protocol(engine);
        List<RulesSection> rulesSections = engine.getRuleSections();
        protocol.reportValidationsStart();
        for (RulesSection section : rulesSections) {
            //FIXME: odstranit pro produkci
            //docasne pro testovani jedine sekce
            //if (section.getName().equals("JPEG 2000")) {
            protocol.reportSectionProcessingStarted(section);
            protocol.addSection(section);
            //TODO: tohle se pocita
            int sectionProblemsTotal = protocol.getSectionProblemsSum(section);
            Map<Level, Integer> sectionProblemsByLevel = protocol.getSectionProblemsByLevel(section);
            boolean printSection = sectionProblemsTotal == 0 && printSectionsWithoutProblems || sectionProblemsTotal != 0 && printSectionsWithProblems;
            if (printSection) {
                String sectionTitle = String.format("Sekce %s: %s", section.getName(), buildSummary(sectionProblemsTotal, sectionProblemsByLevel));
                System.out.println();
                System.out.println(sectionTitle);
                System.out.println(buildDelimiter(sectionTitle.length()));
            }
            List<Rule> rules = engine.getRules(section);
            for (Rule rule : rules) {
                protocol.reportRuleProcessingStarted(rule);
                protocol.addRule(section, rule);
                ValidationResult result = rule.getResult();
                boolean printRule = printSection && (result.hasProblems() && printRulesWithProblems || !result.hasProblems() && printRulesWithoutProblems);
                if (printRule) {
                    int ruleProblemsTotal = protocol.getRuleProblemsSum(rule);
                    Map<Level, Integer> ruleProblemsByLevel = protocol.getRuleProblemsByLevel(rule);
                    System.out.println(String.format("Pravidlo %s: %s", rule.getName(), buildSummary(ruleProblemsTotal, ruleProblemsByLevel)));
                    System.out.println(String.format("\t%s", rule.getDescription()));
                    for (ValidationError error : result.getProblems()) {
                        System.out.println(String.format("\t%s: %s", error.getLevel(), error.getMessage()));
                    }
                }
                protocol.reportRuleProcessingFinished(rule);
            }
            protocol.reportSectionProcessingFinished(section);
            //}
        }
        protocol.reportValidationsEnd();
        String verdict = protocol.isValid() ? "validní" : "nevalidní";
        System.out.println(String.format("\nCelkem: %s, balík je: %s", buildSummary(protocol.getTotalProblemsSum(), protocol.getTotalProblemsByLevel()), verdict));
        if (xmlOutputFile != null) {
            System.out.println("Vytvářím xml export do souboru " + xmlOutputFile.getAbsolutePath());
            buildXmlOutput(xmlOutputFile, protocol);
            System.out.println("Xml export vytvořen");
        }
    }

    private void buildXmlOutput(File xmlOutputFile, Protocol protocol) {
        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.newDocument();

            Element protocolEl = doc.createElement("protocol");
            doc.appendChild(protocolEl);

            Long duration = protocol.getFinishTime() - protocol.getStartTime();
            Date startDate = new Date(protocol.getStartTime());
            Date finishDAte = new Date(protocol.getFinishTime());
            String verdict = protocol.isValid() ? "VALID" : "INVALID";
            Element summaryEl = buildSummaryEl(doc, duration, startDate, finishDAte, protocol.getTotalProblemsSum(), protocol.getTotalProblemsByLevel(), verdict);
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
                Element sectionSummaryEl = buildSummaryEl(doc, protocol.getSectionProcessingDuration(section), null, null,
                        protocol.getSectionProblemsSum(section), protocol.getSectionProblemsByLevel(section), null);
                sectionEl.appendChild(sectionSummaryEl);
                for (Rule rule : protocol.getRules(section)) {
                    Element ruleEl = doc.createElement("rule");
                    sectionEl.appendChild(ruleEl);
                    ruleEl.setAttribute("name", rule.getName());
                    if (rule.getDescription() != null) {
                        ruleEl.setAttribute("description", rule.getDescription());
                    }
                    Element ruleSummaryEl = buildSummaryEl(doc, protocol.getRuleProcessingDuration(rule),
                            null, null, protocol.getRuleProblemsSum(rule), protocol.getRuleProblemsByLevel(rule), null);
                    ruleEl.appendChild(ruleSummaryEl);
                    if (rule.getResult().hasProblems()) {
                        Element problemsEl = (Element) ruleSummaryEl.getElementsByTagName("problems").item(0);
                        for (ValidationError error : rule.getResult().getProblems()) {
                            appendErrorEl(doc, problemsEl, error);
                        }
                    }
                }
                protocol.reportSectionProcessingFinished(section);
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


    private void appendErrorEl(Document doc, Element problemsEl, ValidationError error) {
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


    private String buildDelimiter(int length) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < length; i++) {
            builder.append('-');
        }
        return builder.toString();
    }

    private String buildSummary(int totalProblems, Map<Level, Integer> problemsByLevel) {
        StringBuilder builder = new StringBuilder();
        builder.append(StringUtils.declineProblemNumber(totalProblems));
        if (totalProblems != 0) {
            builder.append(" (");
            boolean first = true;
            for (int i = 0; i < Level.values().length; i++) {
                Level level = Level.values()[i];
                int count = problemsByLevel.get(level);
                if (count != 0) {
                    if (!first) {
                        builder.append(", ");
                    }
                    builder.append(String.format("%dx %s", count, level));
                    first = false;
                }
            }
            builder.append(")");
        }
        return builder.toString();

    }

    public static class Protocol {

        private final Engine engine;

        private long startTime;
        private long finishTime;

        //total
        private int totalProblems;
        private Map<Level, Integer> totalProblemsByLevel;
        private boolean valid;
        //sections
        private final List<RulesSection> sections = new ArrayList<>();
        private final Map<RulesSection, Map<Level, Integer>> sectionProblemsByLevel = new HashMap<>();
        private final Map<RulesSection, Integer> sectionProblemsTotal = new HashMap<>();
        private final Map<RulesSection, Long> startTimeBySection = new HashMap<>();
        private final Map<RulesSection, Long> finishTimeBySection = new HashMap<>();
        //rules
        private Map<RulesSection, List<Rule>> rulesBySection = new HashMap<>();
        private final Map<Rule, Map<Level, Integer>> ruleProblemsByLevel = new HashMap<>();
        private final Map<Rule, Integer> ruleProblemsTotal = new HashMap<>();
        private final Map<Rule, Long> startTimeByRule = new HashMap<>();
        private final Map<Rule, Long> finishTimeByRule = new HashMap<>();

        public Protocol(Engine engine) {
            this.engine = engine;
        }

        public void addSection(RulesSection section) {
            sections.add(section);
            Map<Level, Integer> problemsByLevel = computeTotalProblemsByLevel(section);
            sectionProblemsByLevel.put(section, problemsByLevel);
            int totalProblems = computeTotalProblems(problemsByLevel);
            sectionProblemsTotal.put(section, totalProblems);
        }

        public void addRule(RulesSection section, Rule rule) {
            List<Rule> rules = rulesBySection.get(section);
            if (rules == null) {
                rules = new ArrayList<>();
                rulesBySection.put(section, rules);
            }
            rules.add(rule);
            Map<Level, Integer> problemsByLevel = computeTotalProblemsByLevel(rule);
            int totalProblems = computeTotalProblems(problemsByLevel);
            ruleProblemsByLevel.put(rule, problemsByLevel);
            ruleProblemsTotal.put(rule, totalProblems);
        }

        public void reportSectionProcessingStarted(RulesSection section) {
            startTimeBySection.put(section, System.currentTimeMillis());
        }

        public void reportSectionProcessingFinished(RulesSection section) {
            finishTimeBySection.put(section, System.currentTimeMillis());
        }

        public void reportRuleProcessingStarted(Rule rule) {
            startTimeByRule.put(rule, System.currentTimeMillis());
        }

        public void reportRuleProcessingFinished(Rule rule) {
            finishTimeByRule.put(rule, System.currentTimeMillis());
        }

        public void reportValidationsStart() {
            startTime = System.currentTimeMillis();
        }

        public void reportValidationsEnd() {
            finishTime = System.currentTimeMillis();
            totalProblemsByLevel = computeTotalProblemsByLevel(sections);
            totalProblems = computeTotalProblems(totalProblemsByLevel);
            Integer errors = totalProblemsByLevel.get(Level.ERROR);
            valid = errors == 0;
        }


        private Map<Level, Integer> computeTotalProblemsByLevel(Rule rule) {
            Map<Level, Integer> problemsByLevel = new HashMap<>();
            for (Level level : Level.values()) {
                problemsByLevel.put(level, 0);
            }
            if (rule.getResult().hasProblems()) {
                for (ValidationError error : rule.getResult().getProblems()) {
                    Integer counter = problemsByLevel.get(error.getLevel());
                    problemsByLevel.put(error.getLevel(), ++counter);
                }

            }
            return problemsByLevel;
        }

        private Map<Level, Integer> computeTotalProblemsByLevel(RulesSection section) {
            Map<Level, Integer> problemsByLevel = new HashMap<>();
            for (Level level : Level.values()) {
                problemsByLevel.put(level, 0);
            }
            for (Rule rule : engine.getRules(section)) {
                for (ValidationError error : rule.getResult().getProblems()) {
                    Integer count = problemsByLevel.get(error.getLevel());
                    problemsByLevel.put(error.getLevel(), ++count);
                }
            }
            return problemsByLevel;
        }

        private int computeTotalProblems(Map<Level, Integer> problemsByLevel) {
            int counter = 0;
            for (Integer counterByLevel : problemsByLevel.values()) {
                counter += counterByLevel;
            }
            return counter;
        }


        private Map<Level, Integer> computeTotalProblemsByLevel(List<RulesSection> rulesSections) {
            Map<Level, Integer> problemsByLevel = new HashMap<>();
            for (Level level : Level.values()) {
                problemsByLevel.put(level, 0);
            }
            for (RulesSection section : rulesSections) {
                for (Rule rule : engine.getRules(section)) {
                    for (ValidationError error : rule.getResult().getProblems()) {
                        Integer count = problemsByLevel.get(error.getLevel());
                        problemsByLevel.put(error.getLevel(), ++count);
                    }
                }
            }
            return problemsByLevel;
        }

        public int getSectionProblemsSum(RulesSection section) {
            return sectionProblemsTotal.get(section);
        }

        public Map<Level, Integer> getSectionProblemsByLevel(RulesSection section) {
            return sectionProblemsByLevel.get(section);
        }

        public boolean isValid() {
            return valid;
        }

        public Integer getTotalProblemsSum() {
            return totalProblems;
        }

        public Map<Level, Integer> getTotalProblemsByLevel() {
            return totalProblemsByLevel;
        }

        public long getStartTime() {
            return startTime;
        }

        public long getFinishTime() {
            return finishTime;
        }

        public List<RulesSection> getSections() {
            return sections;
        }

        public List<Rule> getRules(RulesSection section) {
            return rulesBySection.get(section);
        }

        public int getRuleProblemsSum(Rule rule) {
            return ruleProblemsTotal.get(rule);
        }

        public Map<Level, Integer> getRuleProblemsByLevel(Rule rule) {
            return ruleProblemsByLevel.get(rule);
        }

        public long getSectionProcessingDuration(RulesSection section) {
            return finishTimeBySection.get(section) - startTimeBySection.get(section);
        }

        public long getRuleProcessingDuration(Rule rule) {
            return finishTimeByRule.get(rule) - startTimeByRule.get(rule);
        }


    }
}
