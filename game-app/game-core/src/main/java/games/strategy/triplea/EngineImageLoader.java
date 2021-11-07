package games.strategy.triplea;

import games.strategy.engine.ClientFileSystemHelper;
import java.awt.Image;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import lombok.experimental.UtilityClass;

/**
 * Use this to load images from the game engine 'assets' folder. Do *not* use this to read images
 * from maps.
 */
@UtilityClass
public class EngineImageLoader {
  public static final String ASSETS_FOLDER = "assets";

  public Image loadFrameIcon() {
    return loadImage("icons", "ta_icon.png");
  }

  /**
   * Reads an image from the assets folder.
   *
   * @param path Path from assets folder to image, eg: loadImage("folder-in-assets", "image.png");
   */
  public Image loadImage(final String... path) {
    Path imageFilePath = createPathToImage(path);

    if (!Files.exists(imageFilePath)) {
      try {
        imageFilePath =
            ClientFileSystemHelper.getCodeSourceFolder().resolve(createPathToImage(path));
      } catch (final IOException e) {
        throw new IllegalStateException(
            "Error loading image at: ClientFileSystemHelper.getCodeSourceFolder(), "
                + e.getMessage(),
            e);
      }
    }

    if (!Files.exists(imageFilePath)) {
      throw new IllegalStateException(
          "Error loading image, image does not exist at: " + imageFilePath.toAbsolutePath());
    }

    try {
      return ImageIO.read(imageFilePath.toFile());
    } catch (final IOException e) {
      throw new IllegalStateException(
          "Error loading image at: " + imageFilePath.toAbsolutePath() + ", " + e.getMessage(), e);
    }
  }

  protected Path createPathToImage(final String... path) {
    Path imageFilePath = Path.of(ASSETS_FOLDER);
    for (final String pathPart : path) {
      imageFilePath = imageFilePath.resolve(pathPart);
    }
    return imageFilePath;
  }
}
