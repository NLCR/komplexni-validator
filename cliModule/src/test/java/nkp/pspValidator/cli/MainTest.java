package nkp.pspValidator.cli;

import nkp.pspValidator.shared.Dmf;
import nkp.pspValidator.shared.FdmfRegistry;
import nkp.pspValidator.shared.Platform;
import nkp.pspValidator.shared.Validator;
import nkp.pspValidator.shared.engine.exceptions.InvalidXPathExpressionException;
import nkp.pspValidator.shared.engine.exceptions.PspDataException;
import nkp.pspValidator.shared.engine.exceptions.ValidatorConfigurationException;
import nkp.pspValidator.shared.engine.exceptions.XmlFileParsingException;
import npk.pspValidator.cli.Action;
import npk.pspValidator.cli.Main;
import npk.pspValidator.cli.Params;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by Martin Řehánek on 10.11.16.
 */
public class MainTest {


    private static final String MON_1_2 = "../sharedModule/src/test/resources/monograph_1.2/b50eb6b0-f0a4-11e3-b72e-005056827e52";
    private static final String MON_1_2_MAP = "../sharedModule/src/test/resources/monograph_1.2_map/6e9a7000-65c0-11e6-85af-005056827e52";
    private static final String MON_1_2_INVALID_IMAGES = "../sharedModule/src/test/resources/monograph_1.2-invalid_images/b50eb6b0-f0a4-11e3-b72e-005056827e52";

    private static final String PER_1_6 = "../sharedModule/src/test/resources/periodical_1.6/7033d800-0935-11e4-beed-5ef3fc9ae867";

    private static final String ZIP_1 = "/home/martin/zakazky/NKP-Komplexni_Validator/data/zip/aba008-000310.zip";
    private static final String ZIP_2 = "/home/martin/zakazky/NKP-Komplexni_Validator/data/zip/ope301-00000v.zip";
    private static final String ZIP_NOT_ZIP = "/home/martin/zakazky/NKP-Komplexni_Validator/data/zip/not_a_zip.txt";

    //private static final String PER_1_6 = "../sharedModule/src/test/resources/periodical_1.6/ope301-00000v";
    //private static final String PER_1_6_INFO_INVALID_NS = "/home/martin/zakazky/NKP-Komplexni_Validator/data/per_1.6_invalid_info_ns/aba008-000310";

    private static final String PER_1_4 = "../sharedModule/src/test/resources/periodical_1.4/ope301-00000v";


    @org.junit.Test
    public void cli() throws InvalidXPathExpressionException, PspDataException, ValidatorConfigurationException, XmlFileParsingException, FdmfRegistry.UnknownFdmfException {
        Platform platform = Platform.detectOs();
        String configDir = null;
        String imageMagickPath = null;
        String jhovePath = null;
        String jpylyzerPath = null;
        String kakaduPath = null;

        switch (platform.getOperatingSystem()) {
            case WINDOWS:
                configDir = "..\\sharedModule\\src\\main\\resources\\nkp\\pspValidator\\shared\\validatorConfig";
                imageMagickPath = "C:\\Program Files\\ImageMagick-7.0.3-Q16";
                jhovePath = "C:\\Users\\Lenovo\\Documents\\software\\jhove";
                jpylyzerPath = "C:\\Users\\Lenovo\\Documents\\software\\jpylyzer_1.17.0_win64";
                kakaduPath = "C:\\Program Files (x86)\\Kakadu\\";
                break;
            case LINUX:
                configDir = "../sharedModule/src/main/resources/nkp/pspValidator/shared/validatorConfig";
                kakaduPath = "/home/martin/zakazky/NKP-Komplexni_Validator/utility/kakadu/KDU78_Demo_Apps_for_Linux-x86-64_160226";
                break;
            case MAC:
                configDir = "../sharedModule/src/main/resources/nkp/pspValidator/shared/validatorConfig";
                jhovePath = "/Users/martinrehanek/Software/jhove";
                imageMagickPath = "/opt/local/bin";
                jpylyzerPath = "/Users/martinrehanek/Software/jpylyzer-1.17.0/jpylyzer";
        }

        Validator.DevParams devParams = new Validator.DevParams();

        //devParams.getSectionsToRun().add("Soubor CHECKSUM");
        //devParams.getSectionsToRun().add("Soubor INFO");
        //devParams.getSectionsToRun().add("Struktura souborů");
        //devParams.getSectionsToRun().add("Bibliografická metadata");
        //devParams.getSectionsToRun().add("Identifikátory");
        //devParams.getSectionsToRun().add("JPEG 2000");
        //devParams.getSectionsToRun().add("Bibliografická metadata");
        //devParams.getSectionsToRun().add("ALTO");
        //devParams.getSectionsToRun().add("Technická metadata");
        //devParams.getSectionsToRun().add("Autorskoprávní metadata");
        //devParams.getSectionsToRun().add("Secondary METS filesec");
        devParams.getSectionsToRun().add("Strukturální mapy");

        Main.main(devParams, buildParams(
                configDir
                //, MON_1_2
                //, MON_1_2_MAP
                //, MON_1_2_INVALID_IMAGES
                //, PER_1_4
                //, PER_1_6
                //, PER_1_6_INFO_INVALID_NS
                , ZIP_2
                //, ZIP_NOT_ZIP

                , "/tmp"
                , null
                //,"1.0"
                //,"1.2"
                , null
                //, "1.4"
                //, "1.6"
                , null
                //,"1.0"
                //,"1.2"
                , null
                //, "1.4"
                //, "1.6"
                , 2 //verbosity
                , "/tmp"
                , null//"src/test/resources/protocol.xml" //xml protocol
                , imageMagickPath //null //imageMagick path
                , jhovePath //jhove path
                , jpylyzerPath //jpylyzer path
                , kakaduPath  //kakadu path
                , true //disable imageMagick
                , true //disable jhove
                , true //disable jpylyzer
                , true//disable kakadu
        ));
    }

    private String[] buildParams(String configDir, String pspDir, String tmpDir,
                                 String preferDmfModVersion, String preferDmfPerVersion, String forceDmfModVersion, String forceDmfPerVersion,
                                 Integer verbosity, String xmlProtocolDir, String xmlProtocolFile,
                                 String imageMagickPath, String jhovePath, String jpylyzerPath, String kakaduPath,
                                 boolean disableImageMagick, boolean disableJhove, boolean disableJpylyzer, boolean disableKakadu
    ) {
        List<String> params = new ArrayList<>();
        //action
        params.add(String.format("--%s", Params.ACTION));
        params.add(Action.VALIDATE_PSP.toString());
        //config dir
        params.add(String.format("--%s", Params.CONFIG_DIR));
        params.add(configDir);
        //psp
        params.add(String.format("--%s", Params.PSP));
        params.add(pspDir);

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
        if (preferDmfModVersion != null) {
            params.add(String.format("--%s", Params.PREFER_DMF_MON_VERSION));
            params.add(preferDmfModVersion);
        }
        if (preferDmfPerVersion != null) {
            params.add(String.format("--%s", Params.PREFER_DMF_PER_VERSION));
            params.add(preferDmfPerVersion);
        }
        if (forceDmfModVersion != null) {
            params.add(String.format("--%s", Params.FORCE_DMF_MON_VERSION));
            params.add(forceDmfModVersion);
        }
        if (forceDmfPerVersion != null) {
            params.add(String.format("--%s", Params.FORCE_DMF_PER_VERSION));
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

        Object[] array = params.toArray();
        return Arrays.copyOf(array, array.length, String[].class);
    }
}
