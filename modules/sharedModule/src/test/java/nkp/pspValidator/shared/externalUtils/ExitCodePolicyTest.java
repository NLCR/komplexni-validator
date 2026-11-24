package nkp.pspValidator.shared.externalUtils;

import nkp.pspValidator.shared.OperatingSystem;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.Assert.*;

/**
 * Posouzení návratového kódu nástroje ({@link ExitCodePolicy}): pád a nespuštění nástroje podle platformy z konfigurace
 * (ne podle systému, na kterém test běží) a neobvyklý kód podle množiny běžných kódů. Očekávané texty jsou ručně
 * podle zadání (kódy signálů Unixu 128 + n, kódy NTSTATUS Windows).
 */
public class ExitCodePolicyTest {

    private static Set<Integer> codes(Integer... codes) {
        return new HashSet<>(Arrays.asList(codes));
    }

    // Linux a macOS s množinou běžných kódů (Kakadu: 0 a 255)

    @Test
    public void unixNormalCodesAreNotFailure() {
        for (OperatingSystem os : new OperatingSystem[]{OperatingSystem.LINUX, OperatingSystem.MAC}) {
            ExitCodePolicy policy = new ExitCodePolicy(os, codes(0, 255));
            assertNull(os.name(), policy.describeFailure(0));
            assertNull(os.name(), policy.describeFailure(255));
        }
    }

    @Test
    public void unixUnexpectedCodeIsFailure() {
        ExitCodePolicy policy = new ExitCodePolicy(OperatingSystem.LINUX, codes(255, 0));
        assertEquals("skončil s kódem 1 (neočekávaný kód, běžné jsou 0, 255)", policy.describeFailure(1));
        assertEquals("skončil s kódem 254 (neočekávaný kód, běžné jsou 0, 255)", policy.describeFailure(254));
        assertEquals("skončil s kódem 128 (neočekávaný kód, běžné jsou 0, 255)", policy.describeFailure(128));
        assertEquals("skončil s kódem -1 (neočekávaný kód, běžné jsou 0, 255)", policy.describeFailure(-1));
    }

    @Test
    public void unixSignalsAreCrash() {
        ExitCodePolicy policy = new ExitCodePolicy(OperatingSystem.MAC, codes(0, 255));
        assertEquals("skončil s kódem 134 (pád nástroje, signál 6 SIGABRT)", policy.describeFailure(134));
        assertEquals("skončil s kódem 139 (pád nástroje, signál 11 SIGSEGV)", policy.describeFailure(139));
        assertEquals("skončil s kódem 137 (pád nástroje, signál 9 SIGKILL)", policy.describeFailure(137));
        assertEquals("skončil s kódem 143 (pád nástroje, signál 15 SIGTERM)", policy.describeFailure(143));
        // kraje rozsahu 129–159 (signály 1–31)
        assertEquals("skončil s kódem 129 (pád nástroje, signál 1)", policy.describeFailure(129));
        assertEquals("skončil s kódem 159 (pád nástroje, signál 31)", policy.describeFailure(159));
    }

    @Test
    public void unixCannotRunIsFailure() {
        ExitCodePolicy policy = new ExitCodePolicy(OperatingSystem.LINUX, codes(0, 1));
        assertEquals("skončil s kódem 127 (nástroj nebo jeho knihovnu nejde spustit)", policy.describeFailure(127));
        assertEquals("skončil s kódem 126 (nástroj nebo jeho knihovnu nejde spustit)", policy.describeFailure(126));
    }

    @Test
    public void unixCrashIsFailureEvenWhenListedAsNormal() {
        // pád má přednost: ani chybně uvedený kód 134 mezi běžnými pád nezakryje
        ExitCodePolicy policy = new ExitCodePolicy(OperatingSystem.LINUX, codes(0, 134));
        assertEquals("skončil s kódem 134 (pád nástroje, signál 6 SIGABRT)", policy.describeFailure(134));
    }

    // Linux a macOS bez množiny běžných kódů: jen pád a nespuštění

    @Test
    public void unixWithoutNormalCodesOnlyCrash() {
        ExitCodePolicy policy = new ExitCodePolicy(OperatingSystem.LINUX, null);
        assertNull(policy.getNormalCodes());
        assertNull(policy.describeFailure(0));
        assertNull(policy.describeFailure(1));
        assertNull(policy.describeFailure(125));
        assertEquals("skončil s kódem 126 (nástroj nebo jeho knihovnu nejde spustit)", policy.describeFailure(126));
        assertNull(policy.describeFailure(128));
        assertEquals("skončil s kódem 129 (pád nástroje, signál 1)", policy.describeFailure(129));
        assertEquals("skončil s kódem 159 (pád nástroje, signál 31)", policy.describeFailure(159));
        assertNull(policy.describeFailure(160));
        assertNull(policy.describeFailure(255));
        assertNull(policy.describeFailure(-1));
    }

