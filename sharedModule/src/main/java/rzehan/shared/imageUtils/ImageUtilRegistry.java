package rzehan.shared.imageUtils;

import rzehan.shared.OperatingSystem;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Martin Řehánek on 29.9.16.
 * Temporary place for image utils.
 */
public class ImageUtilRegistry {

    private static final String REGEXP_FIRST_LINE = "(?m)^.*$";

    private static Map<String, ImageUtil> imageUtilByName = init();

    private static Map<String, ImageUtil> init() {
        Map<String, ImageUtil> result = new HashMap<>();
        result.put("jpylyzer", jpylyzer());
        result.put("imageMagick", imageMagick());
        result.put("jhove", jhove());
        result.put("kakadu", kakadu());
        return result;
    }

    public static Map<String, ImageUtil> getImageUtilByName() {
        return imageUtilByName;
    }

    private static ImageUtil jpylyzer() {
        ImageUtil util = new ImageUtil("jpylyzer");
        //Linux
        util.getVersionDetectionByPlatform().put(OperatingSystem.LINUX, new ImageUtil.VersionDetection(
                OperatingSystem.LINUX,
                new ImageUtil.Execution("/usr/bin", "jpylyzer --version"),
                //jediny radek STDERR obsahuje verzi typu "1.17.0"
                new ImageUtil.Parsing(Stream.STDERR, REGEXP_FIRST_LINE)
        ));
        //no installer available
        util.getRunByPlatform().put(OperatingSystem.LINUX, new ImageUtil.Run(
                OperatingSystem.LINUX,
                new ImageUtil.Execution("/usr/bin", "jpylyzer jp2In ${IMAGE_FILE}"),
                new ImageUtil.Parsing(Stream.STDOUT, null)
        ));

        //Windows
        util.getVersionDetectionByPlatform().put(OperatingSystem.WINDOWS, new ImageUtil.VersionDetection(
                OperatingSystem.WINDOWS,
                new ImageUtil.Execution("C:\\Program Files\\jpylyzer", "jpylyzer --version"),
                //jediny radek STDERR obsahuje verzi typu "1.17.0"
                new ImageUtil.Parsing(Stream.STDERR, REGEXP_FIRST_LINE)
        ));
        //no installer available
        util.getRunByPlatform().put(OperatingSystem.WINDOWS, new ImageUtil.Run(
                OperatingSystem.WINDOWS,
                new ImageUtil.Execution("C:\\Program Files\\jpylyzer", "jpylyzer jp2In ${IMAGE_FILE}"),
                new ImageUtil.Parsing(Stream.STDOUT, null)
        ));

        //Mac
        // TODO: 29.9.16

        return util;
    }


    private static ImageUtil imageMagick() {
        ImageUtil util = new ImageUtil("imageMagick");
        //Linux
        util.getVersionDetectionByPlatform().put(OperatingSystem.LINUX,
                new ImageUtil.VersionDetection(
                        OperatingSystem.LINUX,
                        new ImageUtil.Execution("/usr/bin", "convert -version"),
                        //first line:
                        //Version: ImageMagick 6.7.7-10 2016-06-01 Q16 http://www.imagemagick.org
                        new ImageUtil.Parsing(Stream.STDOUT, "ImageMagick\\s\\S*\\s\\S*")
                ));
        util.getRunByPlatform().put(OperatingSystem.LINUX, new ImageUtil.Run(
                OperatingSystem.LINUX,
                new ImageUtil.Execution("/usr/bin", "identify -verbose ${IMAGE_FILE}"),
                new ImageUtil.Parsing(Stream.STDOUT, null)
        ));

        //Windows
        util.getVersionDetectionByPlatform().put(OperatingSystem.WINDOWS, new ImageUtil.VersionDetection(
                OperatingSystem.WINDOWS,
                new ImageUtil.Execution("C:\\Program Files\\imageMagick", "convert -version"),
                //fist line:
                //Version: ImageMagick 6.7.7-10 2016-06-01 Q16 http://www.imagemagick.org
                new ImageUtil.Parsing(Stream.STDOUT, "ImageMagick\\s\\S*\\s\\S*")
        ));
        util.getRunByPlatform().put(OperatingSystem.WINDOWS, new ImageUtil.Run(
                OperatingSystem.WINDOWS,
                new ImageUtil.Execution("C:\\Program Files\\imageMagick", "identify -verbose ${IMAGE_FILE}"),
                new ImageUtil.Parsing(Stream.STDOUT, null)
        ));

        //Mac
        // TODO: 29.9.16

        return util;
    }

