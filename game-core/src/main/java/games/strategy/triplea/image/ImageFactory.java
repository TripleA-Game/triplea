package games.strategy.triplea.image;

import java.awt.Image;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;

import games.strategy.triplea.ResourceLoader;

public class ImageFactory {
  private final Map<String, Image> images = new HashMap<>();
  private ResourceLoader resourceLoader;

  public void setResourceLoader(final ResourceLoader loader) {
    resourceLoader = loader;
    images.clear();
  }

  protected Image getImage(final String key1, final String key2, final boolean throwIfNotFound) {
    final Image i1 = getImage(key1, false);
    if (i1 != null) {
      return i1;
    }
    return getImage(key2, throwIfNotFound);
  }

  protected Image getImage(final String key, final boolean throwIfNotFound) {
    if (!images.containsKey(key) || (throwIfNotFound && images.get(key) == null)) {
      final URL url = resourceLoader.getResource(key);
      if (url == null) {
        if (throwIfNotFound) {
          throw new IllegalStateException("Image Not Found:" + key);
        }
        images.put(key, null);
        return null;
      }
      try {
        // We're implementing our own caching system
        ImageIO.setUseCache(false);
        images.put(key, ImageIO.read(url));
      } catch (final IOException e) {
        throw new IllegalStateException(e);
      }
    }
    return images.get(key);
  }
}
