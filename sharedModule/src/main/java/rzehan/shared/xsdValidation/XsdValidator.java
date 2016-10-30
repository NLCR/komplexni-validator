package rzehan.shared.xsdValidation;

import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.File;
import java.io.IOException;

/**
 * Created by martin on 18.10.16.
 */
public class XsdValidator {

    public static void validate(String label, File xsd, File xml) {
        System.out.println("validating " + label);
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
