package nkp.pspValidator.gui.desktop;

import nkp.pspValidator.shared.OperatingSystem;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Příkaz pro nové spuštění aplikace podle způsobu spuštění. Cesty jsou zadané jako řetězce ve tvaru cílového systému,
 * sestavení příkazu se proto testuje stejně na všech platformách. Údaje o procesu odpovídají tomu, co JVM skutečně
 * hlásí (ověřeno na macOS: {@code getInputArguments()} uvádí {@code --module-path=…} jako jeden argument a neuvádí
 * {@code -cp}; {@code sun.java.command} je hlavní třída nebo jar a za mezerou argumenty aplikace).
 */
public class RestartCommandTest {

    private static final List<String> NONE = Collections.emptyList();

    private static RestartCommand.Environment jpackage(OperatingSystem os, String appPath, String workingDir) {
        //udaje o Jave v aplikaci z jpackage nehraji roli
        return new RestartCommand.Environment(os, appPath, appPath, "/runtime", Arrays.asList("-Djpackage.app-version=3.0.0"),
                "/app/guiModule-3.0-shadow.jar", "nkp.pspValidator.gui.Launcher", null, null, NONE, workingDir);
    }

    // ===== jpackage =====

    @Test
    public void jpackageWindowsHandsLauncherOverToExplorer() {
        // spouštěč spuštěný přímo z běžící aplikace skončí spolu s ní (ověřeno na Windows 11, 24. 11. 2026), proto se
        // předává Průzkumníku; explorer.exe skončí hned a vrací 1 i při úspěchu
        RestartCommand.Plan plan = RestartCommand.build(jpackage(OperatingSystem.WINDOWS,
                "C:\\Users\\Uživatel\\AppData\\Local\\Komplexní validátor\\KomplexniValidator.exe", "C:\\Users\\Uživatel\\Dokumenty"));
        assertEquals(Arrays.asList("explorer.exe", "C:\\Users\\Uživatel\\AppData\\Local\\Komplexní validátor\\KomplexniValidator.exe"), plan.getCommand());
        assertNull(plan.getWorkingDir());
        assertTrue(plan.exitsAfterHandOver());
        assertFalse(plan.exitCodeTellsResult());
    }

    @Test
    public void jpackageLinuxRunsLauncherAgain() {
        RestartCommand.Plan plan = RestartCommand.build(jpackage(OperatingSystem.LINUX,
                "/home/uživatel/Komplexní validátor/bin/KomplexniValidator", "/home/uživatel"));
        assertEquals(Arrays.asList("/home/uživatel/Komplexní validátor/bin/KomplexniValidator"), plan.getCommand());
        assertEquals("/home/uživatel", plan.getWorkingDir());
        assertFalse(plan.exitsAfterHandOver());
    }

    /** macOS: nová instance balíčku přes LaunchServices, ne binárka uvnitř .app */
    @Test
    public void jpackageMacOpensNewInstanceOfBundle() {
        RestartCommand.Plan plan = RestartCommand.build(jpackage(OperatingSystem.MAC,
                "/Users/uživatel/Aplikace KV/KomplexniValidator.app/Contents/MacOS/KomplexniValidator", "/"));
        assertEquals(Arrays.asList("open", "-n", "/Users/uživatel/Aplikace KV/KomplexniValidator.app"), plan.getCommand());
        assertNull(plan.getWorkingDir());
        assertTrue(plan.exitsAfterHandOver());
        // open -n vrací nenulový kód, když balíček nejde otevřít
        assertTrue(plan.exitCodeTellsResult());
    }

    /** spouštěč mimo balíček .app (neobvyklé): spustí se přímo */
    @Test
    public void jpackageMacWithoutBundleRunsLauncher() {
        RestartCommand.Plan plan = RestartCommand.build(jpackage(OperatingSystem.MAC, "/opt/kv/bin/KomplexniValidator", "/tmp"));
        assertEquals(Arrays.asList("/opt/kv/bin/KomplexniValidator"), plan.getCommand());
        assertEquals("/tmp", plan.getWorkingDir());
        assertFalse(plan.exitsAfterHandOver());
        //.app jinde nez tesne pred Contents/MacOS
        plan = RestartCommand.build(jpackage(OperatingSystem.MAC, "/Apps/X.app/Contents/Resources/bin/KomplexniValidator", "/"));
        assertEquals(Arrays.asList("/Apps/X.app/Contents/Resources/bin/KomplexniValidator"), plan.getCommand());
    }

