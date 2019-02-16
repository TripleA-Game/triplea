package games.strategy.triplea.ai.pro.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.triplea.java.Interruptibles;
import org.triplea.java.collections.CollectionUtils;

import com.google.common.collect.Streams;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameSequence;
import games.strategy.engine.data.GameStep;
import games.strategy.engine.data.PlayerId;
import games.strategy.engine.data.RelationshipTracker;
import games.strategy.engine.data.RelationshipType;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.Properties;
import games.strategy.triplea.ai.pro.ProData;
import games.strategy.triplea.attachments.TerritoryAttachment;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.settings.ClientSetting;

/**
 * Pro AI utilities (these are very general and maybe should be moved into delegate or engine).
 */
public final class ProUtils {
  private ProUtils() {}

  public static Map<Unit, Territory> newUnitTerritoryMap() {
    final Map<Unit, Territory> unitTerritoryMap = new HashMap<>();
    for (final Territory t : ProData.getData().getMap().getTerritories()) {
      for (final Unit u : t.getUnits()) {
        unitTerritoryMap.put(u, t);
      }
    }
    return unitTerritoryMap;
  }

  /**
   * Returns a list of all players in turn order excluding {@code player}.
   */
  public static List<PlayerId> getOtherPlayersInTurnOrder(final PlayerId player) {
    final GameData data = ProData.getData();
    final List<PlayerId> players = new ArrayList<>();
    final GameSequence sequence = data.getSequence();
    final int startIndex = sequence.getStepIndex();
    for (int i = 0; i < sequence.size(); i++) {
      int currentIndex = startIndex + i;
      if (currentIndex >= sequence.size()) {
        currentIndex -= sequence.size();
      }
      final GameStep step = sequence.getStep(currentIndex);
      final PlayerId stepPlayer = step.getPlayerId();
      if (step.getName().endsWith("CombatMove") && stepPlayer != null && !stepPlayer.equals(player)
          && !players.contains(stepPlayer)) {
        players.add(step.getPlayerId());
      }
    }
    return players;
  }

  public static List<PlayerId> getAlliedPlayersInTurnOrder(final PlayerId player) {
    final GameData data = ProData.getData();
    final List<PlayerId> players = getOtherPlayersInTurnOrder(player);
    players.removeIf(currentPlayer -> !data.getRelationshipTracker().isAllied(player, currentPlayer));
    return players;
  }

  public static List<PlayerId> getEnemyPlayersInTurnOrder(final PlayerId player) {
    final GameData data = ProData.getData();
    final List<PlayerId> players = getOtherPlayersInTurnOrder(player);
    players.removeIf(currentPlayer -> data.getRelationshipTracker().isAllied(player, currentPlayer));
    return players;
  }

  public static boolean isPlayersTurnFirst(final List<PlayerId> playersInOrder, final PlayerId player1,
      final PlayerId player2) {
    for (final PlayerId p : playersInOrder) {
      if (p.equals(player1)) {
        return true;
      } else if (p.equals(player2)) {
        return false;
      }
    }
    return true;
  }

  private static List<PlayerId> getEnemyPlayers(final PlayerId player) {
    final GameData data = ProData.getData();
    final List<PlayerId> enemyPlayers = new ArrayList<>();
    for (final PlayerId players : data.getPlayerList().getPlayers()) {
      if (!data.getRelationshipTracker().isAllied(player, players)) {
        enemyPlayers.add(players);
      }
    }
    return enemyPlayers;
  }

  private static List<PlayerId> getAlliedPlayers(final PlayerId player) {
    final GameData data = ProData.getData();
    final List<PlayerId> alliedPlayers = new ArrayList<>();
    for (final PlayerId players : data.getPlayerList().getPlayers()) {
      if (data.getRelationshipTracker().isAllied(player, players)) {
        alliedPlayers.add(players);
      }
    }
    return alliedPlayers;
  }

