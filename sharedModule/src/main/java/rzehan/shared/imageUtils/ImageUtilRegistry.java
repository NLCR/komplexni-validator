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
        util.getRunByPlatform().put(OperatingSystem.LINUX, new ImageUtil.Run(
                OperatingSystem.LINUX,
                new ImageUtil.Execution("/usr/bin", "jpylyzer jp2In ${IMAGE_FILE}"),
                new ImageUtil.Parsing(Stream.STDOUT, null)
        ));

        //Windows
        util.getVersionDetectionByPlatform().put(OperatingSystem.WINDOWS, new ImageUtil.VersionDetection(
                OperatingSystem.WINDOWS,
                new ImageUtil.Execution("C:\\Users\\Martin\\Software\\jpylyzer_1.17.0_win64", "jpylyzer.exe --version"),
                //single STDERR line:
                // 1.17.0s
                new ImageUtil.Parsing(Stream.STDERR, REGEXP_FIRST_LINE)
        ));
        //FIXME: just freezes
        util.getRunByPlatform().put(OperatingSystem.WINDOWS, new ImageUtil.Run(
                OperatingSystem.WINDOWS,
                new ImageUtil.Execution("C:\\Users\\Martin\\Software\\jpylyzer_1.17.0_win64", "jpylyzer.exe jp2In ${IMAGE_FILE}"),
                //which stream here?
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
                //also on system path
                new ImageUtil.Execution("C:\\Program Files\\ImageMagick-7.0.3-Q16", "magick.exe convert -version"),
                //fist line:
                //Version: ImageMagick 6.7.7-10 2016-06-01 Q16 http://www.imagemagick.org
                new ImageUtil.Parsing(Stream.STDOUT, "ImageMagick\\s\\S*\\s\\S*")
        ));
        util.getRunByPlatform().put(OperatingSystem.WINDOWS, new ImageUtil.Run(
                OperatingSystem.WINDOWS,
                new ImageUtil.Execution("C:\\Program Files\\ImageMagick-7.0.3-Q16", "magick.exe identify -verbose ${IMAGE_FILE}"),
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
                //TODO: deal with this:
                //https://github.com/openpreserve/jhove/issues/32
                //until then temporary fix with configuration file
                new ImageUtil.Execution("C:\\Users\\Martin\\Documents\\PspValidator\\jhove-1_11\\jhove",
                        "jhove.bat -c C:\\Users\\Martin\\Documents\\PspValidator\\jhove-1_11\\jhove\\conf\\jhove.conf --version"),
                //first line of STDOUT looks like "Jhove (Rel. 1.6, 2011-01-04)"
                new ImageUtil.Parsing(Stream.STDOUT, REGEXP_FIRST_LINE)
        ));
        //FIXME: just freezes
        //C:\Users\Martin\Documents\PspValidator\jhove-1_11\jhove\jhove.bat -c C:\Users\Martin\Documents\PspValidator\jhove-1_11\jhove\conf\jhove.conf -h XML -m jpeg2000-hul -k C:\Users\Martin\Documents\PspValidator\mc_b50eb6b0-f0a4-11e3-b72e-005056827e52_0001.jp2
        //works ok in cmd.exe
        util.getRunByPlatform().put(OperatingSystem.WINDOWS, new ImageUtil.Run(
                OperatingSystem.WINDOWS,
                new ImageUtil.Execution("C:\\Users\\Martin\\Documents\\PspValidator\\jhove-1_11\\jhove",
                        "jhove.bat -c C:\\Users\\Martin\\Documents\\PspValidator\\jhove-1_11\\jhove\\conf\\jhove.conf" +
                                " -h XML -m jpeg2000-hul -k ${IMAGE_FILE}"),
                new ImageUtil.Parsing(Stream.STDERR, null)
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
                new ImageUtil.Execution("C:\\Program Files (x86)\\Kakadu", "kdu_expand.exe -version"),
                // last line:
                // Current core system version is v7.8
                new ImageUtil.Parsing(Stream.STDOUT, "Current core system version is \\S*")
        ));
        util.getRunByPlatform().put(OperatingSystem.WINDOWS, new ImageUtil.Run(
                OperatingSystem.WINDOWS,
                new ImageUtil.Execution("C:\\Program Files (x86)\\Kakadu", "kdu_expand.exe -i ${IMAGE_FILE}"),
                new ImageUtil.Parsing(Stream.STDOUT, null)
        ));

        //Mac
        // TODO: 29.9.16


        return util;
    }


}
