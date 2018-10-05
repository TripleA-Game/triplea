package games.strategy.triplea.delegate;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;

class PlaceExtendedDelegateState implements Serializable {
  private static final long serialVersionUID = -4926754941623641735L;

  Serializable superState;
  // add other variables here:
  @SuppressWarnings("checkstyle:MemberName") // rename upon next incompatible release
  public Map<Territory, Collection<Unit>> m_produced;
  @SuppressWarnings("checkstyle:MemberName") // rename upon next incompatible release
  public List<UndoablePlacement> m_placements;
}
