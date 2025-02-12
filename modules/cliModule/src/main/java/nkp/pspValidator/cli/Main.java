package nkp.pspValidator.cli;

import nkp.pspValidator.shared.*;
import nkp.pspValidator.shared.engine.Utils;
import nkp.pspValidator.shared.engine.exceptions.InvalidXPathExpressionException;
import nkp.pspValidator.shared.engine.exceptions.PspDataException;
import nkp.pspValidator.shared.engine.exceptions.ValidatorConfigurationException;
import nkp.pspValidator.shared.engine.exceptions.XmlFileParsingException;
import nkp.pspValidator.shared.engine.types.MetadataFormat;
import nkp.pspValidator.shared.engine.validationFunctions.ValidationResult;
import nkp.pspValidator.shared.externalUtils.CliCommand;
import nkp.pspValidator.shared.externalUtils.ExternalUtil;
import nkp.pspValidator.shared.externalUtils.ExternalUtilManager;
import nkp.pspValidator.shared.externalUtils.ExternalUtilManagerFactory;
import nkp.pspValidator.shared.metadataProfile.DictionaryManager;
import nkp.pspValidator.shared.metadataProfile.MetadataProfile;
import nkp.pspValidator.shared.metadataProfile.MetadataProfileParser;
import nkp.pspValidator.shared.metadataProfile.MetadataProfileValidator;
import org.apache.commons.cli.*;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
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
        //XsdValidator.testXsds();
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
                .withDescription(replaceUmlaut("Akce, která má být provedena. Povolené hodnoty jsou VALIDATE_PSP, VALIDATE_PSP_GROUP, BUILD_MINIFIED_PACKAGE a VALIDATE_METADATA_BY_PROFILE."))
                .hasArg()
                .withArgName("AKCE")
                .withLongOpt(Params.ACTION)
                .create("a"));
        options.addOption(OptionBuilder
                .withDescription(replaceUmlaut("Adresář obsahující formalizované DMF pro jednotlivé verze standardu. " +
                        "Parametr je povinný pro akce VALIDATE_PSP a VALIDATE_PSP_GROUP, u ostatních akcí je ignorován."))
                .hasArg()
                //.withArgName("ADRESÁŘ_FDMF")
                .withArgName("ADRESAR_CONFIG")
                .withLongOpt(Params.CONFIG_DIR)
                .create());
        options.addOption(OptionBuilder
                .withDescription(replaceUmlaut(
                        "Adresář, nebo soubor ZIP obsahující PSP balík. " +
                                "Parametr je povinný pro akce VALIDATE_PSP a BUILD_MINIFIED_PACKAGE, u ostatních akcí je ignorován."))
                .hasArg()
                .withArgName("ADRESAR/SOUBOR_ZIP")
                .withLongOpt(Params.PSP)
                .create());
        options.addOption(OptionBuilder
                .withDescription(replaceUmlaut(
                        "Adresář, nebo soubor ZIP obsahující skupinu PSP balíků. Ty opět mohou být ve formě adresářů nebo souborů ZIP. " +
                                "Parametr je povinný pro akci VALIDATE_PSP_GROUP, u ostatních akcí je ignorován."))
                .hasArg()
                .withArgName("ADRESAR/SOUBOR_ZIP")
                .withLongOpt(Params.PSP_GROUP)
                .create());
        options.addOption(OptionBuilder
                .withDescription(replaceUmlaut(
                        "Adresář, kam bude bude uložen minifikovaný PSP balík. " +
                                "Parametr je povinný pro akci BUILD_MINIFIED_PACKAGE, u ostatních akcí je ignorován."))
                .hasArg()
                .withArgName("ADRESAR_PRO_MINIFIKOVANY_PSP")
                .withLongOpt(Params.MINIFIED_PSP_DIR)
                .create());
        options.addOption(OptionBuilder
                .withDescription(replaceUmlaut(
                        "Adresář pro ukládání xml protokolů s informacemi o validaci každého PSP balíku. Název souboru protokolu je odvozen od názvu PSP balíku a datumu+času začátku validace. " +
                                "Pokud není parametr vyplněn, xml protokol se neukládá (kromě akce VALIDATE_PSP a vyplněného parametru --xml-protocol-file)."))
                .hasArg()
                .withArgName("ADRESAR")
                .withLongOpt(Params.XML_PROTOCOL_DIR)
                .create());
        options.addOption(OptionBuilder
                .withDescription(replaceUmlaut(
                        "Soubor pro uložení xml protokolu s informacemi o validaci PSP balíku. Použije se jen pro akci VALIDATE_PSP. " +
                                "Pokud je zároveň vyplněn parametr --xml-protocol-dir, použije se hodnota z --xml-protocol-file. " +
                                "Pokud není parametr (a zároveň není vyplněn ani --xml-protocol-dir), xml protokol se neukládá. " +
                                "Pokud soubor existuje, je nejprve smazán."))
                .hasArg()
                .withArgName("SOUBOR")
                .withLongOpt(Params.XML_PROTOCOL_FILE)
                .create());
        options.addOption(OptionBuilder
                .withDescription(replaceUmlaut(
                        "Adresář pro ukládání dočasných souborů, konkrétně rozbaleních verzí ZIP souborů. " +
                                "Pokud parametr není vyplněn, bude výsledkem pokusu o validaci PSP balíku/skupiny balíků v ZIPu chyba."))
                .hasArg()
                .withArgName("ADRESAR")
                .withLongOpt(Params.TMP_DIR)
                .create("t"));
        options.addOption(OptionBuilder
                .withDescription(replaceUmlaut(
                        "Identifikátor metadatového profilu. Např.: 'monograph_2.1:biblio:dc:volume_singlevolume_aacr2' nebo 'periodical_2.0:tech:premis_agent'." +
                                "Určeno pro akci VALIDATE_METADATA_BY_PROFILE."))
                .hasArg()
                .withArgName("PROFILE_ID")
                .withLongOpt(Params.METADATA_PROFILE_ID)
                .create());
        options.addOption(OptionBuilder
                .withDescription(replaceUmlaut(
                        "Metadatový soubor pro validaci podle metadatového profilu." +
                                "Určeno pro akci VALIDATE_METADATA_BY_PROFILE."))
                .hasArg()
                .withArgName("SOUBOR_S_METADATY")
                .withLongOpt(Params.METADATA_FILE)
                .create());
        /*TODO: implement*/
       /* options.addOption(OptionBuilder
                .withDescription(replaceUmlaut(
                        "Počet nevalidních PSP balíku, po jehož dosažení validace předčasně skončí. " +
                                "Bude použito jen pro akci VALIDATE_PSP_GROUP. " +
                                "Pokud parametr není vyplněn, budou validováný všechny balíky."))
                .hasArg()
                .withArgName("N")
                .withLongOpt(Params.QUIT_AFTER_NTH_INVALID_PSP)
                .create());*/

        options.addOption(OptionBuilder
                .withDescription(replaceUmlaut(
                        "Preferovaná verze DMF pro validaci monografií. " +
                                "Použije se k validaci, pokud je balík typu Monografie, data balíku neobsahují informaci o vhodné verzi DMF Monografie " +
                                "a parametr --" + Params.FORCED_DMF_MON_VERSION + " není vyplněn."))
                .hasArg()
                .withArgName("VERZE")
                .withLongOpt(Params.PREFERRED_DMF_MON_VERSION)
                .create());

        options.addOption(OptionBuilder
                .withDescription(replaceUmlaut(
                        "Preferovaná verze DMF pro validaci periodik. " +
                                "Použije se k validaci, pokud je balík typu Periodikum, data balíku neobsahují informaci o vhodné verzi DMF Periodikum " +
                                "a parametr --" + Params.FORCED_DMF_PER_VERSION + " není vyplněn."))
                .hasArg()
                .withArgName("VERZE")
                .withLongOpt(Params.PREFERRED_DMF_PER_VERSION)
                .create());
        options.addOption(OptionBuilder
                .withDescription(replaceUmlaut(
                        "Preferovaná verze DMF pro validaci zvukových dokumentů - gramodesek. " +
                                "Použije se k validaci, pokud je balík typu Zvukový dokument (gramodeska), data balíku neobsahují informaci o vhodné verzi DMF Zvukové dokumenty " +
                                "a parametr --" + Params.FORCED_DMF_ADG_VERSION + " není vyplněn."))
                .hasArg()
                .withArgName("VERZE")
                .withLongOpt(Params.PREFERRED_DMF_ADG_VERSION)
                .create());
        options.addOption(OptionBuilder
                .withDescription(replaceUmlaut(
                        "Preferovaná verze DMF pro validaci zvukových dokumentů - fonoválečků. " +
                                "Použije se k validaci, pokud je balík typu Zvukový dokument (fonováleček), data balíku neobsahují informaci o vhodné verzi DMF Zvukové dokumenty " +
                                "a parametr --" + Params.FORCED_DMF_ADF_VERSION + " není vyplněn."))
                .hasArg()
                .withArgName("VERZE")
                .withLongOpt(Params.PREFERRED_DMF_ADF_VERSION)
                .create());
        options.addOption(OptionBuilder
                .withDescription(replaceUmlaut(
                        "Vynucená verze DMF pro validaci monografií. " +
                                //"Použije se k validaci všech balíků typu Monografie bez ohledu na data balíků a hodnotu parametru --preferred-dmf-mon-version."))
                                "Použije se k validaci všech balíků typu Monografie bez ohledu na data balíků a hodnotu parametru --" + Params.PREFERRED_DMF_MON_VERSION + "."))
                .hasArg()
                .withArgName("VERZE")
                .withLongOpt(Params.FORCED_DMF_MON_VERSION)
                .create());
        options.addOption(OptionBuilder
                .withDescription(replaceUmlaut(
                        "Vynucená verze DMF pro validaci periodik. " +
                                "Použije se k validaci všech balíků typu Periodikum bez ohledu na data balíků a hodnotu parametru --" + Params.PREFERRED_DMF_PER_VERSION + "."))
                .hasArg()
                .withArgName("VERZE")
                .withLongOpt(Params.FORCED_DMF_PER_VERSION)
                .create());
        options.addOption(OptionBuilder
                .withDescription(replaceUmlaut(
                        "Vynucená verze DMF pro validaci zvukových dokumentů - gramodesek. " +
                                "Použije se k validaci všech balíků typu Zvukový dokument (gramodeska) bez ohledu na data balíků a hodnotu parametru --" + Params.PREFERRED_DMF_ADG_VERSION + "."))
                .hasArg()
                .withArgName("VERZE")
                .withLongOpt(Params.FORCED_DMF_ADG_VERSION)
                .create());
        options.addOption(OptionBuilder
                .withDescription(replaceUmlaut(
                        "Vynucená verze DMF pro validaci zvukových dokumentů - fonoválečků. " +
                                "Použije se k validaci všech balíků typu Zvukový dokument (fonováleček) bez ohledu na data balíků a hodnotu parametru --" + Params.PREFERRED_DMF_ADF_VERSION + "."))
                .hasArg()
                .withArgName("VERZE")
                .withLongOpt(Params.FORCED_DMF_ADF_VERSION)
                .create());

        options.addOption(OptionBuilder
                .withDescription(replaceUmlaut(
                        "Úroveň podrobnosti výpisu." +
                                " 0 vypíše jen celkový počet chyb a rozhodnutí validní/nevalidní." +
                                " 3 vypíše vše včetně sekcí a pravidel bez chyb."))
                .hasArg()
                .withArgName("0-3")
                .withLongOpt(Params.VERBOSITY)
                .create("v"));

        options.addOption(OptionBuilder
                .withDescription(replaceUmlaut(
                        "Adresář s binárními soubory nástroje ImageMagick. " +
                                "Např. C:\\Program Files\\ImageMagick-7.0.3-Q16 pro Windows, /usr/bin pro Linux."))
                .hasArg()
                .withArgName("ADRESAR")
                .withLongOpt(Params.IMAGEMAGICK_PATH)
                .create());

        options.addOption(OptionBuilder
                .withDescription(replaceUmlaut(
                        "Adresář s binárními soubory nástroje JHOVE." +
                                " Např. C:\\Program Files\\jhove-1_11\\\\jhove pro Windows, /usr/bin pro Linux."))
                .hasArg()
                .withArgName("ADRESAR")
                .withLongOpt(Params.JHOVE_PATH)
                .create());

        options.addOption(OptionBuilder
                .withDescription(replaceUmlaut("Adresář s binárními soubory nástroje Jpylyzer." +
                        " Např. C:\\Program Files\\jpylyzer_1.17.0_win64 pro Windows, /usr/bin pro Linux."))
                .hasArg()
                .withArgName("ADRESAR")
                .withLongOpt(Params.JPYLYZER_PATH)
                .create());

        options.addOption(OptionBuilder
                .withDescription(replaceUmlaut("Adresář s binárními soubory nástroje Kakadu." +
                        " Např. C:\\Program Files\\Kakadu pro Windows, /usr/bin pro Linux."))
                .hasArg()
                .withArgName("ADRESAR")
                .withLongOpt(Params.KAKADU_PATH)
                .create());

        options.addOption(OptionBuilder
                .withDescription(replaceUmlaut("Adresář s binárními soubory nástroje MP3val."))
                .hasArg()
                .withArgName("ADRESAR")
                .withLongOpt(Params.MP3VAL_PATH)
                .create());

        options.addOption(OptionBuilder
                .withDescription(replaceUmlaut("Adresář s binárními soubory nástroje shntool."))
                .hasArg()
                .withArgName("ADRESAR")
                .withLongOpt(Params.SHNTOOL_PATH)
                .create());

        options.addOption(OptionBuilder
                .withDescription(replaceUmlaut("Adresář s binárními soubory nástroje Checkmate."))
                .hasArg()
                .withArgName("ADRESAR")
                .withLongOpt(Params.CHECKMATE_PATH)
                .create());

        options.addOption(OptionBuilder
                .withDescription(replaceUmlaut("Zakáže použití ImageMagick."))
                .withLongOpt(Params.DISABLE_IMAGEMAGICK)
                .create());

        options.addOption(OptionBuilder
                .withDescription(replaceUmlaut("Zakáže použití JHOVE."))
                .withLongOpt("disable-jhove")
                .create());

        options.addOption(OptionBuilder
                .withDescription(replaceUmlaut("Zakáže použití Jpylyzer."))
                .withLongOpt(Params.DISABLE_JPYLYZER)
                .create());

        options.addOption(OptionBuilder
                .withDescription(replaceUmlaut("Zakáže použití Kakadu."))
                .withLongOpt(Params.DISABLE_KAKADU)
                .create());

        options.addOption(OptionBuilder
                .withDescription(replaceUmlaut("Zakáže použití MP3val."))
                .withLongOpt(Params.DISABLE_MP3VAL)
                .create());

        options.addOption(OptionBuilder
                .withDescription(replaceUmlaut("Zakáže použití shntool."))
                .withLongOpt(Params.DISABLE_SHNTOOL)
                .create());

        options.addOption(OptionBuilder
                .withDescription(replaceUmlaut("Zakáže použití Checkmate."))
                .withLongOpt(Params.DISABLE_CHECKMATE)
                .create());

        options.addOption(OptionBuilder
                .withLongOpt(Params.HELP)
                .withDescription(replaceUmlaut("Zobrazit nápovědu."))
                .create());
        options.addOption(OptionBuilder
                .withLongOpt(Params.VERSION)
                .withDescription(replaceUmlaut("Zobrazit informace o verzi."))
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
                File configDir = null;
                switch (action) {
                    case VALIDATE_PSP:
                    case VALIDATE_PSP_GROUP:
                    case VALIDATE_METADATA_BY_PROFILE:
                        if (!line.hasOption(Params.CONFIG_DIR)) {
                            System.err.println(String.format("Chyba: pro akci %s je parametr --%s povinný!", action, Params.CONFIG_DIR));
                            printHelp(options);
                            return;
                        }
                        configDir = new File(line.getOptionValue(Params.CONFIG_DIR));
                }

                //psp / psp-group
                File psp = null;
                File pspGroup = null;
                switch (action) {
                    case VALIDATE_PSP:
                    case BUILD_MINIFIED_PACKAGE: {
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

                File minifiedPspDir = null;
                switch (action) {
                    case BUILD_MINIFIED_PACKAGE: {
                        if (!line.hasOption(Params.MINIFIED_PSP_DIR)) {
                            System.err.println(String.format("Chyba: pro akci %s je parametr --%s povinný!", action, Params.MINIFIED_PSP_DIR));
                            printHelp(options);
                            return;
                        } else {
                            minifiedPspDir = new File(line.getOptionValue(Params.MINIFIED_PSP_DIR));
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

                String metadataProfileId = null;
                File metadataFile = null;
                switch (action) {
                    case VALIDATE_METADATA_BY_PROFILE: {
                        if (!line.hasOption(Params.METADATA_PROFILE_ID)) {
                            System.err.println(String.format("Chyba: pro akci %s je parametr --%s povinný!", action, Params.METADATA_PROFILE_ID));
                            printHelp(options);
                            return;
                        } else {
                            metadataProfileId = line.getOptionValue(Params.METADATA_PROFILE_ID);
                        }
                        if (!line.hasOption(Params.METADATA_FILE)) {
                            System.err.println(String.format("Chyba: pro akci %s je parametr --%s povinný!", action, Params.METADATA_FILE));
                            printHelp(options);
                            return;
                        } else {
                            metadataFile = new File(line.getOptionValue(Params.METADATA_FILE));
                        }
                        break;
                    }
                }

                //quitAfterInvalidPsps
                Integer quitAfterInvalidPsps = null;
                /*TODO: implement*/
                /*if (line.hasOption(Params.QUIT_AFTER_NTH_INVALID_PSP)) {
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
                }*/

                //preferred dmf versions
                String preferDmfMonVersion = null;
                if (line.hasOption(Params.PREFERRED_DMF_MON_VERSION)) {
                    preferDmfMonVersion = line.getOptionValue(Params.PREFERRED_DMF_MON_VERSION);
                }
                String preferDmfPerVersion = null;
                if (line.hasOption(Params.PREFERRED_DMF_PER_VERSION)) {
                    preferDmfPerVersion = line.getOptionValue(Params.PREFERRED_DMF_PER_VERSION);
                }
                String preferDmfAdgVersion = null;
                if (line.hasOption(Params.PREFERRED_DMF_ADG_VERSION)) {
                    preferDmfAdgVersion = line.getOptionValue(Params.PREFERRED_DMF_ADG_VERSION);
                }
                String preferDmfAdfVersion = null;
                if (line.hasOption(Params.PREFERRED_DMF_ADF_VERSION)) {
                    preferDmfAdfVersion = line.getOptionValue(Params.PREFERRED_DMF_ADF_VERSION);
                }
                //force dmf versions
                String forceDmfMonVersion = null;
                if (line.hasOption(Params.FORCED_DMF_MON_VERSION)) {
                    forceDmfMonVersion = line.getOptionValue(Params.FORCED_DMF_MON_VERSION);
                }
                String forceDmfPerVersion = null;
                if (line.hasOption(Params.FORCED_DMF_PER_VERSION)) {
                    forceDmfPerVersion = line.getOptionValue(Params.FORCED_DMF_PER_VERSION);
                }
                String forceDmfAdgVersion = null;
                if (line.hasOption(Params.FORCED_DMF_ADG_VERSION)) {
                    forceDmfAdgVersion = line.getOptionValue(Params.FORCED_DMF_ADG_VERSION);
                }
                String forceDmfAdfVersion = null;
                if (line.hasOption(Params.FORCED_DMF_ADF_VERSION)) {
                    forceDmfAdfVersion = line.getOptionValue(Params.FORCED_DMF_ADF_VERSION);
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

                //external utils
                Map<ExternalUtil, File> utilsPaths = new HashMap<>();
                Set<ExternalUtil> utilsDisabled = new HashSet<>();
                if (line.hasOption(Params.DISABLE_IMAGEMAGICK)) {
                    utilsDisabled.add(ExternalUtil.IMAGE_MAGICK);
                } else {
                    if (line.hasOption(Params.IMAGEMAGICK_PATH)) {
                        utilsPaths.put(ExternalUtil.IMAGE_MAGICK, new File(line.getOptionValue(Params.IMAGEMAGICK_PATH)));
                    }
                }
                if (line.hasOption(Params.DISABLE_JHOVE)) {
                    utilsDisabled.add(ExternalUtil.JHOVE);
                } else {
                    if (line.hasOption(Params.JHOVE_PATH)) {
                        utilsPaths.put(ExternalUtil.JHOVE, new File(line.getOptionValue(Params.JHOVE_PATH)));
                    }
                }
                if (line.hasOption(Params.DISABLE_JPYLYZER)) {
                    utilsDisabled.add(ExternalUtil.JPYLYZER);
                } else {
                    if (line.hasOption(Params.JPYLYZER_PATH)) {
                        utilsPaths.put(ExternalUtil.JPYLYZER, new File(line.getOptionValue(Params.JPYLYZER_PATH)));
                    }
                }
                if (line.hasOption(Params.DISABLE_KAKADU)) {
                    utilsDisabled.add(ExternalUtil.KAKADU);
                } else {
                    if (line.hasOption(Params.KAKADU_PATH)) {
                        utilsPaths.put(ExternalUtil.KAKADU, new File(line.getOptionValue(Params.KAKADU_PATH)));
                    }
                }
                if (line.hasOption(Params.DISABLE_MP3VAL)) {
                    utilsDisabled.add(ExternalUtil.MP3VAL);
                } else {
                    if (line.hasOption(Params.MP3VAL_PATH)) {
                        utilsPaths.put(ExternalUtil.MP3VAL, new File(line.getOptionValue(Params.MP3VAL_PATH)));
                    }
                }
                if (line.hasOption(Params.DISABLE_SHNTOOL)) {
                    utilsDisabled.add(ExternalUtil.SHNTOOL);
                } else {
                    if (line.hasOption(Params.SHNTOOL_PATH)) {
                        utilsPaths.put(ExternalUtil.SHNTOOL, new File(line.getOptionValue(Params.SHNTOOL_PATH)));
                    }
                }
                if (line.hasOption(Params.DISABLE_CHECKMATE)) {
                    utilsDisabled.add(ExternalUtil.CHECKMATE);
                } else {
                    if (line.hasOption(Params.CHECKMATE_PATH)) {
                        utilsPaths.put(ExternalUtil.CHECKMATE, new File(line.getOptionValue(Params.CHECKMATE_PATH)));
                    }
                }

                DmfDetector.Params dmfDetectorParams = new DmfDetector.Params();
                dmfDetectorParams.forcedDmfMonVersion = forceDmfMonVersion;
                dmfDetectorParams.forcedDmfPerVersion = forceDmfPerVersion;
                dmfDetectorParams.forcedDmfAdgVersion = forceDmfAdgVersion;
                dmfDetectorParams.forcedDmfAdfVersion = forceDmfAdfVersion;
                dmfDetectorParams.preferredDmfMonVersion = preferDmfMonVersion;
                dmfDetectorParams.preferredDmfPerVersion = preferDmfPerVersion;
                dmfDetectorParams.preferredDmfAdgVersion = preferDmfAdgVersion;
                dmfDetectorParams.preferredDmfAdfVersion = preferDmfAdfVersion;

                PrintStream out = System.out;
                PrintStream err = System.err;

                switch (action) {
                    case VALIDATE_PSP:
                        validatePsp(psp,
                                configDir, tmpDir,
                                verbosity, out, err, xmlProtocolDir, xmlProtocolFile,
                                dmfDetectorParams,
                                utilsPaths, utilsDisabled,
                                devParams);
                        break;
                    case VALIDATE_PSP_GROUP:
                        validatePspGroup(pspGroup,
                                configDir, tmpDir,
                                verbosity, out, err, xmlProtocolDir,
                                dmfDetectorParams,
                                utilsPaths, utilsDisabled,
                                devParams);
                        break;
                    case BUILD_MINIFIED_PACKAGE:
                        //TODO: level validace do CLI
                        buildMinifiedPackage(psp, minifiedPspDir, 3);
                        break;
                    case VALIDATE_METADATA_BY_PROFILE:
                        ValidationResult result = validateMetadataByProfile(configDir, metadataProfileId, metadataFile);
                        if (result != null) {
                            if (result.hasProblems()) {
                                result.getProblems().forEach(p -> System.out.println(p.getLevel() + ": " + p.getMessage(false)));
                            } else {
                                System.out.println("No problems found in file " + metadataFile.getName() + " against profile " + metadataProfileId);
                            }
                        }
                        break;
                    case DEV:
                        dev();
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

    private static ValidationResult validateMetadataByProfile(File validatorConfigDir, String profileId, File metadataFile) {
        try {
            System.out.println("Validuji oproti profilu " + profileId + " soubor " + metadataFile);
            String[] profileTokens = profileId.split(":");
            String dmf = profileTokens[0];
            DictionaryManager dm = new DictionaryManager(new File(validatorConfigDir, "dictionaries"));
            MetadataProfileParser metadataProfileParser = new MetadataProfileParser(dm);
            switch (profileTokens[1]) {
                case "biblio": {
                    String metadataFormatStr = profileTokens[2];
                    MetadataFormat metadataFormat = null;
                    if (metadataFormatStr.equals("dc")) {
                        metadataFormat = MetadataFormat.DC;
                    } else if (metadataFormatStr.equals("mods")) {
                        metadataFormat = MetadataFormat.MODS;
                    } else {
                        System.out.println("Unknown metadata format: " + metadataFormatStr);
                        return null;
                    }
                    String profileName = profileTokens[3];
                    File profileFile = new File(validatorConfigDir, "fDMF/" + dmf + "/biblioProfiles/" + metadataFormatStr + "/" + profileName + ".xml");
                    if (!profileFile.exists()) {
                        System.out.println("Profile file does not exist: " + profileFile.getAbsolutePath());
                        return null;
                    }
                    MetadataProfile metadataProfile = metadataProfileParser.parseProfile(profileFile);
                    Document metadataDoc = XmlUtils.buildDocumentFromFile(metadataFile, true);
                    ValidationResult result = new ValidationResult();
                    MetadataProfileValidator.validate(metadataProfile, metadataFile, metadataDoc, result, null);
                    return result;
                }
                case "tech": {
                    String profileName = profileTokens[2];
                    File profileFile = new File(validatorConfigDir, "fDMF/" + dmf + "/techProfiles/" + profileName + ".xml");
                    if (!profileFile.exists()) {
                        System.out.println("Profile file does not exist: " + profileFile.getAbsolutePath());
                        return null;
                    }
                    MetadataProfile metadataProfile = metadataProfileParser.parseProfile(profileFile);
                    Document metadataDoc = XmlUtils.buildDocumentFromFile(metadataFile, true);
                    ValidationResult result = new ValidationResult();
                    MetadataProfileValidator.validate(metadataProfile, metadataFile, metadataDoc, result, null);
                    return result;
                }
                default:
                    System.out.println("Unknown profile type: " + profileTokens[1]);
                    return null;
            }
        } catch (ValidatorConfigurationException e) {
            throw new RuntimeException(e);
        } catch (XPathExpressionException e) {
            throw new RuntimeException(e);
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InvalidXPathExpressionException e) {
            throw new RuntimeException(e);
        } catch (SAXException e) {
            throw new RuntimeException(e);
        }
    }

    private static void dev() {
        System.out.println("DEV");
        nkp.pspValidator.shared.Main.templateTest();
    }

    private static void buildMinifiedPackage(File pspDirOrZipFile, File minifiedPspDir, int level) {
        System.out.println("Vyrábím minifikovaný psp balík ze zdrojového balíku " + pspDirOrZipFile.getAbsolutePath());
        System.out.println("Výsledek bude uložen do adresáře " + minifiedPspDir.getAbsolutePath());
        //checkou minifiedPspDir
        if (!minifiedPspDir.exists()) {
            System.err.println(String.format("Chyba: adresář %s neexistuje!", minifiedPspDir.getAbsolutePath()));
        } else if (!minifiedPspDir.isDirectory()) {
            System.err.println(String.format("Chyba: soubor %s není adresář!", minifiedPspDir.getAbsolutePath()));
        } else if (!minifiedPspDir.canWrite()) {
            System.err.println(String.format("Chyba: nemůžu zapisovat do adresáře %s!", minifiedPspDir.getAbsolutePath()));
        } else if (!pspDirOrZipFile.exists()) {
            throw new IllegalStateException(String.format("Soubor %s neexistuje", pspDirOrZipFile.getAbsolutePath()));
        } else {
            try {
                if (pspDirOrZipFile.isDirectory()) {
                    if (!pspDirOrZipFile.canRead()) {
                        throw new IllegalStateException(String.format("Nelze číst adresář %s", pspDirOrZipFile.getAbsolutePath()));
                    }
                    minifyPspDir(pspDirOrZipFile, minifiedPspDir, level);
                } else {
                    minifyPspZip(pspDirOrZipFile, minifiedPspDir, minifiedPspDir, level);
                }
            } catch (IOException e) {
                throw new IllegalStateException("Chyba při minifikaci ", e);
            }
        }
    }

    private static void minifyPspZip(File pspZipFile, File tmpRootDir, File minifiedRootDir, int level) throws IOException {
        try {
            try {
                new ZipFile(pspZipFile);
            } catch (ZipException e) {
                System.out.println(String.format("Soubor %s není adresář ani soubor ZIP, ignoruji.", pspZipFile.getAbsolutePath()));
                return;
            }
            if (tmpRootDir == null) {
                System.err.println(String.format("Chyba: prázdný parametr --%s: adresář pro dočasné soubory je potřeba pro rozbalení ZIP souboru %s!", Params.TMP_DIR, pspZipFile.getAbsolutePath()));
            } else if (!tmpRootDir.exists()) {
                System.err.println(String.format("Chyba: adresář %s neexistuje!", pspZipFile.getAbsolutePath()));
            } else if (!tmpRootDir.isDirectory()) {
                System.err.println(String.format("Chyba: soubor %s není adresář!", pspZipFile.getAbsolutePath()));
            } else if (!tmpRootDir.canWrite()) {
                System.err.println(String.format("Chyba: nemůžu zapisovat do adresáře %s!", pspZipFile.getAbsolutePath()));
            } else {
                File tmpPspDir = new File(tmpRootDir, pspZipFile.getName() + "_extracted");
                if (tmpPspDir.exists()) {
                    System.out.println(String.format("Mažu adresář %s", tmpPspDir.getAbsolutePath()));
                    Utils.deleteNonemptyDir(tmpPspDir);
                }
                System.out.println(String.format("Rozbaluji %s do adresáře %s", pspZipFile.getAbsolutePath(), tmpPspDir.getAbsolutePath()));
                Utils.unzip(pspZipFile, tmpPspDir);
                System.out.println(tmpPspDir.getAbsolutePath());
                minifyPspDir(tmpPspDir, minifiedRootDir, level);
                Utils.deleteNonemptyDir(tmpPspDir);
            }
        } catch (IOException e) {
            System.out.println(String.format("Chyba zpracování ZIP souboru %s: %s!", pspZipFile.getAbsolutePath(), e.getMessage()));
        }
    }

    /**
     * @param level uroven minifikace: 1: obrazky + audio, 2: obrazky + audio + txt, 3: obrazky + audio + txt + alto
     */
    private static void minifyPspDir(File pspDir, File minifiedRootDir, int level) throws IOException {
        String newDirName = pspDir.getName() + "_minified";
        File minifiedDir = new File(minifiedRootDir, newDirName);
        if (minifiedDir.exists()) {
            System.out.println("already exists, deleting: " + minifiedDir.getAbsolutePath());
            Utils.deleteNonemptyDir(minifiedDir);
        }
        Files.walkFileTree(pspDir.toPath(), new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                Path newDir = minifiedDir.toPath().resolve(pspDir.toPath().relativize(dir));
                System.out.println("Creating directory: " + newDir);
                Files.createDirectory(newDir);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                List<String> suffixesOfFilesToMinify;
                if (level == 1) {
                    suffixesOfFilesToMinify = Arrays.asList(".jp2", ".wav", ".mp3");
                } else {
                    suffixesOfFilesToMinify = Arrays.asList(".jp2", ".wav", ".mp3", ".txt");
                }
                boolean minify = false;
                for (String suffix : suffixesOfFilesToMinify) {
                    if (file.getFileName().toString().endsWith(suffix)) {
                        minify = true;
                        break;
                    }
                }
                if (level >= 3) {
                    if (file.getFileName().toString().contains("alto")) {
                        minify = true;
                    }
                }
                if (minify) {
                    System.out.println("Minifying file: " + file);
                    Path newFile = minifiedDir.toPath().resolve(pspDir.toPath().relativize(file));
                    Files.createFile(newFile);
                } else {
                    Path newFile = minifiedDir.toPath().resolve(pspDir.toPath().relativize(file));
                    System.out.println("Copying file: " + newFile);
                    Files.copy(file, newFile);
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }

    /*Docasne odstrani diakritiku, dokud se neopravi problem s kodovanim na Windows*/
    private static String replaceUmlaut(String string) {
        Map<String, String> replacements = new HashMap<>();
        replacements.put("á", "a");
        replacements.put("Á", "A");
        replacements.put("é", "e");
        replacements.put("É", "E");
        replacements.put("ě", "e");
        replacements.put("Ě", "E");
        replacements.put("í", "i");
        replacements.put("Í", "I");
        replacements.put("ó", "o");
        replacements.put("Ó", "O");
        replacements.put("ů", "u");
        replacements.put("Ů", "U");
        replacements.put("ú", "u");
        replacements.put("Ú", "U");

        replacements.put("č", "c");
        replacements.put("Č", "C");
        replacements.put("ď", "d");
        replacements.put("Ď", "D");
        replacements.put("ň", "n");
        replacements.put("Ň", "N");
        replacements.put("ř", "r");
        replacements.put("Ř", "R");
        replacements.put("š", "s");
        replacements.put("Š", "S");
        replacements.put("ť", "t");
        replacements.put("Ť", "T");
        replacements.put("ž", "z");
        replacements.put("Ž", "Z");

        for (String letter : replacements.keySet()) {
            string = string.replaceAll(letter, replacements.get(letter));
        }
        return string;
    }

    private static String toString(String[] args) {
        StringBuilder builder = new StringBuilder();
        for (String string : args) {
            builder.append(string).append(' ');
        }
        return builder.toString();
    }

    private static void printHelp(Options options) {
        String header = replaceUmlaut("\n" +
                "Validuje PSP balík/sadu PSP balíků podle DMF*, nebo vyrobí minifikovaný PSP balík.\n\n" +
                "Validace:\n" +
                "--------\n" +
                "Typ DMF (Monografie/Periodikum/Zvuk-gramodeska) se odvozuje z dat jednotlivých PSP balíků." +
                " Verze DMF použité pro validaci lze ovlivnit parametry" +
                " --" + Params.PREFERRED_DMF_MON_VERSION + "," +
                " --" + Params.PREFERRED_DMF_PER_VERSION + "," +
                " --" + Params.PREFERRED_DMF_ADG_VERSION + "," +
                " --" + Params.FORCED_DMF_MON_VERSION + "," +
                " --" + Params.FORCED_DMF_PER_VERSION + " a" +
                " --" + Params.FORCED_DMF_ADG_VERSION + "." +
                " Dále je potřeba pomocí --config-dir uvést adresář, který obsahuje definice fDMF," +
                " např. monograph_2.1, periodical_2.0, nebo audio_gram_0.5.\n" +
                "\n" +
                "Výroba minifikovaného balíku:\n" +
                "----------------------------\n" +
                "Bude vyrobena kopie balíku s tím, že textové a obrazové soubory budou nahrazny prázdnými soubory se stejným jménem." +
                "\n\n");
        String footer = replaceUmlaut("\n*Definice metadatových formátů. Více na http://www.ndk.cz/standardy-digitalizace/metadata.\n" +
                "Více informací o validátoru najdete na https://github.com/NLCR/komplexni-validator.");
        HelpFormatter formatter = new HelpFormatter();
        formatter.setWidth(100);
        formatter.setOptionComparator(null);
        formatter.printHelp("java -jar KomplexniValidator.jar", header, options, footer, true);
    }

    private static void validatePspGroup(File pspGroup,
                                         File configDir, File tmpDir,
                                         Integer verbosity, PrintStream out, PrintStream err,
                                         File xmlProtocolDir,
                                         DmfDetector.Params dmfDetectorParams,
                                         Map<ExternalUtil, File> utilsPaths, Set<ExternalUtil> utilsDisabled, Validator.DevParams devParams) throws ValidatorConfigurationException, XmlFileParsingException, FdmfRegistry.UnknownFdmfException, PspDataException, InvalidXPathExpressionException {
        Platform platform = Platform.detectOs();
        out.println(String.format("Platforma: %s", platform.toReadableString()));

        //validator configuration
        out.println(String.format("Kořenový adresář konfigurace validátoru: %s", configDir.getAbsolutePath()));
        ValidatorConfigurationManager validatorConfigManager = new ValidatorConfigurationManager(configDir);
        ExternalUtilManager externalUtilManager = new ExternalUtilManagerFactory(validatorConfigManager.getExternalUtilsConfigFile()).buildExternalUtilManager(platform.getOperatingSystem());
        externalUtilManager.setPaths(utilsPaths);
        detectImageTools(out, externalUtilManager, utilsDisabled);


        //pspDirOrZipFile dir or zip file?
        if (!pspGroup.exists()) {
            throw new IllegalStateException(String.format("Soubor %s neexistuje", pspGroup.getAbsolutePath()));
        } else {
            if (pspGroup.isDirectory()) {
                if (!pspGroup.canRead()) {
                    throw new IllegalStateException(String.format("Nelze číst adresář %s", pspGroup.getAbsolutePath()));
                }
                validatePspGroupDir(pspGroup,
                        configDir, tmpDir,
                        verbosity, out, err, xmlProtocolDir,
                        dmfDetectorParams,
                        utilsPaths, utilsDisabled,
                        devParams);
            } else {
                validatePspGroupZip(pspGroup,
                        configDir, tmpDir,
                        verbosity, out, err, xmlProtocolDir,
                        dmfDetectorParams,
                        utilsPaths, utilsDisabled,
                        devParams);
            }
        }
    }

    private static void validatePspGroupZip(File pspGroupFile,
                                            File configDir, File tmpDir,
                                            Integer verbosity, PrintStream out, PrintStream err,
                                            File xmlProtocolDir,
                                            DmfDetector.Params dmfDetectorParams,
                                            Map<ExternalUtil, File> utilsPaths, Set<ExternalUtil> utilsDisabled,
                                            Validator.DevParams devParams) throws PspDataException, XmlFileParsingException, FdmfRegistry.UnknownFdmfException, ValidatorConfigurationException, InvalidXPathExpressionException {
        try {
            try {
                new ZipFile(pspGroupFile);
            } catch (ZipException e) {
                out.println(String.format("Soubor %s není adresář ani soubor ZIP, ignoruji.", pspGroupFile.getAbsolutePath()));
                return;
            }
            if (tmpDir == null) {
                err.println(String.format("Chyba: prázdný parametr --%s: adresář pro dočasné soubory je potřeba pro rozbalení ZIP souboru %s!", Params.TMP_DIR, pspGroupFile.getAbsolutePath()));
            } else if (!tmpDir.exists()) {
                err.println(String.format("Chyba: adresář %s neexistuje!", tmpDir.getAbsolutePath()));
            } else if (!tmpDir.isDirectory()) {
                err.println(String.format("Chyba: soubor %s není adresář!", tmpDir.getAbsolutePath()));
            } else if (!tmpDir.canWrite()) {
                err.println(String.format("Chyba: nemůžu zapisovat do adresáře %s!", tmpDir.getAbsolutePath()));
            } else {
                File containerDir = new File(tmpDir, pspGroupFile.getName() + "_extracted");
                if (containerDir.exists()) {
                    out.println(String.format("Mažu adresář %s", containerDir.getAbsolutePath()));
                    Utils.deleteNonemptyDir(containerDir);
                }
                out.println(String.format("Rozbaluji %s do adresáře %s", pspGroupFile.getAbsolutePath(), containerDir.getAbsolutePath()));
                Utils.unzip(pspGroupFile, containerDir);
                File[] filesInContainer = containerDir.listFiles();
                if (filesInContainer.length == 1 && filesInContainer[0].isDirectory()) {
                    validatePspGroupDir(filesInContainer[0],
                            configDir, tmpDir,
                            verbosity, out, err, xmlProtocolDir,
                            dmfDetectorParams,
                            utilsPaths, utilsDisabled,
                            devParams);
                } else {
                    validatePspGroupDir(containerDir,
                            configDir, tmpDir,
                            verbosity, out, err, xmlProtocolDir,
                            dmfDetectorParams,
                            utilsPaths, utilsDisabled,
                            devParams);
                }
            }
        } catch (IOException e) {
            out.println(String.format("Chyba zpracování ZIP souboru %s: %s!", pspGroupFile.getAbsolutePath(), e.getMessage()));
        }
    }

    private static void validatePspGroupDir(File pspGroupDir,
                                            File configDir, File tmpDir,
                                            Integer verbosity, PrintStream out, PrintStream err,
                                            File xmlProtocolDir,
                                            DmfDetector.Params dmfDetectorParams,
                                            Map<ExternalUtil, File> utilsPaths, Set<ExternalUtil> utilsDisabled,
                                            Validator.DevParams devParams) throws XmlFileParsingException, FdmfRegistry.UnknownFdmfException, PspDataException, ValidatorConfigurationException, InvalidXPathExpressionException {
        for (File pspDirOrZip : pspGroupDir.listFiles()) {
            //TODO: pocitat nevalidni baliky
            validatePsp(pspDirOrZip,
                    configDir, tmpDir,
                    verbosity, out, err,
                    xmlProtocolDir, null,
                    dmfDetectorParams,
                    utilsPaths, utilsDisabled,
                    devParams);
        }
    }

    private static void validatePsp(File pspDirOrZipFile,
                                    File configDir, File tmpDir,
                                    Integer verbosity, PrintStream out, PrintStream err,
                                    File xmlProtocolDir, File xmlProtocolFile,
                                    DmfDetector.Params dmfDetectorParams,
                                    Map<ExternalUtil, File> utilsPaths, Set<ExternalUtil> utilsDisabled,
                                    Validator.DevParams devParams) throws ValidatorConfigurationException, FdmfRegistry.UnknownFdmfException, PspDataException, InvalidXPathExpressionException, XmlFileParsingException {
        Platform platform = Platform.detectOs();
        out.println(String.format("Platforma: %s", platform.toReadableString()));

        //validator configuration
        out.println(String.format("Kořenový adresář konfigurace validátoru: %s", configDir.getAbsolutePath()));
        ValidatorConfigurationManager validatorConfigManager = new ValidatorConfigurationManager(configDir);
        ExternalUtilManager externalUtilManager = new ExternalUtilManagerFactory(validatorConfigManager.getExternalUtilsConfigFile()).buildExternalUtilManager(platform.getOperatingSystem());
        externalUtilManager.setPaths(utilsPaths);
        detectImageTools(out, externalUtilManager, utilsDisabled);

        //pspDirOrZipFile dir or zip file?
        if (!pspDirOrZipFile.exists()) {
            throw new IllegalStateException(String.format("Soubor %s neexistuje", pspDirOrZipFile.getAbsolutePath()));
        } else {
            if (pspDirOrZipFile.isDirectory()) {
                if (!pspDirOrZipFile.canRead()) {
                    throw new IllegalStateException(String.format("Nelze číst adresář %s", pspDirOrZipFile.getAbsolutePath()));
                }
                validatePspDir(pspDirOrZipFile,
                        externalUtilManager, validatorConfigManager,
                        out, verbosity, xmlProtocolDir, xmlProtocolFile,
                        dmfDetectorParams,
                        devParams);
            } else {
                validatePspZip(pspDirOrZipFile,
                        tmpDir,
                        externalUtilManager, validatorConfigManager,
                        out, err, verbosity, xmlProtocolDir, xmlProtocolFile,
                        dmfDetectorParams,
                        devParams);
            }
        }
    }

    private static void validatePspZip(File pspZipFile,
                                       File tmpDir,
                                       ExternalUtilManager externalUtilManager, ValidatorConfigurationManager validatorConfigManager,
                                       PrintStream out, PrintStream err, Integer verbosity,
                                       File xmlProtocolDir, File xmlProtocolFile,
                                       DmfDetector.Params dmfDetectorParams,
                                       Validator.DevParams devParams) throws XmlFileParsingException, FdmfRegistry.UnknownFdmfException, PspDataException, ValidatorConfigurationException, InvalidXPathExpressionException {
        try {
            try {
                new ZipFile(pspZipFile);
            } catch (ZipException e) {
                out.println(String.format("Soubor %s není adresář ani soubor ZIP, ignoruji.", pspZipFile.getAbsolutePath()));
                return;
            }
            if (tmpDir == null) {
                err.println(String.format("Chyba: prázdný parametr --%s: adresář pro dočasné soubory je potřeba pro rozbalení ZIP souboru %s!", Params.TMP_DIR, pspZipFile.getAbsolutePath()));
            } else if (!tmpDir.exists()) {
                err.println(String.format("Chyba: adresář %s neexistuje!", pspZipFile.getAbsolutePath()));
            } else if (!tmpDir.isDirectory()) {
                err.println(String.format("Chyba: soubor %s není adresář!", pspZipFile.getAbsolutePath()));
            } else if (!tmpDir.canWrite()) {
                err.println(String.format("Chyba: nemůžu zapisovat do adresáře %s!", pspZipFile.getAbsolutePath()));
            } else {
                File containerDir = new File(tmpDir, pspZipFile.getName() + "_extracted");
                if (containerDir.exists()) {
                    out.println(String.format("Mažu adresář %s", containerDir.getAbsolutePath()));
                    Utils.deleteNonemptyDir(containerDir);
                }
                out.println(String.format("Rozbaluji %s do adresáře %s", pspZipFile.getAbsolutePath(), containerDir.getAbsolutePath()));
                Utils.unzip(pspZipFile, containerDir);
                File[] filesInContainer = containerDir.listFiles();
                if (filesInContainer.length == 1 && filesInContainer[0].isDirectory()) {
                    validatePspDir(filesInContainer[0],
                            externalUtilManager, validatorConfigManager,
                            out, verbosity, xmlProtocolDir, xmlProtocolFile,
                            dmfDetectorParams,
                            devParams);
                } else {
                    validatePspDir(containerDir,
                            externalUtilManager, validatorConfigManager,
                            out, verbosity, xmlProtocolDir, xmlProtocolFile,
                            dmfDetectorParams,
                            devParams);
                }
            }
        } catch (IOException e) {
            out.println(String.format("Chyba zpracování ZIP souboru %s: %s!", pspZipFile.getAbsolutePath(), e.getMessage()));
        }
    }

    private static void validatePspDir(File pspDir,
                                       ExternalUtilManager externalUtilManager, ValidatorConfigurationManager validatorConfigManager,
                                       PrintStream out, Integer verbosity,
                                       File xmlProtocolDir, File xmlProtocolFile,
                                       DmfDetector.Params dmfDetectorParams,
                                       Validator.DevParams devParams) throws ValidatorConfigurationException, FdmfRegistry.UnknownFdmfException, PspDataException, XmlFileParsingException, InvalidXPathExpressionException {
        //psp dir, dmf detection
        checkReadableDir(pspDir);
        out.println(String.format("Zpracovávám PSP balík %s", pspDir.getAbsolutePath()));


        Dmf dmfResolved = new DmfDetector().resolveDmf(pspDir, dmfDetectorParams);
        out.println(String.format("Bude použita verze standardu %s", dmfResolved));

        //initializes j2k profiles according to selected fDMF
        FdmfConfiguration fdmfConfig = new FdmfRegistry(validatorConfigManager).getFdmfConfig(dmfResolved);
        fdmfConfig.initBinaryFileProfiles(externalUtilManager);

        //xml protocol file
        if (xmlProtocolFile == null) {
            if (xmlProtocolDir != null) {
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss");
                String filename = String.format("%s_%s.xml", simpleDateFormat.format(new Date(System.currentTimeMillis())), pspDir.getName());
                xmlProtocolFile = new File(xmlProtocolDir, filename);
            }
        }

        //validate
        Validator validator = ValidatorFactory.buildValidator(fdmfConfig, pspDir, validatorConfigManager.getDictionaryManager());
        out.println(String.format("Validátor inicializován, spouštím validace"));
        ValidationState.ProgressListener progressListener = null;
        validator.run(xmlProtocolFile, out, verbosity, devParams, null, progressListener, null);
    }


    private static void detectImageTools(PrintStream out, ExternalUtilManager externalUtilManager, Set<ExternalUtil> utilsDisabled) {
        for (ExternalUtil util : ExternalUtil.values()) {
            if (utilsDisabled.contains(util)) {
                out.println(String.format("Vypnuto použití nástroje %s.", util.getUserFriendlyName()));
                externalUtilManager.setUtilDisabled(util);
            } else {
                out.print(String.format("Kontroluji dostupnost nástroje %s: ", util.getUserFriendlyName()));
                if (!externalUtilManager.isVersionDetectionDefined(util)) {
                    out.println("není definován způsob detekce verze");
                } else if (!externalUtilManager.isUtilAvailable(util)) {
                    out.println("není definován způsob spuštění");
                } else {
                    try {
                        String version = externalUtilManager.runUtilVersionDetection(util);
                        if (version != null) {
                            externalUtilManager.setUtilAvailable(util, true);
                            out.println("nalezen, verze: " + version);
                        } else {
                            out.println("nenalezen");
                        }
                    } catch (CliCommand.CliCommandException e) {
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

}
