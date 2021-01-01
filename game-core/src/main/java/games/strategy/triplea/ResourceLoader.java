package games.strategy.triplea;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import games.strategy.engine.ClientFileSystemHelper;
import games.strategy.engine.framework.map.download.DownloadMapsWindow;
import games.strategy.engine.framework.startup.launcher.MapNotFoundException;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.annotation.Nullable;
import javax.imageio.ImageIO;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.triplea.io.ImageLoader;
import org.triplea.java.UrlStreams;
import org.triplea.java.function.ThrowingSupplier;
import org.triplea.swing.SwingComponents;

/**
 * Utility for managing where images and property files for maps and units should be loaded from.
 * Based on java Classloaders.
 */
@Slf4j
public class ResourceLoader implements Closeable {
  public static final String ASSETS_FOLDER = "assets";
  // All maps must have at least a "baseTiles" folder.
  private static final String REQUIRED_ASSET_EXAMPLE_FOLDER = "baseTiles/";

  private final URLClassLoader loader;
  private final String mapPrefix;
  @Getter private final String mapName;

  public ResourceLoader(final String mapName) {
    Preconditions.checkNotNull(mapName);

    final Optional<String> dir = getPath(mapName);
    if (dir.isEmpty()) {
      SwingComponents.promptUser(
          "Download Map?",
          "Map missing: "
              + mapName
              + ", could not join game.\nWould you like to download the map now?"
              + "\nOnce the download completes, you may reconnect to this game.",
          () -> DownloadMapsWindow.showDownloadMapsWindowAndDownload(mapName));

      throw new MapNotFoundException(mapName, getCandidatePaths(mapName));
    }

    // Add the assets folder from the game installation path. This assets folder supplements
    // any map and resources not found in the map are searched for in this folder.
    final String gameAssetsDirectory =
        findDirectory(ClientFileSystemHelper.getRootFolder(), ASSETS_FOLDER)
            .map(File::getAbsolutePath)
            .orElseThrow(GameAssetsNotFoundException::new);

    final File mapFile = new File(mapName);
    final File assetsFile = new File(gameAssetsDirectory);

    mapPrefix = getMapPrefix(mapFile);

    // Note: URLClassLoader does not always respect the ordering of the search URLs
    // To solve this we will get all matching paths and then filter by what matched
    // the assets folder.
    try {
      loader = new URLClassLoader(new URL[] {mapFile.toURI().toURL(), assetsFile.toURI().toURL()});
    } catch (final MalformedURLException e) {
      throw new IllegalArgumentException(
          "Error creating file system paths with map: "
              + mapName
              + ", engine assets path: "
              + assetsFile.getAbsolutePath()
              + ", and path to map: "
              + mapFile.getAbsolutePath(),
          e);
    }
    this.mapName = mapName;
  }

  /**
   * Will return an empty string unless a special prefix is needed, in which case that prefix is
   * constructed based on where the {@code baseTiles} folder is located within the zip.
   */
  private static String getMapPrefix(final File mapZip) {
    try (ZipFile zip = new ZipFile(mapZip)) {
      final Optional<? extends ZipEntry> baseTilesEntry =
          zip.stream()
              .filter(entry -> entry.getName().endsWith(REQUIRED_ASSET_EXAMPLE_FOLDER))
              .findAny();
      if (baseTilesEntry.isPresent()) {
        final String path = baseTilesEntry.get().getName();
        return path.substring(0, path.length() - REQUIRED_ASSET_EXAMPLE_FOLDER.length());
      }
    } catch (final IOException e) {
      // File is not a zip or can't be opened
    }
    return "";
  }

  /**
   * Loads an image from the 'assets' folder. Images downloaded as part of the build to be included
   * with the game are downloaded to this location. Check the gradle build file download images task
   * for more information on what will be contained in that folder.
   */
  public static Image loadImageAssert(final Path path) {
    return ImageLoader.getImage(Path.of(ASSETS_FOLDER).resolve(path).toFile());
  }

  public static ResourceLoader getGameEngineAssetLoader() {
    return getMapResourceLoader("");
  }

  /** Returns a resource loader that will find assets in a map directory. */
  public static ResourceLoader getMapResourceLoader(final String mapName) {
    return new ResourceLoader(mapName);
  }

  private static class GameAssetsNotFoundException extends RuntimeException {
    private static final long serialVersionUID = -8274500540886412040L;

    GameAssetsNotFoundException() {
      super(
          "Unable to find game assets folder starting from location: "
              + ClientFileSystemHelper.getRootFolder().getAbsolutePath()
              + "\nThere is a problem with the installation, please report this to TripleA "
              + "and the path where TripleA is installed.");
    }
  }

  @VisibleForTesting
  static Optional<File> findDirectory(final File startDir, final String targetDirName) {
    for (File currentDir = startDir; currentDir != null; currentDir = currentDir.getParentFile()) {
      final File targetDir = new File(currentDir, targetDirName);
      if (targetDir.isDirectory()) {
        return Optional.of(targetDir);
      }
    }

    return Optional.empty();
  }

  /**
   * Returns a list of candidate directories from which the specified map may be loaded.
   *
   * <p>The candidate directories are returned in order of preference. That is, a candidate
   * directory earlier in the list should be preferred to a candidate directory later in the list
   * assuming they both exist.
   *
   * @param mapName The map name; must not be {@code null}.
   * @return A list of candidate directories; never {@code null}.
   */
  @VisibleForTesting
  static List<File> getMapDirectoryCandidates(final String mapName, final File userMapsFolder) {
    Preconditions.checkNotNull(mapName);

    final String dirName = File.separator + mapName;
    final String normalizedMapName = File.separator + normalizeMapName(mapName) + "-master";
    return List.of(
        new File(userMapsFolder, dirName + File.separator + "map"),
        new File(userMapsFolder, dirName),
        new File(userMapsFolder, normalizedMapName + File.separator + "map"),
        new File(userMapsFolder, normalizedMapName));
  }

