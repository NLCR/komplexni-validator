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

    private final Map<OperatingSystem, Map<ExternalUtil, UtilHandler>> utilVersionDetectionInfoByOs = initProcessInfos();
    private final Map<OperatingSystem, Map<ExternalUtil, UtilHandler>> utilExecutionInfoByOs = initProcessInfos();


    private Map<OperatingSystem, Map<ExternalUtil, UtilHandler>> initProcessInfos() {
        Map<OperatingSystem, Map<ExternalUtil, UtilHandler>> result = new HashMap<>();
        for (OperatingSystem os : OperatingSystem.values()) {
            result.put(os, new HashMap<>());
        }
        return result;
    }

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

            Element executionEl = getFirstChildElementsByName(utilEl, "execution");
            List<Element> executionOsEl = getChildrenElementsByName(executionEl, "operatingSystem");
            for (Element osEl : executionOsEl) {
                OperatingSystem os = OperatingSystem.valueOf(osEl.getAttribute("name"));
                String command = getFirstChildElementsByName(osEl, "command").getTextContent().trim();
                Element parserEl = getFirstChildElementsByName(osEl, "parser");
                Element streamEl = getFirstChildElementsByName(parserEl, "stream");
                Stream stream = Stream.valueOf(streamEl.getTextContent().trim());
                //System.err.println(String.format("execution: os: %s, util: %s, stream: %s", os, util, stream));
                String regexp = getFirstChildElementsByName(parserEl, "regexp").getTextContent().trim();
                addUtilityExecution(os, util, command, stream, regexp);
            }
        }
    }

    public ExternalUtilManager buildExternalUtilManager(OperatingSystem os) {
        return new ExternalUtilManager(utilVersionDetectionInfoByOs.get(os), utilExecutionInfoByOs.get(os));
    }

    private void init() {
        //JPYLYZER
        //single STDERR line:"1.17.0"
        addVersionDetection(OperatingSystem.LINUX, ExternalUtil.JPYLYZER, "jpylyzer --version", Stream.STDERR, REGEXP_FIRST_LINE);
        addUtilityExecution(OperatingSystem.LINUX, ExternalUtil.JPYLYZER, "jpylyzer jp2In ${TARGET_FILE}", Stream.STDOUT, null);
        //single STDERR line:"1.17.0s"
        addVersionDetection(OperatingSystem.WINDOWS, ExternalUtil.JPYLYZER, "jpylyzer.exe --version", Stream.STDERR, REGEXP_FIRST_LINE);
        //FIXME: just freezes
        addUtilityExecution(OperatingSystem.WINDOWS, ExternalUtil.JPYLYZER, "jpylyzer.exe jp2In ${TARGET_FILE}", Stream.STDOUT, null);

        //IMAGE MAGICK
        //first line:
        // Version: ImageMagick 6.7.7-10 2016-06-01 Q16 http://www.imagemagick.org
        addVersionDetection(OperatingSystem.LINUX, ExternalUtil.IMAGE_MAGICK, "convert -version", Stream.STDOUT, "ImageMagick\\s\\S*\\s\\S*");
        addUtilityExecution(OperatingSystem.LINUX, ExternalUtil.IMAGE_MAGICK, "identify -verbose ${TARGET_FILE}", Stream.STDOUT, null);
        //fist line:
        //Version: ImageMagick 6.7.7-10 2016-06-01 Q16 http://www.imagemagick.org
        addVersionDetection(OperatingSystem.WINDOWS, ExternalUtil.IMAGE_MAGICK, "magick.exe convert -version", Stream.STDOUT, "ImageMagick\\s\\S*\\s\\S*");
        addUtilityExecution(OperatingSystem.WINDOWS, ExternalUtil.IMAGE_MAGICK, "magick.exe identify -verbose ${TARGET_FILE}", Stream.STDOUT, null);

        //JHOVE
        //first line of STDOUT looks like:
        //Jhove (Rel. 1.6, 2011-01-04)
        addVersionDetection(OperatingSystem.LINUX, ExternalUtil.JHOVE, "jhove --version", Stream.STDOUT, REGEXP_FIRST_LINE);
        addUtilityExecution(OperatingSystem.LINUX, ExternalUtil.JHOVE, "jhove -h XML -m jpeg2000-hul -k ${TARGET_FILE}", Stream.STDOUT, null);
        //first line of STDOUT:
        //Jhove (Rel. 1.6, 2011-01-04)
        //TODO: deal with this:
        //https://github.com/openpreserve/jhove/issues/32
        //until then temporary fix with configuration file
        addVersionDetection(OperatingSystem.WINDOWS, ExternalUtil.JHOVE, "jhove.bat -c C:\\Users\\Martin\\Documents\\PspValidator\\jhove-1_11\\jhove\\conf\\jhove.conf --version", Stream.STDOUT, REGEXP_FIRST_LINE);
        //FIXME: just freezes
        //C:\Users\Martin\Documents\PspValidator\jhove-1_11\jhove\jhove.bat -c C:\Users\Martin\Documents\PspValidator\jhove-1_11\jhove\conf\jhove.conf -h XML -m jpeg2000-hul -k C:\Users\Martin\Documents\PspValidator\mc_b50eb6b0-f0a4-11e3-b72e-005056827e52_0001.jp2
        //works ok in cmd.exe
        //opravdu stderr?
        addUtilityExecution(OperatingSystem.WINDOWS, ExternalUtil.JHOVE, "jhove.bat -c C:\\Users\\Martin\\Documents\\PspValidator\\jhove-1_11\\jhove\\conf\\jhove.conf" +
                " -h XML -m jpeg2000-hul -k ${TARGET_FILE}", Stream.STDERR, null);

        //KAKADU
        // last line:
        // Current core system version is v7.8
        addVersionDetection(OperatingSystem.LINUX, ExternalUtil.KAKADU, "kdu_expand -version", Stream.STDOUT, "Current core system version is \\S*");
        addUtilityExecution(OperatingSystem.LINUX, ExternalUtil.KAKADU, "kdu_expand -i ${TARGET_FILE}", Stream.STDOUT, null);

        addVersionDetection(OperatingSystem.WINDOWS, ExternalUtil.KAKADU, "kdu_expand.exe -version", Stream.STDOUT, "Current core system version is \\S*");
        addUtilityExecution(OperatingSystem.WINDOWS, ExternalUtil.KAKADU, "kdu_expand.exe -i ${TARGET_FILE}", Stream.STDOUT, null);
    }


    private void addVersionDetection(OperatingSystem os, ExternalUtil util, String rawCommand, Stream outStream, String outRegexp) {
        utilVersionDetectionInfoByOs.get(os).put(util,
                new UtilHandler(
                        new UtilHandler.CommandData(rawCommand),
                        new UtilHandler.Parser(outStream, outRegexp)
                )
        );
    }

    private void addUtilityExecution(OperatingSystem os, ExternalUtil util, String rawCommand, Stream outStream, String outRegexp) {
        utilExecutionInfoByOs.get(os).put(util,
                new UtilHandler(
                        new UtilHandler.CommandData(rawCommand),
                        new UtilHandler.Parser(outStream, outRegexp)
                )
        );
    }

   /* private ExternalUtil jpylyzer() {
        ExternalUtil util = new ExternalUtil("jpylyzer");
        //Linux
        util.defineToolVersionDetection(OperatingSystem.LINUX,
                //new ExternalUtil.Command(new File("/usr/bin"), "jpylyzer --version"),
                new ExternalUtil.Command(null, "jpylyzer --version"),
                //jediny radek STDERR obsahuje verzi typu "1.17.0"
                new ExternalUtil.Parser(Stream.STDERR, REGEXP_FIRST_LINE)
        );


        util.defineToolExecution(OperatingSystem.LINUX,
                //new ExternalUtil.Command(new File("/usr/bin"), "jpylyzer jp2In ${TARGET_FILE}"),
                new ExternalUtil.Command(null, "jpylyzer jp2In ${TARGET_FILE}"),
                new ExternalUtil.Parser(Stream.STDOUT, null)
        );


        //Windows
        util.defineToolVersionDetection(OperatingSystem.WINDOWS,
                //new ExternalUtil.Command("C:\\Users\\Martin\\Software\\jpylyzer_1.17.0_win64", "jpylyzer.exe --version"),
                new ExternalUtil.Command(null, "jpylyzer.exe --version"),
                //single STDERR line:
                // 1.17.0s
                new ExternalUtil.Parser(Stream.STDERR, REGEXP_FIRST_LINE)
        );
        //FIXME: just freezes
        util.defineToolExecution(OperatingSystem.WINDOWS,
                //new ExternalUtil.Command("C:\\Users\\Martin\\Software\\jpylyzer_1.17.0_win64", "jpylyzer.exe jp2In ${TARGET_FILE}"),
                new ExternalUtil.Command(null, "jpylyzer.exe jp2In ${TARGET_FILE}"),
                //which stream here?
                new ExternalUtil.Parser(Stream.STDOUT, null)
        );

        //Mac
        // TODO: 29.9.16

        return util;
    }


    private ExternalUtil imageMagick() {
        ExternalUtil util = new ExternalUtil("imageMagick");
        //Linux
        util.defineToolVersionDetection(OperatingSystem.LINUX,
                //new ExternalUtil.Command("/usr/bin", "convert -version"),
                new ExternalUtil.Command(null, "convert -version"),
                //first line:
                //Version: ImageMagick 6.7.7-10 2016-06-01 Q16 http://www.imagemagick.org
                new ExternalUtil.Parser(Stream.STDOUT, "ImageMagick\\s\\S*\\s\\S*")
        );
        util.defineToolExecution(OperatingSystem.LINUX,
                //new ExternalUtil.Command("/usr/bin", "identify -verbose ${TARGET_FILE}"),
                new ExternalUtil.Command(null, "identify -verbose ${TARGET_FILE}"),
                new ExternalUtil.Parser(Stream.STDOUT, null)
        );

        //Windows
        util.defineToolVersionDetection(OperatingSystem.WINDOWS,
                //also on system path
                //new ExternalUtil.Command("C:\\Program Files\\ImageMagick-7.0.3-Q16", "magick.exe convert -version"),
                new ExternalUtil.Command(null, "magick.exe convert -version"),
                //fist line:
                //Version: ImageMagick 6.7.7-10 2016-06-01 Q16 http://www.imagemagick.org
                new ExternalUtil.Parser(Stream.STDOUT, "ImageMagick\\s\\S*\\s\\S*")
        );
        util.defineToolExecution(OperatingSystem.WINDOWS,
                //new ExternalUtil.Command("C:\\Program Files\\ImageMagick-7.0.3-Q16", "magick.exe identify -verbose ${TARGET_FILE}"),
                new ExternalUtil.Command(null, "magick.exe identify -verbose ${TARGET_FILE}"),
                new ExternalUtil.Parser(Stream.STDOUT, null)
        );

        //Mac
        // TODO: 29.9.16

        return util;
    }

    private ExternalUtil jhove() {
        ExternalUtil util = new ExternalUtil("jhove");
        //Linux
        util.defineToolVersionDetection(OperatingSystem.LINUX,
                //new ExternalUtil.Command("/usr/bin", "jhove --version"),
                new ExternalUtil.Command(null, "jhove --version"),
                //first line of STDOUT looks like "Jhove (Rel. 1.6, 2011-01-04)"
                new ExternalUtil.Parser(Stream.STDOUT, REGEXP_FIRST_LINE)
        );
        util.defineToolExecution(OperatingSystem.LINUX,
                //new ExternalUtil.Command("/usr/bin", "jhove -h XML -m jpeg2000-hul -k ${TARGET_FILE}"),
                new ExternalUtil.Command(null, "jhove -h XML -m jpeg2000-hul -k ${TARGET_FILE}"),
                new ExternalUtil.Parser(Stream.STDOUT, null)
        );

        //Windows
        util.defineToolVersionDetection(OperatingSystem.WINDOWS,
                //TODO: deal with this:
                //https://github.com/openpreserve/jhove/issues/32
                //until then temporary fix with configuration file
                *//*new ExternalUtil.Command("C:\\Users\\Martin\\Documents\\PspValidator\\jhove-1_11\\jhove",
                        "jhove.bat -c C:\\Users\\Martin\\Documents\\PspValidator\\jhove-1_11\\jhove\\conf\\jhove.conf --version"),*//*
                new ExternalUtil.Command(null, "jhove.bat -c C:\\Users\\Martin\\Documents\\PspValidator\\jhove-1_11\\jhove\\conf\\jhove.conf --version"),
                //first line of STDOUT looks like "Jhove (Rel. 1.6, 2011-01-04)"
                new ExternalUtil.Parser(Stream.STDOUT, REGEXP_FIRST_LINE)
        );
        //FIXME: just freezes
        //C:\Users\Martin\Documents\PspValidator\jhove-1_11\jhove\jhove.bat -c C:\Users\Martin\Documents\PspValidator\jhove-1_11\jhove\conf\jhove.conf -h XML -m jpeg2000-hul -k C:\Users\Martin\Documents\PspValidator\mc_b50eb6b0-f0a4-11e3-b72e-005056827e52_0001.jp2
        //works ok in cmd.exe
        util.defineToolExecution(OperatingSystem.WINDOWS,
                *//*new ExternalUtil.Command("C:\\Users\\Martin\\Documents\\PspValidator\\jhove-1_11\\jhove",
                        "jhove.bat -c C:\\Users\\Martin\\Documents\\PspValidator\\jhove-1_11\\jhove\\conf\\jhove.conf" +
                                " -h XML -m jpeg2000-hul -k ${TARGET_FILE}"),*//*
                new ExternalUtil.Command(null, "jhove.bat -c C:\\Users\\Martin\\Documents\\PspValidator\\jhove-1_11\\jhove\\conf\\jhove.conf" +
                        " -h XML -m jpeg2000-hul -k ${TARGET_FILE}"),

                new ExternalUtil.Parser(Stream.STDERR, null)
        );

        //Mac
        // TODO: 29.9.16

        return util;
    }

    private ExternalUtil kakadu() {
        ExternalUtil util = new ExternalUtil("kakadu");
        //Linux
        util.defineToolVersionDetection(OperatingSystem.LINUX,
                new ExternalUtil.Command(null, "kdu_expand -version"),
                // last line:
                // Current core system version is v7.8
                new ExternalUtil.Parser(Stream.STDOUT, "Current core system version is \\S*")
        );
        util.defineToolExecution(OperatingSystem.LINUX,
                new ExternalUtil.Command(null, "kdu_expand -i ${TARGET_FILE}"),
                new ExternalUtil.Parser(Stream.STDOUT, null)
        );

        //Windows
        util.defineToolVersionDetection(OperatingSystem.WINDOWS,
                //new ExternalUtil.Command("C:\\Program Files (x86)\\Kakadu", "kdu_expand.exe -version"),
                new ExternalUtil.Command(null, "kdu_expand.exe -version"),
                // last line:
                // Current core system version is v7.8
                new ExternalUtil.Parser(Stream.STDOUT, "Current core system version is \\S*")
        );
        util.defineToolExecution(OperatingSystem.WINDOWS,
                //new ExternalUtil.Command("C:\\Program Files (x86)\\Kakadu", "kdu_expand.exe -i ${TARGET_FILE}"),
                new ExternalUtil.Command(null, "kdu_expand.exe -i ${TARGET_FILE}"),
                new ExternalUtil.Parser(Stream.STDOUT, null)
        );

        //Mac
        // TODO: 29.9.16


        return util;
    }*/


}
