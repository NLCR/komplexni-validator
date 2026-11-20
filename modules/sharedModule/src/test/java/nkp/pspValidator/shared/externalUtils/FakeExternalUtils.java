package nkp.pspValidator.shared.externalUtils;

import nkp.pspValidator.shared.OperatingSystem;
import nkp.pspValidator.shared.Platform;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertTrue;

/**
 * Falesne externi nastroje pro testy spousteni (ExternalUtilManagerTest) na Unixu i na Windows.
 * <p>
 * Falesny nastroj vypise ulozeny text na stdout a stderr, skonci danym navratovym kodem a argumenty, se kterymi byl
 * spusten, zapise do souboru &lt;nazev&gt;.args vedle sebe ve tvaru [arg1][arg2]... (UTF-8). Vypisovany text se
 * zkopiruje vedle nastroje (&lt;nazev&gt;.stdout, &lt;nazev&gt;.stderr) a nastroj ho cte pres svuj adresar, takze
 * v nastroji samotnem neni zadna cesta s diakritikou.
 * <ul>
 * <li>Linux a macOS: skript pro /bin/sh pod nazvem programu z externalUtils.xml (napr. "jhove", ale i "magick.exe"
 * nebo "jhove.bat" pri testu konfigurace pro Windows).</li>
 * <li>Windows: davka (.bat). Davku jde spustit jen pod nazvem s priponou .bat nebo .cmd (CreateProcess jinou priponu
 * nespusti a bez pripony doplni .exe), proto se na Windows prikazy z externalUtils.xml prevedou
 * ({@link #toBatchCommands(String)}): nazev programu za ${UTIL_PATH} dostane priponu .bat misto .exe nebo misto zadne.
 * Programy, ktere uz v konfiguraci .bat jsou (jhove.bat, verapdf.bat), zustanou beze zmeny a testuji se presne tak,
 * jak je spousti aplikace. U programu .exe se tim na Windows testuje vse krome samotneho spusteni .exe (davku Windows
 * spousti pres cmd.exe, ktery argumenty rozdeli podle sebe).</li>
 * </ul>
 */
public final class FakeExternalUtils {

    public static final boolean WINDOWS_HOST = Platform.detectOs().getOperatingSystem() == OperatingSystem.WINDOWS;

    /**
     * ${UTIL_PATH}, za nim nazev programu a pripadne pripona .exe nebo .bat, a pak mezera nebo konec prikazu.
     * "java -jar ${UTIL_PATH}epubcheck.jar" se nezmeni (za nazvem je .jar).
     */
    private static final Pattern UTIL_PROGRAM = Pattern.compile("\\$\\{UTIL_PATH\\}([A-Za-z0-9_\\-]+)(?:\\.exe|\\.bat)?(?=\\s|<|$)");

    private FakeExternalUtils() {
    }

    /**
     * Nazev souboru falesneho nastroje na tomto systemu pro nazev programu z externalUtils.xml: na Unixu stejny,
     * na Windows s priponou .bat (viz {@link #toBatchCommands(String)}).
     */
    public static String hostFileName(String programName) {
        if (!WINDOWS_HOST) {
            return programName;
        }
        String base = programName;
        if (base.endsWith(".exe") || base.endsWith(".bat")) {
            base = base.substring(0, base.length() - 4);
        }
        return base + ".bat";
    }

    /**
     * Prikazy konfigurace externalUtils.xml pro falesne nastroje jako davky .bat (pouziva se jen na Windows).
     */
    public static String toBatchCommands(String externalUtilsXml) {
        Matcher m = UTIL_PROGRAM.matcher(externalUtilsXml);
        StringBuffer result = new StringBuffer();
        while (m.find()) {
            m.appendReplacement(result, Matcher.quoteReplacement("${UTIL_PATH}" + m.group(1) + ".bat"));
        }
        m.appendTail(result);
        return result.toString();
    }

    /**
     * Konfigurace externalUtils.xml pro tento system: na Unixu puvodni soubor, na Windows jeho kopie v dir
     * s prikazy prevedenymi na davky (viz {@link #toBatchCommands(String)}).
     */
    public static File hostConfig(File config, File dir) throws IOException {
        if (!WINDOWS_HOST) {
            return config;
        }
        File converted = new File(dir, "externalUtils-bat.xml");
        String xml = new String(Files.readAllBytes(config.toPath()), StandardCharsets.UTF_8);
        Files.write(converted.toPath(), toBatchCommands(xml).getBytes(StandardCharsets.UTF_8));
        return converted;
    }

