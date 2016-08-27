package games.strategy.engine.framework.map.download;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.yaml.snakeyaml.Yaml;

import games.strategy.util.Version;

/**
 * Utility class to parse an available map list file config file - used to determine which maps are available for
 * download
 */
public final class DownloadFileParser {

  private DownloadFileParser() {}

  public static enum Tags {
    url, mapType, version, mapName, description
  }

  public static enum ValueType {
    MAP, MAP_TOOL, MAP_SKIN, MAP_MOD
  }

  public static List<DownloadFileDescription> parse(final InputStream is) {
    @SuppressWarnings("unchecked")
    final List<Map<String, String>> yamlData = new Yaml().loadAs(is, List.class);

    final List<DownloadFileDescription> rVal = new ArrayList<>();
    for (final Map<String, String> yaml : yamlData) {
      final String url = yaml.get(Tags.url.toString());
      final String description = yaml.get(Tags.description.toString());
      final String mapName = yaml.get(Tags.mapName.toString());

      Version version = null;
      final Object versionObject = yaml.get(Tags.version.toString());
      if (versionObject != null) {
        version = new Version(String.valueOf(versionObject));
      }

      DownloadFileDescription.DownloadType downloadType = DownloadFileDescription.DownloadType.MAP;

      final String mapTypeString = yaml.get(Tags.mapType.toString());
      if (mapTypeString != null) {
        downloadType = DownloadFileDescription.DownloadType.valueOf(mapTypeString);
      }

      final DownloadFileDescription dl = new DownloadFileDescription(url, description, mapName, version, downloadType);
      rVal.add(dl);
    }
    return rVal;
  }
}