    /** prázdná vlastnost jpackage.app-path není jpackage */
    @Test
    public void blankJpackagePathMeansJava() {
        RestartCommand.Plan plan = RestartCommand.build(new RestartCommand.Environment(OperatingSystem.LINUX, " ", "/usr/bin/java", null,
                NONE, "/kv/gui.jar", "/kv/gui.jar", null, null, NONE, "/kv"));
        assertEquals(Arrays.asList("/usr/bin/java", "-jar", "/kv/gui.jar"), plan.getCommand());
    }

    // ===== java =====

    /** vývojový běh přes Gradle (úloha run): classpath, JavaFX na module path, hlavní třída Launcher */
    @Test
    public void gradleRunOnMac() {
        RestartCommand.Environment env = new RestartCommand.Environment(OperatingSystem.MAC, null,
                "/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home/bin/java", "/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home",
                Arrays.asList("--module-path=/Users/uživatel/.gradle/caches/javafx-graphics-17.0.18-mac-aarch64.jar:/Users/uživatel/.gradle/caches/javafx-base-17.0.18-mac-aarch64.jar",
                        "--add-modules=javafx.base,javafx.graphics", "-Dfile.encoding=UTF-8", "-Duser.country=CZ", "-Duser.language=cs"),
                "/Users/uživatel/Projekty KV/modules/guiModule/build/classes/java/main:/Users/uživatel/Projekty KV/modules/guiModule/build/resources/main",
                "nkp.pspValidator.gui.Launcher", null, null, NONE, "/Users/uživatel/Projekty KV/modules/guiModule");
        RestartCommand.Plan plan = RestartCommand.build(env);
        assertEquals(Arrays.asList("/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home/bin/java",
                "--module-path=/Users/uživatel/.gradle/caches/javafx-graphics-17.0.18-mac-aarch64.jar:/Users/uživatel/.gradle/caches/javafx-base-17.0.18-mac-aarch64.jar",
                "--add-modules=javafx.base,javafx.graphics", "-Dfile.encoding=UTF-8", "-Duser.country=CZ", "-Duser.language=cs",
                "-cp", "/Users/uživatel/Projekty KV/modules/guiModule/build/classes/java/main:/Users/uživatel/Projekty KV/modules/guiModule/build/resources/main",
                "nkp.pspValidator.gui.Launcher"), plan.getCommand());
        assertEquals("/Users/uživatel/Projekty KV/modules/guiModule", plan.getWorkingDir());
        assertFalse(plan.exitsAfterHandOver());
    }

    /** Windows: info().command() bývá k dispozici, argumenty JVM z MXBean; classpath s ; a mezerami zůstává jeden argument */
    @Test
    public void gradleRunOnWindows() {
        RestartCommand.Environment env = new RestartCommand.Environment(OperatingSystem.WINDOWS, null,
                "C:\\Program Files\\Eclipse Adoptium\\jdk-17\\bin\\java.exe", "C:\\Program Files\\Eclipse Adoptium\\jdk-17",
                Arrays.asList("--module-path=C:\\Users\\Uživatel\\.gradle\\javafx-base-17.0.18-win.jar", "-Dkv.configFile=C:\\Users\\Uživatel\\kv nastavení.properties"),
                "C:\\Projekty KV\\guiModule\\build\\classes\\java\\main;C:\\Projekty KV\\sharedModule\\build\\libs\\sharedModule.jar",
                "nkp.pspValidator.gui.Launcher", null, null, NONE, "C:\\Projekty KV\\guiModule");
        RestartCommand.Plan plan = RestartCommand.build(env);
        assertEquals(Arrays.asList("C:\\Program Files\\Eclipse Adoptium\\jdk-17\\bin\\java.exe",
                "--module-path=C:\\Users\\Uživatel\\.gradle\\javafx-base-17.0.18-win.jar", "-Dkv.configFile=C:\\Users\\Uživatel\\kv nastavení.properties",
                "-cp", "C:\\Projekty KV\\guiModule\\build\\classes\\java\\main;C:\\Projekty KV\\sharedModule\\build\\libs\\sharedModule.jar",
                "nkp.pspValidator.gui.Launcher"), plan.getCommand());
        assertEquals("C:\\Projekty KV\\guiModule", plan.getWorkingDir());
    }

    /** zip distribuce: java -jar s cestou s mezerami a diakritikou; argumenty aplikace se předají */
    @Test
    public void javaJarWithSpacesAndDiacritics() {
        RestartCommand.Environment env = new RestartCommand.Environment(OperatingSystem.LINUX, null, "/usr/lib/jvm/java-17/bin/java", null,
                Arrays.asList("-Xmx2g"), "/home/uživatel/Komplexní validátor 3.0/guiModule-3.0-shadow.jar",
                "/home/uživatel/Komplexní validátor 3.0/guiModule-3.0-shadow.jar --dev první", null, null,
                Arrays.asList("--dev", "první"), "/home/uživatel");
        assertEquals(Arrays.asList("/usr/lib/jvm/java-17/bin/java", "-Xmx2g", "-jar",
                "/home/uživatel/Komplexní validátor 3.0/guiModule-3.0-shadow.jar", "--dev", "první"),
                RestartCommand.build(env).getCommand());
    }

