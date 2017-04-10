package npk.pspValidator.cli;

import nkp.pspValidator.shared.*;
import nkp.pspValidator.shared.bak.XsdValidator;
import nkp.pspValidator.shared.engine.Utils;
import nkp.pspValidator.shared.engine.exceptions.InvalidXPathExpressionException;
import nkp.pspValidator.shared.engine.exceptions.PspDataException;
import nkp.pspValidator.shared.engine.exceptions.ValidatorConfigurationException;
import nkp.pspValidator.shared.engine.exceptions.XmlFileParsingException;
import nkp.pspValidator.shared.imageUtils.CliCommand;
import nkp.pspValidator.shared.imageUtils.ImageUtil;
import nkp.pspValidator.shared.imageUtils.ImageUtilManager;
import nkp.pspValidator.shared.imageUtils.ImageUtilManagerFactory;
import org.apache.commons.cli.*;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

/**
 * Created by Martin Řehánek on 27.9.16.
 */
public class Main {

    public static int DEFAULT_VERBOSITY = 2;

    public static void main(String[] args) throws PspDataException, XmlFileParsingException, InvalidXPathExpressionException, FdmfRegistry.UnknownFdmfException, ValidatorConfigurationException, FdmfRegistry.UnknownFdmfException {
        main(null, args);
        //testXsds();
    }

