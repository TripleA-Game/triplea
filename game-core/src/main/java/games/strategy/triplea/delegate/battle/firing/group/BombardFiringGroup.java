package games.strategy.triplea.delegate.battle.firing.group;

import games.strategy.engine.data.Unit;
import java.util.Collection;
import java.util.List;
import lombok.Builder;
import lombok.NonNull;

@Builder
public class BombardFiringGroup {

  private @NonNull final Collection<Unit> firingUnits;
  private @NonNull final Collection<Unit> attackableUnits;
  private @NonNull final Boolean defending;

  public List<FiringGroup> getFiringGroupsWithSuicideFirst() {
    return RegularFiringGroup.getFiringGroupsWorker(defending, firingUnits, attackableUnits);
  }
}
