package nkp.pspValidator.cli;

import nkp.pspValidator.shared.FdmfRegistry;
import nkp.pspValidator.shared.Platform;
import nkp.pspValidator.shared.Validator;
import nkp.pspValidator.shared.engine.exceptions.InvalidXPathExpressionException;
import nkp.pspValidator.shared.engine.exceptions.PspDataException;
import nkp.pspValidator.shared.engine.exceptions.ValidatorConfigurationException;
import nkp.pspValidator.shared.engine.exceptions.XmlFileParsingException;
import nkp.pspValidator.cli.Action;
import nkp.pspValidator.cli.Main;
import nkp.pspValidator.cli.Params;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by Martin Řehánek on 10.11.16.
 */
public class MainTest {

    private static final String HOME_DIR = System.getProperty("user.home");

    private static final String MON_1_4 = HOME_DIR + "/IdeaProjects/komplexni-validator-samples/src/test/resources/monografie/abc135-000itk";
    private static final String MON_1_2 = "../sharedModule/src/test/resources/monograph_1.2/b50eb6b0-f0a4-11e3-b72e-005056827e52";
    private static final String MON_1_2_MAP = "../sharedModule/src/test/resources/monograph_1.2_map/6e9a7000-65c0-11e6-85af-005056827e52";
    private static final String MON_1_2_INVALID_IMAGES = "../sharedModule/src/test/resources/monograph_1.2-invalid_images/b50eb6b0-f0a4-11e3-b72e-005056827e52";
    private static final String MON_2_0_SINGLEVOLUME = HOME_DIR + "/IdeaProjects/komplexni-validator-samples/src/test/resources/monografie/vzorovy_balicek_monografie_2.1_jednosvazkova/0ad7d080-aa89-11ec-adf7-5ef3fc9bb22f";
    private static final String MON_2_0_MULTIVOLUME = HOME_DIR + "/IdeaProjects/komplexni-validator-samples/src/test/resources/monografie/vzorovy_balicek_monografie_2.1_vicesvazkova/47647030-3fb3-11e7-ad33-5ef3fc9ae867";
    private static final String MON_2_1_MULTIVOLUME = HOME_DIR + "/TrineraProjects/Validator/samples-new/Vzorovy_balicek_vicesvazek_2_1/47647030-3fb3-11e7-ad33-5ef3fc9ae867";


    private static final String PER_1_6 = "../sharedModule/src/test/resources/periodical_1.6/7033d800-0935-11e4-beed-5ef3fc9ae867";

    private static final String ZIP_1 = HOME_DIR + "/zakazky/NKP-Komplexni_Validator/data/group/7033d800-0935-11e4-beed-5ef3fc9ae867.zip";
    private static final String ZIP_2 = HOME_DIR + "/zakazky/NKP-Komplexni_Validator/data/group/ope301-00000v.zip";
    private static final String ZIP_NOT_ZIP = HOME_DIR + "/zakazky/NKP-Komplexni_Validator/data/group/not_a_zip.txt";

    private static final String GROUP = HOME_DIR + "/zakazky/NKP-Komplexni_Validator/data/group/";
    private static final String GROUP_ZIP = HOME_DIR + "/zakazky/NKP-Komplexni_Validator/data/group.zip";

    //private static final String PER_1_6 = "../sharedModule/src/test/resources/periodical_1.6/ope301-00000v";
    //private static final String PER_1_6_INFO_INVALID_NS = HOME_DIR + "/zakazky/NKP-Komplexni_Validator/data/per_1.6_invalid_info_ns/aba008-000310";

    private static final String PER_1_4 = "../sharedModule/src/test/resources/periodical_1.4/ope301-00000v";
    private static final String PER_2_0 = HOME_DIR + "/TrineraProjects/Validator/samples-new/vzorovy_balicek_periodika_2/a685f910-f6ee-11ec-aa77-005056827e52";


