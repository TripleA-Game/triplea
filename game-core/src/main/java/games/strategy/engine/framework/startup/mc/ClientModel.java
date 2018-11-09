package games.strategy.engine.framework.startup.mc;

import static games.strategy.engine.framework.CliProperties.TRIPLEA_CLIENT;
import static games.strategy.engine.framework.CliProperties.TRIPLEA_HOST;
import static games.strategy.engine.framework.CliProperties.TRIPLEA_NAME;
import static games.strategy.engine.framework.CliProperties.TRIPLEA_PORT;

import java.awt.Component;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.swing.Action;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import com.google.common.base.Preconditions;

import games.strategy.engine.chat.Chat;
import games.strategy.engine.chat.ChatPanel;
import games.strategy.engine.chat.IChatPanel;
import games.strategy.engine.data.GameData;
import games.strategy.engine.framework.ClientGame;
import games.strategy.engine.framework.GameDataManager;
import games.strategy.engine.framework.GameObjectStreamFactory;
import games.strategy.engine.framework.GameRunner;
import games.strategy.engine.framework.GameState;
import games.strategy.engine.framework.HeadlessAutoSaveType;
import games.strategy.engine.framework.message.PlayerListing;
import games.strategy.engine.framework.network.ui.ChangeGameOptionsClientAction;
import games.strategy.engine.framework.network.ui.ChangeGameToSaveGameClientAction;
import games.strategy.engine.framework.network.ui.ChangeToAutosaveClientAction;
import games.strategy.engine.framework.network.ui.GetGameSaveClientAction;
import games.strategy.engine.framework.network.ui.SetMapClientAction;
import games.strategy.engine.framework.startup.launcher.IServerReady;
import games.strategy.engine.framework.startup.login.ClientLogin;
import games.strategy.engine.framework.startup.ui.ClientOptions;
import games.strategy.engine.framework.startup.ui.PlayerType;
import games.strategy.engine.framework.ui.background.WaitWindow;
import games.strategy.engine.message.ChannelMessenger;
import games.strategy.engine.message.IChannelMessenger;
import games.strategy.engine.message.IRemoteMessenger;
import games.strategy.engine.message.RemoteMessenger;
import games.strategy.engine.message.RemoteName;
import games.strategy.engine.message.unifiedmessenger.UnifiedMessenger;
import games.strategy.engine.player.IGamePlayer;
import games.strategy.io.IoUtils;
import games.strategy.net.ClientMessenger;
import games.strategy.net.CouldNotLogInException;
import games.strategy.net.IClientMessenger;
import games.strategy.net.IMessengerErrorListener;
import games.strategy.net.INode;
import games.strategy.net.MacFinder;
import games.strategy.net.Messengers;
import games.strategy.triplea.settings.ClientSetting;
import games.strategy.ui.SwingAction;
import games.strategy.util.EventThreadJOptionPane;
import games.strategy.util.Interruptibles;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.java.Log;

/**
 * Represents a network aware game client connecting to another game that is acting as a server.
 */
@Log
public class ClientModel implements IMessengerErrorListener {

