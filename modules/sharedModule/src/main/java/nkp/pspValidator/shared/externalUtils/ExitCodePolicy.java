package nkp.pspValidator.shared.externalUtils;

import nkp.pspValidator.shared.OperatingSystem;

import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

/**
 * Posouzení návratového kódu nástroje spuštěného nad souborem (element exitCodes u spuštění v externalUtils.xml).
 * <p>
 * Kód, který znamená pád nebo nespuštění nástroje, je vždy selhání: na Linuxu a macOS 126 a 127 (nástroj nebo jeho
 * knihovnu nejde spustit) a 129–159 (ukončen signálem, kód 128 + číslo signálu, např. 134 SIGABRT, 139 SIGSEGV),
 * na Windows kódy NTSTATUS s úrovní chyba 0xC0000000–0xEFFFFFFF (např. 0xC0000005 pád, 0xC0000135 chybí DLL, 0xE06D7363 neošetřená výjimka C++; malá záporná čísla jako -1 = 0xFFFFFFFF vracejí nástroje samy);
 * Java je vrací jako záporná čísla. Když je navíc uvedena množina běžných kódů (atribut normal), je selháním i každý
 * jiný kód. Platforma se bere z konfigurace (operatingSystem), ne ze systému, na kterém validátor běží.
 */
public class ExitCodePolicy {

    private static final long NTSTATUS_ERROR_MIN = 0xC0000000L;
    private static final long NTSTATUS_ERROR_MAX = 0xEFFFFFFFL;
    private static final long NTSTATUS_DLL_NOT_FOUND = 0xC0000135L;

    private final OperatingSystem os;
    /** běžné kódy, nebo null (běžný je každý kód kromě pádu) */
    private final Set<Integer> normalCodes;

    public ExitCodePolicy(OperatingSystem os, Set<Integer> normalCodes) {
        this.os = os;
        this.normalCodes = normalCodes == null ? null : Collections.unmodifiableSet(new TreeSet<>(normalCodes));
    }

    /**
     * Množina běžných kódů z atributu normal (celá čísla oddělená mezerou nebo čárkou), null pro prázdnou hodnotu.
     *
     * @throws IllegalArgumentException když hodnota není seznam celých čísel
     */
    public static Set<Integer> parseCodes(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        Set<Integer> codes = new TreeSet<>();
        for (String token : value.trim().split("[\\s,]+")) {
            try {
                codes.add(Integer.valueOf(token));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("neplatný návratový kód '" + token + "'");
            }
        }
        return codes;
    }

    public Set<Integer> getNormalCodes() {
        return normalCodes;
    }

    public OperatingSystem getOs() {
        return os;
    }

    /**
     * Popis selhání pro hlášku („skončil s kódem 134 (pád nástroje, signál 6 SIGABRT)“), nebo null, když kód
     * selháním není.
     */
    public String describeFailure(int exitCode) {
        String crash = crashDescription(exitCode);
        if (crash != null) {
            return "skončil s kódem " + codeText(exitCode) + " (" + crash + ")";
        }
        if (normalCodes != null && !normalCodes.contains(exitCode)) {
            return "skončil s kódem " + codeText(exitCode) + " (neočekávaný kód, běžné jsou " + normalCodesText() + ")";
        }
        return null;
    }

    private String codeText(int exitCode) {
        if (os == OperatingSystem.WINDOWS && isNtStatusError(exitCode)) {
            return String.format("0x%08X", exitCode & 0xFFFFFFFFL);
        }
        return Integer.toString(exitCode);
    }

    private String normalCodesText() {
        StringBuilder text = new StringBuilder();
        for (Integer code : normalCodes) {
            if (text.length() > 0) {
                text.append(", ");
            }
            text.append(code);
        }
        return text.toString();
    }

    private String crashDescription(int exitCode) {
        if (os == OperatingSystem.WINDOWS) {
            if (!isNtStatusError(exitCode)) {
                return null;
            }
            return (exitCode & 0xFFFFFFFFL) == NTSTATUS_DLL_NOT_FOUND
                    ? "chybí knihovna DLL nástroje"
                    : "pád nástroje";
        }
        if (exitCode == 126 || exitCode == 127) {
            return "nástroj nebo jeho knihovnu nejde spustit";
        }
        if (exitCode >= 129 && exitCode <= 159) {
            int signal = exitCode - 128;
            String name = signalName(signal);
            return "pád nástroje, signál " + signal + (name == null ? "" : " " + name);
        }
        return null;
    }

    private static boolean isNtStatusError(int exitCode) {
        long unsigned = exitCode & 0xFFFFFFFFL;
        return unsigned >= NTSTATUS_ERROR_MIN && unsigned <= NTSTATUS_ERROR_MAX;
    }

    /** jen signály se stejným číslem na Linuxu i macOS */
    private static String signalName(int signal) {
        switch (signal) {
            case 6:
                return "SIGABRT";
            case 9:
                return "SIGKILL";
            case 11:
                return "SIGSEGV";
            case 15:
                return "SIGTERM";
            default:
                return null;
        }
    }

    @Override
    public String toString() {
        return "ExitCodePolicy{" +
                "os=" + os +
                ", normalCodes=" + normalCodes +
                '}';
    }
}
