package nkp.pspValidator.gui.newvalidation;

import nkp.pspValidator.shared.engine.Utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

/**
 * Rozbalení balíku ze souboru ZIP před validací, bez vazby na JavaFX. Používá ho dialog rozbalování u jednoho balíku
 * i běh skupiny balíků (tam bez dialogu).
 * <p>
 * Postup: kontrola ZIPu a adresáře pro dočasné soubory, nový prázdný pracovní adresář
 * {@code <tmp>/kv-rozbaleno-<náhodné>} a v něm adresář {@code <název souboru bez .zip>}, do kterého se ZIP rozbalí.
 * Když je v rozbaleném adresáři jediná položka, je balíkem ona, jinak celý rozbalený adresář (skupina balíků v ZIPu
 * si balíky určí sama z {@link Result#getExtractDir()}).
 * <p>
 * Archiv se rozbaluje po položkách s hlášením průběhu ({@link UnpackProgress}) a s dotazem na zrušení před každou
 * položkou i během zápisu velkého souboru, takže zrušení rozbalování přeruší. Než se cokoli zapíše, zkontrolují se
 * všechny položky: položka mířící mimo cílový adresář (například „../soubor“) je chyba a nic se nerozbalí.
 * <p>
 * Pracovní adresář je vždy nový, nic existujícího se nemaže (dřívější postup mazal {@code <tmp>/<název>}, což mohl být
 * i cizí adresář nebo sama validovaná skupina). Při zrušení a při chybě se pracovní adresář smaže; po úspěchu ho maže
 * volající, a co zbude, smaže se při ukončení aplikace.
 */
public class ZipUnpacker {

    /** Rozbalení se nepovedlo; hláška je určená uživateli (stejná jako v dřívějším dialogu). */
    public static class UnpackException extends Exception {
        public UnpackException(String message) {
            super(message);
        }
    }

    /** Výsledek rozbalení. */
    public static class Result {
        private final File containerDir;
        private final File extractDir;
        private final File pspDir;

        Result(File containerDir, File extractDir, File pspDir) {
            this.containerDir = containerDir;
            this.extractDir = extractDir;
            this.pspDir = pspDir;
        }

        /** Adresář, do kterého se archiv rozbalil ({@code <pracovní adresář>/<název ZIPu bez .zip>}). */
        public File getExtractDir() {
            return extractDir;
        }

        /** Pracovní adresář vytvořený pro toto rozbalení (po validaci ho lze smazat celý). */
        public File getContainerDir() {
            return containerDir;
        }

        /** Adresář balíku, který se bude validovat. */
        public File getPspDir() {
            return pspDir;
        }
    }

    private static final String WORK_DIR_PREFIX = "kv-rozbaleno-";
    /** pracovní adresáře, které se mají smazat při ukončení aplikace (pokud je do té doby nesmaže volající) */
    private static final Set<File> WORK_DIRS = new LinkedHashSet<>();
    private static boolean shutdownHookRegistered = false;

    private final File tmpDir;

    /** Rozbaluje do adresáře pro dočasné soubory ({@code java.io.tmpdir}). */
    public ZipUnpacker() {
        this(new File(System.getProperty("java.io.tmpdir")));
    }

    public ZipUnpacker(File tmpDir) {
        this.tmpDir = tmpDir;
    }

    /** Jako {@link #unpack(File, BooleanSupplier, Consumer, Consumer)} bez hlášení průběhu. */
    public Result unpack(File zip, BooleanSupplier cancelled, Consumer<String> messages) throws UnpackException {
        return unpack(zip, cancelled, messages, null);
    }

