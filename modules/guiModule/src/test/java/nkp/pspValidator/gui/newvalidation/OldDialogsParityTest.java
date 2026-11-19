package nkp.pspValidator.gui.newvalidation;

import nkp.pspValidator.gui.ConfigurationManager;
import nkp.pspValidator.gui.GuiTestSupport;
import nkp.pspValidator.gui.VersionComparator;
import nkp.pspValidator.shared.Dmf;
import nkp.pspValidator.shared.DmfDetector;
import nkp.pspValidator.shared.FdmfRegistry;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;

import static org.junit.Assert.*;

/**
 * Zafixování chování dřívějších dialogů PspDirValidationConfigurationDialogController
 * a PspZipValidationConfigurationDialogController (smazány ve 3.0, nahradil je dialog Nová validace):
 * <ul>
 *     <li>které klíče config.properties četly a zapisovaly (stará i nová verze aplikace sdílí nastavení),</li>
 *     <li>jak plnily DmfDetector.Params: pro každý typ vynucená verze = zaškrtnuté vynucení ? vybraná verze : null,
 *     totéž pro preferovanou; vybraná verze = uložená, pokud je mezi dostupnými, jinak první položka seznamu
 *     seřazeného od nejnovější (VersionComparator(true) v dialogu adresáře, na který se sjednotilo),</li>
 *     <li>výstupy: XML protokol a textový log výchozně vypnuté, podrobnost výchozně 3.</li>
 * </ul>
 * Referenční implementace níže je přepis kódu starého kontroleru (pole Params podle typu vypsaná ručně), nezávislý
 * na DmfVersionKeys a DmfParamsBuilder.
 */
