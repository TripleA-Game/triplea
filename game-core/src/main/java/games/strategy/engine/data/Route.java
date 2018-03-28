package games.strategy.engine.data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

import games.strategy.engine.data.changefactory.ChangeFactory;
import games.strategy.triplea.Properties;
import games.strategy.triplea.TripleAUnit;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.delegate.AirMovementValidator;
import games.strategy.triplea.delegate.GameStepPropertiesHelper;
import games.strategy.triplea.delegate.Matches;
import games.strategy.util.CollectionUtils;
import games.strategy.util.Tuple;

/**
 * A route between two territories.
 *
 * <p>
 * A route consists of a start territory, and a sequence of steps. To create a route do,
 * <code>
 * Route aRoute = new Route();
 * route.setStart(someTerritory);
 * route.add(anotherTerritory);
 * route.add(yetAnotherTerritory);
 * </code>
 * </p>
 */
public class Route implements Serializable, Iterable<Territory> {
  private static final long serialVersionUID = 8743882455488948557L;
  private static final List<Territory> EMPTY_TERRITORY_LIST = Collections.emptyList();
  private final List<Territory> m_steps = new ArrayList<>();
  private Territory m_start;

  public Route() {}

  public Route(final List<Territory> route) {
    setStart(route.get(0));
    if (route.size() == 1) {
      return;
    }
    for (final Territory t : route.subList(1, route.size())) {
      add(t);
    }
  }

  public Route(final Territory start, final Territory... route) {
    setStart(start);
    for (final Territory t : route) {
      add(t);
    }
  }

  /**
   * Join the two routes. It must be the case that r1.end() equals r2.start()
   * or r1.end() == null and r1.start() equals r2
   *
   * @param r1 route 1
   * @param r2 route 2
   * @return a new Route starting at r1.start() going to r2.end() along r1,
   *         r2, or null if the routes can't be joined it the joining would
   *         form a loop
   */
  public static Route join(final Route r1, final Route r2) {
    if (r1 == null || r2 == null) {
      // throw new IllegalArgumentException("route cant be null r1:" + r1 + " r2:" + r2);
      return null;
    }
    if (r1.numberOfSteps() == 0) {
      if (!r1.getStart().equals(r2.getStart())) {
        throw new IllegalArgumentException("Cannot join, r1 doesnt end where r2 starts. r1:" + r1 + " r2:" + r2);
      }
    } else {
      if (!r1.getEnd().equals(r2.getStart())) {
        throw new IllegalArgumentException("Cannot join, r1 doesnt end where r2 starts. r1:" + r1 + " r2:" + r2);
      }
    }
    final Collection<Territory> c1 = new ArrayList<>(r1.m_steps);
    c1.add(r1.getStart());
    final Collection<Territory> c2 = new ArrayList<>(r2.m_steps);
    if (!CollectionUtils.intersection(c1, c2).isEmpty()) {
      return null;
    }
    final Route joined = new Route();
    joined.setStart(r1.getStart());
    for (final Territory t : r1.getSteps()) {
      joined.add(t);
    }
    for (final Territory t : r2.getSteps()) {
      joined.add(t);
    }
    return joined;
  }

  @Override
  public boolean equals(final Object o) {
    if (o == null) {
      return false;
    }
    final Route other = (Route) o;
    if (!(other.numberOfSteps() == this.numberOfSteps())) {
      return false;
    }
    return other.getStart().equals(this.getStart()) && other.getAllTerritories().equals(this.getAllTerritories());
  }

  @Override
  public int hashCode() {
    return Objects.hash(m_start, getSteps());
  }

  /**
   * Set the start of this route.
   *
   * @param t new start territory
   */
  public void setStart(final Territory t) {
    if (t == null) {
      throw new IllegalStateException("Null territory");
    }
    m_start = t;
  }

  /**
   * @return start territory for this route.
   */
  public Territory getStart() {
    return m_start;
  }

