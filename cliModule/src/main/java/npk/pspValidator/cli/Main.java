package npk.pspValidator.cli;

import nkp.pspValidator.shared.*;
import nkp.pspValidator.shared.engine.exceptions.InvalidXPathExpressionException;
import nkp.pspValidator.shared.engine.exceptions.PspDataException;
import nkp.pspValidator.shared.engine.exceptions.ValidatorConfigurationException;
import nkp.pspValidator.shared.engine.exceptions.XmlParsingException;
import nkp.pspValidator.shared.xsdValidation.XsdValidator;
import org.apache.commons.cli.*;

import java.io.File;

/**
 * Created by Martin Řehánek on 27.9.16.
 */
public class Main {
    public static void main(String[] args) throws PspDataException, XmlParsingException, InvalidXPathExpressionException, FdmfRegistry.UnknownFdmfException, ValidatorConfigurationException {

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
                .withArgName("DMF_VERSION")
                .withLongOpt("dmf-version")
                .create("dv"));

        options.addOption(OptionBuilder
                .withDescription("Adresář obsahující formalizované DMF pro jednotlivé verze standardu.")
                .hasArg()
                .withArgName("FDMF_DIR")
                .withLongOpt("fdmf-dir")
                .create("fd"));
        options.addOption(OptionBuilder
                .withDescription("Adresář PSP balíku, který má být validován.")
                //.isRequired()
                .hasArg()
                .withArgName("PSP_DIR")
                .withLongOpt("psp-dir")
                .create("pd"));

        /*options.addOption(OptionBuilder
                .withDescription("soubor se zabaleným PSP balíkem")
                .hasArg()
                .withArgName("PSP.ZIP")
                .withLongOpt("psp-zip")
                .create("z"));*/