    /** java -jar na Windows s relativní cestou (pracovní adresář se zachová) */
    @Test
    public void javaJarOnWindowsRelative() {
        RestartCommand.Environment env = new RestartCommand.Environment(OperatingSystem.WINDOWS, null, "C:\\jdk\\bin\\javaw.exe", null,
                NONE, "guiModule-3.0-shadow.jar", "guiModule-3.0-shadow.jar", null, null, NONE, "D:\\KV portable");
        RestartCommand.Plan plan = RestartCommand.build(env);
        assertEquals(Arrays.asList("C:\\jdk\\bin\\javaw.exe", "-jar", "guiModule-3.0-shadow.jar"), plan.getCommand());
        assertEquals("D:\\KV portable", plan.getWorkingDir());
    }

    /** jediný jar na classpath, ale spuštěný přes -cp a hlavní třídu: není to -jar */
    @Test
    public void singleJarOnClassPathWithMainClass() {
        RestartCommand.Environment env = new RestartCommand.Environment(OperatingSystem.MAC, null, "/jdk/bin/java", null,
                NONE, "/kv/gui.jar", "nkp.pspValidator.gui.Launcher", null, null, NONE, "/kv");
        assertEquals(Arrays.asList("/jdk/bin/java", "-cp", "/kv/gui.jar", "nkp.pspValidator.gui.Launcher"),
                RestartCommand.build(env).getCommand());
    }

    /** jar, jehož název je začátkem jiného: sun.java.command „/kv/gui.jar2 x“ není spuštění /kv/gui.jar */
    @Test
    public void jarPrefixIsNotEnough() {
        RestartCommand.Environment env = new RestartCommand.Environment(OperatingSystem.MAC, null, "/jdk/bin/java", null,
                NONE, "/kv/gui.jar", "/kv/gui.jar2 x", null, null, NONE, "/kv");
        //„/kv/gui.jar2“ neni nazev tridy, prikaz nejde sestavit
        assertNull(RestartCommand.build(env));
    }

    /** classpath s více položkami končící .jar není spuštění -jar (oddělovač podle systému) */
    @Test
    public void multiEntryClassPathEndingWithJar() {
        RestartCommand.Environment env = new RestartCommand.Environment(OperatingSystem.WINDOWS, null, "C:\\jdk\\bin\\java.exe", null,
                NONE, "C:\\a.jar;C:\\b.jar", "nkp.pspValidator.gui.Launcher", null, null, NONE, "C:\\");
        assertEquals(Arrays.asList("C:\\jdk\\bin\\java.exe", "-cp", "C:\\a.jar;C:\\b.jar", "nkp.pspValidator.gui.Launcher"),
                RestartCommand.build(env).getCommand());
    }

    /** bez cesty k procesu: java z java.home podle systému */
    @Test
    public void javaFromJavaHome() {
        RestartCommand.Environment win = new RestartCommand.Environment(OperatingSystem.WINDOWS, null, null, "C:\\Program Files\\Java\\jdk-17\\",
                NONE, "C:\\kv\\gui.jar", "C:\\kv\\gui.jar", null, null, NONE, "C:\\kv");
        assertEquals(Arrays.asList("C:\\Program Files\\Java\\jdk-17\\bin\\java.exe", "-jar", "C:\\kv\\gui.jar"), RestartCommand.build(win).getCommand());
        RestartCommand.Environment mac = new RestartCommand.Environment(OperatingSystem.MAC, null, " ", "/Library/Java/jdk 17/Contents/Home",
                NONE, "/kv/gui.jar", "/kv/gui.jar", null, null, NONE, "/kv");
        assertEquals(Arrays.asList("/Library/Java/jdk 17/Contents/Home/bin/java", "-jar", "/kv/gui.jar"), RestartCommand.build(mac).getCommand());
        RestartCommand.Environment unknown = new RestartCommand.Environment(null, null, null, "/jdk",
                NONE, "/kv/gui.jar", "/kv/gui.jar", null, null, NONE, "/kv");
        assertEquals(Arrays.asList("/jdk/bin/java", "-jar", "/kv/gui.jar"), RestartCommand.build(unknown).getCommand());
    }

