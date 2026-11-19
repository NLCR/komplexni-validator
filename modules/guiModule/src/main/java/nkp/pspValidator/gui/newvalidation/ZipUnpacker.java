package nkp.pspValidator.gui.newvalidation;

import nkp.pspValidator.shared.engine.Utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

/**
 * Rozbalení balíku ze souboru ZIP před validací, bez vazby na JavaFX. Používá ho dialog rozbalování u jednoho balíku
 * i běh skupiny balíků (tam bez dialogu).
 * <p>
 * Postup: kontrola ZIPu a adresáře pro dočasné soubory, nový prázdný pracovní adresář
 * {@code <tmp>/kv-rozbaleno-<náhodné>} a v něm adresář {@code <název souboru bez .zip>}, do kterého se ZIP rozbalí.
 * Když je v rozbaleném adresáři jediná položka, je balíkem ona, jinak celý rozbalený adresář.
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
        private final File pspDir;

        Result(File containerDir, File pspDir) {
            this.containerDir = containerDir;
            this.pspDir = pspDir;
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

    /**
     * Rozbalí ZIP. Volat z pracovního vlákna.
     *
     * @param cancelled ptá se mezi kroky, zda bylo rozbalování zrušeno (samotné rozbalení archivu se nepřeruší)
     * @param messages  průběžné zprávy pro uživatele („Rozbaluji … do adresáře …“), smí být null
     * @return výsledek, nebo null, když bylo rozbalování zrušeno
     * @throws UnpackException soubor není ZIP, adresář pro dočasné soubory nelze použít, chyba při čtení nebo zápisu
     */
    public Result unpack(File zip, BooleanSupplier cancelled, Consumer<String> messages) throws UnpackException {
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
                Utils.unzip(zip, extractDir);
                if (cancelled.getAsBoolean()) {
                    return null;
                }
                File pspDir = extractDir;
                File[] filesInContainer = extractDir.listFiles();
                if (filesInContainer != null && filesInContainer.length == 1) {
                    pspDir = filesInContainer[0];
                }
                done = true;
                return new Result(workDir, pspDir);
            } finally {
                if (!done) {
                    //zruseno nebo chyba: castecne rozbaleny obsah nenechavat na disku
                    deleteWorkDir(workDir);
                }
            }
        } catch (ZipException e) {
            throw new UnpackException(String.format("Chyba: Soubor %s není korektní ZIP: %s!", zip.getAbsolutePath(), e.getMessage()));
        } catch (IOException e) {
            throw new UnpackException(String.format("Chyba zpracování ZIP souboru %s: %s!", zip.getAbsolutePath(), e.getMessage()));
        }
    }

    /** Název adresáře, do kterého se ZIP v pracovním adresáři rozbalí: název souboru bez přípony .zip. */
    static String extractDirName(File zip) {
        String name = zip.getName();
        if (name.toLowerCase(java.util.Locale.ROOT).endsWith(".zip")) {
            name = name.substring(0, name.length() - ".zip".length());
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