  public static List<PlayerId> getPotentialEnemyPlayers(final PlayerId player) {
    final GameData data = ProData.getData();
    final List<PlayerId> otherPlayers = data.getPlayerList().getPlayers();
    for (final Iterator<PlayerId> it = otherPlayers.iterator(); it.hasNext();) {
      final PlayerId otherPlayer = it.next();
      final RelationshipType relation = data.getRelationshipTracker().getRelationshipType(player, otherPlayer);
      if (Matches.relationshipTypeIsAllied().test(relation) || isPassiveNeutralPlayer(otherPlayer)) {
        it.remove();
      }
    }
    return otherPlayers;
  }

  public static double getPlayerProduction(final PlayerId player, final GameData data) {
    int production = 0;
    for (final Territory place : data.getMap().getTerritories()) {
      // Match will Check if terr is a Land Convoy Route and check ownership of neighboring Sea Zone, or if contested
      if (place.getOwner().equals(player) && Matches.territoryCanCollectIncomeFrom(player, data).test(place)) {
        production += TerritoryAttachment.getProduction(place);
      }
    }
    production *= Properties.getPuMultiplier(data);
    return production;
  }

  public static List<Territory> getLiveEnemyCapitals(final GameData data, final PlayerId player) {
    final List<Territory> enemyCapitals = new ArrayList<>();
    final List<PlayerId> enemyPlayers = getEnemyPlayers(player);
    for (final PlayerId otherPlayer : enemyPlayers) {
      enemyCapitals.addAll(TerritoryAttachment.getAllCurrentlyOwnedCapitals(otherPlayer, data));
    }
    enemyCapitals.retainAll(
        CollectionUtils.getMatches(enemyCapitals, Matches.territoryIsNotImpassableToLandUnits(player, data)));
    enemyCapitals.retainAll(
        CollectionUtils.getMatches(enemyCapitals, Matches.isTerritoryOwnedBy(getPotentialEnemyPlayers(player))));
    return enemyCapitals;
  }

  public static List<Territory> getLiveAlliedCapitals(final GameData data, final PlayerId player) {
    final List<Territory> capitals = new ArrayList<>();
    final List<PlayerId> players = getAlliedPlayers(player);
    for (final PlayerId alliedPlayer : players) {
      capitals.addAll(TerritoryAttachment.getAllCurrentlyOwnedCapitals(alliedPlayer, data));
    }
    capitals.retainAll(CollectionUtils.getMatches(capitals, Matches.territoryIsNotImpassableToLandUnits(player, data)));
    capitals.retainAll(CollectionUtils.getMatches(capitals, Matches.isTerritoryAllied(player, data)));
    return capitals;
  }

  /**
   * Returns the distance to the closest enemy land territory to {@code t}.
   *
   * @return -1 if there is no enemy land territory within a distance of 10 of {@code t}.
   */
  public static int getClosestEnemyLandTerritoryDistance(final GameData data, final PlayerId player,
      final Territory t) {
    final Set<Territory> landTerritories =
        data.getMap().getNeighbors(t, 9, ProMatches.territoryCanPotentiallyMoveLandUnits(player, data));
    final List<Territory> enemyLandTerritories =
        CollectionUtils.getMatches(landTerritories, Matches.isTerritoryOwnedBy(getPotentialEnemyPlayers(player)));
    int minDistance = 10;
    for (final Territory enemyLandTerritory : enemyLandTerritories) {
      final int distance = data.getMap().getDistance(t, enemyLandTerritory,
          ProMatches.territoryCanPotentiallyMoveLandUnits(player, data));
      if (distance < minDistance) {
        minDistance = distance;
      }
    }
    return (minDistance < 10) ? minDistance : -1;
  }

