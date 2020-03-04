package nkp.pspValidator.shared.engine;

import nkp.pspValidator.shared.XmlUtils;
import nkp.pspValidator.shared.engine.evaluationFunctions.TestUtils;
import org.junit.BeforeClass;
import org.junit.Test;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.*;

/**
 * Created by Martin Řehánek on 20.10.16.
 */
public class PatternTest {

    private static Engine engine;

    @BeforeClass
    public static void setup() {
        engine = new Engine(null);
        engine.setProvidedString("STR1", "neco");
        engine.setProvidedString("STR2", "nic");
        engine.setProvidedInteger("NUM1", 6);
        engine.setProvidedString("PSP_ID", "b50eb6b0-f0a4-11e3-b72e-005056827e52");
        engine.setProvidedString("DOT", ".");
    }


    @Test
    public void noVariableRegexpConstant() {
        assertTrue(engine.buildPatternDefinition(engine.buildRawPatternExpression(true, "amdsec")).evaluate().matches("amdsec"));
        assertTrue(engine.buildPatternDefinition(engine.buildRawPatternExpression(true, "AMDSEC")).evaluate().matches("AMDSEC"));

        //case sensitivity
        assertFalse(engine.buildPatternDefinition(engine.buildRawPatternExpression(true, "amdsec")).evaluate().matches("AMDSEC"));
        assertFalse(engine.buildPatternDefinition(engine.buildRawPatternExpression(true, "AMDSEC")).evaluate().matches("amdsec"));
        assertTrue(engine.buildPatternDefinition(engine.buildRawPatternExpression(false, "amdsec")).evaluate().matches("AMDSEC"));
        assertTrue(engine.buildPatternDefinition(engine.buildRawPatternExpression(false, "AMDSEC")).evaluate().matches("amdsec"));

        //more expressions per pattern
        assertTrue(engine.buildPatternDefinition(engine.buildRawPatternExpression(true, "amdsec"), engine.buildRawPatternExpression(true, "ocr")).evaluate().matches("amdsec"));
        assertTrue(engine.buildPatternDefinition(engine.buildRawPatternExpression(true, "amdsec"), engine.buildRawPatternExpression(true, "ocr")).evaluate().matches("ocr"));
        assertFalse(engine.buildPatternDefinition(engine.buildRawPatternExpression(true, "amdsec"), engine.buildRawPatternExpression(true, "ocr")).evaluate().matches("alto"));
    }

    @Test
    public void noVariableRegexp() {
        assertTrue(engine.buildPatternDefinition(engine.buildRawPatternExpression(true, "info_.+\\.xml")).evaluate().matches("info_SOMETHING.xml"));
        assertFalse(engine.buildPatternDefinition(engine.buildRawPatternExpression(true, "info_.+\\.xml")).evaluate().matches("info_SOMETHINGxml"));
        assertFalse(engine.buildPatternDefinition(engine.buildRawPatternExpression(true, "info_.+\\.xml")).evaluate().matches("info_.xml"));

        assertTrue(engine.buildPatternDefinition(engine.buildRawPatternExpression(true, "info\\.xml")).evaluate().matches("info.xml"));
        assertFalse(engine.buildPatternDefinition(engine.buildRawPatternExpression(true, "info\\.xml")).evaluate().matches("info,xml"));

        assertTrue(engine.buildPatternDefinition(engine.buildRawPatternExpression(true, "info_.+\\.xml"), engine.buildRawPatternExpression(true, "info.xml")).evaluate().matches("info.xml"));
        assertTrue(engine.buildPatternDefinition(engine.buildRawPatternExpression(true, "info_.+\\.xml"), engine.buildRawPatternExpression(true, "info.xml")).evaluate().matches("info_SOMETHING.xml"));

        assertTrue(engine.buildPatternDefinition(engine.buildRawPatternExpression(true, "[a-z_\\-\\.]+")).evaluate().matches("x.y_z-a"));
        assertFalse(engine.buildPatternDefinition(engine.buildRawPatternExpression(true, "[a-z_\\-\\.]+")).evaluate().matches(""));
        assertTrue(engine.buildPatternDefinition(engine.buildRawPatternExpression(false, "[a-z_\\-\\.]+")).evaluate().matches("X.y_Z-a"));

        assertTrue(engine.buildPatternDefinition(engine.buildRawPatternExpression(false, "[a-z0-9_\\-\\.]+")).evaluate().matches("info_b50eb6b0-f0a4-11e3-b72e-005056827e52.xml"));
    }


