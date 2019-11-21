package games.strategy.triplea.ai.pro.util;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameMap;
import games.strategy.engine.data.MoveDescription;
import games.strategy.engine.data.PlayerId;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.TripleAUnit;
import games.strategy.triplea.ai.pro.ProData;
import games.strategy.triplea.ai.pro.data.ProTerritory;
import games.strategy.triplea.ai.pro.logging.ProLogger;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.MoveValidator;
import games.strategy.triplea.delegate.TransportTracker;
import games.strategy.triplea.delegate.remote.IMoveDelegate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.triplea.util.Tuple;

/** Pro AI move utilities. */
public final class ProMoveUtils {
  private ProMoveUtils() {}

  /**
   * Calculates normal movement routes (e.g. land, air, sea attack routes, not including amphibious,
   * bombardment, or strategic bombing raid attack routes).
   *
   * @param attackMap Specifies the territories to be attacked.
   * @return The list of moves to perform.
   */
  public static List<MoveDescription> calculateMoveRoutes(
      final PlayerId player,
      final Map<Territory, ProTerritory> attackMap,
      final boolean isCombatMove) {

    final GameData data = ProData.getData();
    final GameMap map = data.getMap();

    // Find all amphib units
    final Set<Unit> amphibUnits =
        attackMap.values().stream()
            .map(ProTerritory::getAmphibAttackMap)
            .map(Map::entrySet)
            .flatMap(Collection::stream)
            .flatMap(e -> Stream.concat(Stream.of(e.getKey()), e.getValue().stream()))
            .collect(Collectors.toSet());

    final var moves = new ArrayList<MoveDescription>();
    // Loop through all territories to attack
    for (final Territory t : attackMap.keySet()) {

      // Loop through each unit that is attacking the current territory
      Tuple<Territory, Unit> lastLandTransport = Tuple.of(null, null);
      for (final Unit u : attackMap.get(t).getUnits()) {

        // Skip amphib units
        if (amphibUnits.contains(u)) {
          continue;
        }

        // Skip if unit is already in move to territory
        final Territory startTerritory = ProData.unitTerritoryMap.get(u);
        if (startTerritory == null || startTerritory.equals(t)) {
          continue;
        }

        // Add unit to move list
        final var unitList = new ArrayList<Unit>();
        unitList.add(u);
        if (Matches.unitIsLandTransport().test(u)) {
          lastLandTransport = Tuple.of(startTerritory, u);
        }

        // If carrier has dependent allied fighters then move them too
        if (Matches.unitIsCarrier().test(u)) {
          final Map<Unit, Collection<Unit>> carrierMustMoveWith =
              MoveValidator.carrierMustMoveWith(
                  startTerritory.getUnits(), startTerritory, data, player);
          if (carrierMustMoveWith.containsKey(u)) {
            unitList.addAll(carrierMustMoveWith.get(u));
          }
        }

        // Determine route and add to move list
        Route route = null;
        if (unitList.stream().anyMatch(Matches.unitIsSea())) {

          // Sea unit (including carriers with planes)
          route =
              map.getRouteForUnit(
                  startTerritory,
                  t,
                  ProMatches.territoryCanMoveSeaUnitsThrough(player, data, isCombatMove),
                  u,
                  player);
        } else if (!unitList.isEmpty() && unitList.stream().allMatch(Matches.unitIsLand())) {

          // Land unit
          route =
              map.getRouteForUnit(
                  startTerritory,
                  t,
                  ProMatches.territoryCanMoveLandUnitsThrough(
                      player, data, u, startTerritory, isCombatMove, List.of()),
                  u,
                  player);
          if (route == null && startTerritory.equals(lastLandTransport.getFirst())) {
            route =
                map.getRouteForUnit(
                    startTerritory,
                    t,
                    ProMatches.territoryCanMoveLandUnitsThrough(
                        player,
                        data,
                        lastLandTransport.getSecond(),
                        startTerritory,
                        isCombatMove,
                        List.of()),
                    u,
                    player);
          }
        } else if (!unitList.isEmpty() && unitList.stream().allMatch(Matches.unitIsAir())) {

          // Air unit
          route =
              map.getRouteForUnit(
                  startTerritory,
                  t,
                  ProMatches.territoryCanMoveAirUnitsAndNoAa(player, data, isCombatMove),
                  u,
                  player);
        }
        if (route == null) {
          ProLogger.warn(
              data.getSequence().getRound()
                  + "-"
                  + data.getSequence().getStep().getName()
                  + ": route is null "
                  + startTerritory
                  + " to "
                  + t
                  + ", units="
                  + unitList);
        }
        moves.add(new MoveDescription(unitList, route));
      }
    }
    return moves;
  }

