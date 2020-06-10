package games.strategy.triplea.delegate.battle.steps.fire.aa;

import static games.strategy.triplea.delegate.battle.FakeBattleState.givenBattleStateBuilder;
import static games.strategy.triplea.delegate.battle.steps.BattleStepsTest.givenUnitWithTypeAa;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import games.strategy.engine.data.Unit;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.delegate.ExecutionStack;
import games.strategy.triplea.delegate.battle.BattleActions;
import games.strategy.triplea.delegate.battle.BattleState;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OffensiveAaFireTest {

  @Mock ExecutionStack executionStack;
  @Mock IDelegateBridge delegateBridge;
  @Mock BattleActions battleActions;

  @ParameterizedTest(name = "[{index}] {0} is {2}")
  @MethodSource
  void testWhatIsValid(
      final String displayName, final BattleState battleState, final boolean expected) {
    final OffensiveAaFire offensiveAaFire = new OffensiveAaFire(battleState, battleActions);
    assertThat(offensiveAaFire.valid(), is(expected));
    if (expected) {
      assertThat(offensiveAaFire.getNames(), hasSize(3));
    } else {
      assertThat(offensiveAaFire.getNames(), hasSize(0));
    }
  }

  static List<Arguments> testWhatIsValid() {
    return List.of(
        Arguments.of(
            "No Offensive Aa", givenBattleStateBuilder().offensiveAa(List.of()).build(), false),
        Arguments.of(
            "Some Offensive Aa",
            givenBattleStateBuilder().offensiveAa(List.of(givenUnitWithTypeAa())).build(),
            true));
  }

  @Test
  void testFiringAaGuns() {
    final OffensiveAaFire offensiveAaFire =
        new OffensiveAaFire(
            givenBattleStateBuilder().offensiveAa(List.of(mock(Unit.class))).build(),
            battleActions);

    offensiveAaFire.execute(executionStack, delegateBridge);

    verify(battleActions).fireOffensiveAaGuns();
  }
}