    @Test
    public void variableRegexp() {
        TestUtils.defineProvidedStringVar(engine, "STR1");
        TestUtils.defineProvidedStringVar(engine, "STR2");

        assertTrue(engine.buildPatternDefinition(engine.buildRawPatternExpression(true, "prefix_${STR1}_suffix")).evaluate().matches("prefix_neco_suffix"));
        assertTrue(engine.buildPatternDefinition(engine.buildRawPatternExpression(true, "prefix_${STR1}\\+${STR2}_suffix")).evaluate().matches("prefix_neco+nic_suffix"));

        //variable not defined
        PatternEvaluation evaluationVarNotDefined = engine.buildPatternDefinition(engine.buildRawPatternExpression(true, "prefix_${STR3}_suffix")).evaluate();
        assertFalse(evaluationVarNotDefined.isOk());
        assertNotNull(evaluationVarNotDefined.getErrorMessage());


        //TODO: ted bere vsechno, klidne zavola toString() na List<File>
/*
        //invalid varialble type
        TestUtils.defineProvidedIntegerVar(engine, "NUM1");
        PatternEvaluation evaluationInvalidVarType = engine.buildPatternDefinition(engine.buildRawPatternExpression(true, "prefix_${NUM1}_suffix")).evaluate();
        assertFalse(evaluationInvalidVarType.isOk());
        assertNotNull(evaluationInvalidVarType.getErrorMessage());
*/

        TestUtils.defineProvidedStringVar(engine, "PSP_ID");
        assertTrue(engine.buildPatternDefinition(engine.buildRawPatternExpression(true, "info_${PSP_ID}\\.xml")).evaluate().matches("info_b50eb6b0-f0a4-11e3-b72e-005056827e52.xml"));
        assertTrue(engine.buildPatternDefinition(engine.buildRawPatternExpression(true, "amd_mets_${PSP_ID}_[0-9]+\\.xml")).evaluate().matches("amd_mets_b50eb6b0-f0a4-11e3-b72e-005056827e52_0.xml"));
        assertTrue(engine.buildPatternDefinition(engine.buildRawPatternExpression(true, "amd_mets_${PSP_ID}_[0-9]+\\.xml")).evaluate().matches("amd_mets_b50eb6b0-f0a4-11e3-b72e-005056827e52_123.xml"));
        assertFalse(engine.buildPatternDefinition(engine.buildRawPatternExpression(true, "amd_mets_${PSP_ID}_[0-9]+\\.xml")).evaluate().matches("amd_mets_b50eb6b0-f0a4-11e3-b72e-005056827e52_.xml"));

        //escaping special characters in variables - \\.[]{}()*+-?^$|
        TestUtils.defineProvidedStringVar(engine, "DOT");
        assertTrue(engine.buildPatternDefinition(engine.buildRawPatternExpression(true, "${DOT}co${DOT}uk")).evaluate().matches(".co.uk"));
        assertFalse(engine.buildPatternDefinition(engine.buildRawPatternExpression(true, "${DOT}co${DOT}uk")).evaluate().matches("0coXuk"));
    }

    @Test
    public void other() {
        assertTrue(engine.buildPatternDefinition(
                engine.buildRawPatternExpression(true, "info_.+\\.xml"),
                engine.buildRawPatternExpression(true, "info\\.xml")
        ).evaluate().matches("info_b50eb6b0-f0a4-11e3-b72e-005056827e52.xml"));
    }


