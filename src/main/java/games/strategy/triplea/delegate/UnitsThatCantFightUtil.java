package games.strategy.triplea.delegate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Predicate;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.util.Match;
import games.strategy.util.PredicateBuilder;

/**
 * Utility for detecting and removing units that can't land at the end of a phase.
 */
public class UnitsThatCantFightUtil {
  private final GameData m_data;

  public UnitsThatCantFightUtil(final GameData data) {
    m_data = data;
  }

  // TODO Used to notify of kamikazi attacks
  Collection<Territory> getTerritoriesWhereUnitsCantFight(final PlayerID player) {
    final Match<Unit> enemyAttackUnits = Match.allOf(Matches.enemyUnit(player, m_data), Matches.unitCanAttack(player));
    final Collection<Territory> cantFight = new ArrayList<>();
    for (final Territory current : m_data.getMap()) {
      // get all owned non-combat units
      final Predicate<Unit> ownedUnitsMatch = PredicateBuilder
          .of(Matches.unitIsInfrastructure().invert())
          .andIf(current.isWater(), Matches.unitIsLand().invert())
          .and(Matches.unitIsOwnedBy(player))
          .build();
      // All owned units
      final int countAllOwnedUnits = current.getUnits().countMatches(ownedUnitsMatch);
      // only noncombat units
      final Collection<Unit> nonCombatUnits =
          current.getUnits().getMatches(ownedUnitsMatch.and(Matches.unitCanAttack(player).invert()));
      if (nonCombatUnits.isEmpty() || nonCombatUnits.size() != countAllOwnedUnits) {
        continue;
      }
      if (current.getUnits().anyMatch(enemyAttackUnits)) {
        cantFight.add(current);
      }
    }
    return cantFight;
  }
}
