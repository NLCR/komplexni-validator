package nkp.pspValidator.shared.engine;

import nkp.pspValidator.shared.engine.evaluationFunctions.*;
import nkp.pspValidator.shared.engine.exceptions.InvalidXPathExpressionException;
import nkp.pspValidator.shared.engine.exceptions.ValidatorConfigurationException;
import nkp.pspValidator.shared.engine.exceptions.XmlFileParsingException;
import nkp.pspValidator.shared.engine.validationFunctions.*;
import nkp.pspValidator.shared.externalUtils.validation.BinaryFileValidator;
import nkp.pspValidator.shared.metadataProfile.biblio.BibliographicMetadataProfilesManager;
import nkp.pspValidator.shared.metadataProfile.mets.MetsProfilesManager;
import nkp.pspValidator.shared.metadataProfile.tech.TechnicalMetadataProfilesManager;
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
    private final BinaryFileValidator binaryFileValidator;

    private BibliographicMetadataProfilesManager bibliographicMetadataProfilesManager;
    private TechnicalMetadataProfilesManager technicalMetadataProfilesManager;
    private MetsProfilesManager metsProfilesManager;

    public BibliographicMetadataProfilesManager getBibliographicMetadataProfilesManager() {
        return bibliographicMetadataProfilesManager;
    }

    public void setBibliographicMetadataProfilesManager(BibliographicMetadataProfilesManager bibliographicMetadataProfilesManager) {
        this.bibliographicMetadataProfilesManager = bibliographicMetadataProfilesManager;
    }

    public TechnicalMetadataProfilesManager getTechnicalMetadataProfilesManager() {
        return technicalMetadataProfilesManager;
    }

    public void setTechnicalMetadataProfilesManager(TechnicalMetadataProfilesManager technicalMetadataProfilesManager) {
        this.technicalMetadataProfilesManager = technicalMetadataProfilesManager;
    }

    public void setMetsProfilesManager(MetsProfilesManager metsProfilesManager) {
        this.metsProfilesManager = metsProfilesManager;
    }

    public MetsProfilesManager getMetsProfilesManager() {
        return metsProfilesManager;
    }

    public Engine(BinaryFileValidator binaryFileValidator) {
        this.binaryFileValidator = binaryFileValidator;
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
                return new EfGetProvidedFile(name, this);
            case "getProvidedString":
                return new EfGetProvidedString(name, this);
            case "getProvidedInteger":
                return new EfGetProvidedInteger(name, this);
            case "getFirstFileFromFileList":
                return new EfGetFirstFileFromFileList(name, this);
            case "findFilesInDirByPattern":
                return new EfFindFilesInDirByPattern(name, this);
            case "getStringByXpath":
                return new EfGetStringByXpath(name, this);
            case "getStringListByXpath":
                return new EfGetStringListByXpath(name, this);
            case "getIdentifiersFromInfoFile":
                return new EfGetIdentifiersFromInfoFile(name, this);
            case "buildListOfStrings":
                return new EfBuildListOfStrings(name, this);
            case "filterIdentifersByTypes":
                return new EfFilterIdentifersByTypes(name, this);
            case "getFileListByXpath":
                return new EfGetFileListByXpath(name, this);
            case "getDcIdentifiersForEachDmdsecId":
                return new EfGetDcIdentifiersForEachDmdsecId(name, this);
            case "getModsIdentifiersForEachDmdsecId":
                return new EfGetModsIdentifiersForEachDmdsecId(name, this);
            case "mergeIdentifiers":
                return new EfMergeIdentifiers(name, this);
            case "mergeFiles":
                return new EfMergeFiles(name, this);
            default:
                throw new RuntimeException(String.format("vyhodnocovací funkce %s neexistuje", name));
        }
    }

    public ValidationFunction buildValidationFunction(String name) throws ValidatorConfigurationException {
        switch (name) {
            case "checkFilelistHasExactSize":
                return new VfCheckFileListHasExactSize(name, this);
            case "checkFilelistSizeInRange":
                return new VfCheckFileListSizeIsInRange(name, this);
            case "checkFilenameMatchesPattern":
                return new VfCheckFilenameMatchesPattern(name, this);
            case "checkAllFilenamesMatchPattern":
                return new VfCheckAllFilenamesMatchPattern(name, this);
            case "checkFileIsDir":
                return new VfCheckFileIsDir(name, this);
            case "checkFileIsNotDir":
                return new VfCheckFileIsNotDir(name, this);
            case "checkNoFileIsDir":
                return new VfCheckNoFileIsDir(name, this);
            case "checkNoOtherFilesInDir":
                return new VfCheckNoOtherFilesInDir(name, this);
            case "checkAllFileListsHaveSameSize":
                return new VfCheckAllFileListsHaveSameSize(name, this);
            case "checkFilenamesLengthsSame":
                return new VfCheckFilenamesLengthsSame(name, this);
            case "checkChecksumFileGeneratedByGrammar":
                return new VfCheckChecksumFileGeneratedByGrammar(name, this);
            case "checkChecksumFileAllPathsMatchFiles":
                return new VfCheckChecksumFileAllPathsMatchFiles(name, this);
            case "checkChecksumFileAllChecksumsMatch":
                return new VfCheckChecksumFileAllChecksumsMatch(name, this);
            case "checkXmlIsWellBuilt":
                return new VfCheckXmlIsWellBuilt(name, this);
            case "checkXmlIsValidByXsd":
                return new VfCheckXmlIsValidByXsd(name, this);
            case "checkInfoFileReferencesPrimaryMets":
                return new VfCheckInfoFileReferencesPrimaryMets(name, this);
            case "checkInfoFileItemsCountMatchesItemtotal":
                return new VfCheckInfoFileItemsCountMatchesItemtotal(name, this);
            case "checkInfoFileItemlistReferencesAllFiles":
                return new VfCheckInfoFileItemlistReferencesAllFiles(name, this);
            case "checkInfoFileChecksumMatches":
                return new VfCheckInfoFileChecksumMatches(name, this);

            //identifiers in general
            case "checkIdentifiersNoDuplicateTypes":
                return new VfCheckIdentifiersNoDuplicateTypes(name, this);
            case "checkIdentifiersNoDuplicates":
                return new VfCheckIdentifiersNoDuplicates(name, this);
            case "checkIdentifiersAllTypesPresent":
                return new VfCheckIdentifiersAllTypesPresent(name, this);
            case "checkIdentifiersNoneTypePresent":
                return new VfCheckIdentifiersNoneTypePresent(name, this);
            case "checkDcIdentifiersDoNotContainWhiteSpaces":
                return new VfCheckDcIdentifiersDoNotContainWhiteSpaces(name, this);
            case "checkUrnNbnIdentifiersValid":
                return new VfCheckUrnNbnIdentifiersValid(name, this);
            case "checkUrnNbnIdentifiersRegistered":
                return new VfCheckUrnNbnIdentifiersRegistered(name, this);
            case "checkStringDerivedFromOneOfIdentifiers":
                return new VfCheckStringDerivedFromOneOfIdentifiers(name, this);
            case "checkDcIdentifiersHaveTypeAndValue":
                return new VfCheckDcIdentifiersHaveTypeAndValue(name, this);
            case "checkPrimaryMetsFilesecContainsAllFilegroups":
                return new VfCheckPrimaryMetsFilesecContainsAllFilegroups(name, this);
            case "checkPrimaryMetsDcIdentifiersMatchModsIdentifiers":
                return new VfCheckPrimaryMetsDcIdentifiersMatchModsIdentifiers(name, this);
            case "checkFileListsMatch":
                return new VfCheckFileListsMatch(name, this);


            //mets
            case "checkMetsFilesecSizesMatch":
                return new VfCheckMetsFilesecSizesMatch(name, this);
            case "checkMetsFilesecChecksumsMatch":
                return new VfCheckMetsFilesecChecksumsMatch(name, this);
            case "checkSecondaryMetsFilesecContainsAllFilegroups":
                return new VfCheckSecondaryMetsFilesecContainsAllFilegroups(name, this);
            case "checkSecondaryMetsFilegroupReferencesSingleFile":
                return new VfCheckSecondaryMetsFilegroupReferencesSingleFile(name, this);
            case "checkPrimaryMetsLogicalMapOk":
                return new VfCheckPrimaryMetsLogicalMapOk(name, this);
            case "checkPrimaryMetsPhysicalMapOk":
                return new VfCheckPrimaryMetsPhysicalMapOk(name, this);
            case "checkPrimaryMetsStructLinksOk":
                return new VfCheckPrimaryMetsStructLinksOk(name, this);
            case "checkSecondaryMetsPhysicalMapOk":
                return new VfCheckSecondaryMetsPhysicalMapOk(name, this);
            case "checkMetsAmdsecElementsMatchProfile":
                return new VfCheckMetsAmdsecElementsMatchProfile(name, this);
            case "checkPremisLinks":
                return new VfCheckPremisLinks(name, this);
            case "checkBinaryFilesValidByExternalUtil":
                return new VfCheckBinaryFilesValidByExternalUtil(name, this);
            case "checkMetsMatchProfile":
                return new VfCheckMetsMatchProfile(name, this);

            //bibliographic metadata
            case "checkBibliographicMetadataMatchProfile":
                return new VfCheckBibliographicMetadataMatchProfile(name, this);
            case "checkMixIsValidByXsd":
                return new VfCheckMixIsValidByXsd(name, this);
            case "checkPremisIsValidByXsd":
                return new VfCheckPremisIsValidByXsd(name, this);
            case "checkCopyrightMdIsValidByXsd":
                return new VfCheckCopyrightmdIsValidByXsd(name, this);

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

    public BinaryFileValidator getBinaryFileValidator() {
        return binaryFileValidator;
    }

}
