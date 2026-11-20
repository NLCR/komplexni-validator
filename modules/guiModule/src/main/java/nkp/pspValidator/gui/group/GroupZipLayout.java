package nkp.pspValidator.gui.group;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

/**
 * Skupina balíků v jednom souboru ZIP: které položky archivu jsou balíky, určené jen ze seznamu názvů položek
 * (archiv se nerozbaluje, {@link #read} čte jen centrální adresář ZIPu). Stejné rozložení platí v dialogu Nová
 * validace (rozpoznání) i při běhu (po rozbalení se balíky berou z tohoto rozložení, ne z obsahu disku). Bez JavaFX.
 * <p>
 * Pravidla (podle CLI, akce VALIDATE_PSP_GROUP se ZIPem, s rozdíly níže):
 * <ul>
 *     <li>Úroveň skupiny: obsahuje-li archiv jediný kořenový adresář, jsou balíky v něm (jako CLI), jinak v kořeni
 *     archivu.</li>
 *     <li>Na úrovni skupiny je balíkem každý adresář a každý soubor s příponou .zip (bez ohledu na velikost písmen);
 *     ostatní soubory se ignorují a skryté položky (název začíná tečkou) se vynechávají, stejně jako u skupiny
 *     v adresáři ({@link GroupPackages}).</li>
 *     <li>Navíc proti CLI: kořenový adresář {@code __MACOSX} (metadata, která do ZIPu přidává Finder) se vynechává; když
 *     jsou na úrovni skupiny soubory XML, jde o jeden balík, ne o skupinu (CLI by validovalo podadresáře balíku jako
 *     balíky), a rozložení skončí chybou; položka s „..“ v cestě (mířící mimo cílový adresář) je chyba hned, ještě před
 *     rozbalením.</li>
 * </ul>
 * Názvy položek ZIPu používají jako oddělovač „/“.
 */
public final class GroupZipLayout {

    /** Výsledek rozložení. */
    public enum Status {
        OK,
        /** archiv nemá žádnou položku */
        EMPTY,
        /** archiv nemá na úrovni skupiny žádný adresář ani soubor ZIP */
        NO_PACKAGES,
        /** na úrovni skupiny jsou soubory XML: archiv je jeden balík */
        SINGLE_PACKAGE,
        /** položka by se rozbalila mimo cílový adresář */
        ENTRY_OUTSIDE
    }

    /** Balík ve skupině: adresář nebo vnořený soubor ZIP. */
    public static final class Package {
        private final String name;
        private final String path;
        private final boolean zip;

        Package(String name, String path, boolean zip) {
            this.name = name;
            this.path = path;
            this.zip = zip;
        }

        /** Název balíku (název adresáře nebo souboru ZIP). */
        public String getName() {
            return name;
        }

        /** Cesta v archivu bez koncového lomítka, např. „davka/nk-01“. */
        public String getPath() {
            return path;
        }

        /** Vnořený soubor ZIP (jinak adresář). */
        public boolean isZip() {
            return zip;
        }

        @Override
        public String toString() {
            return (zip ? "zip " : "dir ") + path;
        }
    }

    /** Kořenový adresář s metadaty Finderu, nikdy není balíkem. */
    static final String MAC_METADATA_DIR = "__MACOSX";

    private final Status status;
    private final String groupPrefix;
    private final List<Package> packages;
    private final List<String> ignored;
    private final String problemEntry;

    private GroupZipLayout(Status status, String groupPrefix, List<Package> packages, List<String> ignored, String problemEntry) {
        this.status = status;
        this.groupPrefix = groupPrefix;
        this.packages = Collections.unmodifiableList(packages);
        this.ignored = Collections.unmodifiableList(ignored);
        this.problemEntry = problemEntry;
    }

    /**
     * Rozložení skupiny podle názvů položek archivu (v libovolném pořadí, adresáře s koncovým „/“ nebo jen jako část
     * cesty souborů).
     */
    public static GroupZipLayout of(Collection<String> entryNames) {
        boolean any = false;
        for (String name : entryNames) {
            if (escapes(name)) {
                return new GroupZipLayout(Status.ENTRY_OUTSIDE, "", Collections.<Package>emptyList(), Collections.<String>emptyList(), name);
            }
            if (!trimSlashes(name).isEmpty()) {
                any = true;
            }
        }
        if (!any) {
            return error(Status.EMPTY);
        }
        //koren archivu, bez skrytych polozek a metadat Finderu
        Map<String, Boolean> top = children(entryNames, "");
        top.remove(MAC_METADATA_DIR);
        String prefix = "";
        Map<String, Boolean> level = top;
        if (top.size() == 1 && top.values().iterator().next()) {
            String only = top.keySet().iterator().next();
            prefix = only + "/";
            level = children(entryNames, prefix);
        }
        List<Package> packages = new ArrayList<>();
        List<String> ignored = new ArrayList<>();
        for (Map.Entry<String, Boolean> child : level.entrySet()) {
            String name = child.getKey();
            if (child.getValue()) {
                packages.add(new Package(name, prefix + name, false));
            } else if (GroupPackages.isZipName(name)) {
                packages.add(new Package(name, prefix + name, true));
            } else if (name.toLowerCase(Locale.ROOT).endsWith(".xml")) {
                return error(Status.SINGLE_PACKAGE);
            } else {
                ignored.add(name);
            }
        }
        if (packages.isEmpty()) {
            return new GroupZipLayout(Status.NO_PACKAGES, prefix, packages, sorted(ignored), null);
        }
        packages.sort((a, b) -> GroupPackages.NAME_ORDER.compare(a.getName(), b.getName()));
        return new GroupZipLayout(Status.OK, prefix, packages, sorted(ignored), null);
    }

