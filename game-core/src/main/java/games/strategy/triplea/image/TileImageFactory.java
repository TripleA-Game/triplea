package games.strategy.triplea.image;

import games.strategy.triplea.ResourceLoader;
import games.strategy.triplea.image.BlendComposite.BlendingMode;
import games.strategy.ui.Util;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.lang.ref.SoftReference;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import javax.imageio.ImageIO;
import lombok.extern.java.Log;

/** A factory for creating the base tile images used to render a map. */
@Log
public final class TileImageFactory {
  // one instance in the application
  private static boolean isDirty = false;
  private static final String SHOW_RELIEF_IMAGES_PREFERENCE = "ShowRelief2";
  private static boolean showReliefImages;
  private static final String SHOW_MAP_BLENDS_PREFERENCE = "ShowBlends";
  private static boolean showMapBlends;
  private static final String SHOW_MAP_BLEND_MODE = "BlendMode";
  private static String showMapBlendMode;
  private static final String SHOW_MAP_BLEND_ALPHA = "BlendAlpha";
  private static float showMapBlendAlpha;
  private static final GraphicsConfiguration configuration =
      GraphicsEnvironment.getLocalGraphicsEnvironment()
          .getDefaultScreenDevice()
          .getDefaultConfiguration();
  // maps image name to ImageRef
  private final Map<String, SoftReference<Image>> imageCache =
      Collections.synchronizedMap(new HashMap<>());
  private ResourceLoader resourceLoader;

  static {
    final Preferences prefs = Preferences.userNodeForPackage(TileImageFactory.class);
    showReliefImages = prefs.getBoolean(SHOW_RELIEF_IMAGES_PREFERENCE, true);
    showMapBlends = prefs.getBoolean(SHOW_MAP_BLENDS_PREFERENCE, false);
    showMapBlendMode = prefs.get(SHOW_MAP_BLEND_MODE, "normal");
    showMapBlendAlpha = prefs.getFloat(SHOW_MAP_BLEND_ALPHA, 1.0f);
  }

  public static boolean getShowReliefImages() {
    return showReliefImages;
  }

  public static boolean getShowMapBlends() {
    return showMapBlends;
  }

  private static String getShowMapBlendMode() {
    return showMapBlendMode.toUpperCase();
  }

  private static float getShowMapBlendAlpha() {
    return showMapBlendAlpha;
  }

  public static void setShowReliefImages(final boolean showReliefImages) {
    isDirty |= TileImageFactory.showReliefImages != showReliefImages;
    TileImageFactory.showReliefImages = showReliefImages;
    final Preferences prefs = Preferences.userNodeForPackage(TileImageFactory.class);
    prefs.putBoolean(SHOW_RELIEF_IMAGES_PREFERENCE, TileImageFactory.showReliefImages);
    try {
      prefs.flush();
    } catch (final BackingStoreException ex) {
      log.log(Level.SEVERE, "Failed to save value: " + showReliefImages, ex);
    }
  }

  public static void setShowMapBlends(final boolean showMapBlends) {
    isDirty |= TileImageFactory.showMapBlends != showMapBlends;
    TileImageFactory.showMapBlends = showMapBlends;
    final Preferences prefs = Preferences.userNodeForPackage(TileImageFactory.class);
    prefs.putBoolean(SHOW_MAP_BLENDS_PREFERENCE, TileImageFactory.showMapBlends);
    try {
      prefs.flush();
    } catch (final BackingStoreException ex) {
      log.log(Level.SEVERE, "failed to save value: " + showMapBlends, ex);
    }
  }

  public static void setShowMapBlendMode(final String showMapBlendMode) {
    isDirty |= !Objects.equals(TileImageFactory.showMapBlendMode, showMapBlendMode);
    TileImageFactory.showMapBlendMode = showMapBlendMode;
    final Preferences prefs = Preferences.userNodeForPackage(TileImageFactory.class);
    prefs.put(SHOW_MAP_BLEND_MODE, TileImageFactory.showMapBlendMode);
    try {
      prefs.flush();
    } catch (final BackingStoreException ex) {
      log.log(Level.SEVERE, "failed to save value: " + showMapBlendMode, ex);
    }
  }

  public static void setShowMapBlendAlpha(final float showMapBlendAlpha) {
    isDirty |= TileImageFactory.showMapBlendAlpha != showMapBlendAlpha;
    TileImageFactory.showMapBlendAlpha = showMapBlendAlpha;
    final Preferences prefs = Preferences.userNodeForPackage(TileImageFactory.class);
    prefs.putFloat(SHOW_MAP_BLEND_ALPHA, TileImageFactory.showMapBlendAlpha);
    try {
      prefs.flush();
    } catch (final BackingStoreException ex) {
      log.log(Level.SEVERE, "failed to save value: " + showMapBlendAlpha, ex);
    }
  }

  public void setMapDir(final ResourceLoader loader) {
    resourceLoader = loader;
    imageCache.clear();
  }

  private Image isImageLoaded(final String fileName) {
    if (imageCache.get(fileName) == null) {
      return null;
    }
    return imageCache.get(fileName).get();
  }

  public Image getBaseTile(final int x, final int y) {
    final String fileName = getBaseTileImageName(x, y);
    if (resourceLoader.getResource(fileName) == null) {
      return null;
    }
    return getImage(fileName, false);
  }

  private static String getBaseTileImageName(final int x, final int y) {
    // we are loading with a class loader now, use /
    return "baseTiles" + "/" + x + "_" + y + ".png";
  }