    @org.junit.Test
    public void cli() throws InvalidXPathExpressionException, PspDataException, ValidatorConfigurationException, XmlFileParsingException, FdmfRegistry.UnknownFdmfException {
        //temporarily disable tests TODO: fix and enable
        //if (true) return;

        Platform platform = Platform.detectOs();
        String configDir = null;
        String imageMagickPath = null;
        String jhovePath = null;
        String jpylyzerPath = null;
        String kakaduPath = null;
        String xmlProtocolsDir = null;
        String xmlProtocolFile = null;

        String MON_1_2_INVALIDIMAGES = null;

        switch (platform.getOperatingSystem()) {
            case WINDOWS:
                configDir = "..\\sharedModule\\src\\main\\resources\\nkp\\pspValidator\\shared\\validatorConfig";
                imageMagickPath = "C:\\Program Files\\ImageMagick-7.0.3-Q16";
                jhovePath = HOME_DIR + "\\OneDrive\\Dokumenty\\software\\jhove-1.29";
                jpylyzerPath = HOME_DIR + "\\OneDrive\\Dokumenty\\software\\jpylyzer_2.2.0_win64";
                kakaduPath = "C:\\Program Files (x86)\\Kakadu\\";
                xmlProtocolsDir = HOME_DIR + "\\komplexni-validator-protocols";
                MON_1_2_INVALIDIMAGES = HOME_DIR + "\\tmp\\b50eb6b0-f0a4-11e3-b72e-005056827e52";
                break;
            case LINUX:
                configDir = "../sharedModule/src/main/resources/nkp/pspValidator/shared/validatorConfig";
                jhovePath = HOME_DIR + "/Software/jhove-1.29";
                kakaduPath = HOME_DIR + "/zakazky/NKP-Komplexni_Validator/utility/kakadu/KDU78_Demo_Apps_for_Linux-x86-64_160226";
                xmlProtocolsDir = HOME_DIR + "/tmp/komplexni-validator-protocols";
                MON_1_2_INVALIDIMAGES = HOME_DIR + "/tmp/b50eb6b0-f0a4-11e3-b72e-005056827e52";
                break;
            case MAC:
                configDir = "../sharedModule/src/main/resources/nkp/pspValidator/shared/validatorConfig";
                jhovePath = HOME_DIR + "/Software/jhove-1.29";
                imageMagickPath = "/opt/local/bin";
                jpylyzerPath = HOME_DIR + "/Software/jpylyzer-1.17.0/jpylyzer";
                xmlProtocolsDir = HOME_DIR + "/tmp/komplexni-validator-protocols";
                xmlProtocolFile = HOME_DIR + "/tmp/komplexni-validator-protocols/protocol.xml";
                MON_1_2_INVALIDIMAGES = HOME_DIR + "/tmp/b50eb6b0-f0a4-11e3-b72e-005056827e52";
                break;
        }

        Validator.DevParams devParams = new Validator.DevParams();

        //devParams.getSectionsToRun().add("Soubor CHECKSUM");
        //devParams.getSectionsToRun().add("Soubor INFO");
        //devParams.getSectionsToRun().add("Struktura souborů");
        //devParams.getSectionsToRun().add("Bibliografická metadata");
        devParams.getSectionsToRun().add("Identifikátory");
        //devParams.getSectionsToRun().add("Obrazová data");
        //devParams.getSectionsToRun().add("OCR ALTO");
        //devParams.getSectionsToRun().add("OCR TEXT");
        //devParams.getSectionsToRun().add("TEST");
        /*
        devParams.getSectionsToRun().add("Technická metadata");
        devParams.getSectionsToRun().add("METS hlavičky");
        devParams.getSectionsToRun().add("Autorskoprávní metadata");
        devParams.getSectionsToRun().add("Sekundární METS filesec");
        devParams.getSectionsToRun().add("Strukturální mapy");*/

        Main.main(devParams, buildParams(
                Action.VALIDATE_PSP,
                //Action.VALIDATE_PSP_GROUP,
                configDir
                , "/tmp"

                //, MON_1_2
                , MON_2_1_MULTIVOLUME
                //, MON_1_2_MAP
                //, MON_1_2_INVALID_IMAGES
                //, MON_1_4
                //, MON_1_2_INVALIDIMAGES
                //, MON_2_0_SINGLEVOLUME
                //, MON_2_0_MULTIVOLUME
                //, PER_1_4
                //, PER_1_6
                //, PER_2_0
                //, PER_1_6_INFO_INVALID_NS
                //, ZIP_1
                //, ZIP_NOT_ZIP

                , null//GROUP
                //, GROUP_ZIP

                //preferred DMF versions
                , null
                //,"1.0"
                //,"1.2"
                , null
                //, "1.4"
                //, "1.6"
                //, null

                //forced DMF versions
                , "2.2"
                , "2.1"
                //, "1.4"
                //, "1.6"
                , 2 //verbosity (3: vsechno, 2: jen sekce a pravidla s chybami a popisy jednotlivych chyb, 1: jen pocty chyb v sekcich s chybami, bez popisu jednotlivych chyb, 0: nic)
                , xmlProtocolsDir //xml protocol dir
                , xmlProtocolFile //xml protocol
                , imageMagickPath //null //imageMagick path
                , jhovePath //jhove path
                , jpylyzerPath //jpylyzer path
                , kakaduPath  //kakadu path
                , true //disable imageMagick
                , true //disable jhove
                , true //disable jpylyzer
                , true//disable kakadu
                , true //disable mp3val
                , true //disable shntool
                , true //disable checkmate
        ));
    }

