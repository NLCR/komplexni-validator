package nkp.pspValidator.shared.externalUtils;

import com.mycila.xmltool.XMLDoc;
import com.mycila.xmltool.XMLTag;
import nkp.pspValidator.shared.OperatingSystem;
import nkp.pspValidator.shared.engine.exceptions.ValidatorConfigurationException;
import org.w3c.dom.Element;

import java.io.File;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.PatternSyntaxException;

import static nkp.pspValidator.shared.XmlUtils.getChildrenElementsByName;
import static nkp.pspValidator.shared.XmlUtils.getFirstChildElementsByName;

/**
 * Created by Martin Řehánek on 29.9.16.
 */
public class ExternalUtilManagerFactory {

    private static ExternalUtilManagerFactory instance;

    private static final String REGEXP_FIRST_LINE = "(?m)^.*$";

    private final Map<OperatingSystem, Map<ExternalUtil, UtilHandler>> utilVersionDetectionInfoByOs = new HashMap<>();
    private final Map<OperatingSystem, Map<ExternalUtilExecution, UtilHandler>> utilExecutionInfoByOs = new HashMap<>();
    private final Map<OperatingSystem, Map<ExternalUtil, VerifiedVersions>> verifiedVersionsByOs = new EnumMap<>(OperatingSystem.class);

    public ExternalUtilManagerFactory(File configFile) throws ValidatorConfigurationException {
        XMLTag doc = XMLDoc.from(configFile, true);
        if (!"externalUtils".equals(doc.getCurrentTagName())) {
            throw new ValidatorConfigurationException("root element není dmf");
        }
        List<Element> utilEls = getChildrenElementsByName(doc.getCurrentTag(), "util");
        for (Element utilEl : utilEls) {
            ExternalUtil util = ExternalUtil.valueOf(utilEl.getAttribute("name"));

            Element verifiedVersionsEl = getFirstChildElementsByName(utilEl, "verifiedVersions");
            if (verifiedVersionsEl != null) {
                addVerifiedVersions(util, verifiedVersionsEl);
            }

            Element versionDetectionEl = getFirstChildElementsByName(utilEl, "versionDetection");
            List<Element> versionDetectionsEls = getChildrenElementsByName(versionDetectionEl, "operatingSystem");
            for (Element osEl : versionDetectionsEls) {
                OperatingSystem os = OperatingSystem.valueOf(osEl.getAttribute("name"));
                String command = getFirstChildElementsByName(osEl, "command").getTextContent().trim();
                Element parserEl = getFirstChildElementsByName(osEl, "parser");
                Element streamEl = getFirstChildElementsByName(parserEl, "stream");
                Stream stream = Stream.valueOf(streamEl.getTextContent().trim());
                String regexp = getFirstChildElementsByName(parserEl, "regexp").getTextContent().trim();
                addVersionDetection(os, util, command, stream, regexp);
            }

            List<Element> executionEls = getChildrenElementsByName(utilEl, "execution");
            for (Element executionEl : executionEls) {
                String executionName = executionEl.getAttribute("name");
                List<Element> executionOsEl = getChildrenElementsByName(executionEl, "operatingSystem");
                for (Element osEl : executionOsEl) {
                    OperatingSystem os = OperatingSystem.valueOf(osEl.getAttribute("name"));
                    String command = getFirstChildElementsByName(osEl, "command").getTextContent().trim();
                    Element parserEl = getFirstChildElementsByName(osEl, "parser");
                    Element streamEl = getFirstChildElementsByName(parserEl, "stream");
                    Stream stream = Stream.valueOf(streamEl.getTextContent().trim());
                    //System.err.println(String.format("execution: os: %s, util: %s, name: %s, stream: %s", os, util, executionName, stream));
                    String regexp = getFirstChildElementsByName(parserEl, "regexp").getTextContent().trim();
                    ExitCodePolicy exitCodePolicy = parseExitCodes(util, executionName, os, getFirstChildElementsByName(osEl, "exitCodes"));
                    UtilHandler.ProcessedEvidence evidence = parseProcessedEvidence(util, executionName, os, getFirstChildElementsByName(osEl, "processedFileEvidence"));
                    addUtilityExecution(os, util, executionName, command, stream, regexp, exitCodePolicy, evidence);
                }
            }
        }
    }