  private Image getImage(final String fileName, final boolean transparent) {
    if (isDirty) {
      isDirty = false;
      imageCache.clear();
    }
    final Image image = isImageLoaded(fileName);
    if (image != null) {
      return image;
    }
    // This is null if there is no image
    final URL url = resourceLoader.getResource(fileName);

    if ((!showMapBlends || !showReliefImages || !transparent) && url == null) {
      return null;
    }
    return loadImage(url, fileName, transparent);
  }

  public Image getReliefTile(final int a, final int b) {
    final String fileName = getReliefTileImageName(a, b);
    return getImage(fileName, true);
  }

  private static String getReliefTileImageName(final int x, final int y) {
    // we are loading with a class loader now, use /
    return "reliefTiles" + "/" + x + "_" + y + ".png";
  }

  /** This method produces a blank white tile for use in blending. */
  private static BufferedImage makeMissingBaseTile(final BufferedImage input) {
    final BufferedImage compatibleImage =
        configuration.createCompatibleImage(
            input.getWidth(null), input.getHeight(null), Transparency.TRANSLUCENT);
    final Graphics2D g2 = compatibleImage.createGraphics();
    g2.fillRect(0, 0, input.getWidth(null), input.getHeight(null));
    g2.drawImage(compatibleImage, 0, 0, null);
    g2.dispose();
    return compatibleImage;
  }

  private Image loadImage(
      final URL imageLocation, final String fileName, final boolean transparent) {
    return (showMapBlends && showReliefImages && transparent)
        ? loadBlendedImage(fileName)
        : loadUnblendedImage(imageLocation, fileName, transparent);
  }

  private Image loadBlendedImage(final String fileName) {
    BufferedImage reliefFile = null;
    BufferedImage baseFile = null;
    // The relief tile
    final String reliefFileName = fileName.replace("baseTiles", "reliefTiles");
    final URL urlRelief = resourceLoader.getResource(reliefFileName);
    // The base tile
    final String baseFileName = fileName.replace("reliefTiles", "baseTiles");
    final URL urlBase = resourceLoader.getResource(baseFileName);
    // blank relief tile
    final String blankReliefFileName = "reliefTiles/blank_relief.png";
    final URL urlBlankRelief = resourceLoader.getResource(blankReliefFileName);

    // Get buffered images
    try {
      if (urlRelief != null) {
        reliefFile = loadCompatibleImage(urlRelief);
      }
      if (urlBase != null) {
        baseFile = loadCompatibleImage(urlBase);
      }
    } catch (final IOException e) {
      log.log(Level.SEVERE, "Failed to load one or more images: " + urlRelief + ", " + urlBase, e);
    }

    // This does the blend
    final float alpha = getShowMapBlendAlpha();
    if (reliefFile == null) {
      try {
        reliefFile = loadCompatibleImage(urlBlankRelief);
      } catch (final IOException e) {
        log.log(Level.SEVERE, "Failed to load image: " + urlBlankRelief, e);
      }
    }
    // This fixes the blank land territories
    if (baseFile == null) {
      baseFile = makeMissingBaseTile(reliefFile);
    }
    /* reversing the to/from files leaves white underlays visible */
    if (reliefFile != null) {
      final BufferedImage blendedImage =
          new BufferedImage(
              reliefFile.getWidth(null), reliefFile.getHeight(null), BufferedImage.TYPE_INT_ARGB);
      final Graphics2D g2 = blendedImage.createGraphics();
      g2.drawImage(reliefFile, 0, 0, null);
      final BlendingMode blendMode = BlendComposite.BlendingMode.valueOf(getShowMapBlendMode());
      final BlendComposite blendComposite = BlendComposite.getInstance(blendMode).derive(alpha);
      g2.setComposite(blendComposite);
      g2.drawImage(baseFile, 0, 0, null);
      final SoftReference<Image> ref = new SoftReference<>(blendedImage);
      imageCache.put(fileName, ref);
      return blendedImage;
    }

    final SoftReference<Image> ref = new SoftReference<>(baseFile);
    imageCache.put(fileName, ref);
    return baseFile;
  }

  private Image loadUnblendedImage(
      final URL imageLocation, final String fileName, final boolean transparent) {
    Image image;
    try {
      final BufferedImage fromFile = ImageIO.read(imageLocation);
      // if we don't copy, drawing the tile to the screen takes significantly longer
      // has something to do with the color model and type of the images
      // some images can be copied quickly to the screen
      // this step is a significant bottle neck in the image drawing process
      // we should try to find a way to avoid it, and load the png directly as the right type
      image = Util.newImage(fromFile.getWidth(null), fromFile.getHeight(null), transparent);
      final Graphics2D g = (Graphics2D) image.getGraphics();
      g.drawImage(fromFile, 0, 0, null);
      g.dispose();
      fromFile.flush();
    } catch (final IOException e) {
      log.log(Level.SEVERE, "Could not load image, url: " + imageLocation.toString(), e);
      image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
    }
    imageCache.put(fileName, new SoftReference<>(image));
    return image;
  }

  private static BufferedImage loadCompatibleImage(final URL resource) throws IOException {
    final BufferedImage image = ImageIO.read(resource);
    return toCompatibleImage(image);
  }

  private static BufferedImage toCompatibleImage(final BufferedImage image) {
    final BufferedImage compatibleImage =
        configuration.createCompatibleImage(
            image.getWidth(), image.getHeight(), Transparency.TRANSLUCENT);
    final Graphics g = compatibleImage.getGraphics();
    g.drawImage(image, 0, 0, null);
    g.dispose();
    return compatibleImage;
  }
}
