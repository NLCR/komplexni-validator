package nkp.pspValidator.gui.newvalidation;

/**
 * Průběh rozbalování souboru ZIP ({@link ZipUnpacker}): kolik souborů (položek, které nejsou adresáři) a bajtů je
 * rozbaleno z kolika. Neměnná hodnota, bez JavaFX.
 */
public final class UnpackProgress {

    private final int filesDone;
    private final int filesTotal;
    private final long bytesDone;
    private final long bytesTotal;

    /**
     * @param bytesTotal součet nekomprimovaných velikostí souborů; záporný, když velikost některého souboru archiv
     *                   neuvádí (podíl se pak počítá podle počtu souborů)
     */
    public UnpackProgress(int filesDone, int filesTotal, long bytesDone, long bytesTotal) {
        this.filesDone = filesDone;
        this.filesTotal = filesTotal;
        this.bytesDone = bytesDone;
        this.bytesTotal = bytesTotal;
    }

    public int getFilesDone() {
        return filesDone;
    }

    public int getFilesTotal() {
        return filesTotal;
    }

    public long getBytesDone() {
        return bytesDone;
    }

    public long getBytesTotal() {
        return bytesTotal;
    }

    /**
     * Podíl hotové práce 0 až 1: podle bajtů, když je velikost známá a nenulová, jinak podle počtu souborů. Archiv bez
     * souborů je hotový (1).
     */
    public double fraction() {
        double value;
        if (bytesTotal > 0) {
            value = (double) bytesDone / bytesTotal;
        } else if (filesTotal > 0) {
            value = (double) filesDone / filesTotal;
        } else {
            return 1;
        }
        return Math.max(0, Math.min(1, value));
    }

    /** „1 234 z 5 678 souborů“, „0 z 1 souboru“ (tisíce oddělené pevnou mezerou). */
    public String countsText() {
        return number(filesDone) + " z " + number(filesTotal) + " " + (filesTotal == 1 ? "souboru" : "souborů");
    }

    /** Nadpis panelu průběhu při rozbalování skupiny: „Rozbaluji skupinu: 1 234 z 5 678 souborů“, před prvním hlášením „Rozbaluji skupinu…“. */
    public static String groupTitle(UnpackProgress progress) {
        return progress == null ? "Rozbaluji skupinu…" : "Rozbaluji skupinu: " + progress.countsText();
    }

    /** Ukazatel průběhu: před prvním hlášením neurčitý (-1), pak podíl hotové práce. */
    public static double groupFraction(UnpackProgress progress) {
        return progress == null ? -1 : progress.fraction();
    }

    /** Číslo s tisíci oddělenými pevnou mezerou (česky): 1234567 → „1 234 567“. */
    static String number(long value) {
        String digits = Long.toString(Math.abs(value));
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < digits.length(); i++) {
            if (i > 0 && (digits.length() - i) % 3 == 0) {
                text.append(' ');
            }
            text.append(digits.charAt(i));
        }
        return (value < 0 ? "-" : "") + text;
    }

    @Override
    public String toString() {
        return filesDone + "/" + filesTotal + " files, " + bytesDone + "/" + bytesTotal + " B";
    }
}