  /**
   * Determines if the route crosses water by checking if any of the
   * territories except the start and end are sea territories.
   *
   * @return whether the route encounters water other than at the start of the route.
   */
  public boolean crossesWater() {
    if (hasNoSteps()) {
      return false;
    }
    final boolean startLand = !m_start.isWater();
    final boolean overWater = anyMatch(Territory::isWater);

    // If we started on land, went over water, and ended on land, we cross water.
    return (startLand && overWater && !getEnd().isWater());
  }

  /**
   * Add the given territory to the end of the route.
   *
   * @param t referring territory
   */
  public void add(final Territory t) {
    if (t == null) {
      throw new IllegalStateException("Null territory");
    }
    if (t.equals(m_start) || m_steps.contains(t)) {
      throw new IllegalArgumentException("Loops not allowed in m_routes, route:" + this + " new territory:" + t);
    }
    m_steps.add(t);
  }

  /**
   * @param u unit that is moving on this route
   * @return the total cost of the route including modifications due to territoryEffects and territoryConnections.
   */
  public int getMovementCost(final Unit u) {
    // TODO implement me
    return m_steps.size();
  }

  /**
   * @return The number of steps in this route. Does not include start.
   */
  public int numberOfSteps() {
    return m_steps.size();
  }

  /**
   * @return The number of steps in this route. DOES include start.
   */
  public int numberOfStepsIncludingStart() {
    return this.getAllTerritories().size();
  }

  /**
   * @param i step number
   * @return territory we will be in after the i'th step for this route has
   *         been made.
   */
  public Territory getTerritoryAtStep(final int i) {
    return m_steps.get(i);
  }

  /**
   * Checking if any of the steps match the given Predicate.
   *
   * @param match referring match
   * @return whether any territories in this route match the given match (start territory is not tested).
   */
  public boolean anyMatch(final Predicate<Territory> match) {
    return m_steps.stream().anyMatch(match);
  }

  /**
   * @param match referring match
   * @return whether all territories in this route match the given match (start and end territories are not tested).
   */
  public boolean allMatchMiddleSteps(final Predicate<Territory> match, final boolean defaultWhenNoMiddleSteps) {
    final List<Territory> middle = getMiddleSteps();
    if (middle.isEmpty()) {
      return defaultWhenNoMiddleSteps;
    }
    for (final Territory t : middle) {
      if (!match.test(t)) {
        return false;
      }
    }
    return true;
  }

  /**
   * @param match referring match
   * @return all territories in this route match the given match (start territory is not tested).
   */
  public Collection<Territory> getMatches(final Predicate<Territory> match) {
    return CollectionUtils.getMatches(m_steps, match);
  }

  @Override
  public String toString() {
    final StringBuilder buf = new StringBuilder("Route:").append(m_start);
    for (final Territory t : getSteps()) {
      buf.append(" -> ");
      buf.append(t.getName());
    }
    return buf.toString();
  }

  public List<Territory> getAllTerritories() {
    final ArrayList<Territory> list = new ArrayList<>(m_steps);
    list.add(0, m_start);
    return list;
  }

  /**
   * @return collection of all territories in this route, without the start.
   */
  public List<Territory> getSteps() {
    if (numberOfSteps() > 0) {
      return new ArrayList<>(m_steps);
    }
    return EMPTY_TERRITORY_LIST;
  }

  /**
   * @return collection of all territories in this route without the start or the end.
   */
  public List<Territory> getMiddleSteps() {
    if (numberOfSteps() > 1) {
      return new ArrayList<>(m_steps).subList(0, numberOfSteps() - 1);
    }
    return EMPTY_TERRITORY_LIST;
  }

  /**
   * @return last territory in the route.
   */
  public Territory getEnd() {
    if (m_steps.isEmpty()) {
      return m_start;
    }
    return m_steps.get(m_steps.size() - 1);
  }

  @Override
  public Iterator<Territory> iterator() {
    return Collections.unmodifiableList(getAllTerritories()).iterator();
  }

  /**
   * @return whether this route has any steps.
   */
  public boolean hasSteps() {
    return !m_steps.isEmpty();
  }

  /**
   * @return whether this route has no steps.
   */
  public boolean hasNoSteps() {
    return !hasSteps();
  }

