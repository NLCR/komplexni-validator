package nkp.pspValidator.shared.engine.utils;

import java.io.File;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Hledání souboru balíku podle cesty z metadat (METS FLocat, položky INFO, řádky souboru CHECKSUM) se shodou
 * velikosti písmen na všech systémech.
 * <p>
 * Windows a macOS velikost písmen v názvech souborů nerozlišují, Linux ano. Dotaz na souborový systém
 * ({@code File.exists()}, otevření souboru) by tak odkaz {@code masterCopy/MC_0001.jp2} na soubor
 * {@code mastercopy/mc_0001.jp2} na Windows a macOS přijal a na Linuxu ne. Tato třída porovnává každou složku
 * cesty (adresáře i název souboru) se skutečnými názvy z výpisu adresáře, takže výsledek na systému souborů
 * nezávisí. Rozlišuje tři stavy: nalezen přesně, nalezen jen bez ohledu na velikost písmen (vrátí skutečnou
 * cestu), nenalezen. Kontroluje se jen část cesty uvnitř kořenového adresáře balíku; cesta mimo něj se
 * nekontroluje (stav {@link Status#OUTSIDE_ROOT}).
 * <p>
 * Názvy se porovnávají po normalizaci Unicode NFC, aby rozdíl ve skladbě znaků s diakritikou (NFC/NFD) nebyl
 * hlášen jako rozdíl ve velikosti písmen; to je jiná vada a tady se neřeší.
 * <p>
 * Výpis každého adresáře se pamatuje do {@link #clear()}. Instanci drží {@code Engine} a validátor ji vyprázdní
 * na začátku každého běhu validace (soubory se mezi běhy, např. opakovanou validací v GUI, mohou změnit).
 * Mapa je souběžná, třídu lze volat z více vláken.
 */
public class PackageFileLookup {

    public enum Status {
        /**
         * soubor nebo adresář existuje a cesta se shoduje včetně velikosti písmen
         */
        FOUND_EXACT,
        /**
         * existuje jen soubor nebo adresář, jehož cesta se liší velikostí písmen (v názvu souboru nebo některého adresáře)
         */
        FOUND_CASE_DIFFERS,
        /**
         * neexistuje ani při porovnání bez ohledu na velikost písmen
         */
        NOT_FOUND,
        /**
         * cesta nevede dovnitř kořenového adresáře balíku (nekontroluje se)
         */
        OUTSIDE_ROOT
    }

    public static final class Result {
        private final Status status;
        private final String requestedRelativePath;
        private final String actualRelativePath;
        private final File actualFile;

        Result(Status status, String requestedRelativePath, String actualRelativePath, File actualFile) {
            this.status = status;
            this.requestedRelativePath = requestedRelativePath;
            this.actualRelativePath = actualRelativePath;
            this.actualFile = actualFile;
        }

        public Status getStatus() {
            return status;
        }

        public boolean isCaseDiffers() {
            return status == Status.FOUND_CASE_DIFFERS;
        }

        /**
         * Cesta z dotazu relativně ke kořeni balíku, oddělovač '/'. Null pro {@link Status#OUTSIDE_ROOT}.
         */
        public String getRequestedRelativePath() {
            return requestedRelativePath;
        }

        /**
         * Skutečná cesta relativně ke kořeni balíku, oddělovač '/'. Jen pro {@link Status#FOUND_EXACT}
         * a {@link Status#FOUND_CASE_DIFFERS}, jinak null.
         */
        public String getActualRelativePath() {
            return actualRelativePath;
        }

        /**
         * Skutečný soubor se skutečnými názvy složek cesty. Jen pro nalezené, jinak null.
         */
        public File getActualFile() {
            return actualFile;
        }

        /**
         * Text hlášky pro {@link Status#FOUND_CASE_DIFFERS}, např. "soubor mastercopy/mc_0001.jp2 nenalezen; v balíku je
         * soubor mastercopy/MC_0001.jp2, jehož název se liší velikostí písmen".
         */
        public String caseDiffersMessage() {
            return PackageFileLookup.caseDiffersMessage(requestedRelativePath, actualRelativePath);
        }

        @Override
        public String toString() {
            return status + " " + requestedRelativePath + " -> " + actualRelativePath;
        }
    }

    /**
     * Výpis jednoho adresáře: skutečné názvy podle NFC tvaru a podle tvaru bez velikosti písmen.
     */
    private static final class DirListing {
        private final Map<String, String> byNfc = new HashMap<>();
        private final Map<String, TreeSet<String>> byFolded = new HashMap<>();

        DirListing(String[] names) {
            for (String name : names) {
                String nfc = nfc(name);
                byNfc.put(nfc, name);
                byFolded.computeIfAbsent(fold(nfc), k -> new TreeSet<>()).add(name);
            }
        }

        String exact(String segment) {
            return byNfc.get(nfc(segment));
        }

        String caseVariant(String segment) {
            TreeSet<String> variants = byFolded.get(fold(nfc(segment)));
            return variants == null ? null : variants.first();
        }
    }

    /**
     * Výpis adresáře, který neexistuje nebo není adresář.
     */
    private static final DirListing NO_DIR = new DirListing(new String[0]);

    private final ConcurrentMap<String, DirListing> listingsByDir = new ConcurrentHashMap<>();
    private final AtomicInteger listingsLoaded = new AtomicInteger();

    /**
     * Najde soubor (nebo adresář) uvnitř kořenového adresáře balíku.
     *
     * @param rootDir kořenový adresář balíku; jeho vlastní cesta se nekontroluje (zadává ji uživatel)
     * @param file    soubor, typicky z {@code Utils.buildAbsoluteFile(rootDir, cestaZMetadat)}; relativní cesta se bere
     *                vůči pracovnímu adresáři jako u {@code File.getAbsoluteFile()}
     */
    public Result lookup(File rootDir, File file) {
        Path root = rootDir.getAbsoluteFile().toPath().normalize();
        Path target = file.getAbsoluteFile().toPath().normalize();
        if (!target.startsWith(root)) {
            return new Result(Status.OUTSIDE_ROOT, null, null, null);
        }
        Path relative = root.relativize(target);
        List<String> segments = new ArrayList<>();
        for (Path segment : relative) {
            String name = segment.toString();
            if (!name.isEmpty()) {
                segments.add(name);
            }
        }
        String requested = String.join("/", segments);
        if (segments.isEmpty()) {
            //prazdna cesta neoznacuje zadny soubor balíku
            return new Result(Status.NOT_FOUND, requested, null, null);
        }
        File current = root.toFile();
        List<String> actualSegments = new ArrayList<>(segments.size());
        boolean caseDiffers = false;
        for (String segment : segments) {
            DirListing listing = listing(current);
            String actual = listing.exact(segment);
            if (actual == null) {
                actual = listing.caseVariant(segment);
                if (actual == null) {
                    return new Result(Status.NOT_FOUND, requested, null, null);
                }
                caseDiffers = true;
            }
            actualSegments.add(actual);
            current = new File(current, actual);
        }
        return new Result(caseDiffers ? Status.FOUND_CASE_DIFFERS : Status.FOUND_EXACT,
                requested, String.join("/", actualSegments), current);
    }

    /**
     * Zapomene všechny zapamatované výpisy adresářů (začátek nového běhu validace).
     */
    public void clear() {
        listingsByDir.clear();
    }

    /**
     * Počet skutečně provedených výpisů adresářů od vytvoření (pro testy mezipaměti).
     */
    public int getListingsLoaded() {
        return listingsLoaded.get();
    }

    private DirListing listing(File dir) {
        return listingsByDir.computeIfAbsent(dir.getPath(), key -> {
            listingsLoaded.incrementAndGet();
            String[] names = dir.list();
            return names == null ? NO_DIR : new DirListing(names);
        });
    }

    // -----------------------------------------------------------------------------------------------------------------
    // Porovnani mnozin souboru (soubory z disku proti souborum z metadat) nezavisle na systemu
    // -----------------------------------------------------------------------------------------------------------------

    /**
     * Klíč souboru pro porovnání množin: absolutní cesta jako řetězec, s rozlišením velikosti písmen na všech systémech.
     * ({@code java.io.File.equals/compareTo} velikost písmen na Windows nerozlišují, na macOS a Linuxu ano.)
     * Cesta se jinak neupravuje, takže na macOS a Linuxu se porovnává stejně jako dosud přes {@code File}.
     */
    public static String pathKey(File file) {
        return file.getAbsoluteFile().getPath();
    }

    /**
     * Shoda dvou souborů podle {@link #pathKey(File)}.
     */
    public static boolean samePath(File a, File b) {
        return pathKey(a).equals(pathKey(b));
    }

    /**
     * Soubor z {@code candidates}, jehož cesta se od cesty {@code file} liší jen velikostí písmen, nebo null.
     * Při více shodách ten s nejmenším klíčem (výsledek nezávisí na pořadí kandidátů).
     */
    public static File findCaseVariant(File file, Collection<File> candidates) {
        return findCaseVariant(file, caseVariantIndex(candidates));
    }

    /**
     * Rejstřík souborů podle cesty bez ohledu na velikost písmen, pro opakované volání
     * {@link #findCaseVariant(File, Map)} nad stejnou množinou kandidátů (jinak by hledání pro každý soubor procházelo
     * všechny kandidáty).
     */
    public static Map<String, TreeMap<String, File>> caseVariantIndex(Collection<File> candidates) {
        Map<String, TreeMap<String, File>> index = new HashMap<>();
        for (File candidate : candidates) {
            String key = pathKey(candidate);
            index.computeIfAbsent(fold(nfc(key)), k -> new TreeMap<>()).putIfAbsent(key, candidate);
        }
        return index;
    }

    /**
     * Jako {@link #findCaseVariant(File, Collection)}, nad rejstříkem z {@link #caseVariantIndex(Collection)}.
     */
    public static File findCaseVariant(File file, Map<String, TreeMap<String, File>> index) {
        String key = pathKey(file);
        TreeMap<String, File> variants = index.get(fold(nfc(key)));
        if (variants == null) {
            return null;
        }
        for (Map.Entry<String, File> variant : variants.entrySet()) {
            if (!variant.getKey().equals(key)) {
                return variant.getValue();
            }
        }
        return null;
    }

    /**
     * Seřazená množina klíčů {@link #pathKey(File)}; při shodném klíči se ponechá první soubor.
     */
    public static TreeMap<String, File> byPathKey(Collection<File> files) {
        TreeMap<String, File> result = new TreeMap<>();
        for (File file : files) {
            result.putIfAbsent(pathKey(file), file.getAbsoluteFile());
        }
        return result;
    }

    // -----------------------------------------------------------------------------------------------------------------
    // Texty hlasek
    // -----------------------------------------------------------------------------------------------------------------

    /**
     * "soubor A nenalezen; v balíku je soubor B, jehož název se liší velikostí písmen"
     */
    public static String caseDiffersMessage(String requested, String actual) {
        return String.format("soubor %s nenalezen; v balíku je soubor %s, jehož název se liší velikostí písmen", requested, actual);
    }

    /**
     * Doplněk hlášky u odkazu z metadat: "; v balíku je soubor B, jehož název se liší velikostí písmen"
     */
    public static String actualFileHint(String actual) {
        return String.format("; v balíku je soubor %s, jehož název se liší velikostí písmen", actual);
    }

    /**
     * Doplněk hlášky u souboru z disku: "; odkaz v metadatech (A) se liší velikostí písmen"
     */
    public static String referenceHint(String referenced) {
        return String.format("; odkaz v metadatech (%s) se liší velikostí písmen", referenced);
    }

    private static String nfc(String s) {
        return Normalizer.normalize(s, Normalizer.Form.NFC);
    }

    private static String fold(String s) {
        return s.toUpperCase(Locale.ROOT).toLowerCase(Locale.ROOT);
    }
}
