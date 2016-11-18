package nkp.pspValidator.cli;

import nkp.pspValidator.shared.Dmf;
import nkp.pspValidator.shared.FdmfRegistry;
import nkp.pspValidator.shared.engine.exceptions.InvalidXPathExpressionException;
import nkp.pspValidator.shared.engine.exceptions.PspDataException;
import nkp.pspValidator.shared.engine.exceptions.ValidatorConfigurationException;
import nkp.pspValidator.shared.engine.exceptions.XmlParsingException;
import npk.pspValidator.cli.Main;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by martin on 10.11.16.
 */
public class MainTest {

    private static final String MON_1_2 = "/home/martin/ssd/IdeaProjects/PspValidator/sharedModule/src/test/resources/monograph_1.2/b50eb6b0-f0a4-11e3-b72e-005056827e52";
    private static final String MON_1_2_MAP = "/home/martin/ssd/IdeaProjects/PspValidator/sharedModule/src/test/resources/monografie_1.2_map/6e9a7000-65c0-11e6-85af-005056827e52";
    private static final String PER_1_6 = "/home/martin/ssd/IdeaProjects/PspValidator/sharedModule/src/test/resources/periodical_1.6/7033d800-0935-11e4-beed-5ef3fc9ae867";


    @org.junit.Test
    public void cli() throws InvalidXPathExpressionException, FdmfRegistry.UnknownFdmfException, PspDataException, ValidatorConfigurationException, XmlParsingException {

        Main.main(buildParams(
                "/home/martin/ssd/IdeaProjects/PspValidator/sharedModule/src/main/resources/nkp/pspValidator/shared/fDMF"
                , MON_1_2
                //, MON_1_2_MAP
                //,PER_1_6
                , Dmf.Type.MONOGRAPH
                , "1.2"
                , null //verbosity
                , null// "/home/martin/ssd/IdeaProjects/PspValidator/cliModule/src/test/resources/protocol.xml" //xml protocol
                , "/usr/bin" //jpylyzer path
                , "/usr/bin" //jhove path
                , null //imageMagick path
                , "/home/martin/zakazky/PSP-validator/utility/kakadu/KDU78_Demo_Apps_for_Linux-x86-64_160226" //kakadu path
        ));
    }

    private String[] buildParams(String fdmfsDir, String pspDir, Dmf.Type dmfType, String dmfVersion, Integer verbosity, String xmlProtocolFile,
                                 String jpylyzerPath, String jhovePath, String imageMagickPath, String kakaduPath
    ) {
        List<String> params = new ArrayList<>();
        params.add("-fd");
        params.add(fdmfsDir);

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

        if (jpylyzerPath != null) {
            params.add("--jpylyzer-path");
            params.add(jpylyzerPath);
        }

        if (jhovePath != null) {
            params.add("--jhove-path");
            params.add(jhovePath);
        }

        if (imageMagickPath != null) {
            params.add("--imageMagick-path");
            params.add(imageMagickPath);
        }

        if (kakaduPath != null) {
            params.add("--kakadu-path");
            params.add(kakaduPath);
        }

        Object[] array = params.toArray();
        return Arrays.copyOf(array, array.length, String[].class);
    }
}