    public static void main(Validator.DevParams devParams, String[] args) {
        /*if (true) {
            Properties properties = System.getProperties();
            properties.list(System.out);
            return;
        }*/

        //https://commons.apache.org/proper/commons-cli/usage.html
        Options options = new Options();
        options.addOption(OptionBuilder
                .withDescription("Akce, kterou má být provedena. Povolené hodnoty jsou VALIDATE_PSP a VALIDATE_PSP_GROUP.")
                .hasArg()
                .withArgName("AKCE")
                .withLongOpt(Params.ACTION)
                .create("a"));
        options.addOption(OptionBuilder
                //.withDescription("Adresář obsahující formalizované DMF pro jednotlivé verze standardu.")
                .withDescription("Adresar obsahujici konfiguraci Komplexniho validatoru vcetne formalizovanych verzi DMF pro jednotlive verze standardu.")
                .hasArg()
                //.withArgName("ADRESÁŘ_FDMF")
                .withArgName("ADRESAR_CONFIG")
                .withLongOpt(Params.CONFIG_DIR)
                .create());
        options.addOption(OptionBuilder
                .withDescription("Adresář, nebo soubor ZIP obsahující PSP balík. " +
                        "Parametr je povinný pro akci VALIDATE_PSP, u ostatních akcí je ignorován.")
                .hasArg()
                .withArgName("ADRESAR/SOUBOR_ZIP")
                .withLongOpt(Params.PSP)
                .create());
        options.addOption(OptionBuilder
                .withDescription("Adresář, nebo soubor ZIP obsahující skupinu PSP balíků. Ty opět mohou být ve formě adresářů nebo souborů ZIP. " +
                        "Parametr je povinný pro akci VALIDATE_PSP_GROUP, u ostatních akcí je ignorován.")
                .hasArg()
                .withArgName("ADRESAR/SOUBOR_ZIP")
                .withLongOpt(Params.PSP_GROUP)
                .create());
        options.addOption(OptionBuilder
                .withDescription("Adresář pro ukládání xml protokolů s informacemi o validaci každého PSP balíku. Název souboru protokolu je odvozen od názvu PSP balíku a datumu+času začátku validace. " +
                        "Pokud není parametr vyplněn, xml protokol se neukládá (kromě akce VALIDATE_PSP a vyplněného parametru xml-protocol-file).")
                .hasArg()
                .withArgName("ADRESAR")
                .withLongOpt(Params.XML_PROTOCOL_DIR)
                .create());
        options.addOption(OptionBuilder
                .withDescription("Soubor pro uložení xml protokolu s informacemi o validaci PSP balíku. Použije se jen pro akci VALIDATE_PSP. " +
                        "Pokud je zároveň vyplněn parametr xml-protocol-dir, použije se hodnota z xml-protocol-file. " +
                        "Pokud není parametr (a zároveň není vyplněn ani xml-protocol-dir), xml protokol se neukládá. " +
                        "Pokud soubor existuje, je nejprve smazán.")
                .hasArg()
                .withArgName("SOUBOR")
                .withLongOpt(Params.XML_PROTOCOL_FILE)
                .create());
        options.addOption(OptionBuilder
                .withDescription("Adresář pro ukládání dočasných souborů, konkrétně rozbaleních verzí ZIP souborů. " +
                        "Pokud parametr není vyplněn, bude výsledkem pokusu o validaci PSP balíku/skupiny balíků v ZIPu chyba.")
                .hasArg()
                .withArgName("ADRESAR")
                .withLongOpt(Params.TMP_DIR)
                .create("t"));
        options.addOption(OptionBuilder
                .withDescription("Počet nevalidních PSP balíku, po jehož dosažení validace předčasně skončí. " +
                        "Bude použito jen pro akci VALIDATE_PSP_GROUP. " +
                        "Pokud parametr není vyplněn, budou validováný všechny balíky.")
                .hasArg()
                .withArgName("N")
                .withLongOpt(Params.QUIT_AFTER_NTH_INVALID_PSP)
                .create());

        options.addOption(OptionBuilder
                .withDescription("Preferovaná verze DMF pro validaci monografií. " +
                        "Použije se k validaci, pokud je balík typu Monografie, data balíku neobsahují informaci o vhodné verzi DMF Monografie " +
                        "a parametr force-dmf-mon-version není vyplněn.")
                .hasArg()
                .withArgName("VERZE")
                .withLongOpt(Params.PREFER_DMF_MON_VERSION)
                .create());

        options.addOption(OptionBuilder
                .withDescription("Preferovaná verze DMF pro validaci periodik. " +
                        "Použije se k validaci, pokud je balík typu Periodikum, data balíku neobsahují informaci o vhodné verzi DMF Periodikum " +
                        "a parametr force-dmf-per-version není vyplněn.")
                .hasArg()
                .withArgName("VERZE")
                .withLongOpt(Params.PREFER_DMF_PER_VERSION)
                .create());
        options.addOption(OptionBuilder
                .withDescription("Vynucená verze DMF pro validaci monografií. " +
                        "Použije se k validaci všech balíků typu Monografie bez ohledu na data balíků a hodnotu parametru prefer-dmf-mon-version.")
                .hasArg()
                .withArgName("VERZE")
                .withLongOpt(Params.FORCE_DMF_MON_VERSION)
                .create());
        options.addOption(OptionBuilder
                .withDescription("Vynucená verze DMF pro validaci periodik. " +
                        "Použije se k validaci všech balíků typu Periodikum bez ohledu na data balíků a hodnotu parametru prefer-dmf-per-version.")
                .hasArg()
                .withArgName("VERZE")
                .withLongOpt(Params.FORCE_DMF_PER_VERSION)
                .create());

        options.addOption(OptionBuilder
                /*.withDescription("Úroveň podrobnosti výpisu." +
                        " 0 vypíše jen celkový počet chyb a rozhodnutí validní/nevalidní." +
                        " 3 vypíše vše včetně sekcí a pravidel bez chyb.")*/
                .withDescription("Uroven podrobnosti vypisu." +
                        " 0 vypise jen celkovy pocet chyb a rozhodnuti validni/nevalidni." +
                        " 3 vypise vse vcetně sekci a pravidel bez chyb.")
                .hasArg()
                .withArgName("0-3")
                .withLongOpt(Params.VERBOSITY)
                .create("v"));

        options.addOption(OptionBuilder
                /*.withDescription("Adresář s binárními soubory nástroje ImageMagick." +
                        " Např. C:\\Program Files\\ImageMagick-7.0.3-Q16 pro Windows, /usr/bin pro Linux.")*/
                .withDescription("Adresar typ s binarnimi soubory nastroje ImageMagick." +
                        " Napr. C:\\Program Files\\ImageMagick-7.0.3-Q16 pro Windows, /usr/bin pro Linux.")
                .hasArg()
                //.withArgName("ADRESÁŘ")
                .withArgName("ADRESAR")
                .withLongOpt(Params.IMAGEMAGICK_PATH)
                .create());

        options.addOption(OptionBuilder
                /*.withDescription("Adresář s binárními soubory nástroje JHOVE." +
                        " Např. C:\\Program Files\\jhove-1_11\\\\jhove pro Windows, /usr/bin pro Linux, TODO pro MacOS.")*/
                .withDescription("Adresar s binarnimi soubory nastroje JHOVE." +
                        " Např. C:\\Program Files\\jhove-1_11\\\\jhove pro Windows, /usr/bin pro Linux.")
                .hasArg()
                //.withArgName("ADRESÁŘ")
                .withArgName("ADRESAR")
                .withLongOpt(Params.JHOVE_PATH)
                .create());

        options.addOption(OptionBuilder
                /*.withDescription("Adresář typ s binárními soubory nástroje Jpylyzer." +
                        " Např. C:\\Program Files\\jpylyzer_1.17.0_win64 pro Windows, /usr/bin pro Linux, TODO pro MacOS.")*/
                .withDescription("Adresar typ s binarnimi soubory nastroje Jpylyzer." +
                        " Např. C:\\Program Files\\jpylyzer_1.17.0_win64 pro Windows, /usr/bin pro Linux.")
                .hasArg()
                //.withArgName("ADRESÁŘ")
                .withArgName("ADRESAR")
                .withLongOpt(Params.JPYLYZER_PATH)
                .create());

        options.addOption(OptionBuilder
                /*.withDescription("Adresář s binárními soubory nástroje Kakadu." +
                        " Např. C:\\Program Files\\Kakadu pro Windows, /usr/bin pro Linux, TODO pro MacOS.")*/
                .withDescription("Adresar s binarnimi soubory nastroje Kakadu." +
                        " Např. C:\\Program Files\\Kakadu pro Windows, /usr/bin pro Linux.")
                .hasArg()
                //.withArgName("ADRESÁŘ")
                .withArgName("ADRESAR")
                .withLongOpt(Params.KAKADU_PATH)
                .create());

        options.addOption(OptionBuilder
                //.withDescription("Zakáže použití ImageMagick.")
                .withDescription("Zakaze pouziti ImageMagick.")
                .withLongOpt(Params.DISABLE_IMAGEMAGICK)
                .create());

        options.addOption(OptionBuilder
                //.withDescription("Zakáže použití JHOVE.")
                .withDescription("Zakaze pouziti JHOVE.")
                .withLongOpt("disable-jhove")
                .create());

        options.addOption(OptionBuilder
                //.withDescription("Zakáže použití Jpylyzer.")
                .withDescription("Zakaze pouziti Jpylyzer.")
                .withLongOpt(Params.DISABLE_JPYLYZER)
                .create());

        options.addOption(OptionBuilder
                //.withDescription("Zakáže použití Kakadu.")
                .withDescription("Zakaze pouziti Kakadu.")
                .withLongOpt(Params.DISABLE_KAKADU)
                .create());

        options.addOption(OptionBuilder
                .withLongOpt(Params.HELP)
                //.withDescription("Zobrazit tuto nápovědu.")
                .withDescription("Zobrazit tuto napovedu.")
                .create());
        options.addOption(OptionBuilder
                .withLongOpt(Params.VERSION)
                .withDescription("Zobrazit informace o verzi.")
                .create());

        CommandLineParser parser = new DefaultParser();
        try {
            // System.out.println(toString(args));
            // parse the command line arguments
            CommandLine line = parser.parse(options, args);
            if (line.hasOption(Params.HELP)) {
                printHelp(options);
            } else if (line.hasOption(Params.VERSION)) {
                System.out.println(String.format("Komplexni validator CLI verze %s, sestaveno: %s", Version.VERSION_CODE, Version.BUILD_DATE));
            } else {

                //action
                if (!line.hasOption(Params.ACTION)) {
                    System.err.println(String.format("Chyba: prázdný parametr %s!", Params.ACTION));
                    printHelp(options);
                    return;
                }
                String actionStr = line.getOptionValue(Params.ACTION);
                Action action;
                try {
                    action = Action.valueOf(actionStr);
                } catch (java.lang.IllegalArgumentException e) {
                    System.err.println(String.format("Chyba: neznámá akce %s!", actionStr));
                    printHelp(options);
                    return;
                }

                //config dir
                if (!line.hasOption(Params.CONFIG_DIR)) {
                    System.err.println(String.format("Chyba: prázdný parametr --%s!", Params.CONFIG_DIR));
                    printHelp(options);
                    return;
                }
                File configDir = new File(line.getOptionValue(Params.CONFIG_DIR));

                //psp / psp-group
                File psp = null;
                File pspGroup = null;
                switch (action) {
                    case VALIDATE_PSP: {
                        if (!line.hasOption(Params.PSP)) {
                            System.err.println(String.format("Chyba: pro akci %s je parametr --%s povinný!", action, Params.PSP));
                            printHelp(options);
                            return;
                        } else {
                            psp = new File(line.getOptionValue(Params.PSP));
                            break;
                        }
                    }
                    case VALIDATE_PSP_GROUP: {
                        if (!line.hasOption(Params.PSP_GROUP)) {
                            System.err.println(String.format("Chyba: pro akci %s je parametr --%s povinný!", action, Params.PSP_GROUP));
                            printHelp(options);
                            return;
                        } else {
                            pspGroup = new File(line.getOptionValue(Params.PSP_GROUP));
                            break;
                        }
                    }
                }

                //xml protocol(x) file/dir
                File xmlProtocolDir = null;
                if (line.hasOption(Params.XML_PROTOCOL_DIR)) {
                    xmlProtocolDir = new File(line.getOptionValue(Params.XML_PROTOCOL_DIR));
                }
                File xmlProtocolFile = null;
                if (line.hasOption(Params.XML_PROTOCOL_FILE)) {
                    xmlProtocolFile = new File(line.getOptionValue(Params.XML_PROTOCOL_FILE));
                }

                //tmp dir
                File tmpDir = null;
                if (line.hasOption(Params.TMP_DIR)) {
                    tmpDir = new File(line.getOptionValue(Params.TMP_DIR));
                }

                //quitAfterInvalidPsps
                Integer quitAfterInvalidPsps = null;
                if (line.hasOption(Params.QUIT_AFTER_NTH_INVALID_PSP)) {
                    try {
                        quitAfterInvalidPsps = Integer.valueOf(line.getOptionValue(Params.QUIT_AFTER_NTH_INVALID_PSP));
                        if (quitAfterInvalidPsps < 1) {
                            System.err.println(String.format("Chyba: hodnota parametru --%s musí být celé kladné číslo!", Params.QUIT_AFTER_NTH_INVALID_PSP));
                            printHelp(options);
                            return;
                        }
                    } catch (NumberFormatException e) {
                        System.err.println(String.format("Chyba: hodnota parametru --%s musí být celé kladné číslo!", Params.QUIT_AFTER_NTH_INVALID_PSP));
                        printHelp(options);
                        return;
                    }
                }

                //prefered dmf versions
                String preferDmfMonVersion = null;
                if (line.hasOption(Params.PREFER_DMF_MON_VERSION)) {
                    preferDmfMonVersion = line.getOptionValue(Params.PREFER_DMF_MON_VERSION);
                }
                String preferDmfPerVersion = null;
                if (line.hasOption(Params.PREFER_DMF_PER_VERSION)) {
                    preferDmfPerVersion = line.getOptionValue(Params.PREFER_DMF_PER_VERSION);
                }

                //forced dmf versions
                String forceDmfMonVersion = null;
                if (line.hasOption(Params.FORCE_DMF_MON_VERSION)) {
                    forceDmfMonVersion = line.getOptionValue(Params.FORCE_DMF_MON_VERSION);
                }
                String forceDmfPerVersion = null;
                if (line.hasOption(Params.FORCE_DMF_PER_VERSION)) {
                    forceDmfPerVersion = line.getOptionValue(Params.FORCE_DMF_PER_VERSION);
                }

                //verbosity
                Integer verbosity = DEFAULT_VERBOSITY;
                if (line.hasOption(Params.VERBOSITY)) {
                    try {
                        verbosity = Integer.valueOf(line.getOptionValue(Params.VERBOSITY));
                        if (verbosity < 0 || verbosity > 3) {
                            System.err.println(String.format("Chyba: hodnota parametru --%s musí být číslo (0-3)!", Params.VERBOSITY));
                            printHelp(options);
                            return;
                        }
                    } catch (NumberFormatException e) {
                        System.err.println(String.format("Chyba: hodnota parametru --%s musí být číslo (0-3)!", Params.VERBOSITY));
                        printHelp(options);
                        return;
                    }
                }

                //image utils
                Map<ImageUtil, File> imageUtilPaths = new HashMap<>();
                Set<ImageUtil> imageUtilsDisabled = new HashSet<>();
                if (line.hasOption(Params.DISABLE_IMAGEMAGICK)) {
                    imageUtilsDisabled.add(ImageUtil.IMAGE_MAGICK);
                } else {
                    if (line.hasOption(Params.IMAGEMAGICK_PATH)) {
                        imageUtilPaths.put(ImageUtil.IMAGE_MAGICK, new File(line.getOptionValue(Params.IMAGEMAGICK_PATH)));
                    }
                }
                if (line.hasOption(Params.DISABLE_JHOVE)) {
                    imageUtilsDisabled.add(ImageUtil.JHOVE);
                } else {
                    if (line.hasOption(Params.JHOVE_PATH)) {
                        imageUtilPaths.put(ImageUtil.JHOVE, new File(line.getOptionValue(Params.JHOVE_PATH)));
                    }
                }
                if (line.hasOption(Params.DISABLE_JPYLYZER)) {
                    imageUtilsDisabled.add(ImageUtil.JPYLYZER);
                } else {
                    if (line.hasOption(Params.JPYLYZER_PATH)) {
                        imageUtilPaths.put(ImageUtil.JPYLYZER, new File(line.getOptionValue(Params.JPYLYZER_PATH)));
                    }
                }
                if (line.hasOption(Params.KAKADU_PATH)) {
                    imageUtilsDisabled.add(ImageUtil.KAKADU);
                } else {
                    if (line.hasOption(Params.DISABLE_KAKADU)) {
                        imageUtilPaths.put(ImageUtil.KAKADU, new File(line.getOptionValue(Params.KAKADU_PATH)));
                    }
                }

                switch (action) {
                    case VALIDATE_PSP:
                        validatePsp(psp,
                                configDir,
                                verbosity, xmlProtocolDir, xmlProtocolFile,
                                tmpDir,
                                preferDmfMonVersion, preferDmfPerVersion, forceDmfMonVersion, forceDmfPerVersion,
                                imageUtilPaths, imageUtilsDisabled,
                                devParams);
                        break;
                    case VALIDATE_PSP_GROUP:
                        validatePspGroup(pspGroup,
                                configDir,
                                verbosity, xmlProtocolDir,
                                tmpDir,
                                preferDmfMonVersion, preferDmfPerVersion, forceDmfMonVersion, forceDmfPerVersion,
                                imageUtilPaths, imageUtilsDisabled,
                                devParams);
                        break;
                }
            }
        } catch (ParseException exp) {
            System.err.println("Chyba parsování parametrů: " + exp.getMessage());
            printHelp(options);
        } catch (XmlFileParsingException e) {
            System.err.println("Chyba:" + e.getMessage());
        } catch (ValidatorConfigurationException e) {
            System.err.println("Chyba:" + e.getMessage());
        } catch (PspDataException e) {
            System.err.println("Chyba:" + e.getMessage());
        } catch (FdmfRegistry.UnknownFdmfException e) {
            System.err.println("Chyba:" + e.getMessage());
        } catch (InvalidXPathExpressionException e) {
            System.err.println("Chyba:" + e.getMessage());
        }
    }

