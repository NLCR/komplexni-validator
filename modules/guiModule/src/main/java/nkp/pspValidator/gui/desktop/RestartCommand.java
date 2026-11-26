package nkp.pspValidator.gui.desktop;

import nkp.pspValidator.shared.OperatingSystem;
import nkp.pspValidator.shared.Platform;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Příkaz, kterým se aplikace spustí znovu (restart po změně adresáře konfigurace validátoru v Nastavení). Jen sestavení
 * příkazu z údajů o běžícím procesu ({@link Environment}), bez JavaFX a bez spouštění (spouští {@link Restarter}).
 * <ul>
 * <li>Aplikace z jpackage (instalátor, portable; spouštěč nastavuje systémovou vlastnost {@code jpackage.app-path}
 * na cestu ke spouštěči): Linux spustí znovu tentýž spouštěč ({@code bin/KomplexniValidator}) se stejným pracovním
 * adresářem. Na Windows se spouštěč ({@code Komplexní validátor.exe}) předá Průzkumníku ({@code explorer.exe <spouštěč>}),
 * tedy stejně jako při spuštění poklepáním: proces spuštěný přímo z běžící aplikace by skončil spolu s ní (ověřeno
 * 24. 11. 2026 na Windows 11 s instalátorem z jpackage 17: nová instance se objevila a po ukončení staré zmizela).
 * {@code explorer.exe} skončí hned a jeho návratový kód o výsledku nevypovídá (vrací 1 i při úspěchu). Na macOS je spouštěč binárka uvnitř balíčku
 * ({@code KomplexniValidator.app/Contents/MacOS/KomplexniValidator}); spouští se přes {@code open -n <balíček.app>}, tedy
 * přes LaunchServices jako při spuštění z Finderu nebo Docku: nová instance je samostatná aplikace (není potomkem
 * končícího procesu, dostane Dock, nabídky a fokus jako běžně spuštěná aplikace), {@code -n} otevře novou instanci
 * i v době, kdy stará ještě běží (bez něj by systém jen aktivoval končící instanci). Příkaz {@code open} skončí hned
 * po předání požadavku. Když cesta nemá tvar balíčku, spustí se přímo spouštěč.</li>
 * <li>Běh z {@code java} (vývojový běh přes Gradle, IDE, {@code java -jar} ze zip distribuce): stejná Java
 * ({@code ProcessHandle.info().command()}, jinak {@code java.home/bin/java}) se stejnými argumenty JVM (z
 * {@code RuntimeMXBean.getInputArguments()}: {@code -D…}, {@code -X…}, {@code --module-path=…}, {@code --add-modules=…};
 * vynechá se jen ladicí agent JDWP, jehož port by byl obsazený), pak {@code -jar <jar>}, nebo {@code -cp <classpath>
 * <hlavní třída>}, nebo {@code -m <modul>/<třída>}, a stejnými argumenty aplikace; pracovní adresář se zachová.
 * Příkaz se skládá z vlastností JVM, ne z {@code ProcessHandle.info().arguments()}: to na Windows bývá prázdné.</li>
 * <li>Jinak (chybí Java, classpath nebo hlavní třída) příkaz sestavit nejde a {@link #build} vrátí null.</li>
 * </ul>
 */
public final class RestartCommand {

    /** spouštěč v balíčku macOS: <balíček>.app/Contents/MacOS/<spouštěč> */
    private static final Pattern MAC_BUNDLE_LAUNCHER = Pattern.compile("^(.+\\.app)/Contents/MacOS/[^/]+$");
    /** plně kvalifikovaný název třídy */
    private static final Pattern CLASS_NAME = Pattern.compile("[\\p{L}_$][\\p{L}\\p{N}_$]*(\\.[\\p{L}_$][\\p{L}\\p{N}_$]*)*");

    private RestartCommand() {
    }

    /** Údaje o běžícím procesu, ze kterých se příkaz skládá (v testech zadané ručně). */
    public static final class Environment {
        final OperatingSystem os;
        final String jpackageAppPath;
        final String javaExecutable;
        final String javaHome;
        final List<String> jvmArgs;
        final String classPath;
        final String javaCommand;
        final String moduleMain;
        final String moduleMainClass;
        final List<String> appArgs;
        final String workingDir;

        /**
         * @param os              null = neznámý, chová se jako Linux
         * @param jpackageAppPath vlastnost {@code jpackage.app-path}, null mimo jpackage
         * @param javaExecutable  spustitelný soubor Javy běžícího procesu, null když ho nelze zjistit
         * @param javaHome        vlastnost {@code java.home} (náhradní cesta k Javě)
         * @param jvmArgs         argumenty JVM ({@code RuntimeMXBean.getInputArguments()})
         * @param classPath       vlastnost {@code java.class.path}
         * @param javaCommand     vlastnost {@code sun.java.command}: hlavní třída nebo jar a za mezerou argumenty
         * @param moduleMain      vlastnost {@code jdk.module.main} (spuštění {@code -m}), jinak null
         * @param moduleMainClass vlastnost {@code jdk.module.main.class}, jinak null
         * @param appArgs         argumenty aplikace
         * @param workingDir      pracovní adresář ({@code user.dir})
         */
        public Environment(OperatingSystem os, String jpackageAppPath, String javaExecutable, String javaHome,
                           List<String> jvmArgs, String classPath, String javaCommand, String moduleMain,
                           String moduleMainClass, List<String> appArgs, String workingDir) {
            this.os = os;
            this.jpackageAppPath = jpackageAppPath;
            this.javaExecutable = javaExecutable;
            this.javaHome = javaHome;
            this.jvmArgs = jvmArgs == null ? Collections.emptyList() : jvmArgs;
            this.classPath = classPath;
            this.javaCommand = javaCommand;
            this.moduleMain = moduleMain;
            this.moduleMainClass = moduleMainClass;
            this.appArgs = appArgs == null ? Collections.emptyList() : appArgs;
            this.workingDir = workingDir;
        }

        /** Údaje o právě běžícím procesu. */
        public static Environment current(List<String> appArgs) {
            String javaExecutable = null;
            try {
                javaExecutable = ProcessHandle.current().info().command().orElse(null);
            } catch (RuntimeException e) {
                //bez cesty k procesu se pouzije java.home
            }
            List<String> jvmArgs;
            try {
                jvmArgs = new ArrayList<>(ManagementFactory.getRuntimeMXBean().getInputArguments());
            } catch (RuntimeException e) {
                jvmArgs = null;
            }
            return new Environment(Platform.detectOs().getOperatingSystem(),
                    System.getProperty("jpackage.app-path"),
                    javaExecutable,
                    System.getProperty("java.home"),
                    jvmArgs,
                    System.getProperty("java.class.path"),
                    System.getProperty("sun.java.command"),
                    System.getProperty("jdk.module.main"),
                    System.getProperty("jdk.module.main.class"),
                    appArgs,
                    System.getProperty("user.dir"));
        }
    }

    /** Sestavený příkaz. */
    public static final class Plan {
        private final List<String> command;
        private final String workingDir;
        private final boolean exitsAfterHandOver;
        private final boolean exitCodeTellsResult;

        Plan(List<String> command, String workingDir, boolean exitsAfterHandOver) {
            this(command, workingDir, exitsAfterHandOver, true);
        }

        Plan(List<String> command, String workingDir, boolean exitsAfterHandOver, boolean exitCodeTellsResult) {
            this.command = Collections.unmodifiableList(command);
            this.workingDir = workingDir;
            this.exitsAfterHandOver = exitsAfterHandOver;
            this.exitCodeTellsResult = exitCodeTellsResult;
        }

        /**
         * U příkazu, který jen předá spuštění systému: nenulový návratový kód znamená chybu ({@code open -n}); false,
         * když kód o výsledku nevypovídá ({@code explorer.exe}).
         */
        public boolean exitCodeTellsResult() {
            return exitCodeTellsResult;
        }

        public List<String> getCommand() {
            return command;
        }

        /** Pracovní adresář nového procesu, null = nezáleží na něm. */
        public String getWorkingDir() {
            return workingDir;
        }

        /**
         * Příkaz jen předá spuštění systému a hned skončí ({@code open -n}); jinak je příkaz sám novou instancí aplikace
         * a má běžet dál.
         */
        public boolean exitsAfterHandOver() {
            return exitsAfterHandOver;
        }

        @Override
        public String toString() {
            return command + (workingDir != null ? " v adresáři " + workingDir : "");
        }
    }

    /** Příkaz pro nové spuštění aplikace, null když ho sestavit nejde. */
    public static Plan build(Environment env) {
        if (env.jpackageAppPath != null && !env.jpackageAppPath.trim().isEmpty()) {
            return jpackage(env);
        }
        return java(env);
    }

    private static Plan jpackage(Environment env) {
        String launcher = env.jpackageAppPath;
        if (env.os == OperatingSystem.MAC) {
            Matcher bundle = MAC_BUNDLE_LAUNCHER.matcher(launcher);
            if (bundle.matches()) {
                return new Plan(list("open", "-n", bundle.group(1)), null, true);
            }
        }
        if (env.os == OperatingSystem.WINDOWS) {
            return new Plan(list("explorer.exe", launcher), null, true, false);
        }
        return new Plan(list(launcher), env.workingDir, false);
    }

    private static Plan java(Environment env) {
        String java = javaExecutable(env);
        if (java == null) {
            return null;
        }
        List<String> command = list(java);
        for (String arg : env.jvmArgs) {
            if (!isDebugAgent(arg)) {
                command.add(arg);
            }
        }
        if (env.moduleMain != null && !env.moduleMain.isEmpty()) {
            String module = env.moduleMain;
            if (!module.contains("/") && env.moduleMainClass != null && !env.moduleMainClass.isEmpty()) {
                module = module + "/" + env.moduleMainClass;
            }
            command.add("-m");
            command.add(module);
        } else {
            if (env.classPath == null || env.classPath.isEmpty() || env.javaCommand == null || env.javaCommand.trim().isEmpty()) {
                return null;
            }
            if (isJar(env)) {
                command.add("-jar");
                command.add(env.classPath);
            } else {
                String mainClass = env.javaCommand.trim().split(" ", 2)[0];
                if (!CLASS_NAME.matcher(mainClass).matches()) {
                    return null;
                }
                command.add("-cp");
                command.add(env.classPath);
                command.add(mainClass);
            }
        }
        command.addAll(env.appArgs);
        return new Plan(command, env.workingDir, false);
    }

    /**
     * Spuštění {@code java -jar}: classpath je jen ten jar a {@code sun.java.command} jím začíná (cesta k jaru může
     * obsahovat mezery, proto se neporovnává jen první slovo).
     */
    private static boolean isJar(Environment env) {
        String cp = env.classPath;
        String separator = env.os == OperatingSystem.WINDOWS ? ";" : ":";
        if (cp.contains(separator) || !cp.toLowerCase(java.util.Locale.ROOT).endsWith(".jar")) {
            return false;
        }
        String command = env.javaCommand.trim();
        return command.equals(cp) || command.startsWith(cp + " ");
    }

    private static String javaExecutable(Environment env) {
        if (env.javaExecutable != null && !env.javaExecutable.trim().isEmpty()) {
            return env.javaExecutable;
        }
        if (env.javaHome == null || env.javaHome.trim().isEmpty()) {
            return null;
        }
        boolean windows = env.os == OperatingSystem.WINDOWS;
        String separator = windows ? "\\" : "/";
        String home = env.javaHome.endsWith(separator) ? env.javaHome.substring(0, env.javaHome.length() - 1) : env.javaHome;
        return home + separator + "bin" + separator + (windows ? "java.exe" : "java");
    }

    /** ladicí agent: nový proces by chtěl tentýž port */
    private static boolean isDebugAgent(String arg) {
        return arg.startsWith("-agentlib:jdwp") || arg.startsWith("-Xrunjdwp");
    }

    private static List<String> list(String... items) {
        List<String> result = new ArrayList<>();
        Collections.addAll(result, items);
        return result;
    }
}
