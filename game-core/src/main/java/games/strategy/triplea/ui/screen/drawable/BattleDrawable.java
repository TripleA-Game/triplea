package games.strategy.triplea.ui.screen.drawable;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Rectangle;
import java.util.HashSet;
import java.util.Set;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerId;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.TripleAUnit;
import games.strategy.triplea.ui.mapdata.MapData;

/**
 * Draws a black outline around the associated territory and draws diagonal stripes over the territory interior if a
 * battle is pending within the territory.
 */
public class BattleDrawable extends TerritoryDrawable {
  private final String territoryName;

  public BattleDrawable(final String territoryName) {
    this.territoryName = territoryName;
  }

  @Override
  public void draw(final Rectangle bounds, final GameData data, final Graphics2D graphics, final MapData mapData) {
    final Set<PlayerId> players = new HashSet<>();
    for (final Unit u : data.getMap().getTerritory(territoryName).getUnitCollection()) {
      if (!TripleAUnit.get(u).getSubmerged()) {
        players.add(u.getOwner());
      }
    }
    final Territory territory = data.getMap().getTerritory(territoryName);
    PlayerId attacker = null;
    boolean draw = false;
    for (final PlayerId p : players) {
      if (!territory.isWater()) {
        if (data.getRelationshipTracker().isAtWar(p, territory.getOwner())) {
          attacker = p;
          draw = true;
          break;
        }

        // O(n^2), but n is usually 2, and almost always < 10
        for (final PlayerId p2 : players) {
          if (data.getRelationshipTracker().isAtWar(p, p2)) {
            draw = true;
            break;
          }
        }
      } else {
        // O(n^2), but n is usually 2, and almost always < 10
        for (final PlayerId p2 : players) {
          if (data.getRelationshipTracker().isAtWar(p, p2)) {
            draw = true;
            break;
          }
        }
      }
    }
    if (draw) {
      final Color stripeColor;
      if (attacker == null || territory.isWater()) {
        stripeColor = Color.RED.brighter();
      } else {
        stripeColor = mapData.getPlayerColor(attacker.getName());
      }
      final Paint paint = new GradientPaint(0 - (float) bounds.getX(), 0 - (float) bounds.getY(),
          new Color(stripeColor.getRed(), stripeColor.getGreen(), stripeColor.getBlue(), 120),
          30 - (float) bounds.getX(), 50 - (float) bounds.getY(), new Color(0, 0, 0, 0), true);
      draw(bounds, graphics, mapData, territory, paint);
    }
  }

  @Override
  public DrawLevel getLevel() {
    return DrawLevel.BATTLE_HIGHLIGHT_LEVEL;
  }
}