    /**
     * Falesny nastroj programName (nazev z externalUtils.xml) v adresari dir. stdout a stderr jsou soubory s textem
     * k vypsani, nebo null (nic nevypise).
     *
     * @return spustitelny soubor nastroje
     */
    public static File create(File dir, String programName, File stdout, File stderr, int exitCode) throws IOException {
        String fileName = hostFileName(programName);
        File stdoutCopy = new File(dir, fileName + ".stdout");
        File stderrCopy = new File(dir, fileName + ".stderr");
        Files.deleteIfExists(stdoutCopy.toPath());
        Files.deleteIfExists(stderrCopy.toPath());
        if (stdout != null) {
            Files.copy(stdout.toPath(), stdoutCopy.toPath());
        }
        if (stderr != null) {
            Files.copy(stderr.toPath(), stderrCopy.toPath());
        }
        String content = WINDOWS_HOST
                ? batch(fileName, stdout != null, stderr != null, exitCode)
                : shellScript(fileName, stdout != null, stderr != null, exitCode);
        File file = new File(dir, fileName);
        Files.write(file.toPath(), content.getBytes(StandardCharsets.US_ASCII));
        assertTrue(file.setExecutable(true));
        return file;
    }

    /**
     * Argumenty posledniho spusteni falesneho nastroje programName v adresari dir, ve tvaru [arg1][arg2]...
     */
    public static String recordedArgs(File dir, String programName) throws IOException {
        File args = new File(dir, hostFileName(programName) + ".args");
        return new String(Files.readAllBytes(args.toPath()), StandardCharsets.UTF_8);
    }

    private static String shellScript(String fileName, boolean stdout, boolean stderr, int exitCode) {
        StringBuilder s = new StringBuilder("#!/bin/sh\n");
        s.append("D=$(dirname \"$0\")\n");
        s.append(": > \"$D/").append(fileName).append(".args\"\n");
        s.append("for a in \"$@\"; do printf '[%s]' \"$a\" >> \"$D/").append(fileName).append(".args\"; done\n");
        if (stdout) {
            s.append("cat \"$D/").append(fileName).append(".stdout\"\n");
        }
        if (stderr) {
            s.append("cat \"$D/").append(fileName).append(".stderr\" >&2\n");
        }
        s.append("exit ").append(exitCode).append('\n');
        return s.toString();
    }

    /**
     * Davka pro cmd.exe. Adresar davky se ulozi pred cyklem (shift posouva i %0). chcp 65001: argumenty zapisuje
     * prikaz set /p v kodove strance konzole, bez prepnuti na UTF-8 by diakritika v argumentech dopadla podle kodove
     * stranky OEM (u ceskych Windows 852). "type" vypise soubor po bajtech beze zmeny (UTF-8, konce radku LF).
     */
    private static String batch(String fileName, boolean stdout, boolean stderr, int exitCode) {
        StringBuilder s = new StringBuilder();
        s.append("@echo off\r\n");
        s.append("setlocal EnableExtensions DisableDelayedExpansion\r\n");
        s.append("chcp 65001 >nul\r\n");
        s.append("set \"FAKE_DIR=%~dp0\"\r\n");
        s.append("type nul > \"%FAKE_DIR%").append(fileName).append(".args\"\r\n");
        s.append(":dalsi\r\n");
        s.append("if \"%~1\"==\"\" goto konec\r\n");
        s.append(">>\"%FAKE_DIR%").append(fileName).append(".args\" <nul set /p \"=[%~1]\"\r\n");
        s.append("shift\r\n");
        s.append("goto dalsi\r\n");
        s.append(":konec\r\n");
        if (stdout) {
            s.append("type \"%FAKE_DIR%").append(fileName).append(".stdout\"\r\n");
        }
        if (stderr) {
            s.append("type \"%FAKE_DIR%").append(fileName).append(".stderr\" 1>&2\r\n");
        }
        s.append("exit /b ").append(exitCode).append("\r\n");
        return s.toString();
    }
}
