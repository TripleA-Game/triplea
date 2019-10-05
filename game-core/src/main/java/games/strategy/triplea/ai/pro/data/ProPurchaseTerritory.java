package games.strategy.triplea.ai.pro.data;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerId;
import games.strategy.engine.data.Territory;
import games.strategy.triplea.Properties;
import games.strategy.triplea.ai.pro.util.ProMatches;
import games.strategy.triplea.delegate.Matches;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/** The result of an AI purchase analysis for a single territory. */
@ToString
public class ProPurchaseTerritory {

  @Getter private final Territory territory;
  @Getter @Setter private int unitProduction;
  @Getter private final List<ProPlaceTerritory> canPlaceTerritories;

  public ProPurchaseTerritory(
      final Territory territory,
      final GameData data,
      final PlayerId player,
      final int unitProduction) {
    this(territory, data, player, unitProduction, false);
  }

  /**
   * Create data structure for tracking unit purchase and list of place territories.
   *
   * @param territory - production territory
   * @param data - current game data
   * @param player - AI player who is purchasing
   * @param unitProduction - max unit production for territory
   * @param isBid - true when bid phase, false when normal purchase phase
   */
  public ProPurchaseTerritory(
      final Territory territory,
      final GameData data,
      final PlayerId player,
      final int unitProduction,
      final boolean isBid) {
    this.territory = territory;
    this.unitProduction = unitProduction;
    canPlaceTerritories = new ArrayList<>();
    canPlaceTerritories.add(new ProPlaceTerritory(territory));
    if (!isBid) {
      if (ProMatches.territoryHasFactoryAndIsNotConqueredOwnedLand(player, data).test(territory)) {
        for (final Territory t :
            data.getMap().getNeighbors(territory, Matches.territoryIsWater())) {
          if (Properties.getWW2V2(data)
              || Properties.getUnitPlacementInEnemySeas(data)
              || !t.getUnitCollection().anyMatch(Matches.enemyUnit(player, data))) {
            canPlaceTerritories.add(new ProPlaceTerritory(t));
          }
        }
      }
    }
  }

  public int getRemainingUnitProduction() {
    int remainingUnitProduction = unitProduction;
    for (final ProPlaceTerritory ppt : canPlaceTerritories) {
      remainingUnitProduction -= ppt.getPlaceUnits().size();
    }
    return remainingUnitProduction;
  }
}
