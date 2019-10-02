package games.strategy.engine.framework.startup.ui;

import com.google.common.annotations.VisibleForTesting;
import games.strategy.engine.data.GameDataEvent;
import games.strategy.engine.framework.IGame;
import games.strategy.engine.framework.startup.SystemPropertyReader;
import games.strategy.engine.framework.startup.mc.GameSelectorModel;
import games.strategy.net.IConnectionChangeListener;
import games.strategy.net.INode;
import games.strategy.net.IServerMessenger;
import games.strategy.net.Node;
import java.time.Instant;
import java.util.Observer;
import java.util.Optional;
import java.util.Timer;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.logging.Level;
import javax.annotation.Nullable;
import lombok.extern.java.Log;
import org.triplea.game.server.HeadlessGameServer;
import org.triplea.http.client.lobby.game.hosting.GameHostingResponse;
import org.triplea.http.client.lobby.game.listing.GameListingClient;
import org.triplea.java.Timers;
import org.triplea.lobby.common.GameDescription;

/**
 * Watches a game in progress, and updates the Lobby with the state of the game.
 *
 * <p>This class opens its own connection to the lobby, and its own messenger.
 */
@Log
public class InGameLobbyWatcher {
  private boolean isShutdown = false;
  private String gameId;
  private GameSelectorModel gameSelectorModel;
  private final Observer gameSelectorModelObserver = (o, arg) -> gameSelectorModelUpdated();
  private IGame game;
  private GameDescription gameDescription;
  private final IConnectionChangeListener connectionChangeListener;
  private final boolean humanPlayer;

  private final GameListingClient gameListingClient;

  private final IServerMessenger serverMessenger;

  private final Timer keepAliveTimer;

  private InGameLobbyWatcher(
      final IServerMessenger serverMessenger,
      final GameHostingResponse gameHostingResponse,
      final GameListingClient gameListingClient,
      final Consumer<String> errorReporter,
      final Consumer<String> reconnectionReporter,
      @Nullable final InGameLobbyWatcher oldWatcher) {
    this(
        serverMessenger,
        gameHostingResponse,
        gameListingClient,
        errorReporter,
        reconnectionReporter,
        Optional.ofNullable(oldWatcher).map(old -> old.gameDescription).orElse(null),
        Optional.ofNullable(oldWatcher).map(old -> old.game).orElse(null));
  }

  private InGameLobbyWatcher(
      final IServerMessenger serverMessenger,
      final GameHostingResponse gameHostingResponse,
      final GameListingClient gameListingClient,
      final Consumer<String> errorReporter,
      final Consumer<String> reconnectionReporter,
      @Nullable final GameDescription oldGameDescription,
      @Nullable final IGame oldGame) {
    this.serverMessenger = serverMessenger;
    this.gameListingClient = gameListingClient;
    humanPlayer = !HeadlessGameServer.headless();

    final boolean passworded = SystemPropertyReader.serverIsPassworded();

    final Instant startDateTime =
        Optional.ofNullable(oldGameDescription)
            .map(GameDescription::getStartDateTime)
            .orElseGet(Instant::now);

    final int playerCount =
        Optional.ofNullable(oldGameDescription)
            .map(GameDescription::getPlayerCount)
            .orElseGet(() -> humanPlayer ? 1 : 0);

    final GameDescription.GameStatus gameStatus =
        Optional.ofNullable(oldGameDescription)
            .map(GameDescription::getStatus)
            .orElse(GameDescription.GameStatus.WAITING_FOR_PLAYERS);

    final int gameRound =
        Optional.ofNullable(oldGameDescription).map(GameDescription::getRound).orElse(0);

    final INode publicNode =
        new Node(
            serverMessenger.getLocalNode().getName(),
            SystemPropertyReader.customHost().orElseGet(gameHostingResponse::getPublicVisibleIp),
            SystemPropertyReader.customPort()
                .orElseGet(() -> serverMessenger.getLocalNode().getPort()));

    gameDescription =
        GameDescription.builder()
            .hostedBy(publicNode)
            .startDateTime(startDateTime)
            .gameName("???")
            .playerCount(playerCount)
            .status(gameStatus)
            .round(gameRound)
            .comment(SystemPropertyReader.gameComments())
            .passworded(passworded)
            .gameVersion("0")
            .build();

    gameId = gameListingClient.postGame(gameDescription.toLobbyGame());

    // Period time is chosen to less than half the keep-alive cut-off time. In case a keep-alive
    // message is lost or missed, we have time to send another one before reaching the cut-off time.
    keepAliveTimer =
        Timers.fixedRateTimer()
            .period((GameListingClient.KEEP_ALIVE_SECONDS / 2) - 1, TimeUnit.SECONDS)
            .task(
                LobbyWatcherKeepAliveTask.builder()
                    .gameId(gameId)
                    .gameIdSetter(id -> gameId = id)
                    .connectionLostReporter(errorReporter)
                    .connectionReEstablishedReporter(reconnectionReporter)
                    .keepAliveSender(gameListingClient::sendKeepAlive)
                    .gamePoster(() -> gameListingClient.postGame(gameDescription.toLobbyGame()))
                    .build());

    connectionChangeListener =
        new IConnectionChangeListener() {
          @Override
          public void connectionRemoved(final INode to) {
            updatePlayerCount();
          }

          @Override
          public void connectionAdded(final INode to) {
            updatePlayerCount();
          }
        };
    // when players join or leave the game update the connection count
    this.serverMessenger.addConnectionChangeListener(connectionChangeListener);
    if (oldGameDescription != null && oldGame != null) {
      this.setGameStatus(oldGameDescription.getStatus(), oldGame);
    }
  }

