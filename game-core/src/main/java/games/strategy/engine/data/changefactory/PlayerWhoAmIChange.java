package games.strategy.engine.data.changefactory;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerId;

class PlayerWhoAmIChange extends Change {
  private static final long serialVersionUID = -1486914230174337300L;

  private final String startWhoAmI;
  private final String endWhoAmI;
  private final String playerName;

  PlayerWhoAmIChange(final String newWhoAmI, final PlayerId player) {
    startWhoAmI = player.getWhoAmI();
    endWhoAmI = newWhoAmI;
    playerName = player.getName();
  }

  PlayerWhoAmIChange(final String startWhoAmI, final String endWhoAmI, final String playerName) {
    this.startWhoAmI = startWhoAmI;
    this.endWhoAmI = endWhoAmI;
    this.playerName = playerName;
  }

  @Override
  protected void perform(final GameData data) {
    final PlayerId player = data.getPlayerList().getPlayerId(playerName);
    player.setWhoAmI(endWhoAmI);
  }

  @Override
  public Change invert() {
    return new PlayerWhoAmIChange(endWhoAmI, startWhoAmI, playerName);
  }

  @Override
  public String toString() {
    return playerName + " changed from " + startWhoAmI + " to " + endWhoAmI;
  }
}
