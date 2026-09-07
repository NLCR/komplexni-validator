package nkp.pspValidator.shared.engine.validationFunctions;

import nkp.pspValidator.shared.FdmfConfiguration;
import nkp.pspValidator.shared.engine.Engine;
import nkp.pspValidator.shared.engine.Level;
import nkp.pspValidator.shared.engine.ValueEvaluation;
import nkp.pspValidator.shared.engine.ValueType;
import nkp.pspValidator.shared.engine.exceptions.ContractException;
import nkp.pspValidator.shared.engine.exceptions.EmptyParamEvaluationException;
import nkp.pspValidator.shared.engine.params.ValueParam;
import nkp.pspValidator.shared.engine.utils.XmlNamespaceUtils;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Validace XML souboru proti XSD, kde se schema vybere podle namespace korenoveho elementu. Pouziti: formaty s vice
 * pripustnymi verzemi, ktere se lisi namespace (ALTO 2.x ns-v2#, 3.x ns-v3#, 4.x ns-v4#). Parametr xsd_files nese
 * vsechna schemata; kazde XSD se priradi k namespace podle sveho targetNamespace. Soubor s namespace, pro ktery zadne
 * XSD neni, je chyba (uroven level), stejne jako soubor, ktery neni validni podle prirazeneho XSD.
 */
public class VfCheckXmlIsValidByXsdByNamespace extends ValidationFunction {

    public static final String PARAM_XSD_FILES = "xsd_files";
    public static final String PARAM_XML_FILE = "xml_file";
    public static final String PARAM_XML_FILES = "xml_files";
    public static final String PARAM_LEVEL = "level";

    public VfCheckXmlIsValidByXsdByNamespace(String name, Engine engine) {
        super(name, engine, new Contract()
                .withValueParam(PARAM_XSD_FILES, ValueType.FILE_LIST, 1, null)
                .withValueParam(PARAM_LEVEL, ValueType.LEVEL, 0, 1)
                .withValueParam(PARAM_XML_FILE, ValueType.FILE, 0, null)
                .withValueParam(PARAM_XML_FILES, ValueType.FILE_LIST, 0, null));
    }

    @Override
    public ValidationResult validate() {
        try {
            checkContractCompliance();

            Set<File> xmlFiles = mergeAbsolutFiles(PARAM_XML_FILE, PARAM_XML_FILES);
            List<File> xsdFiles = new ArrayList<>();
            for (ValueParam param : valueParams.getParams(PARAM_XSD_FILES)) {
                ValueEvaluation evaluation = param.getEvaluation();
                List<File> files = (List<File>) evaluation.getData();
                if (files == null) {
                    throw new EmptyParamEvaluationException(PARAM_XSD_FILES, evaluation);
                }
                xsdFiles.addAll(files);
            }
            if (xsdFiles.isEmpty()) {
                return singlErrorResult(new ValidationProblem(Level.ERROR,
                        String.format("parametr %s neobsahuje žádný soubor XSD", PARAM_XSD_FILES)));
            }
            for (File xsdFile : xsdFiles) {
                if (!xsdFile.exists()) {
                    return singlErrorResult(invalidFileDoesNotExist(xsdFile));
                } else if (xsdFile.isDirectory()) {
                    return singlErrorResult(invalidFileIsDir(xsdFile));
                } else if (!xsdFile.canRead()) {
                    return singlErrorResult(invalidCannotReadFile(xsdFile));
                }
            }

            Level level = Level.ERROR;
            List<ValueParam> paramsLevel = valueParams.getParams(PARAM_LEVEL);
            if (!paramsLevel.isEmpty()) {
                ValueEvaluation evaluation = paramsLevel.get(0).getEvaluation();
                if (evaluation.getData() == null) {
                    return invalidValueParamNull(PARAM_LEVEL, evaluation);
                }
                level = (Level) evaluation.getData();
            }

            return validate(xmlFiles, xsdFiles, level);
        } catch (ContractException e) {
            return invalidContractNotMet(e);
        } catch (Throwable e) {
            return invalidUnexpectedError(e);
        }
    }

    private Set<File> mergeAbsolutFiles(String fileParam, String filesParam) throws EmptyParamEvaluationException {
        Set<File> result = new TreeSet<>();
        for (ValueParam param : valueParams.getParams(fileParam)) {
            ValueEvaluation evaluation = param.getEvaluation();
            File file = (File) evaluation.getData();
            if (file == null) {
                throw new EmptyParamEvaluationException(fileParam, evaluation);
            }
            result.add(file.getAbsoluteFile());
        }
        for (ValueParam param : valueParams.getParams(filesParam)) {
            ValueEvaluation evaluation = param.getEvaluation();
            List<File> files = (List<File>) evaluation.getData();
            if (files == null) {
                throw new EmptyParamEvaluationException(filesParam, evaluation);
            }
            for (File file : files) {
                result.add(file.getAbsoluteFile());
            }
        }
        return result;
    }

    private ValidationResult validate(Set<File> xmlFiles, List<File> xsdFiles, Level level) {
        ValidationResult result = new ValidationResult();
        //namespace -> XSD; pri kolizi vyhrava posledni (seznam je serazeny podle verze, tedy nejnovejsi)
        Map<String, File> xsdByNamespace = new LinkedHashMap<>();
        for (File xsdFile : xsdFiles) {
            try {
                xsdByNamespace.put(XmlNamespaceUtils.getXsdTargetNamespace(xsdFile), xsdFile);
            } catch (IOException | XMLStreamException e) {
                result.addError(new ValidationProblem(Level.ERROR,
                        String.format("nelze přečíst targetNamespace ze souboru XSD %s: %s", xsdFile.getAbsolutePath(), e.getMessage()))
                        .withXsdFile(xsdFile)
                        .withSimpleMessage("nelze přečíst XSD schéma"));
                return result;
            }
        }
        Map<String, Schema> schemaCache = new LinkedHashMap<>();
        for (File xmlFile : xmlFiles) {
            validate(xmlFile, xsdByNamespace, schemaCache, level, result);
        }
        return result;
    }

    private void validate(File xmlFile, Map<String, File> xsdByNamespace, Map<String, Schema> schemaCache, Level level, ValidationResult result) {
        String namespace;
        try {
            namespace = XmlNamespaceUtils.getRootElementNamespace(xmlFile);
        } catch (XMLStreamException e) {
            result.addError(new ValidationProblem(level,
                    String.format("soubor %s není správně utvořený XML dokument: %s", xmlFile, e.getMessage()))
                    .withFile(xmlFile)
                    .withSimpleMessage("soubor není správně utvořený XML dokument"));
            return;
        } catch (IOException e) {
            result.addError(Level.ERROR, xmlFile, "I/O chyba při čtení souboru: %s", e.getMessage());
            return;
        }
        File xsdFile = xsdByNamespace.get(namespace);
        if (xsdFile == null) {
            result.addError(new ValidationProblem(level,
                    String.format("soubor %s má nepodporovanou verzi formátu: namespace kořenového elementu '%s' neodpovídá žádnému z povolených schémat (%s)",
                            xmlFile, namespace, describeSchemas(xsdByNamespace)))
                    .withFile(xmlFile)
                    .withActualValue(namespace)
                    .withValueSpec(describeSchemas(xsdByNamespace))
                    .withSimpleMessage("nepodporovaná verze formátu (namespace kořenového elementu)"));
            return;
        }
        try {
            Schema schema = schemaCache.get(namespace);
            if (schema == null) {
                SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
                schema = schemaFactory.newSchema(xsdFile);
                schemaCache.put(namespace, schema);
            }
            Validator validator = schema.newValidator();
            validator.validate(new StreamSource(xmlFile));
        } catch (SAXException e) {
            result.addError(new ValidationProblem(level,
                    String.format("obsah souboru %s není validní podle Xml schema ze souboru %s: %s", xmlFile, xsdFile.getAbsolutePath(), e.getMessage()))
                    .withFile(xmlFile)
                    .withXsdFile(xsdFile)
                    .withSimpleMessage("obsah souboru není validní podle XSD schématu: " + e.getMessage()));
        } catch (IOException e) {
            result.addError(Level.ERROR, xmlFile, "I/O chyba při čtení souboru: %s", e.getMessage());
        }
    }

    /**
     * Napr. "2.0 (http://www.loc.gov/standards/alto/ns-v2#), 3.1 (…ns-v3#), 4.4 (…ns-v4#)".
     */
    static String describeSchemas(Map<String, File> xsdByNamespace) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, File> entry : xsdByNamespace.entrySet()) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            String version = FdmfConfiguration.versionFromXsdName(entry.getValue().getName());
            sb.append(version.isEmpty() ? entry.getValue().getName() : version)
                    .append(" (").append(entry.getKey()).append(")");
        }
        return sb.toString();
    }
}