    /** Rozložení podle centrálního adresáře otevřeného archivu (obsah položek se nečte). */
    public static GroupZipLayout of(ZipFile zip) {
        List<String> names = new ArrayList<>();
        for (Enumeration<? extends ZipEntry> en = zip.entries(); en.hasMoreElements(); ) {
            names.add(en.nextElement().getName());
        }
        return of(names);
    }

    /**
     * Rozložení podle souboru ZIP (přečte jen centrální adresář).
     *
     * @throws IOException soubor není ZIP nebo ho nelze přečíst; hláška pro uživatele viz {@link #readErrorMessage}
     */
    public static GroupZipLayout read(File zip) throws IOException {
        try (ZipFile zipFile = new ZipFile(zip)) {
            return of(zipFile);
        } catch (IllegalArgumentException e) {
            //nazev polozky v jinem kodovani nez UTF-8
            throw new IOException(e.getMessage(), e);
        }
    }

    /** Srozumitelná hláška, když soubor ZIP nejde otevřít nebo přečíst. */
    public static String readErrorMessage(File zip, Exception e) {
        Throwable cause = e instanceof IOException && e.getCause() instanceof IllegalArgumentException ? e.getCause() : e;
        if (cause instanceof ZipException) {
            return String.format("Soubor %s není platný ZIP (%s).", zip.getName(), cause.getMessage());
        } else if (cause instanceof IllegalArgumentException) {
            return String.format("Názvy souborů v souboru ZIP %s nelze přečíst (%s).", zip.getName(), cause.getMessage());
        }
        return String.format("Soubor ZIP %s nelze přečíst (%s).", zip.getName(), cause.getMessage());
    }

    private static GroupZipLayout error(Status status) {
        return new GroupZipLayout(status, "", Collections.<Package>emptyList(), Collections.<String>emptyList(), null);
    }

    private static List<String> sorted(List<String> names) {
        names.sort(GroupPackages.NAME_ORDER);
        return names;
    }

    /**
     * Přímé položky pod {@code prefix} (bez skrytých): název → je adresář (má pod sebou další položky nebo končí „/“).
     */
    private static Map<String, Boolean> children(Collection<String> entryNames, String prefix) {
        Map<String, Boolean> result = new LinkedHashMap<>();
        for (String entryName : entryNames) {
            //lomitko na zacatku nazvu: prazdna cast cesty se preskoci (rozbali se do ciloveho adresare stejne)
            String name = trimSlashes(entryName);
            if (!name.startsWith(prefix)) {
                continue;
            }
            String rest = trimSlashes(name.substring(prefix.length()));
            if (rest.isEmpty()) {
                continue;
            }
            int slash = rest.indexOf('/');
            String first = slash < 0 ? rest : rest.substring(0, slash);
            if (first.startsWith(".")) {
                continue;
            }
            boolean dir = slash >= 0;
            result.merge(first, dir, Boolean::logicalOr);
        }
        return result;
    }

    private static String trimSlashes(String name) {
        //lomítko a „./“ na začátku názvu (některé nástroje tak položky zapisují) cestu nemění
        String result = name;
        while (result.startsWith("/") || result.startsWith("./")) {
            result = result.substring(result.startsWith("/") ? 1 : 2);
        }
        return result;
    }

    /** Položka s „..“ jako částí cesty (oddělovač „/“ i „\“) by se mohla rozbalit mimo cílový adresář. */
    static boolean escapes(String entryName) {
        for (String part : entryName.split("[/\\\\]")) {
            if (part.equals("..")) {
                return true;
            }
        }
        return false;
    }

    public Status getStatus() {
        return status;
    }

    public boolean isOk() {
        return status == Status.OK;
    }

    /** Cesta úrovně skupiny v archivu: „“ (kořen) nebo „davka/“. */
    public String getGroupPrefix() {
        return groupPrefix;
    }

    /** Balíky seřazené podle názvu (jako u skupiny v adresáři). */
    public List<Package> getPackages() {
        return packages;
    }

    /** Ignorované soubory na úrovni skupiny (nejsou adresář, ZIP ani XML), seřazené. */
    public List<String> getIgnored() {
        return ignored;
    }

    /** Položka mířící mimo cílový adresář ({@link Status#ENTRY_OUTSIDE}), jinak null. */
    public String getProblemEntry() {
        return problemEntry;
    }

    /** Srozumitelná chyba pro uživatele, null u {@link Status#OK}. */
    public String errorMessage() {
        switch (status) {
            case OK:
                return null;
            case EMPTY:
                return "Soubor ZIP je prázdný.";
            case NO_PACKAGES:
                return "Soubor ZIP neobsahuje žádný balík (adresář nebo soubor ZIP)"
                        + (groupPrefix.isEmpty() ? "." : " v adresáři " + groupPrefix.substring(0, groupPrefix.length() - 1) + ".");
            case SINGLE_PACKAGE:
                return "Soubor ZIP obsahuje jeden balík, ne skupinu balíků. Zvolte zdroj „Soubor ZIP“.";
            case ENTRY_OUTSIDE:
                return String.format("Soubor ZIP obsahuje položku mířící mimo cílový adresář (%s), nelze ho bezpečně rozbalit.", problemEntry);
            default:
                throw new IllegalStateException();
        }
    }
}
