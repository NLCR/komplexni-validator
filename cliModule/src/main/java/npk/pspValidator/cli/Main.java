package npk.pspValidator.cli;

import nkp.pspValidator.shared.*;
import nkp.pspValidator.shared.engine.exceptions.InvalidXPathExpressionException;
import nkp.pspValidator.shared.engine.exceptions.PspDataException;
import nkp.pspValidator.shared.engine.exceptions.ValidatorConfigurationException;
import nkp.pspValidator.shared.engine.exceptions.XmlFileParsingException;
import nkp.pspValidator.shared.imageUtils.CliCommand;
import nkp.pspValidator.shared.imageUtils.ImageUtil;
import nkp.pspValidator.shared.imageUtils.ImageUtilManager;
import nkp.pspValidator.shared.imageUtils.ImageUtilManagerFactory;
import nkp.pspValidator.shared.xsdValidation.XsdValidator;
import org.apache.commons.cli.*;

import java.io.File;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by Martin Řehánek on 27.9.16.
 */
public class Main {

    public static int DEFAULT_VERBOSITY = 2;

    public static void main(String[] args) throws PspDataException, XmlFileParsingException, InvalidXPathExpressionException, FdmfRegistry.UnknownFdmfException, ValidatorConfigurationException, FdmfRegistry.UnknownFdmfException {
        main(null, args);
    }

