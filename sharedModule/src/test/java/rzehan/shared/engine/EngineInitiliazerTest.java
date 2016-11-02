package rzehan.shared.engine;

import org.junit.Test;
import rzehan.shared.engine.exceptions.ValidatorConfigurationException;
import rzehan.shared.engine.validationFunctions.ValidationResult;

import java.io.File;
import java.util.List;

/**
 * Created by martin on 29.10.16.
 */
public class EngineInitiliazerTest {


    @Test
    public void initPatterns() {
        try {
            ProvidedVarsManagerImpl pvMgr = new ProvidedVarsManagerImpl();
            pvMgr.addFile("PSP_DIR", new File("src/test/resources/monografie_1.2/b50eb6b0-f0a4-11e3-b72e-005056827e52"));
            //pvMgr.addFile("PSP_DIR", new File("/home/martin/zakazky/DMF-validator/data/monografie_1.2/b50eb6b0-f0a4-11e3-b72e-005056827e52"));

            //TODO: relativni cesta
            File fdmfRoot = new File("/home/martin/ssd/IdeaProjects/PspValidator/sharedModule/src/main/resources/rzehan/shared/fdmf_1_1_3");
            pvMgr.addFile("INFO_XSD_FILE", new File(fdmfRoot, "xsds/info_1.1.xsd"));
            pvMgr.addFile("ALTO_XSD_FILE", new File(fdmfRoot, "xsds/alto_2.0.xsd"));
            pvMgr.addFile("CMD_XSD_FILE", new File(fdmfRoot, "xsds/cmd_0.91.xsd"));
            pvMgr.addFile("DC_XSD_FILE", new File(fdmfRoot, "xsds/dc_1.1.xsd"));
            pvMgr.addFile("METS_XSD_FILE", new File(fdmfRoot, "xsds/mets_1.9.1.xsd"));
            pvMgr.addFile("MIX_XSD_FILE", new File(fdmfRoot, "xsds/mix_2.0.xsd"));
            pvMgr.addFile("MODS_XSD_FILE", new File(fdmfRoot, "xsds/mods_3.5.xsd"));
            pvMgr.addFile("PREMIS_XSD_FILE", new File(fdmfRoot, "xsds/premis_2.2.xsd"));

            //pvMgr.addString("PSP_ID", "b50eb6b0-f0a4-11e3-b72e-005056827e52");
            Engine engine = new Engine(pvMgr);

            System.out.println("INITILIZING");
            System.out.println("-----------");
            EngineInitiliazer initiliazer = new EngineInitiliazer(engine);
            initiliazer.processFile(new File("/home/martin/ssd/IdeaProjects/PspValidator/sharedModule/src/main/resources/rzehan/shared/fdmf_1_1_3/patterns.xml"));
            initiliazer.processFile(new File("/home/martin/ssd/IdeaProjects/PspValidator/sharedModule/src/main/resources/rzehan/shared/fdmf_1_1_3/variables.xml"));
            //initiliazer.processFile(new File("/home/martin/ssd/IdeaProjects/PspValidator/sharedModule/src/main/resources/rzehan/shared/fdmf_1_1_3/examples.xml"));
            initiliazer.processFile(new File("/home/martin/ssd/IdeaProjects/PspValidator/sharedModule/src/main/resources/rzehan/shared/fdmf_1_1_3/rules.xml"));
            System.out.println("-----------");
            System.out.println();

            System.out.println("VALIDATING");
            System.out.println("-----------");
            List<RulesSection> rulesSections = engine.getRuleSections();
            for (RulesSection section : rulesSections) {
                System.out.println("Running section " + section.getName());
                List<Rule> rules = engine.getRules(section);
                for (Rule rule : rules) {
                    //System.out.println("Running rule " + rule.getName());
                    ValidationResult result = rule.getResult();
                    if (result.isValid()) {
                        //System.out.println(String.format("rule %s: OK", rule.getName()));
                    } else {
                        System.out.println(String.format("rule %s: %s: %s", rule.getName(), rule.getLevel(), result.getMessage()));
                        System.out.println(String.format("\t%s", rule.getDescription()));
                    }
                }
            }
        } catch (ValidatorConfigurationException e) {
            System.out.println("chyba v konfiguraci: " + e.getMessage());
        }
    }
}
