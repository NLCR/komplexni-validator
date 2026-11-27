package nkp.pspValidator.shared;

import nkp.pspValidator.shared.engine.exceptions.ValidatorConfigurationException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Stream;

import static org.junit.Assert.*;

/**
 * Liny FdmfRegistry: pri vytvoreni se nacte jen seznam adresaru fDMF, konfigurace konkretni fDMF se nacte a zvaliduje
 * az pri prvnim pouziti. Chyba v konfiguraci se ohlasi pri pouziti dane fDMF se stejnou hlaskou, jakou hlasi
 * FdmfConfiguration (drive pri startu). Kontrolu vsech vydavanych fDMF dela AllFdmfSmokeTest.
 */
public class FdmfRegistryLazyTest {

    private static final File CONFIG_DIR = new File("src/main/resources/nkp/pspValidator/shared/validatorConfig");
    private static final Dmf FUND_UNIT_02 = new Dmf(Dmf.Type.FUND_UNIT, "0.2");
    private static final Dmf DATA_DISC_01 = new Dmf(Dmf.Type.DATA_DISC, "0.1");

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    @Test
    public void constructorLoadsNoFdmfConfiguration() throws Exception {
        FdmfRegistry registry = new FdmfRegistry(new ValidatorConfigurationManager(CONFIG_DIR));
        assertEquals(0, registry.getLoadedFdmfConfigCount());
        //verze jsou k dispozici z nazvu adresaru bez nacteni konfigurace
        assertTrue(registry.getFundUnitFdmfVersions().contains("0.2"));
        assertTrue(registry.getMonographFdmfVersions().contains("2.0"));
        assertEquals(0, registry.getLoadedFdmfConfigCount());
    }

    @Test
    public void loadsOnlyUsedFdmfAndCachesIt() throws Exception {
        FdmfRegistry registry = new FdmfRegistry(new ValidatorConfigurationManager(CONFIG_DIR));
        FdmfConfiguration first = registry.getFdmfConfig(FUND_UNIT_02);
        assertNotNull(first);
        assertEquals(1, registry.getLoadedFdmfConfigCount());
        assertTrue(registry.isFdmfConfigLoaded(FUND_UNIT_02));
        assertFalse(registry.isFdmfConfigLoaded(new Dmf(Dmf.Type.MONOGRAPH, "2.0")));
        assertSame(first, registry.getFdmfConfig(FUND_UNIT_02));
        assertSame(first, registry.getFundUnitFdmfConfig("0.2"));
        assertEquals(1, registry.getLoadedFdmfConfigCount());
    }