    public static void main(Validator.DevParams devParams, String[] args) throws PspDataException, XmlFileParsingException, InvalidXPathExpressionException, FdmfRegistry.UnknownFdmfException, ValidatorConfigurationException, FdmfRegistry.UnknownFdmfException {
        /*if (true) {
            Properties properties = System.getProperties();
            properties.list(System.out);
            return;
        }*/

        //https://commons.apache.org/proper/commons-cli/usage.html
        Options options = new Options();
        options.addOption(OptionBuilder
                .withDescription("Typ typu standardu pro validaci.")
                .hasArg()
                .withArgName("Monograph|Periodical")
                .withLongOpt("dmf-type")
                .create("dt"));
        options.addOption(OptionBuilder
                .withDescription("Verze standardu pro validaci.")
                .hasArg()
                .withArgName("VERZE_DMF")
                .withLongOpt("dmf-version")
                .create("dv"));

        options.addOption(OptionBuilder
                //.withDescription("Adresář obsahující formalizované DMF pro jednotlivé verze standardu.")
                .withDescription("Adresar obsahujici formalizovane DMF pro jednotlive verze standardu.")
                .hasArg()
                //.withArgName("ADRESÁŘ_FDMF")
                .withArgName("ADRESAR_FDMF")
                .withLongOpt("fdmf-dir")
                .create("fd"));
        options.addOption(OptionBuilder
                //.withDescription("Adresář PSP balíku, který má být validován.")
                .withDescription("Adresar PSP baliku, ktery ma byt validovan.")
                //.isRequired()
                .hasArg()
                //.withArgName("ADRESÁŘ_PSP")
                .withArgName("ADRESAR_PSP")
                .withLongOpt("psp-dir")
                .create("pd"));

        /*options.addOption(OptionBuilder
                .withDescription("soubor se zabaleným PSP balíkem")
                .hasArg()
                .withArgName("PSP.ZIP")
                .withLongOpt("psp-zip")
                .create("z"));*/

        options.addOption(OptionBuilder
                //.withDescription("Soubor, do kterého se zapíše strukturovaný protokol o průběhu validace v xml.")
                .withDescription("Soubor, do kterého se zapise strukturovany protokol o prubehu validace v xml.")
                .hasArg()
                .withArgName("SOUBOR")
                .withLongOpt("xml-output-file")
                .create("x"));

        options.addOption(OptionBuilder
                /*.withDescription("Úroveň podrobnosti výpisu." +
                        " 0 vypíše jen celkový počet chyb a rozhodnutí validní/nevalidní." +
                        " 3 vypíše vše včetně sekcí a pravidel bez chyb.")*/
                .withDescription("Uroven podrobnosti vypisu." +
                        " 0 vypise jen celkovy pocet chyb a rozhodnuti validni/nevalidni." +
                        " 3 vypise vse vcetně sekci a pravidel bez chyb.")
                .hasArg()
                .withArgName("0-3")
                .withLongOpt("verbosity")
                .create("v"));


        options.addOption(OptionBuilder
                /*.withDescription("Adresář s binárními soubory nástroje ImageMagick." +
                        " Např. C:\\Program Files\\ImageMagick-7.0.3-Q16 pro Windows, /usr/bin pro Linux.")*/
                .withDescription("Adresar typ s binarnimi soubory nastroje ImageMagick." +
                        " Napr. C:\\Program Files\\ImageMagick-7.0.3-Q16 pro Windows, /usr/bin pro Linux.")
                .hasArg()
                //.withArgName("ADRESÁŘ")
                .withArgName("ADRESAR")
                .withLongOpt("imageMagick-path")
                .create(null));

        options.addOption(OptionBuilder
                /*.withDescription("Adresář s binárními soubory nástroje JHOVE." +
                        " Např. C:\\Program Files\\jhove-1_11\\\\jhove pro Windows, /usr/bin pro Linux, TODO pro MacOS.")*/
                .withDescription("Adresar s binarnimi soubory nastroje JHOVE." +
                        " Např. C:\\Program Files\\jhove-1_11\\\\jhove pro Windows, /usr/bin pro Linux.")
                .hasArg()
                //.withArgName("ADRESÁŘ")
                .withArgName("ADRESAR")
                .withLongOpt("jhove-path")
                .create(null));

        options.addOption(OptionBuilder
                /*.withDescription("Adresář typ s binárními soubory nástroje Jpylyzer." +
                        " Např. C:\\Program Files\\jpylyzer_1.17.0_win64 pro Windows, /usr/bin pro Linux, TODO pro MacOS.")*/
                .withDescription("Adresar typ s binarnimi soubory nastroje Jpylyzer." +
                        " Např. C:\\Program Files\\jpylyzer_1.17.0_win64 pro Windows, /usr/bin pro Linux.")
                .hasArg()
                //.withArgName("ADRESÁŘ")
                .withArgName("ADRESAR")
                .withLongOpt("jpylyzer-path")
                .create(null));

        options.addOption(OptionBuilder
                /*.withDescription("Adresář s binárními soubory nástroje Kakadu." +
                        " Např. C:\\Program Files\\Kakadu pro Windows, /usr/bin pro Linux, TODO pro MacOS.")*/
                .withDescription("Adresar s binarnimi soubory nastroje Kakadu." +
                        " Např. C:\\Program Files\\Kakadu pro Windows, /usr/bin pro Linux.")
                .hasArg()
                //.withArgName("ADRESÁŘ")
                .withArgName("ADRESAR")
                .withLongOpt("kakadu-path")
                .create(null));


        options.addOption(OptionBuilder
                //.withDescription("Zakáže použití ImageMagick.")
                .withDescription("Zakaze pouziti ImageMagick.")
                .withLongOpt("disable-imageMagick")
                .create(null));

        options.addOption(OptionBuilder
                //.withDescription("Zakáže použití JHOVE.")
                .withDescription("Zakaze pouziti JHOVE.")
                .withLongOpt("disable-jhove")
                .create(null));

        options.addOption(OptionBuilder
                //.withDescription("Zakáže použití Jpylyzer.")
                .withDescription("Zakaze pouziti Jpylyzer.")
                .withLongOpt("disable-jpylyzer")
                .create(null));

        options.addOption(OptionBuilder
                //.withDescription("Zakáže použití Kakadu.")
                .withDescription("Zakaze pouziti Kakadu.")
                .withLongOpt("disable-kakadu")
                .create(null));

        options.addOption(OptionBuilder.withLongOpt("help")
                //.withDescription("Zobrazit tuto nápovědu.")
                .withDescription("Zobrazit tuto napovedu.")
                .create());
        options.addOption(OptionBuilder.withLongOpt("version")
                .withDescription("Zobrazit informace o verzi.")
                .create());

        CommandLineParser parser = new DefaultParser();
        try {
            // parse the command line arguments
            CommandLine line = parser.parse(options, args);
            if (line.hasOption("help")) {
                printHelp(options);
            } else if (line.hasOption("version")) {
                //TODO
                System.out.println(String.format("PSP Validator CLI verze %s, sestaveno: %s", Version.VERSION_CODE, Version.BUILD_DATE));
            } else {
                if (!line.hasOption("fd")) {
                    System.err.println("Pro spuštění validace je nutné určit adresář obsahující formalizované DMF.");
                    printHelp(options);
                    return;
                }
                if (!line.hasOption("pd")) {
                    System.err.println("Pro spuštění validace je nutné zadat PSP balík.");
                    printHelp(options);
                    return;
                }

                File fdmfsDir = new File(line.getOptionValue("fd"));
                File pspDir = new File(line.getOptionValue("pd"));
                Dmf.Type dmfType = line.hasOption("dt") ? Dmf.Type.valueOf(line.getOptionValue("dt").toUpperCase()) : null;
                String dmfVersion = line.hasOption("dv") ? line.getOptionValue("dv") : null;
                Integer verbosity = line.hasOption("v") ? Integer.valueOf(line.getOptionValue("v")) : DEFAULT_VERBOSITY;
                File xmlOutputFile = line.hasOption("x") ? new File(line.getOptionValue("x")) : null;

                //image utils
                Map<ImageUtil, File> imageUtilPaths = new HashMap<>();
                Set<ImageUtil> imageUtilsDisabled = new HashSet<>();
                if (line.hasOption("disable-imageMagick")) {
                    imageUtilsDisabled.add(ImageUtil.IMAGE_MAGICK);
                } else {
                    if (line.hasOption("imageMagick-path")) {
                        imageUtilPaths.put(ImageUtil.IMAGE_MAGICK, new File(line.getOptionValue("imageMagick-path")));
                    }
                }
                if (line.hasOption("disable-jhove")) {
                    imageUtilsDisabled.add(ImageUtil.JHOVE);
                } else {
                    if (line.hasOption("jhove-path")) {
                        imageUtilPaths.put(ImageUtil.JHOVE, new File(line.getOptionValue("jhove-path")));
                    }
                }
                if (line.hasOption("disable-jpylyzer")) {
                    imageUtilsDisabled.add(ImageUtil.JPYLYZER);
                } else {
                    if (line.hasOption("jpylyzer-path")) {
                        imageUtilPaths.put(ImageUtil.JPYLYZER, new File(line.getOptionValue("jpylyzer-path")));
                    }
                }
                if (line.hasOption("disable-kakadu")) {
                    imageUtilsDisabled.add(ImageUtil.KAKADU);
                } else {
                    if (line.hasOption("kakadu-path")) {
                        imageUtilPaths.put(ImageUtil.KAKADU, new File(line.getOptionValue("kakadu-path")));
                    } else {
                        System.err.println("no kakadu path");
                    }

                }

                runValidator(new Dmf(dmfType, dmfVersion), fdmfsDir, pspDir, xmlOutputFile, verbosity, imageUtilsDisabled, imageUtilPaths, null, devParams);
            }
        } catch (ParseException exp) {
            System.err.println("Chyba parsování parametrů: " + exp.getMessage());
            printHelp(options);
        }
    }

