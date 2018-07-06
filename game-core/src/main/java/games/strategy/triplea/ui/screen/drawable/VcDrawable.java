package games.strategy.triplea.ui.screen.drawable;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.Territory;
import games.strategy.triplea.ui.mapdata.MapData;

public class VcDrawable implements IDrawable {
  private final Territory location;

  public VcDrawable(final Territory location) {
    this.location = location;
  }

  @Override
  public void draw(final Rectangle bounds, final GameData data, final Graphics2D graphics, final MapData mapData) {

    final Point point = mapData.getVcPlacementPoint(location);
    drawImage(graphics, mapData.getVcImage(), point, bounds);
  }

  @Override
  public int getLevel() {
    return VC_MARKER_LEVEL;
  }
}
