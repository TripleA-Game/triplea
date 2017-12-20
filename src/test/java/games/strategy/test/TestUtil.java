package games.strategy.test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import com.google.common.io.Files;

import games.strategy.ui.SwingAction;

/**
 * A Utility class for test classes.
 */
public final class TestUtil {
  private TestUtil() {}

  /** Create and returns a simple delete on exit temp file with contents equal to the String parameter. */
  public static File createTempFile(final String contents) {
    final File file;
    try {
      file = File.createTempFile("testFile", ".tmp");
      file.deleteOnExit();
      Files.asCharSink(file, StandardCharsets.UTF_8).write(contents);
      return file;
    } catch (final IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * A server socket has a time to live after it is closed in which it is still
   * bound to its port. For testing, we need to use a new port each time
   * to prevent socket already bound errors
   */
  public static int getUniquePort() {
    // store/get from SystemProperties
    // to get around junit reloading
    final String key = "triplea.test.port";
    String prop = System.getProperty(key);
    if (prop == null) {
      // start off with something fairly random, between 12000 - 14000
      prop = Integer.toString(12000 + (int) (Math.random() % 2000));
    }
    int val = Integer.parseInt(prop);
    val++;
    if (val > 15000) {
      val = 12000;
    }
    System.setProperty(key, "" + val);
    return val;
  }

  /**
   * Blocks until all Swing event thread actions have completed.
   *
   * <p>
   * Task is accomplished by adding a do-nothing event with SwingUtilities
   * to the event thread and then blocking until the do-nothing event is done.
   * </p>
   */
  public static void waitForSwingThreads() {
    // add a no-op action to the end of the swing event queue, and then wait for it
    SwingAction.invokeAndWait(() -> {
    });
  }

  public static Class<?>[] getClassArrayFrom(final Class<?>... classes) {
    return classes;
  }
}