    private static ImageUtil jhove() {
        ImageUtil util = new ImageUtil("jhove");
        //Linux
        util.getVersionDetectionByPlatform().put(OperatingSystem.LINUX, new ImageUtil.VersionDetection(
                OperatingSystem.LINUX,
                new ImageUtil.Execution("/usr/bin", "jhove --version"),
                //first line of STDOUT looks like "Jhove (Rel. 1.6, 2011-01-04)"
                new ImageUtil.Parsing(Stream.STDOUT, REGEXP_FIRST_LINE)
        ));
        util.getRunByPlatform().put(OperatingSystem.LINUX, new ImageUtil.Run(
                OperatingSystem.LINUX,
                new ImageUtil.Execution("/usr/bin", "jhove -h XML -m jpeg2000-hul -k ${IMAGE_FILE}"),
                new ImageUtil.Parsing(Stream.STDOUT, null)
        ));

        //Windows
        util.getVersionDetectionByPlatform().put(OperatingSystem.WINDOWS, new ImageUtil.VersionDetection(
                OperatingSystem.WINDOWS,
                new ImageUtil.Execution("C:\\Program Files\\jhove", "jhove --version"),
                //first line of STDOUT looks like "Jhove (Rel. 1.6, 2011-01-04)"
                new ImageUtil.Parsing(Stream.STDOUT, REGEXP_FIRST_LINE)
        ));
        util.getRunByPlatform().put(OperatingSystem.WINDOWS, new ImageUtil.Run(
                OperatingSystem.WINDOWS,
                new ImageUtil.Execution("C:\\Program Files\\jhove", "jhove -h XML -m jpeg2000-hul -k ${IMAGE_FILE}"),
                new ImageUtil.Parsing(Stream.STDOUT, null)
        ));

        //Mac
        // TODO: 29.9.16

        return util;
    }

    private static ImageUtil kakadu() {
        ImageUtil util = new ImageUtil("kakadu");
        //Linux
        util.getVersionDetectionByPlatform().put(OperatingSystem.LINUX, new ImageUtil.VersionDetection(
                OperatingSystem.LINUX,
                new ImageUtil.Execution(null, "kdu_expand -version"),
                // last line:
                // Current core system version is v7.8
                new ImageUtil.Parsing(Stream.STDOUT, "Current core system version is \\S*")
        ));
        util.getRunByPlatform().put(OperatingSystem.LINUX, new ImageUtil.Run(
                OperatingSystem.LINUX,
                new ImageUtil.Execution(null, "kdu_expand -i ${IMAGE_FILE}"),
                new ImageUtil.Parsing(Stream.STDOUT, null)
        ));

        //Windows
        util.getVersionDetectionByPlatform().put(OperatingSystem.WINDOWS, new ImageUtil.VersionDetection(
                OperatingSystem.WINDOWS,
                new ImageUtil.Execution("C:\\Program Files\\kakadu", "kdu_expand"),
                // last line:
                // Current core system version is v7.8
                new ImageUtil.Parsing(Stream.STDOUT, "Current core system version is \\S*")
        ));
        util.getRunByPlatform().put(OperatingSystem.WINDOWS, new ImageUtil.Run(
                OperatingSystem.WINDOWS,
                new ImageUtil.Execution("C:\\Program Files\\kakadu", "kdu_expand -i ${IMAGE_FILE}"),
                new ImageUtil.Parsing(Stream.STDOUT, null)
        ));

        //Mac
        // TODO: 29.9.16

        return util;
    }


}
