package nkp.pspValidator.gui.group;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Balíky ve skupině: přímé položky adresáře skupiny. Balíkem je každý podadresář a každý soubor s příponou .zip
 * (bez ohledu na velikost písmen); ostatní soubory se ignorují, skryté položky (název začíná tečkou) se vynechávají.
 * Balíky jsou seřazené podle názvu bez ohledu na velikost písmen. Bez JavaFX.
 * <p>
 * CLI (akce VALIDATE_PSP_GROUP) bere všechny položky adresáře v pořadí, v jakém je vrátí systém souborů, ZIP pozná
 * podle obsahu a ostatní soubory přeskočí s hláškou. Skupina může být i v jednom souboru ZIP ({@link GroupZipLayout},
 * {@link #inZip}): balíky jsou pak v rozbaleném archivu, ale uživateli se ukazuje cesta k souboru ZIP a balíku v něm
 * ({@link Entry#getDisplayPath()}).
 */
public final class GroupPackages {

    /** Jeden balík ve skupině. */
    public static final class Entry {
        private final File file;
        private final boolean zip;
        private final String displayPath;

        public Entry(File file, boolean zip) {
            this(file, zip, null);
        }

        /**
         * @param displayPath cesta, kterou vidí uživatel (tabulka, CSV, stavový řádek), null = cesta {@code file}
         */
        public Entry(File file, boolean zip, String displayPath) {
            this.file = file;
            this.zip = zip;
            this.displayPath = displayPath;
        }

        /** Soubor nebo adresář, který se validuje (u skupiny v ZIPu v rozbaleném archivu). */
        public File getFile() {
            return file;
        }

        /**
         * Cesta pro uživatele: u skupiny v adresáři absolutní cesta balíku, u skupiny v souboru ZIP cesta k souboru ZIP
         * a balíku v něm („/data/davka.zip/davka/nk-01“), ne dočasný adresář s rozbaleným archivem.
         */
        public String getDisplayPath() {
            return displayPath != null ? displayPath : file.getAbsolutePath();
        }

        /** Balík v souboru ZIP (jinak adresář). */
        public boolean isZip() {
            return zip;
        }

        public String getName() {
            return file.getName();
        }

        @Override
        public String toString() {
            return (zip ? "zip " : "dir ") + file.getName();
        }
    }

    /** Obsah adresáře skupiny. */
    public static final class Listing {
        private final File groupDir;
        private final File zip;
        private final List<Entry> packages;
        private final List<String> ignored;

        Listing(File groupDir, List<Entry> packages, List<String> ignored) {
            this(groupDir, null, packages, ignored);
        }

        Listing(File groupDir, File zip, List<Entry> packages, List<String> ignored) {
            this.groupDir = groupDir;
            this.zip = zip;
            this.packages = Collections.unmodifiableList(packages);
            this.ignored = Collections.unmodifiableList(ignored);
        }

        /** Adresář skupiny, ve kterém se balíky validují (u skupiny v ZIPu v rozbaleném archivu, v dialogu soubor ZIP). */
        public File getGroupDir() {
            return groupDir;
        }

        /** Soubor ZIP se skupinou, null u skupiny v adresáři. */
        public File getZip() {
            return zip;
        }

        /** Co uživatel vybral: adresář skupiny, nebo soubor ZIP se skupinou. */
        public File getSource() {
            return zip != null ? zip : groupDir;
        }

        /** Balíky seřazené podle názvu. */
        public List<Entry> getPackages() {
            return packages;
        }

        /** Názvy ignorovaných souborů (nejsou adresář ani .zip), seřazené. Skryté položky se neuvádějí. */
        public List<String> getIgnored() {
            return ignored;
        }
    }

    /** Řazení podle názvu: bez ohledu na velikost písmen, při shodě podle přesného názvu. */
    static final Comparator<String> NAME_ORDER = String.CASE_INSENSITIVE_ORDER.thenComparing(Comparator.naturalOrder());

    private GroupPackages() {
    }

    /**
     * @throws IOException když adresář neexistuje nebo nejde přečíst
     */
    public static Listing list(File groupDir) throws IOException {
        File[] children = groupDir.listFiles();
        if (children == null) {
            throw new IOException(String.format("Nelze číst obsah adresáře %s.", groupDir.getPath()));
        }
        List<Entry> packages = new ArrayList<>();
        List<String> ignored = new ArrayList<>();
        for (File child : children) {
            String name = child.getName();
            if (name.startsWith(".")) {
                continue;
            }
            if (child.isDirectory()) {
                packages.add(new Entry(child, false));
            } else if (isZipName(name)) {
                packages.add(new Entry(child, true));
            } else {
                ignored.add(name);
            }
        }
        packages.sort((a, b) -> NAME_ORDER.compare(a.getName(), b.getName()));
        ignored.sort(NAME_ORDER);
        return new Listing(groupDir, packages, ignored);
    }

    /**
     * Balíky skupiny v souboru ZIP podle rozložení archivu.
     *
     * @param extractDir adresář, do kterého se archiv rozbalil (balíky se validují v něm); null v dialogu před
     *                   rozbalením (soubory balíků pak jen označují místo v archivu)
     */
    public static Listing inZip(File zip, GroupZipLayout layout, File extractDir) {
        List<Entry> packages = new ArrayList<>();
        for (GroupZipLayout.Package pkg : layout.getPackages()) {
            File displayFile = new File(zip, pkg.getPath());
            File file = extractDir == null ? displayFile : new File(extractDir, pkg.getPath());
            packages.add(new Entry(file, pkg.isZip(), displayFile.getAbsolutePath()));
        }
        String prefix = layout.getGroupPrefix();
        File groupDir = extractDir == null ? zip : prefix.isEmpty() ? extractDir : new File(extractDir, prefix);
        return new Listing(groupDir, zip, packages, new ArrayList<>(layout.getIgnored()));
    }

    static boolean isZipName(String name) {
        return name.toLowerCase(Locale.ROOT).endsWith(".zip");
    }
}
