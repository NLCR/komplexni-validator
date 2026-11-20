package nkp.pspValidator.gui.group;

import nkp.pspValidator.gui.newvalidation.PackageRecognizer;
import nkp.pspValidator.gui.newvalidation.Recognition;
import nkp.pspValidator.gui.newvalidation.ValidationSource;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.zip.ZipFile;

/**
 * Rozpoznání typu a verze balíků skupiny v dialogu Nová validace (pro kartu rozpoznání, sekci nástrojů a přeskočených
 * pravidel). Balíky se rozpoznávají postupně stejnou logikou jako jeden balík ({@link PackageRecognizer}), u velké
 * skupiny jen prvních {@link #DEFAULT_LIMIT}; ostatní se rozpoznají až při validaci. Bez JavaFX, volat z pracovního
 * vlákna.
 * <p>
 * Skupina v souboru ZIP se nerozbaluje: balíky určí {@link GroupZipLayout} ze seznamu položek archivu a u každého
 * balíku-adresáře se vybalí jen XML v jeho kořeni ({@link PackageRecognizer#recognizeInZip}). Vnořený ZIP se v dialogu
 * nerozpoznává ({@link Recognition#deferred}), rozpozná se až při validaci.
 */
public final class GroupRecognition {

    /** Kolik balíků skupiny se v dialogu nejvýš rozpozná (u ZIPu se čte archiv, u velké skupiny by to trvalo). */
    public static final int DEFAULT_LIMIT = 200;

    /** Průběh rozpoznávání: kolik balíků je hotovo z kolika, které se budou rozpoznávat. */
    public interface Progress {
        void recognized(int done, int toRecognize);
    }

    private final File groupDir;
    private final boolean zip;
    private final String error;
    private final GroupPackages.Listing listing;
    private final List<Recognition> recognitions;

    private GroupRecognition(File groupDir, String error, GroupPackages.Listing listing, List<Recognition> recognitions) {
        this(groupDir, false, error, listing, recognitions);
    }

    private GroupRecognition(File groupDir, boolean zip, String error, GroupPackages.Listing listing, List<Recognition> recognitions) {
        this.groupDir = groupDir;
        this.zip = zip;
        this.error = error;
        this.listing = listing;
        this.recognitions = recognitions == null ? Collections.<Recognition>emptyList() : Collections.unmodifiableList(recognitions);
    }

    /**
     * Najde balíky ve skupině a rozpozná nejvýš {@code limit} z nich.
     *
     * @param cancelled zda výsledek ještě někoho zajímá (změna cesty v dialogu); při zrušení vrátí null
     * @param progress  průběh, smí být null
     */
    public static GroupRecognition recognize(File groupDir, int limit, BooleanSupplier cancelled, Progress progress) {
        if (groupDir.isFile()) {
            return recognizeZip(groupDir, limit, cancelled, progress);
        }
        GroupPackages.Listing listing;
        try {
            listing = GroupPackages.list(groupDir);
        } catch (IOException e) {
            return new GroupRecognition(groupDir, e.getMessage(), null, null);
        }
        PackageRecognizer recognizer = new PackageRecognizer();
        List<GroupPackages.Entry> packages = listing.getPackages();
        int toRecognize = Math.min(limit, packages.size());
        List<Recognition> recognitions = new ArrayList<>(toRecognize);
        for (int i = 0; i < toRecognize; i++) {
            if (cancelled.getAsBoolean()) {
                return null;
            }
            if (progress != null) {
                progress.recognized(i, toRecognize);
            }
            GroupPackages.Entry entry = packages.get(i);
            Recognition recognition;
            try {
                recognition = recognizer.recognize(entry.isZip() ? ValidationSource.ZIP : ValidationSource.DIRECTORY, entry.getFile().getPath());
            } catch (RuntimeException e) {
                recognition = Recognition.notRecognized("Balík nelze přečíst: " + e.getMessage());
            }
            recognitions.add(recognition);
        }
        if (cancelled.getAsBoolean()) {
            return null;
        }
        return new GroupRecognition(groupDir, null, listing, recognitions);
    }

    /** Skupina v souboru ZIP (soubor se nerozbaluje). */
    private static GroupRecognition recognizeZip(File zipFile, int limit, BooleanSupplier cancelled, Progress progress) {
        try (ZipFile zip = new ZipFile(zipFile)) {
            GroupZipLayout layout = GroupZipLayout.of(zip);
            if (!layout.isOk()) {
                return new GroupRecognition(zipFile, true, layout.errorMessage(), null, null);
            }
            GroupPackages.Listing listing = GroupPackages.inZip(zipFile, layout, null);
            PackageRecognizer recognizer = new PackageRecognizer();
            List<GroupZipLayout.Package> packages = layout.getPackages();
            int toRecognize = Math.min(limit, packages.size());
            List<Recognition> recognitions = new ArrayList<>(toRecognize);
            for (int i = 0; i < toRecognize; i++) {
                if (cancelled.getAsBoolean()) {
                    return null;
                }
                if (progress != null) {
                    progress.recognized(i, toRecognize);
                }
                GroupZipLayout.Package pkg = packages.get(i);
                Recognition recognition;
                if (pkg.isZip()) {
                    recognition = Recognition.deferred("Balík v souboru ZIP uvnitř skupiny se rozpozná až při validaci.");
                } else {
                    try {
                        recognition = recognizer.recognizeInZip(zip, pkg.getPath() + "/");
                    } catch (IOException | RuntimeException e) {
                        recognition = Recognition.notRecognized("Balík nelze přečíst: " + e.getMessage());
                    }
                }
                recognitions.add(recognition);
            }
            if (cancelled.getAsBoolean()) {
                return null;
            }
            return new GroupRecognition(zipFile, true, null, listing, recognitions);
        } catch (IOException | IllegalArgumentException e) {
            return new GroupRecognition(zipFile, true, GroupZipLayout.readErrorMessage(zipFile, e), null, null);
        }
    }

    /** Pro testy textů: hotové rozpoznání bez čtení disku. */
    static GroupRecognition of(GroupPackages.Listing listing, List<Recognition> recognitions) {
        return new GroupRecognition(listing.getSource(), listing.getZip() != null, null, listing, recognitions);
    }

    /** Vybraný adresář skupiny nebo soubor ZIP se skupinou. */
    public File getGroupDir() {
        return groupDir;
    }

    /** Skupina v souboru ZIP. */
    public boolean isZip() {
        return zip;
    }

    /** Proč nejde adresář přečíst, jinak null. */
    public String getError() {
        return error;
    }

    public boolean isOk() {
        return error == null;
    }

    /** Počet balíků ve skupině (0 při chybě). */
    public int getPackageCount() {
        return listing == null ? 0 : listing.getPackages().size();
    }

    /** Rozpoznání prvních balíků v pořadí {@link GroupPackages.Listing#getPackages()}. */
    public List<Recognition> getRecognitions() {
        return recognitions;
    }

    /** Kolik balíků se kvůli limitu nerozpoznávalo. */
    public int getNotRecognizedDueToLimit() {
        return getPackageCount() - recognitions.size();
    }

    public List<String> getIgnored() {
        return listing == null ? Collections.<String>emptyList() : listing.getIgnored();
    }

    public GroupPackages.Listing getListing() {
        return listing;
    }
}
