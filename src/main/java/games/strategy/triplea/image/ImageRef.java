package games.strategy.triplea.image;

import java.awt.Image;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import games.strategy.debug.ClientLogger;

/**
 * We keep a soft reference to the image to allow it to be garbage collected.
 * Also, the image may not have finished watching when we are created, but the
 * getImage method ensures that the image will be loaded before returning.
 */
class ImageRef {
  private static final ReferenceQueue<Image> referenceQueue = new ReferenceQueue<>();
  private static final Logger logger = Logger.getLogger(ImageRef.class.getName());
  private static final AtomicInteger imageCount = new AtomicInteger();

  static {
    final Thread t = new Thread(() -> {
      while (true) {
        try {
          referenceQueue.remove();
          logger.finer("Removed soft reference image. Image count:" + imageCount.decrementAndGet());
        } catch (final InterruptedException e) {
          ClientLogger.logQuietly(e);
        }
      }
    }, "Tile Image Factory Soft Reference Reclaimer");
    t.setDaemon(true);
    t.start();
  }

  private final Reference<Image> image;

  public ImageRef(final Image image) {
    this.image = new SoftReference<>(image, referenceQueue);
    logger.finer("Added soft reference image. Image count:" + imageCount.incrementAndGet());
  }

  public Image getImage() {
    return image.get();
  }

  public void clear() {
    image.enqueue();
    image.clear();
  }
}
