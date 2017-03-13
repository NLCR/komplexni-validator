package nkp.pspValidator.cli;

import nkp.pspValidator.shared.Dmf;
import nkp.pspValidator.shared.FdmfRegistry;
import nkp.pspValidator.shared.Platform;
import nkp.pspValidator.shared.Validator;
import nkp.pspValidator.shared.engine.exceptions.InvalidXPathExpressionException;
import nkp.pspValidator.shared.engine.exceptions.PspDataException;
import nkp.pspValidator.shared.engine.exceptions.ValidatorConfigurationException;
import nkp.pspValidator.shared.engine.exceptions.XmlFileParsingException;
import npk.pspValidator.cli.Main;

import java.io.File;
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

    //private static final String PER_1_6 = "../sharedModule/src/test/resources/periodical_1.6/ope301-00000v";
    //private static final String PER_1_6_INFO_INVALID_NS = "/home/martin/zakazky/NKP-PSP_validator/data/per_1.6_invalid_info_ns/aba008-000310";

    private static final String PER_1_4 = "../sharedModule/src/test/resources/periodical_1.4/ope301-00000v";


    @org.junit.Test
    public void cli() throws InvalidXPathExpressionException, PspDataException, ValidatorConfigurationException, XmlFileParsingException, FdmfRegistry.UnknownFdmfException {
        Platform platform = Platform.detectOs();
        String validadtorConfigPath = null;
        String imageMagickPath = null;
        String jhovePath = null;
        String jpylyzerPath = null;
        String kakaduPath = null;

        switch (platform.getOperatingSystem()) {
            case WINDOWS:
                validadtorConfigPath = "..\\sharedModule\\src\\main\\resources\\nkp\\pspValidator\\shared\\validatorConfig";
                imageMagickPath = "C:\\Program Files\\ImageMagick-7.0.3-Q16";
                jhovePath = "C:\\Users\\Lenovo\\Documents\\software\\jhove";
                jpylyzerPath = "C:\\Users\\Lenovo\\Documents\\software\\jpylyzer_1.17.0_win64";
                kakaduPath = "C:\\Program Files (x86)\\Kakadu\\";
                break;
            case LINUX:
                validadtorConfigPath = "../sharedModule/src/main/resources/nkp/pspValidator/shared/validatorConfig";
                kakaduPath = "/home/martin/zakazky/NKP-PSP_validator/utility/kakadu/KDU78_Demo_Apps_for_Linux-x86-64_160226";
                break;
            case MAC:
                validadtorConfigPath = "../sharedModule/src/main/resources/nkp/pspValidator/shared/validatorConfig";
                jhovePath = "/Users/martinrehanek/Software/jhove";
                imageMagickPath = "/opt/local/bin";
                jpylyzerPath = "/Users/martinrehanek/Software/jpylyzer-1.17.0/jpylyzer";
        }

        Validator.DevParams devParams = new Validator.DevParams();

        //devParams.getSectionsToRun().add("Soubor CHECKSUM");
        devParams.getSectionsToRun().add("Soubor INFO");
        //devParams.getSectionsToRun().add("Struktura souborů");
        //devParams.getSectionsToRun().add("Bibliografická metadata");
        //devParams.getSectionsToRun().add("Identifikátory");
        //devParams.getSectionsToRun().add("JPEG 2000");
        //devParams.getSectionsToRun().add("Bibliografická metadata");
        //devParams.getSectionsToRun().add("ALTO");
        //devParams.getSectionsToRun().add("Technická metadata");
        //devParams.getSectionsToRun().add("Autorskoprávní metadata");
        //devParams.getSectionsToRun().add("Secondary METS filesec");

        Main.main(devParams, buildParams(
                validadtorConfigPath
                //, MON_1_2_MAP
                //, PER_1_6_INFO_INVALID_NS
                //, PER_1_6
                , PER_1_4
                //, MON_1_2_INVALID_IMAGES
                //, MON_1_2_MAP
                // , PER_1_6
                , null//Dmf.Type.PERIODICAL//, Dmf.Type.MONOGRAPH
                , null//"1.4"//, "1.2"
                , 2 //verbosity
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

    private String[] buildParams(String configDir, String pspDir, Dmf.Type dmfType, String dmfVersion, Integer verbosity, String xmlProtocolFile,
                                 String imageMagickPath, String jhovePath, String jpylyzerPath, String kakaduPath,
                                 boolean disableImageMagick, boolean disableJhove, boolean disableJpylyzer, boolean disableKakadu
    ) {
        List<String> params = new ArrayList<>();
        params.add("-conf");
        params.add(configDir);

        params.add("-pd");
        params.add(pspDir);

        if (dmfType != null) {
            params.add("-dt");
            params.add(dmfType.toString());
        }

        if (dmfVersion != null) {
            params.add("-dv");
            params.add(dmfVersion);
        }

        if (verbosity != null) {
            params.add("-v");
            params.add(verbosity.toString());
        }

        if (xmlProtocolFile != null) {
            params.add("-x");
            params.add(xmlProtocolFile);
        }

        //image utils
        if (imageMagickPath != null) {
            params.add("--imageMagick-path");
            params.add(imageMagickPath);
        }
        if (jhovePath != null) {
            params.add("--jhove-path");
            params.add(jhovePath);
        }
        if (jpylyzerPath != null) {
            params.add("--jpylyzer-path");
            params.add(jpylyzerPath);
        }
        if (kakaduPath != null) {
            params.add("--kakadu-path");
            params.add(kakaduPath);
        }
        if (disableImageMagick) {
            params.add("--disable-imageMagick");
        }
        if (disableJhove) {
            params.add("--disable-jhove");
        }
        if (disableJpylyzer) {
            params.add("--disable-jpylyzer");
        }
        if (disableKakadu) {
            params.add("--disable-kakadu");
        }

        Object[] array = params.toArray();
        return Arrays.copyOf(array, array.length, String[].class);
    }
}
