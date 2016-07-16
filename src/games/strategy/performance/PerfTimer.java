package games.strategy.performance;

import java.util.prefs.Preferences;

/**
 * Provides a high level API to the game engine for performance measurements.
 * This class handles the library details and sends output to 'PerformanceConsole.java'
 */
public class PerfTimer {

  protected static final PerfTimer DISABLED_TIMER = new PerfTimer("disabled");

  public final String title;
  private final long startMillis;

  protected PerfTimer(String title) {
    this.title = title;
    this.startMillis = System.nanoTime();
  }

  private long stopTimer() {
    long end = System.nanoTime();
    return end - startMillis;
  }

  /** Alias for the close method, stops the timer */
  public void stop() {
    processResult(stopTimer(), this);
  }

  private static final String LOG_PERFORMANCE_KEY = "logPerformance";
  private static boolean enabled;

  static {
    enabled = isEnabled();
    if (enabled) {
      PerformanceConsole.getInstance().setVisible(true);
    }
  }

  public static void setEnabled(final boolean isEnabled) {
    if (enabled != isEnabled) {
      enabled = isEnabled;
      PerformanceConsole.getInstance().setVisible(enabled);
      storeEnabledPreference();
    }
  }

  private static void storeEnabledPreference() {
    final Preferences prefs = Preferences.userNodeForPackage(EnablePerformanceLoggingCheckBox.class);
    prefs.put(LOG_PERFORMANCE_KEY, Boolean.valueOf(enabled).toString());
  }

  public static boolean isEnabled() {
    final Preferences prefs = Preferences.userNodeForPackage(EnablePerformanceLoggingCheckBox.class);
    return prefs.getBoolean(LOG_PERFORMANCE_KEY, false);
  }

  public static PerfTimer startTimer(String title) {
    if (!enabled) {
      return DISABLED_TIMER;
    } else {
      return new PerfTimer(title);
    }
  }

  protected static void processResult(long stopNanos, PerfTimer perfTimer) {
    long stopMicros = stopNanos / 1000;

    long milliFraction = (stopMicros % 1000) / 100;
    long millis = (stopMicros / 1000);
    PerformanceConsole.getInstance().append(millis + "." + milliFraction + " ms - " + perfTimer.title + "\n");
  }
}