    private static String toString(String[] args) {
        StringBuilder builder = new StringBuilder();
        for (String string : args) {
            builder.append(string).append(' ');
        }
        return builder.toString();
    }

    private static void printHelp(Options options) {
        /*String header = "Validuje PSP balík, nebo sadu PSP balíků podle DMF*." +
                " Verzi a typ DMF lze vynutit parametry --dmf-type a/nebo --dmf-version, případně jsou odvozeny z dat PSP balíku." +
                " Dále je potřeba pomocí --fdmf-dir uvést adresář, který obsahuje definice fDMF," +
                " typicky adresáře monograph_1.0, monograph_1.2, periodical_1.4 a periodical 1.6.\n\n";
        String footer = "\n*Definice metadatových formátů. Více na http://www.ndk.cz/standardy-digitalizace/metadata.\n" +
                "Více informací o validátoru najdete na https://github.com/NLCR/komplexni-validator.";*/
        String header = "Validuje PSP balik, nebo sadu PSP baliku podle DMF*." +
                " Verzi a typ DMF lze vynutit parametry --dmf-type a/nebo --dmf-version, pripadne jsou odvozeny z dat PSP baliku." +
                " Dale je potreba pomoci --config-dir uvest adresar, ktery obsahuje definice konfiguraci Komplexniho validatoru vcetne fDMF," +
                " typicky adresare monograph_1.0, monograph_1.2, periodical_1.4 a periodical 1.6.\n\n";
        String footer = "\n*Definice metadatovych formatu. Vice na http://www.ndk.cz/standardy-digitalizace/metadata.\n"
                + "Vice informaci o Komplexnim validatoru najdete na https://github.com/NLCR/komplexni-validator.";
//        + "Více informací o Validátoru najdete na https://github.com/NLCR/komplexni-validator.";

        HelpFormatter formatter = new HelpFormatter();
        formatter.setWidth(100);
        formatter.setOptionComparator(null);
        formatter.printHelp("java -jar KomplexniValidator.jar", header, options, footer, true);
    }

