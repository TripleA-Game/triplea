package games.strategy.triplea.delegate.move.validation;

import static games.strategy.triplea.delegate.move.validation.UnitStackingLimitFilter.PLACEMENT_LIMIT;
import static games.strategy.triplea.delegate.move.validation.UnitStackingLimitFilter.filterUnits;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.triplea.java.collections.CollectionUtils.countMatches;

import games.strategy.engine.data.Unit;
import games.strategy.triplea.delegate.AbstractDelegateTestCase;
import games.strategy.triplea.delegate.Matches;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class UnitStackingLimitTest extends AbstractDelegateTestCase {

  @Test
  void testUnitAttachmentStackingLimit() {
    // we can place 4
    List<Unit> fourTanks = armour.create(4, british);
    assertThat(filterUnits(fourTanks, PLACEMENT_LIMIT, british, uk), is(fourTanks));

    // the same four tanks are returned, even if we pass some existing units.
    assertThat(
        filterUnits(fourTanks, PLACEMENT_LIMIT, british, uk, infantry.create(2, british)),
        is(fourTanks));
    // and only 2 are returned if we pass 2 existing tanks
    assertThat(
        filterUnits(fourTanks, PLACEMENT_LIMIT, british, uk, armour.create(2, british)),
        hasSize(2));

    // we can't place 5 per the unit attachment's placementLimit
    List<Unit> fiveTanks = armour.create(5, british);
    assertThat(filterUnits(fiveTanks, PLACEMENT_LIMIT, british, uk), hasSize(4));

    // only 2 can be placed if 2 are already there
    uk.getUnitCollection().addAll(armour.create(2, british));
    assertThat(filterUnits(fourTanks, PLACEMENT_LIMIT, british, uk), hasSize(2));

    // but we can include other units that don't have stacking limits in the list
    // note: still with the two tanks already in the uk
    List<Unit> twoInfantryAndFourTanks = infantry.create(2, british);
    twoInfantryAndFourTanks.addAll(fourTanks);
    assertThat(twoInfantryAndFourTanks, hasSize(6));
    var result = filterUnits(twoInfantryAndFourTanks, PLACEMENT_LIMIT, british, uk);
    assertThat(result, hasSize(4));
    assertThat(countMatches(result, Matches.unitIsOfType(infantry)), is(2));
    assertThat(countMatches(result, Matches.unitIsOfType(armour)), is(2));
  }

  @Test
  void testClassicAaStackingLimit() {
    // No more aa guns to be placed in UK.
    List<Unit> units = aaGun.create(2, british);
    assertThat(filterUnits(units, PLACEMENT_LIMIT, british, uk), empty());

    // Remove the aa gun in UK, now one can be placed.
    uk.getUnitCollection().removeIf(Matches.unitIsOfType(aaGun));
    assertThat(filterUnits(units, PLACEMENT_LIMIT, british, uk), hasSize(1));
  }

  @Test
  void testPlayerAttachmentStackingLimit() {
    // we can place 3 battleships
    List<Unit> units = battleship.create(3, british);
    assertThat(filterUnits(units, PLACEMENT_LIMIT, british, uk), is(units));

    // but not 4
    units = battleship.create(4, british);
    assertThat(filterUnits(units, PLACEMENT_LIMIT, british, uk), hasSize(3));

    // we can also place 2 battleships and a carrier
    units = battleship.create(2, british);
    units.addAll(carrier.create(1, british));
    assertThat(filterUnits(units, PLACEMENT_LIMIT, british, uk), is(units));

    // but not 2 battleships and 2 carriers
    units.addAll(carrier.create(1, british));
    var expected = units.subList(0, 3);
    assertThat(expected, hasSize(3));
    assertThat(filterUnits(units, PLACEMENT_LIMIT, british, uk), is(expected));

    // and that the filtered units returned are in order
    Collections.shuffle(units);
    expected = units.subList(0, 3);
    assertThat(expected, hasSize(3));
    assertThat(filterUnits(units, PLACEMENT_LIMIT, british, uk), is(expected));
  }
}
