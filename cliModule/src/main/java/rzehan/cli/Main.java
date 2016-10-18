package rzehan.cli;

import rzehan.shared.SharedClass;
import rzehan.shared.xsdValidation.XsdValidator;

import java.io.File;

/**
 * Created by Martin Řehánek on 27.9.16.
 */
public class Main {
    public static void main(String[] args) {
        //String shared = "TODO";
        /*String shared = new SharedClass().toString();
        System.out.println("Hello from CLI, shared: " + shared);*/


        //info - TODO: problem s validaci bez namespacu
        File infoXml = new File("/home/martin/IdeaProjects/PspValidator/sharedModule/src/main/resources/rzehan/shared/fdmf_1_1_3/xsds/examples/info.xml");
        File infoXsd = new File("/home/martin/IdeaProjects/PspValidator/sharedModule/src/main/resources/rzehan/shared/fdmf_1_1_3/xsds/alto_2.0.xsd");
        XsdValidator.validate("INFO", infoXsd, infoXml);

        //mix - ok
        File mixXsd = new File("/home/martin/IdeaProjects/PspValidator/sharedModule/src/main/resources/rzehan/shared/fdmf_1_1_3/xsds/mix_2.0.xsd");
        File mixXml = new File("/home/martin/IdeaProjects/PspValidator/sharedModule/src/main/resources/rzehan/shared/fdmf_1_1_3/xsds/examples/mix.xml");
        XsdValidator.validate("MIX", mixXsd, mixXml);

        //premis - ok
        File premisXsd = new File("/home/martin/IdeaProjects/PspValidator/sharedModule/src/main/resources/rzehan/shared/fdmf_1_1_3/xsds/premis_2.2.xsd");
        File premisXml = new File("/home/martin/IdeaProjects/PspValidator/sharedModule/src/main/resources/rzehan/shared/fdmf_1_1_3/xsds/examples/premis.xml");
        XsdValidator.validate("PREMIS", premisXsd, premisXml);

        //dc - TODO, jak je to s tim root elementem a jeste import xml.xsd v xsd
        File dcXsd = new File("/home/martin/IdeaProjects/PspValidator/sharedModule/src/main/resources/rzehan/shared/fdmf_1_1_3/xsds/dc_1.1.xsd");
        File dcXml = new File("/home/martin/IdeaProjects/PspValidator/sharedModule/src/main/resources/rzehan/shared/fdmf_1_1_3/xsds/examples/dc.xml");
        XsdValidator.validate("DC", dcXsd, dcXml);

        //mods - TODO: problem s importem xml.xsd
        File modsXsd = new File("/home/martin/IdeaProjects/PspValidator/sharedModule/src/main/resources/rzehan/shared/fdmf_1_1_3/xsds/mods_3.5.xsd");
        File modsXml = new File("/home/martin/IdeaProjects/PspValidator/sharedModule/src/main/resources/rzehan/shared/fdmf_1_1_3/xsds/examples/mods.xml");
        XsdValidator.validate("MODS", modsXsd, modsXml);

        //mets - ok
        File metsXsd = new File("/home/martin/IdeaProjects/PspValidator/sharedModule/src/main/resources/rzehan/shared/fdmf_1_1_3/xsds/mets_1.9.1.xsd");
        File metsXml = new File("/home/martin/IdeaProjects/PspValidator/sharedModule/src/main/resources/rzehan/shared/fdmf_1_1_3/xsds/examples/mets.xml");
        XsdValidator.validate("METS", metsXsd, metsXml);


    }

}
