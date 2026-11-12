package nkp.pspValidator.shared.externalUtils;

/**
 * Created by Martin Řehánek on 14.11.16.
 */
public enum ExternalUtil {

    IMAGE_MAGICK("ImageMagick", "imageMagick.xml"),
    JHOVE("JHOVE", "jhove.xml"),
    JPYLYZER("Jpylyzer", "jpylyzer.xml"),
    KAKADU("Kakadu", "kakadu.xml"),
    MP3VAL("MP3val", "mp3val.xml"),
    SHNTOOL("shntool", "shntool.xml"),
    CHECKMATE("Checkmate", "checkmate.xml"),
    VERAPDF("veraPDF", "veraPdf.xml"),
    EPUBCHECK("EPUBCheck", "epubCheck.xml");

    private final String userFriendlyName;
    private final String profileFileName;

    ExternalUtil(String userFriendlyName, String profileFileName) {
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