    /**
     * Ověřené řady verzí nástroje podle platformy (element verifiedVersions):
     * <pre>
     * &lt;verifiedVersions&gt;
     *     &lt;operatingSystem name="MAC"&gt;
     *         &lt;series verifiedWith="1.34.0"&gt;1.34&lt;/series&gt;
     *     &lt;/operatingSystem&gt;
     *     &lt;operatingSystem name="WINDOWS"/&gt;             (na platformě není ověřena žádná verze)
     *     &lt;operatingSystem name="LINUX" inheritsFrom="MAC"/&gt; (platí seznam macOS s poznámkou „ověřeno na macOS“)
     * &lt;/verifiedVersions&gt;
     * </pre>
     * Platforma, která v seznamu chybí (nebo nástroj bez elementu verifiedVersions), seznam nemá a verze se na ní
     * neoznačuje. Převzít lze jen vlastní seznam jiné platformy, ne převzatý.
     */
    private void addVerifiedVersions(ExternalUtil util, Element verifiedVersionsEl) throws ValidatorConfigurationException {
        Map<OperatingSystem, VerifiedVersions> own = new EnumMap<>(OperatingSystem.class);
        Map<OperatingSystem, OperatingSystem> inheriting = new EnumMap<>(OperatingSystem.class);
        for (Element osEl : getChildrenElementsByName(verifiedVersionsEl, "operatingSystem")) {
            OperatingSystem os = parseOs(util, osEl.getAttribute("name"));
            if (own.containsKey(os) || inheriting.containsKey(os)) {
                throw new ValidatorConfigurationException("%s: ověřené verze pro %s jsou uvedeny dvakrát", util, os);
            }
            List<Element> seriesEls = getChildrenElementsByName(osEl, "series");
            String inheritsFrom = osEl.getAttribute("inheritsFrom");
            if (inheritsFrom != null && !inheritsFrom.trim().isEmpty()) {
                if (!seriesEls.isEmpty()) {
                    throw new ValidatorConfigurationException("%s: platforma %s přebírá ověřené verze z %s, nesmí mít vlastní řady", util, os, inheritsFrom);
                }
                inheriting.put(os, parseOs(util, inheritsFrom.trim()));
                continue;
            }
            List<VerifiedVersions.Series> series = new ArrayList<>();
            for (Element seriesEl : seriesEls) {
                String value = seriesEl.getTextContent().trim();
                if (!UtilVersion.isValidSeries(value)) {
                    throw new ValidatorConfigurationException("%s: neplatná řada verzí pro %s: '%s'", util, os, value);
                }
                String verifiedWith = seriesEl.getAttribute("verifiedWith");
                series.add(new VerifiedVersions.Series(value, verifiedWith == null || verifiedWith.trim().isEmpty() ? null : verifiedWith.trim()));
            }
            own.put(os, new VerifiedVersions(series, null));
        }
        for (Map.Entry<OperatingSystem, OperatingSystem> entry : inheriting.entrySet()) {
            VerifiedVersions source = own.get(entry.getValue());
            if (source == null) {
                throw new ValidatorConfigurationException("%s: platforma %s přebírá ověřené verze z %s, ta ale vlastní seznam nemá", util, entry.getKey(), entry.getValue());
            }
            verifiedVersionsByOs.computeIfAbsent(entry.getKey(), os -> new EnumMap<>(ExternalUtil.class))
                    .put(util, source.inheritedFrom(entry.getValue()));
        }
        for (Map.Entry<OperatingSystem, VerifiedVersions> entry : own.entrySet()) {
            verifiedVersionsByOs.computeIfAbsent(entry.getKey(), os -> new EnumMap<>(ExternalUtil.class)).put(util, entry.getValue());
        }
    }

    private static OperatingSystem parseOs(ExternalUtil util, String name) throws ValidatorConfigurationException {
        try {
            return OperatingSystem.valueOf(name);
        } catch (IllegalArgumentException e) {
            throw new ValidatorConfigurationException("%s: neznámý operační systém v ověřených verzích: '%s'", util, name);
        }
    }

