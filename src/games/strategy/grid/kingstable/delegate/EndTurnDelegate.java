package games.strategy.grid.kingstable.delegate;

import java.io.Serializable;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.grid.delegate.AbstractPlayByEmailOrForumDelegate;
import games.strategy.grid.kingstable.attachments.PlayerAttachment;
import games.strategy.grid.kingstable.attachments.TerritoryAttachment;
import games.strategy.grid.ui.display.IGridGameDisplay;

/**
 * Responsible for checking for a winner in a game of King's Table.
 *
 */
public class EndTurnDelegate extends AbstractPlayByEmailOrForumDelegate {
  // private boolean gameOver = false;
  /**
   * Called before the delegate will run.
   */
  @Override
  public void start() {
    super.start();
    final PlayerID winner = checkForWinner();
    if (winner != null) {
      signalGameOver(winner.getName() + " wins!");// , waitToLeaveGame);
    }
  }

  @Override
  public void end() {
    super.end();
  }

  @Override
  public Serializable saveState() {
    final KingsTableEndTurnExtendedDelegateState state = new KingsTableEndTurnExtendedDelegateState();
    state.superState = super.saveState();
    // add other variables to state here:
    return state;
  }

  @Override
  public void loadState(final Serializable state) {
    final KingsTableEndTurnExtendedDelegateState s = (KingsTableEndTurnExtendedDelegateState) state;
    super.loadState(s.superState);
    // load other variables from state here:
  }

  /**
   * Notify all players that the game is over.
   *
   * @param status
   *        the "game over" text to be displayed to each user.
   */
  private void signalGameOver(final String status)// , CountDownLatch waiting)
  {
    // If the game is over, we need to be able to alert all UIs to that fact.
    // The display object can send a message to all UIs.
    m_bridge.getHistoryWriter().startEvent(status);
    final IGridGameDisplay display = (IGridGameDisplay) m_bridge.getDisplayChannelBroadcaster();
    display.setStatus(status);
    display.setGameOver();
    m_bridge.stopGameSequence();
  }

  /**
   * Check to see if anyone has won the game.
   *
   * @return the player who has won, or <code>null</code> if there is no winner yet
   */
  private PlayerID checkForWinner() {
    boolean defenderHasKing = false;
    PlayerID attacker = null;
    PlayerID defender = null;
    final GameData data = getData();
    for (final PlayerID player : data.getPlayerList().getPlayers()) {
      final PlayerAttachment pa = (PlayerAttachment) player.getAttachment("playerAttachment");
      if (pa == null) {
        attacker = player;
      } else if (pa.getNeedsKing()) {
        defender = player;
      } else {
        attacker = player;
      }
    }
    if (attacker == null) {
      throw new RuntimeException(
          "Invalid game setup - no attacker is specified. Reconfigure the game xml file so that one player has a playerAttachment with needsKing set to false.");
    }
    if (defender == null) {
      throw new RuntimeException(
          "Invalid game setup - no defender is specified. Reconfigure the game xml file so that one player has a playerAttachment with needsKing set to true.");
    }
    int numAttackerPieces = 0;
    int numDefenderPieces = 0;
    for (final Territory t : data.getMap().getTerritories()) {
      if (t.getUnits().isEmpty()) {
        continue;
      }
      final Unit unit = (Unit) t.getUnits().getUnits().toArray()[0];
      if (unit.getType().getName().equals("king")) {
        defenderHasKing = true;
      }
      if (unit.getOwner().equals(defender)) {
        numDefenderPieces++;
      } else if (unit.getOwner().equals(attacker)) {
        numAttackerPieces++;
      }
      final TerritoryAttachment ta = (TerritoryAttachment) t.getAttachment("territoryAttachment");
      // System.out.println(ta.getName());
      if (ta != null && ta.getKingsExit() && !t.getUnits().isEmpty() && unit.getOwner().equals(defender)) {
        return defender;
      }
    }
    if (!defenderHasKing || numDefenderPieces == 0) {
      return attacker;
    }
    if (numAttackerPieces == 0) {
      return defender;
    }
    return null;
  }
}


class KingsTableEndTurnExtendedDelegateState implements Serializable {
  private static final long serialVersionUID = 1054956757425820238L;
  Serializable superState;
  // add other variables here:
}
