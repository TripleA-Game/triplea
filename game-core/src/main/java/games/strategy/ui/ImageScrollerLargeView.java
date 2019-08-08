package games.strategy.ui;

import com.google.common.primitives.Doubles;
import games.strategy.triplea.settings.ClientSetting;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelListener;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/**
 * A large image that can be scrolled according to a ImageScrollModel. Generally used in conjunction
 * with a ImageScrollerSmallView. We do not take care of drawing ourselves. All we do is keep track
 * of our location and size. Subclasses must take care of rendering
 */
public class ImageScrollerLargeView extends JComponent {

  private static final long serialVersionUID = -7212817233833868483L;

  // bit flags for determining which way we are scrolling
  private static final int NONE = 0b0000;
  private static final int LEFT = 0b0001;
  private static final int RIGHT = 0b0010;
  private static final int TOP = 0b0100;
  private static final int BOTTOM = 0b1000;

  protected final ImageScrollModel model;
  protected double scale = 1;
  private int dragScrollingLastX;
  private int dragScrollingLastY;
  private boolean wasLastActionDragging = false;

  private final ActionListener timerAction =
      new ActionListener() {
        @Override
        public void actionPerformed(final ActionEvent e) {
          if (JOptionPane.getFrameForComponent(ImageScrollerLargeView.this).getFocusOwner()
              == null) {
            insideCount = 0;
            return;
          }
          if (inside && edge != NONE) {
            insideCount++;
            if (insideCount > 6) {
              // Scroll the map when the mouse has hovered inside the scroll zone for long enough
              SwingUtilities.invokeLater(new Scroller());
            }
          }
        }
      };
  // scrolling
  private final Timer timer = new Timer(50, timerAction);
  private boolean inside = false;
  private int insideCount = 0;
  private int edge = NONE;
  private final List<ScrollListener> scrollListeners = new ArrayList<>();

  public ImageScrollerLargeView(final Dimension dimension, final ImageScrollModel model) {
    this.model = model;
    this.model.setMaxBounds((int) dimension.getWidth(), (int) dimension.getHeight());
    setPreferredSize(getImageDimensions());
    setMaximumSize(getImageDimensions());
    final MouseWheelListener mouseWheelListener =
        e -> {
          if (e.isControlDown()) {
            final float zoomFactor = ClientSetting.mapZoomFactor.getValueOrThrow() / 100f;
            final int oldWidth = model.getBoxWidth();
            final int oldHeight = model.getBoxHeight();
            setScale(scale - zoomFactor * e.getPreciseWheelRotation());
            model.set(
                model.getX()
                    + (int)
                        ((getMousePosition().getX() / getWidth())
                            * (oldWidth - model.getBoxWidth())),
                model.getY()
                    + (int)
                        ((getMousePosition().getY() / getHeight())
                            * (oldHeight - model.getBoxHeight())));
          } else {
            if (edge == NONE) {
              insideCount = 0;
            }
            // compute the amount to move
            int dx = 0;
            int dy = 0;
            // In java 11 SHIFT_DOWN_MASK seems to be true for sideways scrolling
            // this doesn't seem to be documented anywhere, but we'll take it for now
            if ((e.getModifiersEx() & InputEvent.SHIFT_DOWN_MASK) == InputEvent.SHIFT_DOWN_MASK) {
              dx =
                  (int)
                      (e.getPreciseWheelRotation()
                          * ClientSetting.wheelScrollAmount.getValueOrThrow());
            } else {
              dy =
                  (int)
                      (e.getPreciseWheelRotation()
                          * ClientSetting.wheelScrollAmount.getValueOrThrow());
            }
            // Update the model, which will handle its own clamping or wrapping depending on the
            // map.
            this.model.set(this.model.getX() + dx, this.model.getY() + dy);
          }
        };
    addMouseWheelListener(mouseWheelListener);
    final MouseAdapter mouseListener =
        new MouseAdapter() {
          @Override
          public void mouseEntered(final MouseEvent e) {
            timer.start();
          }

          @Override
          public void mouseExited(final MouseEvent e) {
            inside = false;
            timer.stop();
          }

          @Override
          public void mouseClicked(final MouseEvent e) {
            requestFocusInWindow();
          }

          @Override
          public void mouseReleased(final MouseEvent e) {
            requestFocusInWindow();
          }
        };
    addMouseListener(mouseListener);
    final MouseAdapter mouseListenerDragScrolling =
        new MouseAdapter() {
          @Override
          public void mousePressed(final MouseEvent e) {
            // try to center around the click
            dragScrollingLastX = e.getX();
            dragScrollingLastY = e.getY();
          }
        };
    addMouseListener(mouseListenerDragScrolling);
    final MouseMotionListener mouseMotionListener =
        new MouseMotionAdapter() {
          @Override
          public void mouseMoved(final MouseEvent e) {
            inside = true;
            final int x = e.getX();
            final int y = e.getY();
            final int height = getHeight();
            final int width = getWidth();
            edge = getNewEdge(x, y, width, height);
            if (edge == NONE) {
              insideCount = 0;
            }
          }
        };
    addMouseMotionListener(mouseMotionListener);
    /*
     * this is used to detect drag scrolling
     */
    final MouseMotionListener mouseDragListener =
        new MouseMotionAdapter() {
          @Override
          public void mouseDragged(final MouseEvent e) {
            requestFocusInWindow();
            // the right button must be the one down
            if ((e.getModifiersEx() & (InputEvent.BUTTON3_DOWN_MASK | InputEvent.BUTTON2_DOWN_MASK))
                != 0) {
              wasLastActionDragging = true;
              inside = false;
              // read in location
              final int x = e.getX();
              final int y = e.getY();
              if (edge == NONE) {
                insideCount = 0;
              }
              // compute the amount to move
              final int dx = (dragScrollingLastX - x);
              final int dy = (dragScrollingLastY - y);
              // move left and right and test for wrap
              final int newX = (ImageScrollerLargeView.this.model.getX() + dx);
              // move up and down and test for edges
              final int newY = ImageScrollerLargeView.this.model.getY() + dy;
              // update the map
              ImageScrollerLargeView.this.model.set(newX, newY);
              // store the location of the mouse for the next move
              dragScrollingLastX = e.getX();
              dragScrollingLastY = e.getY();
            }
          }
        };
    addMouseMotionListener(mouseDragListener);
    final ComponentListener componentListener =
        new ComponentAdapter() {
          @Override
          public void componentResized(final ComponentEvent e) {
            refreshBoxSize();
          }
        };
    addComponentListener(componentListener);
    timer.start();
    this.model.addObserver(
        (o, arg) -> {
          repaint();
          notifyScollListeners();
        });
  }