  /**
   * Reads system properties to see if we should connect to a lobby server.
   *
   * <p>After creation, those properties are cleared, since we should watch the first start game.
   *
   * @return Empty if no watcher should be created
   */
  public static Optional<InGameLobbyWatcher> newInGameLobbyWatcher(
      final IServerMessenger serverMessenger,
      final GameHostingResponse gameHostingResponse,
      final GameListingClient gameListingClient,
      final Consumer<String> errorReporter,
      final Consumer<String> reconnectionReporter,
      final InGameLobbyWatcher oldWatcher) {
    try {
      return Optional.of(
          new InGameLobbyWatcher(
              serverMessenger,
              gameHostingResponse,
              gameListingClient,
              errorReporter,
              reconnectionReporter,
              oldWatcher));
    } catch (final Exception e) {
      log.log(Level.SEVERE, "Failed to create in-game lobby watcher", e);
      return Optional.empty();
    }
  }

  @VisibleForTesting
  static String getLobbySystemProperty(final String key) {
    final String backupKey = key + ".backup";
    final @Nullable String value = System.getProperty(key);
    if (value != null) {
      System.setProperty(backupKey, value);
      return value;
    }

    return System.getProperty(backupKey);
  }

  private void setGame(@Nullable final IGame game) {
    this.game = game;
    if (game != null) {
      game.getData()
          .addGameDataEventListener(
              GameDataEvent.GAME_STEP_CHANGED,
              () -> gameStepChanged(game.getData().getSequence().getRound()));
    }
  }

  private void gameStepChanged(final int round) {
    postUpdate(gameDescription.withRound(round));
  }

  private void gameSelectorModelUpdated() {
    postUpdate(
        gameDescription
            .withGameName(gameSelectorModel.getGameName())
            .withGameVersion(gameSelectorModel.getGameVersion()));
  }

  void setGameSelectorModel(@Nullable final GameSelectorModel model) {
    cleanUpGameModelListener();
    if (model != null) {
      gameSelectorModel = model;
      gameSelectorModel.addObserver(gameSelectorModelObserver);
      gameSelectorModelUpdated();
    }
  }

  private void cleanUpGameModelListener() {
    if (gameSelectorModel != null) {
      gameSelectorModel.deleteObserver(gameSelectorModelObserver);
    }
  }

  private void updatePlayerCount() {
    postUpdate(
        gameDescription.withPlayerCount(serverMessenger.getNodes().size() - (humanPlayer ? 0 : 1)));
  }

  private void postUpdate(final GameDescription newDescription) {
    if (isShutdown || newDescription.equals(gameDescription)) {
      return;
    }
    gameDescription = newDescription;
    gameListingClient.updateGame(gameId, gameDescription.toLobbyGame());
  }

  void shutDown() {
    isShutdown = true;
    gameListingClient.removeGame(gameId);
    serverMessenger.removeConnectionChangeListener(connectionChangeListener);
    keepAliveTimer.cancel();
    cleanUpGameModelListener();
  }

  public boolean isActive() {
    return !isShutdown;
  }

  void setGameStatus(final GameDescription.GameStatus status, final IGame game) {
    setGame(game);
    postUpdate(
        gameDescription
            .withStatus(status)
            .withRound(game == null ? 0 : game.getData().getSequence().getRound()));
  }

  public String getComments() {
    return gameDescription.getComment();
  }

  void setGameComments(final String comments) {
    postUpdate(gameDescription.withComment(comments));
  }

  void setPassworded(final boolean passworded) {
    postUpdate(gameDescription.withPassworded(passworded));
  }
}
