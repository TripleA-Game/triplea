package games.strategy.triplea.ai.pro.util;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameMap;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.triplea.util.Tuple;

/** Pro AI move utilities. */
public final class ProMoveUtils {
  private ProMoveUtils() {}

  private static class Move {
    private final ArrayList<Unit> units;
    private final Route route;
    private final ArrayList<Unit> transportsToLoad;

    Move(final ArrayList<Unit> units, final Route route, final ArrayList<Unit> transportsToLoad) {
      this.units = units;
      this.route = route;
      this.transportsToLoad = transportsToLoad;
    }

    Move(final ArrayList<Unit> units, final Route route) {
      this(units, route, null);
    }

    Move(final Unit unit, final Route route, final Unit transportToLoad) {
      this(mutableSingletonList(unit), route, mutableSingletonList(transportToLoad));
    }

    private static ArrayList<Unit> mutableSingletonList(final Unit unit) {
      return new ArrayList<>(Collections.singletonList(unit));
    }

    boolean isTransportLoad() {
      return this.transportsToLoad != null;
    }

    boolean mergeWith(final Move other) {
      if (other != null
          && other.isTransportLoad() == isTransportLoad()
          && route.equals(other.route)) {
        // Merge units and transports.
        units.addAll(other.units);
        if (isTransportLoad()) {
          transportsToLoad.addAll(other.transportsToLoad);
        }
        return true;
      }
      return false;
    }

    void addTo(
        final List<Collection<Unit>> moveUnits,
        final List<Route> moveRoutes,
        final List<Collection<Unit>> transportsToLoad) {
      moveUnits.add(units);
      moveRoutes.add(route);
      transportsToLoad.add(this.transportsToLoad);
    }
  }

