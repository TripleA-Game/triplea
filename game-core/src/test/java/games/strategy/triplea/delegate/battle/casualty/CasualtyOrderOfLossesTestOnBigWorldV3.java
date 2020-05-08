package games.strategy.triplea.delegate.battle.casualty;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;

import games.strategy.engine.data.Unit;
import games.strategy.triplea.delegate.ImprovedArtillerySupportAdvance;
import games.strategy.triplea.delegate.battle.UnitBattleComparator.CombatModifiers;
import games.strategy.triplea.xml.TestDataBigWorld1942V3;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@SuppressWarnings("SameParameterValue")
class CasualtyOrderOfLossesTestOnBigWorldV3 {

  private TestDataBigWorld1942V3 testData = new TestDataBigWorld1942V3();

  @BeforeEach
  void clearCache() {
    CasualtyOrderOfLosses.clearOolCache();
  }

  @Test
  void improvedArtillery() {
    testData.addTech(new ImprovedArtillerySupportAdvance(testData.gameData));

    final Collection<Unit> attackingUnits = new ArrayList<>();
    attackingUnits.addAll(testData.tank(1));
    attackingUnits.addAll(testData.artillery(1));
    attackingUnits.addAll(testData.marine(1));
    attackingUnits.addAll(testData.marine(1));

    final List<Unit> result =
        CasualtyOrderOfLosses.sortUnitsForCasualtiesWithSupport(amphibAssault(attackingUnits));

    assertThat(result, hasSize(4));
    assertThat(result.get(0).getType(), is(testData.artillery));
    assertThat(result.get(1).getType(), is(testData.tank));
    assertThat(result.get(2).getType(), is(testData.marine));
    assertThat(result.get(3).getType(), is(testData.marine));
  }

  private CasualtyOrderOfLosses.Parameters amphibAssault(final Collection<Unit> amphibUnits) {
    return CasualtyOrderOfLosses.Parameters.builder()
        .targetsToPickFrom(amphibUnits)
        .combatModifiers(
            CombatModifiers.builder()
                .defending(false)
                .amphibious(true)
                .territoryEffects(List.of())
                .build())
        .player(testData.british)
        .enemyUnits(List.of())
        .amphibiousLandAttackers(amphibUnits)
        .battlesite(testData.france)
        .costs(testData.costMap)
        .data(testData.gameData)
        .build();
  }

  @Test
  void amphibAssaultWithoutImprovedArtillery() {
    final Collection<Unit> attackingUnits = new ArrayList<>();
    attackingUnits.addAll(testData.tank(1));
    attackingUnits.addAll(testData.artillery(1));
    attackingUnits.addAll(testData.marine(1));
    attackingUnits.addAll(testData.marine(1));

    final List<Unit> result =
        CasualtyOrderOfLosses.sortUnitsForCasualtiesWithSupport(amphibAssault(attackingUnits));

    assertThat(result, hasSize(4));
    assertThat(result.get(0).getType(), is(testData.artillery));
    assertThat(result.get(1).getType(), is(testData.tank));
    assertThat(result.get(2).getType(), is(testData.marine));
    assertThat(result.get(3).getType(), is(testData.marine)); // << bug, should be tank
  }

  @Test
  @DisplayName("Amphib assaulting marine should be taken last when it is strongest unit")
  void amphibAssaultIsTakenIntoAccount() {
    final Collection<Unit> attackingUnits = new ArrayList<>();
    attackingUnits.addAll(testData.infantry(1));
    attackingUnits.addAll(testData.marine(1));
    attackingUnits.addAll(testData.artillery(1));

    final List<Unit> result =
        CasualtyOrderOfLosses.sortUnitsForCasualtiesWithSupport(amphibAssault(attackingUnits));

    assertThat(result, hasSize(3));
    assertThat(result.get(0).getType(), is(testData.infantry));
    assertThat(result.get(1).getType(), is(testData.artillery));
    assertThat(
        "The marine is attacking at a 3 without support, it is the strongest land unit",
        result.get(2).getType(),
        is(testData.marine));
  }

  @Test
  @DisplayName("Tie between amphib marine and fighter goes to fighter")
  void favorStrongestAttackThenStrongestTotalPower() {
    final Collection<Unit> attackingUnits = new ArrayList<>();
    attackingUnits.addAll(testData.marine(1));
    attackingUnits.addAll(testData.fighter(1));

    final List<Unit> result =
        CasualtyOrderOfLosses.sortUnitsForCasualtiesWithSupport(amphibAssault(attackingUnits));

    assertThat(result, hasSize(2));
    assertThat(
        "marine is attacking at a 3, defends at 2, "
            + "ties with fighter but the weaker defense means it is chosen first",
        result.get(0).getType(),
        is(testData.marine));
    assertThat(
        "fighter ties with marine, attacking at 3, but fighter has better defense power of 4",
        result.get(1).getType(),
        is(testData.fighter));
  }

  @Test
  void strongestPowerOrdering() {
    final Collection<Unit> attackingUnits = new ArrayList<>();
    attackingUnits.addAll(testData.infantry(1)); // attacks at 1
    attackingUnits.addAll(testData.fighter(1)); // attacks at 3
    attackingUnits.addAll(testData.bomber(1)); // attacks at 4

    final List<Unit> result =
        CasualtyOrderOfLosses.sortUnitsForCasualtiesWithSupport(amphibAssault(attackingUnits));

    assertThat(result, hasSize(3));
    assertThat(result.get(0).getType(), is(testData.infantry));
    assertThat(result.get(1).getType(), is(testData.fighter));
    assertThat(result.get(2).getType(), is(testData.bomber));
  }

  @Test
  void infantryAndArtillery() {
    final Collection<Unit> attackingUnits = new ArrayList<>();
    attackingUnits.addAll(testData.infantry(1)); // attacks at 2
    attackingUnits.addAll(testData.artillery(1)); // attacks at 2

    final List<Unit> result =
        CasualtyOrderOfLosses.sortUnitsForCasualtiesWithSupport(amphibAssault(attackingUnits));

    assertThat(result, hasSize(2));
    assertThat(result.get(0).getType(), is(testData.infantry));
    assertThat(
        "Artillery has the better total power", result.get(1).getType(), is(testData.artillery));
  }
}
