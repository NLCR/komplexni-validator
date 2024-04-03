package nkp.pspValidator.shared.dev;

import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.File;
import java.io.IOException;

/**
 * Created by Martin Řehánek on 18.10.16.
 */
public class XsdValidator {

    public static void testXsds() {
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

    public static void validate(String label, File xsd, File xml) {
        //System.out.println("validating " + label);
        //System.out.println("xsd: " + xsd.getAbsolutePath());
        //System.out.println("xml: " + xml.getAbsolutePath());
        SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        try {
            //Schema schema = schemaFactory.newSchema(new File(getResource(schemaFile)));
            Schema schema = schemaFactory.newSchema(xsd);

            /*SAXParserFactory spf = SAXParserFactory.newInstance();
            spf.setValidating(false);//vypne dtd validaci
            spf.setSchema(schema);

            XMLReader xmlReader = spf.newSAXParser().getXMLReader();
            //xmlReader.setContentHandler(...);
            xmlReader.parse(new InputSource()); // will validate against the schema*/


            Validator validator = schema.newValidator();
            //validator.validate(new StreamSource(new File(getResource(xmlFile))));
            validator.validate(new StreamSource(xml));
            System.out.println(label + ": VALID");
            //return true;
        } catch (SAXException | IOException e) {
            System.out.println(label + ": NOT VALID");
            e.printStackTrace();
        } finally {
            System.out.println();
        }
    }

    /*private String getResource(String filename) throws FileNotFoundException {
        URL resource = getClass().getClassLoader().getResource(filename);
        Objects.requireNonNull(resource);

        return resource.getFile();
    }*/

}
