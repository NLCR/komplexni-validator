package nkp.pspValidator.shared.engine;

import nkp.pspValidator.shared.biblio.BiblioTemplatesManager;
import nkp.pspValidator.shared.biblio.TechnicalTemplatesManager;
import nkp.pspValidator.shared.engine.evaluationFunctions.*;
import nkp.pspValidator.shared.engine.exceptions.InvalidXPathExpressionException;
import nkp.pspValidator.shared.engine.exceptions.ValidatorConfigurationException;
import nkp.pspValidator.shared.engine.exceptions.XmlFileParsingException;
import nkp.pspValidator.shared.engine.validationFunctions.*;
import nkp.pspValidator.shared.imageUtils.validation.ImageValidator;
import org.w3c.dom.Document;

import javax.xml.xpath.XPathExpression;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Martin Řehánek on 20.10.16.
 */
public class Engine {

    private final Map<String, ValueDefinition> valueDefinitionsByVarName = new HashMap<>();
    private final Map<String, ValueEvaluation> valueEvaluationsByVarName = new HashMap<>();

    private final Map<String, PatternDefinition> patternDefinitionsByVarName = new HashMap<>();
    private final Map<String, PatternEvaluation> patternEvaluationsByVarName = new HashMap<>();


    private final ProvidedVarsManager providedVarsManager = new ProvidedVarsManager();
    private final ConfigProcessor configProcessor = new ConfigProcessor();
    private final XmlManager xmlManager = new XmlManager(true);

    private final RulesManager rulesManager = new RulesManager();
    private final ImageValidator imageValidator;

    private BiblioTemplatesManager biblioMgr;
    private TechnicalTemplatesManager technicalTemplatesManager;

    public BiblioTemplatesManager getBiblioMgr() {
        return biblioMgr;
    }

    public void setBiblioMgr(BiblioTemplatesManager biblioMgr) {
        this.biblioMgr = biblioMgr;
    }

    public TechnicalTemplatesManager getTechnicalTemplatesManager() {
        return technicalTemplatesManager;
    }

    public void setTechnicalTemplatesManager(TechnicalTemplatesManager technicalTemplatesManager) {
        this.technicalTemplatesManager = technicalTemplatesManager;
    }

    public Engine(ImageValidator imageValidator) {
        this.imageValidator = imageValidator;
    }

    //provided vars

    public void setProvidedString(String stringId, String value) {
        providedVarsManager.addString(stringId, value);
    }

    public void setProvidedInteger(String intId, Integer value) {
        providedVarsManager.addInteger(intId, value);
    }

    public void setProvidedFile(String fileId, File value) {
        providedVarsManager.addFile(fileId, value);
    }

    //config files
    public void processConfigFile(File configFile) throws ValidatorConfigurationException {
        configProcessor.processConfigFile(this, configFile);
    }

    //build methods for creating stuff

    public ValueDefinition buildValueDefinition(ValueType type, EvaluationFunction evaluationFunction) {
        ValueDefinition definition = new ValueDefinition(type, evaluationFunction);
        return definition;
    }

    public PatternDefinition buildPatternDefinition() {
        return new PatternDefinition(this);
    }

    public PatternDefinition buildPatternDefinition(PatternExpression... expressions) {
        PatternDefinition patternDefinition = new PatternDefinition(this);
        for (PatternExpression expression : expressions) {
            patternDefinition.withRawExpression(expression);
        }
        return patternDefinition;
    }

    public EvaluationFunction buildEvaluationFunction(String name) {
        switch (name) {
            case "getProvidedFile":
                return new EfGetProvidedFile(this);
            case "getProvidedString":
                return new EfGetProvidedString(this);
            case "getProvidedInteger":
                return new EfGetProvidedInteger(this);
            case "getFirstFileFromFileList":
                return new EfGetFirstFileFromFileList(this);
            case "findFilesInDirByPattern":
                return new EfFindFilesInDirByPattern(this);
            case "getStringByXpath":
                return new EfGetStringByXpath(this);
            case "getStringListByXpath":
                return new EfGetStringListByXpath(this);
            case "getIdentifiersFromInfoFile":
                return new EfGetIdentifiersFromInfoFile(this);
            case "buildListOfStrings":
                return new EfBuildListOfStrings(this);
            case "filterIdentifersByTypes":
                return new EfFilterIdentifersByTypes(this);
            case "getFileListByXpath":
                return new EfGetFileListByXpath(this);
            case "getDcIdentifiersForEachDmdsecId":
                return new EfGetDcIdentifiersForEachDmdsecId(this);
            case "getModsIdentifiersForEachDmdsecId":
                return new EfGetModsIdentifiersForEachDmdsecId(this);
            case "mergeIdentifiers":
                return new EfMergeIdentifiers(this);
            default:
                throw new RuntimeException(String.format("vyhodnocovací funkce %s neexistuje", name));
        }
    }