    /**
     * Posouzení návratového kódu spuštění nad souborem (element exitCodes, volitelný):
     * <pre>
     * &lt;exitCodes normal="0 255"/&gt;  (pád i každý jiný kód než 0 a 255 je selhání)
     * &lt;exitCodes/&gt;                (jen pád nebo nespuštění nástroje je selhání, viz {@link ExitCodePolicy})
     * </pre>
     * Bez elementu se kód neposuzuje (konfigurace starší než 3.0).
     */
    private static ExitCodePolicy parseExitCodes(ExternalUtil util, String executionName, OperatingSystem os, Element exitCodesEl) throws ValidatorConfigurationException {
        if (exitCodesEl == null) {
            return null;
        }
        try {
            return new ExitCodePolicy(os, ExitCodePolicy.parseCodes(exitCodesEl.getAttribute("normal")));
        } catch (IllegalArgumentException e) {
            throw new ValidatorConfigurationException("%s, spuštění %s, %s: exitCodes: %s", util, executionName, os, e.getMessage());
        }
    }

    /**
     * Znak zpracování souboru (element processedFileEvidence, volitelný): regexp, který musí být v daném proudu
     * výstupu, např. &lt;processedFileEvidence stream="STDOUT"&gt;Consumed [0-9,]+ tile-part&lt;/processedFileEvidence&gt;.
     */
    private static UtilHandler.ProcessedEvidence parseProcessedEvidence(ExternalUtil util, String executionName, OperatingSystem os, Element evidenceEl) throws ValidatorConfigurationException {
        if (evidenceEl == null) {
            return null;
        }
        String where = String.format("%s, spuštění %s, %s: processedFileEvidence", util, executionName, os);
        Stream stream;
        try {
            stream = Stream.valueOf(evidenceEl.getAttribute("stream").trim());
        } catch (IllegalArgumentException e) {
            throw new ValidatorConfigurationException("%s: neplatný proud '%s' (STDOUT nebo STDERR)", where, evidenceEl.getAttribute("stream"));
        }
        String regexp = evidenceEl.getTextContent().trim();
        if (regexp.isEmpty()) {
            throw new ValidatorConfigurationException("%s: chybí regulární výraz", where);
        }
        try {
            return new UtilHandler.ProcessedEvidence(stream, regexp);
        } catch (PatternSyntaxException e) {
            throw new ValidatorConfigurationException("%s: neplatný regulární výraz: %s", where, e.getDescription());
        }
    }

    private void addVersionDetection(OperatingSystem os, ExternalUtil util, String rawCommand, Stream outStream, String outRegexp) {
        if (!utilVersionDetectionInfoByOs.containsKey(os)) {
            utilVersionDetectionInfoByOs.put(os, new HashMap<>());
        }
        utilVersionDetectionInfoByOs.get(os).put(util,
                new UtilHandler(
                        new UtilHandler.CommandData(rawCommand),
                        new UtilHandler.Parser(outStream, outRegexp)
                )
        );
    }

    private void addUtilityExecution(OperatingSystem os, ExternalUtil util, String executionName, String rawCommand, Stream outStream, String outRegexp,
                                     ExitCodePolicy exitCodePolicy, UtilHandler.ProcessedEvidence evidence) {
        ExternalUtilExecution execution = new ExternalUtilExecution(executionName, util);
        if (!utilExecutionInfoByOs.containsKey(os)) {
            utilExecutionInfoByOs.put(os, new HashMap<>());
        }
        utilExecutionInfoByOs.get(os).put(execution,
                new UtilHandler(
                        new UtilHandler.CommandData(rawCommand),
                        new UtilHandler.Parser(outStream, outRegexp),
                        exitCodePolicy,
                        evidence
                )
        );
    }


    public ExternalUtilManager buildExternalUtilManager(OperatingSystem os) {
        Map<ExternalUtil, VerifiedVersions> verified = verifiedVersionsByOs.get(os);
        return new ExternalUtilManager(utilVersionDetectionInfoByOs.get(os), utilExecutionInfoByOs.get(os),
                verified == null ? new EnumMap<>(ExternalUtil.class) : verified);
    }

}
