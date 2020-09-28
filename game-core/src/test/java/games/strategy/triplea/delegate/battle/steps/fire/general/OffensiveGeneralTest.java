package games.strategy.triplea.delegate.battle.steps.fire.general;

import static games.strategy.triplea.delegate.battle.FakeBattleState.givenBattleStateBuilder;
import static games.strategy.triplea.delegate.battle.steps.BattleStepsTest.givenAnyUnit;
import static games.strategy.triplea.delegate.battle.steps.BattleStepsTest.givenUnitFirstStrike;
import static games.strategy.triplea.delegate.battle.steps.MockGameData.givenGameData;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import games.strategy.engine.data.Unit;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.battle.BattleActions;
import games.strategy.triplea.delegate.battle.BattleState;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OffensiveGeneralTest {

  @Mock BattleActions battleActions;

  @Nested
  class GetNames {
    @Test
    void hasNamesIfStandardUnitAvailable() {
      final BattleState battleState =
          givenBattleStateBuilder()
              .gameData(givenGameData().withAlliedAirIndependent(true).build())
              .attackingUnits(List.of(givenAnyUnit()))
              .defendingUnits(List.of(givenAnyUnit()))
              .build();
      final OffensiveGeneral offensiveGeneral = new OffensiveGeneral(battleState, battleActions);
      assertThat(offensiveGeneral.getNames(), hasSize(3));
    }

    @Test
    void hasNoNamesIfStandardUnitIsNotAvailable() {
      final BattleState battleState =
          givenBattleStateBuilder()
              .gameData(givenGameData().withAlliedAirIndependent(true).build())
              .attackingUnits(List.of(givenUnitFirstStrike()))
              .defendingUnits(List.of(givenAnyUnit()))
              .build();
      final OffensiveGeneral offensiveGeneral = new OffensiveGeneral(battleState, battleActions);
      assertThat(offensiveGeneral.getNames(), is(empty()));
    }
  }

  @Test
  void firingFilterIgnoresFirstStrikeUnits() {
    final List<Unit> units =
        List.of(
                givenAnyUnit(),
                givenUnitFirstStrike(),
                givenAnyUnit(),
                givenUnitFirstStrike(),
                givenAnyUnit())
            .stream()
            .filter(OffensiveGeneral.FIRING_UNIT_PREDICATE)
            .collect(Collectors.toList());

    assertThat(units, hasSize(3));
    assertThat(
        "There should be no first strike units",
        units.stream().allMatch(Matches.unitIsFirstStrike().negate()),
        is(true));
  }
}
