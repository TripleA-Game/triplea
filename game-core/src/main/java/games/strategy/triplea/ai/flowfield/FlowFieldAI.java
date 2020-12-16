package games.strategy.triplea.ai.flowfield;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.framework.startup.ui.PlayerType;
import games.strategy.triplea.ai.AbstractAi;
import games.strategy.triplea.delegate.remote.IAbstractPlaceDelegate;
import games.strategy.triplea.delegate.remote.IMoveDelegate;
import games.strategy.triplea.delegate.remote.IPurchaseDelegate;
import games.strategy.triplea.delegate.remote.ITechDelegate;

public class FlowFieldAI extends AbstractAi {
  public FlowFieldAI(final String name) {
    super(name);
  }

  @Override
  public PlayerType getPlayerType() {
    return null;
  }

  @Override
  protected void purchase(
      final boolean purchaseForBid,
      final int pusToSpend,
      final IPurchaseDelegate purchaseDelegate,
      final GameData data,
      final GamePlayer player) {}

  @Override
  protected void tech(
      final ITechDelegate techDelegate, final GameData data, final GamePlayer player) {}

  @Override
  protected void move(
      final boolean nonCombat,
      final IMoveDelegate moveDel,
      final GameData data,
      final GamePlayer player) {}

  @Override
  protected void place(
      final boolean placeForBid,
      final IAbstractPlaceDelegate placeDelegate,
      final GameData data,
      final GamePlayer player) {}
}
