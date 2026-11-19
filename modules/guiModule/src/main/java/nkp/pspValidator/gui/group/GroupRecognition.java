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

/**
 * Rozpoznání typu a verze balíků skupiny v dialogu Nová validace (pro kartu rozpoznání, sekci nástrojů a přeskočených
 * pravidel). Balíky se rozpoznávají postupně stejnou logikou jako jeden balík ({@link PackageRecognizer}), u velké
 * skupiny jen prvních {@link #DEFAULT_LIMIT}; ostatní se rozpoznají až při validaci. Bez JavaFX, volat z pracovního
 * vlákna.
 */
public final class GroupRecognition {

    /** Kolik balíků skupiny se v dialogu nejvýš rozpozná (u ZIPu se čte archiv, u velké skupiny by to trvalo). */
    public static final int DEFAULT_LIMIT = 200;

    /** Průběh rozpoznávání: kolik balíků je hotovo z kolika, které se budou rozpoznávat. */
    public interface Progress {
        void recognized(int done, int toRecognize);
    }

    private final File groupDir;
    private final String error;
    private final GroupPackages.Listing listing;
    private final List<Recognition> recognitions;

    private GroupRecognition(File groupDir, String error, GroupPackages.Listing listing, List<Recognition> recognitions) {
        this.groupDir = groupDir;
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

    /** Pro testy textů: hotové rozpoznání bez čtení disku. */
    static GroupRecognition of(GroupPackages.Listing listing, List<Recognition> recognitions) {
        return new GroupRecognition(listing.getGroupDir(), null, listing, recognitions);
    }

    public File getGroupDir() {
        return groupDir;
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
