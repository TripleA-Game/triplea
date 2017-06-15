package games.strategy.engine.framework.startup.ui;

import java.awt.Frame;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Observer;

import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import games.strategy.debug.ClientLogger;
import games.strategy.debug.HeartBeat;
import games.strategy.engine.ClientContext;
import games.strategy.engine.data.events.GameStepListener;
import games.strategy.engine.framework.GameRunner;
import games.strategy.engine.framework.IGame;
import games.strategy.engine.framework.headlessGameServer.HeadlessGameServer;
import games.strategy.engine.framework.startup.mc.GameSelectorModel;
import games.strategy.engine.lobby.server.GameDescription;
import games.strategy.engine.lobby.server.GameDescription.GameStatus;
import games.strategy.engine.lobby.server.ILobbyGameController;
import games.strategy.engine.lobby.server.LobbyServer;
import games.strategy.engine.lobby.server.RemoteHostUtils;
import games.strategy.engine.lobby.server.login.LobbyLoginValidator;
import games.strategy.engine.message.IRemoteMessenger;
import games.strategy.engine.message.RemoteMessenger;
import games.strategy.engine.message.unifiedmessenger.UnifiedMessenger;
import games.strategy.net.ClientMessenger;
import games.strategy.net.GUID;
import games.strategy.net.IConnectionChangeListener;
import games.strategy.net.IConnectionLogin;
import games.strategy.net.IMessenger;
import games.strategy.net.IMessengerErrorListener;
import games.strategy.net.INode;
import games.strategy.net.IServerMessenger;
import games.strategy.net.MacFinder;
import games.strategy.net.OpenFileUtility;
import games.strategy.triplea.UrlConstants;

/**
 * Watches a game in progress, and updates the Lobby with the state of the game.
 *
 * <p>
 * This class opens its own connection to the lobby, and its own messenger.
 * </p>
 */
public class InGameLobbyWatcher {
  public static final String LOBBY_WATCHER_NAME = "lobby_watcher";
  // this is the messenger used by the game
  // it is different than the messenger we use to connect to
  // the game lobby
  private final IServerMessenger m_gameMessenger;
  private boolean m_shutdown = false;
  private final GUID m_gameID = new GUID();
  private GameSelectorModel m_gameSelectorModel;
  private final Observer m_gameSelectorModelObserver = (o, arg) -> gameSelectorModelUpdated();
  private IGame m_game;
  private final GameStepListener m_gameStepListener =
      (stepName, delegateName, player, round, displayName) -> InGameLobbyWatcher.this.gameStepChanged(round);
  // we create this messenger, and use it to connect to the
  // game lobby
  private final IMessenger m_messenger;
  private final IRemoteMessenger m_remoteMessenger;
  private final GameDescription m_gameDescription;
  private final Object m_mutex = new Object();
  private final IConnectionChangeListener m_connectionChangeListener;
  private final IMessengerErrorListener m_messengerErrorListener;

  /**
   * Reads SystemProperties to see if we should connect to a lobby server
   *
   * <p>
   * After creation, those properties are cleared, since we should watch the first start game.
   * </p>
   *
   * @return null if no watcher should be created
   */
  public static InGameLobbyWatcher newInGameLobbyWatcher(final IServerMessenger gameMessenger, final JComponent parent,
      final InGameLobbyWatcher oldWatcher) {
    final String host = System.getProperties().getProperty(GameRunner.LOBBY_HOST);
    final String port = System.getProperties().getProperty(GameRunner.TRIPLEA_LOBBY_PORT_PROPERTY);
    final String hostedBy = System.getProperties().getProperty(GameRunner.LOBBY_GAME_HOSTED_BY);
    if (host == null || port == null) {
      return null;
    }
    // clear the properties
    System.getProperties().remove(GameRunner.LOBBY_HOST);
    System.getProperties().remove(GameRunner.TRIPLEA_LOBBY_PORT_PROPERTY);
    System.getProperties().remove(GameRunner.LOBBY_GAME_HOSTED_BY);
    // add them as temporary properties (in case we load an old savegame and need them again)
    System.getProperties().setProperty(GameRunner.LOBBY_HOST + GameRunner.OLD_EXTENSION, host);
    System.getProperties().setProperty(GameRunner.TRIPLEA_LOBBY_PORT_PROPERTY + GameRunner.OLD_EXTENSION, port);
    System.getProperties().setProperty(GameRunner.LOBBY_GAME_HOSTED_BY + GameRunner.OLD_EXTENSION, hostedBy);
    final IConnectionLogin login = new IConnectionLogin() {
      @Override
      public void notifyFailedLogin(final String message) {}

      @Override
      public Map<String, String> getProperties(final Map<String, String> challengProperties) {
        final Map<String, String> rVal = new HashMap<>();
        rVal.put(LobbyLoginValidator.ANONYMOUS_LOGIN, Boolean.TRUE.toString());
        rVal.put(LobbyLoginValidator.LOBBY_VERSION, LobbyServer.LOBBY_VERSION.toString());
        rVal.put(LobbyLoginValidator.LOBBY_WATCHER_LOGIN, Boolean.TRUE.toString());
        return rVal;
      }
    };
    try {
      System.out.println("host:" + host + " port:" + port);
      final String mac = MacFinder.getHashedMacAddress();
      final ClientMessenger messenger = new ClientMessenger(host, Integer.parseInt(port),
          getRealName(hostedBy) + "_" + LOBBY_WATCHER_NAME, mac, login);
      final UnifiedMessenger um = new UnifiedMessenger(messenger);
      final RemoteMessenger rm = new RemoteMessenger(um);
      final HeartBeat h = new HeartBeat(messenger.getServerNode());
      rm.registerRemote(h, HeartBeat.getHeartBeatName(um.getLocalNode()));
      final RemoteHostUtils rhu = new RemoteHostUtils(messenger.getServerNode(), gameMessenger);
      rm.registerRemote(rhu, RemoteHostUtils.getRemoteHostUtilsName(um.getLocalNode()));
      return new InGameLobbyWatcher(messenger, rm, gameMessenger, parent, oldWatcher);
    } catch (final Exception e) {
      ClientLogger.logQuietly(e);
      return null;
    }
  }

