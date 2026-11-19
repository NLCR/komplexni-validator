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
 * podle obsahu a ostatní soubory přeskočí s hláškou; skupina tam může být i v souboru ZIP. GUI bere jen adresář.
 */
public final class GroupPackages {

    /** Jeden balík ve skupině. */
    public static final class Entry {
        private final File file;
        private final boolean zip;

        public Entry(File file, boolean zip) {
            this.file = file;
            this.zip = zip;
        }

        public File getFile() {
            return file;
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
        private final List<Entry> packages;
        private final List<String> ignored;

        Listing(File groupDir, List<Entry> packages, List<String> ignored) {
            this.groupDir = groupDir;
            this.packages = Collections.unmodifiableList(packages);
            this.ignored = Collections.unmodifiableList(ignored);
        }

        public File getGroupDir() {
            return groupDir;
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

    static boolean isZipName(String name) {
        return name.toLowerCase(Locale.ROOT).endsWith(".zip");
    }
}