  /**
   * Calculates amphibious movement routes.
   *
   * @param attackMap Specifies the territories to be attacked. Will be updated to reflect any
   *     transports unloading in a specific territory.
   * @return The list of moves to perform.
   */
  public static List<MoveDescription> calculateAmphibRoutes(
      final PlayerId player,
      final Map<Territory, ProTerritory> attackMap,
      final boolean isCombatMove) {

    final GameData data = ProData.getData();
    final GameMap map = data.getMap();

    final MoveBatcher moves = new MoveBatcher();

    // Loop through all territories to attack
    for (final Territory t : attackMap.keySet()) {

      // Loop through each amphib attack map
      final Map<Unit, List<Unit>> amphibAttackMap = attackMap.get(t).getAmphibAttackMap();
      for (final Unit transport : amphibAttackMap.keySet()) {
        int movesLeft = TripleAUnit.get(transport).getMovementLeft().intValue();
        Territory transportTerritory = ProData.unitTerritoryMap.get(transport);
        moves.newSequence();

        // Check if units are already loaded or not
        final var loadedUnits = new ArrayList<Unit>();
        final var remainingUnitsToLoad = new ArrayList<Unit>();

        if (TransportTracker.isTransporting(transport)) {
          loadedUnits.addAll(amphibAttackMap.get(transport));
        } else {
          remainingUnitsToLoad.addAll(amphibAttackMap.get(transport));
        }

        // Load units and move transport
        while (movesLeft >= 0) {

          // Load adjacent units if no enemies present in transport territory
          if (Matches.territoryHasEnemyUnits(player, data).negate().test(transportTerritory)) {
            final var unitsToRemove = new ArrayList<Unit>();
            for (final Unit amphibUnit : remainingUnitsToLoad) {
              final Territory unitTerritory = ProData.unitTerritoryMap.get(amphibUnit);
              if (map.getDistance(transportTerritory, unitTerritory) == 1) {
                final Route route = new Route(unitTerritory, transportTerritory);
                moves.addTransportLoad(amphibUnit, route, transport);
                unitsToRemove.add(amphibUnit);
                loadedUnits.add(amphibUnit);
              }
            }
            for (final Unit u : unitsToRemove) {
              remainingUnitsToLoad.remove(u);
            }
          }

          // Move transport if I'm not already at the end or out of moves
          final Territory unloadTerritory =
              attackMap.get(t).getTransportTerritoryMap().get(transport);
          int distanceFromEnd = map.getDistance(transportTerritory, t);
          if (t.isWater()) {
            distanceFromEnd++;
          }
          if (movesLeft > 0
              && (distanceFromEnd > 1
                  || !remainingUnitsToLoad.isEmpty()
                  || (unloadTerritory != null && !unloadTerritory.equals(transportTerritory)))) {
            final Set<Territory> neighbors =
                map.getNeighbors(
                    transportTerritory,
                    ProMatches.territoryCanMoveSeaUnitsThrough(player, data, isCombatMove));
            Territory territoryToMoveTo = null;
            int minUnitDistance = Integer.MAX_VALUE;
            int maxDistanceFromEnd =
                Integer.MIN_VALUE; // Used to move to farthest away loading territory first
            for (final Territory neighbor : neighbors) {
              if (MoveValidator.validateCanal(
                      new Route(transportTerritory, neighbor), List.of(transport), player, data)
                  != null) {
                continue;
              }
              int distanceFromUnloadTerritory = 0;
              if (unloadTerritory != null) {
                distanceFromUnloadTerritory =
                    map.getDistance_IgnoreEndForCondition(
                        neighbor,
                        unloadTerritory,
                        ProMatches.territoryCanMoveSeaUnitsThrough(player, data, isCombatMove));
              }
              int neighborDistanceFromEnd =
                  map.getDistance_IgnoreEndForCondition(
                      neighbor,
                      t,
                      ProMatches.territoryCanMoveSeaUnitsThrough(player, data, isCombatMove));
              if (t.isWater()) {
                neighborDistanceFromEnd++;
              }
              int maxUnitDistance = 0;
              for (final Unit u : remainingUnitsToLoad) {
                final int distance = map.getDistance(neighbor, ProData.unitTerritoryMap.get(u));
                if (distance > maxUnitDistance) {
                  maxUnitDistance = distance;
                }
              }
              if (neighborDistanceFromEnd <= movesLeft
                  && maxUnitDistance <= minUnitDistance
                  && distanceFromUnloadTerritory < movesLeft
                  && (maxUnitDistance < minUnitDistance
                      || (maxUnitDistance > 1 && neighborDistanceFromEnd > maxDistanceFromEnd)
                      || (maxUnitDistance <= 1 && neighborDistanceFromEnd < maxDistanceFromEnd))) {
                territoryToMoveTo = neighbor;
                minUnitDistance = maxUnitDistance;
                if (neighborDistanceFromEnd > maxDistanceFromEnd) {
                  maxDistanceFromEnd = neighborDistanceFromEnd;
                }
              }
            }
            if (territoryToMoveTo != null) {
              final var unitsToMove = new ArrayList<Unit>();
              unitsToMove.add(transport);
              unitsToMove.addAll(loadedUnits);
              moves.addMove(unitsToMove, new Route(transportTerritory, territoryToMoveTo));
              transportTerritory = territoryToMoveTo;
            }
          }
          movesLeft--;
        }
        if (!remainingUnitsToLoad.isEmpty()) {
          ProLogger.warn(
              data.getSequence().getRound()
                  + "-"
                  + data.getSequence().getStep().getName()
                  + ": "
                  + t
                  + ", remainingUnitsToLoad="
                  + remainingUnitsToLoad);
        }

        // Set territory transport is moving to
        attackMap.get(t).getTransportTerritoryMap().put(transport, transportTerritory);

        // Unload transport
        if (!loadedUnits.isEmpty() && !t.isWater()) {
          final Route route = new Route(transportTerritory, t);
          moves.addMove(loadedUnits, route);
        }
      }
    }

    return moves.batchMoves();
  }

