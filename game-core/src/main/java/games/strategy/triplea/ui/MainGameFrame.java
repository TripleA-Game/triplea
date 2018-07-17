package games.strategy.triplea.ui;

import java.awt.Dimension;

import javax.swing.JComponent;
import javax.swing.JFrame;

import games.strategy.engine.framework.GameRunner;
import games.strategy.engine.framework.IGame;
import games.strategy.engine.framework.LocalPlayers;

public abstract class MainGameFrame extends JFrame {
  private static final long serialVersionUID = 7433347393639606647L;
  protected final LocalPlayers localPlayers;

  protected MainGameFrame(final String name, final LocalPlayers players) {
    super(name);
    localPlayers = players;
    setIconImage(GameRunner.getGameIcon(this));
    // 200 size is pretty arbitrary, goal is to not allow users to shrink window down to nothing.
    setMinimumSize(new Dimension(200, 200));
  }

  public abstract IGame getGame();

  public abstract void leaveGame();

  public abstract void stopGame();

  public abstract void shutdown();

  public abstract void notifyError(String error);

  public abstract void setShowChatTime(final boolean showTime);

  public LocalPlayers getLocalPlayers() {
    return localPlayers;
  }
}
