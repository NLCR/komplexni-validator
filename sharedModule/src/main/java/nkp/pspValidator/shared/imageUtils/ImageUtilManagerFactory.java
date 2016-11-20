package nkp.pspValidator.shared.imageUtils;

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
public class ImageUtilManagerFactory {

    private static ImageUtilManagerFactory instance;

    private static final String REGEXP_FIRST_LINE = "(?m)^.*$";

    private final Map<OperatingSystem, Map<ImageUtil, UtilHandler>> utilVersionDetectionInfoByOs = initProcessInfos();
    private final Map<OperatingSystem, Map<ImageUtil, UtilHandler>> utilExecutionInfoByOs = initProcessInfos();


    private Map<OperatingSystem, Map<ImageUtil, UtilHandler>> initProcessInfos() {
        Map<OperatingSystem, Map<ImageUtil, UtilHandler>> result = new HashMap<>();
        for (OperatingSystem os : OperatingSystem.values()) {
            result.put(os, new HashMap<>());
        }
        return result;
    }

    public ImageUtilManagerFactory(File configFile) throws ValidatorConfigurationException {
        XMLTag doc = XMLDoc.from(configFile, true);
        if (!"imageUtils".equals(doc.getCurrentTagName())) {
            throw new ValidatorConfigurationException("root element není dmf");
        }
        List<Element> imageUtilEls = getChildrenElementsByName(doc.getCurrentTag(), "imageUtil");
        for (Element imageUtilEl : imageUtilEls) {
            ImageUtil util = ImageUtil.valueOf(imageUtilEl.getAttribute("name"));

            Element versionDetectionEl = getFirstChildElementsByName(imageUtilEl, "versionDetection");
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

            Element executionEl = getFirstChildElementsByName(imageUtilEl, "execution");
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

    public ImageUtilManager buildImageUtilManager(OperatingSystem os) {
        return new ImageUtilManager(utilVersionDetectionInfoByOs.get(os), utilExecutionInfoByOs.get(os));
    }

    private void init() {
        //JPYLYZER
        //single STDERR line:"1.17.0"
        addVersionDetection(OperatingSystem.LINUX, ImageUtil.JPYLYZER, "jpylyzer --version", Stream.STDERR, REGEXP_FIRST_LINE);
        addUtilityExecution(OperatingSystem.LINUX, ImageUtil.JPYLYZER, "jpylyzer jp2In ${IMAGE_FILE}", Stream.STDOUT, null);
        //single STDERR line:"1.17.0s"
        addVersionDetection(OperatingSystem.WINDOWS, ImageUtil.JPYLYZER, "jpylyzer.exe --version", Stream.STDERR, REGEXP_FIRST_LINE);
        //FIXME: just freezes
        addUtilityExecution(OperatingSystem.WINDOWS, ImageUtil.JPYLYZER, "jpylyzer.exe jp2In ${IMAGE_FILE}", Stream.STDOUT, null);

        //IMAGE MAGICK
        //first line:
        // Version: ImageMagick 6.7.7-10 2016-06-01 Q16 http://www.imagemagick.org
        addVersionDetection(OperatingSystem.LINUX, ImageUtil.IMAGE_MAGICK, "convert -version", Stream.STDOUT, "ImageMagick\\s\\S*\\s\\S*");
        addUtilityExecution(OperatingSystem.LINUX, ImageUtil.IMAGE_MAGICK, "identify -verbose ${IMAGE_FILE}", Stream.STDOUT, null);
        //fist line:
        //Version: ImageMagick 6.7.7-10 2016-06-01 Q16 http://www.imagemagick.org
        addVersionDetection(OperatingSystem.WINDOWS, ImageUtil.IMAGE_MAGICK, "magick.exe convert -version", Stream.STDOUT, "ImageMagick\\s\\S*\\s\\S*");
        addUtilityExecution(OperatingSystem.WINDOWS, ImageUtil.IMAGE_MAGICK, "magick.exe identify -verbose ${IMAGE_FILE}", Stream.STDOUT, null);

        //JHOVE
        //first line of STDOUT looks like:
        //Jhove (Rel. 1.6, 2011-01-04)
        addVersionDetection(OperatingSystem.LINUX, ImageUtil.JHOVE, "jhove --version", Stream.STDOUT, REGEXP_FIRST_LINE);
        addUtilityExecution(OperatingSystem.LINUX, ImageUtil.JHOVE, "jhove -h XML -m jpeg2000-hul -k ${IMAGE_FILE}", Stream.STDOUT, null);
        //first line of STDOUT:
        //Jhove (Rel. 1.6, 2011-01-04)
        //TODO: deal with this:
        //https://github.com/openpreserve/jhove/issues/32
        //until then temporary fix with configuration file
        addVersionDetection(OperatingSystem.WINDOWS, ImageUtil.JHOVE, "jhove.bat -c C:\\Users\\Martin\\Documents\\PspValidator\\jhove-1_11\\jhove\\conf\\jhove.conf --version", Stream.STDOUT, REGEXP_FIRST_LINE);
        //FIXME: just freezes
        //C:\Users\Martin\Documents\PspValidator\jhove-1_11\jhove\jhove.bat -c C:\Users\Martin\Documents\PspValidator\jhove-1_11\jhove\conf\jhove.conf -h XML -m jpeg2000-hul -k C:\Users\Martin\Documents\PspValidator\mc_b50eb6b0-f0a4-11e3-b72e-005056827e52_0001.jp2
        //works ok in cmd.exe
        //opravdu stderr?
        addUtilityExecution(OperatingSystem.WINDOWS, ImageUtil.JHOVE, "jhove.bat -c C:\\Users\\Martin\\Documents\\PspValidator\\jhove-1_11\\jhove\\conf\\jhove.conf" +
                " -h XML -m jpeg2000-hul -k ${IMAGE_FILE}", Stream.STDERR, null);

        //KAKADU
        // last line:
        // Current core system version is v7.8
        addVersionDetection(OperatingSystem.LINUX, ImageUtil.KAKADU, "kdu_expand -version", Stream.STDOUT, "Current core system version is \\S*");
        addUtilityExecution(OperatingSystem.LINUX, ImageUtil.KAKADU, "kdu_expand -i ${IMAGE_FILE}", Stream.STDOUT, null);

        addVersionDetection(OperatingSystem.WINDOWS, ImageUtil.KAKADU, "kdu_expand.exe -version", Stream.STDOUT, "Current core system version is \\S*");
        addUtilityExecution(OperatingSystem.WINDOWS, ImageUtil.KAKADU, "kdu_expand.exe -i ${IMAGE_FILE}", Stream.STDOUT, null);
    }


    private void addVersionDetection(OperatingSystem os, ImageUtil util, String rawCommand, Stream outStream, String outRegexp) {
        utilVersionDetectionInfoByOs.get(os).put(util,
                new UtilHandler(
                        new UtilHandler.Command(rawCommand),
                        new UtilHandler.Parser(outStream, outRegexp)
                )
        );
    }

    private void addUtilityExecution(OperatingSystem os, ImageUtil util, String rawCommand, Stream outStream, String outRegexp) {
        utilExecutionInfoByOs.get(os).put(util,
                new UtilHandler(
                        new UtilHandler.Command(rawCommand),
                        new UtilHandler.Parser(outStream, outRegexp)
                )
        );
    }

   /* private ImageUtil jpylyzer() {
        ImageUtil util = new ImageUtil("jpylyzer");
        //Linux
        util.defineToolVersionDetection(OperatingSystem.LINUX,
                //new ImageUtil.Command(new File("/usr/bin"), "jpylyzer --version"),
                new ImageUtil.Command(null, "jpylyzer --version"),
                //jediny radek STDERR obsahuje verzi typu "1.17.0"
                new ImageUtil.Parser(Stream.STDERR, REGEXP_FIRST_LINE)
        );


        util.defineToolExecution(OperatingSystem.LINUX,
                //new ImageUtil.Command(new File("/usr/bin"), "jpylyzer jp2In ${IMAGE_FILE}"),
                new ImageUtil.Command(null, "jpylyzer jp2In ${IMAGE_FILE}"),
                new ImageUtil.Parser(Stream.STDOUT, null)
        );


        //Windows
        util.defineToolVersionDetection(OperatingSystem.WINDOWS,
                //new ImageUtil.Command("C:\\Users\\Martin\\Software\\jpylyzer_1.17.0_win64", "jpylyzer.exe --version"),
                new ImageUtil.Command(null, "jpylyzer.exe --version"),
                //single STDERR line:
                // 1.17.0s
                new ImageUtil.Parser(Stream.STDERR, REGEXP_FIRST_LINE)
        );
        //FIXME: just freezes
        util.defineToolExecution(OperatingSystem.WINDOWS,
                //new ImageUtil.Command("C:\\Users\\Martin\\Software\\jpylyzer_1.17.0_win64", "jpylyzer.exe jp2In ${IMAGE_FILE}"),
                new ImageUtil.Command(null, "jpylyzer.exe jp2In ${IMAGE_FILE}"),
                //which stream here?
                new ImageUtil.Parser(Stream.STDOUT, null)
        );

        //Mac
        // TODO: 29.9.16

        return util;
    }


    private ImageUtil imageMagick() {
        ImageUtil util = new ImageUtil("imageMagick");
        //Linux
        util.defineToolVersionDetection(OperatingSystem.LINUX,
                //new ImageUtil.Command("/usr/bin", "convert -version"),
                new ImageUtil.Command(null, "convert -version"),
                //first line:
                //Version: ImageMagick 6.7.7-10 2016-06-01 Q16 http://www.imagemagick.org
                new ImageUtil.Parser(Stream.STDOUT, "ImageMagick\\s\\S*\\s\\S*")
        );
        util.defineToolExecution(OperatingSystem.LINUX,
                //new ImageUtil.Command("/usr/bin", "identify -verbose ${IMAGE_FILE}"),
                new ImageUtil.Command(null, "identify -verbose ${IMAGE_FILE}"),
                new ImageUtil.Parser(Stream.STDOUT, null)
        );

        //Windows
        util.defineToolVersionDetection(OperatingSystem.WINDOWS,
                //also on system path
                //new ImageUtil.Command("C:\\Program Files\\ImageMagick-7.0.3-Q16", "magick.exe convert -version"),
                new ImageUtil.Command(null, "magick.exe convert -version"),
                //fist line:
                //Version: ImageMagick 6.7.7-10 2016-06-01 Q16 http://www.imagemagick.org
                new ImageUtil.Parser(Stream.STDOUT, "ImageMagick\\s\\S*\\s\\S*")
        );
        util.defineToolExecution(OperatingSystem.WINDOWS,
                //new ImageUtil.Command("C:\\Program Files\\ImageMagick-7.0.3-Q16", "magick.exe identify -verbose ${IMAGE_FILE}"),
                new ImageUtil.Command(null, "magick.exe identify -verbose ${IMAGE_FILE}"),
                new ImageUtil.Parser(Stream.STDOUT, null)
        );

        //Mac
        // TODO: 29.9.16

        return util;
    }

    private ImageUtil jhove() {
        ImageUtil util = new ImageUtil("jhove");
        //Linux
        util.defineToolVersionDetection(OperatingSystem.LINUX,
                //new ImageUtil.Command("/usr/bin", "jhove --version"),
                new ImageUtil.Command(null, "jhove --version"),
                //first line of STDOUT looks like "Jhove (Rel. 1.6, 2011-01-04)"
                new ImageUtil.Parser(Stream.STDOUT, REGEXP_FIRST_LINE)
        );
        util.defineToolExecution(OperatingSystem.LINUX,
                //new ImageUtil.Command("/usr/bin", "jhove -h XML -m jpeg2000-hul -k ${IMAGE_FILE}"),
                new ImageUtil.Command(null, "jhove -h XML -m jpeg2000-hul -k ${IMAGE_FILE}"),
                new ImageUtil.Parser(Stream.STDOUT, null)
        );

        //Windows
        util.defineToolVersionDetection(OperatingSystem.WINDOWS,
                //TODO: deal with this:
                //https://github.com/openpreserve/jhove/issues/32
                //until then temporary fix with configuration file
                *//*new ImageUtil.Command("C:\\Users\\Martin\\Documents\\PspValidator\\jhove-1_11\\jhove",
                        "jhove.bat -c C:\\Users\\Martin\\Documents\\PspValidator\\jhove-1_11\\jhove\\conf\\jhove.conf --version"),*//*
                new ImageUtil.Command(null, "jhove.bat -c C:\\Users\\Martin\\Documents\\PspValidator\\jhove-1_11\\jhove\\conf\\jhove.conf --version"),
                //first line of STDOUT looks like "Jhove (Rel. 1.6, 2011-01-04)"
                new ImageUtil.Parser(Stream.STDOUT, REGEXP_FIRST_LINE)
        );
        //FIXME: just freezes
        //C:\Users\Martin\Documents\PspValidator\jhove-1_11\jhove\jhove.bat -c C:\Users\Martin\Documents\PspValidator\jhove-1_11\jhove\conf\jhove.conf -h XML -m jpeg2000-hul -k C:\Users\Martin\Documents\PspValidator\mc_b50eb6b0-f0a4-11e3-b72e-005056827e52_0001.jp2
        //works ok in cmd.exe
        util.defineToolExecution(OperatingSystem.WINDOWS,
                *//*new ImageUtil.Command("C:\\Users\\Martin\\Documents\\PspValidator\\jhove-1_11\\jhove",
                        "jhove.bat -c C:\\Users\\Martin\\Documents\\PspValidator\\jhove-1_11\\jhove\\conf\\jhove.conf" +
                                " -h XML -m jpeg2000-hul -k ${IMAGE_FILE}"),*//*
                new ImageUtil.Command(null, "jhove.bat -c C:\\Users\\Martin\\Documents\\PspValidator\\jhove-1_11\\jhove\\conf\\jhove.conf" +
                        " -h XML -m jpeg2000-hul -k ${IMAGE_FILE}"),

                new ImageUtil.Parser(Stream.STDERR, null)
        );

        //Mac
        // TODO: 29.9.16

        return util;
    }

    private ImageUtil kakadu() {
        ImageUtil util = new ImageUtil("kakadu");
        //Linux
        util.defineToolVersionDetection(OperatingSystem.LINUX,
                new ImageUtil.Command(null, "kdu_expand -version"),
                // last line:
                // Current core system version is v7.8
                new ImageUtil.Parser(Stream.STDOUT, "Current core system version is \\S*")
        );
        util.defineToolExecution(OperatingSystem.LINUX,
                new ImageUtil.Command(null, "kdu_expand -i ${IMAGE_FILE}"),
                new ImageUtil.Parser(Stream.STDOUT, null)
        );

        //Windows
        util.defineToolVersionDetection(OperatingSystem.WINDOWS,
                //new ImageUtil.Command("C:\\Program Files (x86)\\Kakadu", "kdu_expand.exe -version"),
                new ImageUtil.Command(null, "kdu_expand.exe -version"),
                // last line:
                // Current core system version is v7.8
                new ImageUtil.Parser(Stream.STDOUT, "Current core system version is \\S*")
        );
        util.defineToolExecution(OperatingSystem.WINDOWS,
                //new ImageUtil.Command("C:\\Program Files (x86)\\Kakadu", "kdu_expand.exe -i ${IMAGE_FILE}"),
                new ImageUtil.Command(null, "kdu_expand.exe -i ${IMAGE_FILE}"),
                new ImageUtil.Parser(Stream.STDOUT, null)
        );

        //Mac
        // TODO: 29.9.16


        return util;
    }*/


}