    private static void printHelp(Options options) {
        /*String header = "Validuje PSP balík, nebo sadu PSP balíků podle DMF*." +
                " Verzi a typ DMF lze vynutit parametry --dmf-type a/nebo --dmf-version, případně jsou odvozeny z dat PSP balíku." +
                " Dále je potřeba pomocí --fdmf-dir uvést adresář, který obsahuje definice fDMF," +
                " typicky adresáře monograph_1.0, monograph_1.2, periodical_1.4 a periodical 1.6.\n\n";
        String footer = "\n*Definice metadatových formátů. Více na http://www.ndk.cz/standardy-digitalizace/metadata.\n" +
                "Více informací o validátoru najdete na http://TODO ";*/
        String header = "Validuje PSP balik, nebo sadu PSP baliku podle DMF*." +
                " Verzi a typ DMF lze vynutit parametry --dmf-type a/nebo --dmf-version, pripadne jsou odvozeny z dat PSP baliku." +
                " Dale je potreba pomoci --fdmf-dir uvest adresář, který obsahuje definice fDMF," +
                " typicky adresare monograph_1.0, monograph_1.2, periodical_1.4 a periodical 1.6.\n\n";
        String footer = "\n*Definice metadatovych formatu. Vice na http://www.ndk.cz/standardy-digitalizace/metadata.\n"
                + "Vice informaci o Validatoru najdete na https://github.com/rzeh4n/psp-validator.";
//        + "Více informací o Validátoru najdete na https://github.com/rzeh4n/psp-validator.";

        HelpFormatter formatter = new HelpFormatter();
        formatter.setOptionComparator(null);
        formatter.printHelp("java -jar pspValidator", header, options, footer, true);
    }


