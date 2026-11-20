package nkp.pspValidator.gui.settings;

import nkp.pspValidator.gui.ConfigurationManager;
import nkp.pspValidator.gui.GuiTestSupport;
import nkp.pspValidator.shared.externalUtils.ExternalUtil;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import static nkp.pspValidator.shared.externalUtils.ExternalUtil.*;
import static org.junit.Assert.*;

/**
 * Cesty k externím nástrojům v tabulce nástrojů: co se zapíše do nastavení a jakou cestu dostane manažer nástrojů
 * v režimu „hned“ (dialog při startu) a „až Uložit“ (Nastavení), včetně zahození neuložených cest.
 */
public class ToolPathsDraftTest {

    /** Klíče nastavení s adresáři nástrojů (ConfigurationManager.PROP_*_DIR), napsané ručně. */
    private static final String JHOVE_KEY = "jhove.dir";
    private static final String KAKADU_KEY = "kakadu.dir";

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private ConfigurationManager config;
    /** volání PathTarget v pořadí: „NÁSTROJ=cesta“ */
    private final List<String> applied = new ArrayList<>();
    private File savedKakadu;

    @Before
    public void setUp() throws Exception {
        config = GuiTestSupport.configurationManager(tmp.newFolder("root"));
        savedKakadu = tmp.newFolder("kakadu-ulozeny");
        config.setFile(KAKADU_KEY, savedKakadu);
    }

    private ToolPathsDraft.PathTarget target() {
        return (util, path) -> applied.add(util + "=" + (path == null ? "null" : path.getName()));
    }

    @Test
    public void keysAreTheOnesUsedSoFar() {
        assertEquals(JHOVE_KEY, ConfigurationManager.PROP_JHOVE_DIR);
        assertEquals(KAKADU_KEY, ConfigurationManager.PROP_KAKADU_DIR);
    }

    @Test
    public void startsWithSavedPaths() throws Exception {
        ToolPathsDraft draft = ToolPathsDraft.deferred(config, target());
        assertEquals(savedKakadu.getCanonicalFile(), draft.getPath(KAKADU));
        assertNull(draft.getPath(JHOVE));
        assertFalse(draft.isChanged());
        assertEquals(Collections.emptySet(), draft.getChanged());
        //vytvoreni konceptu manazer nemeni (ten uz ma ulozene cesty od startu aplikace)
        assertEquals(Collections.emptyList(), applied);
    }

    @Test
    public void deferredSetIsAppliedButNotWritten() throws Exception {
        File dir = tmp.newFolder("jhove-novy");
        ToolPathsDraft draft = ToolPathsDraft.deferred(config, target());
        draft.setPath(JHOVE, dir);
        assertEquals(Arrays.asList("JHOVE=jhove-novy"), applied);
        assertEquals(dir, draft.getPath(JHOVE));
        assertNull("do uložení se nic nezapíše", config.getStringOrDefault(JHOVE_KEY, null));
        assertTrue(draft.isChanged());
        assertEquals(EnumSet.of(JHOVE), draft.getChanged());
    }

    @Test
    public void deferredSaveWritesOnlyChanged() throws Exception {
        File dir = tmp.newFolder("jhove-novy");
        ToolPathsDraft draft = ToolPathsDraft.deferred(config, target());
        draft.setPath(JHOVE, dir);
        draft.save();
        assertEquals(dir.getCanonicalPath(), config.getStringOrDefault(JHOVE_KEY, null));
        assertEquals(savedKakadu.getCanonicalPath(), config.getStringOrDefault(KAKADU_KEY, null));
        assertNull(config.getStringOrDefault("imageMagick.dir", null));
        assertFalse(draft.isChanged());
        //po ulozeni uz neni co zahodit
        assertEquals(Collections.emptySet(), draft.discard());
        assertEquals(dir, draft.getPath(JHOVE));
        assertEquals(Arrays.asList("JHOVE=jhove-novy"), applied);
    }

    /** Uložení nepřepíše klíče nástrojů, které se v konceptu neměnily (mohly se mezitím změnit jinde). */
    @Test
    public void saveDoesNotOverwriteUnchangedKeys() throws Exception {
        ToolPathsDraft draft = ToolPathsDraft.deferred(config, target());
        File elsewhere = tmp.newFolder("kakadu-jinde");
        config.setFile(KAKADU_KEY, elsewhere);
        draft.setPath(JHOVE, tmp.newFolder("jhove-novy"));
        draft.save();
        assertEquals(elsewhere.getCanonicalPath(), config.getStringOrDefault(KAKADU_KEY, null));
    }

