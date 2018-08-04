package games.strategy.triplea.ui.screen;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import games.strategy.engine.data.GameData;
import games.strategy.thread.LockUtil;
import games.strategy.triplea.ui.mapdata.MapData;
import games.strategy.triplea.ui.screen.drawable.IDrawable;
import games.strategy.triplea.util.Stopwatch;
import games.strategy.ui.Util;

public class Tile {
  public static final LockUtil LOCK_UTIL = LockUtil.INSTANCE;

  private boolean isDirty = true;

  private final Image image;
  private final Rectangle bounds;
  private final double scale;
  private final Lock lock = new ReentrantLock();
  private final SortedMap<Integer, List<IDrawable>> contents = new TreeMap<>();

  Tile(final Rectangle bounds, final double scale) {
    this.bounds = bounds;
    this.scale = scale;
    image = Util.createImage((int) (bounds.getWidth() * scale), (int) (bounds.getHeight() * scale), true);
  }

  public boolean isDirty() {
    acquireLock();
    try {
      return isDirty;
    } finally {
      releaseLock();
    }
  }

  public void acquireLock() {
    LOCK_UTIL.acquireLock(lock);
  }

  public void releaseLock() {
    LOCK_UTIL.releaseLock(lock);
  }

  public Image getImage(final GameData data, final MapData mapData) {
    acquireLock();
    try {
      if (isDirty) {
        final Graphics2D g = (Graphics2D) image.getGraphics();
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        draw(g, data, mapData);
        g.dispose();
      }
      return image;
    } finally {
      releaseLock();
    }
  }

  /**
   * This image may not reflect our current drawables.
   * Use getImage() to get a correct image
   *
   * @return the image we currently have.
   */
  public Image getRawImage() {
    return image;
  }

  private void draw(final Graphics2D g, final GameData data, final MapData mapData) {
    final AffineTransform unscaled = g.getTransform();
    final AffineTransform scaled;
    if (scale != 1) {
      scaled = new AffineTransform();
      scaled.scale(scale, scale);
      g.setTransform(scaled);
    } else {
      scaled = unscaled;
    }
    final Stopwatch stopWatch = new Stopwatch(Logger.getLogger(Tile.class.getName()), Level.FINEST,
        "Drawing Tile at" + bounds);
    // clear
    g.setColor(Color.BLACK);
    g.fill(new Rectangle(0, 0, TileManager.TILE_SIZE, TileManager.TILE_SIZE));
    for (final List<IDrawable> list : contents.values()) {
      for (final IDrawable drawable : list) {
        drawable.draw(bounds, data, g, mapData, unscaled, scaled);
      }
    }
    isDirty = false;
    stopWatch.done();
  }

  void addDrawables(final Collection<IDrawable> drawables) {
    drawables.forEach(this::addDrawable);
  }

  void addDrawable(final IDrawable d) {
    acquireLock();
    try {
      contents.computeIfAbsent(d.getLevel(), l -> new ArrayList<>()).add(d);
      isDirty = true;
    } finally {
      releaseLock();
    }
  }

  void removeDrawables(final Collection<IDrawable> c) {
    acquireLock();
    try {
      contents.values().forEach(l -> l.removeAll(c));
      isDirty = true;
    } finally {
      releaseLock();
    }
  }

  void clear() {
    acquireLock();
    try {
      contents.clear();
      isDirty = true;
    } finally {
      releaseLock();
    }
  }

  List<IDrawable> getDrawables() {
    acquireLock();
    try {
      return contents.values().stream()
          .flatMap(Collection::stream)
          .collect(Collectors.toList());
    } finally {
      releaseLock();
    }
  }

  public Rectangle getBounds() {
    return bounds;
  }
}
