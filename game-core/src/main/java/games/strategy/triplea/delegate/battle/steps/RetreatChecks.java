package games.strategy.triplea.delegate.battle.steps;

import games.strategy.engine.data.GameState;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.Properties;
import games.strategy.triplea.delegate.Matches;
import java.util.Collection;
import java.util.function.Supplier;
import lombok.NonNull;
import lombok.experimental.UtilityClass;

@UtilityClass
public class RetreatChecks {

  public static boolean canAttackerRetreat(
      final @NonNull Collection<Unit> defendingUnits,
      final @NonNull GameState gameData,
      final @NonNull Supplier<Collection<Territory>> getAttackerRetreatTerritories,
      final @NonNull Boolean isAmphibious) {
    if (isAmphibious) {
      return false;
    }
    if (onlyDefenselessTransportsLeft(defendingUnits, gameData)) {
      return false;
    }
    return !getAttackerRetreatTerritories.get().isEmpty();
  }

  public static boolean onlyDefenselessTransportsLeft(
      final @NonNull Collection<Unit> units, final @NonNull GameState gameData) {
    return Properties.getTransportCasualtiesRestricted(gameData.getProperties())
        && !units.isEmpty()
        && units.stream().allMatch(Matches.unitIsTransportButNotCombatTransport());
  }
}
