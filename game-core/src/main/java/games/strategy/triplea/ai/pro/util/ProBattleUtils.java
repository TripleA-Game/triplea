package games.strategy.triplea.ai.pro.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerId;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.Properties;
import games.strategy.triplea.ai.pro.ProData;
import games.strategy.triplea.ai.pro.data.ProPlaceTerritory;
import games.strategy.triplea.ai.pro.data.ProPurchaseTerritory;
import games.strategy.triplea.ai.pro.data.ProTerritory;
import games.strategy.triplea.ai.pro.logging.ProLogger;
import games.strategy.triplea.delegate.BattleCalculator;
import games.strategy.triplea.delegate.DiceRoll;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.MoveValidator;
import games.strategy.triplea.delegate.TerritoryEffectHelper;
import games.strategy.triplea.delegate.UnitBattleComparator;
import games.strategy.util.CollectionUtils;

/**
 * Pro AI battle utilities.
 */
public class ProBattleUtils {

  public static final int SHORT_RANGE = 2;
  public static final int MEDIUM_RANGE = 3;

  /**
   * Return {@code true} if the specified battle would result in an overwhelming win for the attacker. An overwhelming
   * win is defined as the ability to defeat the enemy without any losses or within a single round of combat.
   */
  public static boolean checkForOverwhelmingWin(final Territory t,
      final List<Unit> attackingUnits, final List<Unit> defendingUnits) {
    final GameData data = ProData.getData();

    if (defendingUnits.isEmpty() && !attackingUnits.isEmpty()) {
      return true;
    }

    // Check that defender has at least 1 power
    final double power = estimatePower(t, defendingUnits, attackingUnits, false);
    if (power == 0 && !attackingUnits.isEmpty()) {
      return true;
    }

    // Determine if enough attack power to win in 1 round
    final List<Unit> sortedUnitsList = new ArrayList<>(attackingUnits);
    sortedUnitsList.sort(
        new UnitBattleComparator(false, ProData.unitValueMap, TerritoryEffectHelper.getEffects(t), data, false, false));
    Collections.reverse(sortedUnitsList);
    final int attackPower = DiceRoll.getTotalPower(DiceRoll.getUnitPowerAndRollsForNormalBattles(sortedUnitsList,
        defendingUnits, false, data, t, TerritoryEffectHelper.getEffects(t), false, null), data);
    final List<Unit> defendersWithHitPoints =
        CollectionUtils.getMatches(defendingUnits, Matches.unitIsInfrastructure().negate());
    final int totalDefenderHitPoints = BattleCalculator.getTotalHitpointsLeft(defendersWithHitPoints);
    return ((attackPower / data.getDiceSides()) >= totalDefenderHitPoints);
  }

  /**
   * Returns an estimate of the strength difference between the specified attacking and defending units.
   *
   * @return 0 indicates absolute defender strength; 100+ indicates absolute attacker
   *         strength; 50 indicates equal attacker and defender strength.
   */
  public static double estimateStrengthDifference(final Territory t, final List<Unit> attackingUnits,
      final List<Unit> defendingUnits) {

    if (attackingUnits.stream().allMatch(Matches.unitIsInfrastructure())) {
      return 0;
    }
    if (defendingUnits.stream().allMatch(Matches.unitIsInfrastructure())) {
      return 99999;
    }
    final double attackerStrength = estimateStrength(t, attackingUnits, defendingUnits, true);
    final double defenderStrength = estimateStrength(t, defendingUnits, attackingUnits, false);
    return ((attackerStrength - defenderStrength) / Math.pow(defenderStrength, 0.85) * 50 + 50);
  }

  /**
   * Estimates the strength of {@code myUnits} relative to {@code enemyUnits}.
   *
   * @return The larger the result, the stronger {@code myUnits} are relative to {@code enemyUnits}.
   */
  public static double estimateStrength(final Territory t, final List<Unit> myUnits, final List<Unit> enemyUnits,
      final boolean attacking) {
    final GameData data = ProData.getData();

    List<Unit> unitsThatCanFight =
        CollectionUtils.getMatches(myUnits, Matches.unitCanBeInBattle(attacking, !t.isWater(), 1, true));
    if (Properties.getTransportCasualtiesRestricted(data)) {
      unitsThatCanFight =
          CollectionUtils.getMatches(unitsThatCanFight, Matches.unitIsTransportButNotCombatTransport().negate());
    }
    final int myHitPoints = BattleCalculator.getTotalHitpointsLeft(unitsThatCanFight);
    final double myPower = estimatePower(t, myUnits, enemyUnits, attacking);
    return (2.0 * myHitPoints) + myPower;
  }

