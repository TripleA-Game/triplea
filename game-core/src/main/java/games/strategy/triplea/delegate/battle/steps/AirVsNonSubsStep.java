package games.strategy.triplea.delegate.battle.steps;

import games.strategy.engine.data.Unit;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.delegate.ExecutionStack;
import games.strategy.triplea.delegate.IExecutable;
import games.strategy.triplea.delegate.Matches;
import java.util.Collection;
import java.util.List;

/**
 * Air can not attack subs unless a destroyer is present
 *
 * <p>This step only occurs during naming so PRE_ROUND and IN_ROUND are the same
 */
public abstract class AirVsNonSubsStep extends BattleStep {

  public AirVsNonSubsStep(final StepParameters parameters) {
    super(parameters);
  }

  @Override
  public List<IExecutable> getStepExecutables() {
    return List.of();
  }

  @Override
  protected void execute(final ExecutionStack stack, final IDelegateBridge bridge) {}

  protected boolean airWillMissSubs(
      final Collection<Unit> firingUnits, final Collection<Unit> firedAtUnits) {
    return firingUnits.stream().anyMatch(Matches.unitIsAir())
        && firingUnits.stream().noneMatch(Matches.unitIsDestroyer())
        && firedAtUnits.stream().anyMatch(Matches.unitCanNotBeTargetedByAll());
  }
}
