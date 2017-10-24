package games.strategy.engine.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

/**
 * Given a key, returns the value pair from a properties configuration file.
 */
public class PropertyFileReader {

  private final File propertyFile;

  /**
   * Convenience constructor to create a property file reader
   * centered around a given property file specified by file path.
   *
   * @param propertyFile Path to properties file that will be parsed.
   */
  public PropertyFileReader(final String propertyFile) {
    this(new File(propertyFile));
  }

  /**
   * Creates a property file reader centered around a given property file.
   *
   * @param propertyFile Property file that will be parsed.
   */
  public PropertyFileReader(final File propertyFile) {
    this.propertyFile = propertyFile;
    Preconditions.checkState(propertyFile.exists(),
        "Error, could not load file: " + propertyFile.getAbsolutePath() + ", does not exist");
  }

  /**
   * Returns the value corresponding to a given property key, returns empty if the key is not found.
   * Usage example:
   *
   * <pre>
   * <code>
   * String myValue = readProperty("keyValue");
   * </code>
   * </pre>
   */
  public String readProperty(final String propertyKey) {
    if (propertyKey.trim().isEmpty()) {
      throw new IllegalArgumentException("Error, must specify a property key");
    }
    try (FileInputStream inputStream = new FileInputStream(propertyFile)) {
      final Properties props = new Properties();
      props.load(inputStream);

      return Strings.nullToEmpty(props.getProperty(propertyKey)).trim();
    } catch (final FileNotFoundException e) {
      throw new IllegalStateException("Property file not found: " + propertyFile.getAbsolutePath(), e);
    } catch (final IOException e) {
      throw new IllegalStateException("Failed to read property file: " + propertyFile.getAbsolutePath(), e);
    }
  }
}