  private static double estimatePower(final Territory t, final List<Unit> myUnits, final List<Unit> enemyUnits,
      final boolean attacking) {
    final GameData data = ProData.getData();

    final List<Unit> unitsThatCanFight =
        CollectionUtils.getMatches(myUnits, Matches.unitCanBeInBattle(attacking, !t.isWater(), 1, true));
    final List<Unit> sortedUnitsList = new ArrayList<>(unitsThatCanFight);
    sortedUnitsList.sort(new UnitBattleComparator(!attacking, ProData.unitValueMap,
        TerritoryEffectHelper.getEffects(t), data, false, false));
    Collections.reverse(sortedUnitsList);
    final int myPower = DiceRoll.getTotalPower(DiceRoll.getUnitPowerAndRollsForNormalBattles(sortedUnitsList,
        enemyUnits, !attacking, data, t, TerritoryEffectHelper.getEffects(t), false, null), data);
    return (myPower * 6.0 / data.getDiceSides());
  }

  public static boolean territoryHasLocalLandSuperiority(final Territory t, final int distance, final PlayerId player) {
    return territoryHasLocalLandSuperiority(t, distance, player, new HashMap<>());
  }

  /**
   * Returns {@code true} if {@code player} has land superiority within {@code distance} neighbors of {@code t}.
   */
  public static boolean territoryHasLocalLandSuperiority(final Territory t, final int distance, final PlayerId player,
      final Map<Territory, ProPurchaseTerritory> purchaseTerritories) {

    final GameData data = ProData.getData();
    if (t == null) {
      return true;
    }

    for (int i = 2; i <= distance; i++) {

      // Find enemy strength
      final Set<Territory> nearbyTerritoriesForEnemy =
          data.getMap().getNeighbors(t, i, ProMatches.territoryCanMoveLandUnits(player, data, false));
      nearbyTerritoriesForEnemy.add(t);
      final List<Unit> enemyUnits = new ArrayList<>();
      for (final Territory nearbyTerritory : nearbyTerritoriesForEnemy) {
        enemyUnits.addAll(nearbyTerritory.getUnits().getMatches(ProMatches.unitIsEnemyNotNeutral(player, data)));
      }

      // Find allied strength
      final Set<Territory> nearbyTerritoriesForAllied =
          data.getMap().getNeighbors(t, i - 1, ProMatches.territoryCanMoveLandUnits(player, data, false));
      nearbyTerritoriesForAllied.add(t);
      final List<Unit> alliedUnits = new ArrayList<>();
      for (final Territory nearbyTerritory : nearbyTerritoriesForAllied) {
        alliedUnits.addAll(nearbyTerritory.getUnits().getMatches(Matches.isUnitAllied(player, data)));
      }
      for (final ProPurchaseTerritory purchaseTerritory : purchaseTerritories.values()) {
        for (final ProPlaceTerritory ppt : purchaseTerritory.getCanPlaceTerritories()) {
          if (nearbyTerritoriesForAllied.contains(ppt.getTerritory())) {
            alliedUnits.addAll(ppt.getPlaceUnits());
          }
        }
      }

      // Determine strength difference
      final double strengthDifference = estimateStrengthDifference(t, enemyUnits, alliedUnits);
      ProLogger.trace(t + ", current enemy land strengthDifference=" + strengthDifference + ", distance=" + i
          + ", enemySize=" + enemyUnits.size() + ", alliedSize=" + alliedUnits.size());
      if (strengthDifference > 50) {
        return false;
      }
    }
    return true;
  }

  /**
   * Returns {@code true} if {@code player} has land superiority within {@code distance} neighbors of {@code t} after
   * the specified moves are performed.
   */
  public static boolean territoryHasLocalLandSuperiorityAfterMoves(final Territory t, final int distance,
      final PlayerId player, final Map<Territory, ProTerritory> moveMap) {
    final GameData data = ProData.getData();

    // Find enemy strength
    final Set<Territory> nearbyTerritoriesForEnemy =
        data.getMap().getNeighbors(t, distance, ProMatches.territoryCanMoveLandUnits(player, data, false));
    nearbyTerritoriesForEnemy.add(t);
    final List<Unit> enemyUnits = new ArrayList<>();
    for (final Territory nearbyTerritory : nearbyTerritoriesForEnemy) {
      enemyUnits.addAll(nearbyTerritory.getUnits().getMatches(ProMatches.unitIsEnemyNotNeutral(player, data)));
    }

    // Find allied strength
    final Set<Territory> nearbyTerritoriesForAllied =
        data.getMap().getNeighbors(t, distance - 1, ProMatches.territoryCanMoveLandUnits(player, data, false));
    nearbyTerritoriesForAllied.add(t);
    final List<Unit> alliedUnits = new ArrayList<>();
    for (final Territory nearbyTerritory : nearbyTerritoriesForAllied) {
      if (moveMap.get(nearbyTerritory) != null) {
        alliedUnits.addAll(moveMap.get(nearbyTerritory).getAllDefenders());
      }
    }

    // Determine strength difference
    final double strengthDifference = estimateStrengthDifference(t, enemyUnits, alliedUnits);
    ProLogger.trace(t + ", current enemy land strengthDifference=" + strengthDifference + ", enemySize="
        + enemyUnits.size() + ", alliedSize=" + alliedUnits.size());
    return strengthDifference <= 50;
  }

