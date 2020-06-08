package games.strategy.triplea.delegate.battle.steps.fire.air;

import static games.strategy.triplea.delegate.battle.BattleStepStrings.AIR_ATTACK_NON_SUBS;

import games.strategy.triplea.delegate.battle.BattleState;
import java.util.List;

/** Air can not attack subs unless a destroyer is present */
public class AirAttackVsNonSubsStep extends AirVsNonSubsStep {
  public AirAttackVsNonSubsStep(final BattleState battleState) {
    super(battleState);
  }

  @Override
  public List<String> getNames() {
    return valid() ? List.of(AIR_ATTACK_NON_SUBS) : List.of();
  }

  @Override
  public boolean valid() {
    return airWillMissSubs(battleState.getAttackingUnits(), battleState.getDefendingUnits());
  }
}