    @Test
    public void kakaduPattern() {
        String text = "" +
                "Kakadu Core Error:\n" +
                "Expected to find EPH marker following packet header.  Found 0xff instead.\n" +
                "Kakadu Core Error:\n" +
                "Expected to find EPH marker following packet header.  Found 0x0 instead.\n" +
                "Kakadu Core Error:\n" +
                "Expected to find EPH marker following packet header.  Found 0x0 instead.\n" +
                "Kakadu Core Error:\n" +
                "Expected to find EPH marker following packet header.  Found 0x0 instead.\n" +
                "Kakadu Core Error:\n" +
                "Code-stream must start with an SOC marker!\n" +
                "Kakadu Core Error:\n" +
                "Missing or out-of-sequence tile-parts for tile number 0 in code-stream!\n" +
                "Kakadu Core Error:\n" +
                "Out-of-sequence SOP marker found while attempting to read a packet from the\n" +
                "code-stream!\n" +
                "    Found sequence number 0, but expected 392.\n" +
                "Use the resilient option if you would like to try to recover from this error.\n";

        String regexp = "(?ms)Kakadu Core Error:((?!Kakadu Core Error).)*";
        Pattern p = Pattern.compile(regexp);

        Matcher m = p.matcher(text);
        List<String> errors = new ArrayList<>();
        while (m.find()) {
            String match = m.group();
            //System.err.println(match);
            errors.add(match);
        }
        assertEquals(7, errors.size());
    }

    @Test
    public void imageMagickPatternError() {
        String text = "" +
                "Error : expected SOP marker\n" +
                "Error : expected EPH marker\n" +
                "Error : expected SOP marker\n" +
                "Error : expected EPH marker\n" +
                "Error : expected SOP marker\n" +
                "Error : expected EPH marker\n" +
                "read: segment too long (126853) with max (3140) for codeblock 2 (p=3, b=2, r=4, c=2)\n" +
                "identify: Failed to decode.\n" +
                " `OpenJP2' @ error/jp2.c/JP2ErrorHandler/193.\n" +
                "identify: Failed to decode tile 2/8\n" +
                " `OpenJP2' @ error/jp2.c/JP2ErrorHandler/193.\n" +
                "identify: Failed to decode the codestream in the JP2 file\n" +
                " `OpenJP2' @ error/jp2.c/JP2ErrorHandler/193.\n" +
                "identify: unable to decode image file `detekce_poskozeni/w7201gUC.jp2' @ error/jp2.c/ReadJP2Image/384.\n";
        //String regexp = "^Error.*$";
        //Pattern p = Pattern.compile(regexp, Pattern.MULTILINE);
        String regexp = "(?m)^Error.*$";
        Pattern p = Pattern.compile(regexp);
        Matcher m = p.matcher(text);
        List<String> errors = new ArrayList<>();
        while (m.find()) {
            String match = m.group();
            //System.err.println(match);
            errors.add(match);
        }
        assertEquals(6, errors.size());
        assertEquals("Error : expected SOP marker", errors.get(0));
        assertEquals("Error : expected EPH marker", errors.get(1));
        assertEquals("Error : expected SOP marker", errors.get(2));
        assertEquals("Error : expected EPH marker", errors.get(3));
        assertEquals("Error : expected SOP marker", errors.get(4));
        assertEquals("Error : expected EPH marker", errors.get(5));
    }

    @Test
    public void imageMagickPatterIdentify() {
        String text = "" +
                "Error : expected SOP marker\n" +
                "Error : expected EPH marker\n" +
                "Error : expected SOP marker\n" +
                "Error : expected EPH marker\n" +
                "Error : expected SOP marker\n" +
                "Error : expected EPH marker\n" +
                "read: segment too long (126853) with max (3140) for codeblock 2 (p=3, b=2, r=4, c=2)\n" +
                "identify: Failed to decode.\n" +
                " `OpenJP2' @ error/jp2.c/JP2ErrorHandler/193.\n" +
                "identify: Failed to decode tile 2/8\n" +
                " `OpenJP2' @ error/jp2.c/JP2ErrorHandler/193.\n" +
                "identify: Failed to decode the codestream in the JP2 file\n" +
                " `OpenJP2' @ error/jp2.c/JP2ErrorHandler/193.\n" +
                "identify: unable to decode image file `detekce_poskozeni/w7201gUC.jp2' @ error/jp2.c/ReadJP2Image/384.\n";
        //String regexp = "identify:((?!identify).)*";
        //Pattern p = Pattern.compile(regexp, Pattern.MULTILINE | Pattern.DOTALL);
        String regexp = "(?ms)identify:((?!identify).)*";
        Pattern p = Pattern.compile(regexp);
        Matcher m = p.matcher(text);
        List<String> errors = new ArrayList<>();
        while (m.find()) {
            String match = m.group();
            //System.err.println(match);
            errors.add(match);
        }
        assertEquals(4, errors.size());
    }

