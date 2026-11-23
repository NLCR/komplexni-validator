package nkp.pspValidator.shared.externalUtils;

import nkp.pspValidator.shared.OperatingSystem;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Proměnná prostředí s cestou ke sdíleným knihovnám pro spouštěný externí nástroj.
 * <p>
 * Nástroj dodaný se svými knihovnami mimo systémové adresáře (typicky Kakadu: program v {@code bin/<platforma>},
 * knihovny v {@code lib/<platforma>}) je při startu nenajde, pokud mu cestu neřekne proměnná prostředí
 * ({@code DYLD_LIBRARY_PATH} na macOS, {@code LD_LIBRARY_PATH} na Linuxu). Uživatel ji mívá nastavenou v terminálu, ale
 * aplikace spuštěná z Finderu ji nemá a macOS ji navíc zahazuje při spouštění chráněných programů (shell, přes který
 * běží Gradle nebo spouštěcí skripty). Validátor adresář nástroje zná, proto proměnnou nastaví sám, jen pro spouštěný
 * proces. Na Windows se knihovny hledají vedle programu, tam se nic nenastavuje.
 * <p>
 * Na macOS se nastavuje ZÁLOŽNÍ cesta {@code DYLD_FALLBACK_LIBRARY_PATH}: použije se, jen když knihovna není na svém
 * místě, a nepřebíjí tedy systémové knihovny. {@code DYLD_LIBRARY_PATH} má přednost před vším a cizí knihovny v ní
 * rozbíjejí jiné programy (Java spuštěná s adresářem knihoven Homebrew v této proměnné spadne na neslučitelné libzip),
 * což by postihlo nástroje spouštěné přes Javu (JHOVE, veraPDF, EPUBCheck). Linux záložní cestu nemá, proto se tam
 * přidávají jen adresáře patřící nástroji, žádné obecné adresáře knihoven.
 */
public final class LibraryPathEnvironment {

    public static final String MAC_VARIABLE = "DYLD_FALLBACK_LIBRARY_PATH";
    /** výchozí záložní cesta dyld (platí, když proměnná není nastavena); zachovává se za adresáři nástroje */
    static final String MAC_DEFAULT_FALLBACK_SUFFIX = "/usr/local/lib:/usr/lib";
    public static final String LINUX_VARIABLE = "LD_LIBRARY_PATH";

    private LibraryPathEnvironment() {
    }

    /** Název proměnné pro daný systém, nebo null, když se na něm nic nenastavuje (Windows, neznámý systém). */
    public static String variableName(OperatingSystem os) {
        if (os == OperatingSystem.MAC) {
            return MAC_VARIABLE;
        } else if (os == OperatingSystem.LINUX) {
            return LINUX_VARIABLE;
        }
        return null;
    }

    /**
     * Adresáře, ve kterých mohou být knihovny nástroje, v pořadí hledání; jen ty, které existují:
     * adresář nástroje, jeho podadresář {@code lib} a {@code lib/<název adresáře nástroje>} o dvě úrovně výš
     * (rozložení Kakadu: {@code bin/Mac-arm-64-gcc} a {@code lib/Mac-arm-64-gcc}). Sousední obecný adresář
     * {@code lib} ({@code /opt/homebrew/bin} a {@code /opt/homebrew/lib}, {@code /usr/bin} a {@code /usr/lib}) se
     * záměrně nepřidává: obsahuje knihovny jiných programů.
     */
    public static List<File> libraryDirs(File utilDir) {
        if (utilDir == null) {
            return Collections.emptyList();
        }
        File dir = utilDir.getAbsoluteFile();
        List<File> candidates = new ArrayList<>();
        candidates.add(dir);
        candidates.add(new File(dir, "lib"));
        File parent = dir.getParentFile();
        File grandParent = parent == null ? null : parent.getParentFile();
        if (grandParent != null) {
            candidates.add(new File(new File(grandParent, "lib"), dir.getName()));
        }
        List<File> result = new ArrayList<>();
        for (File candidate : candidates) {
            if (candidate.isDirectory() && !result.contains(candidate)) {
                result.add(candidate);
            }
        }
        return result;
    }

    /**
     * Proměnné, které se mají spouštěnému nástroji nastavit navíc (prázdná mapa, když nic).
     *
     * @param utilDir      adresář nástroje zadaný uživatelem, nebo null (nástroj se hledá v PATH: nic se nenastavuje)
     * @param currentValue dosavadní hodnota proměnné v prostředí validátoru, nebo null; zůstává na začátku, adresáře
     *                     nástroje se připojí za ni. Na macOS bez dosavadní hodnoty se za adresáře nástroje připojí
     *                     výchozí záložní cesta systému, kterou by nastavení proměnné jinak zrušilo.
     */
    public static Map<String, String> forUtil(OperatingSystem os, File utilDir, String currentValue) {
        String variable = variableName(os);
        List<File> dirs = libraryDirs(utilDir);
        if (variable == null || dirs.isEmpty()) {
            return Collections.emptyMap();
        }
        boolean hasCurrent = currentValue != null && !currentValue.trim().isEmpty();
        StringBuilder value = new StringBuilder();
        if (hasCurrent) {
            value.append(currentValue);
        }
        //oddelovac ':' plati na macOS i Linuxu (na Windows se nic nenastavuje)
        for (File dir : dirs) {
            if (value.length() > 0) {
                value.append(':');
            }
            value.append(dir.getPath());
        }
        if (os == OperatingSystem.MAC && !hasCurrent) {
            value.append(':').append(MAC_DEFAULT_FALLBACK_SUFFIX);
        }
        Map<String, String> result = new LinkedHashMap<>();
        result.put(variable, value.toString());
        return result;
    }
}
