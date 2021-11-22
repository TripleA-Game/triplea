package games.strategy.triplea;

import com.google.common.annotations.VisibleForTesting;
import games.strategy.engine.ClientFileSystemHelper;
import games.strategy.triplea.ui.OrderedProperties;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.Closeable;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.imageio.ImageIO;
import lombok.extern.slf4j.Slf4j;
import org.triplea.io.ImageLoader;
import org.triplea.io.PathUtils;
import org.triplea.java.UrlStreams;

/**
 * Utility for managing where images and property files for maps and units should be loaded from.
 * Based on java Classloaders.
 */
@Slf4j
public class ResourceLoader implements Closeable {
  public static final String ASSETS_FOLDER = "assets";

  private final URLClassLoader loader;

  public ResourceLoader(@Nonnull final Path assetFolder) {
    this(List.of(assetFolder));
  }

  public ResourceLoader(List<Path> assetPaths) {
    List<URL> searchUrls = assetPaths.stream().map(PathUtils::toUrl).collect(Collectors.toList());

    Path gameEngineAssets =
        findDirectory(ClientFileSystemHelper.getRootFolder(), ASSETS_FOLDER)
            .orElseThrow(GameAssetsNotFoundException::new);

    searchUrls.add(PathUtils.toUrl(gameEngineAssets));

    // Note: URLClassLoader does not always respect the ordering of the search URLs
    // To solve this we will get all matching paths and then filter by what matched
    // the assets folder.

    loader = new URLClassLoader(searchUrls.toArray(URL[]::new));
  }

  @VisibleForTesting
  ResourceLoader(final URLClassLoader loader) {
    this.loader = loader;
  }

  /**
   * Loads an image from the 'assets' folder. Images downloaded as part of the build to be included
   * with the game are downloaded to this location. Check the gradle build file download images task
   * for more information on what will be contained in that folder.
   */
  public static Image loadImageAsset(final Path path) {
    return ImageLoader.getImage(Path.of(ASSETS_FOLDER).resolve(path));
  }

  private static class GameAssetsNotFoundException extends RuntimeException {
    private static final long serialVersionUID = -8274500540886412040L;

    GameAssetsNotFoundException() {
      super(
          "Unable to find game assets folder starting from location: "
              + ClientFileSystemHelper.getRootFolder().toAbsolutePath()
              + "\nThere is a problem with the installation, please report this to TripleA "
              + "and the path where TripleA is installed.");
    }
  }

  /**
   * Searches from a starting directory for a given directory. If not found, recursively goes up to
   * parent directories searching for the given directory.
   *
   * @param startDir The start of the search path.
   * @param targetDirName The name of the directory to find (must be a directory, not a file)
   * @return Path of the directory as found, otherwise empty.
   */
  @VisibleForTesting
  static Optional<Path> findDirectory(final Path startDir, final String targetDirName) {
    for (Path currentDir = startDir; currentDir != null; currentDir = currentDir.getParent()) {
      final Path targetDir = currentDir.resolve(targetDirName);
      if (Files.isDirectory(targetDir)) {
        return Optional.of(targetDir);
      }
    }

    return Optional.empty();
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
    return findResource(inputPath).orElse(null);
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
    return findResource(inputPath).or(() -> findResource(inputPath2)).orElse(null);
  }

  private Optional<URL> findResource(final String searchPath) {
    return loader.resources(searchPath).findFirst();
  }

  public Optional<Path> optionalResource(final String path) {
    return findResource(path)
        .map(
            url -> {
              try {
                return url.toURI();
              } catch (final URISyntaxException e) {
                throw new IllegalStateException(e);
              }
            })
        .map(Path::of);
  }

  public Path requiredResource(final String path) throws IOException {
    return optionalResource(path).orElseThrow(() -> new FileNotFoundException(path));
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

  public Properties loadAsResource(final String fileName) {
    final Properties properties = new OrderedProperties();
    final URL url = getResource(fileName);
    if (url != null) {
      final Optional<InputStream> optionalInputStream = UrlStreams.openStream(url);
      if (optionalInputStream.isPresent()) {
        try (InputStream inputStream = optionalInputStream.get()) {
          properties.load(inputStream);
        } catch (final IOException e) {
          log.error("Error reading " + fileName, e);
        }
      }
    }
    return properties;
  }
}