    private static void validatePspGroup(File pspGroup, File configDir, Integer verbosity, File xmlProtocolDir, File tmpDir, String preferDmfMonVersion, String preferDmfPerVersion, String forceDmfMonVersion, String forceDmfPerVersion, Map<ImageUtil, File> imageUtilPaths, Set<ImageUtil> imageUtilsDisabled, Validator.DevParams devParams) throws ValidatorConfigurationException, XmlFileParsingException, FdmfRegistry.UnknownFdmfException, PspDataException, InvalidXPathExpressionException {
        PrintStream out = System.out;
        PrintStream err = System.err;
        Platform platform = Platform.detectOs();
        out.println(String.format("Platforma: %s", platform.toReadableString()));

        //validator configuration
        out.println(String.format("Kořenový adresář konfigurace validátoru: %s", configDir.getAbsolutePath()));
        ValidatorConfigurationManager validatorConfigManager = new ValidatorConfigurationManager(configDir);
        ImageUtilManager imageUtilManager = new ImageUtilManagerFactory(validatorConfigManager.getImageUtilsConfigFile()).buildImageUtilManager(platform.getOperatingSystem());
        imageUtilManager.setPaths(imageUtilPaths);
        detectImageTools(out, imageUtilManager, imageUtilsDisabled);

        //TODO: to taky muze byt zip
        checkReadableDir(pspGroup);
        for (File pspDirOrZip : pspGroup.listFiles()) {
            //TODO: pocitat nevalidni baliky
            validatePsp(pspDirOrZip,
                    configDir,
                    verbosity, xmlProtocolDir, null,
                    tmpDir,
                    preferDmfMonVersion, preferDmfPerVersion, forceDmfMonVersion, forceDmfPerVersion,
                    imageUtilPaths, imageUtilsDisabled,
                    devParams);
        }
    }

