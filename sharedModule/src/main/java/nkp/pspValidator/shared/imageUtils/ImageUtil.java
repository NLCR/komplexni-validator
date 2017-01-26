package nkp.pspValidator.shared.imageUtils;

/**
 * Created by Martin Řehánek on 14.11.16.
 */
public enum ImageUtil {

    IMAGE_MAGICK("ImageMagick", "imageMagick.xml"),
    JHOVE("JHOVE", "jhove.xml"),
    JPYLYZER("Jpylyzer", "jpylyzer.xml"),
    KAKADU("Kakadu", "kakadu.xml");

    private final String userFriendlyName;
    private final String profileFileName;

    ImageUtil(String userFriendlyName, String profileFileName) {
        this.userFriendlyName = userFriendlyName;
        this.profileFileName = profileFileName;
    }

    public String getUserFriendlyName() {
        return userFriendlyName;
    }

    public String getProfileFileName() {
        return profileFileName;
    }

}
