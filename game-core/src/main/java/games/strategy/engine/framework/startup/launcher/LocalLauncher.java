package games.strategy.engine.framework.startup.launcher;

import java.awt.Component;
import java.util.HashMap;
import java.util.Set;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import games.strategy.debug.ClientLogger;
import games.strategy.engine.framework.ServerGame;
import games.strategy.engine.framework.message.PlayerListing;
import games.strategy.engine.framework.startup.mc.GameSelectorModel;
import games.strategy.engine.gamePlayer.IGamePlayer;
import games.strategy.engine.random.IRandomSource;
import games.strategy.net.HeadlessServerMessenger;
import games.strategy.net.Messengers;
import games.strategy.util.Interruptibles;

public class LocalLauncher extends AbstractLauncher {
  private final IRandomSource randomSource;
  private final PlayerListing playerListing;

  public LocalLauncher(final GameSelectorModel gameSelectorModel, final IRandomSource randomSource,
      final PlayerListing playerListing) {
    super(gameSelectorModel);
    this.randomSource = randomSource;
    this.playerListing = playerListing;
  }

  @Override
  protected void launchInNewThread(final Component parent) {
    Exception exceptionLoadingGame = null;
    ServerGame game = null;
    try {
      gameData.doPreGameStartDataModifications(playerListing);
      final Messengers messengers = new Messengers(new HeadlessServerMessenger());
      final Set<IGamePlayer> gamePlayers =
          gameData.getGameLoader().createPlayers(playerListing.getLocalPlayerTypes());
      game = new ServerGame(gameData, gamePlayers, new HashMap<>(), messengers);
      game.setRandomSource(randomSource);
      gameData.getGameLoader().startGame(game, gamePlayers, headless);
    } catch (final MapNotFoundException e) {
      exceptionLoadingGame = e;
    } catch (final Exception ex) {
      ClientLogger.logQuietly("Failed to start game", ex);
      exceptionLoadingGame = ex;
    } finally {
      gameLoadingWindow.doneWait();
    }
    try {
      if (exceptionLoadingGame == null) {
        game.startGame();
      }
    } finally {
      // todo(kg), this does not occur on the swing thread, and this notifies setupPanel observers
      // having an oddball issue with the zip stream being closed while parsing to load default game. might be caused
      // by closing of stream while unloading map resources.
      Interruptibles.sleep(100);
      gameSelectorModel.loadDefaultGameNewThread();
      SwingUtilities.invokeLater(() -> JOptionPane.getFrameForComponent(parent).setVisible(true));
    }
  }
}