    private static void validatePsp(File pspDirOrZipFile, File configDir,
                                    Integer verbosity, File xmlProtocolDir, File xmlProtocolFile,
                                    File tmpDir,
                                    String preferDmfMonVersion, String preferDmfPerVersion, String forceDmfMonVersion, String forceDmfPerVersion,
                                    Map<ImageUtil, File> imageUtilPaths, Set<ImageUtil> imageUtilsDisabled,
                                    Validator.DevParams devParams) throws ValidatorConfigurationException, FdmfRegistry.UnknownFdmfException, PspDataException, InvalidXPathExpressionException, XmlFileParsingException {
        PrintStream out = System.out;
        PrintStream err = System.err;
        Platform platform = Platform.detectOs();
        out.println(String.format("Platforma: %s", platform.toReadableString()));

        //validator configuration
        out.println(String.format("Kořenový adresář konfigurace validátoru: %s", configDir.getAbsolutePath()));
        ValidatorConfigurationManager validatorConfigManager = new ValidatorConfigurationManager(configDir);
        ImageUtilManager imageUtilManager = new ImageUtilManagerFactory(validatorConfigManager.getImageUtilsConfigFile()).buildImageUtilManager(platform.getOperatingSystem());
        imageUtilManager.setPaths(imageUtilPaths);
        detectImageTools(out, imageUtilManager, imageUtilsDisabled);

        //pspDirOrZipFile dir or zip file?
        if (!pspDirOrZipFile.exists()) {
            throw new IllegalStateException(String.format("Soubor %s neexistuje", pspDirOrZipFile.getAbsolutePath()));
        } else {
            if (pspDirOrZipFile.isDirectory()) {
                if (!pspDirOrZipFile.canRead()) {
                    throw new IllegalStateException(String.format("Nelze číst adresář %s", pspDirOrZipFile.getAbsolutePath()));
                }
                runValidatorOnPspDir(pspDirOrZipFile,
                        imageUtilManager, validatorConfigManager,
                        out, verbosity, xmlProtocolDir, xmlProtocolFile,
                        preferDmfMonVersion, preferDmfPerVersion, forceDmfMonVersion, forceDmfPerVersion,
                        devParams);
            } else {
                runValidatorOnZip(pspDirOrZipFile,
                        tmpDir,
                        imageUtilManager, validatorConfigManager,
                        out, err, verbosity, xmlProtocolDir, xmlProtocolFile,
                        preferDmfMonVersion, preferDmfPerVersion, forceDmfMonVersion, forceDmfPerVersion,
                        devParams);


            }
        }
    }