    private String[] buildParams(Action action,
                                 String configDir, String tmpDir,
                                 String pspDir, String pspGroupDir,
                                 String preferDmfMonVersion, String preferDmfPerVersion, String forceDmfMonVersion, String forceDmfPerVersion,
                                 Integer verbosity, String xmlProtocolDir, String xmlProtocolFile,
                                 String imageMagickPath, String jhovePath, String jpylyzerPath, String kakaduPath,
                                 boolean disableImageMagick, boolean disableJhove, boolean disableJpylyzer, boolean disableKakadu,
                                 boolean disableMp3val, boolean disableShntool, boolean disableCheckmate
    ) {
        List<String> params = new ArrayList<>();
        //action
        params.add(String.format("--%s", Params.ACTION));
        params.add(action.toString());
        //config dir
        params.add(String.format("--%s", Params.CONFIG_DIR));
        params.add(configDir);
        //psp
        if (pspDir != null) {
            params.add(String.format("--%s", Params.PSP));
            params.add(pspDir);
        }
        //psp-group
        if (pspGroupDir != null) {
            params.add(String.format("--%s", Params.PSP_GROUP));
            params.add(pspGroupDir);
        }

        //xml protocol
        if (xmlProtocolDir != null) {
            params.add(String.format("--%s", Params.XML_PROTOCOL_DIR));
            params.add(xmlProtocolDir);
        }
        if (xmlProtocolFile != null) {
            params.add(String.format("--%s", Params.XML_PROTOCOL_FILE));
            params.add(xmlProtocolFile);
        }

        //tmp-dir
        if (tmpDir != null) {
            params.add(String.format("--%s", Params.TMP_DIR));
            params.add(tmpDir);
        }

        //TODO: quit-after-nth-invalid-psp

        //DMF versions
        if (preferDmfMonVersion != null) {
            params.add(String.format("--%s", Params.PREFERRED_DMF_MON_VERSION));
            params.add(preferDmfMonVersion);
        }
        if (preferDmfPerVersion != null) {
            params.add(String.format("--%s", Params.PREFERRED_DMF_PER_VERSION));
            params.add(preferDmfPerVersion);
        }
        if (forceDmfMonVersion != null) {
            params.add(String.format("--%s", Params.FORCED_DMF_MON_VERSION));
            params.add(forceDmfMonVersion);
        }
        if (forceDmfPerVersion != null) {
            params.add(String.format("--%s", Params.FORCED_DMF_PER_VERSION));
            params.add(forceDmfPerVersion);
        }

        //verbosity
        if (verbosity != null) {
            params.add(String.format("--%s", Params.VERBOSITY));
            params.add(verbosity.toString());
        }

        //image utils
        if (imageMagickPath != null) {
            params.add(String.format("--%s", Params.IMAGEMAGICK_PATH));
            params.add(imageMagickPath);
        }
        if (jhovePath != null) {
            params.add(String.format("--%s", Params.JHOVE_PATH));
            params.add(jhovePath);
        }
        if (jpylyzerPath != null) {
            params.add(String.format("--%s", Params.JPYLYZER_PATH));
            params.add(jpylyzerPath);
        }
        if (kakaduPath != null) {
            params.add(String.format("--%s", Params.KAKADU_PATH));
            params.add(kakaduPath);
        }
        if (disableImageMagick) {
            params.add(String.format("--%s", Params.DISABLE_IMAGEMAGICK));
        }
        if (disableJhove) {
            params.add(String.format("--%s", Params.DISABLE_JHOVE));
        }
        if (disableJpylyzer) {
            params.add(String.format("--%s", Params.DISABLE_JPYLYZER));
        }
        if (disableKakadu) {
            params.add(String.format("--%s", Params.DISABLE_KAKADU));
        }
        if (disableMp3val) {
            params.add(String.format("--%s", Params.DISABLE_MP3VAL));
        }
        if (disableShntool) {
            params.add(String.format("--%s", Params.DISABLE_SHNTOOL));
        }
        if (disableCheckmate) {
            params.add(String.format("--%s", Params.DISABLE_CHECKMATE));
        }
        Object[] array = params.toArray();
        return Arrays.copyOf(array, array.length, String[].class);
    }
}