  public boolean wasLastActionDraggingAndReset() {
    if (wasLastActionDragging) {
      wasLastActionDragging = false;
      return true;
    }
    return false;
  }

  /** For subclasses needing to set the location of the image. */
  protected void setTopLeft(final int x, final int y) {
    model.set(x, y);
  }

  public int getImageWidth() {
    return model.getMaxWidth();
  }

  public int getImageHeight() {
    return model.getMaxHeight();
  }

  public void addScrollListener(final ScrollListener s) {
    scrollListeners.add(s);
  }

  private void notifyScollListeners() {
    for (final ScrollListener element : new ArrayList<>(scrollListeners)) {
      element.scrolled(model.getX(), model.getY());
    }
  }

  private void scroll() {
    final int scrollSpeed = ClientSetting.mapEdgeScrollSpeed.getValueOrThrow();
    final int dx = (edge & LEFT) != 0 ? -scrollSpeed : ((edge & RIGHT) != 0 ? scrollSpeed : 0);
    final int dy = (edge & TOP) != 0 ? -scrollSpeed : ((edge & BOTTOM) != 0 ? scrollSpeed : 0);

    final int newX = (int) (model.getX() + (dx / scale));
    final int newY = (int) (model.getY() + (dy / scale));
    model.set(newX, newY);
  }

  public Dimension getImageDimensions() {
    return new Dimension(model.getMaxWidth(), model.getMaxHeight());
  }

  private static int getNewEdge(final int x, final int y, final int width, final int height) {
    final int mapEdgeScrollZoneSize = ClientSetting.mapEdgeScrollZoneSize.getValueOrThrow();
    int newEdge = NONE;
    if (x < mapEdgeScrollZoneSize) {
      newEdge |= LEFT;
    } else if (width - x < mapEdgeScrollZoneSize) {
      newEdge |= RIGHT;
    }
    if (y < mapEdgeScrollZoneSize) {
      newEdge |= TOP;
    } else if (height - y < mapEdgeScrollZoneSize) {
      newEdge |= BOTTOM;
    }
    return newEdge;
  }

  private void refreshBoxSize() {
    model.setBoxDimensions((int) (getWidth() / scale), (int) (getHeight() / scale));
  }

  /**
   * Sets the view scale.
   *
   * @param value The new scale value. Constrained to the bounds of {@link #getMinScale()} and no
   *     greater than 1. If out of bounds the nearest boundary value is used.
   */
  public void setScale(final double value) {
    scale = constrainScale(value);
    refreshBoxSize();
  }

  public double getMinScale() {
    final double minScale =
        scale
            * Math.max(
                (double) model.getBoxWidth() / model.getMaxWidth(),
                (double) model.getBoxHeight() / model.getMaxHeight());
    return Math.min(minScale, 1);
  }

  private double constrainScale(final double value) {
    return Doubles.constrainToRange(value, getMinScale(), 1.0);
  }

  /** Update will not be seen until update is called. Resets the offscreen image to the original. */
  public int getXOffset() {
    return model.getX();
  }

  public int getYOffset() {
    return model.getY();
  }

  private class Scroller implements Runnable {
    @Override
    public void run() {
      scroll();
    }
  }

  protected double getScaledWidth() {
    return getWidth() / scale;
  }

  protected double getScaledHeight() {
    return getHeight() / scale;
  }

  public void deactivate() {
    timer.stop();
    timer.removeActionListener(timerAction);
  }
}