    /**
     * Rozbalí ZIP. Volat z pracovního vlákna.
     *
     * @param cancelled ptá se mezi kroky, před každou položkou archivu a během zápisu souboru, zda bylo rozbalování
     *                  zrušeno
     * @param messages  průběžné zprávy pro uživatele („Rozbaluji … do adresáře …“), smí být null
     * @param progress  průběh rozbalování (na začátku 0 z N, pak po každém souboru a během velkého souboru), smí být null
     * @return výsledek, nebo null, když bylo rozbalování zrušeno (pracovní adresář je pak smazaný)
     * @throws UnpackException soubor není ZIP, adresář pro dočasné soubory nelze použít, položka míří mimo cílový
     *                         adresář, chyba při čtení nebo zápisu (pracovní adresář je pak smazaný)
     */
    public Result unpack(File zip, BooleanSupplier cancelled, Consumer<String> messages, Consumer<UnpackProgress> progress)
            throws UnpackException {
        Consumer<String> out = messages == null ? m -> {
        } : messages;
        try {
            out.accept("Zpracovávám soubor " + zip.getAbsolutePath());
            if (cancelled.getAsBoolean()) {
                return null;
            }
            out.accept(String.format("Kontroluji soubor %s", zip.getAbsolutePath()));
            try (ZipFile zipFile = new ZipFile(zip)) {
                //jen kontrola, ze jde o citelny ZIP
            }
            if (cancelled.getAsBoolean()) {
                return null;
            }
            out.accept(String.format("Kontroluji adresář %s", tmpDir.getAbsolutePath()));
            if (!tmpDir.exists()) {
                throw new UnpackException(String.format("Chyba: adresář %s neexistuje!", tmpDir.getAbsolutePath()));
            } else if (!tmpDir.isDirectory()) {
                throw new UnpackException(String.format("Chyba: soubor %s není adresář!", tmpDir.getAbsolutePath()));
            } else if (!tmpDir.canWrite()) {
                throw new UnpackException(String.format("Chyba: nemůžu zapisovat do adresáře %s!", tmpDir.getAbsolutePath()));
            }
            if (cancelled.getAsBoolean()) {
                return null;
            }
            File workDir = Files.createTempDirectory(tmpDir.toPath(), WORK_DIR_PREFIX).toFile();
            deleteAtExit(workDir);
            boolean done = false;
            try {
                File extractDir = new File(workDir, extractDirName(zip));
                out.accept(String.format("Rozbaluji %s do adresáře %s", zip.getAbsolutePath(), extractDir.getAbsolutePath()));
                if (!extract(zip, extractDir, cancelled, progress) || cancelled.getAsBoolean()) {
                    return null;
                }
                File pspDir = extractDir;
                File[] filesInContainer = extractDir.listFiles();
                if (filesInContainer != null && filesInContainer.length == 1) {
                    pspDir = filesInContainer[0];
                }
                done = true;
                return new Result(workDir, extractDir, pspDir);
            } finally {
                if (!done) {
                    //zruseno nebo chyba: castecne rozbaleny obsah nenechavat na disku
                    deleteWorkDir(workDir);
                }
            }
        } catch (ZipException e) {
            throw new UnpackException(String.format("Chyba: Soubor %s není korektní ZIP: %s!", zip.getAbsolutePath(), e.getMessage()));
        } catch (IllegalArgumentException e) {
            //nazev polozky v jinem kodovani nez UTF-8 (ZIP z nekterych nastroju ve Windows)
            throw new UnpackException(String.format("Chyba: názvy souborů v ZIP souboru %s nelze přečíst: %s!", zip.getAbsolutePath(), e.getMessage()));
        } catch (IOException e) {
            throw new UnpackException(String.format("Chyba zpracování ZIP souboru %s: %s!", zip.getAbsolutePath(), e.getMessage()));
        }
    }

    /** po kolika bajtech uvnitř velkého souboru se hlásí průběh a ptá na zrušení */
    private static final int PROGRESS_STEP_BYTES = 4 * 1024 * 1024;

