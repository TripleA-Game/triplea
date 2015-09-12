package games.strategy.triplea.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.TripleAUnit;
import games.strategy.triplea.attatchments.UnitAttachment;
import games.strategy.triplea.delegate.Matches;

/**
 * Separates a group of units into distinct categories.
 */
public class UnitSeperator {
  private UnitSeperator() {}

  public static Set<UnitCategory> categorize(final Collection<Unit> units) {
    return categorize(units, null, false, false);
  }


  private static Set<UnitCategory> categorize(final boolean sort, final Collection<Unit> units,
      final Map<Unit, Collection<Unit>> dependent, final boolean categorizeMovement,
      final boolean categorizeTransportCost) {
    return categorize(units, dependent, categorizeMovement, categorizeTransportCost, sort);
  }

  /**
   * Break the units into discrete categories.
   * Do this based on unit owner, and optionally dependent units and movement
   *
   * @param dependent - can be null
   * @param categorizeMovement - whether to categorize by movement
   * @param - sort - if true then sort the categories in UnitCategory order
   *        - if false, then leave categories in original order (based on units)
   * @return a Collection of UnitCategories
   */
  public static Set<UnitCategory> categorize(final Collection<Unit> units, final Map<Unit, Collection<Unit>> dependent,
      final boolean categorizeMovement, final boolean categorizeTransportCost, final boolean sort) {
    // somewhat odd, but we map UnitCategory->UnitCategory,
    // key and value are the same
    // we do this to take advanatge of .equals() on objects that
    // are equal in a special way
    HashMap<UnitCategory, UnitCategory> categories;
    if (sort) {
      categories = new HashMap<UnitCategory, UnitCategory>();
    } else {
      categories = new LinkedHashMap<UnitCategory, UnitCategory>();
    }
    for (final Unit current : units) {
      int unitMovement = -1;
      if (categorizeMovement) {
        unitMovement = TripleAUnit.get(current).getMovementLeft();
      }
      int unitTransportCost = -1;
      if (categorizeTransportCost) {
        unitTransportCost = UnitAttachment.get((current).getUnitType()).getTransportCost();
      }
      Collection<Unit> currentDependents = null;
      if (dependent != null) {
        currentDependents = dependent.get(current);
      }
      final boolean disabled = Matches.UnitIsDisabled.match(current);
      final UnitCategory entry = new UnitCategory(current, currentDependents, unitMovement, current.getHits(),
          TripleAUnit.get(current).getUnitDamage(), disabled, unitTransportCost);
      // we test to see if we have the key using equals, then since
      // key maps to key, we retrieve it to add the unit to the correct
      // category
      if (categories.containsKey(entry)) {
        final UnitCategory stored = categories.get(entry);
        stored.addUnit(current);
      } else {
        categories.put(entry, entry);
      }
    }
    if (sort) {
      return new TreeSet<UnitCategory>(categories.keySet());
    } else {
      return new LinkedHashSet<UnitCategory>(categories.keySet());
    }
  }

  /**
   * Legacy interface.
   * Break the units into discrete categories.
   * Do this based on unit owner, and optionally dependent units and movement
   *
   * @param dependent - can be null
   * @param categorizeMovement - whether to categorize by movement
   * @return a Collection of UnitCategories
   */
  public static Set<UnitCategory> categorize(final Collection<Unit> units, final Map<Unit, Collection<Unit>> dependent,
      final boolean categorizeMovement, final boolean categorizeTransportCost) {
    // sort by default
    return categorize(units, dependent, categorizeMovement, categorizeTransportCost, true);
  }

  public static Set<UnitCategory> categorize(final Map<Unit, Collection<Unit>> dependent, final Collection<Unit> units,
      final boolean categorizeMovement, final boolean categorizeTransportCost) {
    // sort by default
    return categorize(true, units, dependent, categorizeMovement, categorizeTransportCost);
  }
}