    @Test
    public void deferredRemoveAndSave() {
        ToolPathsDraft draft = ToolPathsDraft.deferred(config, target());
        draft.setPath(KAKADU, null);
        assertEquals(Arrays.asList("KAKADU=null"), applied);
        assertNotNull("zrušení se do uložení nezapíše", config.getStringOrDefault(KAKADU_KEY, null));
        assertEquals(EnumSet.of(KAKADU), draft.getChanged());
        draft.save();
        assertNull(config.getStringOrDefault(KAKADU_KEY, null));
        assertNull(draft.getPath(KAKADU));
    }

    @Test
    public void discardRestoresSavedPathsInManager() throws Exception {
        File jhove = tmp.newFolder("jhove-novy");
        File kakadu = tmp.newFolder("kakadu-novy");
        ToolPathsDraft draft = ToolPathsDraft.deferred(config, target());
        draft.setPath(JHOVE, jhove);
        draft.setPath(KAKADU, kakadu);
        applied.clear();
        assertEquals(EnumSet.of(JHOVE, KAKADU), draft.discard());
        //manazer dostane zpet ulozene cesty (JHOVE nenastaven, Kakadu puvodni), v poradi vyctu
        assertEquals(Arrays.asList("JHOVE=null", "KAKADU=kakadu-ulozeny"), applied);
        assertNull(draft.getPath(JHOVE));
        assertEquals(savedKakadu.getCanonicalFile(), draft.getPath(KAKADU));
        assertNull(config.getStringOrDefault(JHOVE_KEY, null));
        assertEquals(savedKakadu.getCanonicalPath(), config.getStringOrDefault(KAKADU_KEY, null));
        assertFalse(draft.isChanged());
    }

    @Test
    public void discardOfRemovedPathRestoresIt() throws Exception {
        ToolPathsDraft draft = ToolPathsDraft.deferred(config, target());
        draft.setPath(KAKADU, null);
        applied.clear();
        assertEquals(EnumSet.of(KAKADU), draft.discard());
        assertEquals(Arrays.asList("KAKADU=kakadu-ulozeny"), applied);
        assertEquals(savedKakadu.getCanonicalFile(), draft.getPath(KAKADU));
    }

    /** Nastavení a vrácení stejné cesty není změna; zahození pak manažer nemění a nic se nekontroluje znovu. */
    @Test
    public void settingBackTheSavedPathIsNoChange() throws Exception {
        File dir = tmp.newFolder("jhove-novy");
        ToolPathsDraft draft = ToolPathsDraft.deferred(config, target());
        draft.setPath(JHOVE, dir);
        draft.setPath(JHOVE, null);
        draft.setPath(KAKADU, savedKakadu.getCanonicalFile());
        assertFalse(draft.isChanged());
        applied.clear();
        assertEquals(Collections.emptySet(), draft.discard());
        assertEquals(Collections.emptyList(), applied);
    }

    @Test
    public void immediateWritesAtOnce() throws Exception {
        File dir = tmp.newFolder("jhove-novy");
        ToolPathsDraft draft = ToolPathsDraft.immediate(config, target());
        assertTrue(draft.isImmediate());
        draft.setPath(JHOVE, dir);
        assertEquals(dir.getCanonicalPath(), config.getStringOrDefault(JHOVE_KEY, null));
        draft.setPath(KAKADU, null);
        assertNull(config.getStringOrDefault(KAKADU_KEY, null));
        assertEquals(Arrays.asList("JHOVE=jhove-novy", "KAKADU=null"), applied);
        //v rezimu hned neni neulozena zmena a zahozeni nic nevraci
        assertFalse(draft.isChanged());
        applied.clear();
        assertEquals(Collections.emptySet(), draft.discard());
        assertEquals(Collections.emptyList(), applied);
        assertEquals(dir, draft.getPath(JHOVE));
        assertNull(draft.getPath(KAKADU));
    }

    @Test
    public void deferredIsNotImmediate() {
        assertFalse(ToolPathsDraft.deferred(config, target()).isImmediate());
    }

    @Test
    public void everyToolHasItsOwnKey() throws Exception {
        ToolPathsDraft draft = ToolPathsDraft.deferred(config, target());
        for (ExternalUtil util : ExternalUtil.values()) {
            draft.setPath(util, tmp.newFolder("d-" + util.name()));
        }
        assertEquals(EnumSet.allOf(ExternalUtil.class), draft.getChanged());
        draft.save();
        //9 nastroju, 9 ruznych klicu s vlastni cestou
        //v poradi vyctu ExternalUtil
        String[] keys = {"imageMagick.dir", "jhove.dir", "jpylyzer.dir", "kakadu.dir", "mp3val.dir", "shntool.dir",
                "checkmate.dir", "verapdf.dir", "epubcheck.dir"};
        for (int i = 0; i < keys.length; i++) {
            String value = config.getStringOrDefault(keys[i], null);
            assertNotNull(keys[i], value);
            assertTrue(keys[i] + " " + value, value.endsWith("d-" + ExternalUtil.values()[i].name()));
        }
    }
}
