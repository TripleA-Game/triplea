package games.strategy.triplea.ui.menubar;

import games.strategy.engine.player.Player;
import games.strategy.triplea.ai.pro.AbstractProAi;
import games.strategy.triplea.ui.TripleAFrame;
import java.awt.event.KeyEvent;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import org.triplea.swing.SwingAction;

public final class DebugMenu extends JMenu {
  private static final long serialVersionUID = -4876915214715298132L;

  private static final Map<String, Function<TripleAFrame, Collection<JMenuItem>>> menuCallbacks =
      new TreeMap<>();

  DebugMenu(final TripleAFrame frame) {
    super("Debug");

    setMnemonic(KeyEvent.VK_D);

    final Set<Player> players = frame.getLocalPlayers().getLocalPlayers();
    final boolean areThereProAIs = players.stream().anyMatch(AbstractProAi.class::isInstance);
    if (areThereProAIs) {
      AbstractProAi.initialize(frame);
      add(SwingAction.of("Show Hard AI Logs", AbstractProAi::showSettingsWindow))
          .setMnemonic(KeyEvent.VK_X);
    }

    menuCallbacks.forEach(
        (name, callback) -> {
          final JMenu playerDebugMenu = new JMenu(name);
          add(playerDebugMenu);
          callback.apply(frame).forEach(playerDebugMenu::add);
        });
  }

  public static void registerMenuCallback(
      final String name, final Function<TripleAFrame, Collection<JMenuItem>> callback) {
    menuCallbacks.put(name, callback);
  }

  public static void unregisterMenuCallback(final String name) {
    menuCallbacks.remove(name);
  }
}
