package nkp.pspValidator.gui.group;

import nkp.pspValidator.gui.results.ProblemCounts;
import nkp.pspValidator.shared.Dmf;
import org.junit.Test;

import java.io.File;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.*;

/**
 * Řádek s vybraným balíkem skupiny a hláška místo jeho výsledků.
 */
public class GroupDetailTextsTest {

    private static GroupRun run(String... names) {
        GroupPackages.Entry[] entries = new GroupPackages.Entry[names.length];
        for (int i = 0; i < names.length; i++) {
            entries[i] = new GroupPackages.Entry(new File("/d", names[i]), names[i].endsWith(".zip"));
        }
        return new GroupRun(new GroupPackages.Listing(new File("/d"), Arrays.asList(entries), Collections.<String>emptyList()));
    }

    @Test
    public void subtitles() {
        GroupRun run = run("a", "b", "c");
        GroupItem a = run.getItems().get(0);
        GroupItem b = run.getItems().get(1);
        GroupItem c = run.getItems().get(2);
        assertEquals("čeká", GroupDetailTexts.subtitle(a));
        run.runStarted(run.getItems(), 0);
        run.packageStarted(a);
        assertEquals("validuje se", GroupDetailTexts.subtitle(a));
        run.packageResolved(a, new Dmf(Dmf.Type.MONOGRAPH, "2.2"));
        assertEquals("Monografie 2.2, validuje se", GroupDetailTexts.subtitle(a));
        run.packageFinished(a, new Dmf(Dmf.Type.MONOGRAPH, "2.2"), new ProblemCounts(12, 3, 5), false, 3400, null, null);
        assertEquals("Monografie 2.2, nevalidní, 12 chyb, 3 varování, 5 informací, trvalo 3,4 s", GroupDetailTexts.subtitle(a));
        run.packageStarted(b);
        run.packageFailed(b, null, null, "není balík", 200);
        assertEquals("chyba, trvalo 0,2 s: není balík", GroupDetailTexts.subtitle(b));
        run.packageStarted(c);
        run.packageFinished(c, new Dmf(Dmf.Type.FUND_UNIT, "0.2"), new ProblemCounts(0, 1, 1), true, 100, null, null);
        assertEquals("Jednotky fondu 0.2, validní, 0 chyb, 1 varování, 1 informace, trvalo 0,1 s", GroupDetailTexts.subtitle(c));
    }

    @Test
    public void messages() {
        GroupRun run = run("a", "b.zip", "c", "d");
        GroupItem a = run.getItems().get(0);
        GroupItem zip = run.getItems().get(1);
        GroupItem c = run.getItems().get(2);
        GroupItem d = run.getItems().get(3);
        assertEquals("Balík čeká na validaci.", GroupDetailTexts.message(a, false, false));
        //s podrobnostmi zadna hlaska
        assertNull(GroupDetailTexts.message(a, true, false));
        run.runStarted(run.getItems(), 0);
        run.packageStarted(a);
        assertEquals("Připravuji validaci balíku…", GroupDetailTexts.message(a, false, false));
        run.packageFailed(a, null, null, "nenalezen soubor PRIMARY-METS", 1);
        assertEquals("Balík nešlo validovat: nenalezen soubor PRIMARY-METS", GroupDetailTexts.message(a, false, false));
        run.packageStarted(zip);
        assertEquals("Rozbaluji soubor ZIP a připravuji validaci balíku…", GroupDetailTexts.message(zip, false, false));
        run.packageFinished(zip, new Dmf(Dmf.Type.MONOGRAPH, "2.2"), ProblemCounts.ZERO, true, 1, null, null);
        assertEquals("Podrobné výsledky balíku nejsou k dispozici.", GroupDetailTexts.message(zip, false, false));
        assertTrue(GroupDetailTexts.message(zip, false, true).startsWith("Podrobné výsledky tohoto balíku byly uvolněny z paměti"));
        //zahozeni ma prednost jen bez podrobnosti
        assertNull(GroupDetailTexts.message(zip, true, true));
        run.packageStarted(c);
        run.cancelRequested();
        run.packageCanceled(c, null, null, 1);
        run.runEnded(10, LocalDateTime.now());
        assertEquals("Validace balíku byla zrušena před kontrolou prvního pravidla.", GroupDetailTexts.message(c, false, false));
        assertEquals("Balík se nevalidoval, validace skupiny byla zrušena dřív, než na něj došla řada.", GroupDetailTexts.message(d, false, false));
    }
}
