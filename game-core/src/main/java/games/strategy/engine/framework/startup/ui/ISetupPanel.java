package games.strategy.engine.framework.startup.ui;

import java.util.List;
import java.util.Observer;
import java.util.Optional;

import javax.swing.Action;
import javax.swing.JComponent;

import games.strategy.engine.chat.IChatPanel;
import games.strategy.engine.framework.startup.launcher.ILauncher;

/**
 * Made so that we can have a headless setup. (this is probably a hack, but used because i do not
 * want to rewrite the entire setup model).
 */
public interface ISetupPanel {
  JComponent getDrawable();

  boolean showCancelButton();

  void addObserver(final Observer observer);

  void notifyObservers();

  /** Subclasses that have chat override this. */
  IChatPanel getChatPanel();

  /** Cleanup should occur here that occurs when we cancel. */
  void cancel();

  /** Indicates we can start the game. */
  boolean canGameStart();

  void preStartGame();

  void postStartGame();

  Optional<ILauncher> getLauncher();

  List<Action> getUserActions();
}
