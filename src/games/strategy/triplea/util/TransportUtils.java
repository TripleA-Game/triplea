package games.strategy.triplea.util;

import games.strategy.engine.data.Route;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.TransportTracker;
import games.strategy.util.Match;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


public class TransportUtils {

  /**
   * Returns a map of unit -> transport (null if no mapping can be done either because there is not sufficient transport
   * capacity or because a unit is not with its transport)
   */
  public static Map<Unit, Unit> mapTransports(final Route route, final Collection<Unit> units,
      final Collection<Unit> transportsToLoad) {
    if (route.isLoad()) {
      return mapTransportsToLoad(units, transportsToLoad);
    }
    if (route.isUnload()) {
      return mapTransportsAlreadyLoaded(units, route.getStart().getUnits().getUnits());
    }
    return mapTransportsAlreadyLoaded(units, units);
  }

  /**
   * Returns a map of unit -> transport. Unit must already be loaded in the transport. If no units are loaded in the
   * transports then an empty Map will be returned.
   */
  private static Map<Unit, Unit> mapTransportsAlreadyLoaded(final Collection<Unit> units,
      final Collection<Unit> transports) {
    final Collection<Unit> canBeTransported = Match.getMatches(units, Matches.UnitCanBeTransported);
    final Collection<Unit> canTransport = Match.getMatches(transports, Matches.UnitCanTransport);
    final Map<Unit, Unit> mapping = new HashMap<Unit, Unit>();
    final Iterator<Unit> land = canBeTransported.iterator();
    while (land.hasNext()) {
      final Unit currentTransported = land.next();
      final Unit transport = TransportTracker.transportedBy(currentTransported);

      // already being transported, make sure it is in transports
      if (transport == null) {
        continue;
      }
      if (!canTransport.contains(transport)) {
        continue;
      }
      mapping.put(currentTransported, transport);
    }
    return mapping;
  }

  /**
   * Returns a map of unit -> transport. Tries to find transports to load all units. If it can't succeed returns an
   * empty Map.
   */
  public static Map<Unit, Unit> mapTransportsToLoad(final Collection<Unit> units, final Collection<Unit> transports) {

    // Sort units with the highest transport cost first
    final Comparator<Unit> transportCostComparator = new Comparator<Unit>() {
      @Override
      public int compare(final Unit o1, final Unit o2) {
        final int cost1 = UnitAttachment.get((o1).getUnitType()).getTransportCost();
        final int cost2 = UnitAttachment.get((o2).getUnitType()).getTransportCost();
        return cost2 - cost1;
      }
    };
    final List<Unit> canBeTransported = Match.getMatches(units, Matches.UnitCanBeTransported);
    Collections.sort(canBeTransported, transportCostComparator);

    // Sort transports with the lowest capacity first
    final Comparator<Unit> transportCapacityComparator = new Comparator<Unit>() {
      @Override
      public int compare(final Unit o1, final Unit o2) {
        final int capacityLeft1 = TransportTracker.getAvailableCapacity(o1);
        final int capacityLeft2 = TransportTracker.getAvailableCapacity(o1);
        return capacityLeft1 - capacityLeft2;
      }
    };
    final List<Unit> canTransport = Match.getMatches(transports, Matches.UnitCanTransport);
    Collections.sort(canTransport, transportCapacityComparator);

    // Add max units to transports
    final Map<Unit, Unit> mapping = new HashMap<Unit, Unit>();
    for (final Unit transport : canTransport) {
      int capacity = TransportTracker.getAvailableCapacity(transport);
      for (final Iterator<Unit> it = canBeTransported.iterator(); it.hasNext();) {
        if (capacity <= 0) {
          break;
        }
        final Unit unit = it.next();
        final int cost = UnitAttachment.get((unit).getType()).getTransportCost();
        if (capacity >= cost) {
          capacity -= cost;
          mapping.put(unit, transport);
          it.remove();
        }
      }
    }
    return mapping;
  }

  private static Comparator<Unit> transportsThatPreviouslyUnloadedComeLast() {
    return new Comparator<Unit>() {
      @Override
      public int compare(final Unit t1, final Unit t2) {
        if (t1 == t2 || t1.equals(t2)) {
          return 0;
        }
        final boolean t1previous = TransportTracker.hasTransportUnloadedInPreviousPhase(t1);
        final boolean t2previous = TransportTracker.hasTransportUnloadedInPreviousPhase(t2);
        if (t1previous == t2previous) {
          return 0;
        }
        if (t1previous == false) {
          return -1;
        }
        return 1;
      }
    };
  }

  public static List<Unit> findUnitsToLoadOnAirTransports(final Collection<Unit> units,
      final Collection<Unit> transports) {
    final Collection<Unit> airTransports = Match.getMatches(transports, Matches.UnitIsAirTransport);

    final Comparator<Unit> c = new Comparator<Unit>() {
      @Override
      public int compare(final Unit o1, final Unit o2) {
        final int cost1 = UnitAttachment.get((o1).getUnitType()).getTransportCost();
        final int cost2 = UnitAttachment.get((o2).getUnitType()).getTransportCost();
        // descending transportCost
        return cost2 - cost1;
      }
    };
    Collections.sort((List<Unit>) units, c);

    // Define the max of all units that could be loaded
    final List<Unit> totalLoad = new ArrayList<Unit>();

    // Get a list of the unit categories
    final Collection<UnitCategory> unitTypes = UnitSeperator.categorize(units, null, false, true);
    final Collection<UnitCategory> transportTypes = UnitSeperator.categorize(airTransports, null, false, false);
    for (final UnitCategory unitType : unitTypes) {
      final int transportCost = unitType.getTransportCost();
      for (final UnitCategory transportType : transportTypes) {
        final int transportCapacity = UnitAttachment.get(transportType.getType()).getTransportCapacity();
        if (transportCost > 0 && transportCapacity >= transportCost) {
          final int transportCount = Match.countMatches(airTransports, Matches.unitIsOfType(transportType.getType()));
          final int ttlTransportCapacity = transportCount * (int) Math.floor(transportCapacity / transportCost);
          totalLoad.addAll(Match.getNMatches(units, ttlTransportCapacity, Matches.unitIsOfType(unitType.getType())));
        }
      }
    }
    return totalLoad;
  }

  public static int getTransportCost(final Collection<Unit> units) {
    if (units == null) {
      return 0;
    }
    int cost = 0;
    final Iterator<Unit> iter = units.iterator();
    while (iter.hasNext()) {
      final Unit item = iter.next();
      cost += UnitAttachment.get(item.getType()).getTransportCost();
    }
    return cost;
  }

}
