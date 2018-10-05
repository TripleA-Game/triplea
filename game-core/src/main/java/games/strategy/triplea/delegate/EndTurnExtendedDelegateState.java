package games.strategy.triplea.delegate;

import java.io.Serializable;

class EndTurnExtendedDelegateState implements Serializable {
  private static final long serialVersionUID = -3939461840835898284L;

  Serializable superState;
  // add other variables here:
  @SuppressWarnings("checkstyle:MemberName") // rename upon next incompatible release
  public boolean m_needToInitialize;
  @SuppressWarnings("checkstyle:MemberName") // rename upon next incompatible release
  public boolean m_hasPostedTurnSummary;
}