  private static String getRealName(final String uniqueName) {
    // Remove any (n) that is added to distinguish duplicate names
    final String name = uniqueName.split(" ")[0];
    return name;
  }

  void setGame(final IGame game) {
    if (m_game != null) {
      m_game.removeGameStepListener(m_gameStepListener);
    }
    m_game = game;
    if (game != null) {
      game.addGameStepListener(m_gameStepListener);
      gameStepChanged(game.getData().getSequence().getRound());
    }
  }

  private void gameStepChanged(final int round) {
    synchronized (m_mutex) {
      if (!m_gameDescription.getRound().equals(Integer.toString(round))) {
        m_gameDescription.setRound(round + "");
      }
      postUpdate();
    }
  }

  private void gameSelectorModelUpdated() {
    synchronized (m_mutex) {
      m_gameDescription.setGameName(m_gameSelectorModel.getGameName());
      m_gameDescription.setGameVersion(m_gameSelectorModel.getGameVersion());
      postUpdate();
    }
  }

  InGameLobbyWatcher(final IMessenger messenger, final IRemoteMessenger remoteMessenger,
      final IServerMessenger serverMessenger, final JComponent parent, final InGameLobbyWatcher oldWatcher) {
    m_messenger = messenger;
    m_remoteMessenger = remoteMessenger;
    m_gameMessenger = serverMessenger;
    final String password = System.getProperty(GameRunner.TRIPLEA_SERVER_PASSWORD_PROPERTY);
    final boolean passworded = password != null && password.length() > 0;
    final Date startDateTime = (oldWatcher == null || oldWatcher.m_gameDescription == null
        || oldWatcher.m_gameDescription.getStartDateTime() == null) ? new Date()
            : oldWatcher.m_gameDescription.getStartDateTime();
    final int playerCount = (oldWatcher == null || oldWatcher.m_gameDescription == null)
        ? (HeadlessGameServer.headless() ? 0 : 1) : oldWatcher.m_gameDescription.getPlayerCount();
    final GameStatus gameStatus =
        (oldWatcher == null || oldWatcher.m_gameDescription == null || oldWatcher.m_gameDescription.getStatus() == null)
            ? GameStatus.WAITING_FOR_PLAYERS : oldWatcher.m_gameDescription.getStatus();
    final String gameRound =
        (oldWatcher == null || oldWatcher.m_gameDescription == null || oldWatcher.m_gameDescription.getRound() == null)
            ? "-" : oldWatcher.m_gameDescription.getRound();
    m_gameDescription = new GameDescription(m_messenger.getLocalNode(), m_gameMessenger.getLocalNode().getPort(),
        startDateTime, "???", playerCount, gameStatus, gameRound, m_gameMessenger.getLocalNode().getName(),
        System.getProperty(GameRunner.LOBBY_GAME_COMMENTS), passworded, ClientContext.engineVersion().toString(), "0");
    final ILobbyGameController controller =
        (ILobbyGameController) m_remoteMessenger.getRemote(ILobbyGameController.GAME_CONTROLLER_REMOTE);
    synchronized (m_mutex) {
      controller.postGame(m_gameID, (GameDescription) m_gameDescription.clone());
    }
    m_messengerErrorListener = (messenger1, reason) -> shutDown();
    m_messenger.addErrorListener(m_messengerErrorListener);
    m_connectionChangeListener = new IConnectionChangeListener() {
      @Override
      public void connectionRemoved(final INode to) {
        updatePlayerCount();
      }

      @Override
      public void connectionAdded(final INode to) {
        updatePlayerCount();
      }
    };
    // when players join or leave the game
    // update the connection count
    m_gameMessenger.addConnectionChangeListener(m_connectionChangeListener);
    if (oldWatcher != null && oldWatcher.m_gameDescription != null) {
      this.setGameStatus(oldWatcher.m_gameDescription.getStatus(), oldWatcher.m_game);
    }
    // if we loose our connection, then shutdown
    final Runnable r = () -> {
      final String addressUsed = controller.testGame(m_gameID);
      // if the server cannot connect to us, then quit
      if (addressUsed != null) {
        if (isActive()) {
          shutDown();
          SwingUtilities.invokeLater(() -> {
            String portString = System.getProperty(GameRunner.TRIPLEA_PORT_PROPERTY);
            if (portString == null || portString.trim().length() <= 0) {
              portString = "3300";
            }
            final String message = "Your computer is not reachable from the internet.\r\n"
                + "Please make sure your Firewall allows incoming connections (hosting) for TripleA.\r\n"
                + "(The firewall exception must be updated every time a new version of TripleA comes out.)\r\n"
                + "And that your Router is configured to send TCP traffic on port " + portString
                + " to your local ip address.\r\n"
                + "See 'How To Host...' in the help menu, at the top of the lobby screen.\r\n"
                + "The server tried to connect to your external ip: " + addressUsed + "\r\n";
            if (HeadlessGameServer.headless()) {
              System.out.println(message);
              System.exit(-1);
            }
            final Frame parentComponent = JOptionPane.getFrameForComponent(parent);
            if (JOptionPane.showConfirmDialog(parentComponent,
                "Do you want to view the tutorial on how to host?  This will open in your internet browser.",
                "View Help Website?", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
              OpenFileUtility.openURL(UrlConstants.HOSTING_GUIDE.toString());
            }
            System.exit(-1);
          });
        }
      }
    };
    new Thread(r).start();
  }

  void setGameSelectorModel(final GameSelectorModel model) {
    cleanUpGameModelListener();
    if (model != null) {
      m_gameSelectorModel = model;
      m_gameSelectorModel.addObserver(m_gameSelectorModelObserver);
      gameSelectorModelUpdated();
    }
  }

  private void cleanUpGameModelListener() {
    if (m_gameSelectorModel != null) {
      m_gameSelectorModel.deleteObserver(m_gameSelectorModelObserver);
    }
  }

  protected void updatePlayerCount() {
    synchronized (m_mutex) {
      m_gameDescription.setPlayerCount(m_gameMessenger.getNodes().size() - (HeadlessGameServer.headless() ? 1 : 0));
      postUpdate();
    }
  }

  private void postUpdate() {
    if (m_shutdown) {
      return;
    }
    synchronized (m_mutex) {
      final ILobbyGameController controller =
          (ILobbyGameController) m_remoteMessenger.getRemote(ILobbyGameController.GAME_CONTROLLER_REMOTE);
      controller.updateGame(m_gameID, (GameDescription) m_gameDescription.clone());
    }
  }

  void shutDown() {
    m_shutdown = true;
    m_messenger.removeErrorListener(m_messengerErrorListener);
    m_messenger.shutDown();
    m_gameMessenger.removeConnectionChangeListener(m_connectionChangeListener);
    cleanUpGameModelListener();
    if (m_game != null) {
      m_game.removeGameStepListener(m_gameStepListener);
    }
  }

  public boolean isActive() {
    return !m_shutdown;
  }

  void setGameStatus(final GameDescription.GameStatus status, final IGame game) {
    synchronized (m_mutex) {
      m_gameDescription.setStatus(status);
      if (game == null) {
        m_gameDescription.setRound("-");
      } else {
        m_gameDescription.setRound(game.getData().getSequence().getRound() + "");
      }
      setGame(game);
      postUpdate();
    }
  }

  public String getComments() {
    return m_gameDescription.getComment();
  }

  public GameDescription getGameDescription() {
    return m_gameDescription;
  }

  void setGameComments(final String comments) {
    synchronized (m_mutex) {
      m_gameDescription.setComment(comments);
      postUpdate();
    }
  }

  void setPassworded(final boolean passworded) {
    synchronized (m_mutex) {
      m_gameDescription.setPassworded(passworded);
      postUpdate();
    }
  }
}