    public ValidationFunction buildValidationFunction(String name) throws ValidatorConfigurationException {
        switch (name) {
            case "checkFilelistHasExactSize":
                return new VfCheckFileListHasExactSize(this);
            case "checkFilenameMatchesPattern":
                return new VfCheckFilenameMatchesPattern(this);
            case "checkAllFilenamesMatchPattern":
                return new VfCheckAllFilenamesMatchPattern(this);
            case "checkFileIsDir":
                return new VfCheckFileIsDir(this);
            case "checkFileIsNotDir":
                return new VfCheckFileIsNotDir(this);
            case "checkNoFileIsDir":
                return new VfCheckNoFileIsDir(this);
            case "checkNoOtherFilesInDir":
                return new VfCheckNoOtherFilesInDir(this);
            case "checkAllFileListsHaveSameSize":
                return new VfCheckAllFileListsHaveSameSize(this);
            case "checkFilenamesLengthsSame":
                return new VfCheckFilenamesLengthsSame(this);
            case "checkChecksumFileGeneratedByGrammar":
                return new VfCheckChecksumFileGeneratedByGrammar(this);
            case "checkChecksumFileAllPathsMatchFiles":
                return new VfCheckChecksumFileAllPathsMatchFiles(this);
            case "checkChecksumFileAllChecksumsMatch":
                return new VfCheckChecksumFileAllChecksumsMatch(this);
            case "checkXmlIsWellBuilt":
                return new VfCheckXmlIsWellBuilt(this);
            case "checkXmlIsValidByXsd":
                return new VfCheckXmlIsValidByXsd(this);
            case "checkInfoFileReferencesPrimaryMets":
                return new VfCheckInfoFileReferencesPrimaryMets(this);
            case "checkInfoFileItemsCountMatchesItemtotal":
                return new VfCheckInfoFileItemsCountMatchesItemtotal(this);
            case "checkInfoFileItemlistReferencesAllFiles":
                return new VfCheckInfoFileItemlistReferencesAllFiles(this);
            case "checkInfoFileChecksumMatches":
                return new VfCheckInfoFileChecksumMatches(this);

            //identifiers in general
            case "checkIdentifiersNoDuplicateTypes":
                return new VfCheckIdentifiersNoDuplicateTypes(this);
            case "checkIdentifiersNoDuplicates":
                return new VfCheckIdentifiersNoDuplicates(this);
            case "checkIdentifiersAllTypesPresent":
                return new VfCheckIdentifiersAllTypesPresent(this);
            case "checkIdentifiersNoneTypePresent":
                return new VfCheckIdentifiersNoneTypePresent(this);
            case "checkDcIdentifiersDoNotContainWhiteSpaces":
                return new VfCheckDcIdentifiersDoNotContainWhiteSpaces(this);
            case "checkStringDerivedFromOneOfIdentifiers":
                return new VfCheckStringDerivedFromOneOfIdentifiers(this);
            case "checkDcIdentifiersHaveTypeAndValue":
                return new VfCheckDcIdentifiersHaveTypeAndValue(this);
            case "checkPrimaryMetsFilesecContainsAllFilegroups":
                return new VfCheckPrimaryMetsFilesecContainsAllFilegroups(this);
            case "checkPrimaryMetsDcIdentifiersMatchModsIdentifiers":
                return new VfCheckPrimaryMetsDcIdentifiersMatchModsIdentifiers(this);
            case "checkFileListsMatch":
                return new VfCheckFileListsMatch(this);
            case "checkMetsFilesecSizesMatch":
                return new VfCheckMetsFilesecSizesMatch(this);
            case "checkMetsFilesecChecksumsMatch":
                return new VfCheckMetsFilesecChecksumsMatch(this);

            case "checkSecondaryMetsFilesecContainsAllFilegroups":
                return new VfCheckSecondaryMetsFilesecContainsAllFilegroups(this);
            case "checkSecondaryMetsFilegroupReferencesSingleFile":
                return new VfCheckSecondaryMetsFilegroupReferencesSingleFile(this);
            case "checkSecondaryMetsAmdsecElementsMatchProfile":
                return new VfCheckSecondaryMetsAmdsecElementsMatchProfile(this);

            case "checkImageFilesValidByExternalUtil":
                return new VfCheckImageFilesValidByExternalUtil(this);

            //bibliographic metadata
            case "checkBibliographicMetadataMatchProfile":
                return new VfCheckBibliographicMetadataMatchProfile(this);
            case "checkMixIsValidByXsd":
                return new VfCheckMixIsValidByXsd(this);
            case "checkPremisIsValidByXsd":
                return new VfCheckPremisIsValidByXsd(this);
            case "checkCopyrightMdIsValidByXsd":
                return new VfCheckCopyrightmdIsValidByXsd(this);

            default:
                throw new ValidatorConfigurationException(String.format("validační funkce %s neexistuje", name));
        }
    }

    public RulesSection buildRuleSection(Integer id, String sectionName, String description) {
        return new RulesSection(id, sectionName, description);
    }

    @Deprecated
    public Rule buildRule(int sectionId, int ruleId, String ruleName, String description, ValidationFunction function) {
        return new Rule(sectionId, ruleId, ruleName, description, function);
    }