    // Windows: jen kódy NTSTATUS s úrovní chyba (0xC0000000–0xEFFFFFFF), Java je vrací jako záporná čísla

    @Test
    public void windowsNtStatusErrorsAreCrash() {
        ExitCodePolicy policy = new ExitCodePolicy(OperatingSystem.WINDOWS, null);
        // 0xC0000005 = -1073741819 (přístup do paměti), 0xC0000409 = -1073740791 (abort() v novějším MSVC)
        assertEquals("skončil s kódem 0xC0000005 (pád nástroje)", policy.describeFailure(-1073741819));
        assertEquals("skončil s kódem 0xC0000409 (pád nástroje)", policy.describeFailure(-1073740791));
        // 0xC0000135 = -1073741515 (chybí DLL)
        assertEquals("skončil s kódem 0xC0000135 (chybí knihovna DLL nástroje)", policy.describeFailure(-1073741515));
        // 0xE06D7363 = -529697949 (neošetřená výjimka C++)
        assertEquals("skončil s kódem 0xE06D7363 (pád nástroje)", policy.describeFailure(-529697949));
        // kraje rozsahu: 0xC0000000 = -1073741824, 0xEFFFFFFF = -268435457
        assertEquals("skončil s kódem 0xC0000000 (pád nástroje)", policy.describeFailure(-1073741824));
        assertEquals("skončil s kódem 0xEFFFFFFF (pád nástroje)", policy.describeFailure(-268435457));
    }

    @Test
    public void windowsOtherCodesAreNotCrash() {
        ExitCodePolicy policy = new ExitCodePolicy(OperatingSystem.WINDOWS, null);
        assertNull(policy.describeFailure(0));
        assertNull(policy.describeFailure(1));
        // kdu_expand.exe při chybě (exit(-1)), ověřeno na Windows 23. 11. 2026
        assertNull(policy.describeFailure(-1));
        // signály Unixu na Windows nejsou
        assertNull(policy.describeFailure(134));
        assertNull(policy.describeFailure(127));
        // těsně mimo rozsah: 0xBFFFFFFF = -1073741825, 0xF0000000 = -268435456
        assertNull(policy.describeFailure(-1073741825));
        assertNull(policy.describeFailure(-268435456));
        // malá záporná čísla vracejí nástroje samy (exit(-2) = 0xFFFFFFFE)
        assertNull(policy.describeFailure(-2));
        // varování NTSTATUS 0x80000003 (breakpoint) = -2147483645 se za pád nepovažuje
        assertNull(policy.describeFailure(-2147483645));
    }

    @Test
    public void windowsWithNormalCodes() {
        ExitCodePolicy policy = new ExitCodePolicy(OperatingSystem.WINDOWS, codes(0, -1));
        assertNull(policy.describeFailure(-1));
        assertEquals("skončil s kódem 1 (neočekávaný kód, běžné jsou -1, 0)", policy.describeFailure(1));
        assertEquals("skončil s kódem 0xC0000005 (pád nástroje)", policy.describeFailure(-1073741819));
    }

    // atribut normal

    @Test
    public void parseCodes() {
        assertEquals(new TreeSet<>(Arrays.asList(0, 1)), ExitCodePolicy.parseCodes("0 1"));
        assertEquals(new TreeSet<>(Arrays.asList(0, 1, 7, 8)), ExitCodePolicy.parseCodes(" 0,1  7 ,8 "));
        assertEquals(new TreeSet<>(Arrays.asList(-1, 0)), ExitCodePolicy.parseCodes("-1\t0"));
        assertEquals(new TreeSet<>(Arrays.asList(255)), ExitCodePolicy.parseCodes("255 255"));
        assertNull(ExitCodePolicy.parseCodes(""));
        assertNull(ExitCodePolicy.parseCodes("   "));
        assertNull(ExitCodePolicy.parseCodes(null));
    }

    @Test
    public void parseCodesRejectsNonIntegers() {
        for (String value : new String[]{"0 x", "1.5", "0;1", "0x1", "99999999999"}) {
            try {
                ExitCodePolicy.parseCodes(value);
                fail(value);
            } catch (IllegalArgumentException e) {
                assertTrue(value + ": " + e.getMessage(), e.getMessage().startsWith("neplatný návratový kód '"));
            }
        }
    }
}
