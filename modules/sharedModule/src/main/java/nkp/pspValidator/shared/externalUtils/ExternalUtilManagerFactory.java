package nkp.pspValidator.shared.externalUtils;

import com.mycila.xmltool.XMLDoc;
import com.mycila.xmltool.XMLTag;
import nkp.pspValidator.shared.OperatingSystem;
import nkp.pspValidator.shared.engine.exceptions.ValidatorConfigurationException;
import org.w3c.dom.Element;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    public ExternalUtilManagerFactory(File configFile) throws ValidatorConfigurationException {
        XMLTag doc = XMLDoc.from(configFile, true);
        if (!"externalUtils".equals(doc.getCurrentTagName())) {
            throw new ValidatorConfigurationException("root element není dmf");
        }
        List<Element> utilEls = getChildrenElementsByName(doc.getCurrentTag(), "util");
        for (Element utilEl : utilEls) {
            ExternalUtil util = ExternalUtil.valueOf(utilEl.getAttribute("name"));

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
                    addUtilityExecution(os, util, executionName, command, stream, regexp);
                }
            }
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

    private void addUtilityExecution(OperatingSystem os, ExternalUtil util, String executionName, String rawCommand, Stream outStream, String outRegexp) {
        ExternalUtilExecution execution = new ExternalUtilExecution(executionName, util);
        if (!utilExecutionInfoByOs.containsKey(os)) {
            utilExecutionInfoByOs.put(os, new HashMap<>());
        }
        utilExecutionInfoByOs.get(os).put(execution,
                new UtilHandler(
                        new UtilHandler.CommandData(rawCommand),
                        new UtilHandler.Parser(outStream, outRegexp)
                )
        );
    }


    public ExternalUtilManager buildExternalUtilManager(OperatingSystem os) {
        return new ExternalUtilManager(utilVersionDetectionInfoByOs.get(os), utilExecutionInfoByOs.get(os));
    }

}