public class OldDialogsParityTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private ConfigurationManager config;
    private FdmfRegistry registry;

    /** Řádek tabulky dřívějšího dialogu: klíče a pole Params pro jeden typ. */
    private static class OldRow {
        final Dmf.Type type;
        final String forcedEnabled, forcedCode, preferredEnabled, preferredCode;
        final String forcedEnabledLiteral, forcedCodeLiteral, preferredEnabledLiteral, preferredCodeLiteral;
        final Function<FdmfRegistry, Set<String>> versions;
        final BiConsumer<DmfDetector.Params, String> setForced, setPreferred;
        final Function<DmfDetector.Params, String> getForced, getPreferred;

        OldRow(Dmf.Type type, String forcedEnabled, String forcedCode, String preferredEnabled, String preferredCode,
               String codeInKey, Function<FdmfRegistry, Set<String>> versions,
               BiConsumer<DmfDetector.Params, String> setForced, BiConsumer<DmfDetector.Params, String> setPreferred,
               Function<DmfDetector.Params, String> getForced, Function<DmfDetector.Params, String> getPreferred) {
            this.type = type;
            this.forcedEnabled = forcedEnabled;
            this.forcedCode = forcedCode;
            this.preferredEnabled = preferredEnabled;
            this.preferredCode = preferredCode;
            this.forcedEnabledLiteral = "force." + codeInKey + ".version.enabled";
            this.forcedCodeLiteral = "force." + codeInKey + ".version.code";
            this.preferredEnabledLiteral = "prefer." + codeInKey + ".version.enabled";
            this.preferredCodeLiteral = "prefer." + codeInKey + ".version.code";
            this.versions = versions;
            this.setForced = setForced;
            this.setPreferred = setPreferred;
            this.getForced = getForced;
            this.getPreferred = getPreferred;
        }
    }

    private static final List<OldRow> OLD_ROWS = Arrays.asList(
            new OldRow(Dmf.Type.MONOGRAPH, ConfigurationManager.PROP_FORCE_MON_VERSION_ENABLED, ConfigurationManager.PROP_FORCE_MON_VERSION_CODE,
                    ConfigurationManager.PROP_PREFER_MON_VERSION_ENABLED, ConfigurationManager.PROP_PREFER_MON_VERSION_CODE, "monograph",
                    FdmfRegistry::getMonographFdmfVersions, (p, v) -> p.forcedDmfMonVersion = v, (p, v) -> p.preferredDmfMonVersion = v,
                    p -> p.forcedDmfMonVersion, p -> p.preferredDmfMonVersion),
            new OldRow(Dmf.Type.PERIODICAL, ConfigurationManager.PROP_FORCE_PER_VERSION_ENABLED, ConfigurationManager.PROP_FORCE_PER_VERSION_CODE,
                    ConfigurationManager.PROP_PREFER_PER_VERSION_ENABLED, ConfigurationManager.PROP_PREFER_PER_VERSION_CODE, "periodical",
                    FdmfRegistry::getPeriodicalFdmfVersions, (p, v) -> p.forcedDmfPerVersion = v, (p, v) -> p.preferredDmfPerVersion = v,
                    p -> p.forcedDmfPerVersion, p -> p.preferredDmfPerVersion),
            new OldRow(Dmf.Type.AUDIO_GRAM, ConfigurationManager.PROP_FORCE_AUDIO_GRAM_VERSION_ENABLED, ConfigurationManager.PROP_FORCE_AUDIO_GRAM_VERSION_CODE,
                    ConfigurationManager.PROP_PREFER_AUDIO_GRAM_VERSION_ENABLED, ConfigurationManager.PROP_PREFER_AUDIO_GRAM_VERSION_CODE, "audio_gram",
                    FdmfRegistry::getAudioGramFdmfVersions, (p, v) -> p.forcedDmfAdgVersion = v, (p, v) -> p.preferredDmfAdgVersion = v,
                    p -> p.forcedDmfAdgVersion, p -> p.preferredDmfAdgVersion),
            new OldRow(Dmf.Type.AUDIO_FONO, ConfigurationManager.PROP_FORCE_AUDIO_FONO_VERSION_ENABLED, ConfigurationManager.PROP_FORCE_AUDIO_FONO_VERSION_CODE,
                    ConfigurationManager.PROP_PREFER_AUDIO_FONO_VERSION_ENABLED, ConfigurationManager.PROP_PREFER_AUDIO_FONO_VERSION_CODE, "audio_fono",
                    FdmfRegistry::getAudioFonoFdmfVersions, (p, v) -> p.forcedDmfAdfVersion = v, (p, v) -> p.preferredDmfAdfVersion = v,
                    p -> p.forcedDmfAdfVersion, p -> p.preferredDmfAdfVersion),
            new OldRow(Dmf.Type.AUDIO_DISC, ConfigurationManager.PROP_FORCE_AUDIO_DISC_VERSION_ENABLED, ConfigurationManager.PROP_FORCE_AUDIO_DISC_VERSION_CODE,
                    ConfigurationManager.PROP_PREFER_AUDIO_DISC_VERSION_ENABLED, ConfigurationManager.PROP_PREFER_AUDIO_DISC_VERSION_CODE, "audio_disc",
                    FdmfRegistry::getAudioDiscFdmfVersions, (p, v) -> p.forcedDmfAdiVersion = v, (p, v) -> p.preferredDmfAdiVersion = v,
                    p -> p.forcedDmfAdiVersion, p -> p.preferredDmfAdiVersion),
            new OldRow(Dmf.Type.AUDIO_NO_CARRIER, ConfigurationManager.PROP_FORCE_AUDIO_NO_CARRIER_VERSION_ENABLED, ConfigurationManager.PROP_FORCE_AUDIO_NO_CARRIER_VERSION_CODE,
                    ConfigurationManager.PROP_PREFER_AUDIO_NO_CARRIER_VERSION_ENABLED, ConfigurationManager.PROP_PREFER_AUDIO_NO_CARRIER_VERSION_CODE, "audio_no_carrier",
                    FdmfRegistry::getAudioNoCarrierFdmfVersions, (p, v) -> p.forcedDmfAdnVersion = v, (p, v) -> p.preferredDmfAdnVersion = v,
                    p -> p.forcedDmfAdnVersion, p -> p.preferredDmfAdnVersion),
            new OldRow(Dmf.Type.DATA_DISC, ConfigurationManager.PROP_FORCE_DIGITAL_DISC_VERSION_ENABLED, ConfigurationManager.PROP_FORCE_DIGITAL_DISC_VERSION_CODE,
                    ConfigurationManager.PROP_PREFER_DIGITAL_DISC_VERSION_ENABLED, ConfigurationManager.PROP_PREFER_DIGITAL_DISC_VERSION_CODE, "digital_disc",
                    FdmfRegistry::getDataDiscFdmfVersions, (p, v) -> p.forcedDmfDadVersion = v, (p, v) -> p.preferredDmfDadVersion = v,
                    p -> p.forcedDmfDadVersion, p -> p.preferredDmfDadVersion),
            new OldRow(Dmf.Type.FUND_UNIT, ConfigurationManager.PROP_FORCE_FUND_UNIT_VERSION_ENABLED, ConfigurationManager.PROP_FORCE_FUND_UNIT_VERSION_CODE,
                    ConfigurationManager.PROP_PREFER_FUND_UNIT_VERSION_ENABLED, ConfigurationManager.PROP_PREFER_FUND_UNIT_VERSION_CODE, "fund_unit",
                    FdmfRegistry::getFundUnitFdmfVersions, (p, v) -> p.forcedDmfFduVersion = v, (p, v) -> p.preferredDmfFduVersion = v,
                    p -> p.forcedDmfFduVersion, p -> p.preferredDmfFduVersion),
            new OldRow(Dmf.Type.EMONOGRAPH, ConfigurationManager.PROP_FORCE_EMONOGRAPH_VERSION_ENABLED, ConfigurationManager.PROP_FORCE_EMONOGRAPH_VERSION_CODE,
                    ConfigurationManager.PROP_PREFER_EMONOGRAPH_VERSION_ENABLED, ConfigurationManager.PROP_PREFER_EMONOGRAPH_VERSION_CODE, "emonograph",
                    FdmfRegistry::getEmonographFdmfVersions, (p, v) -> p.forcedDmfEmoVersion = v, (p, v) -> p.preferredDmfEmoVersion = v,
                    p -> p.forcedDmfEmoVersion, p -> p.preferredDmfEmoVersion),
            new OldRow(Dmf.Type.EPERIODICAL, ConfigurationManager.PROP_FORCE_EPERIODICAL_VERSION_ENABLED, ConfigurationManager.PROP_FORCE_EPERIODICAL_VERSION_CODE,
                    ConfigurationManager.PROP_PREFER_EPERIODICAL_VERSION_ENABLED, ConfigurationManager.PROP_PREFER_EPERIODICAL_VERSION_CODE, "eperiodical",
                    FdmfRegistry::getEperiodicalFdmfVersions, (p, v) -> p.forcedDmfEpeVersion = v, (p, v) -> p.preferredDmfEpeVersion = v,
                    p -> p.forcedDmfEpeVersion, p -> p.preferredDmfEpeVersion),
            new OldRow(Dmf.Type.COMPERIODICAL, ConfigurationManager.PROP_FORCE_COMPERIODICAL_VERSION_ENABLED, ConfigurationManager.PROP_FORCE_COMPERIODICAL_VERSION_CODE,
                    ConfigurationManager.PROP_PREFER_COMPERIODICAL_VERSION_ENABLED, ConfigurationManager.PROP_PREFER_COMPERIODICAL_VERSION_CODE, "composed_perio",
                    FdmfRegistry::getComposedPeriodicalFdmfVersions, (p, v) -> p.forcedDmfSepVersion = v, (p, v) -> p.preferredDmfSepVersion = v,
                    p -> p.forcedDmfSepVersion, p -> p.preferredDmfSepVersion)
    );

    @Before
    public void setUp() throws Exception {
        config = GuiTestSupport.configurationManager(tmp.newFolder("cfg"));
        registry = GuiTestSupport.registry();
    }

    @Test
    public void tableCoversAllTypesOnce() {
        Set<Dmf.Type> types = EnumSet.noneOf(Dmf.Type.class);
        for (OldRow row : OLD_ROWS) {
            assertTrue("dvakrat " + row.type, types.add(row.type));
        }
        assertEquals(EnumSet.allOf(Dmf.Type.class), types);
    }

    @Test
    public void keysAreSameAsInOldDialogs() {
        for (OldRow row : OLD_ROWS) {
            DmfVersionKeys keys = DmfVersionKeys.of(row.type);
            assertEquals(row.type + " forcedEnabled", row.forcedEnabled, keys.forcedEnabled);
            assertEquals(row.type + " forcedCode", row.forcedCode, keys.forcedCode);
            assertEquals(row.type + " preferredEnabled", row.preferredEnabled, keys.preferredEnabled);
            assertEquals(row.type + " preferredCode", row.preferredCode, keys.preferredCode);
            //doslovne hodnoty: config.properties sdili stara i nova verze aplikace
            assertEquals(row.forcedEnabledLiteral, keys.forcedEnabled);
            assertEquals(row.forcedCodeLiteral, keys.forcedCode);
            assertEquals(row.preferredEnabledLiteral, keys.preferredEnabled);
            assertEquals(row.preferredCodeLiteral, keys.preferredCode);
        }
        assertEquals("last.psp.dir", ValidationSource.DIRECTORY.getLastPathKey());
        assertEquals("last.psp.zip", ValidationSource.ZIP.getLastPathKey());
        assertEquals("psp_validation.create_txt_log", ConfigurationManager.PROP_PSP_VALIDATION_CREATE_TXT_LOG);
        assertEquals("psp_validation.create_xml_log", ConfigurationManager.PROP_PSP_VALIDATION_CREATE_XML_LOG);
        assertEquals("text_log.verbosity", ConfigurationManager.PROP_TEXT_LOG_VERBOSITY);
    }

    /** Přepis plnění Params ze starého kontroleru (initChoiceBoxes + validate). */
    private DmfDetector.Params oldDialogParams() {
        DmfDetector.Params params = new DmfDetector.Params();
        for (OldRow row : OLD_ROWS) {
            List<String> versions = new ArrayList<>(row.versions.apply(registry));
            Collections.sort(versions, new VersionComparator(true));
            row.setForced.accept(params, config.getBooleanOrDefault(row.forcedEnabled, false)
                    ? selectedInChoiceBox(versions, config.getStringOrDefault(row.forcedCode, null)) : null);
            row.setPreferred.accept(params, config.getBooleanOrDefault(row.preferredEnabled, false)
                    ? selectedInChoiceBox(versions, config.getStringOrDefault(row.preferredCode, null)) : null);
        }
        return params;
    }

    private static String selectedInChoiceBox(List<String> versions, String stored) {
        if (stored != null && versions.contains(stored)) {
            return stored;
        }
        return versions.isEmpty() ? null : versions.get(0); //selectFirst()
    }

    private void assertSameParams(DmfDetector.Params expected, DmfDetector.Params actual) {
        for (OldRow row : OLD_ROWS) {
            assertEquals(row.type + " forced", row.getForced.apply(expected), row.getForced.apply(actual));
            assertEquals(row.type + " preferred", row.getPreferred.apply(expected), row.getPreferred.apply(actual));
        }
    }

    @Test
    public void emptyConfigGivesNoForcedNoPreferred() {
        DmfDetector.Params params = DmfParamsBuilder.build(new VersionSettings(config, registry));
        for (OldRow row : OLD_ROWS) {
            assertNull(row.getForced.apply(params));
            assertNull(row.getPreferred.apply(params));
        }
        assertSameParams(oldDialogParams(), params);
    }

    @Test
    public void eachTypeLandsInItsOwnParamsField() {
        //pro kazdy typ zvlast zapnout vynuceni a preferenci nejstarsi verze, ostatni typy musi zustat null
        for (OldRow row : OLD_ROWS) {
            ConfigurationManager fresh;
            try {
                fresh = GuiTestSupport.configurationManager(tmp.newFolder("cfg-" + row.type));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            List<String> versions = DocumentTypes.availableVersions(registry, row.type);
            String oldest = versions.get(versions.size() - 1);
            fresh.setBoolean(row.forcedEnabled, true);
            fresh.setString(row.forcedCode, oldest);
            fresh.setBoolean(row.preferredEnabled, true);
            fresh.setString(row.preferredCode, oldest);
            DmfDetector.Params params = DmfParamsBuilder.build(new VersionSettings(fresh, registry));
            for (OldRow other : OLD_ROWS) {
                if (other == row) {
                    assertEquals(row.type + " forced", oldest, other.getForced.apply(params));
                    assertEquals(row.type + " preferred", oldest, other.getPreferred.apply(params));
                } else {
                    assertNull(row.type + " -> " + other.type, other.getForced.apply(params));
                    assertNull(row.type + " -> " + other.type, other.getPreferred.apply(params));
                }
            }
        }
    }

    @Test
    public void storedVersionMissingOrUnknownFallsBackToNewest() {
        config.setBoolean(ConfigurationManager.PROP_FORCE_MON_VERSION_ENABLED, true);
        config.setString(ConfigurationManager.PROP_FORCE_MON_VERSION_CODE, "9.9");
        config.setBoolean(ConfigurationManager.PROP_PREFER_FUND_UNIT_VERSION_ENABLED, true);
        //kod preferovane verze chybi uplne
        DmfDetector.Params params = DmfParamsBuilder.build(new VersionSettings(config, registry));
        assertEquals("2.3", params.forcedDmfMonVersion);
        assertEquals("0.2", params.preferredDmfFduVersion);
        assertSameParams(oldDialogParams(), params);
    }

    @Test
    public void disabledForcingIgnoresStoredVersion() {
        config.setBoolean(ConfigurationManager.PROP_FORCE_PER_VERSION_ENABLED, false);
        config.setString(ConfigurationManager.PROP_FORCE_PER_VERSION_CODE, "1.6");
        DmfDetector.Params params = DmfParamsBuilder.build(new VersionSettings(config, registry));
        assertNull(params.forcedDmfPerVersion);
        assertSameParams(oldDialogParams(), params);
    }

    /** Náhodná nastavení (zapnuto/vypnuto, platná, neplatná a chybějící verze) dají stejné Params jako starý dialog. */
    @Test
    public void randomConfigurationsGiveSameParamsAsOldDialog() throws Exception {
        Random random = new Random(20261119);
        for (int round = 0; round < 40; round++) {
            ConfigurationManager fresh = GuiTestSupport.configurationManager(tmp.newFolder("random-" + round));
            config = fresh;
            for (OldRow row : OLD_ROWS) {
                List<String> versions = DocumentTypes.availableVersions(registry, row.type);
                randomFlagAndCode(random, row.forcedEnabled, row.forcedCode, versions);
                randomFlagAndCode(random, row.preferredEnabled, row.preferredCode, versions);
            }
            assertSameParams(oldDialogParams(), DmfParamsBuilder.build(new VersionSettings(config, registry)));
        }
    }

    private void randomFlagAndCode(Random random, String enabledKey, String codeKey, List<String> versions) {
        switch (random.nextInt(3)) {
            case 0:
                break; //klic chybi
            case 1:
                config.setBoolean(enabledKey, true);
                break;
            default:
                config.setBoolean(enabledKey, false);
        }
        switch (random.nextInt(3)) {
            case 0:
                break; //kod chybi
            case 1:
                config.setString(codeKey, versions.get(random.nextInt(versions.size())));
                break;
            default:
                config.setString(codeKey, "0.0.9");
        }
    }

    @Test
    public void forcingFromNewDialogWritesOldKeys() {
        VersionSettings settings = new VersionSettings(config, registry);
        settings.setForcedVersion(Dmf.Type.FUND_UNIT, "0.1");
        assertTrue(config.getBooleanOrDefault("force.fund_unit.version.enabled", false));
        assertEquals("0.1", config.getStringOrDefault("force.fund_unit.version.code", null));
        assertEquals("0.1", oldDialogParams().forcedDmfFduVersion);
        settings.setForcedVersion(Dmf.Type.FUND_UNIT, null);
        assertFalse(config.getBooleanOrDefault("force.fund_unit.version.enabled", true));
        //vybrana verze zustava ulozena, stejne jako po odskrtnuti ve starem dialogu
        assertEquals("0.1", config.getStringOrDefault("force.fund_unit.version.code", null));
        assertNull(oldDialogParams().forcedDmfFduVersion);
    }

    @Test
    public void outputsDefaultsAndKeysAsOldDialogs() {
        OutputSettings outputs = new OutputSettings(config);
        assertFalse(outputs.isCreateXmlLog());
        assertFalse(outputs.isCreateTxtLog());
        assertEquals(3, outputs.getVerbosity().getLevel());
        outputs.setCreateXmlLog(true);
        outputs.setCreateTxtLog(true);
        assertTrue(config.getBooleanOrDefault(ConfigurationManager.PROP_PSP_VALIDATION_CREATE_XML_LOG, false));
        assertTrue(config.getBooleanOrDefault(ConfigurationManager.PROP_PSP_VALIDATION_CREATE_TXT_LOG, false));
        for (int level = 0; level <= 3; level++) {
            config.setInteger(ConfigurationManager.PROP_TEXT_LOG_VERBOSITY, level);
            assertEquals(level, outputs.getVerbosity().getLevel());
        }
        //neznama hodnota: starý dialog nechal vybranou výchozí volbu „úplně vše“
        config.setInteger(ConfigurationManager.PROP_TEXT_LOG_VERBOSITY, 7);
        assertEquals(3, outputs.getVerbosity().getLevel());
        outputs.setVerbosity(OutputSettings.Verbosity.SECTIONS_WITH_ERRORS);
        assertEquals(Integer.valueOf(1), config.getIntegerOrNull(ConfigurationManager.PROP_TEXT_LOG_VERBOSITY));
    }

    @Test
    public void verbosityLabelsAsOldToggleButtons() {
        assertEquals(Arrays.asList(3, 2, 1, 0), levels(OutputSettings.Verbosity.menuOrder()));
        assertEquals("úplně vše", OutputSettings.Verbosity.ofLevel(3).getLabel());
        assertEquals("jen sekce a pravidla obsahující chyby", OutputSettings.Verbosity.ofLevel(2).getLabel());
        assertEquals("jen sekce obsahující chyby", OutputSettings.Verbosity.ofLevel(1).getLabel());
        assertEquals("jen validní/nevalidní", OutputSettings.Verbosity.ofLevel(0).getLabel());
    }

    private static List<Integer> levels(List<OutputSettings.Verbosity> verbosities) {
        List<Integer> result = new ArrayList<>();
        for (OutputSettings.Verbosity verbosity : verbosities) {
            result.add(verbosity.getLevel());
        }
        return result;
    }
}
