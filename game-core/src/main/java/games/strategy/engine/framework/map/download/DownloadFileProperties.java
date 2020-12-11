package games.strategy.engine.framework.map.download;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Optional;
import java.util.Properties;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.triplea.util.Version;

/** Properties file used to know which map versions have been installed. */
@Slf4j
@NoArgsConstructor
class DownloadFileProperties {
  static final String VERSION_PROPERTY = "map.version";
  private final Properties props = new Properties();

  DownloadFileProperties(final Version mapVersion) {
    props.put(VERSION_PROPERTY, mapVersion.toString());
  }

  static DownloadFileProperties loadForZip(final File zipFile) {
    if (!fromZip(zipFile).exists()) {
      return new DownloadFileProperties();
    }
    final DownloadFileProperties downloadFileProperties = new DownloadFileProperties();
    try (InputStream fis = new FileInputStream(fromZip(zipFile))) {
      downloadFileProperties.props.load(fis);
    } catch (final IOException e) {
      log.error("Failed to read property file: " + fromZip(zipFile).getAbsolutePath(), e);
    }
    return downloadFileProperties;
  }

  void saveForZip(final File zipFile) {
    try (OutputStream fos = new FileOutputStream(fromZip(zipFile))) {
      props.store(fos, null);
    } catch (final IOException e) {
      log.error("Failed to write property file to: " + fromZip(zipFile).getAbsolutePath(), e);
    }
  }

  private static File fromZip(final File zipFile) {
    return new File(zipFile.getAbsolutePath() + ".properties");
  }

  Optional<Version> getVersion() {
    return Optional.ofNullable(props.getProperty(VERSION_PROPERTY)).map(Version::new);
  }
}
