package games.strategy.triplea;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;

import games.strategy.common.swing.SwingComponents;
import games.strategy.debug.ClientLogger;
import games.strategy.engine.ClientContext;
import games.strategy.engine.ClientFileSystemHelper;
import games.strategy.util.Match;

/**
 * Utility for managing where images and property files for maps and units should be loaded from.
 * Based on java Classloaders.
 */
public class ResourceLoader {
  private final URLClassLoader m_loader;
  public static String RESOURCE_FOLDER = "assets";

  public static ResourceLoader getGameEngineAssetLoader() {
    return getMapResourceLoader(null);
  }

  public static ResourceLoader getMapResourceLoader(final String mapName) {
    File atFolder = ClientFileSystemHelper.getRootFolder();
    File resourceFolder = new File(atFolder, RESOURCE_FOLDER);

    while (!resourceFolder.exists() && !resourceFolder.isDirectory()) {
      atFolder = atFolder.getParentFile();
      resourceFolder = new File(atFolder, RESOURCE_FOLDER);
    }

    final List<String> dirs = getPaths(mapName);
    dirs.add(resourceFolder.getAbsolutePath());

    return new ResourceLoader(dirs.toArray(new String[dirs.size()]));
  }

  protected static String normalizeMapZipName(String zipName) {
    StringBuilder sb = new StringBuilder();
    Character lastChar = null;

    String spacesReplaced = zipName.replace(' ', '_');

    for (Character c : spacesReplaced.toCharArray()) {
      // break up camel casing
      if (lastChar != null && Character.isLowerCase(lastChar) && Character.isUpperCase(c)) {
        sb.append("_");
      }
      sb.append(Character.toLowerCase(c));
      lastChar = c;
    }
    return sb.toString();
  }

  private static List<String> getPaths(final String mapName) {
    if (mapName == null) {
      return new ArrayList<String>();
    }
    // find the primary directory/file
    final String dirName = File.separator + mapName;
    final String zipName = dirName + ".zip";
    final List<File> candidates = new ArrayList<File>();
    // prioritize user maps folder over root folder
    candidates.add(new File(ClientFileSystemHelper.getUserMapsFolder(), dirName));
    candidates.add(new File(ClientFileSystemHelper.getUserMapsFolder(), zipName));
    candidates.add(new File(ClientFileSystemHelper.getRootFolder() + File.separator + "maps", dirName));
    candidates.add(new File(ClientFileSystemHelper.getRootFolder() + File.separator + "maps", zipName));

    String normalizedZipName = normalizeMapZipName(zipName);
    candidates.add(new File(ClientFileSystemHelper.getUserMapsFolder(), normalizedZipName));


    final Collection<File> existing = Match.getMatches(candidates, new Match<File>() {
      @Override
      public boolean match(final File f) {
        return f.exists();
      }
    });
    if (existing.size() > 1) {
      System.out.println("INFO: Found too many files for: " + mapName + "  found: " + existing);
    }
    // At least one must exist
    if (existing.isEmpty()) {
      SwingComponents.promptUser("Download map?", "Map missing: " + mapName + ", could not join game.\nWould you like to download the map now?"
          + "\nOnce the download completes, you may reconnect to this game.", () -> {
        ClientContext.mapDownloadController().downloadMap(mapName);
      });

      throw new IllegalStateException("Could not find file folder or zip for map: " + mapName + "\r\n"
          + "Please DOWNLOAD THIS MAP if you do not have it." + "\r\n"
          + "If you are making a map or mod, make sure the mapName property within the xml game file exactly matches the map zip or folder name."
          + "\r\n" + "\r\n");
    }
    final File match = existing.iterator().next();

    final List<String> rVal = new ArrayList<String>();
    rVal.add(match.getAbsolutePath());
    // find dependencies
    try (final URLClassLoader url = new URLClassLoader(new URL[] {match.toURI().toURL()})) {
      final URL dependencesURL = url.getResource("dependencies.txt");
      if (dependencesURL != null) {
        final java.util.Properties dependenciesFile = new java.util.Properties();

        try (final InputStream stream = dependencesURL.openStream()) {
          dependenciesFile.load(stream);
          final String dependencies = dependenciesFile.getProperty("dependencies");
          final StringTokenizer tokens = new StringTokenizer(dependencies, ",", false);
          while (tokens.hasMoreTokens()) {
            // add the dependencies recursivly
            rVal.addAll(getPaths(tokens.nextToken()));
          }
        }
      }
    } catch (final Exception e) {
      ClientLogger.logQuietly(e);
      throw new IllegalStateException(e.getMessage());
    }
    return rVal;
  }

  public void close() {
    try {
      m_loader.close();
    } catch (IOException e) {
      ClientLogger.logQuietly(e);
    }
  }

  private ResourceLoader(final String[] paths) {
    final URL[] urls = new URL[paths.length];
    for (int i = 0; i < paths.length; i++) {
      final File f = new File(paths[i]);
      if (!f.exists()) {
        ClientLogger.logQuietly(f + " does not exist");
      }
      if (!f.isDirectory() && !f.getName().endsWith(".zip")) {
        ClientLogger.logQuietly(f + " is not a directory or a zip file");
      }
      try {
        urls[i] = f.toURI().toURL();
      } catch (final MalformedURLException e) {
        e.printStackTrace();
        throw new IllegalStateException(e.getMessage());
      }
    }
    // Note: URLClassLoader does not always respect the ordering of the search URLs
    // To solve this we will get all matching paths and then filter by what matched
    // the assets folder.
    m_loader = new URLClassLoader(urls);
  }

  public boolean hasPath(final String path) {
    final URL rVal = m_loader.getResource(path);
    return rVal != null;
  }

  /**
   * @param path
   *        (The name of a resource is a '/'-separated path name that identifies the resource. Do not use '\' or
   *        File.separator)
   */
  public URL getResource(final String path) {
    URL defaultUrl = null;
    // Return first any match that is not in the assets folder (we expect that to be the users maps folder (loading from map.zip))
    // If we don't have any matches, then return any matches we had from the assets folder
    for (URL element : getMatchingResources(path)) {// Collections.list(m_loader.getResources(path))) {
      if (element.toString().contains(RESOURCE_FOLDER)) {
        defaultUrl = element;
      } else {
        return element;
      }
    }
    return defaultUrl;
  }

  private List<URL> getMatchingResources(final String path ) {
    try {
      return Collections.list(m_loader.getResources(path));
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  public InputStream getResourceAsStream(final String path) {
    URL url = getResource(path);
    if (url == null) {
      return null;
    }
    try {
      return url.openStream();
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }
}