    private static void runValidator(Dmf dmfPrefered, File fdmfsRoot, File pspRoot,
                                     File xmlOutputFile, int printVerbosity,
                                     Set<ImageUtil> imageUtilsDisabled, Map<ImageUtil, File> imageUtilPaths,
                                     Set<String> runOnlyTheseSections,
                                     Validator.DevParams devParams) throws PspDataException, InvalidXPathExpressionException, XmlFileParsingException, ValidatorConfigurationException, FdmfRegistry.UnknownFdmfException {

        PrintStream out = System.out;
        Platform platform = Platform.detectOs();
        out.println(String.format("Platforma: %s", platform.toReadableString()));
        out.println(String.format("Kořenový adresář fDMF: %s", fdmfsRoot.getAbsolutePath()));
        checkReadableDir(fdmfsRoot);
        checkReadableDir(pspRoot);
        out.println(String.format("Zpracovávám PSP balík %s", pspRoot.getAbsolutePath()));
        Dmf dmfResolved = new DmfDetector().resolveDmf(dmfPrefered, pspRoot);
        out.println(String.format("Bude použita verze standardu %s", dmfResolved));

        FdmfConfiguration fdmfConfig = new FdmfRegistry(fdmfsRoot).getFdmfConfig(dmfResolved);
        File imageUtilsConfigFile = getImageUtilsConfigFile(fdmfsRoot);
        ImageUtilManager imageUtilManager = new ImageUtilManagerFactory(imageUtilsConfigFile).buildImageUtilManager(platform.getOperatingSystem());
        imageUtilManager.setPaths(imageUtilPaths);
        detectImageTools(out, imageUtilManager, imageUtilsDisabled);
        fdmfConfig.initJ2kProfiles(imageUtilManager);

        Validator validator = ValidatorFactory.buildValidator(fdmfConfig, pspRoot);
        out.println(String.format("Validátor inicializován, spouštím validace"));
        /*System.out.println("ěščřžýáíéĚŠČŘŽÝÁÍÉ");
        System.out.println(String.format("ěščřžýáíéĚŠČŘŽÝÁÍÉ"));
        StringBuilder b = new StringBuilder();
        b.append("ěščřžýáíéĚŠČŘŽÝÁÍÉ");
        System.out.println(b.toString());*/

        ValidationState.ProgressListener progressListener = null;

        switch (printVerbosity) {
            case 3:
                //vsechno, vcetne sekci a pravidel bez chyb
                validator.run(xmlOutputFile, out, true, true, true, true, devParams, progressListener);
                break;
            case 2:
                //jen chybove sekce a v popisy jednotlivych chyb (default)
                validator.run(xmlOutputFile, out, true, false, true, false, devParams, progressListener);
                break;
            case 1:
                //jen pocty chyb v chybovych sekcich, bez popisu jednotlivych chyb
                validator.run(xmlOutputFile, out, true, false, false, false, devParams, progressListener);
                break;
            case 0:
                //jen valid/not valid
                validator.run(xmlOutputFile, out, false, false, false, false, devParams, progressListener);
                break;
            default:
                throw new IllegalStateException(String.format("Nepovolená hodnota verbosity: %d. Hodnota musí být v intervalu [0-3]", printVerbosity));
        }
    }

    private static File getImageUtilsConfigFile(File fdmfsRoot) throws ValidatorConfigurationException {
        File file = new File(fdmfsRoot, "imageUtils.xml");
        if (!file.exists()) {
            throw new ValidatorConfigurationException("chybí konfigurační soubor " + file.getAbsolutePath());
        } else if (!file.canRead()) {
            throw new ValidatorConfigurationException("nelze číst konfigurační soubor " + file.getAbsolutePath());
        } else {
            return file;
        }
    }