  /**
   * Calculates bombardment movement routes.
   *
   * @param attackMap Specifies the territories to be attacked.
   * @return The list of moves to perform.
   */
  public static List<MoveDescription> calculateBombardMoveRoutes(
      final PlayerId player, final Map<Territory, ProTerritory> attackMap) {

    final GameData data = ProData.getData();
    final GameMap map = data.getMap();

    final var moves = new ArrayList<MoveDescription>();

    // Loop through all territories to attack
    for (final ProTerritory t : attackMap.values()) {

      // Loop through each unit that is attacking the current territory
      for (final Unit u : t.getBombardTerritoryMap().keySet()) {
        final Territory bombardFromTerritory = t.getBombardTerritoryMap().get(u);

        // Skip if unit is already in move to territory
        final Territory startTerritory = ProData.unitTerritoryMap.get(u);
        if (startTerritory.equals(bombardFromTerritory)) {
          continue;
        }

        // Add unit to move list
        final var unitList = new ArrayList<Unit>();
        unitList.add(u);

        // Determine route and add to move list
        Route route = null;
        if (!unitList.isEmpty()
            && unitList.stream().allMatch(ProMatches.unitCanBeMovedAndIsOwnedSea(player, true))) {

          // Naval unit
          route =
              map.getRouteForUnit(
                  startTerritory,
                  bombardFromTerritory,
                  ProMatches.territoryCanMoveSeaUnitsThrough(player, data, true),
                  u,
                  player);
        }
        moves.add(new MoveDescription(unitList, route));
      }
    }

    return moves;
  }

