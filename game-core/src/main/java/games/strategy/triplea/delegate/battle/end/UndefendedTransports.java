package games.strategy.triplea.delegate.battle.end;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.delegate.Matches;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import org.triplea.java.collections.CollectionUtils;

@Builder
public class UndefendedTransports {

  private @NonNull final GamePlayer player;
  private final boolean isAttacker;
  private final boolean canRetreat;
  private @NonNull final Collection<Unit> attackingUnits;
  private @NonNull final GameData gameData;
  private @NonNull final Territory battleSite;

  @Value(staticConstructor = "of")
  public static class Result {
    Collection<Unit> enemyUnits;
    Collection<Unit> transports;
  }

  private final Result emptyResult = Result.of(List.of(), List.of());

  /** Check for unescorted transports and kill them immediately. */
  public Result check() {
    // if we are the attacker, we can retreat instead of dying
    if (isAttacker && (canRetreat || attackingUnits.stream().anyMatch(Matches.unitIsAir()))) {
      return emptyResult;
    }
    // Get all allied transports in the territory
    final List<Unit> alliedTransports = getAlliedTransports();
    // If no transports, just return
    if (alliedTransports.isEmpty()) {
      return emptyResult;
    }
    // Get all ALLIED, sea & air units in the territory (that are NOT submerged)
    final Collection<Unit> alliedUnits = getAlliedUnits();
    // If transports are unescorted, check opposing forces to see if the Trns die automatically
    if (alliedTransports.size() == alliedUnits.size()) {
      // Get all the ENEMY sea and air units (that can attack) in the territory
      final Collection<Unit> enemyUnits = getEnemyUnits();
      if (!enemyUnits.isEmpty()) {
        return Result.of(enemyUnits, alliedTransports);
      }
    }
    return emptyResult;
  }

  private Collection<Unit> getEnemyUnits() {
    final Predicate<Unit> enemyUnitsMatch =
        Matches.unitIsNotLand()
            .and(Matches.unitIsSubmerged().negate())
            .and(Matches.unitCanAttack(player));
    return CollectionUtils.getMatches(battleSite.getUnits(), enemyUnitsMatch);
  }

  private Collection<Unit> getAlliedUnits() {
    final Predicate<Unit> alliedUnitsMatch =
        Matches.isUnitAllied(player, gameData)
            .and(Matches.unitIsNotLand())
            .and(Matches.unitIsSubmerged().negate());
    return CollectionUtils.getMatches(battleSite.getUnits(), alliedUnitsMatch);
  }

  private List<Unit> getAlliedTransports() {
    final Predicate<Unit> matchAllied =
        Matches.unitIsTransport()
            .and(Matches.unitIsNotCombatTransport())
            .and(Matches.isUnitAllied(player, gameData))
            .and(Matches.unitIsSea());
    return CollectionUtils.getMatches(battleSite.getUnits(), matchAllied);
  }
}