    private static void runValidatorOnZip(File zipFile,
                                          File tmpDir,
                                          ImageUtilManager imageUtilManager, ValidatorConfigurationManager validatorConfigManager,
                                          PrintStream out, PrintStream err, Integer verbosity, File xmlProtocolDir, File xmlProtocolFile,
                                          String preferDmfMonVersion, String preferDmfPerVersion, String forceDmfMonVersion, String forceDmfPerVersion,
                                          Validator.DevParams devParams) throws XmlFileParsingException, FdmfRegistry.UnknownFdmfException, PspDataException, ValidatorConfigurationException, InvalidXPathExpressionException {
        try {
            try {
                new ZipFile(zipFile);
            } catch (ZipException e) {
                out.println(String.format("Soubor %s není adresář ani soubor ZIP, ignoruji.", zipFile.getAbsolutePath()));
                return;
            }
            if (tmpDir == null) {
                err.println(String.format("Chyba: prázdný parametr --%s: adresář pro dočasné soubory je potřeba pro rozbalení ZIP souboru %s!", Params.TMP_DIR, zipFile.getAbsolutePath()));
            } else if (!tmpDir.exists()) {
                err.println(String.format("Chyba: adresář %s neexistuje!", zipFile.getAbsolutePath()));
            } else if (!tmpDir.isDirectory()) {
                err.println(String.format("Chyba: soubor %s není adresář!", zipFile.getAbsolutePath()));
            } else if (!tmpDir.canWrite()) {
                err.println(String.format("Chyba: nemůžu zapisovat do adresáře %s!", zipFile.getAbsolutePath()));
            } else {
                File containerDir = new File(tmpDir, zipFile.getName() + "_extracted");
                if (containerDir.exists()) {
                    out.println(String.format("Mažu adresář %s", containerDir.getAbsolutePath()));
                    Utils.deleteNonemptyDir(containerDir);
                }
                out.println(String.format("Rozbaluji %s do adresáře %s", zipFile.getAbsolutePath(), containerDir.getAbsolutePath()));
                Utils.unzip(zipFile, containerDir);
                File[] filesInContainer = containerDir.listFiles();
                if (filesInContainer.length == 1 && filesInContainer[0].isDirectory()) {
                    runValidatorOnPspDir(filesInContainer[0],
                            imageUtilManager, validatorConfigManager,
                            out, verbosity, xmlProtocolDir, xmlProtocolFile,
                            preferDmfMonVersion, preferDmfPerVersion, forceDmfMonVersion, forceDmfPerVersion,
                            devParams);
                } else {
                    runValidatorOnPspDir(containerDir,
                            imageUtilManager, validatorConfigManager,
                            out, verbosity, xmlProtocolDir, xmlProtocolFile,
                            preferDmfMonVersion, preferDmfPerVersion, forceDmfMonVersion, forceDmfPerVersion,
                            devParams);
                }
            }
        } catch (IOException e) {
            out.println(String.format("Chyba zpracování ZIP souboru %s: %s!", zipFile.getAbsolutePath(), e.getMessage()));
        }
    }

