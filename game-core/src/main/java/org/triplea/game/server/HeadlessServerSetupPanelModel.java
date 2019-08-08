package org.triplea.game.server;

import games.strategy.engine.chat.Chat;
import games.strategy.engine.framework.HeadlessAutoSaveFileUtils;
import games.strategy.engine.framework.IGame;
import games.strategy.engine.framework.LocalPlayers;
import games.strategy.engine.framework.ServerGame;
import games.strategy.engine.framework.startup.launcher.LaunchAction;
import games.strategy.engine.framework.startup.mc.GameSelectorModel;
import games.strategy.engine.framework.startup.mc.ServerModel;
import games.strategy.engine.player.IGamePlayer;
import games.strategy.sound.HeadlessSoundChannel;
import games.strategy.sound.ISound;
import games.strategy.triplea.ui.HeadlessUiContext;
import games.strategy.triplea.ui.UiContext;
import games.strategy.triplea.ui.display.HeadlessDisplay;
import games.strategy.triplea.ui.display.ITripleADisplay;
import java.io.File;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import lombok.extern.java.Log;
import org.triplea.game.startup.ServerSetupModel;

/** Setup panel model for headless server. */
@Log
public class HeadlessServerSetupPanelModel implements ServerSetupModel {

  private final GameSelectorModel gameSelectorModel;
  private HeadlessServerSetup headlessServerSetup;

  HeadlessServerSetupPanelModel(final GameSelectorModel gameSelectorModel) {
    this.gameSelectorModel = gameSelectorModel;
  }

  @Override
  public void showSelectType() {
    new ServerModel(
            gameSelectorModel,
            this,
            null,
            new LaunchAction() {
              @Override
              public void handleGameInterruption(
                  final GameSelectorModel gameSelectorModel, final ServerModel serverModel) {
                try {
                  log.info("Game ended, going back to waiting.");
                  // if we do not do this, we can get into an infinite loop of launching a game,
                  // then crashing out, then launching, etc.
                  serverModel.setAllPlayersToNullNodes();
                  final File f1 = getAutoSaveFileUtils().getHeadlessAutoSaveFile();
                  if (!f1.exists() || !gameSelectorModel.load(f1)) {
                    gameSelectorModel.resetGameDataToNull();
                  }
                } catch (final Exception e1) {
                  log.log(Level.SEVERE, "Failed to load game", e1);
                  gameSelectorModel.resetGameDataToNull();
                }
              }

              @Override
              public void onGameInterrupt() {
                // tell headless server to wait for new connections:
                HeadlessGameServer.waitForUsersHeadlessInstance();
              }

              @Override
              public void onEnd(final String message) {
                log.info(message);
              }

              @Override
              public ITripleADisplay startGame(
                  final LocalPlayers localPlayers,
                  final IGame game,
                  final Set<IGamePlayer> players,
                  final Chat chat) {
                final UiContext uiContext = new HeadlessUiContext();
                uiContext.setDefaultMapDir(game.getData());
                uiContext.setLocalPlayers(localPlayers);
                return new HeadlessDisplay();
              }

              @Override
              public ISound getSoundChannel(final LocalPlayers localPlayers) {
                return new HeadlessSoundChannel();
              }

              @Override
              public File getAutoSaveFile() {
                return getAutoSaveFileUtils().getHeadlessAutoSaveFile();
              }

              @Override
              public void onLaunch(final ServerGame serverGame) {
                HeadlessGameServer.setServerGame(serverGame);
              }

              @Override
              public HeadlessAutoSaveFileUtils getAutoSaveFileUtils() {
                return new HeadlessAutoSaveFileUtils();
              }
            })
        .createServerMessenger();
  }

  @Override
  public void onServerMessengerCreated(final ServerModel serverModel) {
    Optional.ofNullable(headlessServerSetup).ifPresent(HeadlessServerSetup::cancel);
    headlessServerSetup = new HeadlessServerSetup(serverModel, gameSelectorModel);
  }

  public HeadlessServerSetup getPanel() {
    return headlessServerSetup;
  }
}
