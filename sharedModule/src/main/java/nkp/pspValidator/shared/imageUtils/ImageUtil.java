package nkp.pspValidator.shared.imageUtils;

/**
 * Created by martin on 14.11.16.
 */
public enum ImageUtil {

    JPYLYZER("jpylyzer.xml"),
    IMAGE_MAGICK("imageMagick.xml"),
    JHOVE("jhove.xml"),
    KAKADU("kakadu.xml");

    private String profileFileName;

    ImageUtil(String profileFileName) {
        this.profileFileName = profileFileName;
    }

    public String getProfileFileName() {
        return profileFileName;
    }

}