  /**
   * Returns a list of candidate zip files from which the specified map may be loaded.
   *
   * <p>The candidate zip files are returned in order of preference. That is, a candidate file
   * earlier in the list should be preferred to a candidate file later in the list assuming they
   * both exist.
   *
   * @param mapName The map name; must not be {@code null}.
   * @return A list of candidate zip files; never {@code null}.
   */
  public static List<File> getMapZipFileCandidates(
      final String mapName, final File userMapsFolder) {
    Preconditions.checkNotNull(mapName);

    final String normalizedMapName = normalizeMapName(mapName);
    return List.of(
        new File(userMapsFolder, mapName + ".zip"),
        new File(userMapsFolder, normalizedMapName + "-master.zip"),
        new File(userMapsFolder, normalizedMapName + ".zip"));
  }

  @VisibleForTesting
  static String normalizeMapName(final String zipName) {
    final StringBuilder sb = new StringBuilder();
    Character lastChar = null;

    final String spacesReplaced = zipName.replace(' ', '_');

    for (final char c : spacesReplaced.toCharArray()) {
      // break up camel casing
      if (lastChar != null && Character.isLowerCase(lastChar) && Character.isUpperCase(c)) {
        sb.append("_");
      }
      sb.append(Character.toLowerCase(c));
      lastChar = c;
    }
    return sb.toString();
  }

  private static Optional<String> getPath(final String mapName) {
    return getCandidatePaths(mapName).stream()
        .filter(File::exists)
        .findAny()
        .map(File::getAbsolutePath);
  }

  private static List<File> getCandidatePaths(final String mapName) {
    final List<File> candidates = new ArrayList<>();
    candidates.addAll(
        getMapDirectoryCandidates(mapName, ClientFileSystemHelper.getUserMapsFolder()));
    candidates.addAll(getMapZipFileCandidates(mapName, ClientFileSystemHelper.getUserMapsFolder()));
    return candidates;
  }

  @Override
  public void close() {
    try {
      loader.close();
    } catch (final IOException e) {
      log.error("Failed to close resource loader", e);
    }
  }

  public boolean hasPath(final String path) {
    return loader.getResource(path) != null;
  }

  /**
   * Returns the URL of the resource at the specified path or {@code null} if the resource does not
   * exist.
   *
   * @param inputPath (The name of a resource is a '/'-separated path name that identifies the
   *     resource. Do not use '\' or File.separator)
   */
  public @Nullable URL getResource(final String inputPath) {
    final String path = mapPrefix + inputPath;
    return findResource(path).or(() -> findResource(inputPath)).orElse(null);
  }

  /**
   * Returns the URL of the resource at the specified path or {@code null} if the resource does not
   * exist. Tries the given 2 paths in order first in the map resources then engine resources.
   *
   * @param inputPath (The name of a resource is a '/'-separated path name that identifies the
   *     resource. Do not use '\' or File.separator)
   * @param inputPath2 Same as inputPath but this takes second priority when loading
   */
  public @Nullable URL getResource(final String inputPath, final String inputPath2) {
    final String path = mapPrefix + inputPath;
    final String path2 = mapPrefix + inputPath2;
    return findResource(path)
        .or(() -> findResource(path2))
        .or(() -> findResource(inputPath))
        .or(() -> findResource(inputPath2))
        .orElse(null);
  }

  private Optional<URL> findResource(final String searchPath) {
    return getMatchingResources(searchPath).stream().findFirst();
  }

  private List<URL> getMatchingResources(final String path) {
    try {
      return Collections.list(loader.getResources(path));
    } catch (final IOException e) {
      throw new IllegalStateException(e);
    }
  }

  /**
   * Returns an input stream for the specified resource or {@code null} if the resource does not
   * exist. The caller is responsible for closing the returned input stream.
   *
   * @throws IllegalStateException If the specified resource exists but the input stream cannot be
   *     opened.
   */
  public @Nullable InputStream getResourceAsStream(final String path) {
    final URL url = getResource(path);
    if (url == null) {
      return null;
    }

    return UrlStreams.openStream(url)
        .orElseThrow(() -> new IllegalStateException("Failed to open an input stream to: " + path));
  }

  public ThrowingSupplier<InputStream, IOException> optionalResource(final String path) {
    return () ->
        Optional.ofNullable(getResourceAsStream(path))
            .orElseGet(() -> new ByteArrayInputStream(new byte[0]));
  }

  public ThrowingSupplier<InputStream, IOException> requiredResource(final String path) {
    return () ->
        Optional.ofNullable(getResourceAsStream(path))
            .orElseThrow(() -> new FileNotFoundException(path));
  }

  public Optional<Image> loadImage(final String imageName) {
    final URL url = getResource(imageName);
    if (url == null) {
      // this is actually pretty common that we try to read images that are not there. Let the
      // caller
      // decide if this is an error or not.
      return Optional.empty();
    }
    try {
      final BufferedImage bufferedImage = ImageIO.read(url);
      if (bufferedImage == null) {
        log.error("Unsupported Image Format: " + url);
      }
      return Optional.ofNullable(bufferedImage);
    } catch (final IOException e) {
      log.error("Image loading failed: " + imageName, e);
      return Optional.empty();
    }
  }
}
