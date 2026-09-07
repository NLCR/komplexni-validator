package nkp.pspValidator.shared.engine.validationFunctions;

import nkp.pspValidator.shared.engine.Engine;
import nkp.pspValidator.shared.engine.Level;
import nkp.pspValidator.shared.engine.ValueEvaluation;
import nkp.pspValidator.shared.engine.ValueType;
import nkp.pspValidator.shared.engine.exceptions.ContractException;
import nkp.pspValidator.shared.engine.exceptions.EmptyParamEvaluationException;
import nkp.pspValidator.shared.engine.params.ValueParam;
import nkp.pspValidator.shared.engine.utils.XmlNamespaceUtils;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Zkontroluje, ze vsechny XML soubory maji stejny namespace korenoveho elementu, tj. stejnou (major) verzi formatu.
 * Pouziti: ALTO soubory jednoho balicku by mely byt vsechny v jedne verzi. Soubory, ktere nejsou well-formed XML,
 * se preskoci (hlasi je validace podle XSD).
 */
public class VfCheckXmlFilesShareRootNamespace extends ValidationFunction {

    public static final String PARAM_XML_FILES = "xml_files";
    public static final String PARAM_LEVEL = "level";

    public VfCheckXmlFilesShareRootNamespace(String name, Engine engine) {
        super(name, engine, new Contract()
                .withValueParam(PARAM_XML_FILES, ValueType.FILE_LIST, 1, null)
                .withValueParam(PARAM_LEVEL, ValueType.LEVEL, 0, 1));
    }

    @Override
    public ValidationResult validate() {
        try {
            checkContractCompliance();

            List<File> xmlFiles = new ArrayList<>();
            for (ValueParam param : valueParams.getParams(PARAM_XML_FILES)) {
                ValueEvaluation evaluation = param.getEvaluation();
                List<File> files = (List<File>) evaluation.getData();
                if (files == null) {
                    throw new EmptyParamEvaluationException(PARAM_XML_FILES, evaluation);
                }
                xmlFiles.addAll(files);
            }

            Level level = Level.WARNING;
            List<ValueParam> paramsLevel = valueParams.getParams(PARAM_LEVEL);
            if (!paramsLevel.isEmpty()) {
                ValueEvaluation evaluation = paramsLevel.get(0).getEvaluation();
                if (evaluation.getData() == null) {
                    return invalidValueParamNull(PARAM_LEVEL, evaluation);
                }
                level = (Level) evaluation.getData();
            }

            return validate(xmlFiles, level);
        } catch (ContractException e) {
            return invalidContractNotMet(e);
        } catch (Throwable e) {
            return invalidUnexpectedError(e);
        }
    }

    private ValidationResult validate(List<File> xmlFiles, Level level) {
        ValidationResult result = new ValidationResult();
        //namespace -> soubory (serazene podle namespace, aby byla hlaska deterministicka)
        Map<String, List<File>> filesByNamespace = new TreeMap<>();
        for (File xmlFile : xmlFiles) {
            try {
                String namespace = XmlNamespaceUtils.getRootElementNamespace(xmlFile);
                filesByNamespace.computeIfAbsent(namespace, k -> new ArrayList<>()).add(xmlFile);
            } catch (XMLStreamException e) {
                //neni XML, resi jina kontrola
            } catch (IOException e) {
                result.addError(Level.ERROR, xmlFile, "I/O chyba při čtení souboru: %s", e.getMessage());
            }
        }
        if (filesByNamespace.size() > 1) {
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, List<File>> entry : filesByNamespace.entrySet()) {
                if (sb.length() > 0) {
                    sb.append("; ");
                }
                List<File> files = entry.getValue();
                sb.append(String.format("'%s': %d %s, např. %s", entry.getKey(), files.size(),
                        files.size() == 1 ? "soubor" : (files.size() < 5 ? "soubory" : "souborů"), files.get(0).getName()));
            }
            result.addError(new ValidationProblem(level,
                    String.format("soubory nemají jednotnou verzi formátu: nalezeno %d různých namespace kořenového elementu: %s",
                            filesByNamespace.size(), sb))
                    .withActualValue(String.join(", ", filesByNamespace.keySet()))
                    .withSimpleMessage("soubory nemají jednotnou verzi formátu (různé namespace kořenového elementu)"));
        }
        return result;
    }
}