    private static void runValidatorOnPspDir(File pspRoot,
                                             ImageUtilManager imageUtilManager, ValidatorConfigurationManager validatorConfigManager,
                                             PrintStream out, Integer verbosity, File xmlProtocolDir, File xmlProtocolFile,
                                             String preferDmfMonVersion, String preferDmfPerVersion, String forceDmfMonVersion, String forceDmfPerVersion,
                                             Validator.DevParams devParams) throws ValidatorConfigurationException, FdmfRegistry.UnknownFdmfException, PspDataException, XmlFileParsingException, InvalidXPathExpressionException {
        //psp dir, dmf detection
        checkReadableDir(pspRoot);
        out.println(String.format("Zpracovávám PSP balík %s", pspRoot.getAbsolutePath()));
        Dmf dmfResolved = new DmfDetector().resolveDmf(pspRoot, preferDmfMonVersion, preferDmfPerVersion, forceDmfMonVersion, forceDmfPerVersion);
        out.println(String.format("Bude použita verze standardu %s", dmfResolved));

        //initializes j2k profiles according to selected fDMF
        FdmfConfiguration fdmfConfig = new FdmfRegistry(validatorConfigManager).getFdmfConfig(dmfResolved);
        fdmfConfig.initJ2kProfiles(imageUtilManager);

        //xml protocol file
        if (xmlProtocolFile == null) {
            if (xmlProtocolDir != null) {
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'_'HH:mm:ss.SSS");
                String filename = String.format("%s_%s.xml", pspRoot.getName(), simpleDateFormat.format(new Date(System.currentTimeMillis())));
                xmlProtocolFile = new File(xmlProtocolDir, filename);
            }
        }

        //validate
        Validator validator = ValidatorFactory.buildValidator(fdmfConfig, pspRoot, validatorConfigManager.getDictionaryManager());
        out.println(String.format("Validátor inicializován, spouštím validace"));
        ValidationState.ProgressListener progressListener = null;
        switch (verbosity) {
            case 3:
                //vsechno, vcetne sekci a pravidel bez chyb
                validator.run(xmlProtocolFile, out, true, true, true, true, devParams, progressListener);
                break;
            case 2:
                //jen chybove sekce a v popisy jednotlivych chyb (default)
                validator.run(xmlProtocolFile, out, true, false, true, false, devParams, progressListener);
                break;
            case 1:
                //jen pocty chyb v chybovych sekcich, bez popisu jednotlivych chyb
                validator.run(xmlProtocolFile, out, true, false, false, false, devParams, progressListener);
                break;
            case 0:
                //jen valid/not valid
                validator.run(xmlProtocolFile, out, false, false, false, false, devParams, progressListener);
                break;
            default:
                throw new IllegalStateException(String.format("Nepovolená hodnota verbosity: %d. Hodnota musí být v intervalu [0-3]", verbosity));
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

    private static void checkReadableDir(File file) {
        if (!file.exists()) {
            throw new IllegalStateException(String.format("Soubor %s neexistuje", file.getAbsolutePath()));
        } else if (!file.isDirectory()) {
            throw new IllegalStateException(String.format("Soubor %s není adresář", file.getAbsolutePath()));
        } else if (!file.canRead()) {
            throw new IllegalStateException(String.format("Nelze číst adresář %s", file.getAbsolutePath()));
        }
    }

    private static void testXsds() {
        //info - ok
        File infoXsd = new File("../sharedModule/src/main/resources/nkp/pspValidator/shared/validatorConfig/fDMF/monograph_1.2/xsd/info_mon1.2.xsd");
        XsdValidator.validate("INFO", infoXsd, new File("../sharedModule/src/main/resources/nkp/pspValidator/shared-tmp/examples/info.xml"));

        //mix - ok
        File mixXsd = new File("../sharedModule/src/main/resources/nkp/pspValidator/shared/validatorConfig/fDMF/monograph_1.2/xsd/mix_2.0.xsd");
        XsdValidator.validate("MIX", mixXsd, new File("../sharedModule/src/main/resources/nkp/pspValidator/shared-tmp/examples/mix.xml"));

        //premis - ok
        File premisXsd = new File("../sharedModule/src/main/resources/nkp/pspValidator/shared/validatorConfig/fDMF/monograph_1.2/xsd/premis_2.2.xsd");
        XsdValidator.validate("PREMIS-EVENT", premisXsd, new File("../sharedModule/src/main/resources/nkp/pspValidator/shared-tmp/examples/premis-event.xml"));
        XsdValidator.validate("PREMIS-OBJECT", premisXsd, new File("../sharedModule/src/main/resources/nkp/pspValidator/shared-tmp/examples/premis-object.xml"));
        XsdValidator.validate("PREMIS-AGENT", premisXsd, new File("../sharedModule/src/main/resources/nkp/pspValidator/shared-tmp/examples/premis-agent.xml"));

        //dc - ok
        File dcXsd = new File("../sharedModule/src/main/resources/nkp/pspValidator/shared/validatorConfig/fDMF/monograph_1.2/xsd/dc_1.1.xsd");
        XsdValidator.validate("DC", dcXsd, new File("../sharedModule/src/main/resources/nkp/pspValidator/shared-tmp/examples/dc.xml"));

        //mods - ok
        File modsXsd = new File("../sharedModule/src/main/resources/nkp/pspValidator/shared/validatorConfig/fDMF/monograph_1.2/xsd/mods_3.5.xsd");
        XsdValidator.validate("MODS", modsXsd, new File("../sharedModule/src/main/resources/nkp/pspValidator/shared-tmp/examples/mods.xml"));

        //mets - problem https://github.com/NLCR/komplexni-validator/issues/13
        File metsXsd = new File("../sharedModule/src/main/resources/nkp/pspValidator/shared/validatorConfig/fDMF/monograph_1.2/xsd/mets_1.9.1.xsd");
        XsdValidator.validate("METS-PRIMARY", metsXsd, new File("../sharedModule/src/main/resources/nkp/pspValidator/shared-tmp/examples/mets-primary.xml"));
        XsdValidator.validate("METS-SECONDARY", metsXsd, new File("../sharedModule/src/main/resources/nkp/pspValidator/shared-tmp/examples/mets-secondary.xml"));
        //XsdValidator.validate("METS", metsXsd, new File("../sharedModule/src/test/resources/monograph_1.2/b50eb6b0-f0a4-11e3-b72e-005056827e52/amdsec/amd_mets_b50eb6b0-f0a4-11e3-b72e-005056827e52_0001.xml"));

    }

}
