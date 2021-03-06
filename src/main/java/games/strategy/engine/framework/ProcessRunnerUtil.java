package games.strategy.engine.framework;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;

import games.strategy.debug.ClientLogger;
import games.strategy.engine.ClientFileSystemHelper;
import games.strategy.engine.framework.system.SystemProperties;
import games.strategy.util.Version;

/**
 * To hold various static utility methods for running a java program.
 */
public class ProcessRunnerUtil {

  public static void runClass(final Class<?> mainClass) {
    final List<String> commands = new ArrayList<>();
    populateBasicJavaArgs(commands);
    commands.add(mainClass.getName());
    exec(commands);
  }

  public static void populateBasicJavaArgs(final List<String> commands) {
    populateBasicJavaArgs(commands, System.getProperty("java.class.path"));
  }

  public static void populateBasicJavaArgs(final List<String> commands, final long maxMemory) {
    populateBasicJavaArgs(commands, System.getProperty("java.class.path"), Optional.of(String.valueOf(maxMemory)));
  }

  static void populateBasicJavaArgs(final List<String> commands, final String newClasspath) {
    populateBasicJavaArgs(commands, newClasspath, ManagementFactory.getRuntimeMXBean().getInputArguments().stream()
        .filter(s -> s.toLowerCase().startsWith("-xmx")).map(s -> s.substring(4)).findFirst());
  }

  private static void populateBasicJavaArgs(final List<String> commands, final String classpath,
      final Optional<String> maxMemory) {
    final String javaCommand = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
    commands.add(javaCommand);
    commands.add("-classpath");
    if (classpath != null && classpath.length() > 0) {
      commands.add(classpath);
    } else {
      commands.add(System.getProperty("java.class.path"));
    }
    if (maxMemory.isPresent()) {
      System.out.println("Setting memory for new triplea process to: " + maxMemory.get());
      commands.add("-Xmx" + maxMemory.get());
    }
    if (SystemProperties.isMac()) {
      commands.add("-Dapple.laf.useScreenMenuBar=true");
      commands.add("-Xdock:name=\"TripleA\"");
      final File icons = new File(ClientFileSystemHelper.getRootFolder(), "icons/triplea_icon.png");
      if (icons.exists()) {
        commands.add("-Xdock:icon=" + icons.getAbsolutePath() + "");
      }
    }
    final String version = System.getProperty(GameRunner.TRIPLEA_ENGINE_VERSION_BIN);
    if (version != null && version.length() > 0) {
      final Version testVersion;
      try {
        testVersion = new Version(version);
        commands.add("-D" + GameRunner.TRIPLEA_ENGINE_VERSION_BIN + "=" + testVersion.toString());
      } catch (final Exception e) {
        // nothing
      }
    }
  }

  public static void exec(final List<String> commands) {
    // System.out.println("Commands: " + commands);
    final ProcessBuilder builder = new ProcessBuilder(commands);
    // merge the streams, so we only have to start one reader thread
    builder.redirectErrorStream(true);
    try {
      final Process p = builder.start();
      final InputStream s = p.getInputStream();
      // we need to read the input stream to prevent possible
      // deadlocks
      final Thread t = new Thread(() -> {
        try (Scanner scanner = new Scanner(s)) {
          while (scanner.hasNextLine()) {
            System.out.println(scanner.nextLine());
          }
        }
      }, "Process output gobbler");
      t.setDaemon(true);
      t.start();
    } catch (final IOException e) {
      ClientLogger.logQuietly(e);
    }
  }
}
