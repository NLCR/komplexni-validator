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
            pvMgr.addFile("INFO_XSD_FILE", new File("/home/martin/ssd/IdeaProjects/PspValidator/sharedModule/src/main/resources/rzehan/shared/fdmf_1_1_3/xsds/info_1.1.xsd"));

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
