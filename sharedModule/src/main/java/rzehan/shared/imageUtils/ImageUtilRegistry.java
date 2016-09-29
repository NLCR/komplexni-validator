package rzehan.shared.imageUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Martin Řehánek on 29.9.16.
 * Temporary place for image utils.
 */
public class ImageUtilRegistry {

    private static Map<String, ImageUtil> imageUtilByName = init();

    private static Map<String, ImageUtil> init() {
        Map<String, ImageUtil> result = new HashMap<>();
        result.put("jpylyzer", jpylyzer());
        result.put("imageMagick", imagMagick());
        return result;
    }


    public static Map<String, ImageUtil> getImageUtilByName() {
        return imageUtilByName;
    }

    private static ImageUtil jpylyzer() {
        ImageUtil util = new ImageUtil("jpylyzer");
        //Linux
        util.getVersionDetectionByPlatform().put("Linux", new ImageUtil.VersionDetection("Linux",
                new ImageUtil.Execution("/usr/bin", "jpylyzer --version"),
                new ImageUtil.Parsing(Stream.STDERR, "*")
        ));
        //no installer available
        util.getRunByPlatform().put("Linux", new ImageUtil.VersionDetection("Linux",
                new ImageUtil.Execution("/usr/bin", "jpylyzer jp2In ${IMAGE_FILE}"),
                new ImageUtil.Parsing(Stream.STDOUT, "*")
        ));

        //Windows
        util.getVersionDetectionByPlatform().put("Windows", new ImageUtil.VersionDetection("Windows",
                new ImageUtil.Execution("C:\\Program Files\\jpylyzer", "jpylyzer --version"),
                new ImageUtil.Parsing(Stream.STDERR, "*")
        ));
        //no installer available
        util.getRunByPlatform().put("Windows", new ImageUtil.VersionDetection("Linux",
                new ImageUtil.Execution("C:\\Program Files\\jpylyzer", "jpylyzer jp2In ${IMAGE_FILE}"),
                new ImageUtil.Parsing(Stream.STDOUT, "*")
        ));

        //Mac
        // TODO: 29.9.16

        return util;
    }


    private static ImageUtil imagMagick() {
        ImageUtil util = new ImageUtil("imageMagick");
        //Linux
        util.getVersionDetectionByPlatform().put("Linux", new ImageUtil.VersionDetection("Linux",
                new ImageUtil.Execution("/usr/bin", "convert -version"),
                //first line
                //http://stackoverflow.com/questions/29546134/multiline-regex-match-only-the-first-line-ignore-the-rest-of-lines
                new ImageUtil.Parsing(Stream.STDOUT, "(?s)^\\d{3}[ -](.*?)( .*)*$")
        ));
        util.getInstallationByPlatform().put("Linux", new ImageUtil.Installation("Linux",
                new ImageUtil.Execution("//resources//bin", "ImageMagick.deb"))
        );
        util.getRunByPlatform().put("Linux", new ImageUtil.VersionDetection("Linux",
                new ImageUtil.Execution("/usr/bin", "identify -verbose ${IMAGE_FILE}"),
                new ImageUtil.Parsing(Stream.STDOUT, "*")
        ));

        //Windows
        util.getVersionDetectionByPlatform().put("Windows", new ImageUtil.VersionDetection("Windows",
                new ImageUtil.Execution("C:\\Program Files\\imageMagick", "convert -version"),
                //first line
                //http://stackoverflow.com/questions/29546134/multiline-regex-match-only-the-first-line-ignore-the-rest-of-lines
                new ImageUtil.Parsing(Stream.STDOUT, "(?s)^\\d{3}[ -](.*?)( .*)*$")
        ));
        util.getInstallationByPlatform().put("Windows", new ImageUtil.Installation("Linux",
                new ImageUtil.Execution("\\resources\\bin", "ImageMagick-7.0.2-4-Q16-x64-dll.exe"))
        );
        util.getRunByPlatform().put("Windows", new ImageUtil.VersionDetection("Linux",
                new ImageUtil.Execution("C:\\Program Files\\imageMagick", "identify -verbose ${IMAGE_FILE}"),
                new ImageUtil.Parsing(Stream.STDOUT, "*")
        ));

        //Mac
        // TODO: 29.9.16

        return util;
    }


}