    @Test
    public void jhove() {
        String text = "READBOX seen=true<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<jhove xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://hul.harvard.edu/ois/xml/ns/jhove\"\n" +
                "       xsi:schemaLocation=\"http://hul.harvard.edu/ois/xml/ns/jhove http://hul.harvard.edu/ois/xml/xsd/jhove/1.6/jhove.xsd\"\n" +
                "       name=\"Jhove\" release=\"1.6\" date=\"2011-01-04\">\n" +
                "    <date>2016-11-21T00:53:00+01:00</date>\n" +
                "    <repInfo\n" +
                "            uri=\"/home/martin/IdeaProjects/komplexni-validator/cliModule/../sharedModule/src/test/resources/monograph_1.2-invalid_images/b50eb6b0-f0a4-11e3-b72e-005056827e52/mastercopy/mc_b50eb6b0-f0a4-11e3-b72e-005056827e52_0007.jp2\">\n" +
                "        <reportingModule release=\"1.3\" date=\"2007-01-08\">JPEG2000-hul</reportingModule>\n" +
                "        <lastModified>2016-11-20T13:30:25+01:00</lastModified>\n" +
                "        <size>677227</size>\n" +
                "        <format>JPEG 2000</format>\n" +
                "        <status>Well-Formed and valid</status>\n" +
                "        <sigMatch>\n" +
                "            <module>JPEG2000-hul</module>\n" +
                "        </sigMatch>\n" +
                "        <mimeType>image/jp2</mimeType>\n" +
                "        <checksums>\n" +
                "            <checksum type=\"CRC32\">bda73ccf</checksum>\n" +
                "            <checksum type=\"MD5\">ba557951bdda4d7fd6c49963c9c3fafb</checksum>\n" +
                "            <checksum type=\"SHA-1\">b4b4d049c7b4e362ca99ea625126cbe0ecfe62e0</checksum>\n" +
                "        </checksums>\n" +
                "    </repInfo>\n" +
                "</jhove>";
        String regexp = "(?s)<\\?xml.*$";
        Pattern p = Pattern.compile(regexp);
        Matcher m = p.matcher(text);
        int found = 0;
        while (m.find()) {
            found++;
            String match = m.group();
            //System.err.println(match);
            try {
                XmlUtils.buildDocumentFromString(match, true);
            } catch (ParserConfigurationException e) {
                fail(e.getMessage());
            } catch (IOException e) {
                fail(e.getMessage());
            } catch (SAXException e) {
                fail(e.getMessage());
            }
        }
        assertEquals(1, found);
    }

