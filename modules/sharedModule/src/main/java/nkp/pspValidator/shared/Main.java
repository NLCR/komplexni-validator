package nkp.pspValidator.shared;

import nkp.pspValidator.shared.engine.exceptions.InvalidXPathExpressionException;
import nkp.pspValidator.shared.engine.exceptions.ValidatorConfigurationException;
import nkp.pspValidator.shared.engine.types.MetadataFormat;
import nkp.pspValidator.shared.engine.utils.UrnNbnResolverChecker;
import nkp.pspValidator.shared.engine.validationFunctions.ValidationResult;
import nkp.pspValidator.shared.metadataProfile.DictionaryManager;
import nkp.pspValidator.shared.metadataProfile.MetadataProfile;
import nkp.pspValidator.shared.metadataProfile.MetadataProfileParser;
import nkp.pspValidator.shared.metadataProfile.MetadataProfileValidator;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import java.io.File;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

public class Main {

    public static void main(String[] args) throws IOException, KeyStoreException, KeyManagementException, NoSuchAlgorithmException, UrnNbnResolverChecker.ResolverError {
        System.out.println("Hello from shared module. Args: " + buildArgsString(args));
        //checkUrnNbns();
    }

    private static void checkUrnNbns() throws KeyManagementException, NoSuchAlgorithmException, IOException, KeyStoreException {
        UrnNbnResolverChecker checker = new UrnNbnResolverChecker(null);
        //init();
        //ACTIVE
        check(checker, "urn:nbn:cz:mzk-0001vw");
        //FREE
        check(checker, "urn:nbn:cz:mzk-xxxxxx");
        //RESERVED
        check(checker, "urn:nbn:cz:p01nk-000007");
        //DEACTIVATED
        check(checker, "urn:nbn:cz:zne001-00002o");

        //wrong lang code
        check(checker, "urn:nbn:cc:mzk-0001vw");
        //wrong prefix
        check(checker, "urn-nbn-cz:mzk-0001vw");
        //invalid registrar code
        check(checker, "urn:nbn:cz:m-0001vw");
        //invalid document code
        check(checker, "urn:nbn:cz:mzk-000");
    }

    private static void check(UrnNbnResolverChecker checker, String urnNbn) throws IOException, KeyStoreException {
        try {
            checker.check(urnNbn);
        } catch (UrnNbnResolverChecker.ResolverError e) {
            System.out.println("ERROR: " + e.getMessage());
        } catch (UrnNbnResolverChecker.ResolverWarning e) {
            System.out.println("WARNING: " + e.getMessage());
        } catch (XPathExpressionException e) {
            System.out.println("ERROR: " + e.getMessage());
        } catch (InvalidXPathExpressionException e) {
            System.out.println("ERROR: " + e.getMessage());
        }
    }

    private static String buildArgsString(String[] args) {
        StringBuilder sb = new StringBuilder();
        for (String arg : args) {
            sb.append(arg).append(" ");
        }
        return sb.toString();
    }

    public static void templateTest() {
        System.out.println("Hello from shared module templateTest()");
        String dmf = "monograph_1.2"; //TODO: check if exists
        File validatorConfigDir = new File("/Users/martin/IdeaProjects/komplexni-validator/modules/sharedModule/src/main/resources/nkp/pspValidator/shared/validatorConfig");
        File metadataFile = new File("/Users/martin/IdeaProjects/komplexni-validator/modules/sharedModule/src/test/resources/metadata_only/mon_1.2/dc_volume_singlevolume_aacr2.xml");

        //File metadataFile = new File("/Users/martin/IdeaProjects/komplexni-validator/modules/sharedModule/src/test/resources/metadata_only/mon_1.2/dc_volume_singlevolume_aacr2.xml");
        ///Users/martin/IdeaProjects/komplexni-validator/modules/sharedModule/src/test/resources/metadata_only/mon_1.4/mods_page_rda.xml

        String profile = "biblio:dc:volume_singlevolume_aacr2";
        //profile = "biblio:dc:volume_singlevolume_aacr2";
        //profile = "biblio:mods:volume_cartographic_rda";
        //profile = "tech:premis_agent";
        //profile = "tech:premis_object_mc";
        ValidationResult result = validateSingleFile(validatorConfigDir,
                /*"monograph_1.2:biblio:dc:volume_singlevolume_aacr2",
                new File("/Users/martin/IdeaProjects/komplexni-validator/modules/sharedModule/src/test/resources/metadata_only/mon_1.2/dc_volume_singlevolume_aacr2.xml")*/
                /*"monograph_1.4:biblio:mods:page_rda",
                new File("/Users/martin/IdeaProjects/komplexni-validator/modules/sharedModule/src/test/resources/metadata_only/mon_1.4/mods_page_rda.xml")*/
                "monograph_1.4:tech:premis_agent",
                new File("/Users/martin/IdeaProjects/komplexni-validator/modules/sharedModule/src/test/resources/metadata_only/mon_1.4/premis_agent.xml")
        );
        if (result != null) {
            if (result.hasProblems()) {
                result.getProblems().forEach(p -> System.out.println(p.getLevel() + ": " + p.getMessage(false)));
            } else {
                System.out.println("No problems found in file " + metadataFile.getName() + " against profile " + profile);
            }
        }
    }

    private static ValidationResult validateSingleFile(File validatorConfigDir, String profileId, File metadataFile) {
        try {
            String[] profileTokens = profileId.split(":");
            String dmf = profileTokens[0];
            DictionaryManager dm = new DictionaryManager(new File(validatorConfigDir, "dictionaries"));
            MetadataProfileParser metadataProfileParser = new MetadataProfileParser(dm);
            System.out.println("Validating against profile " + profileId + " file " + metadataFile);
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
}
