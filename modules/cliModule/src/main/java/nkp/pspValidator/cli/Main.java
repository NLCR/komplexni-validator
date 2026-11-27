package nkp.pspValidator.cli;

import nkp.pspValidator.shared.engine.utils.UrnNbnResolverChecker;
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
import nkp.pspValidator.shared.externalUtils.VersionCheck;
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

    /**
     * CLI doběhlo: validace proběhla a protokol vznikl (balík může být validní i nevalidní, verdikt je v protokolu
     * a na výstupu), nebo byla vyžádána nápověda či verze.
     */
    static final int EXIT_OK = 0;
    /**
     * Balík (nebo aspoň jeden balík skupiny) se nezvalidoval a protokol nevznikl: pro zjištěný typ a verzi DMF není
     * definovaná fDMF, balík nejde přečíst nebo rozbalit, nebo jeho data nejdou zpracovat. Stejný kód jako u
     * neodchycené výjimky (např. neexistující balík), kdy JVM skončí kódem 1.
     */
    static final int EXIT_PSP_NOT_VALIDATED = 1;
    /**
     * Chyba použití: chybějící nebo neznámý parametr či akce, chybná hodnota parametru, chyba parsování parametrů.
     * Vypíše se hláška na chybový výstup a nápověda.
     */
    static final int EXIT_USAGE_ERROR = 2;
    /**
     * Chyba konfigurace validátoru ({@link ValidatorConfigurationException}): adresář konfigurace neexistuje, chybí
     * v něm manifest.properties, má jinou hlavní verzi nebo je konfigurace jinak vadná.
     */
    static final int EXIT_CONFIG_ERROR = 3;

    public static void main(String[] args) throws PspDataException, XmlFileParsingException, InvalidXPathExpressionException, FdmfRegistry.UnknownFdmfException, ValidatorConfigurationException, FdmfRegistry.UnknownFdmfException {
        int exitCode = run(null, args);
        //XsdValidator.testXsds();
        if (exitCode != EXIT_OK) {
            System.exit(exitCode);
        }
    }

    /**
     * Jako {@link #run(Validator.DevParams, String[])}, návratový kód se zahodí (testy a volání bez ukončení JVM).
     */
    public static void main(Validator.DevParams devParams, String[] args) {
        run(devParams, args);
    }

    static int run(Validator.DevParams devParams, String[] args) {
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
                        "Soubor pro uložení xml protokolu s informacemi o validaci PSP balíku. Použije se jen pro akce VALIDATE_PSP a VALIDATE_METADATA_BY_PROFILE. " +
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
                        "Preferovaná verze DMF pro validaci zvukových dokumentů - zvukových disků. " +
                                "Použije se k validaci, pokud je balík typu Zvukový dokument (zvukový disk), data balíku neobsahují informaci o vhodné verzi DMF Zvukové dokumenty " +
                                "a parametr --" + Params.FORCED_DMF_ADI_VERSION + " není vyplněn."))
                .hasArg()
                .withArgName("VERZE")
                .withLongOpt(Params.PREFERRED_DMF_ADI_VERSION)
                .create());
        options.addOption(OptionBuilder
                .withDescription(replaceUmlaut(
                        "Preferovaná verze DMF pro validaci zvukových dokumentů - zvukových dokumentů bez nosiče. " +
                                "Použije se k validaci, pokud je balík typu Zvukový dokument (zvukové dokumenty bez nosiče), data balíku neobsahují informaci o vhodné verzi DMF Zvukové dokumenty " +
                                "a parametr --" + Params.FORCED_DMF_ADN_VERSION + " není vyplněn."))
                .hasArg()
                .withArgName("VERZE")
                .withLongOpt(Params.PREFERRED_DMF_ADN_VERSION)
                .create());
        options.addOption(OptionBuilder
                .withDescription(replaceUmlaut(
                        "Preferovaná verze DMF pro validaci datových disků. " +
                                "Použije se k validaci, pokud je balík typu Datový disk, data balíku neobsahují informaci o vhodné verzi DMF Datové disky " +
                                "a parametr --" + Params.FORCED_DMF_DAD_VERSION + " není vyplněn."))
                .hasArg()
                .withArgName("VERZE")
                .withLongOpt(Params.PREFERRED_DMF_DAD_VERSION)
                .create());
        options.addOption(OptionBuilder
                .withDescription(replaceUmlaut(
                        "Preferovaná verze DMF pro validaci jednotek fondu. " +
                                "Použije se k validaci, pokud je balík typu Jednotka fondu, data balíku neobsahují informaci o vhodné verzi DMF Jednotky fondu " +
                                "a parametr --" + Params.FORCED_DMF_FDU_VERSION + " není vyplněn."))
                .hasArg()
                .withArgName("VERZE")
                .withLongOpt(Params.PREFERRED_DMF_FDU_VERSION)
                .create());
        options.addOption(OptionBuilder
                .withDescription(replaceUmlaut(
                        "Preferovaná verze DMF pro validaci e-monografií. " +
                                "Použije se k validaci, pokud je balík typu E-monografie, data balíku neobsahují informaci o vhodné verzi DMF E-monografie " +
                                "a parametr --" + Params.FORCED_DMF_EMO_VERSION + " není vyplněn."))
                .hasArg()
                .withArgName("VERZE")
                .withLongOpt(Params.PREFERRED_DMF_EMO_VERSION)
                .create());
        options.addOption(OptionBuilder
                .withDescription(replaceUmlaut(
                        "Preferovaná verze DMF pro validaci e-periodik. " +
                                "Použije se k validaci, pokud je balík typu E-periodikum, data balíku neobsahují informaci o vhodné verzi DMF E-periodika " +
                                "a parametr --" + Params.FORCED_DMF_EPE_VERSION + " není vyplněn."))
                .hasArg()
                .withArgName("VERZE")
                .withLongOpt(Params.PREFERRED_DMF_EPE_VERSION)
                .create());
        options.addOption(OptionBuilder
                .withDescription(replaceUmlaut(
                        "Preferovaná verze DMF pro validaci skládaných e-periodik. " +
                                "Použije se k validaci, pokud je balík typu Skládané e-periodikum, data balíku neobsahují informaci o vhodné verzi DMF Skládaná e-periodika " +
                                "a parametr --" + Params.FORCED_DMF_SEP_VERSION + " není vyplněn."))
                .hasArg()
                .withArgName("VERZE")
                .withLongOpt(Params.PREFERRED_DMF_SEP_VERSION)
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
                        "Vynucená verze DMF pro validaci zvukových dokumentů - zvukových disků. " +
                                "Použije se k validaci všech balíků typu Zvukový dokument (zvukový disk) bez ohledu na data balíků a hodnotu parametru --" + Params.PREFERRED_DMF_ADI_VERSION + "."))
                .hasArg()
                .withArgName("VERZE")
                .withLongOpt(Params.FORCED_DMF_ADI_VERSION)
                .create());
        options.addOption(OptionBuilder
                .withDescription(replaceUmlaut(
                        "Vynucená verze DMF pro validaci zvukových dokumentů - zvukových dokumentů bez nosiče. " +
                                "Použije se k validaci všech balíků typu Zvukový dokument (zvukový dokument bez nosiče) bez ohledu na data balíků a hodnotu parametru --" + Params.PREFERRED_DMF_ADN_VERSION + "."))
                .hasArg()
                .withArgName("VERZE")
                .withLongOpt(Params.FORCED_DMF_ADN_VERSION)
                .create());
        options.addOption(OptionBuilder
                .withDescription(replaceUmlaut(
                        "Vynucená verze DMF pro validaci datových disků. " +
                                "Použije se k validaci všech balíků typu Datový disk bez ohledu na data balíků a hodnotu parametru --" + Params.PREFERRED_DMF_DAD_VERSION + "."))
                .hasArg()
                .withArgName("VERZE")
                .withLongOpt(Params.FORCED_DMF_DAD_VERSION)
                .create());
        options.addOption(OptionBuilder
                .withDescription(replaceUmlaut(
                        "Vynucená verze DMF pro validaci jednotek fondu. " +
                                "Použije se k validaci všech balíků typu Jednotka fondu bez ohledu na data balíků a hodnotu parametru --" + Params.PREFERRED_DMF_FDU_VERSION + "."))
                .hasArg()
                .withArgName("VERZE")
                .withLongOpt(Params.FORCED_DMF_FDU_VERSION)
                .create());
        options.addOption(OptionBuilder
                .withDescription(replaceUmlaut(
                        "Vynucená verze DMF pro validaci e-monografií. " +
                                "Použije se k validaci všech balíků typu E-monografie bez ohledu na data balíků a hodnotu parametru --" + Params.PREFERRED_DMF_EMO_VERSION + "."))
                .hasArg()
                .withArgName("VERZE")
                .withLongOpt(Params.FORCED_DMF_EMO_VERSION)
                .create());
        options.addOption(OptionBuilder
                .withDescription(replaceUmlaut(
                        "Vynucená verze DMF pro validaci e-periodik. " +
                                "Použije se k validaci všech balíků typu E-periodikum bez ohledu na data balíků a hodnotu parametru --" + Params.PREFERRED_DMF_EPE_VERSION + "."))
                .hasArg()
                .withArgName("VERZE")
                .withLongOpt(Params.FORCED_DMF_EPE_VERSION)
                .create());
        options.addOption(OptionBuilder
                .withDescription(replaceUmlaut(
                        "Vynucená verze DMF pro validaci skládaných e-periodik. " +
                                "Použije se k validaci všech balíků typu Skládané e-periodikum bez ohledu na data balíků a hodnotu parametru --" + Params.PREFERRED_DMF_SEP_VERSION + "."))
                .hasArg()
                .withArgName("VERZE")
                .withLongOpt(Params.FORCED_DMF_SEP_VERSION)
                .create());
        options.addOption(OptionBuilder
                .withDescription(replaceUmlaut(
                        "Zastaralé, použijte --" + Params.PREFERRED_DMF_EMO_VERSION + "."))
                .hasArg()
                .withArgName("VERZE")
                .withLongOpt(Params.PREFERRED_DMF_EMON_VERSION_DEPRECATED)
                .create());
        options.addOption(OptionBuilder
                .withDescription(replaceUmlaut(
                        "Zastaralé, použijte --" + Params.PREFERRED_DMF_EPE_VERSION + "."))
                .hasArg()
                .withArgName("VERZE")
                .withLongOpt(Params.PREFERRED_DMF_EPER_VERSION_DEPRECATED)
                .create());
        options.addOption(OptionBuilder
                .withDescription(replaceUmlaut(
                        "Zastaralé, použijte --" + Params.PREFERRED_DMF_SEP_VERSION + "."))
                .hasArg()
                .withArgName("VERZE")
                .withLongOpt(Params.PREFERRED_DMF_COMPER_VERSION_DEPRECATED)
                .create());
        options.addOption(OptionBuilder
                .withDescription(replaceUmlaut(
                        "Zastaralé, použijte --" + Params.FORCED_DMF_EMO_VERSION + "."))
                .hasArg()
                .withArgName("VERZE")
                .withLongOpt(Params.FORCED_DMF_EMON_VERSION_DEPRECATED)
                .create());
        options.addOption(OptionBuilder
                .withDescription(replaceUmlaut(
                        "Zastaralé, použijte --" + Params.FORCED_DMF_EPE_VERSION + "."))
                .hasArg()
                .withArgName("VERZE")
                .withLongOpt(Params.FORCED_DMF_EPER_VERSION_DEPRECATED)
                .create());
        options.addOption(OptionBuilder
                .withDescription(replaceUmlaut(
                        "Zastaralé, použijte --" + Params.FORCED_DMF_SEP_VERSION + "."))
                .hasArg()
                .withArgName("VERZE")
                .withLongOpt(Params.FORCED_DMF_COMPER_VERSION_DEPRECATED)
                .create());

        options.addOption(OptionBuilder
                .withDescription(replaceUmlaut(
                        "Základní URL Resolveru URN:NBN (CZIDLO) pro kontrolu registrace identifikátorů." +
                                " Výchozí je produkční instalace " + UrnNbnResolverChecker.DEFAULT_RESOLVER_BASE_URL + "." +
                                " Pro testování lze zadat např. https://resolver-test.nkp.cz." +
                                " Adresa http:// je povolená jen s přepínačem --" + Params.URNNBN_RESOLVER_ALLOW_HTTP + "." +
                                " Přesměrování z http na https validátor neprovádí, je třeba zadat cílovou adresu."))
                .hasArg()
                .withArgName("URL")
                .withLongOpt(Params.URNNBN_RESOLVER_URL)
                .create());

        options.addOption(OptionBuilder
                .withDescription(replaceUmlaut(
                        "Povolí Resolver URN:NBN na http:// (parametr --" + Params.URNNBN_RESOLVER_URL + ")," +
                                " určeno pro lokální testovací instalace bez TLS."))
                .withLongOpt(Params.URNNBN_RESOLVER_ALLOW_HTTP)
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
                .withDescription(replaceUmlaut("Adresář nástroje veraPDF (obsahuje skript verapdf, resp. verapdf.bat)." +
                        " Např. C:\\Program Files\\verapdf pro Windows, $(brew --prefix)/bin pro macOS s Homebrew."))
                .hasArg()
                .withArgName("ADRESAR")
                .withLongOpt(Params.VERAPDF_PATH)
                .create());

        options.addOption(OptionBuilder
                .withDescription(replaceUmlaut("Adresář nástroje EPUBCheck (obsahuje epubcheck.jar a adresář lib), spouští se přes java z PATH." +
                        " Např. C:\\epubcheck-5.4.0 pro Windows, $(brew --prefix epubcheck)/libexec pro macOS s Homebrew."))
                .hasArg()
                .withArgName("ADRESAR")
                .withLongOpt(Params.EPUBCHECK_PATH)
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
                .withDescription(replaceUmlaut("Zakáže použití veraPDF."))
                .withLongOpt(Params.DISABLE_VERAPDF)
                .create());

        options.addOption(OptionBuilder
                .withDescription(replaceUmlaut("Zakáže použití EPUBCheck."))
                .withLongOpt(Params.DISABLE_EPUBCHECK)
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
                    return EXIT_USAGE_ERROR;
                }
                String actionStr = line.getOptionValue(Params.ACTION);
                Action action;
                try {
                    action = Action.valueOf(actionStr);
                } catch (java.lang.IllegalArgumentException e) {
                    System.err.println(String.format("Chyba: neznámá akce %s!", actionStr));
                    printHelp(options);
                    return EXIT_USAGE_ERROR;
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
                            return EXIT_USAGE_ERROR;
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
                            return EXIT_USAGE_ERROR;
                        } else {
                            psp = new File(line.getOptionValue(Params.PSP));
                            if (!psp.exists()) {
                                // srozumitelná hláška místo neodchycené výjimky z pozdější kontroly
                                System.err.println(String.format("Chyba: balík %s neexistuje!", psp.getAbsolutePath()));
                                return EXIT_PSP_NOT_VALIDATED;
                            }
                            break;
                        }
                    }
                    case VALIDATE_PSP_GROUP: {
                        if (!line.hasOption(Params.PSP_GROUP)) {
                            System.err.println(String.format("Chyba: pro akci %s je parametr --%s povinný!", action, Params.PSP_GROUP));
                            printHelp(options);
                            return EXIT_USAGE_ERROR;
                        } else {
                            pspGroup = new File(line.getOptionValue(Params.PSP_GROUP));
                            if (!pspGroup.exists()) {
                                System.err.println(String.format("Chyba: skupina balíků %s neexistuje!", pspGroup.getAbsolutePath()));
                                return EXIT_PSP_NOT_VALIDATED;
                            }
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
                            return EXIT_USAGE_ERROR;
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
                            return EXIT_USAGE_ERROR;
                        } else {
                            metadataProfileId = line.getOptionValue(Params.METADATA_PROFILE_ID);
                        }
                        if (!line.hasOption(Params.METADATA_FILE)) {
                            System.err.println(String.format("Chyba: pro akci %s je parametr --%s povinný!", action, Params.METADATA_FILE));
                            printHelp(options);
                            return EXIT_USAGE_ERROR;
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
                            return EXIT_USAGE_ERROR;
                        }
                    } catch (NumberFormatException e) {
                        System.err.println(String.format("Chyba: hodnota parametru --%s musí být celé kladné číslo!", Params.QUIT_AFTER_NTH_INVALID_PSP));
                        printHelp(options);
                        return EXIT_USAGE_ERROR;
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
                String preferDmfAdiVersion = null;
                if (line.hasOption(Params.PREFERRED_DMF_ADI_VERSION)) {
                    preferDmfAdiVersion = line.getOptionValue(Params.PREFERRED_DMF_ADI_VERSION);
                }
                String preferDmfAdnVersion = null;
                if (line.hasOption(Params.PREFERRED_DMF_ADN_VERSION)) {
                    preferDmfAdnVersion = line.getOptionValue(Params.PREFERRED_DMF_ADN_VERSION);
                }
                String preferredDmfDadVersion = null;
                if (line.hasOption(Params.PREFERRED_DMF_DAD_VERSION)) {
                    preferredDmfDadVersion = line.getOptionValue(Params.PREFERRED_DMF_DAD_VERSION);
                }
                String preferredDmfFduVersion = null;
                if (line.hasOption(Params.PREFERRED_DMF_FDU_VERSION)) {
                    preferredDmfFduVersion = line.getOptionValue(Params.PREFERRED_DMF_FDU_VERSION);
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
                String forceDmfAdiVersion = null;
                if (line.hasOption(Params.FORCED_DMF_ADI_VERSION)) {
                    forceDmfAdiVersion = line.getOptionValue(Params.FORCED_DMF_ADI_VERSION);
                }
                String forceDmfAdnVersion = null;
                if (line.hasOption(Params.FORCED_DMF_ADN_VERSION)) {
                    forceDmfAdnVersion = line.getOptionValue(Params.FORCED_DMF_ADN_VERSION);
                }
                String forcedDmfDadVersion = null;
                if (line.hasOption(Params.FORCED_DMF_DAD_VERSION)) {
                    forcedDmfDadVersion = line.getOptionValue(Params.FORCED_DMF_DAD_VERSION);
                }
                String forcedDmfFduVersion = null;
                if (line.hasOption(Params.FORCED_DMF_FDU_VERSION)) {
                    forcedDmfFduVersion = line.getOptionValue(Params.FORCED_DMF_FDU_VERSION);
                }
                //e-publikace: hlavni volba ma prednost pred zastaralym aliasem z VEP
                String preferredDmfEmoVersion = optionValueOrAlias(line, Params.PREFERRED_DMF_EMO_VERSION, Params.PREFERRED_DMF_EMON_VERSION_DEPRECATED);
                String preferredDmfEpeVersion = optionValueOrAlias(line, Params.PREFERRED_DMF_EPE_VERSION, Params.PREFERRED_DMF_EPER_VERSION_DEPRECATED);
                String preferredDmfSepVersion = optionValueOrAlias(line, Params.PREFERRED_DMF_SEP_VERSION, Params.PREFERRED_DMF_COMPER_VERSION_DEPRECATED);
                String forcedDmfEmoVersion = optionValueOrAlias(line, Params.FORCED_DMF_EMO_VERSION, Params.FORCED_DMF_EMON_VERSION_DEPRECATED);
                String forcedDmfEpeVersion = optionValueOrAlias(line, Params.FORCED_DMF_EPE_VERSION, Params.FORCED_DMF_EPER_VERSION_DEPRECATED);
                String forcedDmfSepVersion = optionValueOrAlias(line, Params.FORCED_DMF_SEP_VERSION, Params.FORCED_DMF_COMPER_VERSION_DEPRECATED);

                //Resolver URN:NBN
                if (line.hasOption(Params.URNNBN_RESOLVER_URL)) {
                    try {
                        UrnNbnResolverChecker.setResolverBaseUrl(line.getOptionValue(Params.URNNBN_RESOLVER_URL),
                                line.hasOption(Params.URNNBN_RESOLVER_ALLOW_HTTP));
                    } catch (IllegalArgumentException e) {
                        System.err.println(String.format("Chyba: %s!", e.getMessage()));
                        printHelp(options);
                        return EXIT_USAGE_ERROR;
                    }
                }

                //verbosity
                Integer verbosity = DEFAULT_VERBOSITY;
                if (line.hasOption(Params.VERBOSITY)) {
                    try {
                        verbosity = Integer.valueOf(line.getOptionValue(Params.VERBOSITY));
                        if (verbosity < 0 || verbosity > 3) {
                            System.err.println(String.format("Chyba: hodnota parametru --%s musí být číslo (0-3)!", Params.VERBOSITY));
                            printHelp(options);
                            return EXIT_USAGE_ERROR;
                        }
                    } catch (NumberFormatException e) {
                        System.err.println(String.format("Chyba: hodnota parametru --%s musí být číslo (0-3)!", Params.VERBOSITY));
                        printHelp(options);
                        return EXIT_USAGE_ERROR;
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
                if (line.hasOption(Params.DISABLE_VERAPDF)) {
                    utilsDisabled.add(ExternalUtil.VERAPDF);
                } else {
                    if (line.hasOption(Params.VERAPDF_PATH)) {
                        utilsPaths.put(ExternalUtil.VERAPDF, new File(line.getOptionValue(Params.VERAPDF_PATH)));
                    }
                }
                if (line.hasOption(Params.DISABLE_EPUBCHECK)) {
                    utilsDisabled.add(ExternalUtil.EPUBCHECK);
                } else {
                    if (line.hasOption(Params.EPUBCHECK_PATH)) {
                        utilsPaths.put(ExternalUtil.EPUBCHECK, new File(line.getOptionValue(Params.EPUBCHECK_PATH)));
                    }
                }

                DmfDetector.Params dmfDetectorParams = new DmfDetector.Params();
                dmfDetectorParams.forcedDmfMonVersion = forceDmfMonVersion;
                dmfDetectorParams.forcedDmfPerVersion = forceDmfPerVersion;
                dmfDetectorParams.forcedDmfAdgVersion = forceDmfAdgVersion;
                dmfDetectorParams.forcedDmfAdfVersion = forceDmfAdfVersion;
                dmfDetectorParams.forcedDmfAdiVersion = forceDmfAdiVersion;
                dmfDetectorParams.forcedDmfAdnVersion = forceDmfAdnVersion;
                dmfDetectorParams.forcedDmfDadVersion = forcedDmfDadVersion;
                dmfDetectorParams.forcedDmfFduVersion = forcedDmfFduVersion;
                dmfDetectorParams.forcedDmfEmoVersion = forcedDmfEmoVersion;
                dmfDetectorParams.forcedDmfEpeVersion = forcedDmfEpeVersion;
                dmfDetectorParams.forcedDmfSepVersion = forcedDmfSepVersion;
                dmfDetectorParams.preferredDmfMonVersion = preferDmfMonVersion;
                dmfDetectorParams.preferredDmfPerVersion = preferDmfPerVersion;
                dmfDetectorParams.preferredDmfAdgVersion = preferDmfAdgVersion;
                dmfDetectorParams.preferredDmfAdfVersion = preferDmfAdfVersion;
                dmfDetectorParams.preferredDmfAdiVersion = preferDmfAdiVersion;
                dmfDetectorParams.preferredDmfAdnVersion = preferDmfAdnVersion;
                dmfDetectorParams.preferredDmfDadVersion = preferredDmfDadVersion;
                dmfDetectorParams.preferredDmfFduVersion = preferredDmfFduVersion;
                dmfDetectorParams.preferredDmfEmoVersion = preferredDmfEmoVersion;
                dmfDetectorParams.preferredDmfEpeVersion = preferredDmfEpeVersion;
                dmfDetectorParams.preferredDmfSepVersion = preferredDmfSepVersion;

                PrintStream out = System.out;
                PrintStream err = System.err;

                switch (action) {
                    case VALIDATE_PSP:
                        return validatePsp(psp,
                                configDir, tmpDir,
                                verbosity, out, err, xmlProtocolDir, xmlProtocolFile,
                                dmfDetectorParams,
                                utilsPaths, utilsDisabled,
                                devParams);
                    case VALIDATE_PSP_GROUP:
                        return validatePspGroup(pspGroup,
                                configDir, tmpDir,
                                verbosity, out, err, xmlProtocolDir,
                                dmfDetectorParams,
                                utilsPaths, utilsDisabled,
                                devParams);
                    case BUILD_MINIFIED_PACKAGE:
                        //TODO: level validace do CLI
                        return buildMinifiedPackage(psp, minifiedPspDir, 3);
                    case VALIDATE_METADATA_BY_PROFILE:
                        long startTime = System.currentTimeMillis();
                        ValidationResult result = validateMetadataByProfile(configDir, metadataProfileId, metadataFile);
                        long finishTime = System.currentTimeMillis();
                        if (xmlProtocolFile == null) {
                            if (xmlProtocolDir != null) {
                                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss");
                                String filename = String.format("%s_%s.xml", simpleDateFormat.format(new Date(System.currentTimeMillis())), metadataFile.getName());
                                xmlProtocolFile = new File(xmlProtocolDir, filename);
                            }
                        }
                        if (xmlProtocolFile != null) {
                            System.out.println("Ukládám xml protokol do souboru: " + xmlProtocolFile.getAbsolutePath());
                            try {
                                new ValidatorProtocolXmlBuilder().buildProfileValidationXmlOutput(xmlProtocolFile, metadataFile, metadataProfileId, result, startTime, finishTime);
                            } catch (IOException e) {
                                System.err.println("ERROR: " + e.getMessage());
                            }
                        }
                        if (result != null) {
                            if (result.hasProblems()) {
                                result.getProblems().forEach(p -> System.out.println(p.getLevel() + ": " + p.getFullMessage()));
                            } else {
                                System.out.println("Nenalezeny žádné problémy po validaci souboru " + metadataFile.getName() + " oproti profilu " + metadataProfileId);
                            }
                        } else {
                            //nevalidni identifikator profilu (hlaska je uz vypsana), nic se nevalidovalo
                            return EXIT_USAGE_ERROR;
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
            return EXIT_USAGE_ERROR;
        } catch (XmlFileParsingException e) {
            //konfigurace se nacita jen pres ValidatorConfigurationException, tady jde vzdy o XML balíku
            System.err.println("Chyba:" + e.getMessage());
            return EXIT_PSP_NOT_VALIDATED;
        } catch (ValidatorConfigurationException e) {
            System.err.println("Chyba:" + e.getMessage());
            return EXIT_CONFIG_ERROR;
        } catch (PspDataException e) {
            System.err.println("Chyba:" + e.getMessage());
            return EXIT_PSP_NOT_VALIDATED;
        } catch (FdmfRegistry.UnknownFdmfException e) {
            System.err.println("Chyba:" + e.getMessage());
            return EXIT_PSP_NOT_VALIDATED;
        } catch (PspNotValidatedException e) {
            System.err.println(e.getMessage());
            return EXIT_PSP_NOT_VALIDATED;
        } catch (InvalidXPathExpressionException e) {
            System.err.println("Chyba:" + e.getMessage());
            return EXIT_PSP_NOT_VALIDATED;
        }
        return EXIT_OK;
    }

    private static ValidationResult validateMetadataByProfile(File validatorConfigDir, String profileId, File metadataFile) throws ValidatorConfigurationException {
        // tato akce nenačítá celou konfiguraci přes ValidatorConfigurationManager, manifest se proto kontroluje tady
        nkp.pspValidator.shared.ConfigManifest.check(validatorConfigDir);
        try {
            System.out.println("Validuji oproti profilu " + profileId + " soubor " + metadataFile);
            String[] profileTokens = profileId.split(":");
            String dmf = profileTokens[0];
            DictionaryManager dm = new DictionaryManager(new File(validatorConfigDir, "dictionaries"));
            MetadataProfileParser metadataProfileParser = new MetadataProfileParser(dm);
            if (profileTokens.length == 1) {
                System.out.println("Chybí typ profilu (fDmf=" + profileTokens[0] + ")");
                return null;
            }
            switch (profileTokens[1]) {
                case "biblio": {
                    if (profileTokens.length == 2) {
                        System.out.println("Chybí metadatový formát (fDmf=" + profileTokens[0] + ", typ=" + profileTokens[1] + ")");
                        return null;
                    }
                    String metadataFormatStr = profileTokens[2];
                    MetadataFormat metadataFormat = null;
                    if (metadataFormatStr.equals("dc")) {
                        metadataFormat = MetadataFormat.DC;
                    } else if (metadataFormatStr.equals("mods")) {
                        metadataFormat = MetadataFormat.MODS;
                    } else {
                        System.out.println("Neznámý metadatový formát: " + metadataFormatStr + " (fDmf=" + profileTokens[0] + ", typ=" + profileTokens[1] + ")");
                        return null;
                    }
                    if (profileTokens.length == 3) {
                        System.out.println("Chybí název profilu (fDmf=" + profileTokens[0] + ", typ=" + profileTokens[1] + ", formát=" + profileTokens[2] + ")");
                        return null;
                    }
                    String profileName = profileTokens[3];
                    File profileFile = new File(validatorConfigDir, "fDMF/" + dmf + "/biblioProfiles/" + metadataFormatStr + "/" + profileName + ".xml");
                    if (!profileFile.exists()) {
                        System.out.println("Soubor profilu neexistuje: " + profileFile.getName() + " (fDmf=" + profileTokens[0] + ", typ=" + profileTokens[1] + ", formát=" + profileTokens[2] + ")");
                        return null;
                    }
                    MetadataProfile metadataProfile = metadataProfileParser.parseProfile(profileFile);
                    Document metadataDoc = XmlUtils.buildDocumentFromFile(metadataFile, true);
                    ValidationResult result = new ValidationResult();
                    MetadataProfileValidator.validate(metadataProfile, metadataFile, metadataDoc, result, null);
                    return result;
                }
                case "tech": {
                    if (profileTokens.length == 2) {
                        System.out.println("Chybí název profilu (fDmf=" + profileTokens[0] + ", typ=" + profileTokens[1] + ")");
                        return null;
                    }
                    String profileName = profileTokens[2];
                    File profileFile = new File(validatorConfigDir, "fDMF/" + dmf + "/techProfiles/" + profileName + ".xml");
                    if (!profileFile.exists()) {
                        System.out.println("Soubor profilu neexistuje: " + profileFile.getName() + " (fDmf=" + profileTokens[0] + ", typ=" + profileTokens[1] + ")");
                        return null;
                    }
                    MetadataProfile metadataProfile = metadataProfileParser.parseProfile(profileFile);
                    Document metadataDoc = XmlUtils.buildDocumentFromFile(metadataFile, true);
                    ValidationResult result = new ValidationResult();
                    MetadataProfileValidator.validate(metadataProfile, metadataFile, metadataDoc, result, null);
                    return result;
                }
                default:
                    System.out.println("Neznámý typ profilu: " + profileTokens[1] + " (fDmf=" + profileTokens[0] + ")");
                    return null;
            }
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

    /** @return {@link #EXIT_OK}, {@link #EXIT_USAGE_ERROR} (nepoužitelný cílový adresář) nebo {@link #EXIT_PSP_NOT_VALIDATED} (balík nejde zpracovat) */
    private static int buildMinifiedPackage(File pspDirOrZipFile, File minifiedPspDir, int level) {
        System.out.println("Vyrábím minifikovaný psp balík ze zdrojového balíku " + pspDirOrZipFile.getAbsolutePath());
        System.out.println("Výsledek bude uložen do adresáře " + minifiedPspDir.getAbsolutePath());
        //checkou minifiedPspDir
        if (!minifiedPspDir.exists()) {
            System.err.println(String.format("Chyba: adresář %s neexistuje!", minifiedPspDir.getAbsolutePath()));
            return EXIT_USAGE_ERROR;
        } else if (!minifiedPspDir.isDirectory()) {
            System.err.println(String.format("Chyba: soubor %s není adresář!", minifiedPspDir.getAbsolutePath()));
            return EXIT_USAGE_ERROR;
        } else if (!minifiedPspDir.canWrite()) {
            System.err.println(String.format("Chyba: nemůžu zapisovat do adresáře %s!", minifiedPspDir.getAbsolutePath()));
            return EXIT_USAGE_ERROR;
        } else if (!pspDirOrZipFile.exists()) {
            throw new IllegalStateException(String.format("Soubor %s neexistuje", pspDirOrZipFile.getAbsolutePath()));
        } else {
            try {
                if (pspDirOrZipFile.isDirectory()) {
                    if (!pspDirOrZipFile.canRead()) {
                        throw new IllegalStateException(String.format("Nelze číst adresář %s", pspDirOrZipFile.getAbsolutePath()));
                    }
                    minifyPspDir(pspDirOrZipFile, minifiedPspDir, level);
                } else if (!minifyPspZip(pspDirOrZipFile, minifiedPspDir, minifiedPspDir, level)) {
                    return EXIT_PSP_NOT_VALIDATED;
                }
            } catch (IOException e) {
                throw new IllegalStateException("Chyba při minifikaci ", e);
            }
        }
        return EXIT_OK;
    }

    /** @return {@code true}, pokud se balík zminifikoval */
    private static boolean minifyPspZip(File pspZipFile, File tmpRootDir, File minifiedRootDir, int level) throws IOException {
        try {
            try {
                new ZipFile(pspZipFile);
            } catch (ZipException e) {
                System.out.println(String.format("Soubor %s není adresář ani soubor ZIP, ignoruji.", pspZipFile.getAbsolutePath()));
                return false;
            }
            if (tmpRootDir == null) {
                System.err.println(String.format("Chyba: prázdný parametr --%s: adresář pro dočasné soubory je potřeba pro rozbalení ZIP souboru %s!", Params.TMP_DIR, pspZipFile.getAbsolutePath()));
                return false;
            } else if (!tmpRootDir.exists()) {
                System.err.println(String.format("Chyba: adresář %s neexistuje!", pspZipFile.getAbsolutePath()));
                return false;
            } else if (!tmpRootDir.isDirectory()) {
                System.err.println(String.format("Chyba: soubor %s není adresář!", pspZipFile.getAbsolutePath()));
                return false;
            } else if (!tmpRootDir.canWrite()) {
                System.err.println(String.format("Chyba: nemůžu zapisovat do adresáře %s!", pspZipFile.getAbsolutePath()));
                return false;
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
            return false;
        }
        return true;
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
    /**
     * Hodnota volby, pripadne jejiho zastaraleho aliasu (volby Validatoru e-publikaci). Hlavni volba ma prednost.
     */
    private static String optionValueOrAlias(CommandLine line, String option, String deprecatedAlias) {
        if (line.hasOption(option)) {
            return line.getOptionValue(option);
        } else if (line.hasOption(deprecatedAlias)) {
            return line.getOptionValue(deprecatedAlias);
        } else {
            return null;
        }
    }

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
        replacements.put("ý", "y");
        replacements.put("Ý", "Y");
        // pomlčka: nápověda se vypisuje ve výchozím kódování systému, na Windows by z ní vznikl nečitelný znak
        replacements.put("–", "-");

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
                "Typ DMF se odvozuje z dat jednotlivých PSP balíků. Verze DMF použité pro validaci lze ovlivnit" +
                " parametry --preferred-dmf-<typ>-version (preferovaná verze, použije se, když data balíku" +
                " neobsahují informaci o vhodné verzi) a --forced-dmf-<typ>-version (vynucená verze)," +
                " kde <typ> je zkratka typu:\n" +
                "  mon: Monografie\n" +
                "  per: Periodikum\n" +
                "  adg: Zvuk – gramodeska\n" +
                "  adf: Zvuk – fonoválec\n" +
                "  adi: Zvuk – disk\n" +
                "  adn: Zvuk – bez nosiče\n" +
                "  dad: Datové disky\n" +
                "  fdu: Jednotka fondu\n" +
                "  emo: E-monografie\n" +
                "  epe: E-periodikum\n" +
                "  sep: Skládané e-periodikum\n" +
                "Zastaralé aliasy emon, eper a comper (z Validátoru e-publikací) jsou přijímány," +
                " nahrazují zkratky emo, epe a sep.\n" +
                " Dále je potřeba pomocí --config-dir uvést adresář, který obsahuje definice fDMF," +
                " např. monograph_2.1, periodical_2.0, audio_gram_0.5 nebo emonograph_3.1.\n" +
                "\n" +
                "Výroba minifikovaného balíku:\n" +
                "----------------------------\n" +
                "Bude vyrobena kopie balíku s tím, že textové a obrazové soubory budou nahrazny prázdnými soubory se stejným jménem." +
                "\n\n" +
                "Návratové kódy:\n" +
                "--------------\n" +
                "  0: validace proběhla a protokol vznikl (balík může být validní i nevalidní, verdikt je ve výpisu a v protokolu)," +
                " nebo byla vyžádána nápověda či verze\n" +
                "  1: balík (u skupiny aspoň jeden balík) se nezvalidoval, např. pro jeho typ a verzi DMF není fDMF, balík neexistuje" +
                " nebo nejde přečíst či rozbalit\n" +
                "  2: chyba použití, tj. chybějící nebo neznámý parametr či akce, chybná hodnota parametru\n" +
                "  3: chyba konfigurace validátoru (adresář --config-dir neexistuje, chybí v něm manifest.properties," +
                " má jinou hlavní verzi nebo je vadný)\n" +
                "\n");
        String footer = replaceUmlaut("\n*Definice metadatových formátů. Více na https://standardy.ndk.cz/.\n" +
                "Více informací o validátoru najdete na https://github.com/NLCR/komplexni-validator.");
        HelpFormatter formatter = new HelpFormatter();
        formatter.setWidth(100);
        formatter.setOptionComparator(null);
        formatter.printHelp("java -jar KomplexniValidatorCLI.jar", header, options, footer, true);
    }

    /** Konfigurace jiné vedlejší verze než aplikace (3.0 pro 3.1 a naopak) není chyba: jen upozornění, validace pokračuje. */
    private static void printConfigManifestWarning(PrintStream out, ValidatorConfigurationManager validatorConfigManager) {
        ConfigManifest.Result manifest = validatorConfigManager.getManifestResult();
        if (manifest.getStatus() == ConfigManifest.Status.OLDER_MINOR || manifest.getStatus() == ConfigManifest.Status.NEWER_MINOR) {
            out.println("Upozornění: " + manifest.getMessage());
        }
    }

    /** Výsledek zpracování jednoho balíku (adresáře nebo ZIPu). */
    private enum PspOutcome {
        /** Validace proběhla a protokol vznikl (pokud byl požadován). */
        VALIDATED,
        /** Soubor, který není adresář ani ZIP, se přeskočil (ve skupině balíků to není chyba). */
        IGNORED,
        /** Balík se nezvalidoval. */
        NOT_VALIDATED
    }

    /** @return {@link #EXIT_OK}, nebo {@link #EXIT_PSP_NOT_VALIDATED}, pokud se aspoň jeden balík skupiny nezvalidoval */
    private static int validatePspGroup(File pspGroup,
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
        printConfigManifestWarning(out, validatorConfigManager);
        ExternalUtilManager externalUtilManager = new ExternalUtilManagerFactory(validatorConfigManager.getExternalUtilsConfigFile()).buildExternalUtilManager(platform.getOperatingSystem());
        externalUtilManager.setPaths(utilsPaths);
        detectImageTools(out, externalUtilManager, utilsDisabled);
        //konfigurace, detekce nastroju a registr fDMF jsou spolecne pro vsechny balicky skupiny (issue #166)
        FdmfRegistry fdmfRegistry = new FdmfRegistry(validatorConfigManager);


        //pspDirOrZipFile dir or zip file?
        if (!pspGroup.exists()) {
            throw new IllegalStateException(String.format("Soubor %s neexistuje", pspGroup.getAbsolutePath()));
        } else {
            if (pspGroup.isDirectory()) {
                if (!pspGroup.canRead()) {
                    throw new IllegalStateException(String.format("Nelze číst adresář %s", pspGroup.getAbsolutePath()));
                }
                return exitCode(validatePspGroupDir(pspGroup,
                        tmpDir,
                        externalUtilManager, validatorConfigManager, fdmfRegistry,
                        verbosity, out, err, xmlProtocolDir,
                        dmfDetectorParams,
                        devParams));
            } else {
                return exitCode(validatePspGroupZip(pspGroup,
                        tmpDir,
                        externalUtilManager, validatorConfigManager, fdmfRegistry,
                        verbosity, out, err, xmlProtocolDir,
                        dmfDetectorParams,
                        devParams));
            }
        }
    }

    private static int exitCode(boolean allValidated) {
        return allValidated ? EXIT_OK : EXIT_PSP_NOT_VALIDATED;
    }

    /** @return {@code false}, pokud se nějaký balík nezvalidoval */
    private static boolean validatePspGroupZip(File pspGroupFile,
                                            File tmpDir,
                                            ExternalUtilManager externalUtilManager, ValidatorConfigurationManager validatorConfigManager,
                                            FdmfRegistry fdmfRegistry,
                                            Integer verbosity, PrintStream out, PrintStream err,
                                            File xmlProtocolDir,
                                            DmfDetector.Params dmfDetectorParams,
                                            Validator.DevParams devParams) throws PspDataException, XmlFileParsingException, FdmfRegistry.UnknownFdmfException, ValidatorConfigurationException, InvalidXPathExpressionException {
        try {
            try {
                new ZipFile(pspGroupFile);
            } catch (ZipException e) {
                out.println(String.format("Soubor %s není adresář ani soubor ZIP, ignoruji.", pspGroupFile.getAbsolutePath()));
                return false;
            }
            if (tmpDir == null) {
                err.println(String.format("Chyba: prázdný parametr --%s: adresář pro dočasné soubory je potřeba pro rozbalení ZIP souboru %s!", Params.TMP_DIR, pspGroupFile.getAbsolutePath()));
                return false;
            } else if (!tmpDir.exists()) {
                err.println(String.format("Chyba: adresář %s neexistuje!", tmpDir.getAbsolutePath()));
                return false;
            } else if (!tmpDir.isDirectory()) {
                err.println(String.format("Chyba: soubor %s není adresář!", tmpDir.getAbsolutePath()));
                return false;
            } else if (!tmpDir.canWrite()) {
                err.println(String.format("Chyba: nemůžu zapisovat do adresáře %s!", tmpDir.getAbsolutePath()));
                return false;
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
                    return validatePspGroupDir(filesInContainer[0],
                            tmpDir,
                            externalUtilManager, validatorConfigManager, fdmfRegistry,
                            verbosity, out, err, xmlProtocolDir,
                            dmfDetectorParams,
                            devParams);
                } else {
                    return validatePspGroupDir(containerDir,
                            tmpDir,
                            externalUtilManager, validatorConfigManager, fdmfRegistry,
                            verbosity, out, err, xmlProtocolDir,
                            dmfDetectorParams,
                            devParams);
                }
            }
        } catch (IOException e) {
            out.println(String.format("Chyba zpracování ZIP souboru %s: %s!", pspGroupFile.getAbsolutePath(), e.getMessage()));
            return false;
        }
    }

    /** @return {@code false}, pokud se nějaký balík nezvalidoval (ostatní se zvalidují dál) */
    private static boolean validatePspGroupDir(File pspGroupDir,
                                            File tmpDir,
                                            ExternalUtilManager externalUtilManager, ValidatorConfigurationManager validatorConfigManager,
                                            FdmfRegistry fdmfRegistry,
                                            Integer verbosity, PrintStream out, PrintStream err,
                                            File xmlProtocolDir,
                                            DmfDetector.Params dmfDetectorParams,
                                            Validator.DevParams devParams) throws XmlFileParsingException, FdmfRegistry.UnknownFdmfException, PspDataException, ValidatorConfigurationException, InvalidXPathExpressionException {
        boolean allValidated = true;
        for (File pspDirOrZip : pspGroupDir.listFiles()) {
            //TODO: pocitat nevalidni baliky
            PspOutcome outcome = validatePspDirOrZip(pspDirOrZip,
                    tmpDir,
                    externalUtilManager, validatorConfigManager, fdmfRegistry,
                    verbosity, out, err,
                    xmlProtocolDir, null,
                    dmfDetectorParams,
                    devParams);
            if (outcome == PspOutcome.NOT_VALIDATED) {
                allValidated = false;
            }
        }
        return allValidated;
    }

    /** @return {@link #EXIT_OK}, nebo {@link #EXIT_PSP_NOT_VALIDATED}, pokud se balík nezvalidoval */
    private static int validatePsp(File pspDirOrZipFile,
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
        printConfigManifestWarning(out, validatorConfigManager);
        ExternalUtilManager externalUtilManager = new ExternalUtilManagerFactory(validatorConfigManager.getExternalUtilsConfigFile()).buildExternalUtilManager(platform.getOperatingSystem());
        externalUtilManager.setPaths(utilsPaths);
        detectImageTools(out, externalUtilManager, utilsDisabled);
        FdmfRegistry fdmfRegistry = new FdmfRegistry(validatorConfigManager);

        PspOutcome outcome = validatePspDirOrZip(pspDirOrZipFile,
                tmpDir,
                externalUtilManager, validatorConfigManager, fdmfRegistry,
                verbosity, out, err,
                xmlProtocolDir, xmlProtocolFile,
                dmfDetectorParams,
                devParams);
        return outcome == PspOutcome.VALIDATED ? EXIT_OK : EXIT_PSP_NOT_VALIDATED;
    }

    /**
     * Validace jednoho balicku (adresar nebo ZIP) s uz inicializovanou konfiguraci, nastroji a registrem fDMF.
     */
    private static PspOutcome validatePspDirOrZip(File pspDirOrZipFile,
                                            File tmpDir,
                                            ExternalUtilManager externalUtilManager, ValidatorConfigurationManager validatorConfigManager,
                                            FdmfRegistry fdmfRegistry,
                                            Integer verbosity, PrintStream out, PrintStream err,
                                            File xmlProtocolDir, File xmlProtocolFile,
                                            DmfDetector.Params dmfDetectorParams,
                                            Validator.DevParams devParams) throws ValidatorConfigurationException, FdmfRegistry.UnknownFdmfException, PspDataException, InvalidXPathExpressionException, XmlFileParsingException {
        //pspDirOrZipFile dir or zip file?
        if (!pspDirOrZipFile.exists()) {
            throw new IllegalStateException(String.format("Soubor %s neexistuje", pspDirOrZipFile.getAbsolutePath()));
        } else {
            if (pspDirOrZipFile.isDirectory()) {
                if (!pspDirOrZipFile.canRead()) {
                    throw new IllegalStateException(String.format("Nelze číst adresář %s", pspDirOrZipFile.getAbsolutePath()));
                }
                return validatePspDir(pspDirOrZipFile,
                        externalUtilManager, validatorConfigManager, fdmfRegistry,
                        out, verbosity, xmlProtocolDir, xmlProtocolFile,
                        dmfDetectorParams,
                        devParams);
            } else {
                return validatePspZip(pspDirOrZipFile,
                        tmpDir,
                        externalUtilManager, validatorConfigManager, fdmfRegistry,
                        out, err, verbosity, xmlProtocolDir, xmlProtocolFile,
                        dmfDetectorParams,
                        devParams);
            }
        }
    }

    private static PspOutcome validatePspZip(File pspZipFile,
                                       File tmpDir,
                                       ExternalUtilManager externalUtilManager, ValidatorConfigurationManager validatorConfigManager,
                                       FdmfRegistry fdmfRegistry,
                                       PrintStream out, PrintStream err, Integer verbosity,
                                       File xmlProtocolDir, File xmlProtocolFile,
                                       DmfDetector.Params dmfDetectorParams,
                                       Validator.DevParams devParams) throws XmlFileParsingException, FdmfRegistry.UnknownFdmfException, PspDataException, ValidatorConfigurationException, InvalidXPathExpressionException {
        try {
            try {
                new ZipFile(pspZipFile);
            } catch (ZipException e) {
                out.println(String.format("Soubor %s není adresář ani soubor ZIP, ignoruji.", pspZipFile.getAbsolutePath()));
                return PspOutcome.IGNORED;
            }
            if (tmpDir == null) {
                err.println(String.format("Chyba: prázdný parametr --%s: adresář pro dočasné soubory je potřeba pro rozbalení ZIP souboru %s!", Params.TMP_DIR, pspZipFile.getAbsolutePath()));
                return PspOutcome.NOT_VALIDATED;
            } else if (!tmpDir.exists()) {
                err.println(String.format("Chyba: adresář %s neexistuje!", pspZipFile.getAbsolutePath()));
                return PspOutcome.NOT_VALIDATED;
            } else if (!tmpDir.isDirectory()) {
                err.println(String.format("Chyba: soubor %s není adresář!", pspZipFile.getAbsolutePath()));
                return PspOutcome.NOT_VALIDATED;
            } else if (!tmpDir.canWrite()) {
                err.println(String.format("Chyba: nemůžu zapisovat do adresáře %s!", pspZipFile.getAbsolutePath()));
                return PspOutcome.NOT_VALIDATED;
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
                    return validatePspDir(filesInContainer[0],
                            externalUtilManager, validatorConfigManager, fdmfRegistry,
                            out, verbosity, xmlProtocolDir, xmlProtocolFile,
                            dmfDetectorParams,
                            devParams);
                } else {
                    return validatePspDir(containerDir,
                            externalUtilManager, validatorConfigManager, fdmfRegistry,
                            out, verbosity, xmlProtocolDir, xmlProtocolFile,
                            dmfDetectorParams,
                            devParams);
                }
            }
        } catch (IOException e) {
            out.println(String.format("Chyba zpracování ZIP souboru %s: %s!", pspZipFile.getAbsolutePath(), e.getMessage()));
            return PspOutcome.NOT_VALIDATED;
        }
    }

    private static PspOutcome validatePspDir(File pspDir,
                                       ExternalUtilManager externalUtilManager, ValidatorConfigurationManager validatorConfigManager,
                                       FdmfRegistry fdmfRegistry,
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
        FdmfConfiguration fdmfConfig;
        try {
            fdmfConfig = fdmfRegistry.getFdmfConfig(dmfResolved);
        } catch (FdmfRegistry.UnknownFdmfException e) {
            throw new PspNotValidatedException(unknownFdmfMessage(dmfResolved, fdmfRegistry, dmfDetectorParams));
        }
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
        validator.run(pspDir, xmlProtocolFile, out, verbosity, devParams, null, progressListener, null, dmfResolved);
        //protokol je pozadovan, ale nevznikl (napr. nezapisovatelny adresar): hlasku uz vypsal validator na stderr
        if (xmlProtocolFile != null && !xmlProtocolFile.isFile()) {
            return PspOutcome.NOT_VALIDATED;
        }
        return PspOutcome.VALIDATED;
    }


    /**
     * Zjisti dostupnost a verzi nastroju a vypise je. Radky "Kontroluji dostupnost nastroje X: nalezen, verze: ..."
     * zustavaji beze zmeny (muze je cist webovy validator); u neoverene nebo nerozpoznane verze a u verze overene
     * na jine platforme (Linux podle macOS) nasleduje dalsi radek s poznamkou odsazeny ctyrmi mezerami
     * ({@link VersionCheck#cliNote()}), overena verze poznamku nema.
     */
    static void detectImageTools(PrintStream out, ExternalUtilManager externalUtilManager, Set<ExternalUtil> utilsDisabled) {
        for (ExternalUtil util : ExternalUtil.values()) {
            if (utilsDisabled.contains(util)) {
                out.println(String.format("Vypnuto použití nástroje %s.", util.getUserFriendlyName()));
                externalUtilManager.setUtilDisabled(util);
            } else {
                out.print(String.format("Kontroluji dostupnost nástroje %s: ", util.getUserFriendlyName()));
                //po neuspesne detekci se nastroj oznaci jako nedostupny, validace pak hlasi jednu INFO hlasku
                //"nástroj ... není dostupný" misto chyby spusteni u kazdeho souboru (stejne jako GUI)
                if (!externalUtilManager.isVersionDetectionDefined(util)) {
                    externalUtilManager.setUtilAvailable(util, false);
                    out.println("není definován způsob detekce verze");
                } else {
                    try {
                        String version = externalUtilManager.runUtilVersionDetection(util);
                        if (version != null) {
                            externalUtilManager.setUtilAvailable(util, true);
                            externalUtilManager.setUtilVersion(util, version);
                            out.println("nalezen, verze: " + version);
                            String note = externalUtilManager.checkVersion(util, version).cliNote();
                            if (note != null) {
                                out.println("    " + note);
                            }
                        } else {
                            externalUtilManager.setUtilAvailable(util, false);
                            out.println("nenalezen");
                        }
                    } catch (CliCommand.CliCommandException e) {
                        externalUtilManager.setUtilAvailable(util, false);
                        out.println(String.format("nenalezen (%s)", e.getMessage()));
                    }
                }
            }
        }
    }

    /**
     * Balík se nezvalidoval (pro zjištěný typ a verzi DMF není fDMF), hláška je určená uživateli. CLI skončí kódem
     * {@link #EXIT_PSP_NOT_VALIDATED}.
     */
    static class PspNotValidatedException extends RuntimeException {
        PspNotValidatedException(String message) {
            super(message);
        }
    }

    /**
     * Srozumitelná hláška k balíku, pro jehož typ a verzi DMF není definovaná fDMF: zjištěná (nebo vynucená) verze,
     * podporované verze daného typu a volba, kterou lze verzi vynutit.
     */
    static String unknownFdmfMessage(Dmf dmf, FdmfRegistry registry, DmfDetector.Params params) {
        String code = dmfTypeOptionCode(dmf.getType());
        String forcedOption = "--forced-dmf-" + code + "-version";
        boolean forced = dmf.getVersion() != null && dmf.getVersion().equals(forcedVersion(dmf.getType(), params));
        StringBuilder message = new StringBuilder();
        message.append(String.format("Chyba: balík nebyl zvalidován a protokol nevznikl. Pro %s verzi standardu %s není definována fDMF (formalizovaná DMF).",
                forced ? "vynucenou (" + forcedOption + " " + dmf.getVersion() + ")" : "zjištěnou", dmf));
        message.append(System.lineSeparator());
        message.append(String.format("Podporované verze (%s): %s.", dmf.getType(), String.join(", ", sortVersions(supportedVersions(dmf.getType(), registry)))));
        message.append(System.lineSeparator());
        if (forced) {
            message.append(String.format("Zadejte ve volbě %s jednu z podporovaných verzí.", forcedOption));
        } else {
            message.append(String.format("Pokud balík odpovídá jiné verzi, lze ji vynutit volbou %s VERZE (verzi v balíku zjišťuje validátor z info.xml, element metadataversion).", forcedOption));
        }
        return message.toString();
    }

    /** Zkratka typu ve volbách --forced-dmf-…-version a --preferred-dmf-…-version (viz {@link Params}). */
    static String dmfTypeOptionCode(Dmf.Type type) {
        switch (type) {
            case MONOGRAPH:
                return "mon";
            case PERIODICAL:
                return "per";
            case AUDIO_GRAM:
                return "adg";
            case AUDIO_FONO:
                return "adf";
            case AUDIO_DISC:
                return "adi";
            case AUDIO_NO_CARRIER:
                return "adn";
            case DATA_DISC:
                return "dad";
            case FUND_UNIT:
                return "fdu";
            case EMONOGRAPH:
                return "emo";
            case EPERIODICAL:
                return "epe";
            case COMPERIODICAL:
                return "sep";
            default:
                throw new IllegalStateException("neznámý typ " + type);
        }
    }

    private static String forcedVersion(Dmf.Type type, DmfDetector.Params params) {
        switch (type) {
            case MONOGRAPH:
                return params.forcedDmfMonVersion;
            case PERIODICAL:
                return params.forcedDmfPerVersion;
            case AUDIO_GRAM:
                return params.forcedDmfAdgVersion;
            case AUDIO_FONO:
                return params.forcedDmfAdfVersion;
            case AUDIO_DISC:
                return params.forcedDmfAdiVersion;
            case AUDIO_NO_CARRIER:
                return params.forcedDmfAdnVersion;
            case DATA_DISC:
                return params.forcedDmfDadVersion;
            case FUND_UNIT:
                return params.forcedDmfFduVersion;
            case EMONOGRAPH:
                return params.forcedDmfEmoVersion;
            case EPERIODICAL:
                return params.forcedDmfEpeVersion;
            case COMPERIODICAL:
                return params.forcedDmfSepVersion;
            default:
                return null;
        }
    }

    private static Set<String> supportedVersions(Dmf.Type type, FdmfRegistry registry) {
        switch (type) {
            case MONOGRAPH:
                return registry.getMonographFdmfVersions();
            case PERIODICAL:
                return registry.getPeriodicalFdmfVersions();
            case AUDIO_GRAM:
                return registry.getAudioGramFdmfVersions();
            case AUDIO_FONO:
                return registry.getAudioFonoFdmfVersions();
            case AUDIO_DISC:
                return registry.getAudioDiscFdmfVersions();
            case AUDIO_NO_CARRIER:
                return registry.getAudioNoCarrierFdmfVersions();
            case DATA_DISC:
                return registry.getDataDiscFdmfVersions();
            case FUND_UNIT:
                return registry.getFundUnitFdmfVersions();
            case EMONOGRAPH:
                return registry.getEmonographFdmfVersions();
            case EPERIODICAL:
                return registry.getEperiodicalFdmfVersions();
            case COMPERIODICAL:
                return registry.getComposedPeriodicalFdmfVersions();
            default:
                return Collections.emptySet();
        }
    }

    /** Verze seřazené číselně po částech (1.3 &lt; 1.3.1 &lt; 1.10), nečíselné části textově. */
    static List<String> sortVersions(Collection<String> versions) {
        List<String> sorted = new ArrayList<>(versions);
        sorted.sort((a, b) -> {
            String[] pa = a.split("\\.");
            String[] pb = b.split("\\.");
            for (int i = 0; i < Math.min(pa.length, pb.length); i++) {
                int c;
                try {
                    c = Integer.compare(Integer.parseInt(pa[i]), Integer.parseInt(pb[i]));
                } catch (NumberFormatException e) {
                    c = pa[i].compareTo(pb[i]);
                }
                if (c != 0) {
                    return c;
                }
            }
            return Integer.compare(pa.length, pb.length);
        });
        return sorted;
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