  /**
   * Returns the distance to the closest enemy or neutral land territory to {@code t}.
   *
   * @return -1 if there is no enemy or neutral land territory within a distance of 10 of {@code t}.
   */
  public static int getClosestEnemyOrNeutralLandTerritoryDistance(final GameData data, final PlayerId player,
      final Territory t, final Map<Territory, Double> territoryValueMap) {
    final Set<Territory> landTerritories =
        data.getMap().getNeighbors(t, 9, ProMatches.territoryCanPotentiallyMoveLandUnits(player, data));
    final List<Territory> enemyLandTerritories =
        CollectionUtils.getMatches(landTerritories, Matches.isTerritoryOwnedBy(getEnemyPlayers(player)));
    int minDistance = 10;
    for (final Territory enemyLandTerritory : enemyLandTerritories) {
      if (territoryValueMap.get(enemyLandTerritory) <= 0) {
        continue;
      }
      int distance = data.getMap().getDistance(t, enemyLandTerritory,
          ProMatches.territoryCanPotentiallyMoveLandUnits(player, data));
      if (ProUtils.isNeutralLand(enemyLandTerritory)) {
        distance++;
      }
      if (distance < minDistance) {
        minDistance = distance;
      }
    }
    return (minDistance < 10) ? minDistance : -1;
  }

  /**
   * Returns the distance to the closest enemy land territory to {@code t} assuming movement only through water
   * territories.
   *
   * @return -1 if there is no enemy land territory within a distance of 10 of {@code t} when moving only through water
   *         territories.
   */
  public static int getClosestEnemyLandTerritoryDistanceOverWater(final GameData data, final PlayerId player,
      final Territory t) {
    final Set<Territory> neighborTerritories = data.getMap().getNeighbors(t, 9);
    final List<Territory> enemyOrAdjacentLandTerritories = CollectionUtils.getMatches(neighborTerritories,
        ProMatches.territoryIsOrAdjacentToEnemyNotNeutralLand(player, data));
    int minDistance = 10;
    for (final Territory enemyLandTerritory : enemyOrAdjacentLandTerritories) {
      final int distance =
          data.getMap().getDistance_IgnoreEndForCondition(t, enemyLandTerritory, Matches.territoryIsWater());
      if (distance > 0 && distance < minDistance) {
        minDistance = distance;
      }
    }
    return (minDistance < 10) ? minDistance : -1;
  }

  /**
   * Returns whether the game is a FFA based on whether any of the player's enemies are enemies of each other.
   */
  public static boolean isFfa(final GameData data, final PlayerId player) {
    final RelationshipTracker relationshipTracker = data.getRelationshipTracker();
    final Set<PlayerId> enemies = relationshipTracker.getEnemies(player);
    final Set<PlayerId> enemiesWithoutNeutrals =
        enemies.stream().filter(p -> !isNeutralPlayer(p)).collect(Collectors.toSet());
    return enemiesWithoutNeutrals.stream()
        .anyMatch(e -> relationshipTracker.isAtWarWithAnyOfThesePlayers(e, enemiesWithoutNeutrals));
  }

  public static boolean isNeutralLand(final Territory t) {
    return !t.isWater() && ProUtils.isNeutralPlayer(t.getOwner());
  }

  /**
   * Determines whether a player is neutral by checking if all players in its alliance can be considered
   * neutral as defined by: isPassiveNeutralPlayer OR (isHidden and defaultType is AI or DoesNothing).
   */
  public static boolean isNeutralPlayer(final PlayerId player) {
    if (player.isNull()) {
      return true;
    }
    final Set<PlayerId> allies = player.getData().getRelationshipTracker().getAllies(player, true);
    return allies.stream().allMatch(
        a -> isPassiveNeutralPlayer(a) || (a.isHidden() && (a.isDefaultTypeAi() || a.isDefaultTypeDoesNothing())));
  }

  /**
   * Returns true if the player is Null or doesn't have a combat move phase.
   */
  public static boolean isPassiveNeutralPlayer(final PlayerId player) {
    if (player.isNull()) {
      return true;
    }
    return Streams.stream(player.getData().getSequence()).noneMatch(s -> player.equals(s.getPlayerId())
        && s.getName().endsWith("CombatMove") && !s.getName().endsWith("NonCombatMove"));
  }

  /**
   * Pause the game to allow the human player to see what is going on.
   */
  public static void pause() {
    Interruptibles.sleep(ClientSetting.aiPauseDuration.getValueOrThrow());
  }
}