    /** ladicí agent se vynechá (port by byl obsazený), ostatní agenti zůstanou */
    @Test
    public void debugAgentIsDropped() {
        RestartCommand.Environment env = new RestartCommand.Environment(OperatingSystem.MAC, null, "/jdk/bin/java", null,
                Arrays.asList("-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005", "-Xrunjdwp:transport=dt_socket",
                        "-javaagent:/opt/agent.jar", "-agentlib:hprof", "-Dx=1"),
                "/kv/classes", "nkp.pspValidator.gui.Launcher", null, null, NONE, "/kv");
        assertEquals(Arrays.asList("/jdk/bin/java", "-javaagent:/opt/agent.jar", "-agentlib:hprof", "-Dx=1",
                "-cp", "/kv/classes", "nkp.pspValidator.gui.Launcher"), RestartCommand.build(env).getCommand());
    }

    /** spuštění z modulu (-m): modul a třída z vlastností jdk.module.main a jdk.module.main.class */
    @Test
    public void moduleMain() {
        RestartCommand.Environment env = new RestartCommand.Environment(OperatingSystem.LINUX, null, "/jdk/bin/java", null,
                Arrays.asList("--module-path=/kv/mods"), "", "guiModule/nkp.pspValidator.gui.Launcher", "guiModule",
                "nkp.pspValidator.gui.Launcher", Arrays.asList("a"), "/kv");
        assertEquals(Arrays.asList("/jdk/bin/java", "--module-path=/kv/mods", "-m", "guiModule/nkp.pspValidator.gui.Launcher", "a"),
                RestartCommand.build(env).getCommand());
        RestartCommand.Environment withoutClass = new RestartCommand.Environment(OperatingSystem.LINUX, null, "/jdk/bin/java", null,
                NONE, null, "guiModule", "guiModule", null, NONE, "/kv");
        assertEquals(Arrays.asList("/jdk/bin/java", "-m", "guiModule"), RestartCommand.build(withoutClass).getCommand());
    }

    // ===== příkaz sestavit nejde =====

    @Test
    public void noJava() {
        assertNull(RestartCommand.build(new RestartCommand.Environment(OperatingSystem.MAC, null, null, null,
                NONE, "/kv/gui.jar", "/kv/gui.jar", null, null, NONE, "/kv")));
        assertNull(RestartCommand.build(new RestartCommand.Environment(OperatingSystem.MAC, null, "", " ",
                NONE, "/kv/gui.jar", "/kv/gui.jar", null, null, NONE, "/kv")));
    }

    @Test
    public void noClassPathOrCommand() {
        assertNull(RestartCommand.build(new RestartCommand.Environment(OperatingSystem.MAC, null, "/jdk/bin/java", null,
                NONE, null, "nkp.pspValidator.gui.Launcher", null, null, NONE, "/kv")));
        assertNull(RestartCommand.build(new RestartCommand.Environment(OperatingSystem.MAC, null, "/jdk/bin/java", null,
                NONE, "", "nkp.pspValidator.gui.Launcher", null, null, NONE, "/kv")));
        assertNull(RestartCommand.build(new RestartCommand.Environment(OperatingSystem.MAC, null, "/jdk/bin/java", null,
                NONE, "/kv/classes", null, null, null, NONE, "/kv")));
        assertNull(RestartCommand.build(new RestartCommand.Environment(OperatingSystem.MAC, null, "/jdk/bin/java", null,
                NONE, "/kv/classes", "  ", null, null, NONE, "/kv")));
    }

    /** sun.java.command nezačíná názvem třídy (např. jar mimo classpath): nejde */
    @Test
    public void unrecognizedMain() {
        assertNull(RestartCommand.build(new RestartCommand.Environment(OperatingSystem.MAC, null, "/jdk/bin/java", null,
                NONE, "/kv/classes", "/jiný/gui.jar", null, null, NONE, "/kv")));
    }

    @Test
    public void planDescription() {
        RestartCommand.Plan plan = RestartCommand.build(jpackage(OperatingSystem.LINUX, "/opt/kv/bin/KomplexniValidator", "/home/u"));
        assertEquals("[/opt/kv/bin/KomplexniValidator] v adresáři /home/u", plan.toString());
        plan = RestartCommand.build(jpackage(OperatingSystem.MAC, "/Applications/KomplexniValidator.app/Contents/MacOS/KomplexniValidator", "/"));
        assertEquals("[open, -n, /Applications/KomplexniValidator.app]", plan.toString());
    }

    /** příkaz se nedá měnit zvenku */
    @Test(expected = UnsupportedOperationException.class)
    public void commandIsUnmodifiable() {
        RestartCommand.build(jpackage(OperatingSystem.LINUX, "/opt/kv/bin/KomplexniValidator", "/")).getCommand().add("x");
    }
}