  /**
   * This means that there are 2 territories in the route: the start and the end (this is only 1 step).
   *
   * @return whether the route has 1 step
   */
  public boolean hasExactlyOneStep() {
    return this.m_steps.size() == 1;
  }

  /**
   * the territory before the end territory (this could be the start territory
   * in the case of 1 step).
   *
   * @return the territory before the end territory
   */
  public Territory getTerritoryBeforeEnd() {
    return (m_steps.size() <= 1) ? getStart() : getTerritoryAtStep(m_steps.size() - 2);
  }

  /**
   * This only checks if start is water and end is not water.
   *
   * @return whether this route is an unloading route (unloading from transport to land)
   */
  public boolean isUnload() {
    if (hasNoSteps()) {
      return false;
    }
    // we should not check if there is only 1 step, because otherwise movement validation will let users move their
    // tanks over water, so long as they end on land
    return getStart().isWater() && !getEnd().isWater();
  }

  /**
   * This only checks if start is not water, and end is water.
   *
   * @return whether this route is a loading route (loading from land into a transport @ sea)
   */
  public boolean isLoad() {
    return !hasNoSteps() && !getStart().isWater() && getEnd().isWater();
  }

  /**
   * @return whether this route has more then one step.
   */
  public boolean hasMoreThenOneStep() {
    return m_steps.size() > 1;
  }

  /**
   * @return whether there are territories before the end where the territory is owned by null and is not sea.
   */
  public boolean hasNeutralBeforeEnd() {
    for (final Territory current : getMiddleSteps()) {
      // neutral is owned by null and is not sea
      if (!current.isWater() && current.getOwner().equals(PlayerID.NULL_PLAYERID)) {
        return true;
      }
    }
    return false;
  }

  /**
   * @return whether there is some water in the route including start and end.
   */
  public boolean hasWater() {
    return getStart().isWater() || getSteps().stream().anyMatch(Matches.territoryIsWater());
  }

  /**
   * @return whether there is some land in the route including start and end.
   */
  public boolean hasLand() {
    return !getStart().isWater()
        || getAllTerritories().isEmpty()
        || !getAllTerritories().stream().allMatch(Matches.territoryIsWater());
  }

  public int getLargestMovementCost(final Collection<Unit> units) {
    int largestCost = 0;
    for (final Unit unit : units) {
      largestCost = Math.max(largestCost, getMovementCost(unit));
    }
    return largestCost;
  }

  public int getMovementLeft(final Unit unit) {
    final int movementLeft = ((TripleAUnit) unit).getMovementLeft() - getMovementCost(unit);
    return movementLeft;
  }

  public static Change getFuelChanges(final Collection<Unit> units, final Route route, final PlayerID player,
      final GameData data) {
    final CompositeChange changes = new CompositeChange();
    final Tuple<ResourceCollection, Set<Unit>> tuple =
        Route.getFuelCostsAndUnitsChargedFlatFuelCost(units, route, player, data, false);
    if (!tuple.getFirst().isEmpty()) {
      changes.add(ChangeFactory.removeResourceCollection(player, tuple.getFirst()));
      for (final Unit unit : tuple.getSecond()) {
        changes.add(ChangeFactory.unitPropertyChange(unit, Boolean.TRUE, TripleAUnit.CHARGED_FLAT_FUEL_COST));
      }
    }
    return changes;
  }

  public static ResourceCollection getMovementFuelCostCharge(final Collection<Unit> units, final Route route,
      final PlayerID player, final GameData data) {
    return Route.getFuelCostsAndUnitsChargedFlatFuelCost(units, route, player, data, false).getFirst();
  }

