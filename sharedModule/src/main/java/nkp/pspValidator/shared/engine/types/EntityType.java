package nkp.pspValidator.shared.engine.types;

/**
 * Created by Martin Řehánek on 28.11.16.
 */
public enum EntityType {

    VOLUME("VOLUME"), TITLE("TITLE"), SUPPLEMENT("SUPPL"), PICTURE("PICT"), PAGE("PAGE"),//spolecne
    ISSUE("ISSUE"), ARTICLE("ART"), //jen periodika
    CHAPTER("CHAP"), //jen monografie
    SOUNDCOLLECTION("SOUNDCOLLECTION"), SOUNDRECORDING("SOUNDRECORDING"), SOUNDPART("SOUNDPART"),//jen zvuky
    ;

    private final String dmdSecCode;

    EntityType(String dmdSecCode) {
        this.dmdSecCode = dmdSecCode;
    }

    public String getDmdSecCode() {
        return dmdSecCode;
    }
}