    @Test
    public void jhoveDetection() {
        String text = "Jhove (Rel. 1.14.6, 2016-05-12)\n" +
                " Date: 2016-12-07 10:29:40 CET\n" +
                " App:\n" +
                "  API: 1.14.6, 2016-05-12\n" +
                "  Configuration: /Users/martinrehanek/Software/jhove/conf/jhove.conf\n" +
                "  JhoveHome: /Users/martinrehanek/Software/jhove\n" +
                "  Encoding: utf-8\n" +
                "  TempDirectory: /var/folders/30/rb2929n16kld3mnlnpw7wb7h0000gn/T/\n" +
                "  BufferSize: 131072\n" +
                "  Module: AIFF-hul 1.3\n" +
                "  Module: ASCII-hul 1.3\n" +
                "  Module: BYTESTREAM 1.3\n" +
                "  Module: GIF-hul 1.3\n" +
                "  Module: GZIP-kb 0.1\n" +
                "  Module: HTML-hul 1.3\n" +
                "  Module: JPEG-hul 1.2\n" +
                "  Module: JPEG2000-hul 1.3\n" +
                "  Module: PDF-hul 1.7\n" +
                "  Module: PNG-gdm 1.0\n" +
                "  Module: TIFF-hul 1.7\n" +
                "  Module: UTF8-hul 1.6\n" +
                "  Module: WARC-kb 1.0\n" +
                "  Module: WAVE-hul 1.3\n" +
                "  Module: XML-hul 1.4\n" +
                "  OutputHandler: Audit 1.1\n" +
                "  OutputHandler: TEXT 1.5\n" +
                "  OutputHandler: XML 1.7\n" +
                "  Usage: java JHOVE [-c config] [-m module] [-h handler] [-e encoding] [-H handler] [-o output] [-x saxclass] [-t tempdir] [-b bufsize] [-l loglevel] [[-krs] dir-file-or-uri [...]]\n" +
                "  Rights: Derived from software Copyright 2004-2011 by the President and Fellows of Harvard College. Version 1.7 to 1.11 independently released. Version 1.12 onwards released by Open Preservation Foundation. Released under the GNU Lesser General Public License.";
        //String regexp = "(?m)^Jhove.*$";
        String regexp = "Rel\\. [0-9\\.]+";
        Pattern p = Pattern.compile(regexp);
        Matcher m = p.matcher(text);
        int found = 0;
        while (m.find()) {
            found++;
            String match = m.group();
            //assertEquals("Jhove (Rel. 1.14.6, 2016-05-12)", m.group());
            assertEquals("Rel. 1.14.6", m.group());
        }
        assertEquals(1, found);
    }


    @Test
    public void jpylyzerDetection() {
        String text = "1.17.0";
        //String regexp = "ˆ[0-9\\.]+$";
        String regexp = "\\A[0-9\\.]+$";
        Pattern p = Pattern.compile(regexp);
        Matcher m = p.matcher(text);
        int found = 0;
        while (m.find()) {
            found++;
            String match = m.group();
            assertEquals("1.17.0", m.group());
        }
        assertEquals(1, found);
    }


    @Test
    public void years() {
        //RRRR
        String regexp = "[1-9]{1}[0-9]{0,3}|0";
        assertTrue("1925".matches(regexp));
        assertTrue("925".matches(regexp));
        assertTrue("25".matches(regexp));
        assertTrue("5".matches(regexp));
        assertTrue("0".matches(regexp));

        assertFalse("51925".matches(regexp));
        assertFalse("0925".matches(regexp));
        assertFalse("025".matches(regexp));
        assertFalse("05".matches(regexp));

        //RRRR-RRRR
        regexp = "([1-9]{1}[0-9]{0,3}|0)-([1-9]{1}[0-9]{0,3}|0)";
        assertTrue("0-0".matches(regexp));
        assertTrue("2010-2017".matches(regexp));

        //DD.MM.RRRR
        regexp = "[0-9]{1,2}\\.[0-9]{1,2}\\.[0-9]{1,4}";
        assertTrue("1.1.2017".matches(regexp));
        assertTrue("31.10.7".matches(regexp));

        //DD.-DD.MM.RRRR
        regexp = "[0-9]{1,2}\\.-[0-9]{1,2}\\.[0-9]{1,2}\\.[0-9]{1,4}";
        assertTrue("1.-2.1.2017".matches(regexp));
        assertTrue("15.-16.11.7".matches(regexp));
    }

    @Test
    public void infoXsdFilename() {
        String regexp = "info_(mon|per)[0-9]+(\\.([0-9])+)*\\.xsd";
        assertTrue("info_mon1.2.xsd".matches(regexp));
        assertTrue("info_mon1.1.3.xsd".matches(regexp));
        assertTrue("info_per1.6.xsd".matches(regexp));
    }


}