  /**
   * Calculates strategic bombing raid movement routes.
   *
   * @param attackMap Specifies the territories to be attacked.
   * @return The list of moves to perform.
   */
  public static List<MoveDescription> calculateBombingRoutes(
      final PlayerId player, final Map<Territory, ProTerritory> attackMap) {

    final GameData data = ProData.getData();
    final GameMap map = data.getMap();

    final var moves = new ArrayList<MoveDescription>();

    // Loop through all territories to attack
    for (final Territory t : attackMap.keySet()) {

      // Loop through each unit that is attacking the current territory
      for (final Unit u : attackMap.get(t).getBombers()) {

        // Skip if unit is already in move to territory
        final Territory startTerritory = ProData.unitTerritoryMap.get(u);
        if (startTerritory == null || startTerritory.equals(t)) {
          continue;
        }

        // Add unit to move list
        final var unitList = new ArrayList<Unit>();
        unitList.add(u);

        // Determine route and add to move list
        Route route = null;
        if (!unitList.isEmpty() && unitList.stream().allMatch(Matches.unitIsAir())) {
          route =
              map.getRouteForUnit(
                  startTerritory,
                  t,
                  ProMatches.territoryCanMoveAirUnitsAndNoAa(player, data, true),
                  u,
                  player);
        }
        moves.add(new MoveDescription(unitList, route));
      }
    }
    return moves;
  }

  /**
   * Moves the specified groups of units along the specified routes, possibly using the specified
   * transports.
   */
  public static void doMove(final List<MoveDescription> moves, final IMoveDelegate moveDel) {
    final GameData data = ProData.getData();

    // Group non-amphib units of the same type moving on the same route
    // TODO: #5499 Use MoveBatcher here - or ideally at the time the moves are being generated.
    final boolean noTransportLoads =
        moves.stream().allMatch(move -> move.getUnitsToTransports().isEmpty());
    if (noTransportLoads) {
      for (int i = 0; i < moves.size(); i++) {
        final Route r = moves.get(i).getRoute();
        for (int j = i + 1; j < moves.size(); j++) {
          final Route r2 = moves.get(j).getRoute();
          if (r.equals(r2)) {
            final var mergedUnits = new ArrayList<Unit>();
            mergedUnits.addAll(moves.get(j).getUnits());
            mergedUnits.addAll(moves.get(i).getUnits());
            moves.set(i, new MoveDescription(mergedUnits, r));
            moves.remove(i);
            i--;
            break;
          }
        }
      }
    }

    // Move units
    for (final MoveDescription move : moves) {
      // TODO: #5499 Validate this when MoveDescription is constructed.
      if (move.getRoute().getEnd() == null || move.getRoute().getStart() == null) {
        ProLogger.warn(
            data.getSequence().getRound()
                + "-"
                + data.getSequence().getStep().getName()
                + ": route not valid "
                + move.getRoute()
                + " units: "
                + move.getUnits());
        continue;
      }
      final String result = moveDel.performMove(move);
      if (result != null) {
        ProLogger.warn(
            data.getSequence().getRound()
                + "-"
                + data.getSequence().getStep().getName()
                + ": could not move "
                + move.getUnits()
                + " over "
                + move.getRoute()
                + " because: "
                + result);
      }
      if (!ProData.isSimulation) {
        ProUtils.pause();
      }
    }
  }
}
