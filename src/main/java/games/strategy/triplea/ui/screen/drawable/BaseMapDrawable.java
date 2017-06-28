package games.strategy.triplea.ui.screen.drawable;

import java.awt.Image;

import games.strategy.triplea.ui.IUIContext;

public class BaseMapDrawable extends MapTileDrawable {
  public BaseMapDrawable(final int x, final int y, final IUIContext uiContext) {
    super(x, y, uiContext);
  }

  @Override
  public MapTileDrawable getUnscaledCopy() {
    final BaseMapDrawable copy = new BaseMapDrawable(m_x, m_y, m_uiContext);
    copy.m_unscaled = true;
    return copy;
  }

  @Override
  protected Image getImage() {
    if (m_noImage) {
      return null;
    }
    Image image;
    if (m_unscaled) {
      image = m_uiContext.getTileImageFactory().getUnscaledUncachedBaseTile(m_x, m_y);
    } else {
      image = m_uiContext.getTileImageFactory().getBaseTile(m_x, m_y);
    }
    if (image == null) {
      m_noImage = true;
    }
    return image;
  }

  @Override
  public int getLevel() {
    return BASE_MAP_LEVEL;
  }
}
