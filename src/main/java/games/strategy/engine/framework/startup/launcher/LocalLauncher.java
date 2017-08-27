package games.strategy.engine.framework.startup.launcher;

import java.awt.Component;
import java.util.HashMap;
import java.util.Set;
import java.util.logging.Logger;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import games.strategy.debug.ClientLogger;
import games.strategy.engine.framework.ServerGame;
import games.strategy.engine.framework.message.PlayerListing;
import games.strategy.engine.framework.startup.mc.GameSelectorModel;
import games.strategy.engine.gamePlayer.IGamePlayer;
import games.strategy.engine.random.IRandomSource;
import games.strategy.engine.random.ScriptedRandomSource;
import games.strategy.net.HeadlessServerMessenger;
import games.strategy.net.Messengers;
import games.strategy.util.ThreadUtil;

public class LocalLauncher extends AbstractLauncher {
  private static final Logger logger = Logger.getLogger(ILauncher.class.getName());
  private final IRandomSource m_randomSource;
  private final PlayerListing m_playerListing;

  public LocalLauncher(final GameSelectorModel gameSelectorModel, final IRandomSource randomSource,
      final PlayerListing playerListing) {
    super(gameSelectorModel);
    m_randomSource = randomSource;
    m_playerListing = playerListing;
  }

  @Override
  protected void launchInNewThread(final Component parent) {
    Exception exceptionLoadingGame = null;
    ServerGame game = null;
    try {
      m_gameData.doPreGameStartDataModifications(m_playerListing);
      final Messengers messengers = new Messengers(new HeadlessServerMessenger());
      final Set<IGamePlayer> gamePlayers =
          m_gameData.getGameLoader().createPlayers(m_playerListing.getLocalPlayerTypes());
      game = new ServerGame(m_gameData, gamePlayers, new HashMap<>(), messengers);
      game.setRandomSource(m_randomSource);
      // for debugging, we can use a scripted random source
      if (ScriptedRandomSource.useScriptedRandom()) {
        game.setRandomSource(new ScriptedRandomSource());
      }
      m_gameData.getGameLoader().startGame(game, gamePlayers, m_headless);
    } catch (final MapNotFoundException e) {
      exceptionLoadingGame = e;
    } catch (final Exception ex) {
      ClientLogger.logQuietly(ex);
      exceptionLoadingGame = ex;
    } finally {
      m_gameLoadingWindow.doneWait();
    }
    try {
      if (exceptionLoadingGame == null) {
        logger.fine("Game starting");
        game.startGame();
        logger.fine("Game over");
      }
    } finally {
      // todo(kg), this does not occur on the swing thread, and this notifies setupPanel observers
      // having an oddball issue with the zip stream being closed while parsing to load default game. might be caused
      // by closing of stream while unloading map resources.
      ThreadUtil.sleep(100);
      m_gameSelectorModel.loadDefaultGame();
      SwingUtilities.invokeLater(() -> JOptionPane.getFrameForComponent(parent).setVisible(true));
    }
  }
}
