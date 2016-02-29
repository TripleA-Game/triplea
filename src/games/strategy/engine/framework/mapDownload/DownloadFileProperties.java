package games.strategy.engine.framework.mapDownload;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.Properties;

import games.strategy.engine.ClientContext;
import games.strategy.engine.EngineVersion;
import games.strategy.util.Version;

class DownloadFileProperties {
  protected static final String VERSION_PROPERTY = "map.version";
  private final Properties props = new Properties();

  public static DownloadFileProperties loadForZip(final File zipFile) {
    if (!fromZip(zipFile).exists()) {
      return new DownloadFileProperties();
    }
    final DownloadFileProperties rVal = new DownloadFileProperties();
    try (final FileInputStream fis = new FileInputStream(fromZip(zipFile))) {
      rVal.props.load(fis);
    } catch (final IOException e) {
      e.printStackTrace(System.out);
    }
    return rVal;
  }

  public static void saveForZip(final File zipFile, final DownloadFileProperties props) {
    try ( final FileOutputStream fos = new FileOutputStream(fromZip(zipFile))) {
        props.props.store(fos, null);
    } catch (final IOException e) {
      e.printStackTrace(System.out);
    }
  }

  public DownloadFileProperties() {}

  private static File fromZip(final File zipFile) {
    return new File(zipFile.getParent(), getPropertiesFileName(zipFile.getName()));
  }

  public static String getPropertiesFileName(String mapFileName) {
    return mapFileName + ".properties";
  }

  public Version getVersion() {
    if (!props.containsKey(VERSION_PROPERTY)) {
      return null;
    }
    return new Version(props.getProperty(VERSION_PROPERTY));
  }

  private void setVersion(final Version v) {
    if (v != null) {
      props.put(VERSION_PROPERTY, v.toString());
    }
  }

  public void setFrom(final DownloadFileDescription selected) {
    setVersion(selected.getVersion());
    props.setProperty("map.url", selected.getUrl());
    props.setProperty("download.time", new Date().toString());
    props.setProperty("download.hostedBy", selected.getHostedUrl());
    props.setProperty("engine.version", ClientContext.engineVersion().toString());
  }
}