    @Test
    public void unknownFdmfIsReportedWithoutLoading() throws Exception {
        FdmfRegistry registry = new FdmfRegistry(new ValidatorConfigurationManager(CONFIG_DIR));
        try {
            registry.getFdmfConfig(new Dmf(Dmf.Type.MONOGRAPH, "9.9"));
            fail("neexistujici fDMF mela vyhodit UnknownFdmfException");
        } catch (FdmfRegistry.UnknownFdmfException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("Není definována fDMF"));
        }
        assertNull(registry.getMonographFdmfConfig("9.9"));
        assertEquals(0, registry.getLoadedFdmfConfigCount());
    }

    @Test
    public void invalidConfigIsReportedOnFirstUseWithSameMessage() throws Exception {
        File configDir = configWith("data_disc_0.1", "fund_unit_0.2");
        File rules = new File(configDir, "fDMF/data_disc_0.1/rules.xml");
        String original = read(rules);
        //neznamy element v korenu: rules.xml neprojde XSD validaci
        String broken = original.replaceFirst("(<rules-section)", "<neznamy-element/>$1");
        assertNotEquals(original, broken);
        Files.write(rules.toPath(), broken.getBytes(StandardCharsets.UTF_8));

        ValidatorConfigurationManager configManager = new ValidatorConfigurationManager(configDir);
        //registr vznikne, rozbita fDMF se pri startu nenacita
        FdmfRegistry registry = new FdmfRegistry(configManager);
        //jina fDMF funguje
        assertNotNull(registry.getFdmfConfig(FUND_UNIT_02));

        String expected = eagerLoadError(configManager, new File(configDir, "fDMF/data_disc_0.1"));
        assertTrue(expected, expected.contains("není validní"));
        assertTrue(expected, expected.contains(rules.getAbsolutePath()));
        for (int i = 0; i < 2; i++) {
            //chyba se necachuje, hlasi se pri kazdem pouziti
            try {
                registry.getFdmfConfig(DATA_DISC_01);
                fail("rozbita konfigurace mela vyhodit ValidatorConfigurationException");
            } catch (ValidatorConfigurationException e) {
                assertEquals(expected, e.getMessage());
            }
        }
        assertFalse(registry.isFdmfConfigLoaded(DATA_DISC_01));
    }

    @Test
    public void concurrentFirstUseLoadsOneInstance() throws Exception {
        FdmfRegistry registry = new FdmfRegistry(new ValidatorConfigurationManager(CONFIG_DIR));
        ExecutorService executor = Executors.newFixedThreadPool(6);
        try {
            List<Future<FdmfConfiguration>> futures = new ArrayList<>();
            for (int i = 0; i < 6; i++) {
                futures.add(executor.submit((Callable<FdmfConfiguration>) () -> registry.getFdmfConfig(FUND_UNIT_02)));
            }
            FdmfConfiguration first = futures.get(0).get();
            for (Future<FdmfConfiguration> future : futures) {
                assertSame(first, future.get());
            }
        } finally {
            executor.shutdownNow();
        }
        assertEquals(1, registry.getLoadedFdmfConfigCount());
    }

    @Test
    public void configXsdIsCompiledOnceForAllFdmf() throws Exception {
        ValidatorConfigurationManager configManager = new ValidatorConfigurationManager(CONFIG_DIR);
        FdmfRegistry registry = new FdmfRegistry(configManager);
        registry.getFdmfConfig(FUND_UNIT_02);
        registry.getFdmfConfig(DATA_DISC_01);
        registry.getFdmfConfig(new Dmf(Dmf.Type.MONOGRAPH, "2.0"));
        //fdmfConfig.xsd a metadataProfile.xsd (binaryFileProfile.xsd az s profily binarnich souboru)
        assertEquals(2, configManager.getConfigSchemaCache().getCompilationCount());
    }

    private static String eagerLoadError(ValidatorConfigurationManager configManager, File fdmfDir) {
        try {
            new FdmfConfiguration(fdmfDir, configManager.getFdmfConfigXsd(), configManager.getBinaryFileProfileXsd(), configManager.getMetadataProfileXsd());
            fail("rozbita konfigurace mela vyhodit ValidatorConfigurationException");
            return null;
        } catch (ValidatorConfigurationException e) {
            return e.getMessage();
        }
    }

    /**
     * Kopie konfigurace validatoru jen s vybranymi fDMF.
     */
    private File configWith(String... fdmfDirNames) throws Exception {
        File target = tmp.newFolder("validatorConfig");
        copyDir(new File(CONFIG_DIR, "xsd"), new File(target, "xsd"));
        copyDir(new File(CONFIG_DIR, "dictionaries"), new File(target, "dictionaries"));
        Files.copy(new File(CONFIG_DIR, "externalUtils.xml").toPath(), new File(target, "externalUtils.xml").toPath());
        Files.copy(new File(CONFIG_DIR, ConfigManifest.FILE_NAME).toPath(), new File(target, ConfigManifest.FILE_NAME).toPath());
        for (String name : fdmfDirNames) {
            copyDir(new File(CONFIG_DIR, "fDMF/" + name), new File(target, "fDMF/" + name));
        }
        return target;
    }

    private static String read(File file) throws Exception {
        return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
    }

    private static void copyDir(File source, File target) throws Exception {
        try (Stream<Path> paths = Files.walk(source.toPath())) {
            for (Path path : (Iterable<Path>) paths::iterator) {
                Path copy = target.toPath().resolve(source.toPath().relativize(path).toString());
                if (Files.isDirectory(path)) {
                    Files.createDirectories(copy);
                } else {
                    Files.copy(path, copy);
                }
            }
        }
    }
}
