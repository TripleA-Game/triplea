package games.strategy.triplea.ai.proAI.logging;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.logging.Level;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import games.strategy.triplea.ai.proAI.ProAI;

/**
 * Class to manage log settings.
 */
public class LogSettings implements Serializable {
  private static final long serialVersionUID = 2696071717784800413L;
  public boolean LimitLogHistory = true;
  public int LimitLogHistoryTo = 5;
  public boolean EnableAILogging = true;
  public Level AILoggingDepth = Level.FINEST;
  private static LogSettings s_lastSettings = null;
  private static String PROGRAM_SETTINGS = "Program Settings";

  public static LogSettings loadSettings() {
    if (s_lastSettings == null) {
      LogSettings result = new LogSettings();
      try {
        final byte[] pool = Preferences.userNodeForPackage(ProAI.class).getByteArray(PROGRAM_SETTINGS, null);
        if (pool != null) {
          result = (LogSettings) new ObjectInputStream(new ByteArrayInputStream(pool)).readObject();
        }
      } catch (final Exception ex) {
      }
      if (result == null) {
        result = new LogSettings();
      }
      s_lastSettings = result;
      return result;
    } else {
      return s_lastSettings;
    }
  }

  public static void saveSettings(final LogSettings settings) {
    s_lastSettings = settings;
    ObjectOutputStream outputStream = null;
    try {
      final ByteArrayOutputStream pool = new ByteArrayOutputStream(10000);
      outputStream = new ObjectOutputStream(pool);
      outputStream.writeObject(settings);
      final Preferences prefs = Preferences.userNodeForPackage(ProAI.class);
      prefs.putByteArray(PROGRAM_SETTINGS, pool.toByteArray());
      try {
        prefs.flush();
      } catch (final BackingStoreException ex) {
        ex.printStackTrace();
      }
    } catch (final Exception ex) {
      ex.printStackTrace();
    } finally {
      try {
        if (outputStream != null) {
          outputStream.close();
        }
      } catch (final Exception ex) {
        ex.printStackTrace();
      }
    }
  }
}