    //register methods for defining variables (values, patterns, ruleSections, rules)

    public void registerValue(String valueVariableName, ValueEvaluation valueEvaluation) {
        //TODO: check if not defined already
        valueEvaluationsByVarName.put(valueVariableName, valueEvaluation);
    }

    public void registerValueDefinition(String valueVariableName, ValueDefinition definition) {
        //TODO: check if not defined already
        valueDefinitionsByVarName.put(valueVariableName, definition);
    }

    public void registerPatternDefinition(String patternName, PatternDefinition definition) {
        //TODO: check if not defined already
        //nejlepe ValidatorConfigurationException
        patternDefinitionsByVarName.put(patternName, definition);
    }

    public void registerRuleSection(RulesSection section) throws ValidatorConfigurationException {
        //TODO: check if not defined already
        //nejlepe ValidatorConfigurationException
        rulesManager.addSection(section);
    }

    public void registerRule(RulesSection section, Rule rule) throws ValidatorConfigurationException {
        //TODO: check if not defined already
        //nejlepe ValidatorConfigurationException
        rulesManager.addRule(section, rule);
    }

    //get methods for obtaining values, patterns, rule-sections, rules, etc

    public ProvidedVarsManager getProvidedVarsManager() {
        return providedVarsManager;
    }

    public ValueEvaluation getValueEvaluationByVariable(String varName) {
        ValueEvaluation evaluation = valueEvaluationsByVarName.get(varName);
        if (evaluation != null) {
            return evaluation;
        } else {//not evaluated yet
            ValueDefinition definition = valueDefinitionsByVarName.get(varName);
            if (definition == null) {
                return new ValueEvaluation(null, String.format("proměnná pro hodnotu %s není definována", varName));
            } else {
                evaluation = definition.evaluate();
                valueEvaluationsByVarName.put(varName, evaluation);
                return evaluation;
            }
        }
    }

    public PatternEvaluation getPatternEvaluationByVariable(String varName) {
        PatternEvaluation evaluation = patternEvaluationsByVarName.get(varName);
        if (evaluation != null) {
            return evaluation;
        } else {
            PatternDefinition definition = patternDefinitionsByVarName.get(varName);
            if (definition == null) {
                return new PatternEvaluation(null, String.format("proměnná pro vzor %s není definována", varName));
            } else {
                evaluation = definition.evaluate();
                patternEvaluationsByVarName.put(varName, evaluation);
                return evaluation;
            }
        }
    }

    public List<RulesSection> getRuleSections() {
        return rulesManager.getSections();
    }

    public List<Rule> getRules(RulesSection section) {
        return rulesManager.getRules(section);
    }

    public PatternExpression buildRawPatternExpression(boolean caseSensitive, String regexp) {
        return new PatternExpression(caseSensitive, regexp);
    }

    //helper classes, interfaces

    public class ProvidedVarsManager {

        private final Map<String, File> files = new HashMap<>();
        private final Map<String, String> strings = new HashMap<>();
        private final Map<String, Integer> integers = new HashMap<>();

        public File getProvidedFile(String fileId) {
            return files.get(fileId);
        }

        public String getProvidedString(String stringId) {
            return strings.get(stringId);
        }

        public Integer getProvidedInteger(String intId) {
            return integers.get(intId);
        }

        public void addFile(String fileId, File file) {
            files.put(fileId, file);
        }

        public void addString(String stringId, String value) {
            strings.put(stringId, value);
        }

        public void addInteger(String intId, Integer value) {
            integers.put(intId, value);
        }

    }

    private static class RulesManager {
        private final List<RulesSection> sections = new ArrayList<>();
        private final Map<RulesSection, List<Rule>> rulesBySection = new HashMap<>();


        void addSection(RulesSection section) throws ValidatorConfigurationException {
            if (sections.contains(section)) {
                throw new ValidatorConfigurationException(String.format("rules section %s already added", section.getName()));
            } else {
                sections.add(section);
                rulesBySection.put(section, new ArrayList<>());
            }
        }

        void addRule(RulesSection section, Rule rule) throws ValidatorConfigurationException {
            if (!rulesBySection.keySet().contains(section)) {
                throw new ValidatorConfigurationException(String.format("rules section %s not added yet", section.getName()));
            } else {
                rulesBySection.get(section).add(rule);
            }
        }

        List<RulesSection> getSections() {
            return sections;
        }

        List<Rule> getRules(RulesSection section) {
            return rulesBySection.get(section);
        }
    }


    public Document getXmlDocument(File file, boolean nsAware) throws XmlFileParsingException {
        return xmlManager.getXmlDocument(file, nsAware);
    }

    public XPathExpression buildXpath(String xpathExpression) throws InvalidXPathExpressionException {
        return xmlManager.buildXpath(xpathExpression);
    }

    public void defineNamespace(String prefix, String uri) {
        xmlManager.setNamespaceUri(prefix, uri);
    }

    public ImageValidator getImageValidator() {
        return imageValidator;
    }

}