  /**
   * Calculates normal movement routes (e.g. land, air, sea attack routes, not including amphibious,
   * bombardment, or strategic bombing raid attack routes).
   *
   * @param moveUnits Receives the unit groups to move.
   * @param moveRoutes Receives the routes for each unit group in {@code moveUnits}.
   * @param attackMap Specifies the territories to be attacked.
   */
  public static void calculateMoveRoutes(
      final PlayerId player,
      final List<Collection<Unit>> moveUnits,
      final List<Route> moveRoutes,
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
        final List<Unit> unitList = new ArrayList<>();
        unitList.add(u);
        moveUnits.add(unitList);
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
        moveRoutes.add(route);
      }
    }
  }

  /**
   * Calculates amphibious movement routes.
   *
   * @param moveUnits Receives the unit groups to move.
   * @param moveRoutes Receives the routes for each unit group in {@code moveUnits}.
   * @param transportsToLoad Receives the transport groups for each unit group in {@code moveUnits}.
   * @param attackMap Specifies the territories to be attacked. Will be updated to reflect any
   *     transports unloading in a specific territory.
   */
  public static void calculateAmphibRoutes(
      final PlayerId player,
      final List<Collection<Unit>> moveUnits,
      final List<Route> moveRoutes,
      final List<Collection<Unit>> transportsToLoad,
      final Map<Territory, ProTerritory> attackMap,
      final boolean isCombatMove) {

    final GameData data = ProData.getData();
    final GameMap map = data.getMap();

    final var movesMap = new HashMap<Territory, ArrayList<Move>>();
    // Loop through all territories to attack
    for (final Territory t : attackMap.keySet()) {

      // Loop through each amphib attack map
      final Map<Unit, List<Unit>> amphibAttackMap = attackMap.get(t).getAmphibAttackMap();
      for (final Unit transport : amphibAttackMap.keySet()) {
        int movesLeft = TripleAUnit.get(transport).getMovementLeft().intValue();
        Territory transportTerritory = ProData.unitTerritoryMap.get(transport);
        ArrayList<Move> moves = movesMap.computeIfAbsent(transportTerritory, k -> new ArrayList<Move>());

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
              if (map.getDistance(transportTerritory, ProData.unitTerritoryMap.get(amphibUnit))
                  == 1) {
                final Route route =
                    new Route(ProData.unitTerritoryMap.get(amphibUnit), transportTerritory);
                moves.add(new Move(amphibUnit, route, transport));
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
                      new Route(transportTerritory, neighbor),
                      Collections.singletonList(transport),
                      player,
                      data)
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
              final Route route = new Route(transportTerritory, territoryToMoveTo);
              moves.add(new Move(unitsToMove, route));
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
          moves.add(new Move(loadedUnits, route));
        }
      }
    }

    // Re-order and batch the moves. That is, move all the units together, then
    // move all the transports together, then unload them all.
    for (final ArrayList<Move> moves : movesMap.values()) {
      // First, add all the transport loads.
      int i = 0;
      for (final Move move : moves) {
        if (move != null && move.isTransportLoad()) {
          // Find all others with the same route to merge with.
          mergeMoves(move, moves, i + 1);
          move.addTo(moveUnits, moveRoutes, transportsToLoad);
          moves.set(i, null);
        }
        i++;
      }

      // Then, add all the transport and unload moves, merging moves together.
      // Since we process the moves in order, no special logic is needed to make
      // sure transports move before unloading.
      i = 0;
      for (final Move move : moves) {
        if (move != null) {
          mergeMoves(move, moves, i + 1);
          move.addTo(moveUnits, moveRoutes, transportsToLoad);
        }
        i++;
      }
    }
  }

  private static void mergeMoves(
      final Move move, final ArrayList<Move> moves, final int startIndex) {
    for (int i = startIndex; i < moves.size(); i++) {
      if (move.mergeWith(moves.get(i))) {
        moves.set(i, null);
      }
    }
  }

  /**
   * Calculates bombardment movement routes.
   *
   * @param moveUnits Receives the unit groups to move.
   * @param moveRoutes Receives the routes for each unit group in {@code moveUnits}.
   * @param attackMap Specifies the territories to be attacked.
   */
  public static void calculateBombardMoveRoutes(
      final PlayerId player,
      final List<Collection<Unit>> moveUnits,
      final List<Route> moveRoutes,
      final Map<Territory, ProTerritory> attackMap) {

    final GameData data = ProData.getData();
    final GameMap map = data.getMap();

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
        final List<Unit> unitList = new ArrayList<>();
        unitList.add(u);
        moveUnits.add(unitList);

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
        moveRoutes.add(route);
      }
    }
  }

  /**
   * Calculates strategic bombing raid movement routes.
   *
   * @param moveUnits Receives the unit groups to move.
   * @param moveRoutes Receives the routes for each unit group in {@code moveUnits}.
   * @param attackMap Specifies the territories to be attacked.
   */
  public static void calculateBombingRoutes(
      final PlayerId player,
      final List<Collection<Unit>> moveUnits,
      final List<Route> moveRoutes,
      final Map<Territory, ProTerritory> attackMap) {

    final GameData data = ProData.getData();
    final GameMap map = data.getMap();

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
        final List<Unit> unitList = new ArrayList<>();
        unitList.add(u);
        moveUnits.add(unitList);

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
        moveRoutes.add(route);
      }
    }
  }

  public static void doMove(
      final List<Collection<Unit>> moveUnits,
      final List<Route> moveRoutes,
      final IMoveDelegate moveDel) {
    doMove(moveUnits, moveRoutes, null, moveDel);
  }

  /**
   * Moves the specified groups of units along the specified routes, possibly using the specified
   * transports.
   */
  public static void doMove(
      final List<Collection<Unit>> moveUnits,
      final List<Route> moveRoutes,
      final List<Collection<Unit>> transportsToLoad,
      final IMoveDelegate moveDel) {

    final GameData data = ProData.getData();

    // Group non-amphib units of the same type moving on the same route
    if (transportsToLoad == null) {
      for (int i = 0; i < moveRoutes.size(); i++) {
        final Route r = moveRoutes.get(i);
        for (int j = i + 1; j < moveRoutes.size(); j++) {
          final Route r2 = moveRoutes.get(j);
          if (r.equals(r2)) {
            moveUnits.get(j).addAll(moveUnits.get(i));
            moveUnits.remove(i);
            moveRoutes.remove(i);
            i--;
            break;
          }
        }
      }
    }

    // Move units
    for (int i = 0; i < moveRoutes.size(); i++) {
      if (!ProData.isSimulation) {
        ProUtils.pause();
      }
      if (moveRoutes.get(i) == null
          || moveRoutes.get(i).getEnd() == null
          || moveRoutes.get(i).getStart() == null) {
        ProLogger.warn(
            data.getSequence().getRound()
                + "-"
                + data.getSequence().getStep().getName()
                + ": route not valid "
                + moveRoutes.get(i)
                + " units: "
                + moveUnits.get(i));
        continue;
      }
      final String result;
      if (transportsToLoad == null || transportsToLoad.get(i) == null) {
        result = moveDel.move(moveUnits.get(i), moveRoutes.get(i));
      } else {
        result = moveDel.move(moveUnits.get(i), moveRoutes.get(i), transportsToLoad.get(i));
      }
      if (result != null) {
        ProLogger.warn(
            data.getSequence().getRound()
                + "-"
                + data.getSequence().getStep().getName()
                + ": could not move "
                + moveUnits.get(i)
                + " over "
                + moveRoutes.get(i)
                + " because: "
                + result);
      }
    }
  }
}