  public static final RemoteName CLIENT_READY_CHANNEL =
      new RemoteName("games.strategy.engine.framework.startup.mc.ClientModel.CLIENT_READY_CHANNEL", IServerReady.class);
  private final GameObjectStreamFactory objectStreamFactory = new GameObjectStreamFactory(null);
  private final GameSelectorModel gameSelectorModel;
  private final SetupPanelModel typePanelModel;
  private final WaitWindow gameLoadingWindow;
  private IRemoteModelListener listener = IRemoteModelListener.NULL_LISTENER;
  private IChannelMessenger channelMessenger;
  private IRemoteMessenger remoteMessenger;
  private IClientMessenger messenger;
  private Component ui;
  private IChatPanel chatPanel;
  private ClientGame game;
  private boolean hostIsHeadlessBot = false;
  // we set the game data to be null, since we
  // are a client game, and the game data lives on the server
  // however, if we cancel, we want to restore the old game data.
  private GameData gameDataOnStartup;
  private Map<String, String> playersToNodes = new HashMap<>();
  private final IObserverWaitingToJoin observerWaitingToJoin = new IObserverWaitingToJoin() {
    @Override
    public void joinGame(final byte[] gameData, final Map<String, INode> players) {
      remoteMessenger.unregisterRemote(ServerModel.getObserverWaitingToStartName(messenger.getLocalNode()));
      final CountDownLatch latch = new CountDownLatch(1);
      startGame(gameData, players, latch, true);
      try {
        latch.await(GameRunner.MINIMUM_CLIENT_GAMEDATA_LOAD_GRACE_TIME, TimeUnit.SECONDS);
      } catch (final InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }

    @Override
    public void cannotJoinGame(final String reason) {
      SwingUtilities.invokeLater(() -> {
        typePanelModel.showSelectType();
        EventThreadJOptionPane.showMessageDialog(ui, "Could not join game: " + reason);
      });
    }
  };
  private Map<String, Boolean> playersEnabledListing = new HashMap<>();
  private Collection<String> playersAllowedToBeDisabled = new HashSet<>();
  private Map<String, Collection<String>> playerNamesAndAlliancesInTurnOrder = new LinkedHashMap<>();
  private final IClientChannel channelListener = new IClientChannel() {
    @Override
    public void playerListingChanged(final PlayerListing listing) {
      internalPlayerListingChanged(listing);
    }

    @Override
    public void gameReset() {
      objectStreamFactory.setData(null);
      GameRunner.showMainFrame();
    }

    @Override
    public void doneSelectingPlayers(final byte[] gameData, final Map<String, INode> players) {
      final CountDownLatch latch = new CountDownLatch(1);
      startGame(gameData, players, latch, false);
      try {
        latch.await(GameRunner.MINIMUM_CLIENT_GAMEDATA_LOAD_GRACE_TIME, TimeUnit.SECONDS);
      } catch (final InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
  };

  public ClientModel(final GameSelectorModel gameSelectorModel, final SetupPanelModel typePanelModel) {
    this.typePanelModel = typePanelModel;
    this.gameSelectorModel = gameSelectorModel;
    final Interruptibles.Result<WaitWindow> window = Interruptibles
        .awaitResult(() -> SwingAction.invokeAndWaitResult(WaitWindow::new));
    if (!window.completed) {
      throw new IllegalStateException("Error while creating WaitWindow");
    }
    gameLoadingWindow = window.result
        .orElseThrow(() -> new IllegalStateException("Constructor did not return instance"));
  }

  public void setRemoteModelListener(@Nonnull final IRemoteModelListener listener) {
    this.listener = Preconditions.checkNotNull(listener);
    internalPlayerListingChanged(getServerStartup().getPlayerListing());
  }

  private static ClientProps getProps(final Component ui) {
    if (System.getProperty(TRIPLEA_CLIENT, "false").equals("true")
        && GameState.notStarted()) {
      final ClientProps props = new ClientProps();
      props.setHost(System.getProperty(TRIPLEA_HOST));
      props.setName(System.getProperty(TRIPLEA_NAME));
      props.setPort(Integer.parseInt(System.getProperty(TRIPLEA_PORT)));
      GameState.setStarted();
      return props;
    }
    // load in the saved name!
    final String playername = ClientSetting.playerName.getValueOrThrow();
    final Interruptibles.Result<ClientProps> result = Interruptibles.awaitResult(() -> SwingAction
        .invokeAndWaitResult(() -> {
          final ClientOptions options = new ClientOptions(ui, playername, GameRunner.PORT, "127.0.0.1");
          options.setLocationRelativeTo(ui);
          options.setVisible(true);
          options.dispose();
          if (!options.getOkPressed()) {
            return null;
          }
          final ClientProps props = new ClientProps();
          props.setHost(options.getAddress());
          props.setName(options.getName());
          props.setPort(options.getPort());
          return props;
        }));
    if (!result.completed) {
      throw new IllegalStateException("Error during component creation of ClientOptions.");
    }
    return result.result.orElse(null);
  }

  boolean createClientMessenger(final Component ui) {
    this.ui = JOptionPane.getFrameForComponent(ui);
    gameDataOnStartup = gameSelectorModel.getGameData();
    gameSelectorModel.setCanSelect(false);
    // load in the saved name!
    final ClientProps props = getProps(this.ui);
    if (props == null) {
      gameSelectorModel.setCanSelect(true);
      cancel();
      return false;
    }
    final String name = props.getName();
    log.fine("Client playing as:" + name);
    ClientSetting.playerName.setValue(name);
    ClientSetting.flush();
    final int port = props.getPort();
    if (port >= 65536 || port <= 0) {
      EventThreadJOptionPane.showMessageDialog(this.ui, "Invalid Port: " + port, "Error", JOptionPane.ERROR_MESSAGE);
      return false;
    }
    final String address = props.getHost();
    try {
      final String mac = MacFinder.getHashedMacAddress();
      messenger = new ClientMessenger(address, port, name, mac, objectStreamFactory, new ClientLogin(this.ui));
    } catch (final CouldNotLogInException e) {
      EventThreadJOptionPane.showMessageDialog(this.ui, e.getMessage());
      return false;
    } catch (final Exception ioe) {
      log.log(Level.SEVERE, "Unable to connect:" + ioe.getMessage(), ioe);
      return false;
    }
    messenger.addErrorListener(this);
    final UnifiedMessenger unifiedMessenger = new UnifiedMessenger(messenger);
    channelMessenger = new ChannelMessenger(unifiedMessenger);
    remoteMessenger = new RemoteMessenger(unifiedMessenger);
    channelMessenger.registerChannelSubscriber(channelListener, IClientChannel.CHANNEL_NAME);
    chatPanel = ChatPanel.createChatPanel(messenger, channelMessenger, remoteMessenger, ServerModel.CHAT_NAME,
        Chat.ChatSoundProfile.GAME_CHATROOM);
    if (getIsServerHeadlessTest()) {
      gameSelectorModel.setClientModelForHostBots(this);
      ((ChatPanel) chatPanel).getChatMessagePanel()
          .addServerMessage("Welcome to an automated dedicated host service (a host bot). "
              + "\nIf anyone disconnects, the autosave will be reloaded (a save might be loaded right now). "
              + "\nYou can get the current save, or you can load a save (only saves that it has the map for).");
    }
    remoteMessenger.registerRemote(observerWaitingToJoin,
        ServerModel.getObserverWaitingToStartName(messenger.getLocalNode()));
    // save this, it will be cleared later
    gameDataOnStartup = gameSelectorModel.getGameData();
    final IServerStartupRemote serverStartup = getServerStartup();
    final PlayerListing players = serverStartup.getPlayerListing();
    internalPlayerListingChanged(players);
    if (!serverStartup.isGameStarted(messenger.getLocalNode())) {
      remoteMessenger.unregisterRemote(ServerModel.getObserverWaitingToStartName(messenger.getLocalNode()));
    }
    gameSelectorModel.setIsHostHeadlessBot(hostIsHeadlessBot);
    return true;
  }

  private IServerStartupRemote getServerStartup() {
    return (IServerStartupRemote) remoteMessenger.getRemote(ServerModel.SERVER_REMOTE_NAME);
  }

  private List<String> getAvailableServerGames() {
    final Set<String> games = getServerStartup().getAvailableGames();
    if (games == null) {
      return new ArrayList<>();
    }
    return new ArrayList<>(games);
  }

  /**
   * Resets stats and nulls out references, keeps chat alive.
   */
  public void cancel() {
    if (messenger == null) {
      return;
    }
    objectStreamFactory.setData(null);
    messenger.shutDown();
    chatPanel.setChat(null);
    gameSelectorModel.setGameData(gameDataOnStartup);
    gameSelectorModel.setCanSelect(true);
    hostIsHeadlessBot = false;
    gameSelectorModel.setIsHostHeadlessBot(false);
    gameSelectorModel.setClientModelForHostBots(null);
    messenger.removeErrorListener(this);
  }

  private void startGame(final byte[] gameData, final Map<String, INode> players, final CountDownLatch onDone,
      final boolean gameRunning) {
    SwingUtilities.invokeLater(() -> {
      gameLoadingWindow.setVisible(true);
      gameLoadingWindow.setLocationRelativeTo(JOptionPane.getFrameForComponent(ui));
      gameLoadingWindow.showWait();
    });
    try {
      startGameInNewThread(gameData, players, gameRunning);
    } catch (final RuntimeException e) {
      gameLoadingWindow.doneWait();
      throw e;
    } finally {
      if (onDone != null) {
        onDone.countDown();
      }
    }
  }

  private void startGameInNewThread(final byte[] gameData, final Map<String, INode> players,
      final boolean gameRunning) {
    final GameData data;
    try {
      // this normally takes a couple seconds, but can take
      // up to 60 seconds for a freaking huge game
      data = IoUtils.readFromMemory(gameData, GameDataManager::loadGame);
    } catch (final IOException ex) {
      log.log(Level.SEVERE, "Failed to load game", ex);
      return;
    }
    objectStreamFactory.setData(data);
    final Map<String, PlayerType> playerMapping = playersToNodes.entrySet()
        .stream()
        .filter(e -> e.getValue() != null)
        .filter(e -> e.getValue().equals(messenger.getLocalNode().getName()))
        .collect(Collectors.toMap(Map.Entry::getKey, e -> PlayerType.CLIENT_PLAYER));
    final Set<IGamePlayer> playerSet = data.getGameLoader().createPlayers(playerMapping);
    final Messengers messengers = new Messengers(messenger, remoteMessenger, channelMessenger);
    game = new ClientGame(data, playerSet, players, messengers);
    new Thread(() -> {
      SwingUtilities.invokeLater(() -> JOptionPane.getFrameForComponent(ui).setVisible(false));
      try {
        // game will be null if we loose the connection
        if (game != null) {
          try {
            data.getGameLoader().startGame(game, playerSet, false, getChatPanel().getChat());
          } catch (final Exception e) {
            log.log(Level.SEVERE, "Failed to start Game", e);
            game.shutDown();
            messenger.shutDown();
            gameLoadingWindow.doneWait();
            // an ugly hack, we need a better
            // way to get the main frame
            GameRunner.clientLeftGame();
          }
        }
        if (!gameRunning) {
          ((IServerReady) remoteMessenger.getRemote(CLIENT_READY_CHANNEL)).clientReady();
        }
      } finally {
        gameLoadingWindow.doneWait();
      }
    }, "Client Game Launcher").start();
  }

  public void takePlayer(final String playerName) {
    getServerStartup().takePlayer(messenger.getLocalNode(), playerName);
  }

  public void releasePlayer(final String playerName) {
    getServerStartup().releasePlayer(messenger.getLocalNode(), playerName);
  }

  public void disablePlayer(final String playerName) {
    getServerStartup().disablePlayer(playerName);
  }

  public void enablePlayer(final String playerName) {
    getServerStartup().enablePlayer(playerName);
  }

  private void internalPlayerListingChanged(final PlayerListing listing) {
    gameSelectorModel.clearDataButKeepGameInfo(listing.getGameName(), listing.getGameRound(),
        listing.getGameVersion().toString());
    synchronized (this) {
      playersToNodes = listing.getPlayerToNodeListing();
      playersEnabledListing = listing.getPlayersEnabledListing();
      playersAllowedToBeDisabled = listing.getPlayersAllowedToBeDisabled();
      playerNamesAndAlliancesInTurnOrder = listing.getPlayerNamesAndAlliancesInTurnOrderLinkedHashMap();
    }
    listener.playerListChanged();
  }

  /**
   * Local player name, eg: US, to remote node (player physical name, eg: "bob) mapping.
   */
  public synchronized Map<String, String> getPlayerToNodesMapping() {
    return new HashMap<>(playersToNodes);
  }

  /**
   * Returns a map of player node name -> enabled.
   */
  public synchronized Map<String, Boolean> getPlayersEnabledListing() {
    return new HashMap<>(playersEnabledListing);
  }

  /**
   * Returns the set of players that can be disabled.
   */
  public synchronized Collection<String> getPlayersAllowedToBeDisabled() {
    return new HashSet<>(playersAllowedToBeDisabled);
  }

  public synchronized Map<String, Collection<String>> getPlayerNamesAndAlliancesInTurnOrderLinkedHashMap() {
    return new LinkedHashMap<>(playerNamesAndAlliancesInTurnOrder);
  }

  public IClientMessenger getMessenger() {
    return messenger;
  }

  public IServerStartupRemote getServerStartupRemote() {
    return getServerStartup();
  }


  @Override
  public void messengerInvalid(final Throwable reason) {
    // The self chat disconnect notification is simply so we have an on-screen notification of the disconnect.
    // In case for example there are many game windows open, it may not be clear which game disconnected.
    if (chatPanel != null) {
      Optional.ofNullable(chatPanel.getChat())
          .ifPresent(chat -> chat.sendMessage("*** Was Disconnected ***", false));
    }
    EventThreadJOptionPane.showMessageDialog(ui, "Connection to game host lost.\nPlease save and restart.",
        "Connection Lost!", JOptionPane.ERROR_MESSAGE);
  }

  public IChatPanel getChatPanel() {
    return chatPanel;
  }

  private boolean getIsServerHeadlessTest() {
    final IServerStartupRemote serverRemote = getServerStartup();
    hostIsHeadlessBot = serverRemote != null && serverRemote.getIsServerHeadless();
    return hostIsHeadlessBot;
  }

  public boolean getIsServerHeadlessCached() {
    return hostIsHeadlessBot;
  }

  public Action getHostBotSetMapClientAction(final Component parent) {
    return new SetMapClientAction(parent, getMessenger(), getAvailableServerGames());
  }

  public Action getHostBotChangeGameOptionsClientAction(final Component parent) {
    return new ChangeGameOptionsClientAction(parent, getServerStartupRemote());
  }

  public Action getHostBotChangeGameToSaveGameClientAction() {
    return new ChangeGameToSaveGameClientAction(getMessenger());
  }

  public Action getHostBotChangeToAutosaveClientAction(final Component parent,
      final HeadlessAutoSaveType autosaveType) {
    return new ChangeToAutosaveClientAction(parent, getMessenger(), autosaveType);
  }

  public Action getHostBotGetGameSaveClientAction(final Component parent) {
    return new GetGameSaveClientAction(parent, getServerStartupRemote());
  }

  @Override
  public String toString() {
    return "ClientModel GameData:" + (gameDataOnStartup == null ? "null" : gameDataOnStartup.getGameName())
        + "\n"
        + "Connected:" + (messenger == null ? "null" : messenger.isConnected()) + "\n"
        + messenger
        + "\n"
        + remoteMessenger
        + "\n"
        + channelMessenger;
  }

  @Getter
  @Setter
  static class ClientProps {
    private int port;
    private String name;
    private String host;
  }
}