  /**
   * Calculates how much fuel each player needs to scramble the specified units. ONLY SUPPORTS 1 territory distance
   * scrambles properly as otherwise a route needs to be calculated.
   */
  public static Map<PlayerID, ResourceCollection> getScrambleFuelCostCharge(final Collection<Unit> units,
      final Territory from, final Territory to, final GameData data) {
    final Map<PlayerID, ResourceCollection> map = new HashMap<>();
    final Route toRoute = new Route(from, to);
    final Route returnRoute = new Route(to, from);
    for (final Unit unit : units) {
      final PlayerID player = unit.getOwner();
      final ResourceCollection cost = new ResourceCollection(data);
      cost.add(getMovementFuelCostCharge(Collections.singleton(unit), toRoute, player, data));
      cost.add(getFuelCostsAndUnitsChargedFlatFuelCost(
          Collections.singleton(unit), returnRoute, player, data, true).getFirst());
      if (map.containsKey(player)) {
        map.get(player).add(cost);
      } else if (!cost.isEmpty()) {
        map.put(player, cost);
      }
    }
    return map;
  }

  /**
   * Find fuel costs and which units are to be charged flat fuel costs. Ignores dependent units
   * and if non-combat then ignores air units moving with carrier.
   */
  private static Tuple<ResourceCollection, Set<Unit>> getFuelCostsAndUnitsChargedFlatFuelCost(
      final Collection<Unit> units, final Route route, final PlayerID player, final GameData data,
      final boolean ignoreFlat) {

    if (!Properties.getUseFuelCost(data)) {
      return Tuple.of(new ResourceCollection(data), new HashSet<>());
    }

    final Set<Unit> unitsToChargeFuelCosts = new HashSet<>(units);

    // If non-combat then remove air units moving with a carrier
    if (GameStepPropertiesHelper.isNonCombatMove(data, true)) {

      // Add allied air first so that the carriers take them into account before owned air
      final List<Unit> canLandOnCarrierUnits = CollectionUtils.getMatches(units,
          Matches.unitIsOwnedBy(player).negate()
              .and(Matches.isUnitAllied(player, data))
              .and(Matches.unitCanLandOnCarrier()));
      canLandOnCarrierUnits.addAll(CollectionUtils.getMatches(units,
          Matches.unitIsOwnedBy(player).and(Matches.unitCanLandOnCarrier())));
      unitsToChargeFuelCosts.removeAll(AirMovementValidator.whatAirCanLandOnTheseCarriers(
          CollectionUtils.getMatches(units, Matches.unitIsCarrier()),
          canLandOnCarrierUnits, route.getStart()));
    }

    // Remove dependent units
    unitsToChargeFuelCosts.removeAll(CollectionUtils.getMatches(units,
        Matches.unitIsBeingTransportedByOrIsDependentOfSomeUnitInThisList(units, player, data, true)));

    // Find fuel cost and whether to charge flat fuel cost
    final ResourceCollection movementCharge = new ResourceCollection(data);
    final Set<Unit> unitsChargedFlatFuelCost = new HashSet<>();
    for (final Unit unit : unitsToChargeFuelCosts) {
      final Tuple<ResourceCollection, Boolean> tuple =
          route.getFuelCostsAndIfChargedFlatFuelCost(unit, data, ignoreFlat);
      movementCharge.add(tuple.getFirst());
      if (tuple.getSecond()) {
        unitsChargedFlatFuelCost.add(unit);
      }
    }
    return Tuple.of(movementCharge, unitsChargedFlatFuelCost);
  }

  private Tuple<ResourceCollection, Boolean> getFuelCostsAndIfChargedFlatFuelCost(final Unit unit,
      final GameData data, final boolean ignoreFlat) {
    final ResourceCollection resources = new ResourceCollection(data);
    boolean chargedFlatFuelCost = false;
    if (Matches.unitIsBeingTransported().test(unit)) {
      return Tuple.of(resources, chargedFlatFuelCost);
    }
    final UnitAttachment ua = UnitAttachment.get(unit.getType());
    resources.add(ua.getFuelCost());
    resources.multiply(getMovementCost(unit));
    if (!ignoreFlat && Matches.unitHasNotBeenChargedFlatFuelCost().test(unit)) {
      resources.add(ua.getFuelFlatCost());
      chargedFlatFuelCost = true;
    }
    return Tuple.of(resources, chargedFlatFuelCost);
  }

  public static Route create(final List<Route> routes) {
    Route route = new Route();
    for (final Route r : routes) {
      route = Route.join(route, r);
    }
    return route;
  }
}