        options.addOption(OptionBuilder.withLongOpt("help")
                .withDescription("Zobrazit tuto nápovědu.")
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
                System.out.println("pspValidator for CLI verze 0.1");
            } else {
                if (!line.hasOption("fd")) {
                    System.err.println("pro spuštění validace je nutné určit adresář obsahující formalizované DMF");
                    printHelp(options);
                    return;
                }
                if (!line.hasOption("pd")) {
                    System.err.println("pro spuštění validace je nutné zadat PSP balík");
                    printHelp(options);
                    return;
                }
                Dmf.Type dmfType = line.hasOption("dt") ? Dmf.Type.valueOf(line.getOptionValue("dt").toUpperCase()) : null;
                String dmfVersion = line.hasOption("dv") ? line.getOptionValue("dv") : null;


                String fdmfsDir = line.getOptionValue("fd");
                String pspDir = line.getOptionValue("pd");
                validate(new Dmf(dmfType, dmfVersion), new File(fdmfsDir), new File(pspDir));
            }
        } catch (ParseException exp) {
            // oops, something went wrong
            System.err.println("Parsing failed.  Reason: " + exp.getMessage());
            printHelp(options);
        }
        /*validate(new Dmf(Dmf.Type.MONOGRAPH, "1.3"),
                new File("/home/martin/IdeaProjects/PspValidator/sharedModule/src/main/resources/rzehan/shared/fDMF"),
                new File("/home/martin/IdeaProjects/PspValidator/sharedModule/src/test/resources/monograph_1.2/b50eb6b0-f0a4-11e3-b72e-005056827e52")
        );*/
    }

    private static void printHelp(Options options) {
        String header = "Validuje PSP balík, nebo sadu PSP balíků podle DMF*." +
                " Verze a typ DMF lze vynutit parametry --dmf-type a/nebo --dmf-version, případně jsou odvozeny z dat PSP balíku." +
                " Dále je potřeba pomocí --fdmf-dir uvést adresář, který obsahuje definice fDMF," +
                " typicky adresáře monograph_1.0, monograph_1.2, periodical_1.4 a periodical 1.6.\n\n";
        String footer = "\n*Definice metadatových formátů. Více na http://www.ndk.cz/standardy-digitalizace/metadata.\n" +
                "Více informací o validátoru najdete na http://TODO ";

        HelpFormatter formatter = new HelpFormatter();
        formatter.setOptionComparator(null);
        formatter.printHelp("pspValidator", header, options, footer, true);
    }


    private static void validate(Dmf dmfPrefered, File fdmfsRoot, File pspRoot) throws PspDataException, InvalidXPathExpressionException, XmlParsingException, FdmfRegistry.UnknownFdmfException, ValidatorConfigurationException {
        checkReadableDir(pspRoot);
        checkReadableDir(fdmfsRoot);
        System.out.println(String.format("Zpracovávám PSP balík %s", pspRoot.getAbsolutePath()));
        Dmf dmfResolved = new DmfDetector().resolveDmf(dmfPrefered, pspRoot);
        System.out.println(String.format("Bude použita verze standardu %s", dmfResolved));
        File fdmfRoot = new FdmfRegistry(fdmfsRoot).getFdmfDir(dmfResolved);
        System.out.println(String.format("Kořenový adresář fDMF: %s", fdmfRoot.getAbsolutePath()));
        Validator validator = ValidatorFactory.buildValidator(fdmfRoot, pspRoot);
        System.out.println(String.format("Validátor inicializován, spouštím validace"));
        validator.run(false, false, true);
    }

    private static void checkReadableDir(File pspRoot) {
        if (!pspRoot.exists()) {
            throw new IllegalStateException(String.format("soubor %s neexistuje", pspRoot.getAbsolutePath()));
        } else if (!pspRoot.isDirectory()) {
            throw new IllegalStateException(String.format("soubor %s není adresář", pspRoot.getAbsolutePath()));
        } else if (!pspRoot.canRead()) {
            throw new IllegalStateException(String.format("nelze číst adresář %s", pspRoot.getAbsolutePath()));
        }
    }

    private static void testXsds() {
        //info - ok
        File infoXml = new File("/home/martin/IdeaProjects/PspValidator/sharedModule/src/main/resources/rzehan/shared/fdmf_1_1_3/xsd/examples/info.xml");
        File infoXsd = new File("/home/martin/IdeaProjects/PspValidator/sharedModule/src/main/resources/rzehan/shared/fdmf_1_1_3/xsd/info_1.1.xsd");
        XsdValidator.validate("INFO", infoXsd, infoXml);

       /* //mix - ok
        File mixXsd = new File("/home/martin/IdeaProjects/PspValidator/sharedModule/src/main/resources/rzehan/shared/fdmf_1_1_3/xsd/mix_2.0.xsd");
        File mixXml = new File("/home/martin/IdeaProjects/PspValidator/sharedModule/src/main/resources/rzehan/shared/fdmf_1_1_3/xsd/examples/mix.xml");
        XsdValidator.validate("MIX", mixXsd, mixXml);

        //premis - ok
        File premisXsd = new File("/home/martin/IdeaProjects/PspValidator/sharedModule/src/main/resources/rzehan/shared/fdmf_1_1_3/xsd/premis_2.2.xsd");
        File premisXml = new File("/home/martin/IdeaProjects/PspValidator/sharedModule/src/main/resources/rzehan/shared/fdmf_1_1_3/xsd/examples/premis.xml");
        XsdValidator.validate("PREMIS", premisXsd, premisXml);

        //dc - TODO, jak je to s tim root elementem a jeste import xml.xsd v xsd
        File dcXsd = new File("/home/martin/IdeaProjects/PspValidator/sharedModule/src/main/resources/rzehan/shared/fdmf_1_1_3/xsd/dc_1.1.xsd");
        File dcXml = new File("/home/martin/IdeaProjects/PspValidator/sharedModule/src/main/resources/rzehan/shared/fdmf_1_1_3/xsd/examples/dc.xml");
        XsdValidator.validate("DC", dcXsd, dcXml);

        //mods - TODO: problem s importem xml.xsd
        File modsXsd = new File("/home/martin/IdeaProjects/PspValidator/sharedModule/src/main/resources/rzehan/shared/fdmf_1_1_3/xsd/mods_3.5.xsd");
        File modsXml = new File("/home/martin/IdeaProjects/PspValidator/sharedModule/src/main/resources/rzehan/shared/fdmf_1_1_3/xsd/examples/mods.xml");
        XsdValidator.validate("MODS", modsXsd, modsXml);

        //mets - ok
        File metsXsd = new File("/home/martin/IdeaProjects/PspValidator/sharedModule/src/main/resources/rzehan/shared/fdmf_1_1_3/xsd/mets_1.9.1.xsd");
        File metsXml = new File("/home/martin/IdeaProjects/PspValidator/sharedModule/src/main/resources/rzehan/shared/fdmf_1_1_3/xsd/examples/mets.xml");
        XsdValidator.validate("METS", metsXsd, metsXml);*/
    }

}