    /**
     * Rozbalí všechny položky archivu do {@code outFolder}. Nejdřív zkontroluje, že žádná položka nemíří mimo cílový
     * adresář (stejná kontrola jako dřívější rozbalování v jádře), pak rozbaluje po položkách.
     *
     * @return false, když bylo rozbalování zrušeno
     */
    static boolean extract(File zip, File outFolder, BooleanSupplier cancelled, Consumer<UnpackProgress> progress) throws IOException {
        Consumer<UnpackProgress> report = progress == null ? p -> {
        } : progress;
        if (!outFolder.isDirectory() && !outFolder.mkdirs()) {
            throw new IOException("nelze vytvořit adresář " + outFolder.getAbsolutePath());
        }
        String outCanonical = outFolder.getCanonicalPath();
        try (ZipFile zipFile = new ZipFile(zip)) {
            List<? extends ZipEntry> entries = Collections.list(zipFile.entries());
            int filesTotal = 0;
            long bytesTotal = 0;
            for (ZipEntry entry : entries) {
                File target = new File(outFolder, entry.getName());
                String targetCanonical = target.getCanonicalPath();
                //polozka s cestou mimo cilovy adresar (napr. "../soubor") by prepsala cizi soubory
                if (!targetCanonical.equals(outCanonical) && !targetCanonical.startsWith(outCanonical + File.separator)) {
                    throw new IOException("položka '" + entry.getName() + "' míří mimo cílový adresář");
                }
                if (!entry.isDirectory()) {
                    filesTotal++;
                    bytesTotal = entry.getSize() < 0 || bytesTotal < 0 ? -1 : bytesTotal + entry.getSize();
                }
            }
            int filesDone = 0;
            long bytesDone = 0;
            report.accept(new UnpackProgress(0, filesTotal, 0, bytesTotal));
            byte[] buffer = new byte[64 * 1024];
            for (ZipEntry entry : entries) {
                if (cancelled.getAsBoolean()) {
                    return false;
                }
                File target = new File(outFolder, entry.getName());
                if (entry.isDirectory()) {
                    target.mkdirs();
                    continue;
                }
                target.getParentFile().mkdirs();
                long sinceReport = 0;
                try (InputStream in = zipFile.getInputStream(entry); OutputStream fileOut = new FileOutputStream(target)) {
                    int len;
                    while ((len = in.read(buffer)) > 0) {
                        fileOut.write(buffer, 0, len);
                        bytesDone += len;
                        sinceReport += len;
                        if (sinceReport >= PROGRESS_STEP_BYTES) {
                            sinceReport = 0;
                            if (cancelled.getAsBoolean()) {
                                return false;
                            }
                            report.accept(new UnpackProgress(filesDone, filesTotal, bytesDone, bytesTotal));
                        }
                    }
                }
                filesDone++;
                report.accept(new UnpackProgress(filesDone, filesTotal, bytesDone, bytesTotal));
            }
            return true;
        }
    }

    /** Název adresáře, do kterého se ZIP v pracovním adresáři rozbalí: název souboru bez přípony .zip. */
    static String extractDirName(File zip) {
        String name = zip.getName();
        if (name.toLowerCase(java.util.Locale.ROOT).endsWith(".zip")) {
            name = name.substring(0, name.length() - ".zip".length());
        }
        //soubor „.zip“, „..zip“ nebo „...zip“ by dal prázdný název, „.“ nebo „..“ a rozbalilo by se mimo vlastní
        //pracovní adresář
        if (name.isEmpty() || name.equals(".") || name.equals("..")) {
            return "rozbaleno";
        }
        return name;
    }

    /**
     * Smaže pracovní adresář vytvořený metodou {@link #unpack}. Jiný adresář nesmaže: maže jen adresáře, které tato
     * třída sama vytvořila a dosud nesmazala.
     */
    public static void deleteWorkDir(File workDir) {
        synchronized (WORK_DIRS) {
            if (workDir == null || !WORK_DIRS.remove(workDir)) {
                return;
            }
        }
        try {
            if (workDir.exists()) {
                Utils.deleteNonemptyDir(workDir);
            }
        } catch (IOException | RuntimeException e) {
            System.err.println("nelze smazat rozbalený balík " + workDir + ": " + e.getMessage());
        }
    }

    private static void deleteAtExit(File workDir) {
        synchronized (WORK_DIRS) {
            WORK_DIRS.add(workDir);
            if (!shutdownHookRegistered) {
                shutdownHookRegistered = true;
                Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    File[] left;
                    synchronized (WORK_DIRS) {
                        left = WORK_DIRS.toArray(new File[0]);
                    }
                    for (File dir : left) {
                        deleteWorkDir(dir);
                    }
                }, "kv-uklid-rozbalenych-baliku"));
            }
        }
    }
}
