package rzehan.shared.engine;

import org.junit.Test;

import java.io.File;

/**
 * Created by martin on 29.10.16.
 */
public class EngineInitiliazerTest {


    @Test
    public void initPatterns() {
        ProvidedVarsManagerImpl pvMgr = new ProvidedVarsManagerImpl();
        pvMgr.addFile("PSP_DIR", new File("src/test/resources/monografie_1.2/b50eb6b0-f0a4-11e3-b72e-005056827e52"));
        pvMgr.addString("PSP_ID", "b50eb6b0-f0a4-11e3-b72e-005056827e52");
        Engine engine = new Engine(pvMgr);

        EngineInitiliazer initiliazer = new EngineInitiliazer(engine);
        initiliazer.processFile(new File("/home/martin/ssd/IdeaProjects/PspValidator/sharedModule/src/main/resources/rzehan/shared/fdmf_1_1_3/patterns.xml"));
        initiliazer.processFile(new File("/home/martin/ssd/IdeaProjects/PspValidator/sharedModule/src/main/resources/rzehan/shared/fdmf_1_1_3/variables.xml"));
        //initiliazer.processFile(new File("/home/martin/ssd/IdeaProjects/PspValidator/sharedModule/src/main/resources/rzehan/shared/fdmf_1_1_3/examples.xml"));

        initiliazer.processFile(new File("/home/martin/ssd/IdeaProjects/PspValidator/sharedModule/src/main/resources/rzehan/shared/fdmf_1_1_3/rules.xml"));

    }
}
