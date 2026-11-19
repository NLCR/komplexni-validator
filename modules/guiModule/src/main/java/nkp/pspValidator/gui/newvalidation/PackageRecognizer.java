package nkp.pspValidator.gui.newvalidation;

import nkp.pspValidator.shared.Dmf;
import nkp.pspValidator.shared.DmfDetector;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

/**
 * Rozpoznání typu a verze balíku před validací, stejnou logikou jako validace ({@link DmfDetector}: typ z atributu
 * TYPE hlavního METS, verze z metadataversion v info.xml).
 * <p>
 * U ZIPu se archiv celý nerozbaluje: z centrálního adresáře ZIPu se určí kořen balíku stejně jako po rozbalení
 * (UnzipAndValidateDialogController: jediná položka v kořeni archivu je adresář balíku, jinak je balíkem kořen archivu)
 * a do dočasného adresáře se vybalí jen soubory XML přímo v kořeni balíku (METS a info), nad kterými se pustí
 * {@link DmfDetector}. Dočasný adresář se hned smaže.
 * <p>
 * Volá se z pracovního vlákna (čtení ZIPu a XML může trvat), nemá vazbu na JavaFX.
 */
public class PackageRecognizer {

    public Recognition recognize(ValidationSource source, String pathText) {
        String pathProblem = source.checkPath(pathText);
        if (pathProblem != null) {
            return Recognition.invalidPath(pathProblem);
        }
        File file = new File(pathText.trim());
        switch (source) {
            case DIRECTORY:
                return recognizeDir(file, null);
            case ZIP:
                return recognizeZip(file);
            default:
                throw new IllegalStateException();
        }
    }

    private Recognition recognizeDir(File dir, File hiddenTmpDir) {
        DmfDetector detector = new DmfDetector();
        Dmf.Type type;
        try {
            type = detector.detectDmfType(dir);
        } catch (Exception e) {
            return Recognition.notRecognized("Typ dokumentu nelze zjistit: " + message(e, hiddenTmpDir));
        }
        try {
            String version = detector.detectDmfVersionFromInfoFile(type, dir);
            return Recognition.recognized(type, version, null);
        } catch (Exception e) {
            return Recognition.recognized(type, null, message(e, hiddenTmpDir));
        }
    }

    private Recognition recognizeZip(File zipFile) {
        Path tmpDir = null;
        try (ZipFile zip = new ZipFile(zipFile)) {
            Set<String> topLevel = new LinkedHashSet<>();
            Set<String> topLevelDirs = new LinkedHashSet<>();
            for (Enumeration<? extends ZipEntry> en = zip.entries(); en.hasMoreElements(); ) {
                String name = en.nextElement().getName();
                int slash = name.indexOf('/');
                String first = slash < 0 ? name : name.substring(0, slash);
                if (!first.isEmpty()) {
                    topLevel.add(first);
                    if (slash >= 0) {
                        topLevelDirs.add(first);
                    }
                }
            }
            if (topLevel.isEmpty()) {
                return Recognition.notRecognized("Soubor ZIP je prázdný.");
            }
            String prefix = "";
            if (topLevel.size() == 1) {
                String only = topLevel.iterator().next();
                if (!topLevelDirs.contains(only)) {
                    return Recognition.notRecognized(String.format("Soubor ZIP obsahuje jen soubor %s, ne adresář balíku.", only));
                }
                prefix = only + "/";
            }
            tmpDir = Files.createTempDirectory("kv-rozpoznani");
            for (Enumeration<? extends ZipEntry> en = zip.entries(); en.hasMoreElements(); ) {
                ZipEntry entry = en.nextElement();
                String name = entry.getName();
                if (entry.isDirectory() || !name.startsWith(prefix)) {
                    continue;
                }
                String rest = name.substring(prefix.length());
                if (rest.isEmpty() || rest.contains("/") || rest.contains("\\")
                        || !rest.toLowerCase(Locale.ROOT).endsWith(".xml")) {
                    continue;
                }
                try (InputStream in = zip.getInputStream(entry)) {
                    Files.copy(in, tmpDir.resolve(rest), StandardCopyOption.REPLACE_EXISTING);
                }
            }
            return recognizeDir(tmpDir.toFile(), tmpDir.toFile());
        } catch (ZipException e) {
            return Recognition.notRecognized(String.format("Soubor není platný ZIP (%s).", e.getMessage()));
        } catch (IOException e) {
            return Recognition.notRecognized(String.format("Soubor ZIP nelze přečíst (%s).", e.getMessage()));
        } finally {
            deleteQuietly(tmpDir);
        }
    }

    private static String message(Exception e, File hiddenTmpDir) {
        String message = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
        if (hiddenTmpDir != null) {
            //hlasky o chybe XML obsahuji cestu k docasnemu souboru, uzivatel zna jen ZIP
            message = message.replace(hiddenTmpDir.getAbsolutePath() + File.separator, "");
        }
        return message;
    }

    private static void deleteQuietly(Path dir) {
        if (dir == null) {
            return;
        }
        try (Stream<Path> paths = Files.walk(dir)) {
            paths.sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
        } catch (IOException e) {
            //docasny adresar, nic dalsiho
        }
    }
}