  /**
   * Returns {@code true} if {@code player} has naval superiority within at least 3 neighbors of {@code t}.
   */
  public static boolean territoryHasLocalNavalSuperiority(final Territory t, final PlayerId player,
      final Map<Territory, ProPurchaseTerritory> purchaseTerritories, final List<Unit> unitsToPlace) {
    final GameData data = ProData.getData();

    int landDistance = ProUtils.getClosestEnemyLandTerritoryDistanceOverWater(data, player, t);
    if (landDistance <= 0) {
      landDistance = 10;
    }
    final int enemyDistance = Math.max(3, (landDistance + 1));
    final Set<Territory> nearbyTerritories = data.getMap().getNeighbors(t, enemyDistance);
    final List<Territory> nearbyLandTerritories =
        CollectionUtils.getMatches(nearbyTerritories, Matches.territoryIsLand());
    final Set<Territory> nearbyEnemySeaTerritories =
        data.getMap().getNeighbors(t, enemyDistance, Matches.territoryIsWater());
    nearbyEnemySeaTerritories.add(t);
    final int alliedDistance = (enemyDistance + 1) / 2;
    final Set<Territory> nearbyAlliedSeaTerritories =
        data.getMap().getNeighbors(t, alliedDistance, Matches.territoryIsWater());
    nearbyAlliedSeaTerritories.add(t);
    final List<Unit> enemyUnitsInLandTerritories = new ArrayList<>();
    for (final Territory nearbyLandTerritory : nearbyLandTerritories) {
      enemyUnitsInLandTerritories
          .addAll(nearbyLandTerritory.getUnits().getMatches(ProMatches.unitIsEnemyAir(player, data)));
    }
    final List<Unit> enemyUnitsInSeaTerritories = new ArrayList<>();
    for (final Territory nearbySeaTerritory : nearbyEnemySeaTerritories) {
      final List<Unit> enemySeaUnits =
          nearbySeaTerritory.getUnits().getMatches(ProMatches.unitIsEnemyNotLand(player, data));
      if (enemySeaUnits.isEmpty()) {
        continue;
      }
      final Route route = data.getMap().getRoute_IgnoreEnd(t, nearbySeaTerritory, Matches.territoryIsWater());
      if (route == null) {
        continue;
      }
      if (MoveValidator.validateCanal(route, enemySeaUnits, enemySeaUnits.get(0).getOwner(), data) != null) {
        continue;
      }
      final int routeLength = route.numberOfSteps();
      if (routeLength <= enemyDistance) {
        enemyUnitsInSeaTerritories.addAll(enemySeaUnits);
      }
    }
    final List<Unit> alliedUnitsInSeaTerritories = new ArrayList<>();
    final List<Unit> myUnitsInSeaTerritories = new ArrayList<>();
    for (final Territory nearbySeaTerritory : nearbyAlliedSeaTerritories) {
      myUnitsInSeaTerritories.addAll(nearbySeaTerritory.getUnits().getMatches(ProMatches.unitIsOwnedNotLand(player)));
      myUnitsInSeaTerritories.addAll(ProPurchaseUtils.getPlaceUnits(nearbySeaTerritory, purchaseTerritories));
      alliedUnitsInSeaTerritories
          .addAll(nearbySeaTerritory.getUnits().getMatches(ProMatches.unitIsAlliedNotOwned(player, data)));
    }
    ProLogger.trace(t + ", enemyDistance=" + enemyDistance + ", alliedDistance=" + alliedDistance + ", enemyAirUnits="
        + enemyUnitsInLandTerritories + ", enemySeaUnits=" + enemyUnitsInSeaTerritories + ", mySeaUnits="
        + myUnitsInSeaTerritories);

    // Find current naval defense strength
    final List<Unit> myUnits = new ArrayList<>(myUnitsInSeaTerritories);
    myUnits.addAll(unitsToPlace);
    myUnits.addAll(alliedUnitsInSeaTerritories);
    final List<Unit> enemyAttackers = new ArrayList<>(enemyUnitsInSeaTerritories);
    enemyAttackers.addAll(enemyUnitsInLandTerritories);
    final double defenseStrengthDifference = estimateStrengthDifference(t, enemyAttackers, myUnits);
    ProLogger.trace(t + ", current enemy naval attack strengthDifference=" + defenseStrengthDifference + ", enemySize="
        + enemyAttackers.size() + ", alliedSize=" + myUnits.size());

    // Find current naval attack strength
    double attackStrengthDifference = estimateStrengthDifference(t, myUnits, enemyUnitsInSeaTerritories);
    attackStrengthDifference +=
        0.5 * estimateStrengthDifference(t, alliedUnitsInSeaTerritories, enemyUnitsInSeaTerritories);
    ProLogger.trace(t + ", current allied naval attack strengthDifference=" + attackStrengthDifference + ", alliedSize="
        + myUnits.size() + ", enemySize=" + enemyUnitsInSeaTerritories.size());

    // If I have naval attack/defense superiority then break
    return (defenseStrengthDifference < 50 && attackStrengthDifference > 50);
  }

}