    private static void detectImageTools(PrintStream out, ImageUtilManager imageUtilManager, Set<ImageUtil> utilsDisabled) {
        for (ImageUtil util : ImageUtil.values()) {
            if (utilsDisabled.contains(util)) {
                out.println(String.format("Vypnuto použití nástroje %s.", util.getUserFriendlyName()));
            } else {
                out.print(String.format("Kontroluji dostupnost nástroje %s: ", util.getUserFriendlyName()));
                if (!imageUtilManager.isVersionDetectionDefined(util)) {
                    out.println("není definován způsob detekce verze");
                } else if (!imageUtilManager.isUtilExecutionDefined(util)) {
                    out.println("není definován způsob spuštění");
                } else {
                    try {
                        String version = imageUtilManager.runUtilVersionDetection(util);
                        if (version != null) {
                            imageUtilManager.setUtilAvailable(util, true);
                            out.println("nalezen, verze: " + version);
                        } else {
                            out.println("nenalezen");
                        }
                    } catch (CliCommand.CliCommandException e) {
                        //System.out.println(String.format("chyba běhu %s: %s ", util.getUserFriendlyName(), e.getMessage()));
                        out.println(String.format("nenalezen (%s)", e.getMessage()));
                    }
                }
            }
        }
    }

    private static void checkReadableDir(File pspRoot) {
        if (!pspRoot.exists()) {
            throw new IllegalStateException(String.format("Soubor %s neexistuje", pspRoot.getAbsolutePath()));
        } else if (!pspRoot.isDirectory()) {
            throw new IllegalStateException(String.format("Soubor %s není adresář", pspRoot.getAbsolutePath()));
        } else if (!pspRoot.canRead()) {
            throw new IllegalStateException(String.format("Nelze číst adresář %s", pspRoot.getAbsolutePath()));
        }
    }

    private static void testXsds() {
        //info - ok
        File infoXml = new File("../sharedModule/src/main/resources/nkp/pspValidator/shared/fDMF/monograph_1.2/xsd/examples/info.xml");

        File infoXsd = new File("../sharedModule/src/main/resources/nkp/pspValidator/shared/fDMF/monograph_1.2/xsd/info_1.1.xsd");
        XsdValidator.validate("INFO", infoXsd, infoXml);

       /* //mix - ok
        File mixXsd = new File("../sharedModule/src/main/resources/nkp/pspValidator/shared/fDMF/monograph_1.2/xsd/mix_2.0.xsd");
        File mixXml = new File("../sharedModule/src/main/resources/nkp/pspValidator/shared/fDMF/monograph_1.2/xsd/examples/mix.xml");
        XsdValidator.validate("MIX", mixXsd, mixXml);

        //premis - ok
        File premisXsd = new File("../sharedModule/src/main/resources/nkp/pspValidator/shared/fDMF/monograph_1.2/xsd/premis_2.2.xsd");
        File premisXml = new File("../sharedModule/src/main/resources/nkp/pspValidator/shared/fDMF/monograph_1.2/xsd/examples/premis.xml");
        XsdValidator.validate("PREMIS", premisXsd, premisXml);

        //dc - TODO, jak je to s tim root elementem a jeste import xml.xsd v xsd
        File dcXsd = new File("../sharedModule/src/main/resources/nkp/pspValidator/shared/fDMF/monograph_1.2/xsd/dc_1.1.xsd");
        File dcXml = new File("../sharedModule/src/main/resources/nkp/pspValidator/shared/fDMF/monograph_1.2/xsd/examples/dc.xml");
        XsdValidator.validate("DC", dcXsd, dcXml);

        //mods - TODO: problem s importem xml.xsd
        File modsXsd = new File("../sharedModule/src/main/resources/nkp/pspValidator/shared/fDMF/monograph_1.2/xsd/mods_3.5.xsd");
        File modsXml = new File("../sharedModule/src/main/resources/nkp/pspValidator/shared/fDMF/monograph_1.2/xsd/examples/mods.xml");
        XsdValidator.validate("MODS", modsXsd, modsXml);

        //mets - ok
        File metsXsd = new File("../sharedModule/src/main/resources/nkp/pspValidator/shared/fDMF/monograph_1.2/xsd/mets_1.9.1.xsd");
        File metsXml = new File("../sharedModule/src/main/resources/nkp/pspValidator/shared/fDMF/monograph_1.2/xsd/examples/mets.xml");
        XsdValidator.validate("METS", metsXsd, metsXml);*/
    }

}
